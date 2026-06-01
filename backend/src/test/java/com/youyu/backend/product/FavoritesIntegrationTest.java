package com.youyu.backend.product;

import com.youyu.backend.BackendTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FavoritesIntegrationTest extends BackendTestBase {

    @Test
    void addListAndRemoveFavoritesFollowFrozenContract() throws Exception {
        String token = "mock-1001-USER";
        long beforeCount = favoriteCountOf(3001L);

        mockMvc.perform(post("/api/favorites")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 3001
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(3001))
                .andExpect(jsonPath("$.data.favorite").value(true));

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(3001));

        assertEquals(beforeCount + 1, favoriteCountOf(3001L));

        mockMvc.perform(delete("/api/favorites/{productId}", 3001)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(3001))
                .andExpect(jsonPath("$.data.favorite").value(false));

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));

        assertEquals(beforeCount, favoriteCountOf(3001L));
    }

    @Test
    void addAndRemoveFavoritesAreIdempotent() throws Exception {
        String token = "mock-1002-USER";
        long beforeCount = favoriteCountOf(3002L);

        mockMvc.perform(post("/api/favorites")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 3002
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/favorites")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 3002
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorite").value(true));

        assertEquals(beforeCount + 1, favoriteCountOf(3002L));
        assertEquals(1L, favoriteRowCount(1002L, 3002L));

        mockMvc.perform(delete("/api/favorites/{productId}", 3002)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/favorites/{productId}", 3002)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorite").value(false));

        assertEquals(beforeCount, favoriteCountOf(3002L));
        assertEquals(0L, favoriteRowCount(1002L, 3002L));
    }

    @Test
    void favoritesRequireVisibleProductAndAuthentication() throws Exception {
        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/favorites")
                        .header("Authorization", "Bearer mock-1001-USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 99999
                                }
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/favorites")
                        .header("Authorization", "Bearer mock-1001-USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "oops"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    private long favoriteCountOf(long productId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT favorite_count FROM products WHERE id = ?",
                Long.class,
                productId
        );
        return count == null ? 0L : count;
    }

    private long favoriteRowCount(long userId, long productId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_favorites WHERE user_id = ? AND product_id = ?",
                Long.class,
                userId,
                productId
        );
        return count == null ? 0L : count;
    }
}
