package com.youyu.backend.mapper.search.impl;

import com.youyu.backend.mapper.search.SearchLogMapper;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Primary
@Component
public class JdbcSearchLogMapper implements SearchLogMapper {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSearchLogMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insert(String keyword, String normalizedKeyword, Long userId, int resultCount) {
        jdbcTemplate.update("""
                INSERT INTO search_logs (keyword, normalized_keyword, user_id, result_count)
                VALUES (?, ?, ?, ?)
                """, keyword, normalizedKeyword, userId, resultCount);
    }

    @Override
    public List<Map<String, Object>> findRecentDailyAggregates(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return jdbcTemplate.queryForList("""
                SELECT normalized_keyword, DATE(created_at) AS search_date,
                       COUNT(*) AS search_count, SUM(result_count) AS result_count_sum
                FROM search_logs
                WHERE created_at >= ?
                GROUP BY normalized_keyword, DATE(created_at)
                ORDER BY search_date DESC, normalized_keyword ASC
                """, Timestamp.valueOf(since)).stream().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("normalizedKeyword", first(row, "normalized_keyword", "NORMALIZED_KEYWORD"));
            map.put("searchDate", first(row, "search_date", "SEARCH_DATE"));
            map.put("searchCount", first(row, "search_count", "SEARCH_COUNT"));
            map.put("resultCountSum", first(row, "result_count_sum", "RESULT_COUNT_SUM"));
            return map;
        }).toList();
    }

    @Override
    public List<Map<String, Object>> findRecentDailyAggregatesByPrefix(String normalizedPrefix, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        String prefix = normalizedPrefix + "%";
        return jdbcTemplate.queryForList("""
                SELECT normalized_keyword, DATE(created_at) AS search_date,
                       COUNT(*) AS search_count, SUM(result_count) AS result_count_sum
                FROM search_logs
                WHERE created_at >= ?
                  AND normalized_keyword LIKE ?
                GROUP BY normalized_keyword, DATE(created_at)
                ORDER BY search_date DESC, normalized_keyword ASC
                """, Timestamp.valueOf(since), prefix).stream().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("normalizedKeyword", first(row, "normalized_keyword", "NORMALIZED_KEYWORD"));
            map.put("searchDate", first(row, "search_date", "SEARCH_DATE"));
            map.put("searchCount", first(row, "search_count", "SEARCH_COUNT"));
            map.put("resultCountSum", first(row, "result_count_sum", "RESULT_COUNT_SUM"));
            return map;
        }).toList();
    }

    @Override
    public List<Map<String, Object>> findTopKeywordsForRecentWindow(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return findTopKeywords("""
                SELECT normalized_keyword, keyword
                FROM (
                    SELECT normalized_keyword,
                           keyword,
                           COUNT(*) AS keyword_search_count,
                           MAX(created_at) AS last_seen_at,
                           ROW_NUMBER() OVER (
                               PARTITION BY normalized_keyword
                               ORDER BY COUNT(*) DESC, MAX(created_at) DESC, keyword ASC
                           ) AS row_num
                    FROM search_logs
                    WHERE created_at >= ?
                    GROUP BY normalized_keyword, keyword
                ) ranked_keywords
                WHERE row_num = 1
                ORDER BY normalized_keyword ASC
                """, Timestamp.valueOf(since));
    }

    @Override
    public List<Map<String, Object>> findTopKeywordsForRecentWindowByPrefix(String normalizedPrefix, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        String prefix = normalizedPrefix + "%";
        return findTopKeywords("""
                SELECT normalized_keyword, keyword
                FROM (
                    SELECT normalized_keyword,
                           keyword,
                           COUNT(*) AS keyword_search_count,
                           MAX(created_at) AS last_seen_at,
                           ROW_NUMBER() OVER (
                               PARTITION BY normalized_keyword
                               ORDER BY COUNT(*) DESC, MAX(created_at) DESC, keyword ASC
                           ) AS row_num
                    FROM search_logs
                    WHERE created_at >= ?
                      AND normalized_keyword LIKE ?
                    GROUP BY normalized_keyword, keyword
                ) ranked_keywords
                WHERE row_num = 1
                ORDER BY normalized_keyword ASC
                """, Timestamp.valueOf(since), prefix);
    }

    @Override
    public List<Map<String, Object>> findPage(int offset, int limit) {
        return jdbcTemplate.queryForList("""
                SELECT id, keyword, normalized_keyword, user_id, result_count, created_at
                FROM search_logs
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """, limit, offset).stream().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", first(row, "id", "ID"));
            map.put("keyword", first(row, "keyword", "KEYWORD"));
            map.put("normalizedKeyword", first(row, "normalized_keyword", "NORMALIZED_KEYWORD"));
            map.put("userId", first(row, "user_id", "USER_ID"));
            map.put("resultCount", first(row, "result_count", "RESULT_COUNT"));
            map.put("createdAt", first(row, "created_at", "CREATED_AT"));
            return map;
        }).toList();
    }

    @Override
    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM search_logs", Long.class);
        return count == null ? 0L : count;
    }

    private List<Map<String, Object>> findTopKeywords(String sql, Object... args) {
        return jdbcTemplate.queryForList(sql, args).stream().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("normalizedKeyword", first(row, "normalized_keyword", "NORMALIZED_KEYWORD"));
            map.put("keyword", first(row, "keyword", "KEYWORD"));
            return map;
        }).toList();
    }

    private Object first(Map<String, Object> row, String camelKey, String upperKey) {
        Object value = row.get(camelKey);
        return value == null ? row.get(upperKey) : value;
    }
}
