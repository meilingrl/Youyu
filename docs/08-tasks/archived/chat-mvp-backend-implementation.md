# Task: Chat MVP Backend Implementation

## Metadata

- ID: chat-mvp-backend-implementation
- Status: completed
- Owner: Claude
- Track: feature
- Depends on: none (可独立执行)
- Priority: high
- Planned date: 2026-05-25
- Completed date: 2026-05-25

## Objective

实现消息功能的后端 MVP,提供基础的会话管理和消息发送能力。范围严格限定为文本消息 + 轮询刷新,不包含 WebSocket、未读数统计、图片上传等高级功能。

## Background

根据 `communication-and-after-sales-boundary.md`,当前阶段不构建复杂 IM 系统,采用轻量级沟通方案。

**MVP 范围定义:**
- ✅ 会话列表(按用户维度)
- ✅ 消息列表(分页加载)
- ✅ 发送文本消息(纯文本,无附件)
- ✅ 从商品/店铺页发起会话(携带 productId/shopId)
- ✅ 基础权限控制(登录用户可发送)

**明确不做:**
- ❌ 未读数统计和最后消息摘要
- ❌ 图片/文件/语音消息
- ❌ WebSocket 实时推送
- ❌ 已读回执
- ❌ 群聊功能

## Database Design

### Table 1: chat_conversations

```sql
CREATE TABLE IF NOT EXISTS chat_conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(32) NOT NULL DEFAULT 'direct',
    product_id BIGINT,
    shop_id BIGINT,
    user_a_id BIGINT NOT NULL,
    user_b_id BIGINT NOT NULL,
    last_message_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_conv_user_a FOREIGN KEY (user_a_id) REFERENCES users(id),
    CONSTRAINT fk_chat_conv_user_b FOREIGN KEY (user_b_id) REFERENCES users(id),
    CONSTRAINT fk_chat_conv_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_chat_conv_shop FOREIGN KEY (shop_id) REFERENCES shops(id),
    INDEX idx_user_a_last_message (user_a_id, last_message_at DESC),
    INDEX idx_user_b_last_message (user_b_id, last_message_at DESC),
    UNIQUE INDEX uk_conversation_pair (user_a_id, user_b_id, product_id, shop_id)
);
```

**字段说明:**
- `type`: 会话类型,默认 'direct'(直接对话),预留 'product_inquiry'(商品咨询)、'shop_inquiry'(店铺咨询)
- `product_id`: 关联商品 ID(可选,用于商品咨询场景)
- `shop_id`: 关联店铺 ID(可选,用于店铺咨询场景)
- `user_a_id`: 发起方用户 ID
- `user_b_id`: 接收方用户 ID
- `last_message_at`: 最后消息时间(用于排序)
- `uk_conversation_pair`: 唯一约束,防止重复创建会话

### Table 2: chat_messages

```sql
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_user_id BIGINT NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_msg_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id),
    CONSTRAINT fk_chat_msg_sender FOREIGN KEY (sender_user_id) REFERENCES users(id),
    INDEX idx_conversation_created (conversation_id, created_at DESC)
);
```

**字段说明:**
- `conversation_id`: 所属会话 ID
- `sender_user_id`: 发送者用户 ID
- `body`: 消息内容(纯文本)
- `created_at`: 发送时间

## API Design

### 1. GET /api/chat/conversations

获取当前用户的会话列表。

**请求参数:**
```
page: int (默认 0)
size: int (默认 20,最大 50)
```

**响应:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "type": "product_inquiry",
        "productId": 123,
        "shopId": 45,
        "peerUser": {
          "id": 2,
          "username": "seller01",
          "nickname": "晨曦二手书铺",
          "avatar": "..."
        },
        "lastMessageAt": "2026-05-25T14:20:00",
        "createdAt": "2026-05-25T10:00:00"
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

**业务逻辑:**
- 查询当前用户作为 `user_a_id` 或 `user_b_id` 的所有会话
- 按 `last_message_at` 倒序排序
- 分页返回
- 返回对方用户的基本信息(nickname, avatar)

### 2. POST /api/chat/conversations

查找或创建会话(幂等操作)。

**请求体:**
```json
{
  "peerUserId": 2,
  "productId": 123,  // 可选
  "shopId": 45       // 可选
}
```

**响应:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "type": "product_inquiry",
    "productId": 123,
    "shopId": 45,
    "peerUser": { ... },
    "lastMessageAt": "2026-05-25T14:20:00",
    "createdAt": "2026-05-25T10:00:00"
  }
}
```

**业务逻辑:**
- 根据 `(currentUserId, peerUserId, productId, shopId)` 查找现有会话
- 如果存在,直接返回
- 如果不存在,创建新会话:
  - `user_a_id` = currentUserId
  - `user_b_id` = peerUserId
  - `type` = 根据 productId/shopId 判断('product_inquiry' / 'shop_inquiry' / 'direct')
- 幂等性保证: 唯一索引 `uk_conversation_pair`

### 3. GET /api/chat/conversations/{id}/messages

获取会话的消息列表。

**请求参数:**
```
page: int (默认 0)
size: int (默认 20,最大 100)
```

**响应:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "conversationId": 1,
        "senderUserId": 2,
        "body": "您好,退款已提交审核。",
        "createdAt": "2026-05-25T14:18:00"
      },
      {
        "id": 2,
        "conversationId": 1,
        "senderUserId": 1,
        "body": "好的,谢谢!",
        "createdAt": "2026-05-25T14:20:00"
      }
    ],
    "totalElements": 2,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

**业务逻辑:**
- 验证当前用户是会话参与者(user_a_id 或 user_b_id)
- 按 `created_at` 倒序排序(最新消息在前)
- 分页返回

### 4. POST /api/chat/conversations/{id}/messages

发送消息。

**请求体:**
```json
{
  "body": "您好,请问教材退款进度如何?"
}
```

**响应:**
```json
{
  "success": true,
  "data": {
    "id": 3,
    "conversationId": 1,
    "senderUserId": 1,
    "body": "您好,请问教材退款进度如何?",
    "createdAt": "2026-05-25T14:25:00"
  }
}
```

**业务逻辑:**
- 验证当前用户是会话参与者
- 验证消息内容非空,长度 ≤ 2000 字符
- 插入消息记录
- 更新会话的 `last_message_at`
- 返回新消息

## Backend Architecture

### Package Structure

```
backend/src/main/java/com/youyu/backend/
├── controller/chat/
│   ├── ChatController.java
│   ├── ConversationDTO.java
│   ├── MessageDTO.java
│   ├── CreateConversationRequest.java
│   └── SendMessageRequest.java
├── service/chat/
│   ├── ChatService.java
│   └── ChatServiceImpl.java
├── mapper/chat/
│   ├── ChatConversationMapper.java
│   ├── JdbcChatConversationMapper.java
│   ├── ChatMessageMapper.java
│   └── JdbcChatMessageMapper.java
└── entity/chat/
    ├── ChatConversation.java
    └── ChatMessage.java
```

### Entity Classes

**ChatConversation.java:**
```java
package com.youyu.backend.entity.chat;

import java.time.LocalDateTime;

public class ChatConversation {
    private Long id;
    private String type;
    private Long productId;
    private Long shopId;
    private Long userAId;
    private Long userBId;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    
    // getters and setters
}
```

**ChatMessage.java:**
```java
package com.youyu.backend.entity.chat;

import java.time.LocalDateTime;

public class ChatMessage {
    private Long id;
    private Long conversationId;
    private Long senderUserId;
    private String body;
    private LocalDateTime createdAt;
    
    // getters and setters
}
```

### Mapper Interfaces

**ChatConversationMapper.java:**
```java
package com.youyu.backend.mapper.chat;

import java.util.List;
import java.util.Map;

public interface ChatConversationMapper {
    Map<String, Object> findById(Long id);
    List<Map<String, Object>> findByUserId(Long userId, int offset, int limit);
    int countByUserId(Long userId);
    Map<String, Object> findByParticipants(Long userAId, Long userBId, Long productId, Long shopId);
    Long insert(Map<String, Object> conversation);
    int updateLastMessageAt(Long id, String lastMessageAt);
}
```

**ChatMessageMapper.java:**
```java
package com.youyu.backend.mapper.chat;

import java.util.List;
import java.util.Map;

public interface ChatMessageMapper {
    List<Map<String, Object>> findByConversationId(Long conversationId, int offset, int limit);
    int countByConversationId(Long conversationId);
    Long insert(Map<String, Object> message);
}
```

### Service Interface

**ChatService.java:**
```java
package com.youyu.backend.service.chat;

import java.util.Map;

public interface ChatService {
    Map<String, Object> getConversations(Long userId, int page, int size);
    Map<String, Object> findOrCreateConversation(Long currentUserId, Long peerUserId, Long productId, Long shopId);
    Map<String, Object> getMessages(Long conversationId, Long currentUserId, int page, int size);
    Map<String, Object> sendMessage(Long conversationId, Long currentUserId, String body);
}
```

### Controller

**ChatController.java:**
```java
package com.youyu.backend.controller.chat;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.service.chat.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversations")
    @LoginRequired
    public ApiResponse<Map<String, Object>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = AuthContextHolder.getUser().getUserId();
        return ApiResponse.success(chatService.getConversations(userId, page, size));
    }

    @PostMapping("/conversations")
    @LoginRequired
    public ApiResponse<Map<String, Object>> createConversation(@RequestBody CreateConversationRequest request) {
        Long currentUserId = AuthContextHolder.getUser().getUserId();
        return ApiResponse.success(chatService.findOrCreateConversation(
            currentUserId, request.getPeerUserId(), request.getProductId(), request.getShopId()));
    }

    @GetMapping("/conversations/{id}/messages")
    @LoginRequired
    public ApiResponse<Map<String, Object>> getMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = AuthContextHolder.getUser().getUserId();
        return ApiResponse.success(chatService.getMessages(id, userId, page, size));
    }

    @PostMapping("/conversations/{id}/messages")
    @LoginRequired
    public ApiResponse<Map<String, Object>> sendMessage(
            @PathVariable Long id,
            @RequestBody SendMessageRequest request) {
        Long userId = AuthContextHolder.getUser().getUserId();
        return ApiResponse.success(chatService.sendMessage(id, userId, request.getBody()));
    }
}
```

## Files to Read

- `backend/src/main/resources/schema.sql` (数据库结构)
- `backend/src/main/java/com/youyu/backend/controller/order/OrderController.java` (参考 Controller 模式)
- `backend/src/main/java/com/youyu/backend/service/order/OrderServiceImpl.java` (参考 Service 模式)
- `backend/src/main/java/com/youyu/backend/mapper/order/JdbcOrderMapper.java` (参考 Mapper 模式)
- `backend/src/main/java/com/youyu/backend/entity/order/Order.java` (参考 Entity 模式)

## In Scope

### 1. 数据库迁移
- 在 `schema.sql` 末尾添加两张表的 DDL
- 确保幂等性(使用 `CREATE TABLE IF NOT EXISTS`)

### 2. 实体类
- `ChatConversation.java`
- `ChatMessage.java`

### 3. Mapper 层
- `ChatConversationMapper.java` (接口)
- `JdbcChatConversationMapper.java` (实现)
- `ChatMessageMapper.java` (接口)
- `JdbcChatMessageMapper.java` (实现)

### 4. Service 层
- `ChatService.java` (接口)
- `ChatServiceImpl.java` (实现,带 `@Service` 和 `@Transactional`)

### 5. Controller 层
- `ChatController.java`
- DTOs: `ConversationDTO`, `MessageDTO`, `CreateConversationRequest`, `SendMessageRequest`

### 6. 集成测试
- `ChatControllerTest.java` (基础 CRUD 测试)

### 7. HTTP Smoke Test
- `docs/06-http/chat.http`

## Out of Scope

- ❌ 未读数统计
- ❌ 最后消息摘要
- ❌ WebSocket 实时推送
- ❌ 图片/文件上传
- ❌ 消息搜索
- ❌ 消息删除/撤回
- ❌ 群聊功能
- ❌ 管理端 API

## Hard Limits

- **不修改**: 现有表结构
- **不修改**: 现有 Service/Controller 文件
- **不引入**: 新的第三方依赖(使用现有 Spring Boot + JDBC)
- **不实现**: 超出 MVP 范围的功能

## Allowed Changes

- `backend/src/main/resources/schema.sql` (追加两张表)
- `backend/src/main/java/com/youyu/backend/entity/chat/` (新建)
- `backend/src/main/java/com/youyu/backend/mapper/chat/` (新建)
- `backend/src/main/java/com/youyu/backend/service/chat/` (新建)
- `backend/src/main/java/com/youyu/backend/controller/chat/` (新建)
- `backend/src/test/java/com/youyu/backend/controller/chat/` (新建)
- `docs/06-http/chat.http` (新建)
- `CHANGELOG.md`

## Implementation Steps

### Step 1: 数据库迁移
1. 在 `schema.sql` 末尾添加 `chat_conversations` 表 DDL
2. 添加 `chat_messages` 表 DDL
3. 确认索引和外键约束正确

### Step 2: 实体类
1. 创建 `entity/chat/ChatConversation.java`
2. 创建 `entity/chat/ChatMessage.java`

### Step 3: Mapper 层
1. 创建 `ChatConversationMapper.java` 接口
2. 实现 `JdbcChatConversationMapper.java`:
   - `findById`
   - `findByUserId` (分页)
   - `countByUserId`
   - `findByParticipants` (查找现有会话)
   - `insert`
   - `updateLastMessageAt`
3. 创建 `ChatMessageMapper.java` 接口
4. 实现 `JdbcChatMessageMapper.java`:
   - `findByConversationId` (分页)
   - `countByConversationId`
   - `insert`

### Step 4: Service 层
1. 创建 `ChatService.java` 接口
2. 实现 `ChatServiceImpl.java`:
   - `getConversations`: 查询会话列表,填充对方用户信息
   - `findOrCreateConversation`: 查找或创建会话(幂等)
   - `getMessages`: 验证权限 + 查询消息列表
   - `sendMessage`: 验证权限 + 插入消息 + 更新 last_message_at

### Step 5: Controller 层
1. 创建 DTOs:
   - `CreateConversationRequest.java`
   - `SendMessageRequest.java`
2. 创建 `ChatController.java`:
   - 4个端点
   - 使用 `@LoginRequired`
   - 返回 `ApiResponse<T>`

### Step 6: 集成测试
1. 创建 `ChatControllerTest.java`:
   - 测试创建会话
   - 测试发送消息
   - 测试查询会话列表
   - 测试查询消息列表
   - 测试权限校验

### Step 7: HTTP Smoke Test
1. 创建 `docs/06-http/chat.http`:
   - 登录获取 token
   - 创建会话
   - 发送消息
   - 查询会话列表
   - 查询消息列表

### Step 8: 验证
1. 启动后端: `mvnw.cmd spring-boot:run`
2. 运行集成测试: `mvnw.cmd test -Dtest=ChatControllerTest`
3. 手动测试 HTTP 端点
4. 确认无控制台错误

### Step 9: 文档更新
1. 更新 `CHANGELOG.md`

## Test Plan

### 单元测试
- Mapper 层测试(可选,集成测试已覆盖)

### 集成测试
- [ ] 创建会话成功
- [ ] 重复创建会话返回现有会话(幂等性)
- [ ] 发送消息成功
- [ ] 查询会话列表分页正确
- [ ] 查询消息列表分页正确
- [ ] 非参与者无法查看会话消息(权限校验)
- [ ] 消息内容为空时返回错误
- [ ] 消息内容超长时返回错误

### HTTP Smoke Test
- [ ] 所有端点返回 200
- [ ] 响应格式符合 `ApiResponse<T>` 规范
- [ ] 分页参数正确工作

## Acceptance Criteria

- [ ] 两张表成功创建,索引和外键正确
- [ ] 4个 API 端点全部实现并通过测试
- [ ] 会话创建幂等性正确(重复调用返回同一会话)
- [ ] 消息发送后 `last_message_at` 正确更新
- [ ] 权限校验正确(非参与者无法访问)
- [ ] 分页功能正确(page/size 参数生效)
- [ ] 集成测试全部通过
- [ ] HTTP smoke test 全部通过
- [ ] 无控制台错误或警告
- [ ] `CHANGELOG.md` 已更新

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] `docs/06-http/chat.http`

## Risks and Mitigations

### 风险1: 唯一索引冲突
- **风险**: `uk_conversation_pair` 可能因 NULL 值导致重复
- **缓解**: 在 Service 层处理 NULL 值,确保查询和插入逻辑一致

### 风险2: 并发创建会话
- **风险**: 两个请求同时创建会话可能导致唯一索引冲突
- **缓解**: 捕获唯一索引异常,重新查询并返回现有会话

### 风险3: 性能问题
- **风险**: 会话列表查询可能扫描大量数据
- **缓解**: 使用复合索引 `idx_user_a_last_message` 和 `idx_user_b_last_message`

## Final Report Format

```markdown
## Return Report — chat-mvp-backend-implementation

### A. Branch & Commit
- Branch: <name>
- Commit SHA: <sha>
- Files changed: (paste `git diff --stat`)

### B. Implementation Summary
- 数据库迁移: ✓
- 实体类: ✓
- Mapper 层: ✓
- Service 层: ✓
- Controller 层: ✓
- 集成测试: ✓
- HTTP Smoke Test: ✓

### C. Test Results
- 集成测试: [通过/失败]
- HTTP Smoke Test: [通过/失败]

### D. API Endpoints
- GET /api/chat/conversations: ✓
- POST /api/chat/conversations: ✓
- GET /api/chat/conversations/{id}/messages: ✓
- POST /api/chat/conversations/{id}/messages: ✓

### E. Acceptance Criteria Check
- [x/✗] 每项验收标准的检查结果

### F. Known Issues
- 列出任何已知问题或限制

### G. Next Steps
- 前端集成任务可以开始
```

## Completion Notes

(待执行后填写)
