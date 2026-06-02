-- Spend history for zhangsan (user_id=1001) as buyer across multiple months.
-- Orders are COMPLETED and PAID to appear in spend visualization charts.

INSERT INTO orders (
    id, order_no, buyer_user_id, seller_user_id, shop_id, order_status, fulfillment_type, payment_status,
    goods_amount, discount_amount, pay_amount, buyer_note, submitted_at, paid_at, completed_at, cancelled_at, closed_reason
) VALUES
-- January 2026
(8801, 'ZS202601A', 1001, 1004, 4002, 'completed', 'offline', 'paid',
    45.00, 0.00, 45.00, '', '2026-01-08 14:00:00', '2026-01-08 14:05:00', '2026-01-10 10:00:00', NULL, ''),
(8802, 'ZS202601B', 1001, 1010, 4010, 'completed', 'logistics', 'paid',
    22.00, 0.00, 22.00, '', '2026-01-15 09:30:00', '2026-01-15 09:35:00', '2026-01-18 15:00:00', NULL, ''),
-- February 2026
(8803, 'ZS202602A', 1001, 1004, 4002, 'completed', 'offline', 'paid',
    68.50, 0.00, 68.50, '开学采购', '2026-02-20 11:00:00', '2026-02-20 11:05:00', '2026-02-22 14:00:00', NULL, ''),
(8804, 'ZS202602B', 1001, 1010, 4010, 'completed', 'logistics', 'paid',
    16.50, 0.00, 16.50, '', '2026-02-25 16:00:00', '2026-02-25 16:10:00', '2026-02-28 09:00:00', NULL, ''),
-- March 2026
(8805, 'ZS202603A', 1001, 1004, 4002, 'completed', 'offline', 'paid',
    35.00, 0.00, 35.00, '', '2026-03-05 10:00:00', '2026-03-05 10:05:00', '2026-03-06 12:00:00', NULL, ''),
(8806, 'ZS202603B', 1001, 1010, 4010, 'completed', 'digital', 'paid',
    19.90, 0.00, 19.90, '', '2026-03-12 14:30:00', '2026-03-12 14:35:00', '2026-03-12 14:40:00', NULL, ''),
(8807, 'ZS202603C', 1001, 1002, NULL, 'completed', 'offline', 'paid',
    88.00, 5.00, 83.00, '课本', '2026-03-20 09:00:00', '2026-03-20 09:05:00', '2026-03-21 16:00:00', NULL, ''),
-- April 2026
(8808, 'ZS202604A', 1001, 1004, 4002, 'completed', 'logistics', 'paid',
    120.00, 10.00, 110.00, '制图套装', '2026-04-02 08:00:00', '2026-04-02 08:10:00', '2026-04-05 12:00:00', NULL, ''),
(8809, 'ZS202604B', 1001, 1010, 4010, 'completed', 'offline', 'paid',
    44.00, 0.00, 44.00, '', '2026-04-15 13:00:00', '2026-04-15 13:05:00', '2026-04-16 10:00:00', NULL, ''),
(8810, 'ZS202604C', 1001, 1002, NULL, 'completed', 'offline', 'paid',
    25.00, 0.00, 25.00, '', '2026-04-22 17:00:00', '2026-04-22 17:05:00', '2026-04-23 11:00:00', NULL, ''),
-- May 2026
(8811, 'ZS202605A', 1001, 1004, 4002, 'completed', 'offline', 'paid',
    58.00, 0.00, 58.00, '期末复习资料', '2026-05-03 10:00:00', '2026-05-03 10:05:00', '2026-05-04 09:00:00', NULL, ''),
(8812, 'ZS202605B', 1001, 1010, 4010, 'completed', 'logistics', 'paid',
    33.00, 0.00, 33.00, '', '2026-05-10 11:00:00', '2026-05-10 11:05:00', '2026-05-13 14:00:00', NULL, ''),
(8813, 'ZS202605C', 1001, 1002, NULL, 'completed', 'digital', 'paid',
    9.90, 0.00, 9.90, '', '2026-05-18 15:00:00', '2026-05-18 15:05:00', '2026-05-18 15:10:00', NULL, ''),
(8814, 'ZS202605D', 1001, 1004, 4002, 'completed', 'offline', 'paid',
    75.00, 5.00, 70.00, '毕设参考书', '2026-05-25 09:00:00', '2026-05-25 09:10:00', '2026-05-26 10:00:00', NULL, ''),
-- June 2026
(8815, 'ZS202606A', 1001, 1010, 4010, 'completed', 'offline', 'paid',
    22.00, 0.00, 22.00, '', '2026-06-01 10:00:00', '2026-06-01 10:05:00', '2026-06-01 14:00:00', NULL, '')
ON DUPLICATE KEY UPDATE order_status = VALUES(order_status), payment_status = VALUES(payment_status);

INSERT INTO order_items (
    id, order_id, product_id, title_snapshot, image_snapshot, price_snapshot, quantity, subtotal_amount, product_type_snapshot, created_at
) VALUES
(8901, 8801, 3002, 'Engineering Drawing Tool Set', '', 45.00, 1, 45.00, 'physical', '2026-01-08 14:00:00'),
(8902, 8802, 3010, 'Seed USB Hub 4-Port', '', 22.00, 1, 22.00, 'physical', '2026-01-15 09:30:00'),
(8903, 8803, 3002, 'Engineering Drawing Tool Set', '', 35.00, 1, 35.00, 'physical', '2026-02-20 11:00:00'),
(8904, 8803, 3004, 'CET-6 Vocabulary Book', '', 33.50, 1, 33.50, 'physical', '2026-02-20 11:00:00'),
(8905, 8804, 3011, 'Seed Wireless Mouse', '', 16.50, 1, 16.50, 'physical', '2026-02-25 16:00:00'),
(8906, 8805, 3002, 'Engineering Drawing Tool Set', '', 35.00, 1, 35.00, 'physical', '2026-03-05 10:00:00'),
(8907, 8806, 3001, 'Advanced Math Review Pack', '', 19.90, 1, 19.90, 'digital', '2026-03-12 14:30:00'),
(8908, 8807, 3004, 'CET-6 Vocabulary Book', '', 33.50, 2, 67.00, 'physical', '2026-03-20 09:00:00'),
(8909, 8808, 3002, 'Engineering Drawing Tool Set', '', 120.00, 1, 120.00, 'physical', '2026-04-02 08:00:00'),
(8910, 8809, 3010, 'Seed USB Hub 4-Port', '', 22.00, 2, 44.00, 'physical', '2026-04-15 13:00:00'),
(8911, 8810, 3003, 'Dorm Storage Rack', '', 25.00, 1, 25.00, 'physical', '2026-04-22 17:00:00'),
(8912, 8811, 3001, 'Advanced Math Review Pack', '', 19.90, 2, 39.80, 'digital', '2026-05-03 10:00:00'),
(8913, 8811, 3003, 'Dorm Storage Rack', '', 18.00, 1, 18.00, 'physical', '2026-05-03 10:00:00'),
(8914, 8812, 3010, 'Seed USB Hub 4-Port', '', 22.00, 1, 22.00, 'physical', '2026-05-10 11:00:00'),
(8915, 8812, 3011, 'Seed Wireless Mouse', '', 11.00, 1, 11.00, 'physical', '2026-05-10 11:00:00'),
(8916, 8813, 3001, 'Advanced Math Review Pack', '', 9.90, 1, 9.90, 'digital', '2026-05-18 15:00:00'),
(8917, 8814, 3004, 'CET-6 Vocabulary Book', '', 33.50, 2, 67.00, 'physical', '2026-05-25 09:00:00'),
(8918, 8815, 3010, 'Seed USB Hub 4-Port', '', 22.00, 1, 22.00, 'physical', '2026-06-01 10:00:00')
ON DUPLICATE KEY UPDATE title_snapshot = VALUES(title_snapshot);

INSERT INTO payment_records (
    id, order_id, user_id, payment_method, payment_channel, amount, currency, status, paid_at, created_at, updated_at
) VALUES
(8701, 8801, 1001, 'platform_balance', 'internal', 45.00, 'CNY', 'success', '2026-01-08 14:05:00', '2026-01-08 14:05:00', CURRENT_TIMESTAMP),
(8702, 8802, 1001, 'platform_balance', 'internal', 22.00, 'CNY', 'success', '2026-01-15 09:35:00', '2026-01-15 09:35:00', CURRENT_TIMESTAMP),
(8703, 8803, 1001, 'platform_balance', 'internal', 68.50, 'CNY', 'success', '2026-02-20 11:05:00', '2026-02-20 11:05:00', CURRENT_TIMESTAMP),
(8704, 8804, 1001, 'platform_balance', 'internal', 16.50, 'CNY', 'success', '2026-02-25 16:10:00', '2026-02-25 16:10:00', CURRENT_TIMESTAMP),
(8705, 8805, 1001, 'platform_balance', 'internal', 35.00, 'CNY', 'success', '2026-03-05 10:05:00', '2026-03-05 10:05:00', CURRENT_TIMESTAMP),
(8706, 8806, 1001, 'platform_balance', 'internal', 19.90, 'CNY', 'success', '2026-03-12 14:35:00', '2026-03-12 14:35:00', CURRENT_TIMESTAMP),
(8707, 8807, 1001, 'platform_balance', 'internal', 83.00, 'CNY', 'success', '2026-03-20 09:05:00', '2026-03-20 09:05:00', CURRENT_TIMESTAMP),
(8708, 8808, 1001, 'platform_balance', 'internal', 110.00, 'CNY', 'success', '2026-04-02 08:10:00', '2026-04-02 08:10:00', CURRENT_TIMESTAMP),
(8709, 8809, 1001, 'platform_balance', 'internal', 44.00, 'CNY', 'success', '2026-04-15 13:05:00', '2026-04-15 13:05:00', CURRENT_TIMESTAMP),
(8710, 8810, 1001, 'platform_balance', 'internal', 25.00, 'CNY', 'success', '2026-04-22 17:05:00', '2026-04-22 17:05:00', CURRENT_TIMESTAMP),
(8711, 8811, 1001, 'platform_balance', 'internal', 58.00, 'CNY', 'success', '2026-05-03 10:05:00', '2026-05-03 10:05:00', CURRENT_TIMESTAMP),
(8712, 8812, 1001, 'platform_balance', 'internal', 33.00, 'CNY', 'success', '2026-05-10 11:05:00', '2026-05-10 11:05:00', CURRENT_TIMESTAMP),
(8713, 8813, 1001, 'platform_balance', 'internal', 9.90, 'CNY', 'success', '2026-05-18 15:05:00', '2026-05-18 15:05:00', CURRENT_TIMESTAMP),
(8714, 8814, 1001, 'platform_balance', 'internal', 70.00, 'CNY', 'success', '2026-05-25 09:10:00', '2026-05-25 09:10:00', CURRENT_TIMESTAMP),
(8715, 8815, 1001, 'platform_balance', 'internal', 22.00, 'CNY', 'success', '2026-06-01 10:05:00', '2026-06-01 10:05:00', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE status = VALUES(status);
