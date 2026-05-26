# Task 06: 快捷回复（卖家工具）

## Metadata

- **Task ID**: task-06
- **Priority**: P1
- **Estimated effort**: 6-8 hours
- **Dependencies**: None (独立任务)
- **Assignee**: 待分配

---

## Context

### 问题描述

卖家需要频繁回复买家的常见问题（"在的"、"包邮吗"、"什么时候发货"等）,手动输入效率低。快捷回复功能可以预设常用话术,一键发送。

### 业务价值

快捷回复能够显著提升卖家的响应速度,减少重复劳动,提升买家满意度。

---

## Goals

实现以下功能:

1. **卖家可以管理快捷回复**: 创建、编辑、删除快捷回复
2. **消息输入框显示快捷回复按钮**: 点击显示快捷回复列表
3. **点击快捷回复自动填充**: 点击快捷回复自动填充到输入框
4. **默认快捷回复**: 提供默认话术,用户可自定义

---

## Database Schema

### 新建 `quick_replies` 表

需要创建以下表:

```sql
CREATE TABLE IF NOT EXISTS quick_replies (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_quick_reply_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_user_sort (user_id, sort_order)
);
```

**字段说明**:
- `user_id`: 用户 ID（卖家）
- `content`: 快捷回复内容（TEXT 类型,最大 65535 字符）
- `sort_order`: 排序顺序（用于自定义排序）
- `created_at`: 创建时间
- `updated_at`: 更新时间

---

## API Contract

### 1. 获取快捷回复列表

**Endpoint**: `GET /api/chat/quick-replies`

**Request**:
- Headers: `Authorization: Bearer <token>`

**Response**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 2,
      "content": "亲,在的!有什么可以帮您的吗?",
      "sortOrder": 0,
      "createdAt": "2026-05-25T10:00:00",
      "updatedAt": "2026-05-25T10:00:00"
    }
  ]
}
```

**Business Logic**:
- 按 `sort_order` ASC, `created_at` ASC 排序

### 2. 创建快捷回复

**Endpoint**: `POST /api/chat/quick-replies`

**Request**:
```json
{
  "content": "包邮哦,全国包邮!",
  "sortOrder": 1
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": 2
  }
}
```

**Business Logic**:
- 验证内容不为空
- 内容长度限制: 最大 500 字符

### 3. 更新快捷回复

**Endpoint**: `PUT /api/chat/quick-replies/{id}`

**Request**:
```json
{
  "content": "今天下单,明天发货!",
  "sortOrder": 2
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
- 验证快捷回复属于当前用户
- 验证内容不为空

### 4. 删除快捷回复

**Endpoint**: `DELETE /api/chat/quick-replies/{id}`

**Response**:
```json
{
  "success": true,
  "data": null
}
```

**Business Logic**:
- 验证快捷回复属于当前用户

---

## Frontend Requirements

### 1. 快捷回复面板组件

创建快捷回复面板组件,显示:
- 快捷回复列表（最多显示 10 条）
- 点击快捷回复自动填充到输入框
- 如果用户没有自定义快捷回复,显示默认话术

**默认快捷回复**:
- "亲,在的!有什么可以帮您的吗?"
- "包邮哦,全国包邮!"
- "今天下单,明天发货!"
- "有现货,可以立即发货!"
- "支持七天无理由退换货!"
- "有任何问题随时联系我!"

### 2. 消息输入区域添加快捷回复按钮

在消息输入框左侧添加快捷回复按钮:
- 按钮图标: ⚡ 或 "快捷回复"
- 点击按钮显示快捷回复面板
- 面板显示在输入框上方

### 3. 快捷回复管理页面（可选）

创建快捷回复管理页面（`/app/settings/quick-replies`）:
- 显示所有快捷回复
- 支持创建、编辑、删除
- 支持拖拽排序

---

## Acceptance Criteria

### 后端验证

- [ ] 数据库迁移脚本执行成功,表和索引创建成功
- [ ] 创建快捷回复成功
- [ ] 获取快捷回复列表成功（按 sort_order 排序）
- [ ] 更新快捷回复成功
- [ ] 删除快捷回复成功
- [ ] 非所有者无法修改/删除他人的快捷回复
- [ ] 后端测试全部通过 (`mvnw.cmd test`)

### 前端验证

- [ ] 点击"快捷回复"按钮显示面板
- [ ] 面板显示默认快捷回复（如果用户没有自定义）
- [ ] 点击快捷回复自动填充到输入框
- [ ] 点击快捷回复后面板自动关闭
- [ ] 快捷回复面板滚动正常（超过 6 条时）
- [ ] 前端测试全部通过 (`npm test`)

### 集成测试

- [ ] 卖家创建自定义快捷回复
- [ ] 卖家在聊天中使用快捷回复
- [ ] 快捷回复内容正确填充到输入框
- [ ] 发送快捷回复消息成功

---

## Technical Constraints

### 必须遵守的约束

1. **权限控制**: 只能修改/删除自己的快捷回复
2. **内容长度限制**: 最大 500 字符
3. **默认快捷回复**: 前端提供 6 条默认话术,用户可自定义覆盖
4. **排序**: 按 `sort_order` ASC, `created_at` ASC 排序
5. **独立功能**: 不依赖其他任务,可独立开发
6. **纯 JDBC**: 使用 `JdbcTemplate`,不使用 ORM
7. **事务管理**: 使用 `@Transactional` 保证数据一致性

---

## Files to Modify

### 后端

- `backend/src/main/resources/schema.sql` - 创建表和索引
- `backend/src/main/java/com/youyu/backend/entity/chat/` - 创建 Entity 类
- `backend/src/main/java/com/youyu/backend/mapper/chat/` - 创建 Mapper 接口和实现
- `backend/src/main/java/com/youyu/backend/service/chat/` - 创建 Service 接口和实现
- `backend/src/main/java/com/youyu/backend/controller/chat/` - 创建 Controller

### 前端

- `frontend/src/api/modules/chat.js` - 添加 API 方法
- `frontend/src/stores/chat.js` - 添加状态和方法
- `frontend/src/components/chat/QuickReplyPanel.vue` - 创建快捷回复面板组件
- `frontend/src/views/app/MessagesView.vue` - 添加快捷回复按钮

### 文档

- `docs/06-http/chat.http` - 添加 HTTP 测试用例
- `docs/09-api-spec/chat.md` - 更新 API 文档
- `CHANGELOG.md` - 添加变更记录

---

## Notes

- 本任务与其他任务完全独立,可并行开发
- 快捷回复是卖家提效工具,能够显著提升响应速度
- task-11（自动回复）依赖本任务

---

## Completion Checklist

完成后,向头 Agent 报告:

- [ ] 所有验收标准已满足
- [ ] 所有测试通过
- [ ] 文档已更新
- [ ] CHANGELOG.md 已更新
- [ ] 代码已提交到分支
- [ ] 遇到的问题和解决方案(如有)
- [ ] 通知头 Agent: task-11 可以开始
