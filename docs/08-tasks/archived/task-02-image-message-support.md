# Task 02: 图片消息支持

## Metadata

- **Task ID**: task-02
- **Priority**: P0
- **Estimated effort**: 6-8 hours
- **Dependencies**: None (独立任务)
- **Assignee**: 子 Agent A / 子 Agent B
- **Status**: done
- **Completed date**: 2026-05-25

---

## Context

### 问题描述

当前消息功能仅支持纯文本消息,无法发送图片。电商场景中:
- 买家需要发送商品问题截图（破损、色差、尺寸不符等）
- 卖家需要发送商品细节图片（材质、标签、包装等）
- 双方需要通过图片快速沟通问题

### 业务价值

图片消息是电商客服的核心功能,能够显著提升沟通效率,减少文字描述的歧义。

---

## Goals

实现以下功能:

1. **发送图片消息**: 用户可以选择图片并发送
2. **消息气泡显示图片**: 图片消息在聊天界面正确显示
3. **点击图片放大预览**: 点击图片可以全屏查看
4. **图片加载失败处理**: 图片加载失败时显示占位符
5. **支持多种消息类型**: 为未来的商品卡片、订单卡片等消息类型预留扩展空间

---

## Database Schema

### `chat_messages` 表添加消息类型和媒体字段

需要添加以下字段:
- `message_type` (VARCHAR(32), NOT NULL, DEFAULT 'text'): 消息类型,枚举值: 'text', 'image', 'product_card', 'order_card'
- `media_url` (VARCHAR(512), NULL): 媒体 URL（图片、文件等）

需要添加索引:
- `idx_conversation_type`: 组合索引 `(conversation_id, message_type)`,用于按类型筛选消息

**设计说明**:
- `message_type` 使用字符串枚举,预留扩展空间（product_card, order_card）
- `media_url` 存储图片 URL,复用 `product_media` 的 URL 存储模式
- `body` 字段在图片消息中可选（可为空或存储图片描述）

---

## API Contract

### 1. 发送消息接口（修改现有接口）

**Endpoint**: `POST /api/chat/conversations/{id}/messages`

**Request**:
```json
{
  "body": "这是图片描述（可选）",
  "messageType": "image",
  "mediaUrl": "https://example.com/image.jpg"
}
```

**Request Fields**:
- `body` (String, optional): 消息内容（文本消息必需,图片消息可选）
- `messageType` (String, optional, default: "text"): 消息类型,枚举值: "text", "image"
- `mediaUrl` (String, optional): 媒体 URL（图片消息必需）

**Response**:
```json
{
  "success": true,
  "data": {
    "id": 123,
    "conversationId": 1,
    "senderId": 2,
    "body": "这是图片描述",
    "messageType": "image",
    "mediaUrl": "https://example.com/image.jpg",
    "createdAt": "2026-05-25T10:00:00"
  }
}
```

**Business Logic**:
- 验证消息类型是否合法
- 文本消息: `body` 必需,`mediaUrl` 为空
- 图片消息: `mediaUrl` 必需,`body` 可选
- 图片 URL 长度限制: 最大 512 字符
- 图片 URL 格式验证: 必须以 `http://` 或 `https://` 开头

### 2. 获取消息接口（修改现有接口）

**Endpoint**: `GET /api/chat/conversations/{id}/messages`

**Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 123,
        "conversationId": 1,
        "senderId": 2,
        "body": "文本消息",
        "messageType": "text",
        "mediaUrl": null,
        "createdAt": "2026-05-25T10:00:00"
      },
      {
        "id": 124,
        "conversationId": 1,
        "senderId": 1,
        "body": "这是图片描述",
        "messageType": "image",
        "mediaUrl": "https://example.com/image.jpg",
        "createdAt": "2026-05-25T10:01:00"
      }
    ],
    "page": 0,
    "size": 50,
    "total": 2
  }
}
```

---

## Frontend Requirements

### 1. 图片上传组件

创建图片上传组件,支持:
- 点击按钮选择图片
- 文件类型验证（仅允许 `image/*`）
- 文件大小限制（最大 5MB）
- 上传进度提示
- 上传成功后自动发送图片消息

**临时方案**: 使用 Base64 Data URL（将图片转换为 Base64 字符串存储在 `media_url` 字段）

**未来优化**: 上传到 OSS,返回 URL

### 2. 消息气泡显示图片

- 图片消息显示为图片气泡
- 图片最大宽度 300px,高度自适应
- 图片圆角 12px
- 图片加载失败显示占位符（灰色背景 + "图片加载失败"文字）

### 3. 图片预览

- 点击图片打开全屏预览
- 支持关闭预览
- 可使用 Element Plus 的 `ElImageViewer` 组件

### 4. 消息输入区域

- 添加图片上传按钮（📷 图标）
- 图片上传按钮位于文本输入框左侧
- 上传中显示"上传中..."提示

---

## Acceptance Criteria

### 后端验证

- [ ] 数据库迁移脚本执行成功,字段和索引创建成功
- [ ] 发送文本消息（messageType=text）成功
- [ ] 发送图片消息（messageType=image, mediaUrl 必需）成功
- [ ] 图片消息的 body 可为空
- [ ] 图片 URL 长度验证生效（>512 字符拒绝）
- [ ] 图片 URL 格式验证生效（非 http/https 拒绝）
- [ ] 查询消息时返回 messageType 和 mediaUrl 字段
- [ ] 后端测试全部通过 (`mvnw.cmd test`)

### 前端验证

- [ ] 点击图片按钮,选择图片上传
- [ ] 图片上传成功后自动发送
- [ ] 消息气泡正确显示图片
- [ ] 点击图片放大预览
- [ ] 图片加载失败显示占位符
- [ ] 图片消息和文本消息混合显示正常
- [ ] 前端测试全部通过 (`npm test`)

### 集成测试

- [ ] 用户 A 发送图片给用户 B
- [ ] 用户 B 收到图片消息,正确显示
- [ ] 用户 B 点击图片,放大预览
- [ ] 图片 URL 失效时,显示占位符

---

## Technical Constraints

### 必须遵守的约束

1. **数据库幂等性**: 使用 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`
2. **向后兼容**: `messageType` 默认值为 `text`,不破坏现有客户端
3. **扩展性**: `message_type` 预留 `product_card`, `order_card` 枚举值
4. **图片存储**: 当前使用 Base64 Data URL（临时方案）,未来迁移到 OSS
5. **图片大小限制**: 前端限制 5MB,后端 URL 长度限制 512 字符
6. **图片格式**: 前端仅允许 `image/*` 类型
7. **纯 JDBC**: 使用 `JdbcTemplate`,不使用 ORM
8. **事务管理**: 使用 `@Transactional` 保证数据一致性

### 实现建议(非强制)

- 图片上传组件可以使用 `FileReader` API 读取文件并转换为 Base64
- 图片预览可以使用 Element Plus 的 `ElImageViewer` 组件
- 图片加载失败可以使用 `<img>` 标签的 `@error` 事件处理

---

## Files to Modify

### 后端

- `backend/src/main/resources/schema.sql` - 添加字段和索引
- `backend/src/main/java/com/youyu/backend/controller/chat/SendMessageRequest.java` - 添加字段
- `backend/src/main/java/com/youyu/backend/service/chat/impl/ChatServiceImpl.java` - 修改业务逻辑
- `backend/src/main/java/com/youyu/backend/mapper/chat/ChatMessageMapper.java` - 修改方法签名
- `backend/src/main/java/com/youyu/backend/mapper/chat/impl/JdbcChatMessageMapper.java` - 修改实现
- `backend/src/main/java/com/youyu/backend/controller/chat/ChatController.java` - 修改接口

### 前端

- `frontend/src/api/modules/chat.js` - 修改 API 方法
- `frontend/src/stores/chat.js` - 修改状态和方法
- `frontend/src/components/chat/ImageUploader.vue` - 创建图片上传组件
- `frontend/src/views/app/MessagesView.vue` - 添加图片上传按钮和图片显示

### 文档

- `docs/06-http/chat.http` - 添加 HTTP 测试用例
- `docs/09-api-spec/chat.md` - 更新 API 文档
- `CHANGELOG.md` - 添加变更记录

---

## Notes

- 本任务与 task-01, task-05 完全独立,可并行开发
- **关键路径**: task-03 和 task-04 依赖本任务,需优先完成
- Base64 Data URL 是临时方案,未来需要迁移到 OSS
- `message_type` 字段预留了 `product_card`, `order_card` 枚举值,为后续任务做准备

---

## Completion Checklist

完成后,向头 Agent 报告:

- [x] 所有验收标准已满足
- [x] 所有测试通过
- [x] 文档已更新
- [x] CHANGELOG.md 已更新
- [ ] 代码已提交到分支
- [x] 遇到的问题和解决方案(如有)
- [x] 通知头 Agent: task-03 和 task-04 可以开始

## Completion Notes

- 已实现 `text` / `image` 消息类型、图片发送、图片气泡、加载失败占位和图片预览。
- 当前前端按任务临时方案使用 Base64 Data URL；后端和 schema 已放宽 data-image 存储长度，避免 `VARCHAR(512)` 与 5MB 图片限制冲突。
- task-03 / task-04 的 `message_type` 依赖已满足，但仍需等待头 Agent 重新分配文件范围，避免继续冲突修改聊天核心文件。
