-- CampusMarket MVP database initialization script
-- Target database: MySQL 8.0+

CREATE DATABASE IF NOT EXISTS `campus_market`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE `campus_market`;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名/账号标识',
  `phone` VARCHAR(32) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码摘要',
  `nickname` VARCHAR(64) NOT NULL COMMENT '昵称',
  `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
  `gender` VARCHAR(16) DEFAULT NULL COMMENT '性别',
  `birth_date` DATE DEFAULT NULL COMMENT '出生日期',
  `status` VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '用户状态：active/disabled/locked/pending_activation',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_phone` (`phone`),
  UNIQUE KEY `uk_user_email` (`email`),
  KEY `idx_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `admin_profile` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '管理员档案ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '关联用户ID',
  `admin_role` VARCHAR(32) NOT NULL DEFAULT 'operator' COMMENT '管理员角色：super_admin/reviewer/operator',
  `status` VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '管理员状态：active/disabled',
  `display_name` VARCHAR(64) DEFAULT NULL COMMENT '后台展示名称',
  `note` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_profile_user` (`user_id`),
  KEY `idx_admin_profile_role` (`admin_role`),
  KEY `idx_admin_profile_status` (`status`),
  CONSTRAINT `fk_admin_profile_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员档案表';

CREATE TABLE IF NOT EXISTS `student_verification` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '认证记录ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `student_no` VARCHAR(64) NOT NULL COMMENT '学号',
  `real_name` VARCHAR(64) NOT NULL COMMENT '真实姓名',
  `college_name` VARCHAR(128) DEFAULT NULL COMMENT '学院名称',
  `major_name` VARCHAR(128) DEFAULT NULL COMMENT '专业名称',
  `grade_name` VARCHAR(64) DEFAULT NULL COMMENT '年级',
  `campus_email` VARCHAR(128) DEFAULT NULL COMMENT '校园邮箱',
  `verification_method` VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT '认证方式：manual/email/document',
  `verification_status` VARCHAR(32) NOT NULL DEFAULT 'pending_review' COMMENT '认证状态：pending_review/approved/rejected/revoked/expired',
  `verification_period` VARCHAR(32) DEFAULT NULL COMMENT '认证周期，如2026-Spring',
  `is_current` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否当前生效记录',
  `risk_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否命中风险标记',
  `risk_note` VARCHAR(255) DEFAULT NULL COMMENT '风险说明',
  `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `reviewed_at` DATETIME DEFAULT NULL COMMENT '审核时间',
  `reviewed_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人ID，当前仅预留',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '驳回原因',
  `review_note` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `valid_from` DATETIME DEFAULT NULL COMMENT '认证生效时间',
  `valid_until` DATETIME DEFAULT NULL COMMENT '认证失效时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_student_verification_user` (`user_id`),
  KEY `idx_student_verification_student_no` (`student_no`),
  KEY `idx_student_verification_campus_email` (`campus_email`),
  KEY `idx_student_verification_status` (`verification_status`),
  KEY `idx_student_verification_current` (`user_id`, `is_current`),
  CONSTRAINT `fk_student_verification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生认证记录表';

CREATE TABLE IF NOT EXISTS `user_privilege_profile` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限档位ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `can_purchase` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可购买',
  `can_publish` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可发布',
  `can_review` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可评价',
  `can_open_shop` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可申请开店',
  `is_restricted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否受限',
  `restriction_reason` VARCHAR(255) DEFAULT NULL COMMENT '受限原因',
  `effective_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生效时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_privilege_profile_user` (`user_id`),
  CONSTRAINT `fk_user_privilege_profile_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户权限档位表';

CREATE TABLE IF NOT EXISTS `user_address` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `address_type` VARCHAR(32) NOT NULL DEFAULT 'shipping' COMMENT '地址类型：shipping/offline_meet',
  `contact_name` VARCHAR(64) NOT NULL COMMENT '联系人',
  `contact_phone` VARCHAR(32) NOT NULL COMMENT '联系电话',
  `province` VARCHAR(64) DEFAULT NULL COMMENT '省',
  `city` VARCHAR(64) DEFAULT NULL COMMENT '市',
  `district` VARCHAR(64) DEFAULT NULL COMMENT '区',
  `campus_name` VARCHAR(128) DEFAULT NULL COMMENT '校区',
  `building_info` VARCHAR(255) DEFAULT NULL COMMENT '宿舍/教学楼等信息',
  `detail_address` VARCHAR(255) NOT NULL COMMENT '详细地址',
  `postal_code` VARCHAR(32) DEFAULT NULL COMMENT '邮编',
  `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认地址',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否逻辑删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '逻辑删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_address_user` (`user_id`),
  KEY `idx_user_address_default` (`user_id`, `is_default`),
  KEY `idx_user_address_deleted` (`is_deleted`),
  CONSTRAINT `fk_user_address_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户地址表';

CREATE TABLE IF NOT EXISTS `category` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '类目ID',
  `parent_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '父类目ID',
  `name` VARCHAR(128) NOT NULL COMMENT '类目名称',
  `status` VARCHAR(32) NOT NULL DEFAULT 'enabled' COMMENT '类目状态：enabled/disabled',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '类目说明',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否逻辑删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '逻辑删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_parent_name` (`parent_id`, `name`),
  KEY `idx_category_status` (`status`),
  KEY `idx_category_deleted` (`is_deleted`),
  CONSTRAINT `fk_category_parent` FOREIGN KEY (`parent_id`) REFERENCES `category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品类目表';

CREATE TABLE IF NOT EXISTS `shop` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '店铺ID',
  `owner_user_id` BIGINT UNSIGNED NOT NULL COMMENT '店主用户ID',
  `name` VARCHAR(128) NOT NULL COMMENT '店铺名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '店铺简介',
  `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '店铺头像',
  `cover_url` VARCHAR(512) DEFAULT NULL COMMENT '店铺封面',
  `announcement` VARCHAR(500) DEFAULT NULL COMMENT '店铺公告',
  `status` VARCHAR(32) NOT NULL DEFAULT 'inactive' COMMENT '店铺经营状态：inactive/active/disabled/closed',
  `review_status` VARCHAR(32) NOT NULL DEFAULT 'pending_review' COMMENT '店铺审核状态：pending_review/approved/rejected',
  `reviewed_at` DATETIME DEFAULT NULL COMMENT '审核时间',
  `reviewed_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人ID，当前仅预留',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '驳回原因',
  `rating_score` DECIMAL(3,2) NOT NULL DEFAULT 5.00 COMMENT '店铺评分',
  `follower_count` INT NOT NULL DEFAULT 0 COMMENT '关注数',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否逻辑删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '逻辑删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_owner_user` (`owner_user_id`),
  UNIQUE KEY `uk_shop_name` (`name`),
  KEY `idx_shop_status` (`status`),
  KEY `idx_shop_review_status` (`review_status`),
  KEY `idx_shop_deleted` (`is_deleted`),
  CONSTRAINT `fk_shop_owner_user` FOREIGN KEY (`owner_user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='店铺表';

CREATE TABLE IF NOT EXISTS `shop_capability_profile` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '店铺能力档位ID',
  `shop_id` BIGINT UNSIGNED NOT NULL COMMENT '店铺ID',
  `capability_level` VARCHAR(32) NOT NULL DEFAULT 'basic' COMMENT '能力等级：basic/growing/advanced',
  `max_active_product_count` INT NOT NULL DEFAULT 20 COMMENT '最大上架数',
  `can_config_announcement` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可配置公告',
  `can_config_loyalty_offer` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可配置常客优惠',
  `can_issue_light_coupon` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可发放简化优惠券',
  `can_join_platform_activity` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可参与活动',
  `effective_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生效时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_capability_profile_shop` (`shop_id`),
  CONSTRAINT `fk_shop_capability_profile_shop` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='店铺能力档位表';

CREATE TABLE IF NOT EXISTS `product` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `seller_user_id` BIGINT UNSIGNED NOT NULL COMMENT '卖家用户ID',
  `shop_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '所属店铺ID',
  `category_id` BIGINT UNSIGNED NOT NULL COMMENT '类目ID',
  `title` VARCHAR(255) NOT NULL COMMENT '商品标题',
  `subtitle` VARCHAR(255) DEFAULT NULL COMMENT '商品副标题',
  `description` TEXT DEFAULT NULL COMMENT '商品描述',
  `detail_content` MEDIUMTEXT DEFAULT NULL COMMENT '详情内容',
  `product_type` VARCHAR(32) NOT NULL COMMENT '商品类型：physical/digital/service',
  `status` VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT '商品状态：draft/on_sale/off_sale/sold_out/closed',
  `review_status` VARCHAR(32) NOT NULL DEFAULT 'not_required' COMMENT '审核状态：not_required/pending_review/approved/rejected',
  `main_image_url` VARCHAR(512) DEFAULT NULL COMMENT '主图地址',
  `sale_price` DECIMAL(10,2) NOT NULL COMMENT '售价',
  `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价参考',
  `stock_quantity` INT NOT NULL DEFAULT 1 COMMENT '库存数量',
  `supports_logistics` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否支持物流',
  `supports_offline_delivery` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否支持线下交付',
  `supports_digital_delivery` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否支持电子交付',
  `allow_preview` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否允许预览',
  `preview_rule_text` VARCHAR(255) DEFAULT NULL COMMENT '预览规则说明',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览量',
  `favorite_count` INT NOT NULL DEFAULT 0 COMMENT '收藏量',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否逻辑删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '逻辑删除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_seller` (`seller_user_id`),
  KEY `idx_product_shop` (`shop_id`),
  KEY `idx_product_category` (`category_id`),
  KEY `idx_product_status` (`status`),
  KEY `idx_product_review_status` (`review_status`),
  KEY `idx_product_type` (`product_type`),
  KEY `idx_product_deleted` (`is_deleted`),
  CONSTRAINT `fk_product_seller_user` FOREIGN KEY (`seller_user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_product_shop` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`id`),
  CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品表';

CREATE TABLE IF NOT EXISTS `product_media` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '媒体ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `media_type` VARCHAR(32) NOT NULL COMMENT '媒体类型：main/detail/preview/cover',
  `media_url` VARCHAR(512) NOT NULL COMMENT '媒体地址',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_media_product` (`product_id`),
  KEY `idx_product_media_type` (`media_type`),
  CONSTRAINT `fk_product_media_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品媒体表';

CREATE TABLE IF NOT EXISTS `product_review_task` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审核任务ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `review_type` VARCHAR(32) NOT NULL DEFAULT 'digital_product' COMMENT '审核类型：digital_product/risk_check',
  `review_status` VARCHAR(32) NOT NULL DEFAULT 'pending_review' COMMENT '审核状态：pending_review/approved/rejected/cancelled',
  `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `reviewed_at` DATETIME DEFAULT NULL COMMENT '审核时间',
  `reviewed_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人ID，当前仅预留',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '驳回原因',
  `review_note` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_review_task_product` (`product_id`),
  KEY `idx_product_review_task_status` (`review_status`),
  CONSTRAINT `fk_product_review_task_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品审核任务表';

CREATE TABLE IF NOT EXISTS `product_digital_asset` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '资源ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `asset_name` VARCHAR(255) NOT NULL COMMENT '资源名称',
  `asset_type` VARCHAR(32) NOT NULL COMMENT '资源类型：document/image/archive/video/other',
  `storage_path` VARCHAR(512) NOT NULL COMMENT '资源存储路径',
  `is_preview_asset` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否预览资源',
  `preview_order` INT NOT NULL DEFAULT 0 COMMENT '预览顺序',
  `is_full_asset` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否完整资源',
  `status` VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '资源状态：active/disabled/replaced',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_digital_asset_product` (`product_id`),
  KEY `idx_product_digital_asset_status` (`status`),
  KEY `idx_product_digital_asset_preview` (`product_id`, `is_preview_asset`),
  CONSTRAINT `fk_product_digital_asset_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='电子资料资源表';

CREATE TABLE IF NOT EXISTS `cart_item` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '购物车项ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
  `is_selected` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否勾选',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cart_item_user_product` (`user_id`, `product_id`),
  KEY `idx_cart_item_selected` (`user_id`, `is_selected`),
  CONSTRAINT `fk_cart_item_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_cart_item_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购物车表';

CREATE TABLE IF NOT EXISTS `order` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `buyer_user_id` BIGINT UNSIGNED NOT NULL COMMENT '买家用户ID',
  `seller_user_id` BIGINT UNSIGNED NOT NULL COMMENT '卖家用户ID',
  `shop_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '店铺ID',
  `order_status` VARCHAR(32) NOT NULL DEFAULT 'pending_payment' COMMENT '订单状态：pending_payment/pending_fulfillment/pending_receipt/completed/cancelled/refunding/refunded',
  `fulfillment_type` VARCHAR(32) NOT NULL COMMENT '履约方式：logistics/offline/digital',
  `payment_status` VARCHAR(32) NOT NULL DEFAULT 'unpaid' COMMENT '支付状态：unpaid/paying/paid/failed/refunding/refunded',
  `goods_amount` DECIMAL(10,2) NOT NULL COMMENT '商品总额',
  `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
  `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `paid_at` DATETIME DEFAULT NULL COMMENT '支付时间',
  `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `cancelled_at` DATETIME DEFAULT NULL COMMENT '取消时间',
  `closed_reason` VARCHAR(255) DEFAULT NULL COMMENT '关闭或取消原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_order_no` (`order_no`),
  KEY `idx_order_buyer` (`buyer_user_id`),
  KEY `idx_order_seller` (`seller_user_id`),
  KEY `idx_order_shop` (`shop_id`),
  KEY `idx_order_order_status` (`order_status`),
  KEY `idx_order_payment_status` (`payment_status`),
  KEY `idx_order_submitted_at` (`submitted_at`),
  CONSTRAINT `fk_order_buyer_user` FOREIGN KEY (`buyer_user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_order_seller_user` FOREIGN KEY (`seller_user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_order_shop` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';

CREATE TABLE IF NOT EXISTS `order_item` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单项ID',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `product_title_snapshot` VARCHAR(255) NOT NULL COMMENT '商品标题快照',
  `product_main_image_snapshot` VARCHAR(512) DEFAULT NULL COMMENT '商品主图快照',
  `product_type_snapshot` VARCHAR(32) NOT NULL COMMENT '商品类型快照',
  `unit_price_snapshot` DECIMAL(10,2) NOT NULL COMMENT '单价快照',
  `quantity` INT NOT NULL COMMENT '购买数量',
  `subtotal_amount` DECIMAL(10,2) NOT NULL COMMENT '小计金额',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_item_order` (`order_id`),
  KEY `idx_order_item_product` (`product_id`),
  CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`),
  CONSTRAINT `fk_order_item_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单项表';

CREATE TABLE IF NOT EXISTS `order_fulfillment` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '履约信息ID',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `fulfillment_type` VARCHAR(32) NOT NULL COMMENT '履约方式：logistics/offline/digital',
  `fulfillment_status` VARCHAR(32) NOT NULL DEFAULT 'pending_action' COMMENT '履约状态：pending_action/in_transit/delivered/pending_buyer_confirm/completed',
  `address_snapshot` TEXT DEFAULT NULL COMMENT '收货地址快照(JSON文本)',
  `logistics_company` VARCHAR(128) DEFAULT NULL COMMENT '物流公司',
  `tracking_no` VARCHAR(64) DEFAULT NULL COMMENT '物流单号',
  `shipped_at` DATETIME DEFAULT NULL COMMENT '发货时间',
  `scheduled_meet_at` DATETIME DEFAULT NULL COMMENT '线下约定时间',
  `meet_location` VARCHAR(255) DEFAULT NULL COMMENT '线下约定地点',
  `seller_confirmed_offline` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '卖家确认线下交付',
  `buyer_confirmed_offline` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '买家确认线下交付',
  `preview_rule_snapshot` VARCHAR(255) DEFAULT NULL COMMENT '预览规则快照',
  `download_access_status` VARCHAR(32) NOT NULL DEFAULT 'preview_only' COMMENT '下载权限状态：preview_only/full_open/blocked',
  `download_opened_at` DATETIME DEFAULT NULL COMMENT '完整下载开放时间',
  `download_log_summary` TEXT DEFAULT NULL COMMENT '下载留痕摘要',
  `seller_confirmed_at` DATETIME DEFAULT NULL COMMENT '卖家确认履约时间',
  `buyer_confirmed_at` DATETIME DEFAULT NULL COMMENT '买家确认收货时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_fulfillment_order` (`order_id`),
  KEY `idx_order_fulfillment_status` (`fulfillment_status`),
  CONSTRAINT `fk_order_fulfillment_order` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单履约信息表';

CREATE TABLE IF NOT EXISTS `payment_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '支付记录ID',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `payment_no` VARCHAR(64) NOT NULL COMMENT '支付流水号',
  `payment_method` VARCHAR(32) NOT NULL DEFAULT 'mock_gateway' COMMENT '支付方式：mock_gateway/mock_balance/other_reserved',
  `payment_channel` VARCHAR(32) NOT NULL DEFAULT 'internal_mock' COMMENT '支付渠道：internal_mock/sandbox_reserved',
  `payment_status` VARCHAR(32) NOT NULL DEFAULT 'initiated' COMMENT '支付状态：initiated/success/failed/cancelled/refunding/refunded',
  `payment_amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  `requested_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
  `paid_at` DATETIME DEFAULT NULL COMMENT '成功时间',
  `failed_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
  `callback_payload_summary` TEXT DEFAULT NULL COMMENT '回调摘要',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_record_payment_no` (`payment_no`),
  KEY `idx_payment_record_order` (`order_id`),
  KEY `idx_payment_record_status` (`payment_status`),
  CONSTRAINT `fk_payment_record_order` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付记录表';

CREATE TABLE IF NOT EXISTS `refund_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '退款记录ID',
  `refund_no` VARCHAR(64) NOT NULL COMMENT '退款单号',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `payment_record_id` BIGINT UNSIGNED NOT NULL COMMENT '支付记录ID',
  `refund_status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '退款状态：pending/processing/completed/rejected/cancelled',
  `refund_amount` DECIMAL(10,2) NOT NULL COMMENT '退款金额',
  `refund_reason` VARCHAR(255) NOT NULL COMMENT '退款原因',
  `requested_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `processed_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `processed_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人ID，当前仅预留',
  `process_note` VARCHAR(500) DEFAULT NULL COMMENT '处理备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_record_refund_no` (`refund_no`),
  KEY `idx_refund_record_order` (`order_id`),
  KEY `idx_refund_record_payment` (`payment_record_id`),
  KEY `idx_refund_record_status` (`refund_status`),
  CONSTRAINT `fk_refund_record_order` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`),
  CONSTRAINT `fk_refund_record_payment` FOREIGN KEY (`payment_record_id`) REFERENCES `payment_record` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款记录表';

CREATE TABLE IF NOT EXISTS `product_favorite` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_favorite_user_product` (`user_id`, `product_id`),
  KEY `idx_product_favorite_product` (`product_id`),
  CONSTRAINT `fk_product_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_product_favorite_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品收藏表';

CREATE TABLE IF NOT EXISTS `review` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `order_item_id` BIGINT UNSIGNED NOT NULL COMMENT '订单项ID',
  `product_id` BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `buyer_user_id` BIGINT UNSIGNED NOT NULL COMMENT '买家用户ID',
  `seller_user_id` BIGINT UNSIGNED NOT NULL COMMENT '卖家用户ID',
  `score` TINYINT UNSIGNED NOT NULL COMMENT '评分，1-5',
  `content` VARCHAR(1000) DEFAULT NULL COMMENT '评价内容',
  `is_anonymous` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否匿名',
  `status` VARCHAR(32) NOT NULL DEFAULT 'visible' COMMENT '评价状态：visible/hidden/pending_moderation',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_order_item_buyer` (`order_item_id`, `buyer_user_id`),
  KEY `idx_review_product` (`product_id`),
  KEY `idx_review_seller` (`seller_user_id`),
  KEY `idx_review_status` (`status`),
  CONSTRAINT `fk_review_order` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`),
  CONSTRAINT `fk_review_order_item` FOREIGN KEY (`order_item_id`) REFERENCES `order_item` (`id`),
  CONSTRAINT `fk_review_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`),
  CONSTRAINT `fk_review_buyer_user` FOREIGN KEY (`buyer_user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_review_seller_user` FOREIGN KEY (`seller_user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评价表';

CREATE TABLE IF NOT EXISTS `report` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '举报ID',
  `reporter_user_id` BIGINT UNSIGNED NOT NULL COMMENT '举报人用户ID',
  `target_type` VARCHAR(32) NOT NULL COMMENT '举报对象类型：user/product/shop/order',
  `target_id` BIGINT UNSIGNED NOT NULL COMMENT '举报对象ID',
  `reason_type` VARCHAR(32) NOT NULL COMMENT '举报原因类型',
  `content` VARCHAR(1000) DEFAULT NULL COMMENT '举报内容',
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '举报状态：pending/processing/resolved/rejected',
  `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `processed_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `processed_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人ID，当前仅预留',
  `resolution` VARCHAR(500) DEFAULT NULL COMMENT '处理结论',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_report_reporter` (`reporter_user_id`),
  KEY `idx_report_target` (`target_type`, `target_id`),
  KEY `idx_report_status` (`status`),
  CONSTRAINT `fk_report_reporter_user` FOREIGN KEY (`reporter_user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='举报表';

CREATE TABLE IF NOT EXISTS `credit_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '信用记录ID',
  `subject_type` VARCHAR(32) NOT NULL COMMENT '主体类型：user/shop',
  `subject_id` BIGINT UNSIGNED NOT NULL COMMENT '主体ID',
  `event_type` VARCHAR(64) NOT NULL COMMENT '信用事件类型',
  `change_direction` VARCHAR(16) NOT NULL COMMENT '变动方向：increase/decrease',
  `change_value` INT NOT NULL DEFAULT 0 COMMENT '变动值',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '事件说明',
  `occurred_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_credit_record_subject` (`subject_type`, `subject_id`),
  KEY `idx_credit_record_event_type` (`event_type`),
  KEY `idx_credit_record_occurred_at` (`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='信用记录表';

CREATE TABLE IF NOT EXISTS `risk_restriction` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '限制记录ID',
  `subject_type` VARCHAR(32) NOT NULL COMMENT '主体类型：user/shop',
  `subject_id` BIGINT UNSIGNED NOT NULL COMMENT '主体ID',
  `restriction_type` VARCHAR(32) NOT NULL COMMENT '限制类型：login/trade/publish/open_shop/shop_operation',
  `reason` VARCHAR(500) NOT NULL COMMENT '限制原因',
  `status` VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '限制状态：active/released/expired',
  `effective_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生效时间',
  `expired_at` DATETIME DEFAULT NULL COMMENT '结束时间',
  `released_at` DATETIME DEFAULT NULL COMMENT '解除时间',
  `released_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '解除操作人ID，当前仅预留',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_risk_restriction_subject` (`subject_type`, `subject_id`),
  KEY `idx_risk_restriction_status` (`status`),
  KEY `idx_risk_restriction_effective_at` (`effective_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='风控限制表';
