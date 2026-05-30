package com.youyu.backend.config;

import java.sql.Connection;
import java.sql.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.youyu.backend.mapper.chat.ChatConversationMapper;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Applies additive chat support columns to pre-existing {@code chat_conversations} tables.
 * {@code schema.sql} only runs {@code CREATE TABLE IF NOT EXISTS}, so older MySQL databases
 * keep the original table shape until these columns are added.
 */
@Component
public class ChatSupportSchemaUpgrader {

    private static final Logger log = LoggerFactory.getLogger(ChatSupportSchemaUpgrader.class);
    private static final String TABLE = "chat_conversations";

    private static final String PLATFORM_CS_PASSWORD_HASH =
            "$2a$10$jJy4r2olYY7ca7bAvZRuJe9Z77E.JZxzVTugqYw6S8lr4ahKU2hqG";

    private final JdbcTemplate jdbcTemplate;
    private final ChatConversationMapper conversationMapper;

    public ChatSupportSchemaUpgrader(JdbcTemplate jdbcTemplate, ChatConversationMapper conversationMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.conversationMapper = conversationMapper;
    }

    public void upgradeIfNeeded() {
        ensurePlatformCsUser();
        ensureLegacyChatColumns();
        ensureSupportColumns();
        backfillSupportStatuses();
        conversationMapper.resetSchemaAvailabilityCache();
    }

    private void ensureLegacyChatColumns() {
        addColumnIfMissing(TABLE, "unread_count_a", "INT NOT NULL DEFAULT 0");
        addColumnIfMissing(TABLE, "unread_count_b", "INT NOT NULL DEFAULT 0");
        addColumnIfMissing(TABLE, "is_pinned_a", "BOOLEAN NOT NULL DEFAULT FALSE");
        addColumnIfMissing(TABLE, "is_pinned_b", "BOOLEAN NOT NULL DEFAULT FALSE");
        addColumnIfMissing(TABLE, "is_muted_a", "BOOLEAN NOT NULL DEFAULT FALSE");
        addColumnIfMissing(TABLE, "is_muted_b", "BOOLEAN NOT NULL DEFAULT FALSE");
        addColumnIfMissing(TABLE, "deleted_by_a_at", "TIMESTAMP NULL");
        addColumnIfMissing(TABLE, "deleted_by_b_at", "TIMESTAMP NULL");
        addColumnIfMissing(TABLE, "auto_replied_to_a_at", "TIMESTAMP NULL");
        addColumnIfMissing(TABLE, "auto_replied_to_b_at", "TIMESTAMP NULL");

        addColumnIfMissing("chat_messages", "is_read", "BOOLEAN NOT NULL DEFAULT FALSE");
        addColumnIfMissing("chat_messages", "read_at", "TIMESTAMP NULL");
        addColumnIfMissing("chat_messages", "message_type", "VARCHAR(32) NOT NULL DEFAULT 'text'");
        addColumnIfMissing("chat_messages", "media_url", "MEDIUMTEXT NULL");
        addColumnIfMissing("chat_messages", "product_id", "BIGINT NULL");
        addColumnIfMissing("chat_messages", "order_id", "BIGINT NULL");
        addColumnIfMissing("chat_messages", "is_recalled", "BOOLEAN NOT NULL DEFAULT FALSE");
        addColumnIfMissing("chat_messages", "recalled_at", "TIMESTAMP NULL");
    }

    private void ensureSupportColumns() {
        addColumnIfMissing(TABLE, "support_status", "VARCHAR(16) NULL");
        addColumnIfMissing(TABLE, "assigned_admin_id", "BIGINT NULL");

        if (usesMySqlCompatibleCatalog()) {
            ensureForeignKey();
            ensureIndex("idx_support_status", "support_status, last_message_at DESC");
            ensureIndex("idx_support_assigned", "assigned_admin_id, support_status");
        }
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        if (columnExists(tableName, columnName)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        log.info("Added {}.{}", tableName, columnName);
    }

    private void backfillSupportStatuses() {
        int aiDefaults = jdbcTemplate.update("""
                UPDATE chat_conversations
                SET support_status = 'ai'
                WHERE type = 'support'
                  AND (support_status IS NULL OR support_status = '')
                """);
        if (aiDefaults > 0) {
            log.info("Backfilled support_status=ai on {} support conversation(s)", aiDefaults);
        }

        int pendingRecovery = jdbcTemplate.update("""
                UPDATE chat_conversations cc
                SET support_status = 'pending'
                WHERE cc.type = 'support'
                  AND cc.support_status = 'ai'
                  AND EXISTS (
                    SELECT 1 FROM chat_messages m
                    WHERE m.conversation_id = cc.id
                      AND m.body LIKE '%转接人工客服%'
                  )
                """);
        if (pendingRecovery > 0) {
            log.info("Recovered support_status=pending on {} escalated conversation(s)", pendingRecovery);
        }
    }

    private void ensurePlatformCsUser() {
        Long existingId = conversationMapper.findPlatformCsUserId();
        if (existingId != null) {
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO users (
                    username, phone, email, password_hash, nickname, avatar, status, role,
                    registered_at, created_at, updated_at
                ) VALUES (
                    'platform_cs', '13800001099', 'cs@campus.edu.cn', ?, '平台客服', '', 'active', 'USER',
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """, PLATFORM_CS_PASSWORD_HASH);
        log.info("Seeded platform_cs account for online customer service");
    }

    private void ensureForeignKey() {
        if (constraintExists("fk_chat_conv_admin")) {
            return;
        }
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE chat_conversations
                    ADD CONSTRAINT fk_chat_conv_admin
                    FOREIGN KEY (assigned_admin_id) REFERENCES users(id)
                    """);
            log.info("Added chat_conversations.fk_chat_conv_admin");
        } catch (Exception ex) {
            log.warn("Could not add fk_chat_conv_admin (may already exist): {}", ex.getMessage());
        }
    }

    private void ensureIndex(String indexName, String columns) {
        if (indexExists(indexName)) {
            return;
        }
        try {
            jdbcTemplate.execute(
                    "CREATE INDEX " + indexName + " ON chat_conversations (" + columns + ")");
            log.info("Added index {} on chat_conversations", indexName);
        } catch (Exception ex) {
            log.warn("Could not add index {} (may already exist): {}", indexName, ex.getMessage());
        }
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

    private boolean usesMySqlCompatibleCatalog() {
        return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) conn -> {
            String product = conn.getMetaData().getDatabaseProductName();
            if (product == null) {
                return false;
            }
            String normalized = product.toLowerCase();
            return normalized.contains("mysql") || normalized.contains("mariadb");
        }));
    }

    private boolean constraintExists(String constraintName) {
        try {
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.TABLE_CONSTRAINTS
                    WHERE CONSTRAINT_SCHEMA = DATABASE()
                      AND TABLE_NAME = ?
                      AND CONSTRAINT_NAME = ?
                    """, Long.class, TABLE, constraintName);
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean indexExists(String indexName) {
        try {
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = ?
                      AND INDEX_NAME = ?
                    """, Long.class, TABLE, indexName);
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }
}
