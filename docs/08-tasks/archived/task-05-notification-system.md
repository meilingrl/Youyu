# Task 05: 站内通知系统

## Metadata

- **Task ID**: task-05
- **Priority**: P0
- **Estimated effort**: 8-10 hours
- **Dependencies**: None (独立任务)
- **Assignee**: 子 Agent C
- **Status**: done
- **Completed date**: 2026-05-25

---

## Context

### 问题描述

当前系统缺少站内通知功能,用户无法及时收到重要信息:
- 订单状态变更（已支付、已发货、已送达）
- 评价提醒（待评价的订单）
- 系统消息（活动通知、政策变更）

用户需要主动刷新页面才能看到订单状态变化,体验差。

### 业务价值

站内通知是电商平台的核心功能,能够及时告知用户重要信息,提升用户体验和平台活跃度。

---

## Goals

实现以下功能:

1. **订单状态变更通知**: 订单状态变更时自动发送通知给买家和卖家
2. **导航栏显示未读通知红点**: 显示未读通知数量
3. **通知列表页面**: 显示所有通知,支持分页
4. **点击通知跳转**: 点击通知跳转到对应页面（如订单详情）
5. **标记已读**: 支持单个标记已读和全部标记已读

---

## Database Schema

### 新建 `notifications` 表

需要创建以下表:

```sql
CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  type VARCHAR(32) NOT NULL,
  title VARCHAR(200) NOT NULL,
  body TEXT NOT NULL,
  action_url VARCHAR(512) NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_user_read (user_id, is_read),
  INDEX idx_user_created (user_id, created_at DESC)
);
```

**字段说明**:
- `user_id`: 接收通知的用户 ID
- `type`: 通知类型,枚举值: 'order_status', 'review_reminder', 'system'
- `title`: 通知标题（最大 200 字符）
- `body`: 通知内容（TEXT 类型）
- `action_url`: 点击跳转的 URL（如 `/app/orders/123`）
- `is_read`: 是否已读
- `created_at`: 创建时间

**索引说明**:
- `idx_user_read`: 用于快速查询用户的未读通知
- `idx_user_created`: 用于按时间倒序查询用户的通知列表

---

## API Contract

### 1. 获取通知列表

**Endpoint**: `GET /api/notifications`

**Request**:
- Query parameters:
  - `page` (int, optional, default: 0): 页码
  - `size` (int, optional, default: 20): 每页数量
- Headers: `Authorization: Bearer <token>`

**Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "userId": 2,
        "type": "order_status",
        "title": "订单状态更新",
        "body": "您的订单 ORD-20260525-001 状态已更新为：已发货",
        "actionUrl": "/app/orders/123",
        "isRead": false,
        "createdAt": "2026-05-25T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "total": 5,
    "totalPages": 1
  }
}
```

### 2. 获取未读通知计数

**Endpoint**: `GET /api/notifications/unread-count`

**Request**:
- Headers: `Authorization: Bearer <token>`

**Response**:
```json
{
  "success": true,
  "data": {
    "count": 3
  }
}
```

### 3. 标记单个通知已读

**Endpoint**: `POST /api/notifications/{id}/read`

**Request**:
- Path parameter: `id` (Long) - 通知 ID
- Headers: `Authorization: Bearer <token>`

**Response**:
```json
{
  "success": true,
  "data": null
}
```

**Business Logic**:
- 验证通知属于当前用户
- 将通知标记为已读

### 4. 标记所有通知已读

**Endpoint**: `POST /api/notifications/read-all`

**Request**:
- Headers: `Authorization: Bearer <token>`

**Response**:
```json
{
  "success": true,
  "data": null
}
```

**Business Logic**:
- 将当前用户的所有未读通知标记为已读

---

## Business Integration

### 订单状态变更时发送通知

需要在订单状态变更时自动发送通知:

**触发场景**:
- 订单支付成功 → 通知买家和卖家
- 订单发货 → 通知买家
- 订单送达 → 通知买家
- 订单取消 → 通知买家和卖家
- 订单退款 → 通知买家和卖家

**通知内容示例**:
- 标题: "订单状态更新"
- 内容: "您的订单 {订单号} 状态已更新为：{状态文本}"
- 跳转链接: `/app/orders/{订单ID}`

**实现位置**:
- 在 `OrderServiceImpl` 的订单状态变更方法中调用通知服务

---

## Frontend Requirements

### 1. 通知列表页面

创建通知列表页面 (`/app/notifications`),显示:
- 所有通知列表（分页）
- 未读通知高亮显示（背景色区分）
- 通知类型图标（📦 订单、⭐ 评价、🔔 系统）
- 通知时间（相对时间,如"5分钟前"）
- 点击通知跳转到对应页面
- "全部标记为已读"按钮

### 2. 导航栏显示未读红点

在导航栏添加通知图标:
- 显示通知图标（🔔）
- 未读计数 > 0 时显示红色徽章
- 未读计数超过 99 显示为 "99+"
- 点击跳转到通知列表页面

### 3. 通知轮询刷新

- 每分钟刷新一次未读计数
- 或与会话列表轮询同步刷新

---

## Acceptance Criteria

### 后端验证

- [ ] 数据库迁移脚本执行成功,表和索引创建成功
- [ ] 发送通知接口成功
- [ ] 获取通知列表接口返回正确数据（分页）
- [ ] 获取未读计数接口返回正确数字
- [ ] 标记单个通知已读成功
- [ ] 标记所有通知已读成功
- [ ] 订单状态变更时自动发送通知
- [ ] 后端测试全部通过 (`mvnw.cmd test`)

### 前端验证

- [ ] 导航栏显示通知图标和未读红点
- [ ] 点击通知图标跳转到通知列表页
- [ ] 通知列表正确显示所有通知（分页）
- [ ] 未读通知高亮显示
- [ ] 点击通知标记已读并跳转到对应页面
- [ ] "全部标记为已读"按钮生效
- [ ] 未读计数实时更新
- [ ] 前端测试全部通过 (`npm test`)

### 集成测试

- [ ] 订单状态从"待支付"变更为"已支付",买家和卖家收到通知
- [ ] 导航栏未读红点显示
- [ ] 点击通知跳转到订单详情页
- [ ] 标记已读后红点消失

---

## Technical Constraints

### 必须遵守的约束

1. **独立系统**: 通知系统与聊天系统独立,不复用 `chat_messages` 表
2. **异步发送**: 通知发送不阻塞主业务流程（使用 `@Transactional`）
3. **通知类型扩展**: `type` 字段预留扩展空间（review_reminder, system）
4. **性能考虑**: 未读计数使用索引优化查询
5. **前端轮询**: 每分钟刷新一次未读计数（未来可升级为 WebSocket 推送）
6. **纯 JDBC**: 使用 `JdbcTemplate`,不使用 ORM
7. **事务管理**: 使用 `@Transactional` 保证数据一致性

### 实现建议(非强制)

- 通知服务可以作为独立的 Service,被其他业务模块调用
- 通知类型可以使用枚举类定义
- 通知内容可以使用模板生成

---

## Files to Modify

### 后端

- `backend/src/main/resources/schema.sql` - 创建表和索引
- `backend/src/main/java/com/youyu/backend/entity/notification/` - 创建 Entity 类
- `backend/src/main/java/com/youyu/backend/mapper/notification/` - 创建 Mapper 接口和实现
- `backend/src/main/java/com/youyu/backend/service/notification/` - 创建 Service 接口和实现
- `backend/src/main/java/com/youyu/backend/controller/notification/` - 创建 Controller
- `backend/src/main/java/com/youyu/backend/service/order/impl/OrderServiceImpl.java` - 集成通知发送

### 前端

- `frontend/src/api/modules/notification.js` - 创建 API 模块
- `frontend/src/api/index.js` - 导出 notification 模块
- `frontend/src/stores/notification.js` - 创建 Store
- `frontend/src/views/app/NotificationsView.vue` - 创建通知列表页面
- `frontend/src/router/modules/app.js` - 添加路由
- `frontend/src/components/layout/AppHeader.vue` - 添加通知图标

### 文档

- `docs/06-http/notification.http` - 创建 HTTP 测试用例
- `docs/09-api-spec/notification.md` - 创建 API 文档
- `CHANGELOG.md` - 添加变更记录

---

## Notes

- 本任务与 task-01, task-02 完全独立,可并行开发
- 通知系统是独立的功能模块,不依赖聊天系统
- 订单状态变更通知是第一个业务集成点,未来可扩展到评价提醒、系统消息等

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

- 已实现通知列表、未读计数、单条已读、全部已读、通知列表页和路由。
- 已接入订单取消、发货、确认收货、退款等订单状态通知，并由头 Agent 补充支付成功通知。
- 已接入桌面 Header 和移动抽屉通知红点。
