package com.youyu.backend.service.search;

import java.util.List;
import java.util.Map;

public interface SearchService {

    void recordKeywordSearch(String keyword, Long userId, int resultCount);

    java.util.Optional<ProductSearchResult> searchProducts(ProductSearchCriteria criteria);

    void syncProductSearchDocument(Map<String, Object> productDocument);

    void removeProductSearchDocument(Long productId);

    Map<String, Object> reindexProductSearch();

    List<Map<String, Object>> listHotKeywords();

    List<Map<String, Object>> suggestKeywords(String query, int limit);

    List<Map<String, Object>> listGovernanceRules();

    Map<String, Object> createGovernanceRule(Map<String, Object> command);

    Map<String, Object> updateGovernanceRule(Long id, Map<String, Object> command);

    void deleteGovernanceRule(Long id);

    Map<String, Object> listSearchLogs(int page, int pageSize);
}
