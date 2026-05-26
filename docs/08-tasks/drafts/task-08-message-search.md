# Task 08: 消息搜索

## Metadata

- **Task ID**: task-08
- **Priority**: P2
- **Estimated effort**: 8-10 hours
- **Dependencies**: None (独立任务)
- **Assignee**: 待分配

---

## Context

### 问题描述

用户需要查找历史消息中的关键信息（商品名称、订单号、约定事项等）。当前只能手动滚动查看,效率低。

### 业务价值

消息搜索能够帮助用户快速定位历史记录,特别是在纠纷处理、订单追溯等场景中非常有用。

---

## Goals

实现以下功能:

1. **按关键词搜索消息**: 输入关键词搜索消息内容
2. **按时间范围筛选**: 选择开始日期和结束日期筛选
3. **搜索结果高亮**: 关键词在搜索结果中高亮显示
4. **点击结果跳转**: 点击搜索结果跳转到对应会话和消息

---

## Database Schema

### 可选优化: 添加全文索引

```sql
-- 为消息内容添加全文索引（MySQL 5.7+）
ALTER TABLE chat_messages ADD FULLTEXT INDEX idx_body_fulltext (body);
```

**设计说明**:
- 全文索引可选,小规模数据（<10万条消息）使用 `LIKE` 查询即可
- 大规模数据建议使用全文索引或 Elasticsearch

---

## API Contract

### 搜索消息

**Endpoint**: `GET /api/chat/messages/search`

**Request**:
- Query parameters:
  - `keyword` (String, optional): 搜索关键词
  - `startTime` (ISO 8601 DateTime, optional): 开始时间
  - `endTime` (ISO 8601 DateTime, optional): 结束时间
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
        "id": 127,
        "conversationId": 1,
        "senderId": 2,
        "body": "这个商品包邮吗?",
        "messageType": "text",
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

**Business Logic**:
- 仅搜索当前用户参与的会话消息
- 按时间倒序排列
- 支持关键词 + 时间范围组合搜索

---

## Frontend Requirements

### 1. 消息搜索组件

创建消息搜索组件,包含:
- 关键词输入框
- 开始日期选择器
- 结束日期选择器
- 搜索按钮
- 搜索结果列表

### 2. 搜索结果显示

搜索结果列表显示:
- 消息内容（关键词高亮）
- 消息时间
- 点击跳转到对应会话

### 3. 关键词高亮

使用 `<mark>` 标签高亮关键词:
- 黄色背景
- 注意 XSS 防护

### 4. 搜索入口

在消息中心添加搜索按钮:
- 点击显示搜索面板
- 搜索面板替换消息线程区域

---

## Acceptance Criteria

### 后端验证

- [ ] 按关键词搜索消息成功
- [ ] 按时间范围筛选消息成功
- [ ] 关键词 + 时间范围组合搜索成功
- [ ] 搜索结果仅返回当前用户参与的会话消息
- [ ] 搜索结果按时间倒序排列
- [ ] 分页功能正常
- [ ] 后端测试全部通过 (`mvnw.cmd test`)

### 前端验证

- [ ] 点击搜索按钮显示搜索面板
- [ ] 输入关键词搜索成功
- [ ] 选择日期范围筛选成功
- [ ] 搜索结果高亮显示关键词
- [ ] 点击搜索结果跳转到对应会话和消息
- [ ] 无结果时显示"未找到匹配的消息"
- [ ] 前端测试全部通过 (`npm test`)

### 集成测试

- [ ] 用户搜索"包邮",返回所有包含"包邮"的消息
- [ ] 用户搜索最近 7 天的消息,返回正确结果
- [ ] 用户点击搜索结果,跳转到对应会话,消息高亮显示

---

## Technical Constraints

### 必须遵守的约束

1. **搜索性能**: 小规模数据使用 `LIKE` 查询,大规模数据建议全文索引或 Elasticsearch
2. **权限控制**: 仅搜索当前用户参与的会话消息
3. **关键词高亮**: 前端使用 `<mark>` 标签高亮,注意 XSS 防护
4. **时间范围**: 使用 ISO 8601 格式传递时间参数
5. **独立功能**: 不依赖其他任务,可独立开发
6. **纯 JDBC**: 使用 `JdbcTemplate`,不使用 ORM

---

## Files to Modify

### 后端

- `backend/src/main/resources/schema.sql` - 可选添加全文索引
- `backend/src/main/java/com/youyu/backend/mapper/chat/ChatMessageMapper.java` - 添加搜索方法
- `backend/src/main/java/com/youyu/backend/mapper/chat/impl/JdbcChatMessageMapper.java` - 实现搜索方法
- `backend/src/main/java/com/youyu/backend/service/chat/impl/ChatServiceImpl.java` - 添加搜索业务逻辑
- `backend/src/main/java/com/youyu/backend/controller/chat/ChatController.java` - 添加搜索接口

### 前端

- `frontend/src/api/modules/chat.js` - 添加搜索 API 方法
- `frontend/src/stores/chat.js` - 添加搜索状态和方法
- `frontend/src/components/chat/MessageSearch.vue` - 创建搜索组件
- `frontend/src/views/app/MessagesView.vue` - 添加搜索入口

### 文档

- `docs/06-http/chat.http` - 添加 HTTP 测试用例
- `docs/09-api-spec/chat.md` - 更新 API 文档
- `CHANGELOG.md` - 添加变更记录

---

## Notes

- 本任务与其他 P2 任务完全独立,可并行开发
- 搜索性能取决于数据量,小规模数据无需全文索引

---

## Completion Checklist

完成后,向头 Agent 报告:

- [ ] 所有验收标准已满足
- [ ] 所有测试通过
- [ ] 文档已更新
- [ ] CHANGELOG.md 已更新
- [ ] 代码已提交到分支
- [ ] 遇到的问题和解决方案(如有)
