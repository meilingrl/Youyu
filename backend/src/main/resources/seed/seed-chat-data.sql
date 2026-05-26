-- Message-center seed data for development and acceptance testing.
-- User IDs: 1001=zhangsan, 1002=lisi, 1003=wangwu.

-- Conversations include unread counters so red-dot/read-state behavior is visible
-- immediately after starting the app with the seed profile.
INSERT INTO chat_conversations (
    id, type, product_id, shop_id, user_a_id, user_b_id,
    unread_count_a, unread_count_b, last_message_at, created_at
)
VALUES
(1, 'product_inquiry', 3001, NULL, 1001, 1002, 1, 0, '2026-05-25 14:20:00', '2026-05-25 10:00:00'),
(2, 'product_inquiry', 3002, NULL, 1001, 1003, 1, 0, '2026-05-24 19:05:00', '2026-05-24 18:00:00'),
(3, 'shop_inquiry', NULL, 4001, 1001, 1002, 0, 1, '2026-05-24 11:40:00', '2026-05-24 11:30:00'),
(4, 'direct', NULL, NULL, 1002, 1003, 0, 0, '2026-05-23 16:30:00', '2026-05-23 15:00:00');

INSERT INTO chat_messages (
    id, conversation_id, sender_user_id, body, is_read, read_at, message_type, media_url, created_at
)
VALUES
(1, 1, 1001, 'Hi, is the advanced math review pack still available?', TRUE, '2026-05-25 10:06:00', 'text', NULL, '2026-05-25 10:05:00'),
(2, 1, 1002, 'Yes, it is still available and ready for campus pickup.', TRUE, '2026-05-25 10:11:00', 'text', NULL, '2026-05-25 10:10:00'),
(3, 1, 1001, 'Great. Can we meet near the library tomorrow afternoon?', TRUE, '2026-05-25 10:16:00', 'text', NULL, '2026-05-25 10:15:00'),
(4, 1, 1002, 'Tomorrow 3 PM at the library entrance works for me.', FALSE, NULL, 'text', NULL, '2026-05-25 10:20:00'),
(5, 1, 1001, 'See you there. Thanks.', TRUE, '2026-05-25 14:21:00', 'text', NULL, '2026-05-25 14:20:00'),
(6, 2, 1001, 'Hi, is the drawing tool set still in stock?', TRUE, '2026-05-24 18:31:00', 'text', NULL, '2026-05-24 18:30:00'),
(7, 2, 1003, 'It is available. I can bring it to the teaching building tomorrow.', FALSE, NULL, 'text', NULL, '2026-05-24 19:05:00'),
(8, 3, 1001, 'Do you have more computer networking notes in your shop?', FALSE, NULL, 'text', NULL, '2026-05-24 11:35:00'),
(9, 3, 1002, 'Yes, I have network notes and a TCP/IP summary available.', TRUE, '2026-05-24 11:41:00', 'text', NULL, '2026-05-24 11:40:00'),
(10, 4, 1002, 'Any useful study material recommendations recently?', TRUE, '2026-05-23 15:31:00', 'text', NULL, '2026-05-23 15:30:00'),
(11, 4, 1003, 'I recommend Computer Systems: A Programmer Perspective.', TRUE, '2026-05-23 16:01:00', 'text', NULL, '2026-05-23 16:00:00'),
(12, 4, 1002, 'Good idea. I will check it out.', TRUE, '2026-05-23 16:31:00', 'text', NULL, '2026-05-23 16:30:00');

-- Notifications are compactly surfaced inside the message center, not as a main
-- navigation section.
INSERT INTO notifications (id, user_id, type, title, body, action_url, is_read, created_at)
VALUES
(12001, 1001, 'order_status', 'Order payment reminder', 'Order SEED8002 is waiting for payment confirmation.', '/app/orders', FALSE, '2026-05-25 15:10:00'),
(12002, 1001, 'system', 'Message center enabled', 'Unread messages and order notifications are available in the message center.', '/app/messages', TRUE, '2026-05-24 09:00:00'),
(12003, 1002, 'review_reminder', 'Pending review reminder', 'You have a pending campus review task to check.', '/app/reviews/pending', FALSE, '2026-05-25 12:30:00');
