-- Message-center seed data for development and acceptance testing.
-- Existing demo users: 1001=zhangsan, 1002=lisi, 1003=wangwu, 1004=zhaoliu,
-- 1010=seedbuyer, 1011=seedbuyer2, 1012=seedbuyer3, 1013=seedbuyer4.
-- Zhang San conversation range owned by this seed file:
--   chat_conversations: 13001-13016
--   chat_messages:      14001-14044

-- Legacy compact seed rows kept idempotent for compatibility with older local
-- databases that already loaded the initial message-center seed.
INSERT INTO chat_conversations (
    id, type, product_id, shop_id, user_a_id, user_b_id,
    unread_count_a, unread_count_b, last_message_at, created_at
)
VALUES
(1, 'product_inquiry', 3001, NULL, 1001, 1002, 1, 0, '2026-05-25 14:20:00', '2026-05-25 10:00:00'),
(2, 'product_inquiry', 3002, NULL, 1001, 1003, 1, 0, '2026-05-24 19:05:00', '2026-05-24 18:00:00'),
(3, 'shop_inquiry', NULL, 4001, 1001, 1002, 0, 1, '2026-05-24 11:40:00', '2026-05-24 11:30:00'),
(4, 'direct', NULL, NULL, 1002, 1003, 0, 0, '2026-05-23 16:30:00', '2026-05-23 15:00:00')
ON DUPLICATE KEY UPDATE
    type = VALUES(type),
    product_id = VALUES(product_id),
    shop_id = VALUES(shop_id),
    user_a_id = VALUES(user_a_id),
    user_b_id = VALUES(user_b_id),
    unread_count_a = VALUES(unread_count_a),
    unread_count_b = VALUES(unread_count_b),
    last_message_at = VALUES(last_message_at),
    created_at = VALUES(created_at);

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
(12, 4, 1002, 'Good idea. I will check it out.', TRUE, '2026-05-23 16:31:00', 'text', NULL, '2026-05-23 16:30:00')
ON DUPLICATE KEY UPDATE
    conversation_id = VALUES(conversation_id),
    sender_user_id = VALUES(sender_user_id),
    body = VALUES(body),
    is_read = VALUES(is_read),
    read_at = VALUES(read_at),
    message_type = VALUES(message_type),
    media_url = VALUES(media_url),
    created_at = VALUES(created_at);

-- Expanded Zhang San test inbox. Each conversation has 2-3 messages and varied
-- read state so list sorting, unread badges, search, left/right bubbles, and
-- preview text can be tested without creating additional users.
INSERT INTO chat_conversations (
    id, type, product_id, shop_id, user_a_id, user_b_id,
    unread_count_a, unread_count_b, last_message_at, created_at
)
VALUES
(13001, 'product_inquiry', 3010, NULL, 1001, 1010, 2, 0, '2026-05-26 20:18:00', '2026-05-26 20:10:00'),
(13002, 'product_inquiry', 3002, NULL, 1001, 1004, 0, 1, '2026-05-26 19:44:00', '2026-05-26 19:32:00'),
(13003, 'direct', NULL, NULL, 1011, 1001, 1, 0, '2026-05-26 18:52:00', '2026-05-26 18:40:00'),
(13004, 'product_inquiry', 3003, NULL, 1001, 1011, 0, 0, '2026-05-26 17:36:00', '2026-05-26 17:20:00'),
(13005, 'shop_inquiry', NULL, 4011, 1001, 1012, 1, 0, '2026-05-26 16:08:00', '2026-05-26 15:55:00'),
(13006, 'product_inquiry', 3001, NULL, 1013, 1001, 0, 1, '2026-05-26 14:25:00', '2026-05-26 14:12:00'),
(13007, 'direct', NULL, NULL, 1001, 1010, 2, 0, '2026-05-26 12:10:00', '2026-05-26 11:58:00'),
(13008, 'product_inquiry', 3011, NULL, 1001, 1010, 0, 0, '2026-05-25 22:16:00', '2026-05-25 22:00:00'),
(13009, 'product_inquiry', 3012, NULL, 1001, 1010, 1, 0, '2026-05-25 21:05:00', '2026-05-25 20:50:00'),
(13010, 'product_inquiry', 3013, NULL, 1001, 1012, 0, 1, '2026-05-25 18:40:00', '2026-05-25 18:22:00'),
(13011, 'shop_inquiry', NULL, 4001, 1012, 1001, 1, 0, '2026-05-25 16:28:00', '2026-05-25 16:08:00'),
(13012, 'product_inquiry', 3004, NULL, 1001, 1013, 0, 0, '2026-05-25 13:15:00', '2026-05-25 12:58:00'),
(13013, 'direct', NULL, NULL, 1004, 1001, 1, 0, '2026-05-24 21:30:00', '2026-05-24 21:10:00'),
(13014, 'product_inquiry', 3014, NULL, 1001, 1012, 0, 0, '2026-05-24 19:45:00', '2026-05-24 19:25:00'),
(13015, 'shop_inquiry', NULL, 4010, 1010, 1001, 0, 1, '2026-05-24 15:35:00', '2026-05-24 15:16:00'),
(13016, 'direct', NULL, NULL, 1001, 1002, 1, 0, '2026-05-23 20:20:00', '2026-05-23 20:00:00')
ON DUPLICATE KEY UPDATE
    type = VALUES(type),
    product_id = VALUES(product_id),
    shop_id = VALUES(shop_id),
    user_a_id = VALUES(user_a_id),
    user_b_id = VALUES(user_b_id),
    unread_count_a = VALUES(unread_count_a),
    unread_count_b = VALUES(unread_count_b),
    last_message_at = VALUES(last_message_at),
    created_at = VALUES(created_at);

INSERT INTO chat_messages (
    id, conversation_id, sender_user_id, body, is_read, read_at, message_type, media_url, product_id, order_id, created_at
)
VALUES
(14001, 13001, 1001, '这个 USB Hub 还能走校内自提吗？我在浑南宿舍区。', TRUE, '2026-05-26 20:11:00', 'text', NULL, 3010, NULL, '2026-05-26 20:10:00'),
(14002, 13001, 1010, '可以，今晚 8 点半后在七舍楼下取。', FALSE, NULL, 'text', NULL, 3010, NULL, '2026-05-26 20:16:00'),
(14003, 13001, 1010, '包装盒和 Type-C 转接头都还在。', FALSE, NULL, 'text', NULL, 3010, NULL, '2026-05-26 20:18:00'),
(14004, 13002, 1001, '工程制图工具套装最低 30 可以吗？', TRUE, '2026-05-26 19:33:00', 'text', NULL, 3002, NULL, '2026-05-26 19:32:00'),
(14005, 13002, 1004, '尺规都齐，35 已经是二手价了。', TRUE, '2026-05-26 19:39:00', 'text', NULL, 3002, NULL, '2026-05-26 19:38:00'),
(14006, 13002, 1001, '那 33 我现在下单，今晚能取吗？', FALSE, NULL, 'text', NULL, 3002, NULL, '2026-05-26 19:44:00'),
(14007, 13003, 1011, '我刚拍的订单 SEED8003，宿舍架子能不能明天中午取？', TRUE, '2026-05-26 18:42:00', 'text', NULL, 3003, 8003, '2026-05-26 18:40:00'),
(14008, 13003, 1001, '可以，中午 12:20 在图书馆东门。', TRUE, '2026-05-26 18:47:00', 'text', NULL, 3003, 8003, '2026-05-26 18:46:00'),
(14009, 13003, 1011, '好的，我带订单页过去。', FALSE, NULL, 'text', NULL, 3003, 8003, '2026-05-26 18:52:00'),
(14010, 13004, 1001, '折叠宿舍收纳架还有明显划痕吗？', TRUE, '2026-05-26 17:21:00', 'text', NULL, 3003, NULL, '2026-05-26 17:20:00'),
(14011, 13004, 1011, '边角有一点磨损，不影响承重。', TRUE, '2026-05-26 17:29:00', 'text', NULL, 3003, NULL, '2026-05-26 17:28:00'),
(14012, 13004, 1001, '可以，麻烦留到明晚。', TRUE, '2026-05-26 17:37:00', 'text', NULL, 3003, NULL, '2026-05-26 17:36:00'),
(14013, 13005, 1001, '你们店里的机器人实验线材支持批量买吗？', TRUE, '2026-05-26 15:57:00', 'text', NULL, NULL, NULL, '2026-05-26 15:55:00'),
(14014, 13005, 1012, '可以，USB-C 线 10 条以上按 9 元一条。', TRUE, '2026-05-26 16:03:00', 'text', NULL, 3013, NULL, '2026-05-26 16:02:00'),
(14015, 13005, 1012, '如果是社团采购，可以约创新楼 B1 自提。', FALSE, NULL, 'text', NULL, NULL, NULL, '2026-05-26 16:08:00'),
(14016, 13006, 1013, '高数资料付款后多久能看到完整版？', TRUE, '2026-05-26 14:14:00', 'text', NULL, 3001, 8009, '2026-05-26 14:12:00'),
(14017, 13006, 1001, '确认收货后系统会开放下载，预览现在就能看。', FALSE, NULL, 'text', NULL, 3001, 8009, '2026-05-26 14:25:00'),
(14018, 13007, 1001, 'SEED8008 的收纳架我看到还是待付款，是不是支付没同步？', TRUE, '2026-05-26 11:59:00', 'text', NULL, 3003, 8008, '2026-05-26 11:58:00'),
(14019, 13007, 1010, '我这边也显示未付款，可以先别发货。', FALSE, NULL, 'text', NULL, 3003, 8008, '2026-05-26 12:05:00'),
(14020, 13007, 1010, '等我晚点重新点支付。', FALSE, NULL, 'text', NULL, 3003, 8008, '2026-05-26 12:10:00'),
(14021, 13008, 1001, '无线鼠标支持快递到南湖校区吗？', TRUE, '2026-05-25 22:01:00', 'text', NULL, 3011, NULL, '2026-05-25 22:00:00'),
(14022, 13008, 1010, '支持，中通次日到，校内自提也可以。', TRUE, '2026-05-25 22:10:00', 'text', NULL, 3011, NULL, '2026-05-25 22:09:00'),
(14023, 13008, 1001, '那我备注南湖驿站，辛苦。', TRUE, '2026-05-25 22:17:00', 'text', NULL, 3011, NULL, '2026-05-25 22:16:00'),
(14024, 13009, 1001, '科学计算器是考试允许型号吗？', TRUE, '2026-05-25 20:51:00', 'text', NULL, 3012, NULL, '2026-05-25 20:50:00'),
(14025, 13009, 1010, '是常见考试型号，电池也刚换。', FALSE, NULL, 'text', NULL, 3012, NULL, '2026-05-25 21:05:00'),
(14026, 13010, 1001, 'USB-C 线 2 米的还剩几条？', TRUE, '2026-05-25 18:23:00', 'text', NULL, 3013, NULL, '2026-05-25 18:22:00'),
(14027, 13010, 1012, '还剩 20 条，社团活动可以一起拿。', TRUE, '2026-05-25 18:31:00', 'text', NULL, 3013, NULL, '2026-05-25 18:30:00'),
(14028, 13010, 1001, '先帮我留 3 条，明天确认数量。', FALSE, NULL, 'text', NULL, 3013, NULL, '2026-05-25 18:40:00'),
(14029, 13011, 1012, '课程笔记店的 TCP/IP 总结还能补货吗？', TRUE, '2026-05-25 16:09:00', 'text', NULL, NULL, NULL, '2026-05-25 16:08:00'),
(14030, 13011, 1001, '今晚会重新上传新版，审核通过后可买。', TRUE, '2026-05-25 16:18:00', 'text', NULL, 3004, NULL, '2026-05-25 16:17:00'),
(14031, 13011, 1012, '麻烦上架后提醒我一下。', FALSE, NULL, 'text', NULL, NULL, NULL, '2026-05-25 16:28:00'),
(14032, 13012, 1001, '数据结构笔记还在审核，暂时不能付款。', TRUE, '2026-05-25 12:59:00', 'text', NULL, 3004, NULL, '2026-05-25 12:58:00'),
(14033, 13012, 1013, '可以先看目录预览吗？我想搜一下图算法章节。', TRUE, '2026-05-25 13:08:00', 'text', NULL, 3004, NULL, '2026-05-25 13:07:00'),
(14034, 13012, 1001, '目录可以，完整版要等审核结果。', TRUE, '2026-05-25 13:16:00', 'text', NULL, 3004, NULL, '2026-05-25 13:15:00'),
(14035, 13013, 1004, '你上次问的制图板售后，我可以换一套直尺。', TRUE, '2026-05-24 21:11:00', 'text', NULL, 3002, 8010, '2026-05-24 21:10:00'),
(14036, 13013, 1001, '可以，原来的三角尺边缘有裂口。', TRUE, '2026-05-24 21:18:00', 'text', NULL, 3002, 8010, '2026-05-24 21:17:00'),
(14037, 13013, 1004, '明天下午我放到教学楼值班室。', FALSE, NULL, 'text', NULL, 3002, 8010, '2026-05-24 21:30:00'),
(14038, 13014, 1001, '迷你万用表能测电容吗？实验课需要。', TRUE, '2026-05-24 19:26:00', 'text', NULL, 3014, NULL, '2026-05-24 19:25:00'),
(14039, 13014, 1012, '这个型号不测电容，适合基础电压电阻。', TRUE, '2026-05-24 19:35:00', 'text', NULL, 3014, NULL, '2026-05-24 19:34:00'),
(14040, 13014, 1001, '了解，那我再看看实验要求。', TRUE, '2026-05-24 19:46:00', 'text', NULL, 3014, NULL, '2026-05-24 19:45:00'),
(14041, 13015, 1010, '店铺公告里写校园自提优先，周末可以取吗？', TRUE, '2026-05-24 15:17:00', 'text', NULL, NULL, NULL, '2026-05-24 15:16:00'),
(14042, 13015, 1001, '周六可以，周日我不在学校。', FALSE, NULL, 'text', NULL, NULL, NULL, '2026-05-24 15:35:00'),
(14043, 13016, 1001, '你那边客服咨询入口能看到消息中心通知吗？', TRUE, '2026-05-23 20:01:00', 'text', NULL, NULL, NULL, '2026-05-23 20:00:00'),
(14044, 13016, 1002, '可以看到红点，但列表搜索还需要更多样例。', FALSE, NULL, 'text', NULL, NULL, NULL, '2026-05-23 20:20:00')
ON DUPLICATE KEY UPDATE
    conversation_id = VALUES(conversation_id),
    sender_user_id = VALUES(sender_user_id),
    body = VALUES(body),
    is_read = VALUES(is_read),
    read_at = VALUES(read_at),
    message_type = VALUES(message_type),
    media_url = VALUES(media_url),
    product_id = VALUES(product_id),
    order_id = VALUES(order_id),
    created_at = VALUES(created_at);

-- Notifications are compactly surfaced inside the message center, not as a main
-- navigation section.
INSERT INTO notifications (id, user_id, type, title, body, action_url, is_read, created_at)
VALUES
(12001, 1001, 'order_status', 'Order payment reminder', 'Order SEED8002 is waiting for payment confirmation.', '/app/orders', FALSE, '2026-05-25 15:10:00'),
(12002, 1001, 'system', 'Message center enabled', 'Unread messages and order notifications are available in the message center.', '/app/messages', TRUE, '2026-05-24 09:00:00'),
(12003, 1002, 'review_reminder', 'Pending review reminder', 'You have a pending campus review task to check.', '/app/reviews/pending', FALSE, '2026-05-25 12:30:00')
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    type = VALUES(type),
    title = VALUES(title),
    body = VALUES(body),
    action_url = VALUES(action_url),
    is_read = VALUES(is_read),
    created_at = VALUES(created_at);
