package com.campusmarket.backend.mapper.search;

import java.util.List;
import java.util.Map;

public interface SearchLogMapper {

    void insert(String keyword, String normalizedKeyword, Long userId, int resultCount);

    List<Map<String, Object>> findRecentDailyAggregates(int days);

    List<Map<String, Object>> findRecentDailyAggregatesByPrefix(String normalizedPrefix, int days);

    List<Map<String, Object>> findTopKeywordsForRecentWindow(int days);

    List<Map<String, Object>> findTopKeywordsForRecentWindowByPrefix(String normalizedPrefix, int days);

    List<Map<String, Object>> findPage(int offset, int limit);

    long countAll();
}
