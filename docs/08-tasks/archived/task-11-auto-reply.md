# Task 11: 自动回复（卖家离线回复）

## Metadata

- **Task ID**: task-11
- **Priority**: P2
- **Estimated effort**: 6-8 hours
- **Dependencies**: task-06 (快捷回复)
- **Assignee**: 待分配

---

## Context

### 问题描述

卖家不在线时,买家发送消息无法及时得到回复,影响用户体验。自动回复功能可以在卖家离线时自动回复预设消息,告知买家卖家不在线,稍后会回复。

### 业务价值

自动回复能够提升买家体验,减少因等待回复而流失的潜在客户。

---

## Goals

实现以下功能:

1. **卖家可以设置自动回复开关**: 启用/禁用自动回复
2. **卖家可以自定义自动回复内容**: 编辑自动回复文案
3. **卖家离线时自动回复**: 买家发送消息时自动回复
4. **每个会话只自动回复一次**: 24 小时内每个会话只自动回复一次（避免骚扰）

---

## Database Schema

### 新建 `auto_reply_settings` 表

需要创建以下表:

```sql
CREATE TABLE IF NOT EXISTS auto_reply_settings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  reply_content TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_auto_reply_user FOREIGN KEY (user_id) REFERENCES users(id),
  UNIQUE KEY uk_user_id (user_id)
);
```

### `chat_conversations` 表添加自动回复记录字段

需要添加以下字段:
- `auto_replied_to_a_at` (TIMESTAMP, NULL): 最后一次向 user_a 自动回复的时间
- `auto_replied_to_b_at` (TIMESTAMP, NULL): 最后一次向 user_b 自动回复的时间

**设计说明**:
- `auto_reply_settings` 存储用户的自动回复配置
- `auto_replied_to_a_at` 和 `auto_replied_to_b_at` 记录最后一次自动回复时间,避免重复回复
- 每个会话 24 小时内只自动回复一次

---

## API Contract

### 1. 获取自动回复设置

**Endpoint**: `GET /api/chat/auto-reply`

**Response**:
```json
{
  "success": true,
  "data": {
    "isEnabled": false,
    "replyContent": "您好,我暂时不在线,稍后会回复您的消息。"
  }
}
```

**Business Logic**:
- 如果用户没有设置,返回默认配置

### 2. 更新自动回复设置

**Endpoint**: `PUT /api/chat/auto-reply`

**Request**:
```json
{
  "isEnabled": true,
  "replyContent": "您好,我暂时不在线,稍后会回复您的消息。"
}
```

**Response**:
```json
{
  "success": true,
  "data": null
}
```

**Business Logic**:
- 验证内容不为空
- 内容长度限制: 最大 500 字符
- 使用 `INSERT ... ON DUPLICATE KEY UPDATE` 实现 upsert

---

## Business Integration

### 发送消息时触发自动回复

在 `ChatServiceImpl.sendMessage()` 方法中:
1. 查询接收者的自动回复设置
2. 如果启用自动回复,检查是否已经自动回复过（24 小时内）
3. 如果未回复过,自动发送回复消息
4. 更新自动回复时间

**触发条件**:
- 接收者启用了自动回复
- 24 小时内未向该会话自动回复过

---

## Frontend Requirements

### 1. 自动回复设置页面

创建自动回复设置页面 (`/app/settings/auto-reply`),显示:
- 启用/禁用开关
- 自动回复内容输入框（多行文本）
- 保存按钮
- 提示文字："当您离线时,系统会自动回复买家的第一条消息。"

### 2. 默认自动回复内容

如果用户没有设置,显示默认内容:
- "您好,我暂时不在线,稍后会回复您的消息。"

---

## Acceptance Criteria

### 后端验证

- [ ] 数据库迁移脚本执行成功,表和字段创建成功
- [ ] 获取自动回复设置成功（默认返回关闭状态）
- [ ] 更新自动回复设置成功
- [ ] 卖家离线时,买家发送消息触发自动回复
- [ ] 每个会话 24 小时内只自动回复一次
- [ ] 自动回复消息正确插入到消息表
- [ ] 后端测试全部通过 (`mvnw.cmd test`)

### 前端验证

- [ ] 自动回复设置页面正确显示当前设置
- [ ] 切换开关可以启用/禁用自动回复
- [ ] 修改自动回复内容并保存成功
- [ ] 禁用状态下,文本框不可编辑
- [ ] 保存后显示成功提示
- [ ] 前端测试全部通过 (`npm test`)

### 集成测试

- [ ] 卖家启用自动回复,设置内容为"我不在,稍后回复"
- [ ] 买家发送消息,立即收到自动回复
- [ ] 买家再次发送消息,24 小时内不再收到自动回复
- [ ] 24 小时后,买家发送消息,再次收到自动回复

---

## Technical Constraints

### 必须遵守的约束

1. **依赖 task-06**: 复用快捷回复的内容管理逻辑
2. **频率限制**: 每个会话 24 小时内只自动回复一次
3. **内容长度限制**: 最大 500 字符
4. **触发时机**: 仅在接收者离线时触发（可选实现在线状态检测）
5. **默认内容**: 提供默认自动回复内容
6. **纯 JDBC**: 使用 `JdbcTemplate`,不使用 ORM

---

## Files to Modify

### 后端

- `backend/src/main/resources/schema.sql` - 创建表和字段
- `backend/src/main/java/com/youyu/backend/entity/chat/` - 创建 Entity 类
- `backend/src/main/java/com/youyu/backend/mapper/chat/` - 创建 Mapper 接口和实现
- `backend/src/main/java/com/youyu/backend/service/chat/` - 创建 Service 接口和实现
- `backend/src/main/java/com/youyu/backend/controller/chat/` - 创建 Controller
- `backend/src/main/java/com/youyu/backend/service/chat/impl/ChatServiceImpl.java` - 集成自动回复逻辑

### 前端

- `frontend/src/api/modules/chat.js` - 添加 API 方法
- `frontend/src/views/app/SettingsAutoReplyView.vue` - 创建设置页面
- `frontend/src/router/modules/app.js` - 添加路由

### 文档

- `docs/06-http/chat.http` - 添加 HTTP 测试用例
- `docs/09-api-spec/chat.md` - 更新 API 文档
- `CHANGELOG.md` - 添加变更记录

---

## Notes

- 本任务依赖 task-06,需等 task-06 完成后才能开始
- 自动回复是卖家提效工具,能够提升买家体验

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
- Implemented `auto_reply_settings`, settings APIs, the settings page, and send-message auto-reply insertion with 24-hour throttling.
- Added backend coverage for settings persistence and trigger behavior.
- Verification: `backend\mvnw.cmd test`, `frontend\npm test`, `frontend\npm run build`.
