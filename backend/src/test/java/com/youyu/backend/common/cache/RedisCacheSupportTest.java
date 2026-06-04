package com.youyu.backend.common.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youyu.backend.config.RedisCacheProperties;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

@SuppressWarnings("deprecation")
class RedisCacheSupportTest {

    private final RedisCacheProperties properties = new RedisCacheProperties();
    private final RecordingStringRedisTemplate redisTemplate = new RecordingStringRedisTemplate();
    private final RedisCacheSupport cacheSupport =
            new RedisCacheSupport(properties, redisTemplate, new ObjectMapper());

    @Test
    void evictionIsNoOpWhenCacheDisabled() {
        cacheSupport.evict("search:hot");
        cacheSupport.evictByPrefix("recommend:");

        assertFalse(redisTemplate.deleteKeyCalled);
        assertFalse(redisTemplate.deleteBatchCalled);
        assertFalse(redisTemplate.scanCalled);
        assertFalse(redisTemplate.keysCalled);
    }

    @Test
    void exactEvictDeletesNamespacedKeyWhenCacheEnabled() {
        properties.setEnabled(true);

        cacheSupport.evict("search:hot");

        assertEquals("youyu:cache:search:hot", redisTemplate.deletedKey);
    }

    @Test
    void prefixEvictUsesScanInsteadOfKeys() {
        properties.setEnabled(true);
        redisTemplate.scanKeys = List.of(
                "youyu:cache:recommend:home:anonymous:4",
                "youyu:cache:recommend:also-bought:42:4"
        );

        cacheSupport.evictByPrefix("recommend:");

        assertTrue(redisTemplate.scanCalled);
        assertFalse(redisTemplate.keysCalled);
        assertEquals("youyu:cache:recommend:*", redisTemplate.scanOptions.getPattern());
        assertEquals(List.of(List.of(
                "youyu:cache:recommend:home:anonymous:4",
                "youyu:cache:recommend:also-bought:42:4"
        )), redisTemplate.deletedBatches);
        assertTrue(redisTemplate.lastCursorClosed);
    }

    @Test
    void prefixEvictDegradesWhenRedisScanFails() {
        properties.setEnabled(true);
        redisTemplate.scanFailure = new IllegalStateException("redis unavailable");

        cacheSupport.evictByPrefix("recommend:");

        assertTrue(redisTemplate.scanCalled);
        assertFalse(redisTemplate.keysCalled);
    }

    private static class RecordingStringRedisTemplate extends StringRedisTemplate {
        private boolean deleteKeyCalled;
        private boolean deleteBatchCalled;
        private boolean scanCalled;
        private boolean keysCalled;
        private boolean lastCursorClosed;
        private String deletedKey;
        private ScanOptions scanOptions;
        private List<String> scanKeys = List.of();
        private RuntimeException scanFailure;
        private final List<List<String>> deletedBatches = new ArrayList<>();

        @Override
        public Boolean delete(String key) {
            deleteKeyCalled = true;
            deletedKey = key;
            return true;
        }

        @Override
        public Long delete(Collection<String> keys) {
            deleteBatchCalled = true;
            deletedBatches.add(List.copyOf(keys));
            return (long) keys.size();
        }

        @Override
        @SuppressWarnings("deprecation")
        public Set<String> keys(String pattern) {
            keysCalled = true;
            return Set.of();
        }

        @Override
        public Cursor<String> scan(ScanOptions options) {
            scanCalled = true;
            scanOptions = options;
            if (scanFailure != null) {
                throw scanFailure;
            }
            return new RecordingCursor(scanKeys, () -> lastCursorClosed = true);
        }
    }

    private static class RecordingCursor implements Cursor<String> {
        private final Iterator<String> iterator;
        private final Runnable closeCallback;
        private boolean closed;
        private long position;

        RecordingCursor(List<String> keys, Runnable closeCallback) {
            this.iterator = keys.iterator();
            this.closeCallback = closeCallback;
        }

        @Override
        public CursorId getId() {
            return CursorId.of(0);
        }

        @Override
        public long getCursorId() {
            return 0;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public long getPosition() {
            return position;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public String next() {
            if (!iterator.hasNext()) {
                throw new NoSuchElementException();
            }
            position++;
            return iterator.next();
        }

        @Override
        public void close() {
            closed = true;
            closeCallback.run();
        }
    }
}
