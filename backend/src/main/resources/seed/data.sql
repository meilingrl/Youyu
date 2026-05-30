INSERT INTO users (id, username, password_hash, nickname, status, role, registered_at, created_at, updated_at)
VALUES (9001, 'admin',
    '$2a$10$jJy4r2olYY7ca7bAvZRuJe9Z77E.JZxzVTugqYw6S8lr4ahKU2hqG',
    '平台管理员', 'active', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE username = VALUES(username), password_hash = VALUES(password_hash),
    nickname = VALUES(nickname), status = VALUES(status), role = VALUES(role);

INSERT INTO users (id, username, password_hash, nickname, status, role, registered_at, created_at, updated_at)
VALUES
(9101, 'superadmin', '$2a$10$jJy4r2olYY7ca7bAvZRuJe9Z77E.JZxzVTugqYw6S8lr4ahKU2hqG', 'Super Admin', 'active', 'SUPER_ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9102, 'supportagent', '$2a$10$jJy4r2olYY7ca7bAvZRuJe9Z77E.JZxzVTugqYw6S8lr4ahKU2hqG', 'Support Agent', 'active', 'SUPPORT_AGENT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9103, 'reviewer', '$2a$10$jJy4r2olYY7ca7bAvZRuJe9Z77E.JZxzVTugqYw6S8lr4ahKU2hqG', 'Reviewer', 'active', 'REVIEWER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9104, 'operator', '$2a$10$jJy4r2olYY7ca7bAvZRuJe9Z77E.JZxzVTugqYw6S8lr4ahKU2hqG', 'Operator', 'active', 'OPERATOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9105, 'orderadmin', '$2a$10$jJy4r2olYY7ca7bAvZRuJe9Z77E.JZxzVTugqYw6S8lr4ahKU2hqG', 'Order Admin', 'active', 'ORDER_ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE username = VALUES(username), password_hash = VALUES(password_hash),
    nickname = VALUES(nickname), status = VALUES(status), role = VALUES(role);

INSERT INTO users (id, username, phone, email, password_hash, nickname, avatar, status, registered_at, last_login_at, created_at, updated_at)
VALUES
(1001, 'zhangsan', '13800000001', 'zhangsan@campus.edu.cn', '$2a$10$gtBIsqnrdZkJaiT0V4WbUOoYcJkcUQAJINfCwagPVRQEjFW8GM79O', 'Zhang San', '', 'active', '2026-04-01 10:00:00', '2026-05-09 09:20:00', '2026-04-01 10:00:00', CURRENT_TIMESTAMP),
(1002, 'lisi', '13800000002', 'lisi@campus.edu.cn', '$2a$10$pwkJs7I8pFZxYbs6Ndnzfu7tevMJZw5p6p6H5hZvN6qfkR1L1CxSe', 'Li Si', '', 'active', '2026-04-18 14:10:00', '2026-05-08 18:05:00', '2026-04-18 14:10:00', CURRENT_TIMESTAMP),
(1003, 'wangwu', '13800000003', 'wangwu@campus.edu.cn', '$2a$10$n5H6NcLtliSZix028EOYy.bwmgUAyTm2a39fAoCMmT2GjurlgBIr.', 'Wang Wu', '', 'disabled', '2026-03-06 11:20:00', '2026-05-07 20:00:00', '2026-03-06 11:20:00', CURRENT_TIMESTAMP),
(1004, 'zhaoliu', '13800000004', 'zhaoliu@campus.edu.cn', '$2a$10$4EyFZNGU6azp27ZOrKQ73uCmQZ8jmx.0U4WhCbG3GdFa4bPg4D6XG', 'Zhao Liu', '', 'active', '2026-02-15 09:45:00', '2026-05-09 08:30:00', '2026-02-15 09:45:00', CURRENT_TIMESTAMP),
(1010, 'seedbuyer', '13800000010', 'seedbuyer@campus.edu.cn', '$2a$10$hPahEgkO9/nkJVoY5etkIO7tO50pBlujmFhcAWJ6Pw.kDpzDu7nJG', 'Seed Buyer', '', 'active', '2026-05-10 08:00:00', NULL, '2026-05-10 08:00:00', CURRENT_TIMESTAMP),
(1011, 'seedbuyer2', '13800000011', 'seedbuyer2@campus.edu.cn', '$2a$10$VbnVgh/nPlDNnYeuos5HkePah.XJAmzxZaEk6JTYrqgxduBO7Aep.', 'Seed Buyer Two', '', 'active', '2026-05-10 08:05:00', NULL, '2026-05-10 08:05:00', CURRENT_TIMESTAMP),
(1012, 'seedbuyer3', '13800000012', 'seedbuyer3@campus.edu.cn', '$2a$10$hAEXKhS/5z9bHhc.q62tV.bPdScZg84hUJcfmvLkoWi/58/Eqjys2', 'Seed Buyer Three', '', 'active', '2026-05-10 08:10:00', NULL, '2026-05-10 08:10:00', CURRENT_TIMESTAMP),
(1013, 'seedbuyer4', '13800000013', 'seedbuyer4@campus.edu.cn', '$2a$10$Qvr6K2FMf5wIneefReslV.4.y05Q7yvQECBqSoD9ExdmOTZWGUpcO', 'Seed Buyer Four', '', 'active', '2026-05-10 08:12:00', NULL, '2026-05-10 08:12:00', CURRENT_TIMESTAMP);

-- Platform online customer-service identity. Support conversations always pair the
-- requester (user_a) with this account (user_b); admins reply on its behalf.
INSERT INTO users (id, username, phone, email, password_hash, nickname, avatar, status, registered_at, last_login_at, created_at, updated_at)
VALUES
(1099, 'platform_cs', '13800001099', 'cs@campus.edu.cn', '$2a$10$jJy4r2olYY7ca7bAvZRuJe9Z77E.JZxzVTugqYw6S8lr4ahKU2hqG', '平台客服', '', 'active', '2026-04-01 09:00:00', NULL, '2026-04-01 09:00:00', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE username = VALUES(username), nickname = VALUES(nickname), status = VALUES(status);

INSERT INTO user_privilege_profiles (user_id, can_purchase, can_publish, can_review, can_apply_shop, is_restricted, restricted_reason, credit_level, created_at, updated_at)
VALUES
(1001, TRUE, TRUE, TRUE, TRUE, FALSE, '', 'L2 stable', '2026-04-01 10:00:00', CURRENT_TIMESTAMP),
(1002, TRUE, FALSE, TRUE, FALSE, FALSE, '', 'L0 new', '2026-04-18 14:10:00', CURRENT_TIMESTAMP),
(1003, TRUE, FALSE, TRUE, FALSE, TRUE, 'pending risk review', 'L1 verified', '2026-03-06 11:20:00', CURRENT_TIMESTAMP),
(1004, TRUE, TRUE, TRUE, FALSE, FALSE, '', 'L1 verified', '2026-02-15 09:45:00', CURRENT_TIMESTAMP),
(1010, TRUE, TRUE, TRUE, TRUE, FALSE, '', 'L1 verified', '2026-05-10 08:00:00', CURRENT_TIMESTAMP),
(1011, TRUE, FALSE, TRUE, FALSE, FALSE, '', 'L0 new', '2026-05-10 08:05:00', CURRENT_TIMESTAMP),
(1012, TRUE, TRUE, TRUE, TRUE, FALSE, '', 'L1 verified', '2026-05-10 08:10:00', CURRENT_TIMESTAMP),
(1013, TRUE, TRUE, TRUE, FALSE, FALSE, '', 'L1 verified', '2026-05-10 08:12:00', CURRENT_TIMESTAMP);

INSERT INTO student_verifications (id, user_id, student_no, real_name, college, major, grade, campus_email, verification_method, verification_status, submitted_at, reviewed_at, reviewer_id, reject_reason, review_note, risk_flag, created_at, updated_at)
VALUES
(2001, 1001, '20240001', 'Zhang San', 'Computer Science', 'Software Engineering', '2024', 'zhangsan@campus.edu.cn', 'manual_review', 'approved', '2026-04-02 09:00:00', '2026-04-02 16:40:00', 9001, '', 'approved', FALSE, '2026-04-02 09:00:00', CURRENT_TIMESTAMP),
(2002, 1002, '20240002', 'Li Si', 'Foreign Languages', 'English', '2024', 'lisi@campus.edu.cn', 'manual_review', 'pending_review', '2026-05-08 13:20:00', NULL, NULL, '', '', FALSE, '2026-05-08 13:20:00', CURRENT_TIMESTAMP),
(2003, 1003, '20230011', 'Wang Wu', 'Management', 'Business Administration', '2023', 'wangwu@campus.edu.cn', 'manual_review', 'rejected', '2026-05-06 15:00:00', '2026-05-07 10:10:00', 9001, 'identity mismatch', 'please resubmit valid proof', TRUE, '2026-05-06 15:00:00', CURRENT_TIMESTAMP),
(2004, 1010, '20240110', 'Seed Buyer', 'Computer Science', 'Software Engineering', '2024', 'seedbuyer@campus.edu.cn', 'manual_review', 'approved', '2026-05-10 09:00:00', '2026-05-10 09:30:00', 9001, '', 'seed approved', FALSE, '2026-05-10 09:00:00', CURRENT_TIMESTAMP),
(2005, 1011, '20240111', 'Seed Buyer Two', 'Economics', 'Finance', '2024', 'seedbuyer2@campus.edu.cn', 'manual_review', 'pending_review', '2026-05-10 09:10:00', NULL, NULL, '', '', FALSE, '2026-05-10 09:10:00', CURRENT_TIMESTAMP),
(2006, 1012, '20240112', 'Seed Buyer Three', 'Mechanical Engineering', 'Mechatronics', '2024', 'seedbuyer3@campus.edu.cn', 'manual_review', 'approved', '2026-05-10 09:15:00', '2026-05-10 09:45:00', 9001, '', 'seed batch', FALSE, '2026-05-10 09:15:00', CURRENT_TIMESTAMP),
(2007, 1013, '20240113', 'Seed Buyer Four', 'Automation', 'Robotics', '2025', 'seedbuyer4@campus.edu.cn', 'manual_review', 'approved', '2026-05-10 09:18:00', '2026-05-10 09:50:00', 9001, '', 'seed batch', FALSE, '2026-05-10 09:18:00', CURRENT_TIMESTAMP);

INSERT INTO user_addresses (id, user_id, receiver_name, receiver_phone, address_type, province, city, district, detail_address, campus_area, is_default, created_at, updated_at)
VALUES
(7001, 1001, 'Zhang San', '13800000001', 'campus', 'Liaoning', 'Shenyang', 'Hunnan', 'NEU Hunnan Campus Dorm 1', 'Hunnan Campus', TRUE, '2026-04-03 12:00:00', CURRENT_TIMESTAMP),
(7002, 1010, 'Seed Buyer', '13800000010', 'campus', 'Liaoning', 'Shenyang', 'Hunnan', 'NEU Hunnan Campus Dorm 7', 'Hunnan Campus', TRUE, '2026-05-10 09:00:00', CURRENT_TIMESTAMP),
(7003, 1011, 'Seed Buyer Two', '13800000011', 'campus', 'Liaoning', 'Shenyang', 'Hunnan', 'NEU Hunnan Library Annex', 'Hunnan Campus', TRUE, '2026-05-10 09:05:00', CURRENT_TIMESTAMP),
(7004, 1012, 'Seed Buyer Three', '13800000012', 'campus', 'Liaoning', 'Shenyang', 'Hunnan', 'NEU Gym Locker 12', 'Hunnan Campus', TRUE, '2026-05-10 09:20:00', CURRENT_TIMESTAMP),
(7005, 1013, 'Seed Buyer Four', '13800000013', 'campus', 'Liaoning', 'Shenyang', 'Hunnan', 'NEU Innovation Building B1', 'Hunnan Campus', TRUE, '2026-05-10 09:22:00', CURRENT_TIMESTAMP);

INSERT INTO categories (id, parent_id, name, status, sort_order, description)
VALUES
(1, NULL, 'Learning Materials', 'enabled', 10, 'Textbooks, notes and review materials'),
(2, NULL, 'Learning Tools', 'enabled', 20, 'Calculators, drawing tools and major-specific tools'),
(3, NULL, 'Dorm Life', 'enabled', 30, 'Dorm and living items'),
(4, NULL, 'Digital Accessories', 'enabled', 40, 'Keyboard, mouse, headset and storage devices');

INSERT INTO shops (id, owner_user_id, name, description, avatar_url, cover_url, announcement, status, review_status, reviewed_at, reviewed_by, reject_reason, rating_score, follower_count, is_deleted, created_at, updated_at)
VALUES
(4001, 1001, 'Course Notes Shop', 'Self-organized course notes and review materials.', NULL, 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=900&q=80', 'Digital materials are listed only after review.', 'active', 'approved', CURRENT_TIMESTAMP, 9001, NULL, 4.90, 18, FALSE, '2026-04-03 12:00:00', CURRENT_TIMESTAMP),
(4002, 1004, 'Photo Study Room', 'Photography notes and a few second-hand tools.', NULL, 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=900&q=80', 'Campus pickup is supported.', 'active', 'approved', CURRENT_TIMESTAMP, 9001, NULL, 4.80, 12, FALSE, '2026-04-12 09:00:00', CURRENT_TIMESTAMP),
(4003, 1002, 'Pending Campus Shop', 'A shop application waiting for review.', NULL, NULL, NULL, 'inactive', 'pending_review', NULL, NULL, NULL, 5.00, 0, FALSE, '2026-05-09 10:00:00', CURRENT_TIMESTAMP),
(4010, 1010, 'Seed Secondhand Corner', 'Seed shop for JDBC demo and buyer-owned listings.', NULL, 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80', 'Campus pickup preferred.', 'active', 'approved', CURRENT_TIMESTAMP, 9001, NULL, 4.95, 3, FALSE, '2026-05-10 08:00:00', CURRENT_TIMESTAMP),
(4011, 1012, 'Seed Robotics Mart', 'Second-hand lab tools and cables from seed user 1012.', NULL, 'https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?auto=format&fit=crop&w=900&q=80', 'Meet at maker space after 6pm.', 'active', 'approved', CURRENT_TIMESTAMP, 9001, NULL, 4.70, 1, FALSE, '2026-05-10 08:12:00', CURRENT_TIMESTAMP);

INSERT INTO shop_capability_profiles (id, shop_id, capability_level, max_active_product_count, can_config_announcement, can_config_loyalty_offer, can_issue_light_coupon, can_join_platform_activity, created_at, updated_at)
VALUES
(4101, 4001, 'basic', 20, TRUE, FALSE, FALSE, FALSE, '2026-04-03 12:00:00', CURRENT_TIMESTAMP),
(4102, 4002, 'basic', 20, TRUE, FALSE, FALSE, FALSE, '2026-04-12 09:00:00', CURRENT_TIMESTAMP),
(4103, 4003, 'basic', 20, TRUE, FALSE, FALSE, FALSE, '2026-05-09 10:00:00', CURRENT_TIMESTAMP),
(4110, 4010, 'basic', 20, TRUE, FALSE, FALSE, FALSE, '2026-05-10 08:00:00', CURRENT_TIMESTAMP),
(4111, 4011, 'basic', 20, TRUE, FALSE, FALSE, FALSE, '2026-05-10 08:12:00', CURRENT_TIMESTAMP);

INSERT INTO products (id, seller_user_id, shop_id, category_id, title, subtitle, description, detail_content, product_type, status, review_status, review_reject_reason, main_image_url, sale_price, original_price, stock_quantity, supports_logistics, supports_offline_delivery, supports_digital_delivery, allow_preview, preview_rule_text, view_count, favorite_count, is_deleted, created_at, updated_at)
VALUES
(3001, 1001, 4001, 1, 'Advanced Math Review Pack', 'Self-organized notes and exercise index', 'Self-organized advanced math materials with limited preview.', 'Self-organized advanced math materials with limited preview.', 'digital', 'on_sale', 'approved', NULL, 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&w=900&q=80', 19.90, 29.90, 20, FALSE, FALSE, TRUE, TRUE, 'Limited preview before purchase; full download after receipt confirmation', 156, 28, FALSE, '2026-05-01 09:00:00', CURRENT_TIMESTAMP),
(3002, 1004, 4002, 2, 'Engineering Drawing Tool Set', 'Ruler and template set in good condition', 'Suitable for engineering drawing courses.', 'Suitable for engineering drawing courses.', 'physical', 'on_sale', 'not_required', NULL, 'https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=900&q=80', 35.00, 58.00, 1, TRUE, TRUE, FALSE, FALSE, NULL, 88, 10, FALSE, '2026-05-03 09:00:00', CURRENT_TIMESTAMP),
(3003, 1001, NULL, 3, 'Dorm Storage Rack', 'Foldable, campus pickup', 'Second-hand dorm storage rack.', 'Second-hand dorm storage rack.', 'physical', 'on_sale', 'not_required', NULL, 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=900&q=80', 18.00, 39.00, 1, FALSE, TRUE, FALSE, FALSE, NULL, 43, 5, FALSE, '2026-05-04 09:00:00', CURRENT_TIMESTAMP),
(3004, 1001, 4001, 1, 'Data Structure Notes', 'Digital material waiting for review', 'Self-organized data structure notes awaiting admin review.', 'Self-organized data structure notes awaiting admin review.', 'digital', 'off_sale', 'pending_review', NULL, 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=900&q=80', 15.00, 20.00, 10, FALSE, FALSE, TRUE, TRUE, 'Preview opens after review approval', 12, 1, FALSE, '2026-05-08 12:00:00', CURRENT_TIMESTAMP),
(3010, 1010, 4010, 3, 'Seed USB Hub 4-Port', 'Barely used, boxed', 'USB 3.0 hub for dorm desk setup.', 'USB 3.0 hub for dorm desk setup.', 'physical', 'on_sale', 'not_required', NULL, 'https://images.unsplash.com/photo-1625948515291-69613efd103f?auto=format&fit=crop&w=900&q=80', 22.00, 45.00, 5, TRUE, TRUE, FALSE, FALSE, NULL, 5, 0, FALSE, '2026-05-10 08:30:00', CURRENT_TIMESTAMP),
(3011, 1010, 4010, 4, 'Seed Wireless Mouse', 'Logistics or campus meet', 'Lightweight mouse for laptop.', 'Lightweight mouse for laptop.', 'physical', 'on_sale', 'not_required', NULL, 'https://images.unsplash.com/photo-1527814050087-3793815479db?auto=format&fit=crop&w=900&q=80', 16.50, 32.00, 8, TRUE, TRUE, FALSE, FALSE, NULL, 3, 0, FALSE, '2026-05-10 08:35:00', CURRENT_TIMESTAMP),
(3012, 1010, 4010, 2, 'Seed Scientific Calculator', 'TI-style for exams', 'Approved models only; battery included.', 'Approved models only; battery included.', 'physical', 'on_sale', 'not_required', NULL, 'https://images.unsplash.com/photo-1587145820266-a5951ee786f4?auto=format&fit=crop&w=900&q=80', 48.00, 89.00, 4, TRUE, TRUE, FALSE, FALSE, NULL, 11, 2, FALSE, '2026-05-10 08:40:00', CURRENT_TIMESTAMP),
(3013, 1012, 4011, 4, 'Seed USB-C Cable 2m', 'Braided', 'High-speed charging cable.', 'High-speed charging cable.', 'physical', 'on_sale', 'not_required', NULL, 'https://images.unsplash.com/photo-1621478374422-35206faaed30?auto=format&fit=crop&w=900&q=80', 9.90, 25.00, 20, TRUE, TRUE, FALSE, FALSE, NULL, 7, 1, FALSE, '2026-05-10 08:45:00', CURRENT_TIMESTAMP),
(3014, 1012, 4011, 2, 'Seed Multimeter Mini', 'For lab course', 'Compact multimeter for intro circuits.', 'Compact multimeter for intro circuits.', 'physical', 'on_sale', 'not_required', NULL, 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?auto=format&fit=crop&w=900&q=80', 42.00, 79.00, 2, FALSE, TRUE, FALSE, FALSE, NULL, 4, 0, FALSE, '2026-05-10 08:48:00', CURRENT_TIMESTAMP);

INSERT INTO product_media (id, product_id, media_type, media_url, sort_order)
VALUES
(3101, 3001, 'cover', 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?auto=format&fit=crop&w=900&q=80', 1),
(3102, 3002, 'cover', 'https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=900&q=80', 1),
(3103, 3003, 'cover', 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=900&q=80', 1),
(3104, 3004, 'cover', 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=900&q=80', 1),
(3110, 3010, 'cover', 'https://images.unsplash.com/photo-1625948515291-69613efd103f?auto=format&fit=crop&w=900&q=80', 1),
(3111, 3011, 'cover', 'https://images.unsplash.com/photo-1527814050087-3793815479db?auto=format&fit=crop&w=900&q=80', 1),
(3112, 3012, 'cover', 'https://images.unsplash.com/photo-1587145820266-a5951ee786f4?auto=format&fit=crop&w=900&q=80', 1),
(3113, 3013, 'cover', 'https://images.unsplash.com/photo-1621478374422-35206faaed30?auto=format&fit=crop&w=900&q=80', 1),
(3114, 3014, 'cover', 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?auto=format&fit=crop&w=900&q=80', 1);

INSERT INTO product_digital_assets (id, product_id, asset_type, asset_name, storage_path, is_preview, preview_rule, status, sort_order)
VALUES
(3201, 3001, 'pdf', 'Catalog Preview.pdf', 'preview://3001/catalog', TRUE, 'limited_preview', 'active', 1),
(3202, 3001, 'pdf', 'Sample Chapter.pdf', 'preview://3001/sample', TRUE, 'limited_preview', 'active', 2),
(3203, 3001, 'zip', 'Full Pack.zip', 'download://3001/full', FALSE, 'confirm_receipt_required', 'active', 99);

INSERT INTO product_review_tasks (id, product_id, review_type, review_status, submitted_at, reviewed_at, reviewed_by, reject_reason, review_note, created_at, updated_at)
VALUES
(5001, 3001, 'digital_product', 'approved', '2026-05-01 09:00:00', '2026-05-01 10:00:00', 9001, NULL, 'source statement complete', '2026-05-01 09:00:00', CURRENT_TIMESTAMP),
(5002, 3004, 'digital_product', 'pending_review', '2026-05-08 12:00:00', NULL, NULL, NULL, NULL, '2026-05-08 12:00:00', CURRENT_TIMESTAMP);

INSERT INTO reports (
    id, reporter_user_id, reporter_name, target_type, target_id, target_label,
    reason_type, content, status, submitted_at, processed_at, processed_by,
    resolution, created_at, updated_at
) VALUES
(6001, 1002, 'Li Si', 'product', 3002, 'Engineering Drawing Tool Set',
 'copyright', 'Possible unauthorized reposted material from a public drive.',
 'pending', '2026-05-09 09:30:00', NULL, NULL, '',
 '2026-05-09 09:30:00', CURRENT_TIMESTAMP),
(6002, 1001, 'Zhang San', 'shop', 4003, 'Pending Campus Shop',
 'fraud', 'Suspected repeated sale of invalid materials.',
 'processing', '2026-05-08 10:10:00', '2026-05-09 08:20:00', 'Admin9001',
 'Escalated for manual review and temporary shop restriction.',
 '2026-05-08 10:10:00', CURRENT_TIMESTAMP),
(6003, 1004, 'Zhao Liu', 'user', 1003, 'Wang Wu',
 'abuse', 'Abusive behavior reported during communication.',
 'resolved', '2026-05-07 18:40:00', '2026-05-08 12:00:00', 'Admin9001',
 'Warning issued and risk event recorded.',
 '2026-05-07 18:40:00', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE status = VALUES(status), processed_at = VALUES(processed_at),
    processed_by = VALUES(processed_by), resolution = VALUES(resolution);

INSERT INTO support_tickets (
    id, ticket_no, requester_user_id, category, subject, content, status, priority,
    related_type, related_id, assigned_admin_user_id, last_replied_by, last_replied_at,
    created_at, updated_at
) VALUES
(6201, 'SEED-SUP-0001', 1001, 'order', 'Order pickup support', 'I need help confirming an offline pickup detail.', 'waiting_user', 'normal',
 'order', 8001, 9102, 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6202, 'SEED-SUP-0002', 1010, 'account', 'Account verification question', 'Please check my campus verification status.', 'open', 'normal',
 NULL, NULL, NULL, 'user', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE status = VALUES(status), assigned_admin_user_id = VALUES(assigned_admin_user_id),
    last_replied_by = VALUES(last_replied_by), last_replied_at = VALUES(last_replied_at), updated_at = VALUES(updated_at);

INSERT INTO support_ticket_messages (
    id, ticket_id, sender_user_id, sender_role, message_type, content, created_at
) VALUES
(6211, 6201, 1001, 'user', 'public_reply', 'I need help confirming an offline pickup detail.', CURRENT_TIMESTAMP),
(6212, 6201, 9102, 'admin', 'public_reply', 'Please share the pickup time window.', CURRENT_TIMESTAMP),
(6213, 6201, 9102, 'admin', 'internal_note', 'Check order context before final reply.', CURRENT_TIMESTAMP),
(6214, 6202, 1010, 'user', 'public_reply', 'Please check my campus verification status.', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE content = VALUES(content);
