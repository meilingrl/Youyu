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
