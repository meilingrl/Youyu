# Task 04: 订单卡片消息

## Metadata

- **Task ID**: task-04
- **Priority**: P1
- **Estimated effort**: 6-8 hours
- **Dependencies**: task-02 (图片消息支持)
- **Assignee**: 待分配

---

## Context

### 问题描述

售后场景中,买家和卖家需要讨论订单问题（物流、退款、商品质量等）。订单卡片消息可以快速关联订单信息,方便双方查看订单详情。

### 业务价值

订单卡片消息能够显著提升售后沟通效率,双方可以快速定位到具体订单,减少沟通成本。

---

## Goals

实现以下功能:

1. **订单详情页添加"联系卖家/买家"按钮**: 自动发送订单卡片
2. **消息气泡显示订单卡片**: 显示订单号、状态、商品、金额
3. **点击卡片跳转到订单详情页**: 点击"查看订单详情"按钮跳转
4. **订单状态实时显示**: 订单卡片显示最新状态

---

## Database Schema

### `chat_messages` 表添加订单关联字段

需要添加以下字段:
- `order_id` (BIGINT, NULL): 关联订单 ID（订单卡片消息）
- 外键约束: `FOREIGN KEY (order_id) REFERENCES orders(id)`

需要添加索引:
- `idx_message_order`: 索引 `(order_id)`,用于查询订单相关的消息

**设计说明**:
- `message_type = 'order_card'` 时,`order_id` 必需
- `body` 字段可选（用于附加说明,如"物流有问题"）
- 订单信息从 orders 表 JOIN 获取

---

## API Contract

### 1. 发送消息接口（修改现有接口）

**Endpoint**: `POST /api/chat/conversations/{id}/messages`

**Request**:
```json
{
  "body": "物流有问题（可选）",
  "messageType": "order_card",
  "orderId": 456
}
```

**Request Fields**:
- `body` (String, optional): 附加说明
- `messageType` (String): 必须为 "order_card"
- `orderId` (Long): 订单 ID（必需）

**Business Logic**:
- 验证订单是否存在
- 验证用户是订单参与者（买家或卖家）
- 插入消息时关联订单 ID

### 2. 获取消息接口（修改现有接口）

**Endpoint**: `GET /api/chat/conversations/{id}/messages`

**Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 126,
        "messageType": "order_card",
        "body": "物流有问题",
        "orderId": 456,
        "order": {
          "id": 456,
          "orderNumber": "ORD-20260525-001",
          "status": "shipped",
          "totalAmount": 199.00,
          "productTitle": "商品标题",
          "productImage": "https://example.com/product.jpg",
          "createdAt": "2026-05-25T09:00:00"
        },
        "createdAt": "2026-05-25T10:03:00"
      }
    ]
  }
}
```

**Business Logic**:
- 查询消息时,如果 `message_type = 'order_card'`,自动 JOIN `orders` 和 `order_items` 表
- 返回订单的必要信息: id, orderNumber, status, totalAmount, productTitle（第一个商品）, productImage（第一个商品图片）
- 如果订单已删除,`order` 字段为 null

---

## Frontend Requirements

### 1. 订单详情页添加"联系卖家/买家"按钮

在订单详情页添加按钮:
- 买家看到"联系卖家"按钮
- 卖家看到"联系买家"按钮
- 点击按钮自动查找或创建会话
- 自动发送订单卡片消息
- 发送成功后跳转到消息中心

### 2. 订单卡片组件

创建订单卡片组件,显示:
- 订单号（如 "订单 #ORD-20260525-001"）
- 订单状态（带颜色标识）
- 商品图片（60x60px）
- 商品标题（最多 2 行,超出省略）
- 订单金额（红色,加粗）
- "查看订单详情"按钮
- 附加说明（如果有）

### 3. 消息气泡显示订单卡片

在消息气泡中显示订单卡片:
- 订单卡片最大宽度 320px
- 点击"查看订单详情"按钮跳转到订单详情页
- 订单状态颜色映射:
  - pending: 橙色
  - paid: 蓝色
  - shipped: 紫色
  - delivered: 绿色
  - completed: 灰色
  - cancelled: 红色

---

## Acceptance Criteria

### 后端验证

- [ ] 数据库迁移脚本执行成功,字段和索引创建成功
- [ ] 发送订单卡片消息（messageType=order_card, orderId 必需）成功
- [ ] 订单不存在时返回错误
- [ ] 非订单参与者发送订单卡片时返回错误
- [ ] 查询消息时自动附加订单信息（orderNumber, status, totalAmount, productTitle, productImage）
- [ ] 后端测试全部通过 (`mvnw.cmd test`)

### 前端验证

- [ ] 订单详情页显示"联系卖家/买家"按钮
- [ ] 点击按钮自动发送订单卡片
- [ ] 消息气泡正确显示订单卡片（订单号、状态、商品、金额）
- [ ] 点击"查看订单详情"跳转到订单详情页
- [ ] 订单状态颜色正确显示
- [ ] 前端测试全部通过 (`npm test`)

### 集成测试

- [ ] 买家在订单详情页联系卖家,发送订单卡片
- [ ] 卖家收到订单卡片消息,正确显示
- [ ] 卖家点击"查看订单详情",跳转到订单详情页
- [ ] 订单状态变更后,历史消息中的订单卡片显示最新状态

---

## Technical Constraints

### 必须遵守的约束

1. **依赖 task-02**: 必须先完成图片消息支持（`message_type` 字段）
2. **订单权限验证**: 发送时验证用户是订单参与者（买家或卖家）
3. **订单信息附加**: 查询消息时自动 JOIN `orders` 和 `order_items` 表
4. **订单状态实时性**: 订单卡片显示最新状态（不缓存）
5. **性能考虑**: 订单信息仅返回必要字段和第一个商品信息
6. **纯 JDBC**: 使用 `JdbcTemplate`,不使用 ORM
7. **事务管理**: 使用 `@Transactional` 保证数据一致性

---

## Files to Modify

### 后端

- `backend/src/main/resources/schema.sql` - 添加字段和索引
- `backend/src/main/java/com/youyu/backend/controller/chat/SendMessageRequest.java` - 添加 orderId 字段
- `backend/src/main/java/com/youyu/backend/service/chat/impl/ChatServiceImpl.java` - 修改业务逻辑
- `backend/src/main/java/com/youyu/backend/mapper/chat/ChatMessageMapper.java` - 修改方法签名
- `backend/src/main/java/com/youyu/backend/mapper/chat/impl/JdbcChatMessageMapper.java` - 修改实现
- `backend/src/main/java/com/youyu/backend/controller/chat/ChatController.java` - 修改接口

### 前端

- `frontend/src/api/modules/chat.js` - 修改 API 方法
- `frontend/src/stores/chat.js` - 修改状态和方法
- `frontend/src/components/chat/OrderCardMessage.vue` - 创建订单卡片组件
- `frontend/src/views/app/MessagesView.vue` - 添加订单卡片显示
- `frontend/src/views/app/TradeOrderDetailView.vue` - 添加联系按钮

### 文档

- `docs/06-http/chat.http` - 添加 HTTP 测试用例
- `docs/09-api-spec/chat.md` - 更新 API 文档
- `CHANGELOG.md` - 添加变更记录

---

## Notes

- 本任务依赖 task-02,需等 task-02 完成后才能开始
- 订单卡片是电商售后场景的核心功能,能够显著提升沟通效率
- 订单状态实时显示,不缓存订单信息

---

## Completion Checklist

完成后,向头 Agent 报告:

- [ ] 所有验收标准已满足
- [ ] 所有测试通过
- [ ] 文档已更新
- [ ] CHANGELOG.md 已更新
- [ ] 代码已提交到分支
- [ ] 遇到的问题和解决方案(如有)
