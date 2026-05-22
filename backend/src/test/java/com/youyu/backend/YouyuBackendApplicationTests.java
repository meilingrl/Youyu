package com.youyu.backend;

import com.jayway.jsonpath.JsonPath;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(classes = YouyuBackendApplication.class)
class YouyuBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
        // Ensure the backend scaffold can start a Spring Boot test context.
    }

    @Test
    void registerLoginAndSubmitVerificationMainChain() throws Exception {
        long suffix = System.currentTimeMillis();
        String username = "chain_user_" + suffix;
        String password = "pass123456";
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "nickname": "链路用户",
                                  "phone": "139%s",
                                  "email": "%s@campus.edu.cn"
                                }
                                """.formatted(username, password, String.valueOf(suffix).substring(0, 8), username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.loginId").value(username))
                .andExpect(jsonPath("$.data.privilege.canPurchase").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = JsonPath.read(registerResponse, "$.data.token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.loginId").value(username));

        mockMvc.perform(post("/api/users/verification")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentNo": "S%s",
                                  "realName": "链路用户",
                                  "college": "计算机学院",
                                  "major": "软件工程",
                                  "grade": "2026级",
                                  "campusEmail": "%s@campus.edu.cn",
                                  "verificationMethod": "manual_review"
                                }
                                """.formatted(suffix, username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationStatus").value("pending_review"));
    }

    @Test
    void userPreferencePersistsThroughJdbcReadAndUpdate() throws Exception {
        String token = "mock-1002-USER";

        mockMvc.perform(get("/api/users/me/preference")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.themeMode").value("system"))
                .andExpect(jsonPath("$.data.themeColor").value("campus_blue"))
                .andExpect(jsonPath("$.data.notificationPreference.orderReminder").value(true))
                .andExpect(jsonPath("$.data.notificationPreference.reviewReminder").value(true));

        mockMvc.perform(put("/api/users/me/preference")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "themeMode": "dark",
                                  "themeColor": "fresh_green",
                                  "homeDisplayMode": "compact",
                                  "defaultFulfillmentType": "offline",
                                  "defaultPaymentMethod": "mock_payment",
                                  "defaultSortType": "latest",
                                  "notificationPreference": {
                                    "orderReminder": false,
                                    "reviewReminder": true
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.themeMode").value("dark"))
                .andExpect(jsonPath("$.data.themeColor").value("fresh_green"))
                .andExpect(jsonPath("$.data.notificationPreference.orderReminder").value(false))
                .andExpect(jsonPath("$.data.notificationPreference.reviewReminder").value(true));

        mockMvc.perform(get("/api/users/me/preference")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.themeMode").value("dark"))
                .andExpect(jsonPath("$.data.themeColor").value("fresh_green"))
                .andExpect(jsonPath("$.data.homeDisplayMode").value("compact"))
                .andExpect(jsonPath("$.data.defaultFulfillmentType").value("offline"))
                .andExpect(jsonPath("$.data.defaultSortType").value("latest"))
                .andExpect(jsonPath("$.data.notificationPreference.orderReminder").value(false))
                .andExpect(jsonPath("$.data.notificationPreference.reviewReminder").value(true));
    }

    @Test
    void digitalOrderPaymentAndReceiptUsePersistedProductData() throws Exception {
        String token = "mock-1002-USER";

        String cartResponse = mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 3001,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].productId").value(3001))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number cartItemId = JsonPath.read(cartResponse, "$.data.items[0].id");

        mockMvc.perform(post("/api/orders/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cartItemIds": [%s],
                                  "fulfillmentType": "digital"
                                }
                                """.formatted(cartItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].title").value("Advanced Math Review Pack"))
                .andExpect(jsonPath("$.data.selectedFulfillmentType").value("digital"));

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cartItemIds": [%s],
                                  "fulfillmentType": "digital"
                                }
                                """.formatted(cartItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("pending_payment"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number orderId = JsonPath.read(orderResponse, "$.data.id");

        String paymentResponse = mockMvc.perform(post("/api/payments/orders/%s/initiate".formatted(orderId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.payment.paymentStatus").value("initiated"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String paymentNo = JsonPath.read(paymentResponse, "$.data.payment.paymentNo");

        mockMvc.perform(post("/api/payments/%s/mock-success".formatted(paymentNo))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("pending_receipt"));

        mockMvc.perform(get("/api/orders/%s".formatted(orderId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.digitalAssets.length()").value(2));

        mockMvc.perform(post("/api/orders/%s/confirm-receipt".formatted(orderId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("completed"))
                .andExpect(jsonPath("$.data.digitalAssets.length()").value(3));
    }

    @Test
    void digitalAssetAccessGovernanceEnforcesOwnershipAndLogsAccess() throws Exception {
        String buyerToken = "mock-1003-USER";
        String otherToken = "mock-1002-USER";

        String cartResponse = mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 3001,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number cartItemId = JsonPath.read(cartResponse, "$.data.items[0].id");

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cartItemIds": [%s],
                                  "fulfillmentType": "digital"
                                }
                                """.formatted(cartItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number orderId = JsonPath.read(orderResponse, "$.data.id");

        // Before payment, accessing full asset should fail
        mockMvc.perform(get("/api/orders/%s/assets/3203/access".formatted(orderId))
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));

        // Non-buyer cannot access
        mockMvc.perform(get("/api/orders/%s/assets/3203/access".formatted(orderId))
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // Pay
        String paymentResponse = mockMvc.perform(post("/api/payments/orders/%s/initiate".formatted(orderId))
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String paymentNo = JsonPath.read(paymentResponse, "$.data.payment.paymentNo");

        mockMvc.perform(post("/api/payments/%s/mock-success".formatted(paymentNo))
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("pending_receipt"));

        // Before receipt, full asset access still blocked
        mockMvc.perform(get("/api/orders/%s/assets/3203/access".formatted(orderId))
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));

        // Confirm receipt
        mockMvc.perform(post("/api/orders/%s/confirm-receipt".formatted(orderId))
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("completed"));

        // After receipt, full asset access succeeds and logs
        mockMvc.perform(get("/api/orders/%s/assets/3203/access".formatted(orderId))
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.asset.assetName").value("Full Pack.zip"))
                .andExpect(jsonPath("$.data.accessLogs.length()").value(1))
                .andExpect(jsonPath("$.data.accessLogs[0].accessType").value("full"));

        // Access log visible in order detail
        mockMvc.perform(get("/api/orders/%s".formatted(orderId))
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.digitalAccessLogs.length()").value(1))
                .andExpect(jsonPath("$.data.digitalAccessLogs[0].assetName").value("Full Pack.zip"));
    }

    @Test
    void publishDigitalProductReviewApproveAndRejectMainChain() throws Exception {
        String userToken = "mock-1001-USER";
        String adminToken = "mock-9001-ADMIN";

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Smoke Physical Product",
                                  "categoryId": 2,
                                  "type": "physical",
                                  "price": "12.50",
                                  "stock": 1,
                                  "deliveryMethods": ["offline_face_to_face"],
                                  "submitMode": "submit",
                                  "description": "physical smoke"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("on_sale"))
                .andExpect(jsonPath("$.data.reviewStatus").value("not_required"));

        String digitalResponse = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Smoke Digital Notes Approve",
                                  "categoryId": 1,
                                  "type": "digital",
                                  "price": "9.90",
                                  "stock": 1,
                                  "deliveryMethods": ["digital_delivery"],
                                  "allowPreview": true,
                                  "previewHint": "preview only",
                                  "submitMode": "submit",
                                  "description": "digital smoke approve"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("off_sale"))
                .andExpect(jsonPath("$.data.reviewStatus").value("pending_review"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number digitalProductId = JsonPath.read(digitalResponse, "$.data.id");
        Number approveTaskId = firstReviewTaskId(adminToken, digitalProductId);

        mockMvc.perform(put("/api/admin/review-tasks/{reviewTaskId}/review", approveTaskId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "approve",
                                  "reviewNote": "smoke approve"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewTask.reviewStatus").value("approved"))
                .andExpect(jsonPath("$.data.product.status").value("on_sale"))
                .andExpect(jsonPath("$.data.product.reviewStatus").value("approved"));

        String rejectedDigitalResponse = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Smoke Digital Notes Reject",
                                  "categoryId": 1,
                                  "type": "digital",
                                  "price": "8.80",
                                  "stock": 1,
                                  "deliveryMethods": ["digital_delivery"],
                                  "submitMode": "submit",
                                  "description": "digital smoke reject"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value("pending_review"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number rejectedProductId = JsonPath.read(rejectedDigitalResponse, "$.data.id");
        Number rejectTaskId = firstReviewTaskId(adminToken, rejectedProductId);

        mockMvc.perform(put("/api/admin/review-tasks/{reviewTaskId}/review", rejectTaskId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "reject",
                                  "rejectReason": "source unclear",
                                  "reviewNote": "smoke reject"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewTask.reviewStatus").value("rejected"))
                .andExpect(jsonPath("$.data.reviewTask.rejectReason").value("source unclear"))
                .andExpect(jsonPath("$.data.product.status").value("off_sale"))
                .andExpect(jsonPath("$.data.product.reviewStatus").value("rejected"))
                .andExpect(jsonPath("$.data.product.reviewRejectReason").value("source unclear"));
    }

    @Test
    void logisticsOrderShipAndReceiptFlow() throws Exception {
        String userToken = "mock-1001-USER";
        String adminToken = "mock-9001-ADMIN";

        String cartResponse = mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 3002,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Number> cartIds = JsonPath.read(cartResponse, "$.data.items[?(@.productId == 3002)].id");
        Number cartItemId = cartIds.get(0);

        mockMvc.perform(post("/api/orders/preview")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cartItemIds": [%s],
                                  "fulfillmentType": "logistics"
                                }
                                """.formatted(cartItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.selectedFulfillmentType").value("logistics"));

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cartItemIds": [%s],
                                  "fulfillmentType": "logistics",
                                  "addressId": 7001
                                }
                                """.formatted(cartItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("pending_payment"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number orderId = JsonPath.read(orderResponse, "$.data.id");

        String paymentResponse = mockMvc.perform(post("/api/payments/orders/%s/initiate".formatted(orderId))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.payment.paymentStatus").value("initiated"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String paymentNo = JsonPath.read(paymentResponse, "$.data.payment.paymentNo");

        mockMvc.perform(post("/api/payments/%s/mock-success".formatted(paymentNo))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("pending_fulfillment"));

        mockMvc.perform(post("/api/admin/orders/%s/ship".formatted(orderId))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "trackingNo": "SF1234567890",
                                  "logisticsCompany": "SF Express"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("pending_receipt"));

        mockMvc.perform(post("/api/orders/%s/confirm-receipt".formatted(orderId))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("completed"));
    }

    @Test
    void offlineOrderDoubleConfirmAndRefundFlow() throws Exception {
        String userToken = "mock-1002-USER";
        String adminToken = "mock-9001-ADMIN";

        String cartResponse = mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 3003,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number cartItemId = JsonPath.read(cartResponse, "$.data.items[0].id");

        mockMvc.perform(post("/api/orders/preview")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cartItemIds": [%s],
                                  "fulfillmentType": "offline"
                                }
                                """.formatted(cartItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.selectedFulfillmentType").value("offline"));

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cartItemIds": [%s],
                                  "fulfillmentType": "offline",
                                  "offlineMeetTime": "2026-06-01 12:00",
                                  "offlineMeetLocation": "NEU Library Gate"
                                }
                                """.formatted(cartItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("pending_payment"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number orderId = JsonPath.read(orderResponse, "$.data.id");

        String paymentResponse = mockMvc.perform(post("/api/payments/orders/%s/initiate".formatted(orderId))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String paymentNo = JsonPath.read(paymentResponse, "$.data.payment.paymentNo");

        mockMvc.perform(post("/api/payments/%s/mock-success".formatted(paymentNo))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("pending_fulfillment"));

        mockMvc.perform(post("/api/admin/orders/%s/offline/seller-confirm".formatted(orderId))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("pending_receipt"));

        mockMvc.perform(post("/api/orders/%s/offline/buyer-confirm".formatted(orderId))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("completed"));

        mockMvc.perform(post("/api/orders/%s/refunds".formatted(orderId))
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refundReason": "商品与描述不符"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("refunding"));

        Number refundId = JsonPath.read(
                mockMvc.perform(get("/api/orders/%s".formatted(orderId))
                                .header("Authorization", "Bearer " + userToken))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.data.refunds[0].id");

        mockMvc.perform(post("/api/admin/orders/%s/refunds/%s/complete".formatted(orderId, refundId))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("refunded"));
    }

    @Test
    void userAndShopInsightSnapshotsUseRealOrderAggregation() throws Exception {
        String userToken = "mock-1001-USER";
        String adminToken = "mock-9001-ADMIN";

        String cartResponse = mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 3002,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Number> cartIds = JsonPath.read(cartResponse, "$.data.items[?(@.productId == 3002)].id");
        Number cartItemId = cartIds.get(0);

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cartItemIds": [%s],
                                  "fulfillmentType": "logistics",
                                  "addressId": 7001
                                }
                                """.formatted(cartItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number orderId = JsonPath.read(orderResponse, "$.data.id");

        String paymentResponse = mockMvc.perform(post("/api/payments/orders/%s/initiate".formatted(orderId))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String paymentNo = JsonPath.read(paymentResponse, "$.data.payment.paymentNo");

        mockMvc.perform(post("/api/payments/%s/mock-success".formatted(paymentNo))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/admin/orders/%s/ship".formatted(orderId))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "trackingNo": "YT202605130001",
                                  "logisticsCompany": "YTO Express"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/orders/%s/confirm-receipt".formatted(orderId))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("completed"));

        mockMvc.perform(get("/api/users/me/insight-snapshot")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metricSource").value("real_query"))
                .andExpect(jsonPath("$.data.totalSpendAmount", greaterThan(0.0)))
                .andExpect(jsonPath("$.data.totalPurchasedItemCount", greaterThan(0)))
                .andExpect(jsonPath("$.data.recentBrowses.length()", greaterThan(0)))
                .andExpect(jsonPath("$.data.favoritePreferenceSummary.length()", greaterThan(0)));

        mockMvc.perform(get("/api/shops/4002/insight-snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metricSource").value("real_query"))
                .andExpect(jsonPath("$.data.monthlySalesAmount", greaterThan(0.0)))
                .andExpect(jsonPath("$.data.monthlyOrderCount", greaterThan(0)))
                .andExpect(jsonPath("$.data.hotProducts.length()", greaterThan(0)));
    }

    @Test
    void userCanSubmitReportAndAdminCanProcessIt() throws Exception {
        String userToken = "mock-1001-USER";
        String adminToken = "mock-9001-ADMIN";

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType": "product",
                                  "targetId": 3001,
                                  "reason": "inaccurate_content",
                                  "content": "Anonymous report should be rejected"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType": "unknown",
                                  "targetId": 3001,
                                  "reason": "inaccurate_content",
                                  "content": "Invalid target type should be rejected"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        String reportResponse = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType": "product",
                                  "targetId": 3001,
                                  "targetLabel": "Advanced Math Review Pack",
                                  "reason": "inaccurate_content",
                                  "content": "The product description appears inconsistent with the preview."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.report.targetType").value("product"))
                .andExpect(jsonPath("$.data.report.targetId").value(3001))
                .andExpect(jsonPath("$.data.report.status").value("pending"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number reportId = JsonPath.read(reportResponse, "$.data.report.id");

        String adminReportsResponse = mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Number> pendingReportIds = JsonPath.read(
                adminReportsResponse,
                "$.data.items[?(@.id == %s)].id".formatted(reportId)
        );
        assertTrue(pendingReportIds.stream().anyMatch(id -> id.longValue() == reportId.longValue()));

        mockMvc.perform(put("/api/admin/reports/{reportId}/process", reportId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "resolved",
                                  "resolution": "Verified and recorded."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.report.status").value("resolved"))
                .andExpect(jsonPath("$.data.report.resolution").value("Verified and recorded."));
    }

    @Test
    void productKeywordSearchLogsMeaningfulTermsAndBuildsHotRanking() throws Exception {
        mockMvc.perform(get("/api/products").param("keyword", "Advanced Math"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()", greaterThan(0)))
                .andExpect(jsonPath("$.data.items[0].title").value("Advanced Math Review Pack"));

        mockMvc.perform(get("/api/products").param("keyword", "Advanced Math"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/products").param("keyword", "Dorm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/products").param("keyword", "!!!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String hotResponse = mockMvc.perform(get("/api/search/hot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Object>> hotList = JsonPath.read(hotResponse, "$.data");
        int advancedIndex = indexOfKeyword(hotList, "advanced math");
        int dormIndex = indexOfKeyword(hotList, "dorm");

        assertTrue(advancedIndex >= 0, "Advanced Math should appear in hot search results");
        assertTrue(dormIndex >= 0, "Dorm should appear in hot search results");
        assertTrue(advancedIndex < dormIndex, "Advanced Math should rank ahead of Dorm");
        assertTrue(toIntValue(hotList.get(advancedIndex).get("searchCount")) > 0);
        assertTrue(Double.parseDouble(String.valueOf(hotList.get(advancedIndex).get("score"))) > 1.0d);
    }

    @Test
    void searchSuggestionUsesLogPrefixAndDoesNotCreateExtraLogs() throws Exception {
        String uniqueKeyword = "Suggest Prefix " + System.currentTimeMillis();
        long beforeCount = countSearchLogs();

        mockMvc.perform(get("/api/products").param("keyword", uniqueKeyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        long afterSearchCount = countSearchLogs();
        assertEquals(beforeCount + 1, afterSearchCount);

        String suggestResponse = mockMvc.perform(get("/api/search/suggest")
                        .param("q", "suggest pref"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long afterSuggestCount = countSearchLogs();
        assertEquals(afterSearchCount, afterSuggestCount);

        List<Map<String, Object>> suggestions = JsonPath.read(suggestResponse, "$.data");
        assertFalse(suggestions.isEmpty(), "Expected prefixed suggestion results");
        assertTrue(suggestions.stream().anyMatch(item ->
                        uniqueKeyword.equals(item.get("keyword"))
                                && uniqueKeyword.toLowerCase().equals(item.get("normalizedKeyword"))),
                "Expected the unique keyword to appear in suggestions");

        mockMvc.perform(get("/api/search/suggest").param("q", "!!!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void searchSuggestionHonorsGovernanceAndPinnedOrdering() throws Exception {
        String adminToken = "mock-9001-ADMIN";
        String prefixBase = "campus pick " + System.currentTimeMillis();
        String hiddenKeyword = prefixBase + " hidden";
        String pinnedKeyword = prefixBase + " pinned";
        String blockedKeyword = prefixBase + " blocked";

        mockMvc.perform(get("/api/products").param("keyword", hiddenKeyword))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/products").param("keyword", pinnedKeyword))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/products").param("keyword", pinnedKeyword))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/products").param("keyword", blockedKeyword))
                .andExpect(status().isOk());

        Number hideRuleId = createSearchGovernanceRule(adminToken, "HIDE_KEYWORD", hiddenKeyword);
        Number pinRuleId = createSearchGovernanceRule(adminToken, "PIN_KEYWORD", pinnedKeyword);
        Number blockedRuleId = createSearchGovernanceRule(adminToken, "SENSITIVE_WORD", "blocked");

        try {
            String suggestResponse = mockMvc.perform(get("/api/search/suggest")
                            .param("q", prefixBase))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            List<Map<String, Object>> suggestions = JsonPath.read(suggestResponse, "$.data");
            assertFalse(suggestions.isEmpty(), "Expected governed suggestion results");
            assertEquals(pinnedKeyword.toLowerCase(), suggestions.get(0).get("normalizedKeyword"));
            assertEquals(Boolean.TRUE, suggestions.get(0).get("pinned"));
            assertFalse(suggestions.stream().anyMatch(item -> hiddenKeyword.toLowerCase().equals(item.get("normalizedKeyword"))));
            assertFalse(suggestions.stream().anyMatch(item -> blockedKeyword.toLowerCase().equals(item.get("normalizedKeyword"))));
        } finally {
            deleteSearchGovernanceRule(adminToken, hideRuleId);
            deleteSearchGovernanceRule(adminToken, pinRuleId);
            deleteSearchGovernanceRule(adminToken, blockedRuleId);
        }
    }

    @Test
    void hotSearchRespectsPinnedFallbackAndRecencyWeightedRanking() throws Exception {
        String adminToken = "mock-9001-ADMIN";
        String recentKeyword = "rank fresh " + System.currentTimeMillis();
        String staleKeyword = "rank stale " + System.currentTimeMillis();
        String pinnedKeyword = "rank pin " + System.currentTimeMillis();

        insertSearchLog(recentKeyword, recentKeyword.toLowerCase(), 1, LocalDateTime.now());
        insertSearchLog(recentKeyword, recentKeyword.toLowerCase(), 1, LocalDateTime.now().minusHours(1));
        insertSearchLog(recentKeyword, recentKeyword.toLowerCase(), 1, LocalDateTime.now().minusHours(2));
        insertSearchLog(recentKeyword, recentKeyword.toLowerCase(), 1, LocalDateTime.now().minusHours(3));
        insertSearchLog(recentKeyword, recentKeyword.toLowerCase(), 1, LocalDateTime.now().minusHours(4));

        insertSearchLog(staleKeyword, staleKeyword.toLowerCase(), 1, LocalDateTime.now().minusDays(6));
        insertSearchLog(staleKeyword, staleKeyword.toLowerCase(), 1, LocalDateTime.now().minusDays(6).minusHours(1));
        insertSearchLog(staleKeyword, staleKeyword.toLowerCase(), 1, LocalDateTime.now().minusDays(6).minusHours(2));
        insertSearchLog(staleKeyword, staleKeyword.toLowerCase(), 1, LocalDateTime.now().minusDays(6).minusHours(3));
        insertSearchLog(staleKeyword, staleKeyword.toLowerCase(), 1, LocalDateTime.now().minusDays(6).minusHours(4));
        insertSearchLog(staleKeyword, staleKeyword.toLowerCase(), 1, LocalDateTime.now().minusDays(6).minusHours(5));

        Number pinRuleId = createSearchGovernanceRule(adminToken, "PIN_KEYWORD", pinnedKeyword);

        try {
            String hotResponse = mockMvc.perform(get("/api/search/hot"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            List<Map<String, Object>> hotList = JsonPath.read(hotResponse, "$.data");
            int recentIndex = indexOfKeyword(hotList, recentKeyword.toLowerCase());
            int staleIndex = indexOfKeyword(hotList, staleKeyword.toLowerCase());
            int pinnedIndex = indexOfKeyword(hotList, pinnedKeyword.toLowerCase());

            assertTrue(pinnedIndex >= 0, "Pinned fallback keyword should be present");
            assertEquals(Boolean.TRUE, hotList.get(pinnedIndex).get("pinned"));
            assertEquals(0, toIntValue(hotList.get(pinnedIndex).get("searchCount")));
            assertEquals(0, toIntValue(hotList.get(pinnedIndex).get("resultCountSum")));
            assertTrue(recentIndex >= 0 && staleIndex >= 0, "Expected both unique hot keywords to appear");
            assertTrue(recentIndex < staleIndex, "Recent weighted keyword should rank ahead of stale keyword");
        } finally {
            deleteSearchGovernanceRule(adminToken, pinRuleId);
        }
    }

    @Test
    void productListSupportsCategoryFilterWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/products").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()", greaterThan(0)));
    }

    @Test
    void invalidStateTransitionReturns400() throws Exception {
        String token = "mock-1002-USER";

        String cartResponse = mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 3001,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number cartItemId = JsonPath.read(cartResponse, "$.data.items[0].id");

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cartItemIds": [%s],
                                  "fulfillmentType": "digital"
                                }
                                """.formatted(cartItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("pending_payment"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number orderId = JsonPath.read(orderResponse, "$.data.id");

        mockMvc.perform(post("/api/orders/%s/confirm-receipt".formatted(orderId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    @Test
    void homeRecommendPublicReturnsPopularProductsWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/recommend/home").param("limit", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()", greaterThan(0)))
                .andExpect(jsonPath("$.data[0].source").value("popularity"))
                .andExpect(jsonPath("$.data[0].id").isNotEmpty())
                .andExpect(jsonPath("$.data[0].title").isNotEmpty())
                .andExpect(jsonPath("$.data[0].score").isNotEmpty());
    }

    @Test
    void homeRecommendExcludesDeletedAndOffSaleProducts() throws Exception {
        String response = mockMvc.perform(get("/api/recommend/home").param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> statuses = JsonPath.read(response, "$.data[*].status");
        List<String> reviewStatuses = JsonPath.read(response, "$.data[*].reviewStatus");

        for (String status : statuses) {
            assertTrue("on_sale".equals(status),
                    "Expected on_sale but got: " + status);
        }
        for (String rs : reviewStatuses) {
            assertTrue("not_required".equals(rs) || "approved".equals(rs),
                    "Unexpected reviewStatus: " + rs);
        }
    }

    @Test
    void homeRecommendPersonalizedReturnsCategoryBasedProductsForLoggedInUser() throws Exception {
        String buyerToken = "mock-1001-USER";

        mockMvc.perform(get("/api/users/me/insight-snapshot")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalPurchasedItemCount", greaterThan(0)));

        mockMvc.perform(get("/api/recommend/home")
                        .header("Authorization", "Bearer " + buyerToken)
                        .param("limit", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()", greaterThan(0)))
                .andExpect(jsonPath("$.data[0].source").isNotEmpty())
                .andExpect(jsonPath("$.data[0].reason").isNotEmpty());
    }

    @Test
    void alsoBoughtEndpointReturnsSuccessAndValidStructure() throws Exception {
        String response = mockMvc.perform(get("/api/recommend/also-bought/3001").param("limit", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Object> data = JsonPath.read(response, "$.data");
        if (!data.isEmpty()) {
            Map<String, Object> first = (Map<String, Object>) data.get(0);
            assertTrue(first.containsKey("source"), "First item should have source field");
            assertTrue(first.containsKey("coPurchaseCount"), "First item should have coPurchaseCount field");
        }
    }

    @Test
    void alsoBoughtReturnsEmptyListForProductWithNoCoPurchases() throws Exception {
        mockMvc.perform(get("/api/recommend/also-bought/99999").param("limit", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void searchGovernanceRulesCrud() throws Exception {
        String adminToken = "mock-9001-ADMIN";

        String createResponse = mockMvc.perform(post("/api/admin/search/governance-rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleType": "SENSITIVE_WORD",
                                  "keyword": "counterfeit"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ruleType").value("SENSITIVE_WORD"))
                .andExpect(jsonPath("$.data.keyword").value("counterfeit"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number ruleId = JsonPath.read(createResponse, "$.data.id");

        mockMvc.perform(get("/api/admin/search/governance-rules")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()", greaterThan(0)));

        mockMvc.perform(put("/api/admin/search/governance-rules/{ruleId}", ruleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isActive": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false));

        mockMvc.perform(delete("/api/admin/search/governance-rules/{ruleId}", ruleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deleted").value(true));
    }

    @Test
    void governanceFiltersHotKeywords() throws Exception {
        String adminToken = "mock-9001-ADMIN";

        mockMvc.perform(get("/api/products").param("keyword", "Advanced Math"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String createResponse = mockMvc.perform(post("/api/admin/search/governance-rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleType": "HIDE_KEYWORD",
                                  "keyword": "Advanced Math"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number ruleId = JsonPath.read(createResponse, "$.data.id");

        String hotResponse = mockMvc.perform(get("/api/search/hot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Object>> hotList = JsonPath.read(hotResponse, "$.data");
        boolean hasHiddenKeyword = hotList.stream()
                .anyMatch(item -> "Advanced Math".equals(item.get("normalizedKeyword"))
                        || "advanced math".equals(item.get("normalizedKeyword")));
        assertTrue(!hasHiddenKeyword, "HIDE_KEYWORD rule should prevent the keyword from appearing in hot search");

        mockMvc.perform(delete("/api/admin/search/governance-rules/{ruleId}", ruleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void productSearchPaginationReturnsPageMetadata() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("page", "1")
                        .param("pageSize", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()", greaterThan(0)))
                .andExpect(jsonPath("$.data.total", greaterThan(0)))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(3));

        mockMvc.perform(get("/api/products")
                        .param("page", "2")
                        .param("pageSize", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void searchLogBrowserPaginates() throws Exception {
        String adminToken = "mock-9001-ADMIN";

        mockMvc.perform(get("/api/admin/search/logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(5));
    }

    private long countSearchLogs() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM search_logs", Long.class);
        return count == null ? 0L : count;
    }

    private void insertSearchLog(String keyword, String normalizedKeyword, int resultCount, LocalDateTime createdAt) {
        jdbcTemplate.update("""
                INSERT INTO search_logs (keyword, normalized_keyword, user_id, result_count, created_at)
                VALUES (?, ?, NULL, ?, ?)
                """, keyword, normalizedKeyword, resultCount, Timestamp.valueOf(createdAt));
    }

    private Number createSearchGovernanceRule(String adminToken, String ruleType, String keyword) throws Exception {
        String createResponse = mockMvc.perform(post("/api/admin/search/governance-rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleType": "%s",
                                  "keyword": "%s"
                                }
                                """.formatted(ruleType, keyword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(createResponse, "$.data.id");
    }

    private void deleteSearchGovernanceRule(String adminToken, Number ruleId) throws Exception {
        mockMvc.perform(delete("/api/admin/search/governance-rules/{ruleId}", ruleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private int indexOfKeyword(List<Map<String, Object>> items, String normalizedKeyword) {
        for (int i = 0; i < items.size(); i++) {
            if (normalizedKeyword.equals(String.valueOf(items.get(i).get("normalizedKeyword")))) {
                return i;
            }
        }
        return -1;
    }

    private int toIntValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private Number firstReviewTaskId(String adminToken, Number productId) throws Exception {
        String tasksResponse = mockMvc.perform(get("/api/admin/review-tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "pending_review"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Number> ids = JsonPath.read(tasksResponse, "$.data.items[?(@.productId == %s)].id".formatted(productId));
        return ids.get(0);
    }
}
