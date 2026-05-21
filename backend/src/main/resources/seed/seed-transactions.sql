-- Idempotent cart + order seed (reserved ids). Safe to re-run under profile `seed`.
DELETE FROM order_items WHERE order_id BETWEEN 8001 AND 8010;
DELETE FROM order_fulfillments WHERE order_id BETWEEN 8001 AND 8010;
DELETE FROM orders WHERE id BETWEEN 8001 AND 8010;
DELETE FROM cart_items WHERE id BETWEEN 9101 AND 9110;

-- Carts for seed buyers (avoid user_id=1001; TransactionDataStore.seedCart owns that slot).
INSERT INTO cart_items (id, user_id, product_id, quantity, selected, created_at, updated_at)
VALUES
(9101, 1010, 3002, 1, TRUE, '2026-05-10 10:15:00', CURRENT_TIMESTAMP),
(9102, 1010, 3010, 2, TRUE, '2026-05-10 10:16:00', CURRENT_TIMESTAMP),
(9103, 1011, 3003, 1, FALSE, '2026-05-10 10:17:00', CURRENT_TIMESTAMP),
(9104, 1010, 3011, 1, FALSE, '2026-05-10 10:18:00', CURRENT_TIMESTAMP),
(9105, 1010, 3001, 1, TRUE, '2026-05-10 10:19:00', CURRENT_TIMESTAMP),
(9106, 1011, 3010, 2, TRUE, '2026-05-10 10:20:00', CURRENT_TIMESTAMP),
(9107, 1012, 3002, 1, TRUE, '2026-05-10 10:21:00', CURRENT_TIMESTAMP),
(9108, 1012, 3013, 3, TRUE, '2026-05-10 10:22:00', CURRENT_TIMESTAMP),
(9109, 1013, 3012, 1, FALSE, '2026-05-10 10:23:00', CURRENT_TIMESTAMP),
(9110, 1013, 3001, 1, TRUE, '2026-05-10 10:24:00', CURRENT_TIMESTAMP);

-- Orders: pending_payment / unpaid, aligned with TransactionDataStore.createOrder initial rows.
INSERT INTO orders (
    id, order_no, buyer_user_id, seller_user_id, shop_id, order_status, fulfillment_type, payment_status,
    goods_amount, discount_amount, pay_amount, buyer_note, submitted_at, paid_at, completed_at, cancelled_at, closed_reason
) VALUES
(8001, 'SEED8001', 1010, 1004, 4002, 'pending_payment', 'logistics', 'unpaid',
    35.00, 0.00, 35.00, 'seed logistics', '2026-05-10 10:20:00', NULL, NULL, NULL, ''),
(8002, 'SEED8002', 1010, 1001, 4001, 'pending_payment', 'digital', 'unpaid',
    19.90, 0.00, 19.90, '', '2026-05-10 10:21:00', NULL, NULL, NULL, ''),
(8003, 'SEED8003', 1011, 1001, NULL, 'pending_payment', 'offline', 'unpaid',
    18.00, 0.00, 18.00, 'seed offline meet', '2026-05-10 10:22:00', NULL, NULL, NULL, ''),
(8004, 'SEED8004', 1010, 1004, 4002, 'pending_payment', 'offline', 'unpaid',
    35.00, 0.00, 35.00, 'second seed offline', '2026-05-10 10:25:00', NULL, NULL, NULL, ''),
(8005, 'SEED8005', 1012, 1001, 4001, 'pending_payment', 'digital', 'unpaid',
    19.90, 0.00, 19.90, '', '2026-05-10 10:26:00', NULL, NULL, NULL, ''),
(8006, 'SEED8006', 1011, 1010, 4010, 'pending_payment', 'logistics', 'unpaid',
    44.00, 0.00, 44.00, 'two hubs', '2026-05-10 10:27:00', NULL, NULL, NULL, ''),
(8007, 'SEED8007', 1011, 1010, 4010, 'pending_payment', 'offline', 'unpaid',
    16.50, 0.00, 16.50, 'meet at dorm', '2026-05-10 10:28:00', NULL, NULL, NULL, ''),
(8008, 'SEED8008', 1010, 1001, NULL, 'pending_payment', 'offline', 'unpaid',
    18.00, 0.00, 18.00, 'rack pickup', '2026-05-10 10:29:00', NULL, NULL, NULL, ''),
(8009, 'SEED8009', 1013, 1001, 4001, 'pending_payment', 'digital', 'unpaid',
    19.90, 0.00, 19.90, '', '2026-05-10 10:30:00', NULL, NULL, NULL, ''),
(8010, 'SEED8010', 1011, 1004, 4002, 'pending_payment', 'logistics', 'unpaid',
    35.00, 0.00, 35.00, '', '2026-05-10 10:31:00', NULL, NULL, NULL, '');

INSERT INTO order_items (
    id, order_id, product_id, title_snapshot, image_snapshot, price_snapshot, quantity, subtotal_amount, product_type_snapshot, created_at
) VALUES
(8101, 8001, 3002, 'Engineering Drawing Tool Set', 'https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=900&q=80', 35.00, 1, 35.00, 'physical', '2026-05-10 10:20:00'),
(8102, 8002, 3001, 'Advanced Math Review Pack', 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&w=900&q=80', 19.90, 1, 19.90, 'digital', '2026-05-10 10:21:00'),
(8103, 8003, 3003, 'Dorm Storage Rack', 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=900&q=80', 18.00, 1, 18.00, 'physical', '2026-05-10 10:22:00'),
(8104, 8004, 3002, 'Engineering Drawing Tool Set', 'https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=900&q=80', 35.00, 1, 35.00, 'physical', '2026-05-10 10:25:00'),
(8105, 8005, 3001, 'Advanced Math Review Pack', 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&w=900&q=80', 19.90, 1, 19.90, 'digital', '2026-05-10 10:26:00'),
(8106, 8006, 3010, 'Seed USB Hub 4-Port', 'https://images.unsplash.com/photo-1625948515291-69613efd103f?auto=format&fit=crop&w=900&q=80', 22.00, 2, 44.00, 'physical', '2026-05-10 10:27:00'),
(8107, 8007, 3011, 'Seed Wireless Mouse', 'https://images.unsplash.com/photo-1527814050087-3793815479db?auto=format&fit=crop&w=900&q=80', 16.50, 1, 16.50, 'physical', '2026-05-10 10:28:00'),
(8108, 8008, 3003, 'Dorm Storage Rack', 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=900&q=80', 18.00, 1, 18.00, 'physical', '2026-05-10 10:29:00'),
(8109, 8009, 3001, 'Advanced Math Review Pack', 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&w=900&q=80', 19.90, 1, 19.90, 'digital', '2026-05-10 10:30:00'),
(8110, 8010, 3002, 'Engineering Drawing Tool Set', 'https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=900&q=80', 35.00, 1, 35.00, 'physical', '2026-05-10 10:31:00');

INSERT INTO order_fulfillments (
    id, order_id, fulfillment_type, fulfillment_status, seller_confirmed_at, buyer_confirmed_at, buyer_note, address_snapshot,
    logistics_no, logistics_company, shipped_at, offline_meeting_time, offline_meeting_place,
    offline_seller_confirmed, offline_buyer_confirmed, preview_rule_snapshot, download_access_status, digital_access_opened_at
) VALUES
(8501, 8001, 'logistics', 'pending_action', NULL, NULL, 'seed logistics', '',
    '', '', NULL, '', '', FALSE, FALSE, '', 'not_applicable', NULL),
(8502, 8002, 'digital', 'pending_action', NULL, NULL, '', '',
    '', '', NULL, '', '', FALSE, FALSE, 'Limited preview before purchase; full download after receipt confirmation', 'preview_only', NULL),
(8503, 8003, 'offline', 'pending_action', NULL, NULL, 'seed offline meet', '',
    '', '', NULL, '2026-06-01 14:00', 'NEU Gate A', FALSE, FALSE, '', 'not_applicable', NULL),
(8504, 8004, 'offline', 'pending_action', NULL, NULL, 'second seed offline', '',
    '', '', NULL, '2026-06-02 18:30', 'NEU South Gate', FALSE, FALSE, '', 'not_applicable', NULL),
(8505, 8005, 'digital', 'pending_action', NULL, NULL, '', '',
    '', '', NULL, '', '', FALSE, FALSE, 'Limited preview before purchase; full download after receipt confirmation', 'preview_only', NULL),
(8506, 8006, 'logistics', 'pending_action', NULL, NULL, 'two hubs', '',
    '', '', NULL, '', '', FALSE, FALSE, '', 'not_applicable', NULL),
(8507, 8007, 'offline', 'pending_action', NULL, NULL, 'meet at dorm', '',
    '', '', NULL, '2026-06-03 12:00', 'Dorm 7 lobby', FALSE, FALSE, '', 'not_applicable', NULL),
(8508, 8008, 'offline', 'pending_action', NULL, NULL, 'rack pickup', '',
    '', '', NULL, '2026-06-04 19:00', 'NEU bike shed', FALSE, FALSE, '', 'not_applicable', NULL),
(8509, 8009, 'digital', 'pending_action', NULL, NULL, '', '',
    '', '', NULL, '', '', FALSE, FALSE, 'Limited preview before purchase; full download after receipt confirmation', 'preview_only', NULL),
(8510, 8010, 'logistics', 'pending_action', NULL, NULL, '', '',
    '', '', NULL, '', '', FALSE, FALSE, '', 'not_applicable', NULL);
