package com.youyu.backend.mapper.chat.impl;

import com.youyu.backend.mapper.chat.SupportConsoleMapper;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcSupportConsoleMapper implements SupportConsoleMapper {

    private final JdbcTemplate jdbcTemplate;
    private Boolean unreadColumnsAvailable;
    private Boolean messageTypeColumnAvailable;
    private Boolean messageRecallColumnAvailable;

    public JdbcSupportConsoleMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> findQueue(String filter, Long adminId, int offset, int limit) {
        List<Object> params = new ArrayList<>();
        String where = buildFilterClause(filter, adminId, params);
        String sql = """
            SELECT cc.id, cc.type, cc.support_status, cc.assigned_admin_id,
                   %s, cc.last_message_at, cc.created_at,
                   ua.id AS requester_id, ua.username AS requester_username,
                   ua.nickname AS requester_nickname, ua.avatar AS requester_avatar,
                   adm.id AS admin_id, adm.username AS admin_username, adm.nickname AS admin_nickname,
                   %s AS last_body,
                   %s AS last_type,
                   %s AS last_recalled
            FROM chat_conversations cc
            LEFT JOIN users ua ON cc.user_a_id = ua.id
            LEFT JOIN users adm ON cc.assigned_admin_id = adm.id
            WHERE (cc.type = 'support'
                   OR (cc.type = 'direct' AND EXISTS (
                       SELECT 1 FROM users cs WHERE cs.id = cc.user_b_id AND cs.username = 'platform_cs'
                   )))
              AND %s
            ORDER BY cc.last_message_at DESC, cc.id DESC
            LIMIT ? OFFSET ?
            """.formatted(unreadSelect(), lastBodySelect(), lastTypeSelect(), lastRecalledSelect(), where);
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.queryForList(sql, params.toArray()).stream()
                .map(this::normalizeQueueRow)
                .toList();
    }

    @Override
    public int countQueue(String filter, Long adminId) {
        List<Object> params = new ArrayList<>();
        String where = buildFilterClause(filter, adminId, params);
        String sql = """
            SELECT COUNT(*) FROM chat_conversations cc
            WHERE (cc.type = 'support'
                   OR (cc.type = 'direct' AND EXISTS (
                       SELECT 1 FROM users cs WHERE cs.id = cc.user_b_id AND cs.username = 'platform_cs'
                   )))
              AND %s
            """.formatted(where);
        Long count = jdbcTemplate.queryForObject(sql, Long.class, params.toArray());
        return count == null ? 0 : count.intValue();
    }

    @Override
    public Map<String, Object> countByStatus(Long adminId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pending", countQueue("pending", adminId));
        result.put("active", countQueue("active", adminId));
        result.put("mine", countQueue("mine", adminId));
        result.put("closed", countQueue("closed", adminId));
        return result;
    }

    private String unreadSelect() {
        return unreadColumnsAvailable()
                ? "cc.unread_count_b"
                : "0 AS unread_count_b";
    }

    private String lastBodySelect() {
        return """
            (SELECT m.body FROM chat_messages m WHERE m.conversation_id = cc.id
                ORDER BY m.created_at DESC, m.id DESC LIMIT 1)
            """;
    }

    private String lastTypeSelect() {
        if (messageTypeColumnAvailable()) {
            return """
                (SELECT m.message_type FROM chat_messages m WHERE m.conversation_id = cc.id
                    ORDER BY m.created_at DESC, m.id DESC LIMIT 1)
                """;
        }
        return "'text'";
    }

    private String lastRecalledSelect() {
        if (messageRecallColumnAvailable()) {
            return """
                (SELECT m.is_recalled FROM chat_messages m WHERE m.conversation_id = cc.id
                    ORDER BY m.created_at DESC, m.id DESC LIMIT 1)
                """;
        }
        return "FALSE";
    }

    private String buildFilterClause(String filter, Long adminId, List<Object> params) {
        String normalized = filter == null ? "" : filter.trim().toLowerCase();
        return switch (normalized) {
            case "pending" -> "cc.support_status = 'pending'";
            case "active" -> "cc.support_status = 'human'";
            case "closed" -> "cc.support_status = 'closed'";
            case "mine" -> {
                params.add(adminId);
                yield "cc.assigned_admin_id = ? AND cc.support_status IN ('pending', 'human')";
            }
            default -> "cc.support_status IS NOT NULL";
        };
    }

    private Map<String, Object> normalizeQueueRow(Map<String, Object> raw) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", raw.get("id"));
        row.put("type", raw.get("type"));
        row.put("supportStatus", raw.get("support_status"));
        row.put("assignedAdminId", raw.get("assigned_admin_id"));
        row.put("unreadCount", raw.get("unread_count_b"));
        row.put("lastMessageAt", raw.get("last_message_at"));
        row.put("createdAt", raw.get("created_at"));
        row.put("lastMessagePreview", previewFor(raw));

        Map<String, Object> requester = new LinkedHashMap<>();
        requester.put("id", raw.get("requester_id"));
        requester.put("username", raw.get("requester_username"));
        requester.put("nickname", raw.get("requester_nickname"));
        requester.put("avatar", raw.get("requester_avatar"));
        row.put("requester", requester);

        if (raw.get("admin_id") != null) {
            Map<String, Object> admin = new LinkedHashMap<>();
            admin.put("id", raw.get("admin_id"));
            admin.put("username", raw.get("admin_username"));
            admin.put("nickname", raw.get("admin_nickname"));
            row.put("assignedAdmin", admin);
        } else {
            row.put("assignedAdmin", null);
        }
        return row;
    }

    private String previewFor(Map<String, Object> raw) {
        Object recalled = raw.get("last_recalled");
        if (recalled instanceof Boolean bool && bool) {
            return "消息已撤回";
        }
        if (recalled instanceof Number number && number.intValue() != 0) {
            return "消息已撤回";
        }
        String type = String.valueOf(raw.getOrDefault("last_type", "text"));
        return switch (type) {
            case "image" -> "[图片]";
            case "product_card" -> "[商品卡片]";
            case "order_card" -> "[订单卡片]";
            default -> {
                Object body = raw.get("last_body");
                String text = body == null ? "" : body.toString();
                yield text.isBlank() ? "开始对话" : text;
            }
        };
    }

    private boolean unreadColumnsAvailable() {
        if (unreadColumnsAvailable == null) {
            unreadColumnsAvailable = columnExists("chat_conversations", "unread_count_b");
        }
        return unreadColumnsAvailable;
    }

    private boolean messageTypeColumnAvailable() {
        if (messageTypeColumnAvailable == null) {
            messageTypeColumnAvailable = columnExists("chat_messages", "message_type");
        }
        return messageTypeColumnAvailable;
    }

    private boolean messageRecallColumnAvailable() {
        if (messageRecallColumnAvailable == null) {
            messageRecallColumnAvailable = columnExists("chat_messages", "is_recalled");
        }
        return messageRecallColumnAvailable;
    }

    private boolean columnExists(String tableName, String columnName) {
        return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) conn -> {
            try (ResultSet upper = conn.getMetaData().getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
                if (upper.next()) {
                    return true;
                }
            }
            try (ResultSet lower = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
                return lower.next();
            }
        }));
    }
}
