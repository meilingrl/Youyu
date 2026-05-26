# Task 09: 会话管理（置顶、删除、静音）

## Metadata

- **Task ID**: task-09
- **Priority**: P2
- **Estimated effort**: 6-8 hours
- **Dependencies**: None (独立任务)
- **Assignee**: 待分配

---

## Context

### 问题描述

用户需要管理大量会话,重要会话容易被淹没。会话管理功能可以置顶重要会话、删除无用会话、静音骚扰会话。

### 业务价值

会话管理能够帮助用户更好地组织会话列表,提升使用效率。

---

## Goals

实现以下功能:

1. **置顶会话**: 重要会话显示在列表顶部
2. **删除会话**: 软删除会话（保留历史消息）
3. **静音会话**: 不显示未读红点,不推送通知

---

## Database Schema

### `chat_conversations` 表添加管理字段

需要添加以下字段:
- `is_pinned_a` (BOOLEAN, NOT NULL, DEFAULT FALSE): user_a 是否置顶
- `is_pinned_b` (BOOLEAN, NOT NULL, DEFAULT FALSE): user_b 是否置顶
- `is_muted_a` (BOOLEAN, NOT NULL, DEFAULT FALSE): user_a 是否静音
- `is_muted_b` (BOOLEAN, NOT NULL, DEFAULT FALSE): user_b 是否静音
- `deleted_by_a_at` (TIMESTAMP, NULL): user_a 删除时间
- `deleted_by_b_at` (TIMESTAMP, NULL): user_b 删除时间

需要添加索引:
- `idx_user_a_pinned`: 组合索引 `(user_a_id, is_pinned_a)`
- `idx_user_b_pinned`: 组合索引 `(user_b_id, is_pinned_b)`

**设计说明**:
- 使用双向字段（`_a` 和 `_b`）,每个用户独立管理自己的会话
- 软删除：记录删除时间,不物理删除会话和消息
- 静音会话：不影响消息接收,只是不显示未读红点和通知

---

## API Contract

### 1. 置顶/取消置顶会话

**Endpoint**: `POST /api/chat/conversations/{id}/pin`

**Request**:
```json
{
  "isPinned": true
}
```

**Response**:
```json
{
  "success": true,
  "data": null
}
```

### 2. 静音/取消静音会话

**Endpoint**: `POST /api/chat/conversations/{id}/mute`

**Request**:
```json
{
  "isMuted": true
}
```

**Response**:
```json
{
  "success": true,
  "data": null
}
```

### 3. 删除会话

**Endpoint**: `DELETE /api/chat/conversations/{id}`

**Response**:
```json
{
  "success": true,
  "data": null
}
```

**Business Logic**:
- 软删除：记录删除时间,不物理删除
- 删除后会话不再出现在列表中
- 历史消息仍可查询

### 4. 修改会话列表接口

**GET /api/chat/conversations** 需要:
- 排除已删除的会话
- 置顶会话排在前面
- 返回每个会话的管理状态（isPinned, isMuted）

---

## Frontend Requirements

### 1. 会话操作菜单

创建会话操作菜单组件,显示:
- "置顶会话" / "取消置顶"
- "静音会话" / "取消静音"
- "删除会话"

### 2. 会话列表显示

- 置顶会话显示在列表顶部,带📌图标
- 置顶会话背景色区分（浅橙色）
- 静音会话不显示未读红点
- 静音会话透明度降低（opacity: 0.6）

### 3. 删除确认

删除会话需要二次确认:
- 弹出确认对话框
- 提示"删除后将不再显示此会话,但历史消息会保留"

---

## Acceptance Criteria

### 后端验证

- [ ] 数据库迁移脚本执行成功,字段和索引创建成功
- [ ] 置顶会话成功,会话列表置顶会话在前
- [ ] 取消置顶成功
- [ ] 静音会话成功
- [ ] 取消静音成功
- [ ] 删除会话成功,会话不再出现在列表中
- [ ] 删除会话后,历史消息仍可查询（软删除）
- [ ] 双向独立：用户 A 删除会话,用户 B 仍可见
- [ ] 后端测试全部通过 (`mvnw.cmd test`)

### 前端验证

- [ ] 会话列表显示操作菜单按钮
- [ ] 点击菜单按钮显示操作选项
- [ ] 置顶会话显示在列表顶部,带📌图标
- [ ] 静音会话不显示未读红点
- [ ] 删除会话后从列表移除
- [ ] 删除会话需要二次确认
- [ ] 前端测试全部通过 (`npm test`)

### 集成测试

- [ ] 用户 A 置顶会话,会话显示在列表顶部
- [ ] 用户 A 静音会话,收到新消息时不显示未读红点
- [ ] 用户 A 删除会话,会话从列表消失
- [ ] 用户 B 仍可见会话,不受用户 A 删除影响

---

## Technical Constraints

### 必须遵守的约束

1. **双向独立**: 每个用户独立管理自己的会话状态
2. **软删除**: 记录删除时间,不物理删除数据
3. **排序规则**: 置顶会话在前,然后按最后消息时间倒序
4. **静音逻辑**: 静音会话不显示未读红点,但消息仍正常接收
5. **独立功能**: 不依赖其他任务,可独立开发
6. **纯 JDBC**: 使用 `JdbcTemplate`,不使用 ORM

---

## Files to Modify

### 后端

- `backend/src/main/resources/schema.sql` - 添加字段和索引
- `backend/src/main/java/com/youyu/backend/mapper/chat/ChatConversationMapper.java` - 添加方法
- `backend/src/main/java/com/youyu/backend/mapper/chat/impl/JdbcChatConversationMapper.java` - 实现方法
- `backend/src/main/java/com/youyu/backend/service/chat/impl/ChatServiceImpl.java` - 添加业务逻辑
- `backend/src/main/java/com/youyu/backend/controller/chat/ChatController.java` - 添加接口

### 前端

- `frontend/src/api/modules/chat.js` - 添加 API 方法
- `frontend/src/stores/chat.js` - 添加状态和方法
- `frontend/src/components/chat/ConversationMenu.vue` - 创建操作菜单组件
- `frontend/src/views/app/MessagesView.vue` - 添加操作菜单

### 文档

- `docs/06-http/chat.http` - 添加 HTTP 测试用例
- `docs/09-api-spec/chat.md` - 更新 API 文档
- `CHANGELOG.md` - 添加变更记录

---

## Notes

- 本任务与其他 P2 任务完全独立,可并行开发
- 双向设计确保每个用户独立管理自己的会话状态

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
- Implemented per-user pin, mute, and soft-delete fields and endpoints.
- Added conversation menu controls, muted/pinned state rendering, stable conversation switching, and list previews for all conversations.
- Verification: `backend\mvnw.cmd test`, `frontend\npm test`, `frontend\npm run build`.
