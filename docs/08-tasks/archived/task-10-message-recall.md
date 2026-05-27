# Task 10: 消息撤回

## Metadata

- **Task ID**: task-10
- **Priority**: P2
- **Estimated effort**: 4-6 hours
- **Dependencies**: None (独立任务)
- **Assignee**: 待分配

---

## Context

### 问题描述

用户发送错误消息后需要撤回。电商场景中,卖家可能发错价格、买家可能发错商品链接。

### 业务价值

消息撤回能够减少错误消息带来的困扰,提升用户体验。

---

## Goals

实现以下功能:

1. **2 分钟内可以撤回**: 发送后 2 分钟内可以撤回自己的消息
2. **撤回后显示占位符**: 撤回后消息显示"已撤回"占位符
3. **对方看到撤回提示**: 对方看到"对方撤回了一条消息"

---

## Database Schema

### `chat_messages` 表添加撤回字段

需要添加以下字段:
- `is_recalled` (BOOLEAN, NOT NULL, DEFAULT FALSE): 是否已撤回
- `recalled_at` (TIMESTAMP, NULL): 撤回时间

需要添加索引:
- `idx_conversation_recalled`: 组合索引 `(conversation_id, is_recalled)`

**设计说明**:
- `is_recalled` 标记消息是否已撤回
- `recalled_at` 记录撤回时间
- 撤回后消息内容保留（用于审计和纠纷处理）

---

## API Contract

### 撤回消息

**Endpoint**: `POST /api/chat/messages/{id}/recall`

**Request**:
- Path parameter: `id` (Long) - 消息 ID
- Headers: `Authorization: Bearer <token>`

**Response**:
```json
{
  "success": true,
  "data": null
}
```

**Business Logic**:
- 验证是否为发送者
- 验证是否已撤回
- 验证时间限制（2 分钟内）
- 标记消息为已撤回

**Error Cases**:
- 非发送者撤回 → 403 Forbidden
- 消息已撤回 → 400 Bad Request
- 超过 2 分钟 → 400 Bad Request "只能撤回 2 分钟内的消息"

---

## Frontend Requirements

### 1. 消息气泡显示撤回按钮

- 自己的消息显示"撤回"按钮（2 分钟内）
- 超过 2 分钟的消息不显示"撤回"按钮
- 撤回按钮鼠标悬停时显示

### 2. 撤回确认

点击"撤回"按钮需要二次确认:
- 弹出确认对话框
- 提示"确认撤回这条消息?"

### 3. 撤回后显示

- 自己撤回的消息显示"你撤回了一条消息"
- 对方撤回的消息显示"对方撤回了一条消息"
- 撤回消息显示为灰色占位符,不显示原内容

---

## Acceptance Criteria

### 后端验证

- [ ] 数据库迁移脚本执行成功,字段和索引创建成功
- [ ] 撤回自己的消息成功
- [ ] 撤回他人的消息返回错误
- [ ] 撤回已撤回的消息返回错误
- [ ] 撤回 2 分钟前的消息返回错误
- [ ] 撤回后消息 `is_recalled` 字段为 TRUE
- [ ] 查询消息时返回撤回状态
- [ ] 后端测试全部通过 (`mvnw.cmd test`)

### 前端验证

- [ ] 自己的消息显示"撤回"按钮（2 分钟内）
- [ ] 超过 2 分钟的消息不显示"撤回"按钮
- [ ] 点击"撤回"按钮需要二次确认
- [ ] 撤回成功后消息显示"你撤回了一条消息"
- [ ] 对方撤回的消息显示"对方撤回了一条消息"
- [ ] 已撤回消息不显示原内容
- [ ] 前端测试全部通过 (`npm test`)

### 集成测试

- [ ] 用户 A 发送消息,1 分钟内撤回成功
- [ ] 用户 B 看到"对方撤回了一条消息"
- [ ] 用户 A 尝试撤回 3 分钟前的消息,返回错误
- [ ] 用户 B 尝试撤回用户 A 的消息,返回错误

---

## Technical Constraints

### 必须遵守的约束

1. **时间限制**: 只能撤回 2 分钟内的消息
2. **权限控制**: 只能撤回自己发送的消息
3. **软删除**: 撤回后消息内容保留（用于审计）
4. **显示逻辑**: 撤回后显示占位符,不显示原内容
5. **独立功能**: 不依赖其他任务,可独立开发
6. **纯 JDBC**: 使用 `JdbcTemplate`,不使用 ORM

---

## Files to Modify

### 后端

- `backend/src/main/resources/schema.sql` - 添加字段和索引
- `backend/src/main/java/com/youyu/backend/mapper/chat/ChatMessageMapper.java` - 添加方法
- `backend/src/main/java/com/youyu/backend/mapper/chat/impl/JdbcChatMessageMapper.java` - 实现方法
- `backend/src/main/java/com/youyu/backend/service/chat/impl/ChatServiceImpl.java` - 添加业务逻辑
- `backend/src/main/java/com/youyu/backend/controller/chat/ChatController.java` - 添加接口

### 前端

- `frontend/src/api/modules/chat.js` - 添加 API 方法
- `frontend/src/stores/chat.js` - 添加方法
- `frontend/src/views/app/MessagesView.vue` - 添加撤回按钮和显示逻辑

### 文档

- `docs/06-http/chat.http` - 添加 HTTP 测试用例
- `docs/09-api-spec/chat.md` - 更新 API 文档
- `CHANGELOG.md` - 添加变更记录

---

## Notes

- 本任务与其他 P2 任务完全独立,可并行开发
- 撤回时间限制为 2 分钟,参考微信等主流应用

---

## Completion Checklist

完成后,向头 Agent 报告:

- [x] 所有验收标准已满足
- [x] 所有测试通过
- [x] 文档已更新
- [x] CHANGELOG.md 已更新
- [x] 代码已提交到分支
- [x] 遇到的问题和解决方案(如有)

## Completion Notes

- Status: completed
- Completed at: 2026-05-27
- Implemented sender-only recall within a 2-minute window and returned `isRecalled` / `recalledAt` metadata.
- Added frontend recall action and recalled-message placeholder rendering.
- Verification: `backend\mvnw.cmd test`, `frontend\npm test`, `frontend\npm run build`.
