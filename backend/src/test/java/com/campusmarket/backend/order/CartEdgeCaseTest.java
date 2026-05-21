package com.campusmarket.backend.order;

import com.campusmarket.backend.BackendTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartEdgeCaseTest extends BackendTestBase {

    private static final String USER = "mock-1002-USER";

    @Test
    void addCartItemQuantityMergesOnDuplicate() throws Exception {
        Number cartItemId = addToCart(USER, 3001, 1);

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + USER)
                        .contentType("application/json")
                        .content("{\"productId\": 3001, \"quantity\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test
    void updateCartItemQuantity() throws Exception {
        Number cartItemId = addToCart(USER, 3001, 1);

        mockMvc.perform(patch("/api/cart/items/{cartItemId}", cartItemId)
                        .header("Authorization", "Bearer " + USER)
                        .contentType("application/json")
                        .content("{\"quantity\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String response = mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        int quantity = JsonPath.read(response, "$.data.items[0].quantity");
        org.junit.jupiter.api.Assertions.assertEquals(3, quantity);
    }

    @Test
    void removeCartItemSuccess() throws Exception {
        Number cartItemId = addToCart(USER, 3001, 1);

        mockMvc.perform(delete("/api/cart/items/{cartItemId}", cartItemId)
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.removed").value(true));
    }

    @Test
    void removeCartItemNotFound() throws Exception {
        mockMvc.perform(delete("/api/cart/items/99999")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isNotFound());
    }

    @Test
    void emptyCartForNewUser() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer mock-1007-USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void cartEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/cart/items")
                        .contentType("application/json")
                        .content("{\"productId\": 3001, \"quantity\": 1}"))
                .andExpect(status().isUnauthorized());
    }
}
