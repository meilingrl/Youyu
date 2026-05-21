# Comment Completeness Sprint

## Metadata

- ID: comment-completeness-sprint
- Status: archived
- Owner: meilingrl
- Track: cross-cutting
- Depends on: W9 of code-quality-cleanup-sprint (completed)
- Priority: medium
- Planned date: 2026-05-19

## Objective

为后端核心业务逻辑补充 WHY 型注释（设计意图、非显而易见的约束、业务规则），为前端 Stores 和 Utils 的导出函数补充 JSDoc。

## Background

W9（code-quality-cleanup-sprint）已完成 Search + Recommend 模块的算法/常量注释。本轮覆盖剩余模块：Review、Payment、Order、TransactionDataStore（后端），以及全部前端 Stores 和 Utils（前端）。

注释原则：
- **写 WHY，不写 WHAT**：解释为什么这么做，不解释代码做了什么
- **跳过自解释的代码**：命名已经说清楚了就不加
- **不修改任何逻辑代码**：纯注释变更

## Work Items Overview

```
W1 ─┐
W2 ─┤
W3 ─┤
W4 ─┼─ 全部并行（8 项独立，无文件重叠）
W5 ─┤
W6 ─┤
W7 ─┤
W8 ─┘
```

---

## W1: ReviewServiceImpl 评分聚合与降级策略注释

### 范围

- `backend/src/main/java/.../service/review/impl/ReviewServiceImpl.java`

### 具体改动

为以下位置补充注释（只加注释，不改逻辑）：

**1. `submitProductReview` 方法内，`recalculateProductRating` 调用处（约第 79 行）**

当前注释 `// Recalculate product rating` 只说做了什么。替换为说明 WHY：

```java
// 重新聚合并持久化评分到 products 表的冗余字段（rating_score / review_count），
// 避免每次商品列表/详情读取时执行 AVG / COUNT 聚合查询。
// 注意：此操作在事务内执行，并发提交评价时存在短暂的读写竞争窗口。
recalculateProductRating(productId);
```

**2. `submitShopReview` 方法内，`recalculateShopRating` 调用处（约第 130 行）**

同理：

```java
// 同上：将店铺评分聚合并写入 shops 表冗余字段，以读优化换写开销。
recalculateShopRating(shopId);
```

**3. `recalculateProductRating` 方法体（约第 230-241 行）**

方法级别加 JavaDoc：

```java
/**
 * 重新聚合所有商品评价分数，更新 products 表的冗余评分字段。
 * avgScore 四舍五入到 2 位小数以避免浮点 artifacts（如 4.299999999）。
 * 防御性的 instanceof Number 类型检查兼容 JDBC 驱动返回 BigDecimal / Double / null 的差异。
 */
```

**4. `recalculateShopRating` 方法体（约第 243-254 行）**

```java
/**
 * 同上：重新聚合店铺评分，更新 shops 表冗余字段。
 */
```

**5. `getProductReviewSummary` 中 JDBC 取值处（约第 167-168 行）**

在 `instanceof Number n` 行前加注释：

```java
// 防御性解包：JDBC queryForList 返回原始 Object，可能是 BigDecimal、Double、Long 或 null。
// Mapper 层 SQL 已使用 COALESCE(AVG(score), 0.0)，此处是双重保险。
```

**6. `getProductReviewSummary` 中 distribution stub 处（约第 174-181 行）**

当前注释替换为带 TODO 标记的版本：

```java
// TODO: 用真实 GROUP BY 查询替换 stub distribution：
//   SELECT score, COUNT(*) FROM reviews WHERE product_id = ? GROUP BY score
// 当前返回 5 个评分级别全 0，作为 MVP 占位。
```

**7. `getShopReviewSummary` 中 distribution stub 处（约第 198-204 行）**

同理替换为 TODO 标记。

### 验收标准

- [ ] 7 处注释已添加/修正
- [ ] 未修改任何逻辑代码
- [ ] `mvnw.cmd test` 通过

---

## W2: JdbcReviewMapper SQL 语义注释

### 范围

- `backend/src/main/java/.../mapper/review/impl/JdbcReviewMapper.java`

### 具体改动

**1. `summarizeProductRatings` SQL 中 COALESCE 处（约第 90 行）**

在 SQL 字符串前加注释：

```java
// COALESCE 确保无评价时 avg_score 返回 0.0 而非 NULL——SQL 的 AVG() 对空结果集返回 NULL。
```

**2. `summarizeShopRatings` SQL 中 COALESCE 处（约第 154 行）**

同样加注释：

```java
// 同上：COALESCE 防止空结果集时 AVG() 返回 NULL。
```

**3. `findPendingReviewableOrderItems` SQL（约第 188-198 行）**

在方法上加 JavaDoc 或 SQL 上方加注释，解释反连接（anti-join）模式：

```java
/**
 * 查找买家已完成但尚未评价的订单商品。
 *
 * 使用 LEFT JOIN + WHERE r.id IS NULL 实现反连接（anti-join）：
 * 找出 order_items 中不存在对应 review 的行。
 * 注意：r.buyer_user_id = ? 写在 LEFT JOIN 条件中而非 WHERE 中——
 * 如果放到 WHERE 会使 LEFT JOIN 退化为 INNER JOIN（因为 NULL = ? 永远为 false）。
 */
```

**4. `findMyProductReviews` SQL 中 LEFT JOIN 处（约第 219-224 行）**

```java
// LEFT JOIN（而非 INNER JOIN）：即使 order_item 被删除或成为孤儿行，
// 用户的评价历史仍然展示（product_title / product_image 为 null），优雅降级。
```

### 验收标准

- [ ] 4 处注释已添加
- [ ] 未修改任何逻辑代码
- [ ] `mvnw.cmd test` 通过

---

## W3: PaymentServiceImpl 支付流程注释

### 范围

- `backend/src/main/java/.../service/payment/impl/PaymentServiceImpl.java`

### 具体改动

**1. `gatewayInfo()` 方法（约第 27-32 行）**

```java
// 当前为 Mock 网关实现。接入真实支付网关时，此方法改为根据 payment_method
// 动态选择 PaymentChannelRouter 返回的网关信息。可通过 @Profile("dev") 条件化加载 Mock。
```

**2. `initiatePayment` 中订单状态检查（约第 43 行）**

```java
// 状态机约束：只有 PENDING_PAYMENT 状态的订单才能发起支付，
// 转换路径 PENDING_PAYMENT → (支付中) → PAID / FAILED。
```

**3. `initiatePayment` 中 hasActivePayment 检查（约第 46-48 行）**

```java
// 幂等性保护：同一订单已有非失败支付记录时拒绝重复创建。
// 失败支付允许重试（paymentStatus == "failed" 的支付不计为 active）。
```

**4. `initiatePayment` 中 createPayment 调用（约第 52-56 行）**

```java
// gatewayCode = "internal_mock" 是 Mock 实现占位。
// 接入真实网关后，gatewayCode 由 PaymentChannelRouter 按 payment_method 动态决定。
```

**5. `completeMockPayment` 中幂等性 guard（约第 77-79 行）**

```java
// 幂等性 guard：防止重复完成已成功的支付。
```

**6. `completeMockPayment` 中双状态更新（约第 80-83 行）**

```java
// 双状态同步：同时更新 payment.paymentStatus 和 order.paymentStatus。
// 订单级状态提供汇总视图，支付记录级状态追踪每次支付尝试。
// 注意：若支付网关回调丢失，此处可能成为数据不一致的源头。
```

**7. `completeMockPayment` 中履行类型分支（约第 85-96 行）**

```java
// 数字商品：付款完成即视为发货，跳过 PENDING_FULFILLMENT 直接进入 PENDING_RECEIPT。
//   买家确认收货后即可下载完整数字资产。
// 非数字商品（物流/线下）：付款后进入 PENDING_FULFILLMENT，等待卖家操作（发货/会面）。
```

**8. `nextActionAfterPayment` 方法（约第 109-122 行）**

```java
/**
 * 根据履行类型返回支付后的下一步动作提示。
 * 返回值格式：actor_action_detail（下划线分隔），由前端渲染为操作按钮/引导文案。
 * - 数字商品：buyer_confirm_receipt_to_unlock_full_download（买家确认收货解锁完整下载）
 * - 物流商品：seller_fill_tracking_info（卖家填写快递信息）
 * - 线下商品：wait_for_offline_appointment_and_double_confirmation（等待双方确认）
 */
```

### 验收标准

- [ ] 8 处注释已添加
- [ ] 未修改任何逻辑代码
- [ ] `mvnw.cmd test` 通过

---

## W4: OrderServiceImpl 订单生命周期注释

### 范围

- `backend/src/main/java/.../service/order/impl/OrderServiceImpl.java`

### 具体改动

**1. `cart()` 中 selected 过滤（约第 40-43 行）**

```java
// 只有 selected=true 的购物车项目参与金额计算，
// 允许用户在不删除商品的情况下只结算部分购物车。
```

**2. `addCartItem()` 中 saveCartItem 调用（约第 61 行）**

```java
// saveCartItem 实现 Upsert 语义：已存在的商品更新数量，不存在则新增。
// 不会创建重复的购物车行。
```

**3. `createOrder()` 中地址快照（约第 99-105 行）**

```java
// 地址在订单创建时快照（值拷贝），而非外键引用。
// 用户后续修改地址不影响已有订单的收货地址。
```

**4. `createOrder()` 中 offlineMeetTime 格式转换（约第 112-116 行）**

```java
// replace(" ", "T") 将前端友好格式 "2026-05-19 14:30" 转为 ISO-8601 格式供 LocalDateTime.parse() 解析。
```

**5. `createOrder()` 中 createOrder 调用（约第 118 行）**

```java
// TransactionDataStore.createOrder() 在一个方法内原子执行：
// 插入 orders + order_items + order_fulfillments + 删除购物车项。
// 若中途失败，JDBC 事务回滚保证不会产生部分订单。
```

**6. `getOrderDetail()` 中 adminView 安全检查（约第 146 行）**

```java
// adminView = true 绕过买家身份校验（assertBuyer），仅限 AdminController 路径设置。
// 前端不可信，此参数应由服务端管理端点在服务端硬编码为 true。
```

**7. `getOrderDetail()` 中 buildAvailableActions 设计说明（约第 159 行）**

```java
// buildAvailableActions() 返回当前订单状态下可执行的操作列表，
// 前端根据此列表渲染按钮，而非硬编码自己的状态机规则。
```

**8. `getOrderDetail()` 中退款规则占位（约第 162-163 行）**

```java
// refundRuleText 为 MVP 占位文本。
// TODO: 接入真实支付网关后实现实际退款规则文案。
```

**9. `cancelOrder()` 中 closedReason（约第 174 行）**

```java
// 取消原因枚举：buyer_cancelled（买家取消）/ system_cancelled（系统取消）/ payment_timeout（支付超时）。
// 当前 MVP 仅使用 buyer_cancelled。
```

**10. `confirmReceipt()` 中线下订单委派（约第 183-184 行）**

```java
// 线下订单委派到 buyerConfirmOffline()，因为线下需要买卖双方双人确认，
// 无法在此通用方法中完成。
```

**11. `confirmReceipt()` 中数字资产访问时间线（约第 191-194 行）**

```java
// 确认收货后设置 full_access：买家从此时起可下载完整数字资产。
// 在此之前（付款后、确认前）仅能访问预览资产。
```

**12. `sellerConfirmOffline()` 中幂等性 guard（约第 222-225 行）**

```java
// 幂等性 guard：若订单已是 PENDING_RECEIPT（买家先确认），跳过状态转换，
// 但仍需更新履行数据。
```

**13. `sellerConfirmOffline()` 中双人确认（约第 230-232 行）**

```java
// 线下订单双人确认：任一方确认时检查另一方是否已确认。
// 若双方均已确认 → 订单立即完成（finalizeOfflineOrder）。
```

**14. `buyerConfirmOffline()` 中双人确认（约第 252-254 行）**

```java
// 同上：买家侧对称逻辑，与 sellerConfirmOffline 镜像。
```

**15. `applyRefund()` 中数字商品不可退款（约第 262 行）**

```java
// 数字商品在内容完整交付后不支持退款。
// 政策理由：数字内容一旦下载即不可撤回。
```

**16. `applyRefund()` 中状态机 guard（约第 266-268 行）**

```java
// 状态机闭合性：CANCELLED 和 REFUNDED 是终态，不可再进入退款流程。
// 仅非终态订单可以申请退款。
```

**17. `applyRefund()` 中使用最新支付记录（约第 284 行）**

```java
// 取最新支付记录退款（按 initiated_at 排序的最后一条），匹配支付网关惯例。
// 注意：如同一订单存在多次成功支付，此逻辑可能需要改为按 payment_id 精确定位。
```

**18. `accessDigitalAsset()` 中访问控制（约第 320-321 行）**

```java
// 仅 full_access 状态允许下载完整资产，预览资产（is_preview_asset）走单独的公开端点。
// 两层访问模型：预览资产无需授权；完整资产需购买 + 确认收货。
```

**19. `accessDigitalAsset()` 中预览资产拒绝（约第 333-335 行）**

```java
// 预览资产不通过此端点提供——拒绝请求以避免访问控制混淆。
```

**20. `buildOrderPreview()` 中跨卖家限制（约第 362-364 行）**

```java
// 限制：当前 MVP 仅支持同一卖家商品合并下单。
// 跨卖家拆分订单为未来迭代范围。
```

**21. `buildOrderPreview()` 中履行类型交集（约第 366-369 行）**

```java
// 取所有购物车商品支持履行类型的交集（最小公分母策略）：
// 若任一商品不支持物流，则该批次订单不可选物流。
```

**22. `buildOrderPreview()` 中 discountAmount 占位（约第 401-402 行）**

```java
// TODO: 折扣/促销尚未实现，discountAmount 固定为零。
// 未来扩展点：优惠券码、会员折扣、满减促销。
```

**23. `buildAvailableActions()` 方法级（约第 482-509 行）**

```java
/**
 * 构建基于订单状态的可用操作列表。
 *
 * 操作列表由前端渲染为按钮——前端不应硬编码自己的状态机，
 * 而应根据此列表决定显示哪些操作。
 *
 * 映射关系（按状态）：
 *   PENDING_PAYMENT  → pay / cancel
 *   PENDING_RECEIPT  → confirm_receipt（非线下）/ offline actions（线下）
 *   PENDING_FULFILLMENT → seller_ship（管理员）
 *   REFUNDING        → complete_refund（管理员）
 *   (已支付 + 非数字 + 未退款) → apply_refund（买家）
 */
```

**24. `buildAccessibleDigitalAssets()` 方法级（约第 512-525 行）**

```java
/**
 * 构建可访问的数字资产列表。
 *
 * 两层访问模型：
 * - 预览资产：始终包含（付款后即可见，帮助买家决定是否确认收货）
 * - 完整资产：仅 full_access 状态下包含（确认收货后才可下载）
 */
```

**25. `ensureProductPurchasable()` 中条件逻辑（约第 583 行）**

```java
// 购买条件：
// 1) 商品必须在售（status = on_sale）
// 2) 数字商品额外要求审核通过（review_status = approved）
//    实体商品不要求审核通过——只要在售即可购买。
```

### 验收标准

- [ ] 25 处注释已添加
- [ ] 未修改任何逻辑代码
- [ ] `mvnw.cmd test` 通过

---

## W5: TransactionDataStore 架构模式注释

### 范围

- `backend/src/main/java/.../service/transaction/support/TransactionDataStore.java`

### 具体改动

**1. `PersistentMap` 内部类（约第 766-782 行）**

在类定义前加注释：

```java
/**
 * 可持久化的 Map 包装器——重写 put() 在值变更时自动生成数据库 UPDATE。
 *
 * 设计目的：避免显式 save() 调用，减少样板代码。
 * 陷阱：调用者通过 map.put("field", value) 即触发数据库写入，无显式保存点。
 *       对返回的 Map 的任何修改都是即时的持久化操作。
 *
 * 注意：loading 字段非 volatile，假设在同步方法内使用。
 *       多线程环境下直接操作此 Map 可能导致竞态。
 */
```

**2. `nextOrderNo()` / `nextPaymentNo()` / `nextRefundNo()` 方法组（约第 597-607 行）**

```java
// 单号格式：CM + yyyyMMdd + - + 6位序号（如 CM20260519-000001）
// PAY...  / REF... 同理。
// 注意：序号为进程内 AtomicLong，重启后从 1 重新计数。
// 当前仅适用于单节点开发/测试——生产环境需持久化序列生成器或数据库序列。
```

**3. `seedCart()` 方法（约第 498-504 行）**

```java
// 演示/开发种子数据——硬编码 userId=1001, productId=3001/3002。
// 仅在购物车为空时填充，不应在生产环境启用。
// TODO: 通过 @Profile("dev") 或配置开关条件化。
```

**4. `toJson()` / `fromJsonMap()` 中 catch 块（约第 726-745 行）**

```java
// 静默降级：序列化/反序列化失败时返回空值或空字符串。
// 注意：此策略可能掩盖数据损坏——特别是 address_snapshot，
// 若序列化失败且无日志告警，订单上的地址快照可能静默丢失。
// TODO: 至少应在降级时输出 warn 日志。
```

### 验收标准

- [ ] 4 处注释已添加
- [ ] 未修改任何逻辑代码
- [ ] `mvnw.cmd test` 通过

---

## W6: stores/market.js JSDoc

### 范围

- `frontend/src/stores/market.js`

### 具体改动

为全部 24 个导出函数添加 JSDoc。格式约定：

```javascript
/**
 * 简短描述函数做什么（中文）。
 *
 * @param {Type} paramName - 参数说明
 * @returns {Type} 返回值说明
 * @sideEffects 修改的响应式状态（ref 名称）
 */
```

函数清单及文档要点：

| # | 函数 | 参数 | 返回值 | 副作用 |
|---|------|------|--------|--------|
| 1 | `isFavorite(productId)` | `{string\|number}` | `{boolean}` | 无 |
| 2 | `toggleFavorite(productId)` | `{string\|number}` | `{Promise<void>}` | 调用远程 API，刷新收藏列表 |
| 3 | `getProductById(productId)` | `{string\|number}` | `{object\|null}` | 无 |
| 4 | `getShopById(shopId)` | `{string\|number}` | `{object\|null}` | 无 |
| 5 | `getProductsByShopId(shopId)` | `{string\|number}` | `{object[]}` | 无 |
| 6 | `getMyProducts()` | 无 | `{object[]}` | 无（纯本地读取） |
| 7 | `getShopInsightById(shopId)` | `{string\|number}` | `{object\|null}` | 无 |
| 8 | `setProducts(list)` | `{object[]} [list]` | `{void}` | 替换 `products` ref，对每项做 normalizeProduct |
| 9 | `loadProducts(params)` | `{object} [params]` 查询/分页参数 | `{Promise<object[]>}` | 更新 `products`、`searchTotal`、`searchPage`、`searchPageSize`、`loadingProducts`、`productError` |
| 10 | `loadProductDetail(productId)` | `{string\|number}` | `{Promise<object>}` | 向 `products` 列表 upsert，重新派生分类 |
| 11 | `loadMyProducts()` | 无 | `{Promise<object[]>}` | 填充 `myProducts` ref |
| 12 | `publishProduct(payload)` | `{object}` 创建商品参数 | `{Promise<object>}` | 向 `myProducts` 和 `products` 头部插入 |
| 13 | `loadFavorites()` | 无 | `{Promise<object[]>}` | 填充 `favoriteIds`，扩展 `products` |
| 14 | `toggleFavoriteRemote(productId)` | `{string\|number}` | `{Promise<void>}` | 通过 API 切换收藏后刷新 |
| 15 | `loadMyShop()` | 无 | `{Promise<object>}` | 填充 `ownedShop` ref，扩展 `shops`、`products` |
| 16 | `loadShopDetail(shopId)` | `{string\|number}` | `{Promise<object>}` | 扩展 `shops`、`products` |
| 17 | `loadProfile(currentUser)` | `{object} [currentUser]` 回退用户数据 | `{Promise<object>}` | 填充 `profile`，管理 `loadingProfile` / `profileError` |
| 18 | `loadUserAddresses()` | 无 | `{Promise<object[]>}` | 合并地址到 `profile` |
| 19 | `applyUserPreference(payload, source)` | `{object}` 偏好字段 / `{string} [source]` 来源标签 | `{void}` | 写入 localStorage，更新 `userPreference` ref |
| 20 | `loadUserPreference()` | 无 | `{Promise<object>}` | 从 API 填充 `userPreference`，管理 `loadingPreference` / `preferenceError` |
| 21 | `updateUserPreference(payload)` | `{object}` 偏好字段 | `{Promise<object>}` | API 持久化 + 本地应用，管理 `savingPreference` / `preferenceError` |
| 22 | `loadUserInsightSnapshot()` | 无 | `{Promise<object>}` | 填充 `userInsightSnapshot`，管理 `loadingUserInsight` / `userInsightError` / `userInsightStatus` |
| 23 | `loadShopInsightSnapshot(shopId)` | `{string\|number}` | `{Promise<object>}` | 填充 `shopInsightById[shopId]`，管理 `loadingShopInsight` / `shopInsightError` |
| 24 | `getVerificationDraft()` | 无 | `{object}` | 无（同步方法，基于 `profile` 当前值计算默认值） |

### 验收标准

- [ ] 24 个导出函数均有 JSDoc
- [ ] JSDoc 格式符合约定（@param / @returns / @sideEffects）
- [ ] 未修改任何逻辑代码
- [ ] `npm run build` 通过

---

## W7: stores/review.js + recommend.js + auth.js JSDoc

### 范围

- `frontend/src/stores/review.js`
- `frontend/src/stores/recommend.js`
- `frontend/src/stores/auth.js`

### 具体改动

为三个 Store 的全部导出函数添加 JSDoc（格式同 W6）。

**review.js（8 个函数）：**

| # | 函数 | 参数 | 副作用 |
|---|------|------|--------|
| 1 | `loadPendingReviews()` | 无 | `pendingItems`、`loadingPending` / `pendingError` |
| 2 | `loadMyReviews()` | 无 | `myProductReviews` / `myShopReviews`、`loadingMyReviews` / `myReviewsError` |
| 3 | `doSubmitProductReview(payload)` | `{object}` 评价内容 | `submittingProduct` / `submitProductError` |
| 4 | `doSubmitShopReview(payload)` | `{object}` 评价内容 | `submittingShop` / `submitShopError` |
| 5 | `loadProductReviews(productId, page, pageSize)` | `{string\|number}` / `{number} [page=1]` / `{number} [pageSize=10]` | `productReviews` / `productReviewTotal`，loading/error |
| 6 | `loadProductReviewSummary(productId)` | `{string\|number}` | `productReviewSummary`，loading/error |
| 7 | `loadShopReviews(shopId, page, pageSize)` | `{string\|number}` / `{number} [page=1]` / `{number} [pageSize=10]` | `shopReviews` / `shopReviewTotal`，loading/error |
| 8 | `loadShopReviewSummary(shopId)` | `{string\|number}` | `shopReviewSummary`，loading/error |

**recommend.js（2 个函数）：**

| # | 函数 | 参数 | 副作用 |
|---|------|------|--------|
| 1 | `loadHomeRecommend(limit)` | `{number} [limit=8]` | `homeRecommendList`、`loadingHome` / `homeError` |
| 2 | `loadAlsoBought(productId, limit)` | `{string\|number}` / `{number} [limit=6]` | `alsoBoughtList`、`loadingAlsoBought` / `alsoBoughtError` |

**auth.js（4 个函数）：**

| # | 函数 | 参数 | 副作用 |
|---|------|------|--------|
| 1 | `login(credentials)` | `{object}` 包含 loginId 和 password | 写入 localStorage session |
| 2 | `registerAsUser(payload)` | `{object} [payload]` | 不设置 session（需后续登录） |
| 3 | `setSession(payload)` | `{object\|null}` | 写入 localStorage，更新 `session` ref |
| 4 | `logout()` | 无 | 清除 `session` ref，移除 localStorage 中的认证数据 |

### 验收标准

- [ ] review.js 8 个导出函数均有 JSDoc
- [ ] recommend.js 2 个导出函数均有 JSDoc
- [ ] auth.js 4 个导出函数均有 JSDoc
- [ ] 未修改任何逻辑代码
- [ ] `npm run build` 通过

---

## W8: frontend utils JSDoc

### 范围

- `frontend/src/utils/market-normalizers.js`
- `frontend/src/utils/auth.js`
- `frontend/src/utils/storage.js`
- `frontend/src/utils/error-utils.js`

（`async-store-helper.js` 已有完整 JSDoc，跳过）

### 具体改动

为四个文件全部导出函数添加 JSDoc。

**market-normalizers.js（4 个函数）：**

| # | 函数 | 参数 | 返回值 |
|---|------|------|--------|
| 1 | `normalizeProduct(raw)` | `{object} [raw={}]` API 原始产品数据 | `{object}` 规范化产品（所有字段保证存在） |
| 2 | `categoriesFromProducts(products)` | `{object[]} [products=[]]` | `{object[]}` 去重后的 `[{id, name}]` 列表 |
| 3 | `normalizeShop(raw)` | `{object} [raw={}]` API 原始店铺数据 | `{object}` 规范化店铺 |
| 4 | `normalizeProfile(raw, currentUser)` | `{object} [raw={}]` / `{object\|null} [currentUser=null]` | `{object}` 带嵌套 verification / privilege / addresses 的规范化 profile |

**auth.js（4 个函数）：**

| # | 函数 | 参数 | 返回值 / 副作用 |
|---|------|------|----------------|
| 1 | `getAuthStorage()` | 无 | `{object\|null}` 从 localStorage 读取 |
| 2 | `setAuthStorage(payload)` | `{object\|null}` | 写入 localStorage |
| 3 | `clearAuthStorage()` | 无 | 移除 localStorage key |
| 4 | `getAuthToken()` | 无 | `{string}` 从 session 提取 token |

**storage.js（3 个函数）：**

| # | 函数 | 参数 | 返回值 / 副作用 |
|---|------|------|----------------|
| 1 | `getStorage(key, fallback)` | `{string}` / `{*} [fallback=null]` | `{*}` 解析后的 JSON 值或 fallback |
| 2 | `setStorage(key, value)` | `{string}` / `{*}` | 写入 JSON 序列化值到 localStorage |
| 3 | `removeStorage(key)` | `{string}` | 从 localStorage 移除 key |

**error-utils.js（1 个函数）：**

| # | 函数 | 参数 | 返回值 |
|---|------|------|--------|
| 1 | `resolveErrorMessage(error)` | `{*}` API 错误对象或异常 | `{string}` 中文错误消息 |

### 验收标准

- [ ] market-normalizers.js 4 个导出函数均有 JSDoc
- [ ] auth.js 4 个导出函数均有 JSDoc
- [ ] storage.js 3 个导出函数均有 JSDoc
- [ ] error-utils.js 1 个导出函数有 JSDoc
- [ ] 未修改任何逻辑代码
- [ ] `npm run build` 通过

---

## Risks

- **W4（OrderServiceImpl）** 注释数量最多（25 处），执行时间最长。但所有注释位置已精确定位到行号/方法名，Agent 可逐条添加。
- 前端 JSDoc（W6-W8）是机械性工作，Agent 按表格照填即可，出错概率低。
- 所有 Work Item 操作不同文件，零合并冲突。

## Documentation Updates Required

- [x] 各 Work Item 完成后更新本文档对应项的完成状态
- [x] 全部完成后，`code-quality-cleanup-sprint.md` 的 W9 可标记为全面完成
- [x] 全部完成后将本文档移至 `archived/`

## Completion Notes

- **2026-05-19**: 8 个子 Agent 并行执行，全部通过。98 处注释/JSDoc 覆盖 15 个文件。
- 后端 `mvnw.cmd test` 80/80 pass，前端 `npm run build` clean。
- Commit `902a41a`，已 push 到 `codex/ui-ux-redesign`。
