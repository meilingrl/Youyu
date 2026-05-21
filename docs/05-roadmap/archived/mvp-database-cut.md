# MVP 数据库裁剪建议

## 1. 目的

本文件用于把核心实体模型裁剪成 `MVP` 第一阶段实际应落表的范围。

原则：

- 先保证闭环
- 再考虑高级能力
- 预留扩展，但不一次建完所有表

## 2. MVP 必做表

### 用户与身份域

必须落表：

- `user`
- `student_verification`
- `user_privilege_profile`
- `user_address`

原因：

- 承载账号、认证、权限和收货/交付信息

### 商品与类目域

必须落表：

- `category`
- `product`
- `product_media`
- `product_review_task`

建议落表：

- `scenario_tag`
- `major_tag`
- `product_tag_relation`

说明：

- 如果时间紧，标签关系可暂缓到第二阶段

### 店铺与经营域

必须落表：

- `shop`
- `shop_capability_profile`

建议落表：

- `shop_follow`

说明：

- 店铺关注不是主闭环阻塞项，可后置

### 交易与支付域

必须落表：

- `cart_item`
- `order`
- `order_item`
- `order_fulfillment`
- `payment_record`
- `refund_record`

原因：

- 这是交易闭环核心

### 内容与评价域

必须落表：

- `product_favorite`
- `review`

建议落表：

- `review_media`

### 风险与治理域

必须落表：

- `report`
- `credit_record`
- `risk_restriction`

### 电子交付域

必须落表：

- `product_digital_asset`

原因：

- 电子商品是主业务方向之一，不能只靠字符串字段临时拼接

## 3. MVP 可延后表

可以延后到第二阶段：

- `coupon`
- `shop_follow`
- `review_media`
- 更细粒度标签关系表
- 数据分析中间表或统计快照表

## 4. 字段设计原则

### 审计字段

核心表建议统一保留：

- `created_at`
- `updated_at`
- `created_by`（如适用）
- `updated_by`（如适用）

### 逻辑删除

对管理型实体建议优先采用：

- `is_deleted`

适用对象：

- 商品
- 类目
- 店铺
- 地址

### 状态枚举

每个领域对象的状态字段必须统一命名与枚举管理，避免散落硬编码。

建议优先统一：

- 用户状态
- 认证状态
- 商品状态
- 审核状态
- 店铺状态
- 订单主状态
- 支付状态
- 退款状态
- 举报状态

## 5. 开发落地建议

第一轮数据库实现建议顺序：

1. 用户与认证
2. 商品与审核
3. 店铺
4. 订单与支付
5. 举报与信用
6. 电子资料资源

## 6. 当前结论

`MVP` 数据库不追求最少表，而追求“结构正确、主链路完整、后续扩展不推翻”。
