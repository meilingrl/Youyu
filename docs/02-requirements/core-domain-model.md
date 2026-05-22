# 核心实体模型

## 1. 目的

本文档定义 `Youyu` 的核心业务对象，用于指导数据库设计、接口设计和模块拆分。

本文档关注的是业务对象与职责，不直接等同于最终物理表结构。

## 2. 当前建模原则

- 一个学生用户是统一主体，不拆买家表和卖家表
- 权限、认证、信用、店铺能力通过状态与关联实体表达
- 订单主状态与履约细节分离
- 交易记录与分析记录分离
- 偏好设置与交易主链解耦
- 统计能力优先基于业务数据衍生，不强行单独造复杂分析域

## 3. 领域划分

当前建议按以下领域组织：

- 用户与身份域
- 商品与目录域
- 店铺与经营域
- 交易与支付域
- 内容与互动域
- 风险与治理域
- 洞察与偏好域

## 4. 用户与身份域

### 4.1 User

职责：

- 平台统一主体
- 承载账号、公开资料与基础状态

关键属性：

- userId
- username
- phone
- email
- passwordHash
- nickname
- avatar
- status
- registeredAt
- lastLoginAt

### 4.2 StudentVerification

职责：

- 承载学生身份认证申请与审核结果

关键属性：

- verificationId
- userId
- studentNo
- realName
- college
- major
- grade
- campusEmail
- verificationMethod
- verificationStatus
- submittedAt
- reviewedAt
- reviewerId
- rejectReason

### 4.3 UserPrivilegeProfile

职责：

- 表达当前用户可拥有的交易权限

关键属性：

- userId
- canPurchase
- canPublish
- canReview
- canApplyShop
- isRestricted
- restrictedReason

### 4.4 UserAddress

职责：

- 承载物流地址与校园场景常用交付地点

关键属性：

- addressId
- userId
- receiverName
- receiverPhone
- addressType
- province
- city
- district
- detailAddress
- campusArea
- isDefault

## 5. 商品与目录域

### 5.1 Category

职责：

- 商品分类体系

关键属性：

- categoryId
- parentId
- name
- sortOrder
- status

### 5.2 Product

职责：

- 平台统一商品主体
- 兼容实物商品与电子资料

关键属性：

- productId
- sellerUserId
- shopId
- title
- subtitle
- description
- productType
- productStatus
- reviewStatus
- price
- originalPrice
- stock
- categoryId
- supportLogistics
- supportOffline
- supportDigital
- supportPreview
- previewRule
- viewCount
- favoriteCount

### 5.3 ProductMedia

职责：

- 承载商品图片与预览图

关键属性：

- mediaId
- productId
- mediaType
- url
- sortOrder

### 5.4 ProductDigitalAsset

职责：

- 承载电子商品的受控资源

关键属性：

- assetId
- productId
- assetType
- assetName
- storagePath
- isPreview
- previewRule
- status

### 5.5 ProductReviewTask

职责：

- 承载资料类商品的审核任务

关键属性：

- reviewTaskId
- productId
- reviewType
- reviewStatus
- submittedAt
- reviewedAt
- reviewerId
- rejectReason

## 6. 店铺与经营域

### 6.1 Shop

职责：

- 学生卖家进入持续经营阶段后的经营主体

关键属性：

- shopId
- ownerUserId
- name
- description
- cover
- status
- score
- notice
- createdAt

### 6.2 ShopCapabilityProfile

职责：

- 表达店铺当前可获得的经营能力档位

关键属性：

- shopId
- capabilityLevel
- maxProductCount
- canSetNotice
- canSetLoyaltyDiscount
- canUseCoupon
- canJoinActivity

## 7. 交易与支付域

### 7.1 CartItem

职责：

- 购物车条目

关键属性：

- cartItemId
- userId
- productId
- quantity
- selected

### 7.2 Order

职责：

- 交易主单

关键属性：

- orderId
- orderNo
- buyerUserId
- sellerUserId
- shopId
- orderStatus
- fulfillmentType
- paymentStatus
- goodsAmount
- discountAmount
- payAmount
- createdAt
- paidAt
- completedAt
- cancelledAt

### 7.3 OrderItem

职责：

- 订单商品快照

关键属性：

- orderItemId
- orderId
- productId
- titleSnapshot
- imageSnapshot
- priceSnapshot
- quantity
- subtotalAmount
- productTypeSnapshot

### 7.4 OrderFulfillment

职责：

- 承载物流、线下、电子交付的差异化细节

关键属性：

- fulfillmentId
- orderId
- fulfillmentType
- fulfillmentStatus
- sellerConfirmedAt
- buyerConfirmedAt
- logisticsNo
- logisticsCompany
- offlineMeetingTime
- offlineMeetingPlace
- digitalAccessOpenedAt

### 7.5 PaymentRecord

职责：

- 承载模拟支付与未来支付网关接入的统一支付记录

关键属性：

- paymentRecordId
- orderId
- paymentNo
- paymentMethod
- paymentChannel
- paymentStatus
- amount
- initiatedAt
- succeededAt
- failedReason

### 7.6 RefundRecord

职责：

- 承载退款申请与处理结果

关键属性：

- refundRecordId
- orderId
- paymentRecordId
- refundStatus
- refundAmount
- refundReason
- appliedAt
- processedAt
- completedAt

## 8. 内容与互动域

### 8.1 ProductFavorite

职责：

- 用户与商品收藏关系

### 8.2 Review

职责：

- 用户对订单或商品的评价

### 8.3 BrowseHistory

职责：

- 用户最近浏览留痕
- 作为个人洞察与推荐预留基础

关键属性：

- browseHistoryId
- userId
- productId
- viewedAt
- sourcePage

## 9. 风险与治理域

### 9.1 Report

职责：

- 举报记录

### 9.2 CreditRecord

职责：

- 用户或店铺的信用事件记录

### 9.3 RiskRestriction

职责：

- 用户或店铺的限制措施记录

## 10. 洞察与偏好域

### 10.1 UserPreference

职责：

- 承载用户界面偏好与默认交易设置

关键属性：

- userId
- themeMode
- themeColor
- defaultAddressId
- defaultFulfillmentType
- defaultPaymentMethod
- defaultSortType
- notificationPreference

说明：

- 该对象与交易主链低耦合
- 适合在当前阶段并行开发

### 10.2 UserInsightSnapshot

职责：

- 承载用户中心的轻量统计摘要

关键属性：

- userId
- totalSpendAmount
- totalPaidOrderCount
- recentBrowseCount
- favoriteCategorySummary
- lastCalculatedAt

说明：

- 当前阶段可以先定义接口与聚合口径
- 可先不落为单独物理表，允许通过查询实时拼装

### 10.3 ShopInsightSnapshot

职责：

- 承载店铺中心的轻量经营摘要

关键属性：

- shopId
- monthlySalesAmount
- monthlyOrderCount
- hotProductSummary
- viewCountSummary
- repeatBuyerCount
- lastCalculatedAt

## 11. 当前阶段设计取舍

当前明确独立建模的对象：

- StudentVerification
- UserPrivilegeProfile
- ShopCapabilityProfile
- OrderFulfillment
- PaymentRecord
- RefundRecord
- ProductDigitalAsset
- BrowseHistory
- UserPreference
- CreditRecord
- RiskRestriction

当前不急于独立深化的对象：

- 复杂营销活动体系
- 推荐算法实体
- 独立售后主单体系
- 多商户结算体系

## 12. 下一步建模建议

基于本文档，当前最自然的下一步工作是：

1. 以 `MVP` 优先级裁剪实体落库顺序
2. 先打通交易、认证、审核主链
3. 再将 `BrowseHistory`、`UserPreference`、`UserInsightSnapshot`、`ShopInsightSnapshot` 接入真实数据能力
