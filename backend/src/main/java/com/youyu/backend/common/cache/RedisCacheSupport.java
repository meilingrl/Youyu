package com.youyu.backend.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youyu.backend.config.RedisCacheProperties;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisCacheSupport {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheSupport.class);
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<>() {
    };

    private final RedisCacheProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheSupport(RedisCacheProperties properties,
                             StringRedisTemplate redisTemplate,
                             ObjectMapper objectMapper) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public Optional<List<Map<String, Object>>> getList(String key) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        String namespacedKey = namespacedKey(key);
        try {
            String value = redisTemplate.opsForValue().get(namespacedKey);
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, LIST_OF_MAPS));
        } catch (RuntimeException ex) {
            log.warn("Redis cache read failed", ex);
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Redis cache payload is unreadable", ex);
            return Optional.empty();
        }
    }

    public void putList(String key, List<Map<String, Object>> value, Duration ttl) {
        if (!isEnabled() || value == null || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        String namespacedKey = namespacedKey(key);
        try {
            redisTemplate.opsForValue().set(namespacedKey, objectMapper.writeValueAsString(value), ttl);
        } catch (RuntimeException ex) {
            log.warn("Redis cache write failed", ex);
        } catch (Exception ex) {
            log.warn("Redis cache payload could not be serialized", ex);
        }
    }

    public void evict(String key) {
        if (!isEnabled()) {
            return;
        }
        String namespacedKey = namespacedKey(key);
        try {
            redisTemplate.delete(namespacedKey);
        } catch (RuntimeException ex) {
            log.warn("Redis cache eviction failed", ex);
        }
    }

    public void evictByPrefix(String prefix) {
        if (!isEnabled()) {
            return;
        }
        String namespacedPrefix = namespacedKey(prefix);
        try {
            Set<String> keys = redisTemplate.keys(namespacedPrefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (RuntimeException ex) {
            log.warn("Redis cache prefix eviction failed for prefix={}", namespacedPrefix, ex);
        }
    }

    private String namespacedKey(String key) {
        return properties.getKeyPrefix() + key;
    }
}
