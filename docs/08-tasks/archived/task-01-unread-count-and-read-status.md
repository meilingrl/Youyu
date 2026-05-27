# Task 01: 未读消息计数与已读状态

## Metadata

- **Task ID**: task-01
- **Priority**: P0
- **Estimated effort**: 6-8 hours
- **Dependencies**: None (独立任务)
- **Assignee**: 子 Agent A / 子 Agent B
- **Status**: done
- **Completed date**: 2026-05-25

---

## Context

### 问题描述

当前消息功能缺少未读消息计数和已读状态追踪,导致:
- 用户无法知道是否有新消息
- 会话列表的未读计数硬编码为 0
- 导航栏无法显示全局未读红点

### 业务价值

这是消息系统的最基础功能,用户需要通过未读红点快速判断是否有新消息需要处理。电商场景中,买家需要及时看到卖家回复,卖家需要及时看到买家咨询。

---

## Goals

实现以下功能:

1. **会话列表显示未读计数**: 每个会话显示该会话的未读消息数
2. **导航栏显示全局未读红点**: 显示所有会话的未读消息总数
3. **打开会话自动标记已读**: 用户打开会话后,该会话的未读消息自动标记为已读
4. **未读计数实时更新**: 收到新消息时,未读计数自动增加

---

## Database Schema

### 1. `chat_messages` 表添加已读字段

需要添加以下字段:
- `is_read` (BOOLEAN, NOT NULL, DEFAULT FALSE): 消息是否已读
- `read_at` (TIMESTAMP, NULL): 消息已读时间

需要添加索引:
- `idx_conversation_unread`: 组合索引 `(conversation_id, is_read)`,用于快速查询未读消息

### 2. `chat_conversations` 表添加未读计数字段

需要添加以下字段:
- `unread_count_a` (INT, NOT NULL, DEFAULT 0): user_a 的未读消息数
- `unread_count_b` (INT, NOT NULL, DEFAULT 0): user_b 的未读消息数

需要添加索引:
- `idx_user_a_unread`: 组合索引 `(user_a_id, unread_count_a)`
- `idx_user_b_unread`: 组合索引 `(user_b_id, unread_count_b)`

**设计说明**:
- 使用冗余字段 `unread_count_a/b` 避免实时统计,提升性能
- 双向设计:每个用户独立维护自己的未读计数
- `is_read` 字段用于消息级别的已读状态
- `read_at` 字段记录已读时间,可用于未来的已读回执功能

---

## API Contract

### 1. 标记会话已读

**Endpoint**: `POST /api/chat/conversations/{id}/read`

**Request**:
- Path parameter: `id` (Long) - 会话 ID
- Headers: `Authorization: Bearer <token>`

**Response**:
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "OK",
  "data": null,
  "traceId": "uuid"
}
```

**Business Logic**:
- 验证用户是会话参与者
- 将该会话中所有对方发送的未读消息标记为已读
- 重置当前用户的未读计数为 0

### 2. 获取全局未读计数

**Endpoint**: `GET /api/chat/unread-count`

**Request**:
- Headers: `Authorization: Bearer <token>`

**Response**:
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "count": 5
  },
  "traceId": "uuid"
}
```

**Business Logic**:
- 统计当前用户所有会话的未读消息总数
- 返回 `unread_count_a` 或 `unread_count_b` 的总和(取决于用户是 user_a 还是 user_b)

### 3. 修改现有接口

**GET /api/chat/conversations** 需要返回每个会话的未读计数:

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "userAId": 1,
        "userBId": 2,
        "lastMessageAt": "2026-05-25T10:00:00",
        "unreadCount": 3,  // 新增字段
        "peerUser": { ... }
      }
    ],
    "page": 0,
    "size": 20,
    "total": 10
  }
}
```

---

## Frontend Requirements

### 1. 会话列表显示未读红点

- 每个会话项显示未读计数(如果 > 0)
- 未读计数显示为红色圆形徽章
- 未读计数超过 99 显示为 "99+"

### 2. 导航栏显示全局未读红点

- 在"消息"导航链接上显示全局未读计数
- 未读计数 > 0 时显示红色徽章
- 未读计数超过 99 显示为 "99+"

### 3. 打开会话自动标记已读

- 用户打开会话(调用 `fetchMessages`)时,自动调用标记已读接口
- 标记已读成功后,本地更新会话的未读计数为 0
- 刷新全局未读计数

### 4. 轮询刷新未读计数

- 每次轮询刷新会话列表时,自动更新未读计数
- 全局未读计数每分钟刷新一次(或与会话列表同步刷新)

---

## Acceptance Criteria

### 后端验证

- [ ] 数据库迁移脚本执行成功,字段和索引创建成功
- [ ] 发送消息后,对方的 `unread_count_a/b` 自动增加 1
- [ ] 调用 `POST /api/chat/conversations/{id}/read` 后,未读计数重置为 0
- [ ] 调用 `GET /api/chat/unread-count` 返回正确的总未读数
- [ ] 多个会话的未读计数独立计算,互不影响
- [ ] 后端测试全部通过 (`mvnw.cmd test`)

### 前端验证

- [ ] 会话列表正确显示每个会话的未读红点(数字)
- [ ] 打开会话后,未读红点立即消失
- [ ] 导航栏显示全局未读计数红点
- [ ] 发送消息后,对方会话列表实时更新未读红点(通过轮询)
- [ ] 轮询刷新时,未读计数正确更新
- [ ] 前端测试全部通过 (`npm test`)

### 集成测试

- [ ] 用户 A 发送消息给用户 B
- [ ] 用户 B 的会话列表显示未读红点(数字 1)
- [ ] 用户 B 的导航栏显示全局未读计数(数字 1)
- [ ] 用户 B 打开会话,未读红点消失
- [ ] 用户 B 的导航栏全局未读计数减少 1

---

## Technical Constraints

### 必须遵守的约束

1. **数据库幂等性**: 使用 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`
2. **并发安全**: 使用 `CASE WHEN` 条件更新,避免竞态条件
3. **性能优化**: 使用冗余字段 `unread_count_a/b`,避免实时统计
4. **向后兼容**: 新增字段使用默认值,不破坏现有客户端
5. **权限验证**: 所有接口验证用户是否为会话参与者
6. **纯 JDBC**: 使用 `JdbcTemplate`,不使用 ORM
7. **事务管理**: 使用 `@Transactional` 保证数据一致性

### 实现建议(非强制)

- Mapper 层返回 `Map<String, Object>` 或 `List<Map<String, Object>>`
- Service 层处理业务逻辑,调用 Mapper
- Controller 层验证权限,调用 Service,返回 `ApiResponse<T>`
- 前端 Store 使用 Pinia Composition API
- 前端数据规范化:在 Store 边界转换 snake_case → camelCase

---

## Files to Modify

### 后端

- `backend/src/main/resources/schema.sql` - 添加字段和索引
- `backend/src/main/java/com/youyu/backend/mapper/chat/ChatConversationMapper.java` - 添加方法签名
- `backend/src/main/java/com/youyu/backend/mapper/chat/impl/JdbcChatConversationMapper.java` - 实现方法
- `backend/src/main/java/com/youyu/backend/mapper/chat/ChatMessageMapper.java` - 添加方法签名
- `backend/src/main/java/com/youyu/backend/mapper/chat/impl/JdbcChatMessageMapper.java` - 实现方法
- `backend/src/main/java/com/youyu/backend/service/chat/impl/ChatServiceImpl.java` - 添加业务逻辑
- `backend/src/main/java/com/youyu/backend/controller/chat/ChatController.java` - 添加接口

### 前端

- `frontend/src/api/modules/chat.js` - 添加 API 方法
- `frontend/src/stores/chat.js` - 添加状态和方法
- `frontend/src/views/app/MessagesView.vue` - 显示未读红点
- `frontend/src/components/layout/AppHeader.vue` - 显示全局未读红点

### 文档

- `docs/06-http/chat.http` - 添加 HTTP 测试用例
- `docs/09-api-spec/chat.md` - 更新 API 文档
- `CHANGELOG.md` - 添加变更记录

---

## Notes

- 本任务与 task-02, task-05 完全独立,可并行开发
- 未读计数使用冗余字段设计,避免实时统计带来的性能问题
- 双向设计确保每个用户独立维护自己的未读状态
- 已读时间 `read_at` 字段为未来的已读回执功能预留

---

## Completion Checklist

完成后,向头 Agent 报告:

- [x] 所有验收标准已满足
- [x] 所有测试通过
- [x] 文档已更新
- [x] CHANGELOG.md 已更新
- [ ] 代码已提交到分支
- [x] 遇到的问题和解决方案(如有)

## Completion Notes

- 已实现全局未读计数、会话未读计数、打开会话标记已读。
- 已接入桌面 Header、移动抽屉和移动底部消息入口红点。
- 后端与前端测试在子 Agent 验收中通过；最终集成后由头 Agent 复跑。
