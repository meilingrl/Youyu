package com.youyu.backend.service.product.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youyu.backend.common.cache.RedisCacheSupport;
import com.youyu.backend.config.RedisCacheProperties;
import com.youyu.backend.mapper.recommend.RecommendMapper;
import com.youyu.backend.mapper.user.UserMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RecommendServiceCacheTest {

    private final RecommendMapper recommendMapper = mock(RecommendMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final RedisCacheProperties cacheProperties = new RedisCacheProperties();
    private final RecordingCacheSupport cacheSupport = new RecordingCacheSupport(cacheProperties);
    private final RecommendServiceImpl service =
            new RecommendServiceImpl(recommendMapper, userMapper, cacheSupport, cacheProperties);

    @Test
    void homeRecommendReturnsCachedAnonymousResultWithoutDatabaseQuery() {
        List<Map<String, Object>> cached = List.of(Map.of("id", 3001L, "source", "popularity"));
        cacheSupport.cached.put("recommend:home:anonymous:4", cached);

        List<Map<String, Object>> result = service.recommendForHome(4, null);

        assertEquals(cached, result);
        verify(recommendMapper, never()).findPopularProducts(4);
    }

    @Test
    void homeRecommendStoresDatabaseResultOnCacheMiss() {
        List<Map<String, Object>> loaded = List.of(Map.of("id", 3002L, "source", "popularity"));
        cacheProperties.setHomeRecommendTtl(Duration.ofMinutes(3));
        when(recommendMapper.findPopularProducts(3)).thenReturn(loaded);

        List<Map<String, Object>> result = service.recommendForHome(3, null);

        assertEquals(loaded, result);
        assertEquals(loaded, cacheSupport.cached.get("recommend:home:anonymous:3"));
        assertEquals(Duration.ofMinutes(3), cacheSupport.ttls.get("recommend:home:anonymous:3"));
    }

    @Test
    void invalidateRecommendationCachesClearsRecommendationPrefix() {
        service.invalidateRecommendationCaches();

        assertEquals("recommend:", cacheSupport.evictedPrefix);
        assertEquals(null, cacheSupport.evictedKey);
    }

    private static class RecordingCacheSupport extends RedisCacheSupport {
        private final Map<String, List<Map<String, Object>>> cached = new HashMap<>();
        private final Map<String, Duration> ttls = new HashMap<>();
        private String evictedKey;
        private String evictedPrefix;

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

        @Override
        public void evictByPrefix(String prefix) {
            evictedPrefix = prefix;
        }
    }
}
