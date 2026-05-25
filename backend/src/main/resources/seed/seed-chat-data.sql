-- Chat seed data for testing
-- This file creates sample conversations and messages for development and testing
-- User IDs: 1001=zhangsan, 1002=lisi, 1003=wangwu, 1004=zhaoliu

-- Sample conversations between users
-- Conversation 1: zhangsan (1001) <-> lisi (1002) about product inquiry
INSERT INTO chat_conversations (id, type, product_id, shop_id, user_a_id, user_b_id, last_message_at, created_at)
VALUES (1, 'product_inquiry', 1, NULL, 1001, 1002, '2026-05-25 14:20:00', '2026-05-25 10:00:00');

-- Conversation 2: zhangsan (1001) <-> wangwu (1003) about product inquiry
INSERT INTO chat_conversations (id, type, product_id, shop_id, user_a_id, user_b_id, last_message_at, created_at)
VALUES (2, 'product_inquiry', 2, NULL, 1001, 1003, '2026-05-24 19:05:00', '2026-05-24 18:00:00');

-- Conversation 3: zhangsan (1001) <-> lisi (1002) shop inquiry
INSERT INTO chat_conversations (id, type, shop_id, user_a_id, user_b_id, last_message_at, created_at)
VALUES (3, 'shop_inquiry', 1, 1001, 1002, '2026-05-24 11:40:00', '2026-05-24 11:30:00');

-- Conversation 4: lisi (1002) <-> wangwu (1003) direct conversation
INSERT INTO chat_conversations (id, type, user_a_id, user_b_id, last_message_at, created_at)
VALUES (4, 'direct', 1002, 1003, '2026-05-23 16:30:00', '2026-05-23 15:00:00');

-- Messages for Conversation 1 (zhangsan <-> lisi about product 1)
INSERT INTO chat_messages (id, conversation_id, sender_user_id, body, created_at)
VALUES
(1, 1, 1001, '你好，请问这本《数据结构与算法分析》还有货吗？', '2026-05-25 10:05:00'),
(2, 1, 1002, '您好！有货的，这是我上学期用的教材，9成新。', '2026-05-25 10:10:00'),
(3, 1, 1001, '太好了！请问可以在哪里交接呢？', '2026-05-25 10:15:00'),
(4, 1, 1002, '可以在图书馆门口或者一教大厅，您方便的话明天下午3点可以吗？', '2026-05-25 10:20:00'),
(5, 1, 1001, '明天下午3点图书馆门口见，谢谢！', '2026-05-25 14:20:00');

-- Messages for Conversation 2 (zhangsan <-> wangwu about product 2)
INSERT INTO chat_messages (id, conversation_id, sender_user_id, body, created_at)
VALUES
(6, 2, 1001, '你好，请问这个实验试剂还有库存吗？', '2026-05-24 18:30:00'),
(7, 2, 1003, '您好！这款试剂目前有货，明天可以在一教门口交接。', '2026-05-24 19:05:00');

-- Messages for Conversation 3 (zhangsan <-> lisi shop inquiry)
INSERT INTO chat_messages (id, conversation_id, sender_user_id, body, created_at)
VALUES
(8, 3, 1001, '你好，请问你们店铺有没有计算机网络相关的教材？', '2026-05-24 11:35:00'),
(9, 3, 1002, '您好！我们有《计算机网络：自顶向下方法》和《TCP/IP详解》，都是正版二手书。', '2026-05-24 11:40:00');

-- Messages for Conversation 4 (lisi <-> wangwu)
INSERT INTO chat_messages (id, conversation_id, sender_user_id, body, created_at)
VALUES
(10, 4, 1002, '嗨，最近有什么好的学习资料推荐吗？', '2026-05-23 15:30:00'),
(11, 4, 1003, '我最近在看《深入理解计算机系统》，强烈推荐！', '2026-05-23 16:00:00'),
(12, 4, 1002, '好的，我去找找看，谢谢推荐！', '2026-05-23 16:30:00');
