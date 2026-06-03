package com.youyu.backend.product;

import com.youyu.backend.BackendTestBase;
import com.youyu.backend.service.search.ProductSearchCriteria;
import com.youyu.backend.service.search.ProductSearchIndex;
import com.youyu.backend.service.search.ProductSearchResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductSearchIntegrationTest extends BackendTestBase {

    @MockBean
    private ProductSearchIndex productSearchIndex;

    @BeforeEach
    void setUpSearchFixtures() {
        upsertProduct(92001L, "SearchAlpha Backpack", BigDecimal.valueOf(19.90));
        upsertProduct(92002L, "SearchAlpha Calculator", BigDecimal.valueOf(9.90));
    }

    @Test
    void productListFallsBackToMysqlWhenSearchIndexUnavailable() throws Exception {
        when(productSearchIndex.search(ArgumentMatchers.any(ProductSearchCriteria.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/products")
                        .param("keyword", "SearchAlpha")
                        .param("sort", "price_asc")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(92002))
                .andExpect(jsonPath("$.data.items[1].id").value(92001))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(2))
                .andExpect(jsonPath("$.data.sort").value("price_asc"));
    }

    @Test
    void productListHydratesMysqlCardsInSearchResultOrder() throws Exception {
        when(productSearchIndex.search(ArgumentMatchers.any(ProductSearchCriteria.class)))
                .thenReturn(Optional.of(new ProductSearchResult(List.of(92001L, 92002L), 2)));

        mockMvc.perform(get("/api/products")
                        .param("keyword", "intentional typo")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(92001))
                .andExpect(jsonPath("$.data.items[0].title").value("SearchAlpha Backpack"))
                .andExpect(jsonPath("$.data.items[1].id").value(92002))
                .andExpect(jsonPath("$.data.items[1].title").value("SearchAlpha Calculator"))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    private void upsertProduct(Long productId, String title, BigDecimal salePrice) {
        jdbcTemplate.update("""
                DELETE FROM products
                WHERE id = ?
                """, productId);
        jdbcTemplate.update("""
                INSERT INTO products (
                    id, seller_user_id, shop_id, category_id, title, subtitle, description,
                    detail_content, product_type, status, review_status, main_image_url,
                    sale_price, original_price, stock_quantity, supports_logistics,
                    supports_offline_delivery, supports_digital_delivery, allow_preview,
                    preview_rule_text, view_count, favorite_count, is_deleted, updated_at
                ) VALUES (
                    ?, 1001, 4001, 1, ?, '', 'SearchAlpha public product fixture',
                    'SearchAlpha public product fixture', 'physical', 'on_sale', 'not_required',
                    'https://example.com/search-fixture.png', ?, NULL, 9, TRUE, TRUE, FALSE,
                    FALSE, '', 0, 0, FALSE, CURRENT_TIMESTAMP
                )
                """, productId, title, salePrice);
    }
}
