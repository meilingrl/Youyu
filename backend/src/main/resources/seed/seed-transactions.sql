-- Idempotent cart + order seed (reserved ids). Safe to re-run under profile `seed`.
DELETE FROM mediation_cases WHERE related_order_id BETWEEN 8001 AND 8010;
DELETE FROM admin_audit_logs WHERE id BETWEEN 92001 AND 92010;
DELETE FROM chat_messages WHERE order_id BETWEEN 8001 AND 8010;
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

INSERT INTO reports (
    id, reporter_user_id, reporter_name, target_type, target_id, target_label,
    reason_type, content, status, submitted_at, processed_at, processed_by,
    resolution, created_at, updated_at
) VALUES
(6010, 1010, 'Seed Buyer', 'order', 8001, 'Order SEED8001',
 'after_sales_dispute', 'Buyer says the drawing tool shipment has not been clarified.',
 'pending', '2026-05-27 09:10:00', NULL, NULL, '',
 '2026-05-27 09:10:00', CURRENT_TIMESTAMP),
(6011, 1010, 'Seed Buyer', 'order', 8008, 'Order SEED8008',
 'payment_delivery_dispute', 'Buyer and seller disagree on whether the offline rack order should proceed.',
 'processing', '2026-05-27 09:20:00', '2026-05-27 09:35:00', 'Admin9001',
 'Escalated to mediation case MED-SEED-8008.',
 '2026-05-27 09:20:00', CURRENT_TIMESTAMP),
(6012, 1001, 'Zhang San', 'order', 8010, 'Order SEED8010',
 'quality_dispute', 'Seller reported a repeated after-sales dispute on the drawing tool order.',
 'resolved', '2026-05-27 09:30:00', '2026-05-27 11:00:00', 'Admin9001',
 'Resolved by mediation case MED-SEED-8010.',
 '2026-05-27 09:30:00', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    target_type = VALUES(target_type),
    target_id = VALUES(target_id),
    target_label = VALUES(target_label),
    reason_type = VALUES(reason_type),
    content = VALUES(content),
    status = VALUES(status),
    processed_at = VALUES(processed_at),
    processed_by = VALUES(processed_by),
    resolution = VALUES(resolution),
    updated_at = VALUES(updated_at);

INSERT INTO mediation_cases (
    id, case_no, source_report_id, related_order_id, buyer_user_id, seller_user_id, reporter_user_id,
    status, decision_category, decision_summary, enforcement_summary, cancel_reason,
    decided_by_admin_user_id, decided_at, created_by_admin_user_id, created_at, updated_at, last_status_changed_at
) VALUES
(70001, 'MED-SEED-8008', 6011, 8008, 1010, 1001, 1010,
 'evidence_review', NULL, NULL, NULL, NULL,
 NULL, NULL, 9001, '2026-05-27 09:35:00', CURRENT_TIMESTAMP, '2026-05-27 09:35:00'),
(70002, 'MED-SEED-8010', 6012, 8010, 1011, 1004, 1001,
 'resolved', 'order_completion_required', 'Seller replacement evidence is sufficient; order completion is required.',
 'Order owner should keep fulfillment evidence available for follow-up.', NULL,
 9001, '2026-05-27 11:00:00', 9001, '2026-05-27 09:45:00', CURRENT_TIMESTAMP, '2026-05-27 11:00:00')
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    decision_category = VALUES(decision_category),
    decision_summary = VALUES(decision_summary),
    enforcement_summary = VALUES(enforcement_summary),
    cancel_reason = VALUES(cancel_reason),
    decided_by_admin_user_id = VALUES(decided_by_admin_user_id),
    decided_at = VALUES(decided_at),
    updated_at = VALUES(updated_at),
    last_status_changed_at = VALUES(last_status_changed_at);

INSERT INTO admin_audit_logs (
    id, operator_user_id, operator_role, action, target_type, target_id, summary, created_at
) VALUES
(92001, 9001, 'ADMIN', 'USER_STATUS_UPDATE', 'USER', 1003,
 'Seed audit: disabled risk account remains under review.', '2026-05-27 08:30:00'),
(92002, 9103, 'REVIEWER', 'STUDENT_VERIFICATION_REVIEW', 'STUDENT_VERIFICATION', 2003,
 'Seed audit: rejected verification due to identity mismatch.', '2026-05-27 08:45:00'),
(92003, 9103, 'REVIEWER', 'PRODUCT_REVIEW_TASK_REVIEW', 'PRODUCT_REVIEW_TASK', 5001,
 'Seed audit: approved digital material after source statement check.', '2026-05-27 09:00:00'),
(92004, 9102, 'SUPPORT_AGENT', 'REPORT_PROCESS', 'REPORT', 6011,
 'Seed audit: escalated order-backed report into mediation context.', '2026-05-27 09:35:00'),
(92005, 9104, 'OPERATOR', 'SEARCH_GOVERNANCE_RULE_UPDATE', 'SEARCH_GOVERNANCE_RULE', 1,
 'Seed audit: reviewed hot-search governance rule state.', '2026-05-27 10:10:00'),
(92006, 9001, 'ADMIN', 'SHOP_STATUS_UPDATE', 'SHOP', 4003,
 'Seed audit: pending shop kept in review queue for local verification.', '2026-05-27 10:40:00')
ON DUPLICATE KEY UPDATE
    operator_user_id = VALUES(operator_user_id),
    operator_role = VALUES(operator_role),
    action = VALUES(action),
    target_type = VALUES(target_type),
    target_id = VALUES(target_id),
    summary = VALUES(summary),
    created_at = VALUES(created_at);
