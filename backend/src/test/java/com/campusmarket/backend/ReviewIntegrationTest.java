package com.campusmarket.backend;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(classes = CampusMarketBackendApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /** Create a completed offline order and return the order JSON response. */
    private String completeOfflineOrder(String userToken, long productId) throws Exception {
        String cartResp = mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": %d, \"quantity\": 1}".formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();
        List<Number> cartIds = JsonPath.read(cartResp,
                "$.data.items[?(@.productId == %d)].id".formatted(productId));
        Number cartItemId = cartIds.get(0);

        String orderResp = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cartItemIds": [%s], "fulfillmentType": "offline",
                                 "offlineMeetTime": "2026-07-01 12:00",
                                 "offlineMeetLocation": "Library"}
                                """.formatted(cartItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("pending_payment"))
                .andReturn().getResponse().getContentAsString();
        Number orderId = JsonPath.read(orderResp, "$.data.id");

        String payResp = mockMvc.perform(post("/api/payments/orders/%s/initiate".formatted(orderId))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();
        String paymentNo = JsonPath.read(payResp, "$.data.payment.paymentNo");

        mockMvc.perform(post("/api/payments/%s/mock-success".formatted(paymentNo))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Offline: seller confirms first → pending_receipt
        mockMvc.perform(post("/api/admin/orders/%s/offline/seller-confirm".formatted(orderId))
                        .header("Authorization", "Bearer " + "mock-9001-ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        // Offline: buyer confirms → completed
        mockMvc.perform(post("/api/orders/%s/offline/buyer-confirm".formatted(orderId))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("completed"));

        return orderResp;
    }

    /** Get the first order item id from order detail. */
    private Number firstOrderItemId(String userToken, Number orderId) throws Exception {
        String detailResp = mockMvc.perform(get("/api/orders/%s".formatted(orderId))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> itemIds = JsonPath.read(detailResp, "$.data.items[*].id");
        return itemIds.isEmpty() ? null : itemIds.get(0);
    }

    // ══════════════════════════════════════════════
    // Product review tests
    // ══════════════════════════════════════════════

    @Test
    @Order(1)
    void submitProductReviewSuccessAndUpdatesRating() throws Exception {
        String token = "mock-1002-USER";
        String orderResp = completeOfflineOrder(token, 3003);
        Number orderId = JsonPath.read(orderResp, "$.data.id");
        Number itemId = firstOrderItemId(token, orderId);

        mockMvc.perform(post("/api/reviews/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderItemId": %d, "score": 5, "content": "Excellent"}
                                """.formatted(itemId.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.score").value(5))
                .andExpect(jsonPath("$.data.productId").exists());

        mockMvc.perform(get("/api/products/3003/review-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.avgScore").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.data.reviewCount").value(greaterThan(0)));
    }

    @Test
    @Order(2)
    void submitProductReviewDuplicateFails() throws Exception {
        String token = "mock-1002-USER";
        // Re-use the completed order from test 1
        String ordersResp = mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> orderIds = JsonPath.read(ordersResp,
                "$.data[?(@.orderStatus == 'completed')].id");
        Number orderId = orderIds.get(0);
        Number itemId = firstOrderItemId(token, orderId);

        // Duplicate should fail (already reviewed in test 1)
        mockMvc.perform(post("/api/reviews/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderItemId": %d, "score": 3}
                                """.formatted(itemId.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    @Test
    @Order(3)
    void submitProductReviewInvalidScoreReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/reviews/products")
                        .header("Authorization", "Bearer mock-1001-USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderItemId\": 1, \"score\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(post("/api/reviews/products")
                        .header("Authorization", "Bearer mock-1001-USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderItemId\": 1, \"score\": 6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(4)
    void submitProductReviewUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(post("/api/reviews/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderItemId\": 1, \"score\": 5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    void submitProductReviewForWrongBuyerFails() throws Exception {
        // buyerToken created a completed order + review in test 1
        String buyerToken = "mock-1002-USER";
        String ordersResp = mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> orderIds = JsonPath.read(ordersResp,
                "$.data[?(@.orderStatus == 'completed')].id");
        Number orderId = orderIds.get(0);
        Number itemId = firstOrderItemId(buyerToken, orderId);

        // mock-1001-USER is NOT the buyer
        mockMvc.perform(post("/api/reviews/products")
                        .header("Authorization", "Bearer mock-1001-USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderItemId": %d, "score": 3}
                                """.formatted(itemId.longValue())))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    void pendingReviewItemsDisappearsAfterReview() throws Exception {
        String token = "mock-1002-USER";
        // We already reviewed the only item from tests 1-2, so pending should be empty or
        // contain only unreviewed items. Create a new order and verify the flow.
        String orderResp = completeOfflineOrder(token, 3003);
        Number orderId = JsonPath.read(orderResp, "$.data.id");
        Number itemId = firstOrderItemId(token, orderId);

        // Pending should now contain the new item
        String pendingResp = mockMvc.perform(get("/api/reviews/pending")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/reviews/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderItemId": %d, "score": 4}
                                """.formatted(itemId.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Item should no longer appear in pending
        mockMvc.perform(get("/api/reviews/pending")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == %d)]".formatted(itemId.longValue())).doesNotExist());
    }

    // ══════════════════════════════════════════════
    // Shop review tests
    // ══════════════════════════════════════════════

    @Test
    @Order(7)
    void submitShopReviewSuccess() throws Exception {
        String token = "mock-1003-USER";
        // Product 3002 has shopId=4002, needed for shop review
        String orderResp = completeOfflineOrder(token, 3002);
        Number orderId = JsonPath.read(orderResp, "$.data.id");

        String detail = mockMvc.perform(get("/api/orders/%s".formatted(orderId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Number shopId = JsonPath.read(detail, "$.data.shopId");

        mockMvc.perform(post("/api/reviews/shops")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId": %d, "score": 5, "content": "Great shop!"}
                                """.formatted(shopId.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.score").value(5));
    }

    @Test
    @Order(8)
    void submitShopReviewDuplicateFails() throws Exception {
        String token = "mock-1003-USER";
        String ordersResp = mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> orderIds = JsonPath.read(ordersResp,
                "$.data[?(@.orderStatus == 'completed')].id");
        Number orderId = orderIds.get(0);

        String detail = mockMvc.perform(get("/api/orders/%s".formatted(orderId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Number shopId = JsonPath.read(detail, "$.data.shopId");

        // Duplicate (already reviewed in test 7)
        mockMvc.perform(post("/api/reviews/shops")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId": %d, "score": 3}
                                """.formatted(shopId.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    @Test
    @Order(9)
    void submitShopReviewWithoutPurchaseFails() throws Exception {
        // mock-1007-USER has no orders from any shop
        mockMvc.perform(post("/api/reviews/shops")
                        .header("Authorization", "Bearer mock-1007-USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shopId\": 4001, \"score\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    // ══════════════════════════════════════════════
    // Public endpoints
    // ══════════════════════════════════════════════

    @Test
    @Order(10)
    void productReviewEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/api/products/3003/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());

        mockMvc.perform(get("/api/products/3003/review-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.avgScore").exists());
    }

    @Test
    @Order(11)
    void shopReviewEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/api/shops/4001/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());

        mockMvc.perform(get("/api/shops/4001/review-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.avgScore").exists());
    }

    @Test
    @Order(12)
    void getMyReviewsReturnsBothTypes() throws Exception {
        mockMvc.perform(get("/api/reviews/mine")
                        .header("Authorization", "Bearer mock-1002-USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productReviews").isArray())
                .andExpect(jsonPath("$.data.shopReviews").isArray());
    }

    // ══════════════════════════════════════════════
    // Direct lookup edge cases
    // ══════════════════════════════════════════════

    @Test
    @Order(13)
    void submitProductReviewNonexistentOrderItemReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/reviews/products")
                        .header("Authorization", "Bearer mock-1001-USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderItemId\": 99999, \"score\": 5}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @Order(14)
    void submitProductReviewForNonCompletedOrderFails() throws Exception {
        String token = "mock-1001-USER";
        // Create an order but don't complete it — leave it at pending_payment
        String cartResp = mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 3003, \"quantity\": 1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> cartIds = JsonPath.read(cartResp,
                "$.data.items[?(@.productId == 3003)].id");
        Number cartItemId = cartIds.get(0);

        String orderResp = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cartItemIds": [%s], "fulfillmentType": "offline",
                                 "offlineMeetTime": "2026-08-01 12:00",
                                 "offlineMeetLocation": "Library"}
                                """.formatted(cartItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("pending_payment"))
                .andReturn().getResponse().getContentAsString();
        Number orderId = JsonPath.read(orderResp, "$.data.id");

        // Get order item without completing the order
        String detailResp = mockMvc.perform(get("/api/orders/%s".formatted(orderId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> itemIds = JsonPath.read(detailResp, "$.data.items[*].id");
        Number itemId = itemIds.get(0);

        // Review should fail because order is not completed
        mockMvc.perform(post("/api/reviews/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderItemId": %d, "score": 4}
                                """.formatted(itemId.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }
}
