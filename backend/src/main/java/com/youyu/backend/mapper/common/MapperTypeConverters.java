package com.youyu.backend.mapper.common;

import java.util.Map;

public final class MapperTypeConverters {

    private MapperTypeConverters() {}

    public static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public static int toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            String text = String.valueOf(value).trim();
            if (text.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public static long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value == null) return 0L;
        return Long.parseLong(String.valueOf(value));
    }

    public static double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        if (value == null) return 0.0;
        return Double.parseDouble(String.valueOf(value));
    }

    /** Try camelCase first, then fall back to UPPER_SNAKE (H2/MySQL compatibility). */
    public static Object first(Map<String, Object> row, String camelKey, String upperKey) {
        Object value = row.get(camelKey);
        return value == null ? row.get(upperKey) : value;
    }

    /** Direct lookup by UPPER_SNAKE key. */
    public static Object first(Map<String, Object> row, String upperKey) {
        return row.get(upperKey);
    }
}
