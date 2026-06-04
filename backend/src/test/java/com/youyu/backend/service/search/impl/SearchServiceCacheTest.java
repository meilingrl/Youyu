package com.youyu.backend.service.search.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youyu.backend.common.cache.RedisCacheSupport;
import com.youyu.backend.config.RedisCacheProperties;
import com.youyu.backend.mapper.product.ProductMapper;
import com.youyu.backend.mapper.search.SearchGovernanceMapper;
import com.youyu.backend.mapper.search.SearchLogMapper;
import com.youyu.backend.service.search.ProductSearchIndex;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SearchServiceCacheTest {

    private final SearchLogMapper searchLogMapper = mock(SearchLogMapper.class);
    private final SearchGovernanceMapper governanceMapper = mock(SearchGovernanceMapper.class);
    private final ProductMapper productMapper = mock(ProductMapper.class);
    private final ProductSearchIndex productSearchIndex = mock(ProductSearchIndex.class);
    private final RedisCacheProperties cacheProperties = new RedisCacheProperties();
    private final RecordingCacheSupport cacheSupport = new RecordingCacheSupport(cacheProperties);
    private final SearchServiceImpl service = new SearchServiceImpl(
            searchLogMapper,
            governanceMapper,
            productMapper,
            productSearchIndex,
            cacheSupport,
            cacheProperties
    );

    @Test
    void hotKeywordsReturnCachedResultWithoutAggregateQueries() {
        List<Map<String, Object>> cached = List.of(Map.of("normalizedKeyword", "math", "searchCount", 12));
        cacheSupport.cached.put("search:hot", cached);

        List<Map<String, Object>> result = service.listHotKeywords();

        assertEquals(cached, result);
        verify(searchLogMapper, never()).findRecentDailyAggregates(7);
        verify(governanceMapper, never()).findAllActive();
    }

    @Test
    void hotKeywordsStoreComputedResultOnCacheMiss() {
        cacheProperties.setHotSearchTtl(Duration.ofMinutes(5));
        when(governanceMapper.findAllActive()).thenReturn(List.of());
        when(searchLogMapper.findRecentDailyAggregates(7)).thenReturn(List.of());
        when(searchLogMapper.findTopKeywordsForRecentWindow(7)).thenReturn(List.of());

        List<Map<String, Object>> result = service.listHotKeywords();

        assertEquals(List.of(), result);
        assertEquals(List.of(), cacheSupport.cached.get("search:hot"));
        assertEquals(Duration.ofMinutes(5), cacheSupport.ttls.get("search:hot"));
    }

    @Test
    void governanceMutationEvictsHotKeywordCache() {
        Map<String, Object> insertedRule = new LinkedHashMap<>();
        insertedRule.put("ruleType", "PIN_KEYWORD");
        insertedRule.put("keyword", "math");
        insertedRule.put("displayLabel", null);
        when(governanceMapper.insert(insertedRule)).thenReturn(88L);
        when(governanceMapper.findById(88L)).thenReturn(Optional.of(Map.of("id", 88L)));

        service.createGovernanceRule(Map.of("ruleType", "PIN_KEYWORD", "keyword", "math"));

        assertEquals("search:hot", cacheSupport.evictedKey);
    }

    private static class RecordingCacheSupport extends RedisCacheSupport {
        private final Map<String, List<Map<String, Object>>> cached = new HashMap<>();
        private final Map<String, Duration> ttls = new HashMap<>();
        private String evictedKey;

        RecordingCacheSupport(RedisCacheProperties properties) {
            super(properties, null, new ObjectMapper());
        }

        @Override
        public Optional<List<Map<String, Object>>> getList(String key) {
            return Optional.ofNullable(cached.get(key));
        }

        @Override
        public void putList(String key, List<Map<String, Object>> value, Duration ttl) {
            cached.put(key, value);
            ttls.put(key, ttl);
        }

        @Override
        public void evict(String key) {
            evictedKey = key;
        }
    }
}
