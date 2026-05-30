-- Additive migration for online customer-service chat columns.
-- Safe to run on an existing `youyu` database created before 2026-05-30.
-- The backend also applies this automatically on startup via ChatSupportSchemaUpgrader.

USE youyu;

ALTER TABLE chat_conversations
    ADD COLUMN support_status VARCHAR(16) NULL;

ALTER TABLE chat_conversations
    ADD COLUMN assigned_admin_id BIGINT NULL;

ALTER TABLE chat_conversations
    ADD CONSTRAINT fk_chat_conv_admin
    FOREIGN KEY (assigned_admin_id) REFERENCES users(id);

CREATE INDEX idx_support_status ON chat_conversations (support_status, last_message_at DESC);
CREATE INDEX idx_support_assigned ON chat_conversations (assigned_admin_id, support_status);

-- Platform CS identity (required for POST /api/chat/support/session)
INSERT INTO users (
    username, phone, email, password_hash, nickname, avatar, status, role,
    registered_at, created_at, updated_at
) VALUES (
    'platform_cs', '13800001099', 'cs@campus.edu.cn',
    '$2a$10$jJy4r2olYY7ca7bAvZRuJe9Z77E.JZxzVTugqYw6S8lr4ahKU2hqG',
    '平台客服', '', 'active', 'USER',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), status = VALUES(status);
