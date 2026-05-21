package com.campusmarket.backend.payment;

import com.campusmarket.backend.BackendTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentEdgeCaseTest extends BackendTestBase {

    private static final String USER = "mock-1002-USER";

    @Test
    void initiatePaymentOnNonExistentOrder() throws Exception {
        mockMvc.perform(post("/api/payments/orders/99999/initiate")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isNotFound());
    }

    @Test
    void initiatePaymentOnOtherUsersOrder() throws Exception {
        Number cartItemId = addToCart(USER, 3001, 1);
        Number orderId = createOrder(USER, cartItemId, "digital", null);

        mockMvc.perform(post("/api/payments/orders/%s/initiate".formatted(orderId))
                        .header("Authorization", "Bearer mock-1001-USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void doublePaymentInitiation() throws Exception {
        Number cartItemId = addToCart(USER, 3001, 1);
        Number orderId = createOrder(USER, cartItemId, "digital", null);

        initiatePayment(USER, orderId);

        mockMvc.perform(post("/api/payments/orders/%s/initiate".formatted(orderId))
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void mockSuccessOnNonExistentPayment() throws Exception {
        mockMvc.perform(post("/api/payments/NONEXISTENT/mock-success")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isNotFound());
    }

    @Test
    void paymentGatewayReturnsAvailableMethods() throws Exception {
        mockMvc.perform(get("/api/payments/gateway")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    void paymentEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/payments/gateway"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/payments/orders/1/initiate"))
                .andExpect(status().isUnauthorized());
    }
}
