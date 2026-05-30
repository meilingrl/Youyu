CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    phone VARCHAR(32),
    email VARCHAR(128) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    avatar VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_privilege_profiles (
    user_id BIGINT PRIMARY KEY,
    can_purchase BOOLEAN NOT NULL DEFAULT TRUE,
    can_publish BOOLEAN NOT NULL DEFAULT FALSE,
    can_review BOOLEAN NOT NULL DEFAULT TRUE,
    can_apply_shop BOOLEAN NOT NULL DEFAULT FALSE,
    is_restricted BOOLEAN NOT NULL DEFAULT FALSE,
    restricted_reason VARCHAR(255),
    credit_level VARCHAR(64) NOT NULL DEFAULT 'L0 new user',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_privilege_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS student_verifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    student_no VARCHAR(64) NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    college VARCHAR(128),
    major VARCHAR(128),
    grade VARCHAR(64),
    campus_email VARCHAR(128),
    verification_method VARCHAR(32) NOT NULL DEFAULT 'manual_review',
    verification_status VARCHAR(32) NOT NULL DEFAULT 'pending_review',
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    reviewer_id BIGINT,
    reject_reason VARCHAR(255),
    review_note VARCHAR(255),
    risk_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_verification_user FOREIGN KEY (user_id) REFERENCES users(id)
);

DROP INDEX uk_student_verifications_approved_student_no;

CREATE INDEX idx_student_verifications_user_submitted ON student_verifications(user_id, submitted_at, id);
CREATE INDEX idx_student_verifications_status_submitted ON student_verifications(verification_status, submitted_at, id);

CREATE TABLE IF NOT EXISTS user_addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    receiver_name VARCHAR(64) NOT NULL,
    receiver_phone VARCHAR(32) NOT NULL,
    address_type VARCHAR(32) NOT NULL DEFAULT 'campus',
    province VARCHAR(64),
    city VARCHAR(64),
    district VARCHAR(64),
    detail_address VARCHAR(255) NOT NULL,
    campus_area VARCHAR(128),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_address_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS user_preferences (
    user_id BIGINT PRIMARY KEY,
    theme_mode VARCHAR(32) NOT NULL DEFAULT 'system',
    theme_color VARCHAR(64) NOT NULL DEFAULT 'campus_blue',
    home_display_mode VARCHAR(32) NOT NULL DEFAULT 'card',
    default_address_id BIGINT,
    default_fulfillment_type VARCHAR(32) NOT NULL DEFAULT 'any',
    default_payment_method VARCHAR(64) NOT NULL DEFAULT 'mock_payment',
    default_sort_type VARCHAR(64) NOT NULL DEFAULT 'comprehensive',
    order_reminder BOOLEAN NOT NULL DEFAULT TRUE,
    review_reminder BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT,
    name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'enabled',
    sort_order INT NOT NULL DEFAULT 0,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS shops (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL UNIQUE,
    description VARCHAR(500),
    avatar_url VARCHAR(512),
    cover_url VARCHAR(512),
    announcement VARCHAR(500),
    status VARCHAR(32) NOT NULL DEFAULT 'inactive',
    review_status VARCHAR(32) NOT NULL DEFAULT 'pending_review',
    reviewed_at TIMESTAMP,
    reviewed_by BIGINT,
    reject_reason VARCHAR(255),
    rating_score DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    review_count INT NOT NULL DEFAULT 0,
    follower_count INT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_shops_owner UNIQUE (owner_user_id)
);

CREATE INDEX idx_shops_status ON shops(status);
CREATE INDEX idx_shops_review_status ON shops(review_status);

CREATE TABLE IF NOT EXISTS shop_capability_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id BIGINT NOT NULL UNIQUE,
    capability_level VARCHAR(32) NOT NULL DEFAULT 'basic',
    max_active_product_count INT NOT NULL DEFAULT 20,
    can_config_announcement BOOLEAN NOT NULL DEFAULT TRUE,
    can_config_loyalty_offer BOOLEAN NOT NULL DEFAULT FALSE,
    can_issue_light_coupon BOOLEAN NOT NULL DEFAULT FALSE,
    can_join_platform_activity BOOLEAN NOT NULL DEFAULT FALSE,
    effective_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_user_id BIGINT NOT NULL,
    shop_id BIGINT,
    category_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255),
    description TEXT,
    detail_content TEXT,
    product_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    review_status VARCHAR(32) NOT NULL DEFAULT 'not_required',
    review_reject_reason VARCHAR(255),
    main_image_url VARCHAR(512),
    sale_price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2),
    stock_quantity INT NOT NULL DEFAULT 1,
    supports_logistics BOOLEAN NOT NULL DEFAULT FALSE,
    supports_offline_delivery BOOLEAN NOT NULL DEFAULT TRUE,
    supports_digital_delivery BOOLEAN NOT NULL DEFAULT FALSE,
    allow_preview BOOLEAN NOT NULL DEFAULT FALSE,
    preview_rule_text VARCHAR(255),
    view_count INT NOT NULL DEFAULT 0,
    favorite_count INT NOT NULL DEFAULT 0,
    rating_score DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    review_count INT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_seller ON products(seller_user_id);
CREATE INDEX idx_products_shop ON products(shop_id);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_review_status ON products(review_status);
CREATE INDEX idx_products_type ON products(product_type);
-- Composite index for the public listing base path: findPublicByFiltersPaged / countPublicByFilters (status + is_deleted fixed filters + created_at ORDER BY)
CREATE INDEX idx_products_public_base ON products(status, is_deleted, created_at);
-- Composite index for filtered public listing: adds product_type + category_id optional filters over the public base path
CREATE INDEX idx_products_public_type_cat ON products(status, is_deleted, product_type, category_id, created_at);

CREATE TABLE IF NOT EXISTS product_media (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    media_type VARCHAR(32) NOT NULL,
    media_url VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_media_product ON product_media(product_id);

CREATE TABLE IF NOT EXISTS product_digital_assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    asset_type VARCHAR(32) NOT NULL,
    asset_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    is_preview BOOLEAN NOT NULL DEFAULT FALSE,
    preview_rule VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_digital_assets_product ON product_digital_assets(product_id);

CREATE TABLE IF NOT EXISTS product_review_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    review_type VARCHAR(32) NOT NULL DEFAULT 'digital_product',
    review_status VARCHAR(32) NOT NULL DEFAULT 'pending_review',
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    reviewed_by BIGINT,
    reject_reason VARCHAR(255),
    review_note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_review_tasks_product ON product_review_tasks(product_id);
CREATE INDEX idx_product_review_tasks_status ON product_review_tasks(review_status);

CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    selected BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cart_items_user_product UNIQUE (user_id, product_id)
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    buyer_user_id BIGINT NOT NULL,
    seller_user_id BIGINT NOT NULL,
    shop_id BIGINT,
    order_status VARCHAR(32) NOT NULL,
    fulfillment_type VARCHAR(32) NOT NULL,
    payment_status VARCHAR(32) NOT NULL,
    goods_amount DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL,
    pay_amount DECIMAL(12,2) NOT NULL,
    buyer_note VARCHAR(500),
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    closed_reason VARCHAR(100),
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_user_id) REFERENCES users(id)
);

CREATE INDEX idx_orders_buyer ON orders(buyer_user_id, submitted_at, id);
CREATE INDEX idx_orders_shop_status ON orders(shop_id, order_status, payment_status, completed_at);

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    title_snapshot VARCHAR(200) NOT NULL,
    image_snapshot VARCHAR(500),
    price_snapshot DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal_amount DECIMAL(12,2) NOT NULL,
    product_type_snapshot VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX idx_order_items_order ON order_items(order_id, id);
CREATE INDEX idx_order_items_product ON order_items(product_id);

CREATE TABLE IF NOT EXISTS marketing_coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id BIGINT NOT NULL,
    owner_user_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(500),
    coupon_type VARCHAR(32) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL,
    minimum_spend_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_quantity INT NOT NULL,
    claimed_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    review_status VARCHAR(32) NOT NULL DEFAULT 'pending_review',
    reject_reason VARCHAR(255),
    review_note VARCHAR(500),
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_marketing_coupons_shop FOREIGN KEY (shop_id) REFERENCES shops(id),
    CONSTRAINT fk_marketing_coupons_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
);

CREATE INDEX idx_marketing_coupons_owner ON marketing_coupons(owner_user_id, created_at, id);
CREATE INDEX idx_marketing_coupons_public ON marketing_coupons(shop_id, review_status, status, start_at, end_at);

CREATE TABLE IF NOT EXISTS user_coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'claimed',
    claimed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP,
    order_id BIGINT,
    CONSTRAINT fk_user_coupons_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_coupons_coupon FOREIGN KEY (coupon_id) REFERENCES marketing_coupons(id),
    CONSTRAINT uk_user_coupons_user_coupon UNIQUE (user_id, coupon_id)
);

CREATE INDEX idx_user_coupons_user_status ON user_coupons(user_id, status, claimed_at, id);
CREATE INDEX idx_user_coupons_order ON user_coupons(order_id);

CREATE TABLE IF NOT EXISTS order_coupon_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    user_coupon_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    coupon_title VARCHAR(128) NOT NULL,
    coupon_type VARCHAR(32) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL,
    minimum_spend_amount DECIMAL(12,2) NOT NULL,
    order_goods_amount DECIMAL(12,2) NOT NULL,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_coupon_app_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_coupon_app_user_coupon FOREIGN KEY (user_coupon_id) REFERENCES user_coupons(id),
    CONSTRAINT fk_order_coupon_app_coupon FOREIGN KEY (coupon_id) REFERENCES marketing_coupons(id)
);

CREATE INDEX idx_order_coupon_app_coupon ON order_coupon_applications(coupon_id, applied_at, id);

CREATE TABLE IF NOT EXISTS shop_activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id BIGINT NOT NULL,
    owner_user_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    review_status VARCHAR(32) NOT NULL DEFAULT 'pending_review',
    reject_reason VARCHAR(255),
    review_note VARCHAR(500),
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shop_activities_shop FOREIGN KEY (shop_id) REFERENCES shops(id),
    CONSTRAINT fk_shop_activities_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
);

CREATE INDEX idx_shop_activities_owner ON shop_activities(owner_user_id, created_at, id);
CREATE INDEX idx_shop_activities_public ON shop_activities(shop_id, review_status, status, start_at, end_at);

CREATE TABLE IF NOT EXISTS order_fulfillments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    fulfillment_type VARCHAR(32) NOT NULL,
    fulfillment_status VARCHAR(32) NOT NULL,
    seller_confirmed_at TIMESTAMP,
    buyer_confirmed_at TIMESTAMP,
    buyer_note VARCHAR(500),
    address_snapshot VARCHAR(1000),
    logistics_no VARCHAR(100),
    logistics_company VARCHAR(100),
    shipped_at TIMESTAMP,
    offline_meeting_time VARCHAR(100),
    offline_meeting_place VARCHAR(200),
    offline_seller_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    offline_buyer_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    preview_rule_snapshot VARCHAR(500),
    download_access_status VARCHAR(50),
    digital_access_opened_at TIMESTAMP,
    CONSTRAINT fk_order_fulfillments_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE IF NOT EXISTS payment_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    payment_no VARCHAR(40) NOT NULL UNIQUE,
    payment_method VARCHAR(50) NOT NULL,
    payment_channel VARCHAR(50) NOT NULL,
    payment_status VARCHAR(32) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    initiated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    succeeded_at TIMESTAMP,
    failed_reason VARCHAR(500),
    callback_summary VARCHAR(1000),
    CONSTRAINT fk_payment_records_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX idx_payment_records_order ON payment_records(order_id, initiated_at, id);

CREATE TABLE IF NOT EXISTS refund_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    payment_record_id BIGINT NOT NULL,
    refund_no VARCHAR(40) NOT NULL UNIQUE,
    refund_status VARCHAR(32) NOT NULL,
    refund_amount DECIMAL(12,2) NOT NULL,
    refund_reason VARCHAR(500) NOT NULL,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_refund_records_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_refund_records_payment FOREIGN KEY (payment_record_id) REFERENCES payment_records(id)
);

CREATE INDEX idx_refund_records_order ON refund_records(order_id, applied_at, id);

CREATE TABLE IF NOT EXISTS reports (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_user_id  BIGINT        NOT NULL,
    reporter_name     VARCHAR(64)   NOT NULL,
    target_type       VARCHAR(32)   NOT NULL,
    target_id         BIGINT        NOT NULL,
    target_label      VARCHAR(255),
    reason_type       VARCHAR(64),
    content           TEXT,
    status            VARCHAR(32)   NOT NULL DEFAULT 'pending',
    submitted_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at      TIMESTAMP,
    processed_by      VARCHAR(64),
    resolution        TEXT,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_user_id) REFERENCES users(id)
);

CREATE INDEX idx_reports_submitted ON reports(submitted_at);
CREATE INDEX idx_reports_status_submitted ON reports(status, submitted_at);

CREATE TABLE IF NOT EXISTS support_tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_no VARCHAR(40) NOT NULL UNIQUE,
    requester_user_id BIGINT NOT NULL,
    category VARCHAR(32) NOT NULL,
    subject VARCHAR(120) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'open',
    priority VARCHAR(32) NOT NULL DEFAULT 'normal',
    related_type VARCHAR(32),
    related_id BIGINT,
    assigned_admin_user_id BIGINT,
    last_replied_by VARCHAR(32),
    last_replied_at TIMESTAMP,
    resolved_at TIMESTAMP,
    closed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_support_ticket_requester FOREIGN KEY (requester_user_id) REFERENCES users(id),
    CONSTRAINT fk_support_ticket_assignee FOREIGN KEY (assigned_admin_user_id) REFERENCES users(id)
);

CREATE INDEX idx_support_tickets_requester_updated ON support_tickets(requester_user_id, updated_at, id);
CREATE INDEX idx_support_tickets_status_updated ON support_tickets(status, updated_at, id);
CREATE INDEX idx_support_tickets_assignee_updated ON support_tickets(assigned_admin_user_id, updated_at, id);

CREATE TABLE IF NOT EXISTS support_ticket_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    sender_user_id BIGINT,
    sender_role VARCHAR(32) NOT NULL,
    message_type VARCHAR(32) NOT NULL DEFAULT 'public_reply',
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_support_ticket_message_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets(id),
    CONSTRAINT fk_support_ticket_message_sender FOREIGN KEY (sender_user_id) REFERENCES users(id)
);

CREATE INDEX idx_support_ticket_messages_ticket_created ON support_ticket_messages(ticket_id, created_at, id);

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_user_id BIGINT NOT NULL,
    operator_role VARCHAR(32) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id BIGINT NOT NULL,
    summary VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_audit_log_operator FOREIGN KEY (operator_user_id) REFERENCES users(id)
);

CREATE INDEX idx_admin_audit_logs_created ON admin_audit_logs(created_at, id);
CREATE INDEX idx_admin_audit_logs_action_created ON admin_audit_logs(action, created_at, id);
CREATE INDEX idx_admin_audit_logs_target ON admin_audit_logs(target_type, target_id, created_at, id);
CREATE INDEX idx_admin_audit_logs_operator ON admin_audit_logs(operator_user_id, created_at, id);

CREATE TABLE IF NOT EXISTS mediation_cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_no VARCHAR(40) NOT NULL UNIQUE,
    source_report_id BIGINT NOT NULL UNIQUE,
    related_order_id BIGINT NOT NULL,
    buyer_user_id BIGINT NOT NULL,
    seller_user_id BIGINT NOT NULL,
    reporter_user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'opened',
    decision_category VARCHAR(64),
    decision_summary TEXT,
    enforcement_summary TEXT,
    cancel_reason VARCHAR(500),
    decided_by_admin_user_id BIGINT,
    decided_at TIMESTAMP,
    created_by_admin_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_status_changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mediation_case_report FOREIGN KEY (source_report_id) REFERENCES reports(id),
    CONSTRAINT fk_mediation_case_order FOREIGN KEY (related_order_id) REFERENCES orders(id),
    CONSTRAINT fk_mediation_case_buyer FOREIGN KEY (buyer_user_id) REFERENCES users(id),
    CONSTRAINT fk_mediation_case_seller FOREIGN KEY (seller_user_id) REFERENCES users(id),
    CONSTRAINT fk_mediation_case_reporter FOREIGN KEY (reporter_user_id) REFERENCES users(id),
    CONSTRAINT fk_mediation_case_created_by FOREIGN KEY (created_by_admin_user_id) REFERENCES users(id),
    CONSTRAINT fk_mediation_case_decided_by FOREIGN KEY (decided_by_admin_user_id) REFERENCES users(id)
);

CREATE INDEX idx_mediation_cases_status_updated ON mediation_cases(status, updated_at, id);
CREATE INDEX idx_mediation_cases_order ON mediation_cases(related_order_id);
CREATE INDEX idx_mediation_cases_decision ON mediation_cases(decision_category);

CREATE TABLE IF NOT EXISTS search_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(255) NOT NULL,
    normalized_keyword VARCHAR(255) NOT NULL,
    user_id BIGINT,
    result_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_search_log_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_search_logs_created_at ON search_logs(created_at);
CREATE INDEX idx_search_logs_normalized_keyword ON search_logs(normalized_keyword);

CREATE TABLE IF NOT EXISTS search_governance_rules (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_type       VARCHAR(32)  NOT NULL,
    keyword         VARCHAR(255) NOT NULL,
    display_label   VARCHAR(255) DEFAULT NULL,
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sgr_type ON search_governance_rules(rule_type);
CREATE INDEX idx_sgr_active ON search_governance_rules(is_active);
CREATE UNIQUE INDEX uk_sgr_type_keyword ON search_governance_rules(rule_type, keyword);

CREATE TABLE IF NOT EXISTS digital_access_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    asset_id    BIGINT       NOT NULL,
    asset_name  VARCHAR(255) NOT NULL,
    access_type VARCHAR(32)  NOT NULL DEFAULT 'full',
    accessed_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_digital_access_log_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_digital_access_log_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_digital_access_log_asset FOREIGN KEY (asset_id) REFERENCES product_digital_assets(id)
);

CREATE INDEX idx_digital_access_log_order ON digital_access_logs(order_id);
CREATE INDEX idx_digital_access_log_user ON digital_access_logs(user_id);

CREATE TABLE IF NOT EXISTS reviews (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    buyer_user_id BIGINT NOT NULL,
    product_id    BIGINT NOT NULL,
    score         TINYINT NOT NULL,
    content       VARCHAR(1000),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(id),
    CONSTRAINT fk_reviews_buyer      FOREIGN KEY (buyer_user_id) REFERENCES users(id),
    CONSTRAINT fk_reviews_product    FOREIGN KEY (product_id)    REFERENCES products(id),
    CONSTRAINT uk_reviews_order_item_buyer UNIQUE (order_item_id, buyer_user_id)
);

CREATE INDEX idx_reviews_product ON reviews(product_id);
CREATE INDEX idx_reviews_buyer   ON reviews(buyer_user_id);

CREATE TABLE IF NOT EXISTS shop_reviews (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id       BIGINT NOT NULL,
    buyer_user_id BIGINT NOT NULL,
    score         TINYINT NOT NULL,
    content       VARCHAR(1000),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shop_reviews_shop  FOREIGN KEY (shop_id) REFERENCES shops(id),
    CONSTRAINT fk_shop_reviews_buyer FOREIGN KEY (buyer_user_id) REFERENCES users(id),
    CONSTRAINT uk_shop_reviews_shop_buyer UNIQUE (shop_id, buyer_user_id)
);

CREATE INDEX idx_shop_reviews_shop  ON shop_reviews(shop_id);
CREATE INDEX idx_shop_reviews_buyer ON shop_reviews(buyer_user_id);

CREATE TABLE IF NOT EXISTS chat_conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(32) NOT NULL DEFAULT 'direct',
    product_id BIGINT,
    shop_id BIGINT,
    user_a_id BIGINT NOT NULL,
    user_b_id BIGINT NOT NULL,
    unread_count_a INT NOT NULL DEFAULT 0,
    unread_count_b INT NOT NULL DEFAULT 0,
    is_pinned_a BOOLEAN NOT NULL DEFAULT FALSE,
    is_pinned_b BOOLEAN NOT NULL DEFAULT FALSE,
    is_muted_a BOOLEAN NOT NULL DEFAULT FALSE,
    is_muted_b BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_by_a_at TIMESTAMP NULL,
    deleted_by_b_at TIMESTAMP NULL,
    auto_replied_to_a_at TIMESTAMP NULL,
    auto_replied_to_b_at TIMESTAMP NULL,
    support_status VARCHAR(16) NULL,
    assigned_admin_id BIGINT NULL,
    last_message_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_conv_user_a FOREIGN KEY (user_a_id) REFERENCES users(id),
    CONSTRAINT fk_chat_conv_user_b FOREIGN KEY (user_b_id) REFERENCES users(id),
    CONSTRAINT fk_chat_conv_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_chat_conv_shop FOREIGN KEY (shop_id) REFERENCES shops(id),
    CONSTRAINT fk_chat_conv_admin FOREIGN KEY (assigned_admin_id) REFERENCES users(id),
    INDEX idx_user_a_last_message (user_a_id, last_message_at DESC),
    INDEX idx_user_b_last_message (user_b_id, last_message_at DESC),
    INDEX idx_user_a_unread (user_a_id, unread_count_a),
    INDEX idx_user_b_unread (user_b_id, unread_count_b),
    INDEX idx_user_a_pinned (user_a_id, is_pinned_a),
    INDEX idx_user_b_pinned (user_b_id, is_pinned_b),
    INDEX idx_support_status (support_status, last_message_at DESC),
    INDEX idx_support_assigned (assigned_admin_id, support_status),
    UNIQUE INDEX uk_conversation_pair (user_a_id, user_b_id, product_id, shop_id)
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_user_id BIGINT NOT NULL,
    body TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    message_type VARCHAR(32) NOT NULL DEFAULT 'text',
    media_url MEDIUMTEXT NULL,
    product_id BIGINT NULL,
    order_id BIGINT NULL,
    is_recalled BOOLEAN NOT NULL DEFAULT FALSE,
    recalled_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_msg_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id),
    CONSTRAINT fk_chat_msg_sender FOREIGN KEY (sender_user_id) REFERENCES users(id),
    CONSTRAINT fk_chat_msg_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_chat_msg_order FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_conversation_created (conversation_id, created_at DESC),
    INDEX idx_conversation_unread (conversation_id, is_read),
    INDEX idx_conversation_type (conversation_id, message_type),
    INDEX idx_conversation_recalled (conversation_id, is_recalled),
    INDEX idx_message_product (product_id),
    INDEX idx_message_order (order_id)
);

CREATE TABLE IF NOT EXISTS auto_reply_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    reply_content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_auto_reply_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_auto_reply_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS quick_replies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_quick_reply_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_quick_reply_user_sort (user_id, sort_order, created_at)
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    action_url VARCHAR(512),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_notification_user_read (user_id, is_read),
    INDEX idx_notification_user_created (user_id, created_at DESC)
);
