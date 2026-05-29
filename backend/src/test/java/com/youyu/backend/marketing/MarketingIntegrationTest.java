package com.youyu.backend.marketing;

import com.jayway.jsonpath.JsonPath;
import com.youyu.backend.BackendTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MarketingIntegrationTest extends BackendTestBase {

    private static final String OWNER = "mock-1001-USER";
    private static final String OTHER_OWNER = "mock-1004-USER";
    private static final String BUYER = "mock-1011-USER";
    private static final String SECOND_BUYER = "mock-1012-USER";
    private static final String REVIEWER = "mock-9103-REVIEWER";
    private static final String OPERATOR = "mock-9104-OPERATOR";

    @Test
    void couponRequiresReviewBeforeClaimAndCanBeUsedOnceInOrder() throws Exception {
        Number couponId = createCoupon("THRESHOLD", "25.00", "5.00", 3);

        mockMvc.perform(get("/api/marketing/coupons/available")
                        .header("Authorization", "Bearer " + BUYER)
                        .param("shopId", "4001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == %s)]".formatted(couponId), hasSize(0)));

        approveCoupon(couponId);

        Number userCouponId = claimCoupon(BUYER, couponId);
        Number cartItemId = addProductToCart(1011L, BUYER, 3001L, 2);

        mockMvc.perform(post("/api/orders/preview")
                        .header("Authorization", "Bearer " + BUYER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cartItemIds": [%s],
                                  "fulfillmentType": "digital",
                                  "userCouponId": %s
                                }
                                """.formatted(cartItemId, userCouponId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.couponDiscountAmount").value(5.00))
                .andExpect(jsonPath("$.data.payableAmount").value(34.80))
                .andExpect(jsonPath("$.data.appliedCoupon.userCouponId").value(userCouponId.intValue()))
                .andExpect(jsonPath("$.data.availableCoupons", hasSize(greaterThanOrEqualTo(1))));

        String orderResponse = createOrderAndReturnJson(
                BUYER,
                cartItemId,
                "digital",
                "\"userCouponId\": %s".formatted(userCouponId)
        );
        Number orderId = JsonPath.read(orderResponse, "$.data.id");

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .header("Authorization", "Bearer " + BUYER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.discountAmount").value(5.00))
                .andExpect(jsonPath("$.data.payableAmount").value(34.80))
                .andExpect(jsonPath("$.data.appliedCoupon.userCouponId").value(userCouponId.intValue()))
                .andExpect(jsonPath("$.data.appliedCoupon.discountAmount").value(5.00));

        Number secondCartItemId = addProductToCart(1011L, BUYER, 3001L, 1);
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + BUYER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cartItemIds": [%s],
                                  "fulfillmentType": "digital",
                                  "userCouponId": %s
                                }
                                """.formatted(secondCartItemId, userCouponId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    @Test
    void duplicateClaimAndOverClaimAreRejectedWithoutOverstatingClaimedQuantity() throws Exception {
        Number couponId = createCoupon("FIXED", "0.00", "3.00", 1);
        approveCoupon(couponId);

        claimCoupon(BUYER, couponId);

        mockMvc.perform(post("/api/marketing/coupons/{couponId}/claim", couponId)
                        .header("Authorization", "Bearer " + BUYER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));

        mockMvc.perform(post("/api/marketing/coupons/{couponId}/claim", couponId)
                        .header("Authorization", "Bearer " + SECOND_BUYER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));

        Integer claimedQuantity = jdbcTemplate.queryForObject(
                "SELECT claimed_quantity FROM marketing_coupons WHERE id = ?",
                Integer.class,
                couponId
        );
        org.assertj.core.api.Assertions.assertThat(claimedQuantity).isEqualTo(1);
    }

    @Test
    void ownerBoundaryAndAdminMarketingPermissionAreEnforced() throws Exception {
        Number couponId = createCoupon("FIXED", "0.00", "2.00", 2);

        mockMvc.perform(put("/api/marketing/owner/coupons/{couponId}/status", couponId)
                        .header("Authorization", "Bearer " + OTHER_OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"disabled\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/admin/marketing/coupons")
                        .header("Authorization", "Bearer " + REVIEWER)
                        .param("reviewStatus", "pending_review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/admin/marketing/coupons")
                        .header("Authorization", "Bearer " + OPERATOR))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void activityRequiresReviewBeforePublicDisplay() throws Exception {
        Number activityId = createActivity();

        mockMvc.perform(get("/api/marketing/shops/4001/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == %s)]".formatted(activityId), hasSize(0)));

        mockMvc.perform(put("/api/admin/marketing/activities/{activityId}/review", activityId)
                        .header("Authorization", "Bearer " + REVIEWER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"approve\",\"reviewNote\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviewStatus").value("approved"));

        mockMvc.perform(get("/api/marketing/shops/4001/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == %s)]".formatted(activityId), hasSize(1)));
    }

    private Number createCoupon(String couponType, String minimumSpendAmount, String discountAmount, int totalQuantity) throws Exception {
        String response = mockMvc.perform(post("/api/marketing/owner/coupons")
                        .header("Authorization", "Bearer " + OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Marketing Test Coupon",
                                  "description": "integration test coupon",
                                  "couponType": "%s",
                                  "minimumSpendAmount": %s,
                                  "discountAmount": %s,
                                  "totalQuantity": %d,
                                  "startAt": "2020-01-01T00:00:00",
                                  "endAt": "2099-01-01T00:00:00"
                                }
                                """.formatted(couponType, minimumSpendAmount, discountAmount, totalQuantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviewStatus").value("pending_review"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.data.id");
    }

    private void approveCoupon(Number couponId) throws Exception {
        mockMvc.perform(put("/api/admin/marketing/coupons/{couponId}/review", couponId)
                        .header("Authorization", "Bearer " + REVIEWER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"approve\",\"reviewNote\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviewStatus").value("approved"));
    }

    private Number claimCoupon(String token, Number couponId) throws Exception {
        String response = mockMvc.perform(post("/api/marketing/coupons/{couponId}/claim", couponId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.data.userCouponId");
    }

    private Number addProductToCart(Long userId, String token, Long productId, int quantity) throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": %d, \"quantity\": %d}".formatted(productId, quantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cart_items WHERE user_id = ? AND product_id = ?",
                Number.class,
                userId,
                productId
        );
    }

    private Number createActivity() throws Exception {
        String response = mockMvc.perform(post("/api/marketing/owner/activities")
                        .header("Authorization", "Bearer " + OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Marketing Test Activity",
                                  "description": "review gated activity",
                                  "startAt": "2020-01-01T00:00:00",
                                  "endAt": "2099-01-01T00:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviewStatus").value("pending_review"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.data.id");
    }
}
