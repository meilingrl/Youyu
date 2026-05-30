package com.youyu.backend.mapper.chat.impl;

import com.youyu.backend.common.support.JdbcGeneratedKey;
import com.youyu.backend.mapper.chat.ChatConversationMapper;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

@Primary
@Component
public class JdbcChatConversationMapper implements ChatConversationMapper {

    private final JdbcTemplate jdbcTemplate;
    private Boolean unreadColumnsAvailable;
    private Boolean managementColumnsAvailable;
    private Boolean autoReplyColumnsAvailable;
    private Boolean supportColumnsAvailable;

    public JdbcChatConversationMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, Object> findById(Long id) {
        String unreadSelect = unreadColumnsAvailable()
                ? "cc.unread_count_a, cc.unread_count_b,"
                : "0 as unread_count_a, 0 as unread_count_b,";
        String managementSelect = managementColumnsAvailable()
                ? "cc.is_pinned_a, cc.is_pinned_b, cc.is_muted_a, cc.is_muted_b, cc.deleted_by_a_at, cc.deleted_by_b_at,"
                : "FALSE as is_pinned_a, FALSE as is_pinned_b, FALSE as is_muted_a, FALSE as is_muted_b, NULL as deleted_by_a_at, NULL as deleted_by_b_at,";
        String autoReplySelect = autoReplyColumnsAvailable()
                ? "cc.auto_replied_to_a_at, cc.auto_replied_to_b_at,"
                : "NULL as auto_replied_to_a_at, NULL as auto_replied_to_b_at,";
        String supportSelect = supportColumnsAvailable()
                ? "cc.support_status, cc.assigned_admin_id,"
                : "NULL as support_status, NULL as assigned_admin_id,";
        String sql = """
            SELECT cc.id, cc.type, cc.product_id, cc.shop_id, cc.user_a_id, cc.user_b_id,
                   %s
                   %s
                   %s
                   %s
                   cc.last_message_at, cc.created_at,
                   ua.id as user_a_id, ua.username as user_a_username, ua.nickname as user_a_nickname, ua.avatar as user_a_avatar,
                   ub.id as user_b_id, ub.username as user_b_username, ub.nickname as user_b_nickname, ub.avatar as user_b_avatar
            FROM chat_conversations cc
            LEFT JOIN users ua ON cc.user_a_id = ua.id
            LEFT JOIN users ub ON cc.user_b_id = ub.id
            WHERE cc.id = ?
            """.formatted(unreadSelect, managementSelect, autoReplySelect, supportSelect);
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, id);
        return results.isEmpty() ? null : normalizeConversation(results.get(0));
    }

    @Override
    public List<Map<String, Object>> findByUserId(Long userId, int offset, int limit) {
        String unreadSelect = unreadColumnsAvailable()
                ? "cc.unread_count_a, cc.unread_count_b,"
                : "0 as unread_count_a, 0 as unread_count_b,";
        String managementSelect = managementColumnsAvailable()
                ? "cc.is_pinned_a, cc.is_pinned_b, cc.is_muted_a, cc.is_muted_b, cc.deleted_by_a_at, cc.deleted_by_b_at,"
                : "FALSE as is_pinned_a, FALSE as is_pinned_b, FALSE as is_muted_a, FALSE as is_muted_b, NULL as deleted_by_a_at, NULL as deleted_by_b_at,";
        String autoReplySelect = autoReplyColumnsAvailable()
                ? "cc.auto_replied_to_a_at, cc.auto_replied_to_b_at,"
                : "NULL as auto_replied_to_a_at, NULL as auto_replied_to_b_at,";
        String supportSelect = supportColumnsAvailable()
                ? "cc.support_status, cc.assigned_admin_id,"
                : "NULL as support_status, NULL as assigned_admin_id,";
        String whereClause = managementColumnsAvailable()
                ? "WHERE (cc.user_a_id = ? AND cc.deleted_by_a_at IS NULL) OR (cc.user_b_id = ? AND cc.deleted_by_b_at IS NULL)"
                : "WHERE cc.user_a_id = ? OR cc.user_b_id = ?";
        String orderClause = managementColumnsAvailable()
                ? "ORDER BY CASE WHEN cc.user_a_id = ? THEN cc.is_pinned_a WHEN cc.user_b_id = ? THEN cc.is_pinned_b ELSE FALSE END DESC, cc.last_message_at DESC"
                : "ORDER BY cc.last_message_at DESC";
        String sql = """
            SELECT cc.id, cc.type, cc.product_id, cc.shop_id, cc.user_a_id, cc.user_b_id,
                   %s
                   %s
                   %s
                   %s
                   cc.last_message_at, cc.created_at,
                   ua.id as user_a_id, ua.username as user_a_username, ua.nickname as user_a_nickname, ua.avatar as user_a_avatar,
                   ub.id as user_b_id, ub.username as user_b_username, ub.nickname as user_b_nickname, ub.avatar as user_b_avatar
            FROM chat_conversations cc
            LEFT JOIN users ua ON cc.user_a_id = ua.id
            LEFT JOIN users ub ON cc.user_b_id = ub.id
            %s
            %s
            LIMIT ? OFFSET ?
            """.formatted(unreadSelect, managementSelect, autoReplySelect, supportSelect, whereClause, orderClause);
        Object[] params = managementColumnsAvailable()
                ? new Object[] {userId, userId, userId, userId, limit, offset}
                : new Object[] {userId, userId, limit, offset};
        return jdbcTemplate.queryForList(sql, params).stream()
                .map(this::normalizeConversation)
                .toList();
    }

    @Override
    public int countByUserId(Long userId) {
        String sql = managementColumnsAvailable()
                ? """
                    SELECT COUNT(*) FROM chat_conversations
                    WHERE (user_a_id = ? AND deleted_by_a_at IS NULL)
                       OR (user_b_id = ? AND deleted_by_b_at IS NULL)
                    """
                : "SELECT COUNT(*) FROM chat_conversations WHERE user_a_id = ? OR user_b_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, userId, userId);
        return count == null ? 0 : count.intValue();
    }

    @Override
    public Map<String, Object> findByParticipants(Long userAId, Long userBId, Long productId, Long shopId) {
        String unreadSelect = unreadColumnsAvailable()
                ? "cc.unread_count_a, cc.unread_count_b,"
                : "0 as unread_count_a, 0 as unread_count_b,";
        String managementSelect = managementColumnsAvailable()
                ? "cc.is_pinned_a, cc.is_pinned_b, cc.is_muted_a, cc.is_muted_b, cc.deleted_by_a_at, cc.deleted_by_b_at,"
                : "FALSE as is_pinned_a, FALSE as is_pinned_b, FALSE as is_muted_a, FALSE as is_muted_b, NULL as deleted_by_a_at, NULL as deleted_by_b_at,";
        String autoReplySelect = autoReplyColumnsAvailable()
                ? "cc.auto_replied_to_a_at, cc.auto_replied_to_b_at,"
                : "NULL as auto_replied_to_a_at, NULL as auto_replied_to_b_at,";
        String supportSelect = supportColumnsAvailable()
                ? "cc.support_status, cc.assigned_admin_id,"
                : "NULL as support_status, NULL as assigned_admin_id,";
        StringBuilder sql = new StringBuilder("""
            SELECT cc.id, cc.type, cc.product_id, cc.shop_id, cc.user_a_id, cc.user_b_id,
                   %s
                   %s
                   %s
                   %s
                   cc.last_message_at, cc.created_at,
                   ua.id as user_a_id, ua.username as user_a_username, ua.nickname as user_a_nickname, ua.avatar as user_a_avatar,
                   ub.id as user_b_id, ub.username as user_b_username, ub.nickname as user_b_nickname, ub.avatar as user_b_avatar
            FROM chat_conversations cc
            LEFT JOIN users ua ON cc.user_a_id = ua.id
            LEFT JOIN users ub ON cc.user_b_id = ub.id
            WHERE cc.user_a_id = ? AND cc.user_b_id = ?
            """.formatted(unreadSelect, managementSelect, autoReplySelect, supportSelect));

        List<Object> params = new ArrayList<>();
        params.add(userAId);
        params.add(userBId);

        if (productId == null) {
            sql.append(" AND cc.product_id IS NULL");
        } else {
            sql.append(" AND cc.product_id = ?");
            params.add(productId);
        }

        if (shopId == null) {
            sql.append(" AND cc.shop_id IS NULL");
        } else {
            sql.append(" AND cc.shop_id = ?");
            params.add(shopId);
        }

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return results.isEmpty() ? null : normalizeConversation(results.get(0));
    }

    @Override
    public Map<String, Object> findSupportByRequesterAndCs(Long requesterUserId, Long csUserId) {
        String unreadSelect = unreadColumnsAvailable()
                ? "cc.unread_count_a, cc.unread_count_b,"
                : "0 as unread_count_a, 0 as unread_count_b,";
        String managementSelect = managementColumnsAvailable()
                ? "cc.is_pinned_a, cc.is_pinned_b, cc.is_muted_a, cc.is_muted_b, cc.deleted_by_a_at, cc.deleted_by_b_at,"
                : "FALSE as is_pinned_a, FALSE as is_pinned_b, FALSE as is_muted_a, FALSE as is_muted_b, NULL as deleted_by_a_at, NULL as deleted_by_b_at,";
        String autoReplySelect = autoReplyColumnsAvailable()
                ? "cc.auto_replied_to_a_at, cc.auto_replied_to_b_at,"
                : "NULL as auto_replied_to_a_at, NULL as auto_replied_to_b_at,";
        String supportSelect = supportColumnsAvailable()
                ? "cc.support_status, cc.assigned_admin_id,"
                : "NULL as support_status, NULL as assigned_admin_id,";
        String sql = """
            SELECT cc.id, cc.type, cc.product_id, cc.shop_id, cc.user_a_id, cc.user_b_id,
                   %s
                   %s
                   %s
                   %s
                   cc.last_message_at, cc.created_at,
                   ua.id as user_a_id, ua.username as user_a_username, ua.nickname as user_a_nickname, ua.avatar as user_a_avatar,
                   ub.id as user_b_id, ub.username as user_b_username, ub.nickname as user_b_nickname, ub.avatar as user_b_avatar
            FROM chat_conversations cc
            LEFT JOIN users ua ON cc.user_a_id = ua.id
            LEFT JOIN users ub ON cc.user_b_id = ub.id
            WHERE cc.type = 'support'
              AND cc.user_a_id = ?
              AND cc.user_b_id = ?
              AND cc.product_id IS NULL
              AND cc.shop_id IS NULL
            LIMIT 1
            """.formatted(unreadSelect, managementSelect, autoReplySelect, supportSelect);
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, requesterUserId, csUserId);
        return results.isEmpty() ? null : normalizeConversation(results.get(0));
    }

    @Override
    public Long insert(Map<String, Object> conversation) {
        String sql = """
            INSERT INTO chat_conversations (type, product_id, shop_id, user_a_id, user_b_id, last_message_at, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, (String) conversation.get("type"));
            ps.setObject(2, conversation.get("productId"));
            ps.setObject(3, conversation.get("shopId"));
            ps.setLong(4, (Long) conversation.get("userAId"));
            ps.setLong(5, (Long) conversation.get("userBId"));
            ps.setTimestamp(6, Timestamp.valueOf((LocalDateTime) conversation.get("lastMessageAt")));
            ps.setTimestamp(7, Timestamp.valueOf((LocalDateTime) conversation.get("createdAt")));
            return ps;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "conversation_id");
    }

    @Override
    public int updateType(Long id, String type) {
        return jdbcTemplate.update("UPDATE chat_conversations SET type = ? WHERE id = ?", type, id);
    }

    @Override
    public int updateLastMessageAt(Long id, String lastMessageAt) {
        String sql = "UPDATE chat_conversations SET last_message_at = ? WHERE id = ?";
        return jdbcTemplate.update(sql, Timestamp.valueOf(LocalDateTime.parse(lastMessageAt)), id);
    }

    @Override
    public int incrementUnreadCount(Long id, Long recipientUserId) {
        if (!unreadColumnsAvailable()) {
            return 0;
        }
        String sql = """
            UPDATE chat_conversations
            SET unread_count_a = CASE WHEN user_a_id = ? THEN unread_count_a + 1 ELSE unread_count_a END,
                unread_count_b = CASE WHEN user_b_id = ? THEN unread_count_b + 1 ELSE unread_count_b END
            WHERE id = ? AND (user_a_id = ? OR user_b_id = ?)
            """;
        return jdbcTemplate.update(sql, recipientUserId, recipientUserId, id, recipientUserId, recipientUserId);
    }

    @Override
    public int clearUnreadCount(Long id, Long userId) {
        if (!unreadColumnsAvailable()) {
            return 0;
        }
        String sql = """
            UPDATE chat_conversations
            SET unread_count_a = CASE WHEN user_a_id = ? THEN 0 ELSE unread_count_a END,
                unread_count_b = CASE WHEN user_b_id = ? THEN 0 ELSE unread_count_b END
            WHERE id = ? AND (user_a_id = ? OR user_b_id = ?)
            """;
        return jdbcTemplate.update(sql, userId, userId, id, userId, userId);
    }

    @Override
    public int sumUnreadCountByUserId(Long userId) {
        if (!unreadColumnsAvailable()) {
            return 0;
        }
        String sql = managementColumnsAvailable()
                ? """
                    SELECT COALESCE(SUM(CASE
                        WHEN user_a_id = ? AND is_muted_a = FALSE AND deleted_by_a_at IS NULL THEN unread_count_a
                        WHEN user_b_id = ? AND is_muted_b = FALSE AND deleted_by_b_at IS NULL THEN unread_count_b
                        ELSE 0
                    END), 0)
                    FROM chat_conversations
                    WHERE (user_a_id = ? AND deleted_by_a_at IS NULL)
                       OR (user_b_id = ? AND deleted_by_b_at IS NULL)
                    """
                : """
                    SELECT COALESCE(SUM(CASE
                        WHEN user_a_id = ? THEN unread_count_a
                        WHEN user_b_id = ? THEN unread_count_b
                        ELSE 0
                    END), 0)
                    FROM chat_conversations
                    WHERE user_a_id = ? OR user_b_id = ?
                    """;
        Long count = jdbcTemplate.queryForObject(sql, Long.class, userId, userId, userId, userId);
        return count == null ? 0 : count.intValue();
    }

    @Override
    public int updatePinStatus(Long id, Long userId, boolean pinned) {
        if (!managementColumnsAvailable()) {
            return 0;
        }
        String sql = """
            UPDATE chat_conversations
            SET is_pinned_a = CASE WHEN user_a_id = ? THEN ? ELSE is_pinned_a END,
                is_pinned_b = CASE WHEN user_b_id = ? THEN ? ELSE is_pinned_b END
            WHERE id = ? AND (user_a_id = ? OR user_b_id = ?)
            """;
        return jdbcTemplate.update(sql, userId, pinned, userId, pinned, id, userId, userId);
    }

    @Override
    public int updateMuteStatus(Long id, Long userId, boolean muted) {
        if (!managementColumnsAvailable()) {
            return 0;
        }
        String sql = """
            UPDATE chat_conversations
            SET is_muted_a = CASE WHEN user_a_id = ? THEN ? ELSE is_muted_a END,
                is_muted_b = CASE WHEN user_b_id = ? THEN ? ELSE is_muted_b END
            WHERE id = ? AND (user_a_id = ? OR user_b_id = ?)
            """;
        return jdbcTemplate.update(sql, userId, muted, userId, muted, id, userId, userId);
    }

    @Override
    public int softDelete(Long id, Long userId) {
        if (!managementColumnsAvailable()) {
            return 0;
        }
        String sql = """
            UPDATE chat_conversations
            SET deleted_by_a_at = CASE WHEN user_a_id = ? THEN CURRENT_TIMESTAMP ELSE deleted_by_a_at END,
                deleted_by_b_at = CASE WHEN user_b_id = ? THEN CURRENT_TIMESTAMP ELSE deleted_by_b_at END
            WHERE id = ? AND (user_a_id = ? OR user_b_id = ?)
            """;
        return jdbcTemplate.update(sql, userId, userId, id, userId, userId);
    }

    @Override
    public int restoreForUser(Long id, Long userId) {
        if (!managementColumnsAvailable()) {
            return 0;
        }
        String sql = """
            UPDATE chat_conversations
            SET deleted_by_a_at = CASE WHEN user_a_id = ? THEN NULL ELSE deleted_by_a_at END,
                deleted_by_b_at = CASE WHEN user_b_id = ? THEN NULL ELSE deleted_by_b_at END
            WHERE id = ? AND (user_a_id = ? OR user_b_id = ?)
            """;
        return jdbcTemplate.update(sql, userId, userId, id, userId, userId);
    }

    @Override
    public int updateAutoRepliedAt(Long id, Long targetUserId, LocalDateTime repliedAt) {
        if (!autoReplyColumnsAvailable()) {
            return 0;
        }
        String sql = """
            UPDATE chat_conversations
            SET auto_replied_to_a_at = CASE WHEN user_a_id = ? THEN ? ELSE auto_replied_to_a_at END,
                auto_replied_to_b_at = CASE WHEN user_b_id = ? THEN ? ELSE auto_replied_to_b_at END
            WHERE id = ? AND (user_a_id = ? OR user_b_id = ?)
            """;
        Timestamp timestamp = Timestamp.valueOf(repliedAt);
        return jdbcTemplate.update(sql, targetUserId, timestamp, targetUserId, timestamp, id, targetUserId, targetUserId);
    }

    @Override
    public Long findPlatformCsUserId() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM users WHERE username = 'platform_cs' LIMIT 1");
        if (rows.isEmpty()) {
            return null;
        }
        Object id = rows.get(0).get("id");
        return id instanceof Number number ? number.longValue() : null;
    }

    @Override
    public int updateSupportStatus(Long id, String supportStatus) {
        if (!supportColumnsAvailable()) {
            return 0;
        }
        return jdbcTemplate.update(
                "UPDATE chat_conversations SET support_status = ? WHERE id = ?", supportStatus, id);
    }

    @Override
    public int clearSupportAssignment(Long id) {
        if (!supportColumnsAvailable()) {
            return 0;
        }
        return jdbcTemplate.update(
                "UPDATE chat_conversations SET assigned_admin_id = NULL WHERE id = ?", id);
    }

    @Override
    public int assignSupportAgent(Long id, Long adminId) {
        if (!supportColumnsAvailable()) {
            return 0;
        }
        return jdbcTemplate.update(
                "UPDATE chat_conversations SET assigned_admin_id = ?, support_status = 'human' WHERE id = ?",
                adminId, id);
    }

    private Map<String, Object> normalizeConversation(Map<String, Object> raw) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("id", raw.get("id"));
        normalized.put("type", raw.get("type"));
        normalized.put("productId", raw.get("product_id"));
        normalized.put("shopId", raw.get("shop_id"));
        normalized.put("userAId", raw.get("user_a_id"));
        normalized.put("userBId", raw.get("user_b_id"));
        normalized.put("unreadCountA", raw.get("unread_count_a"));
        normalized.put("unreadCountB", raw.get("unread_count_b"));
        normalized.put("isPinnedA", raw.get("is_pinned_a"));
        normalized.put("isPinnedB", raw.get("is_pinned_b"));
        normalized.put("isMutedA", raw.get("is_muted_a"));
        normalized.put("isMutedB", raw.get("is_muted_b"));
        normalized.put("deletedByAAt", raw.get("deleted_by_a_at"));
        normalized.put("deletedByBAt", raw.get("deleted_by_b_at"));
        normalized.put("autoRepliedToAAt", raw.get("auto_replied_to_a_at"));
        normalized.put("autoRepliedToBAt", raw.get("auto_replied_to_b_at"));
        normalized.put("supportStatus", raw.get("support_status"));
        normalized.put("assignedAdminId", raw.get("assigned_admin_id"));
        normalized.put("lastMessageAt", raw.get("last_message_at"));
        normalized.put("createdAt", raw.get("created_at"));

        // Add user info
        Map<String, Object> userA = new LinkedHashMap<>();
        userA.put("id", raw.get("user_a_id"));
        userA.put("username", raw.get("user_a_username"));
        userA.put("nickname", raw.get("user_a_nickname"));
        userA.put("avatar", raw.get("user_a_avatar"));
        normalized.put("userA", userA);

        Map<String, Object> userB = new LinkedHashMap<>();
        userB.put("id", raw.get("user_b_id"));
        userB.put("username", raw.get("user_b_username"));
        userB.put("nickname", raw.get("user_b_nickname"));
        userB.put("avatar", raw.get("user_b_avatar"));
        normalized.put("userB", userB);

        return normalized;
    }

    private boolean unreadColumnsAvailable() {
        if (unreadColumnsAvailable == null) {
            unreadColumnsAvailable = columnExists("chat_conversations", "unread_count_a")
                    && columnExists("chat_conversations", "unread_count_b");
        }
        return unreadColumnsAvailable;
    }

    private boolean managementColumnsAvailable() {
        if (managementColumnsAvailable == null) {
            managementColumnsAvailable = columnExists("chat_conversations", "is_pinned_a")
                    && columnExists("chat_conversations", "is_pinned_b")
                    && columnExists("chat_conversations", "is_muted_a")
                    && columnExists("chat_conversations", "is_muted_b")
                    && columnExists("chat_conversations", "deleted_by_a_at")
                    && columnExists("chat_conversations", "deleted_by_b_at");
        }
        return managementColumnsAvailable;
    }

    private boolean autoReplyColumnsAvailable() {
        if (autoReplyColumnsAvailable == null) {
            autoReplyColumnsAvailable = columnExists("chat_conversations", "auto_replied_to_a_at")
                    && columnExists("chat_conversations", "auto_replied_to_b_at");
        }
        return autoReplyColumnsAvailable;
    }

    private boolean supportColumnsAvailable() {
        if (supportColumnsAvailable == null) {
            supportColumnsAvailable = columnExists("chat_conversations", "support_status")
                    && columnExists("chat_conversations", "assigned_admin_id");
        }
        return supportColumnsAvailable;
    }

    @Override
    public void resetSchemaAvailabilityCache() {
        unreadColumnsAvailable = null;
        managementColumnsAvailable = null;
        autoReplyColumnsAvailable = null;
        supportColumnsAvailable = null;
    }

    private boolean columnExists(String tableName, String columnName) {
        return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) (conn) -> {
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
                if (rs.next()) {
                    return true;
                }
            }
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
                return rs.next();
            }
        }));
    }
}
