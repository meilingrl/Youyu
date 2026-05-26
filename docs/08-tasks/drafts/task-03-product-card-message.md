# Task 03: 商品卡片消息

## Metadata

- **Task ID**: task-03
- **Priority**: P1
- **Estimated effort**: 6-8 hours
- **Dependencies**: task-02 (图片消息支持)
- **Assignee**: 待分配

---

## Context

### 问题描述

电商场景中,用户需要快速分享商品链接。当前只能复制粘贴商品 URL,体验差。商品卡片消息可以在聊天中直接展示商品信息（图片、标题、价格）,点击跳转到商品详情页。

### 业务价值

商品卡片消息能够显著提升商品分享的转化率,买家可以快速了解商品信息,卖家可以快速推荐商品。

---

## Goals

实现以下功能:

1. **商品详情页添加"分享到聊天"按钮**: 用户可以选择会话分享商品
2. **消息气泡显示商品卡片**: 显示商品图片、标题、价格、状态
3. **点击卡片跳转到商品详情页**: 点击"查看详情"按钮跳转
4. **商品下架处理**: 商品下架后,卡片显示"已下架"状态,按钮禁用

---

## Database Schema

### `chat_messages` 表添加商品关联字段

需要添加以下字段:
- `product_id` (BIGINT, NULL): 关联商品 ID（商品卡片消息）
- 外键约束: `FOREIGN KEY (product_id) REFERENCES products(id)`

需要添加索引:
- `idx_message_product`: 索引 `(product_id)`,用于查询商品相关的消息

**设计说明**:
- `message_type = 'product_card'` 时,`product_id` 必需
- `body` 字段可选（用于附加文字说明）
- `media_url` 字段不使用（商品图片从 products 表获取）

---

## API Contract

### 1. 发送消息接口（修改现有接口）

**Endpoint**: `POST /api/chat/conversations/{id}/messages`

**Request**:
```json
{
  "body": "这个商品不错（可选）",
  "messageType": "product_card",
  "productId": 123
}
```

**Request Fields**:
- `body` (String, optional): 附加文字说明
- `messageType` (String): 必须为 "product_card"
- `productId` (Long): 商品 ID（必需）

**Business Logic**:
- 验证商品是否存在
- 验证商品状态是否为 `active`（已下架商品不能分享）
- 插入消息时关联商品 ID

### 2. 获取消息接口（修改现有接口）

**Endpoint**: `GET /api/chat/conversations/{id}/messages`

**Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 125,
        "messageType": "product_card",
        "body": "这个商品不错",
        "productId": 123,
        "product": {
          "id": 123,
          "title": "商品标题",
          "price": 99.00,
          "status": "active",
          "imageUrl": "https://example.com/product.jpg"
        },
        "createdAt": "2026-05-25T10:02:00"
      }
    ]
  }
}
```

**Business Logic**:
- 查询消息时,如果 `message_type = 'product_card'`,自动 JOIN `products` 表
- 返回商品的必要信息: id, title, price, status, imageUrl（首图）
- 如果商品已删除,`product` 字段为 null

---

## Frontend Requirements

### 1. 商品详情页添加"分享到聊天"按钮

在商品详情页添加按钮:
- 点击按钮弹出会话选择对话框
- 显示用户的所有会话列表
- 选择会话后发送商品卡片消息
- 发送成功后跳转到消息中心

### 2. 商品卡片组件

创建商品卡片组件,显示:
- 商品图片（80x80px）
- 商品标题（最多 2 行,超出省略）
- 商品价格（红色,加粗）
- 商品状态（已下架时显示灰色文字）
- "查看详情"按钮（商品下架时禁用）
- 附加文字说明（如果有）

### 3. 消息气泡显示商品卡片

在消息气泡中显示商品卡片:
- 商品卡片最大宽度 320px
- 点击"查看详情"按钮跳转到商品详情页
- 商品下架时,卡片显示"已下架"状态,按钮禁用

---

## Acceptance Criteria

### 后端验证

- [ ] 数据库迁移脚本执行成功,字段和索引创建成功
- [ ] 发送商品卡片消息（messageType=product_card, productId 必需）成功
- [ ] 商品不存在时返回错误
- [ ] 商品已下架时返回错误
- [ ] 查询消息时自动附加商品信息（id, title, price, imageUrl, status）
- [ ] 商品信息包含首图 URL
- [ ] 后端测试全部通过 (`mvnw.cmd test`)

### 前端验证

- [ ] 商品详情页显示"分享到聊天"按钮
- [ ] 点击按钮弹出会话选择对话框
- [ ] 选择会话后成功发送商品卡片
- [ ] 消息气泡正确显示商品卡片（图片、标题、价格）
- [ ] 点击"查看详情"跳转到商品详情页
- [ ] 商品已下架时显示"已下架"状态,按钮禁用
- [ ] 前端测试全部通过 (`npm test`)

### 集成测试

- [ ] 用户 A 在商品详情页分享商品给用户 B
- [ ] 用户 B 收到商品卡片消息,正确显示
- [ ] 用户 B 点击"查看详情",跳转到商品详情页
- [ ] 商品下架后,卡片显示"已下架"状态

---

## Technical Constraints

### 必须遵守的约束

1. **依赖 task-02**: 必须先完成图片消息支持（`message_type` 字段）
2. **商品验证**: 发送时验证商品存在且状态为 `active`
3. **商品信息附加**: 查询消息时自动 JOIN `products` 表
4. **向后兼容**: 商品下架后,历史消息仍可查看,但显示"已下架"状态
5. **性能考虑**: 商品信息仅返回必要字段（id, title, price, imageUrl, status）
6. **纯 JDBC**: 使用 `JdbcTemplate`,不使用 ORM
7. **事务管理**: 使用 `@Transactional` 保证数据一致性

---

## Files to Modify

### 后端

- `backend/src/main/resources/schema.sql` - 添加字段和索引
- `backend/src/main/java/com/youyu/backend/controller/chat/SendMessageRequest.java` - 添加 productId 字段
- `backend/src/main/java/com/youyu/backend/service/chat/impl/ChatServiceImpl.java` - 修改业务逻辑
- `backend/src/main/java/com/youyu/backend/mapper/chat/ChatMessageMapper.java` - 修改方法签名
- `backend/src/main/java/com/youyu/backend/mapper/chat/impl/JdbcChatMessageMapper.java` - 修改实现
- `backend/src/main/java/com/youyu/backend/controller/chat/ChatController.java` - 修改接口

### 前端

- `frontend/src/api/modules/chat.js` - 修改 API 方法
- `frontend/src/stores/chat.js` - 修改状态和方法
- `frontend/src/components/chat/ProductCardMessage.vue` - 创建商品卡片组件
- `frontend/src/views/app/MessagesView.vue` - 添加商品卡片显示
- `frontend/src/views/app/ProductDetailView.vue` - 添加分享按钮

### 文档

- `docs/06-http/chat.http` - 添加 HTTP 测试用例
- `docs/09-api-spec/chat.md` - 更新 API 文档
- `CHANGELOG.md` - 添加变更记录

---

## Notes

- 本任务依赖 task-02,需等 task-02 完成后才能开始
- 商品卡片是电商场景的特色功能,能够显著提升商品分享的转化率
- 商品下架后,历史消息仍可查看,但显示"已下架"状态

---

## Completion Checklist

完成后,向头 Agent 报告:

- [ ] 所有验收标准已满足
- [ ] 所有测试通过
- [ ] 文档已更新
- [ ] CHANGELOG.md 已更新
- [ ] 代码已提交到分支
- [ ] 遇到的问题和解决方案(如有)
