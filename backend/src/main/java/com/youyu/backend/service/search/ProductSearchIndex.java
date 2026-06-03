package com.youyu.backend.service.search;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProductSearchIndex {

    Optional<ProductSearchResult> search(ProductSearchCriteria criteria);

    void indexProduct(Map<String, Object> productDocument);

    void deleteProduct(Long productId);

    Map<String, Object> reindexProducts(List<Map<String, Object>> productDocuments);
}
