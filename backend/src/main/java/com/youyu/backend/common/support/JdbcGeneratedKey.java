package com.youyu.backend.common.support;

import java.util.Locale;
import java.util.Map;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.support.KeyHolder;

public final class JdbcGeneratedKey {

    private JdbcGeneratedKey() {
    }

    public static Long requiredLong(KeyHolder keyHolder, String label) {
        Long directKey = directKey(keyHolder);
        if (directKey != null) {
            return directKey;
        }

        for (Map<String, Object> keys : keyHolder.getKeyList()) {
            Object id = first(keys, "ID");
            if (id instanceof Number number) {
                return number.longValue();
            }
            if (id != null) {
                return Long.parseLong(String.valueOf(id));
            }
        }

        throw new IllegalStateException("Failed to read generated " + label);
    }

    private static Long directKey(KeyHolder keyHolder) {
        try {
            Number key = keyHolder.getKey();
            return key == null ? null : key.longValue();
        } catch (InvalidDataAccessApiUsageException ignored) {
            return null;
        }
    }

    private static Object first(Map<String, Object> keys, String key) {
        Object value = keys.get(key);
        return value == null ? keys.get(key.toLowerCase(Locale.ROOT)) : value;
    }
}
