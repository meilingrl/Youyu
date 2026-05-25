# Task: Chat MVP Frontend Integration

## Metadata

- ID: chat-mvp-frontend-integration
- Status: draft
- Owner: unassigned
- Track: feature
- Depends on: `chat-mvp-backend-implementation` (必须先完成)
- Priority: high
- Planned date: 2026-05-25
- Completed date:

## Objective

将消息功能前端与后端 API 集成,移除占位数据,启用真实的消息发送和接收功能。包括实现 Pinia store、API 模块、轮询逻辑,以及在商品/店铺详情页添加"联系卖家"入口。

## Background

当前 `MessagesView.vue` 使用硬编码的占位数据 (`placeholderConversations`),发送消息功能被禁用。后端 API 已实现(任务2),现在需要前端接入真实数据。

**前置条件:**
- 后端 4 个 API 端点已实现并通过测试
- 数据库表已创建
- HTTP smoke test 已验证

## API Endpoints (from Backend)

### 1. GET /api/chat/conversations
获取会话列表,分页返回。

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

### 2. POST /api/chat/conversations
查找或创建会话(幂等)。

**请求:**
```json
{
  "peerUserId": 2,
  "productId": 123,
  "shopId": 45
}
```

### 3. GET /api/chat/conversations/{id}/messages
获取消息列表,分页返回,按时间倒序。

### 4. POST /api/chat/conversations/{id}/messages
发送消息。

**请求:**
```json
{
  "body": "您好,请问教材退款进度如何?"
}
```

## Frontend Architecture

### Store Design (`stores/chat.js`)

```javascript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as chatApi from '@/api/modules/chat'

export const useChatStore = defineStore('chat', () => {
  // State
  const conversations = ref([])
  const activeConversationId = ref(null)
  const messages = ref([])
  const loading = ref(false)
  const sending = ref(false)
  const pollingTimer = ref(null)
  
  // Computed
  const activeConversation = computed(() => {
    if (!activeConversationId.value) return null
    return conversations.value.find(c => c.id === activeConversationId.value)
  })
  
  // Actions
  async function fetchConversations(page = 0, size = 20) {
    loading.value = true
    try {
      const response = await chatApi.getConversations({ page, size })
      conversations.value = response.content
      return response
    } catch (error) {
      console.error('Failed to fetch conversations:', error)
      throw error
    } finally {
      loading.value = false
    }
  }
  
  async function findOrCreateConversation(peerUserId, productId, shopId) {
    loading.value = true
    try {
      const conversation = await chatApi.createConversation({
        peerUserId,
        productId,
        shopId
      })
      // 添加到列表(如果不存在)
      const exists = conversations.value.find(c => c.id === conversation.id)
      if (!exists) {
        conversations.value.unshift(conversation)
      }
      activeConversationId.value = conversation.id
      await fetchMessages(conversation.id)
      return conversation
    } catch (error) {
      console.error('Failed to create conversation:', error)
      throw error
    } finally {
      loading.value = false
    }
  }
  
  async function fetchMessages(conversationId, page = 0, size = 50) {
    loading.value = true
    try {
      const response = await chatApi.getMessages(conversationId, { page, size })
      // 后端返回倒序,前端需要正序显示
      messages.value = response.content.reverse()
      return response
    } catch (error) {
      console.error('Failed to fetch messages:', error)
      throw error
    } finally {
      loading.value = false
    }
  }
  
  async function sendMessage(conversationId, body) {
    if (!body.trim()) return
    
    sending.value = true
    try {
      const message = await chatApi.sendMessage(conversationId, { body })
      // 添加到消息列表
      messages.value.push(message)
      // 更新会话的 lastMessageAt
      const conv = conversations.value.find(c => c.id === conversationId)
      if (conv) {
        conv.lastMessageAt = message.createdAt
      }
      return message
    } catch (error) {
      console.error('Failed to send message:', error)
      throw error
    } finally {
      sending.value = false
    }
  }
  
  // 轮询逻辑
  function startPolling(conversationId, interval = 8000) {
    stopPolling()
    pollingTimer.value = setInterval(async () => {
      try {
        const response = await chatApi.getMessages(conversationId, { page: 0, size: 50 })
        const newMessages = response.content.reverse()
        // 只更新新消息(避免闪烁)
        if (newMessages.length > messages.value.length) {
          messages.value = newMessages
        }
      } catch (error) {
        console.error('Polling failed:', error)
      }
    }, interval)
  }
  
  function stopPolling() {
    if (pollingTimer.value) {
      clearInterval(pollingTimer.value)
      pollingTimer.value = null
    }
  }
  
  // 清理
  function $reset() {
    conversations.value = []
    activeConversationId.value = null
    messages.value = []
    loading.value = false
    sending.value = false
    stopPolling()
  }
  
  return {
    // State
    conversations,
    activeConversationId,
    messages,
    loading,
    sending,
    
    // Computed
    activeConversation,
    
    // Actions
    fetchConversations,
    findOrCreateConversation,
    fetchMessages,
    sendMessage,
    startPolling,
    stopPolling,
    $reset
  }
})
```

### API Module (`api/modules/chat.js`)

```javascript
import service from '@/api/client'

/**
 * 获取会话列表
 */
export async function getConversations(params) {
  return service.get('/chat/conversations', { params })
}

/**
 * 查找或创建会话
 */
export async function createConversation(data) {
  return service.post('/chat/conversations', data)
}

/**
 * 获取消息列表
 */
export async function getMessages(conversationId, params) {
  return service.get(`/chat/conversations/${conversationId}/messages`, { params })
}

/**
 * 发送消息
 */
export async function sendMessage(conversationId, data) {
  return service.post(`/chat/conversations/${conversationId}/messages`, data)
}
```

### API Index Export (`api/index.js`)

在现有的 `api/index.js` 中添加:

```javascript
export * as chat from './modules/chat'
```

## Files to Read

- `frontend/src/views/app/MessagesView.vue` (当前实现)
- `frontend/src/stores/auth.js` (参考 store 模式)
- `frontend/src/stores/market.js` (参考 store 模式)
- `frontend/src/api/modules/product.js` (参考 API 模式)
- `frontend/src/api/client.js` (Axios 实例)
- `frontend/src/views/app/ProductDetailView.vue` (添加入口)
- `frontend/src/views/app/ShopDetailView.vue` (添加入口)

## In Scope

### 1. API 模块
- 创建 `api/modules/chat.js`
- 在 `api/index.js` 中导出

### 2. Pinia Store
- 创建 `stores/chat.js`
- 实现会话列表、消息列表、发送消息、轮询逻辑

### 3. MessagesView 改造
- 移除 `placeholderConversations` 硬编码数据
- 接入 `useChatStore`
- 实现真实的会话切换和消息加载
- 启用发送消息功能
- 添加轮询逻辑(进入会话时启动,离开时停止)
- 添加加载状态和错误处理

### 4. 商品详情页入口
- 在 `ProductDetailView.vue` 添加"联系卖家"按钮
- 点击后:
  - 调用 `findOrCreateConversation(sellerId, productId, null)`
  - 跳转到 `/app/messages`

### 5. 店铺详情页入口
- 在 `ShopDetailView.vue` 添加"联系店主"按钮
- 点击后:
  - 调用 `findOrCreateConversation(ownerId, null, shopId)`
  - 跳转到 `/app/messages`

### 6. 单元测试
- `stores/chat.spec.js` (基础 store 测试)

## Out of Scope

- ❌ 未读数统计(后端未实现)
- ❌ 最后消息摘要(后端未实现)
- ❌ WebSocket 实时推送
- ❌ 图片/文件上传
- ❌ 消息搜索
- ❌ 消息删除/撤回
- ❌ 管理端支持页面改造

## Hard Limits

- **不修改**: 后端任何文件
- **不修改**: 路由配置(保持现有路由结构)
- **不引入**: 新的第三方依赖
- **不实现**: 超出 MVP 范围的功能

## Allowed Changes

- `frontend/src/api/modules/chat.js` (新建)
- `frontend/src/api/index.js` (添加导出)
- `frontend/src/stores/chat.js` (新建)
- `frontend/src/views/app/MessagesView.vue` (改造)
- `frontend/src/views/app/ProductDetailView.vue` (添加按钮)
- `frontend/src/views/app/ShopDetailView.vue` (添加按钮)
- `frontend/src/stores/chat.spec.js` (新建,可选)
- `CHANGELOG.md`

## Implementation Steps

### Step 1: API 模块
1. 创建 `api/modules/chat.js`
2. 实现 4 个 API 函数
3. 在 `api/index.js` 中添加导出

### Step 2: Pinia Store
1. 创建 `stores/chat.js`
2. 实现状态管理:
   - `conversations`, `activeConversationId`, `messages`
   - `loading`, `sending`
3. 实现 Actions:
   - `fetchConversations`
   - `findOrCreateConversation`
   - `fetchMessages`
   - `sendMessage`
   - `startPolling`, `stopPolling`

### Step 3: MessagesView 改造
1. 移除 `placeholderConversations` 等硬编码数据
2. 导入 `useChatStore`
3. 在 `onMounted` 中调用 `fetchConversations`
4. 修改 `openConversation`:
   - 调用 `fetchMessages(conversationId)`
   - 调用 `startPolling(conversationId)`
5. 修改 `backToList`:
   - 调用 `stopPolling()`
6. 启用发送消息:
   - 移除 `disabled` 属性
   - 绑定 `@click` 到 `handleSendMessage`
   - 实现 `handleSendMessage` 函数
7. 添加加载状态:
   - 会话列表加载骨架屏
   - 消息列表加载骨架屏
   - 发送按钮加载状态
8. 添加错误处理:
   - 使用 `ElMessage` 显示错误提示

### Step 4: 商品详情页入口
1. 读取 `ProductDetailView.vue`
2. 找到合适的位置添加"联系卖家"按钮
3. 实现点击逻辑:
   ```javascript
   import { useChatStore } from '@/stores/chat'
   import { useRouter } from 'vue-router'
   
   const chatStore = useChatStore()
   const router = useRouter()
   
   async function handleContactSeller() {
     try {
       await chatStore.findOrCreateConversation(
         product.sellerId,
         product.id,
         null
       )
       router.push('/app/messages')
     } catch (error) {
       ElMessage.error('无法发起会话')
     }
   }
   ```

### Step 5: 店铺详情页入口
1. 读取 `ShopDetailView.vue`
2. 找到合适的位置添加"联系店主"按钮
3. 实现点击逻辑(类似商品详情页)

### Step 6: 测试验证
1. 启动后端: `mvnw.cmd spring-boot:run`
2. 启动前端: `npm run dev`
3. 测试流程:
   - 登录用户
   - 访问商品详情页,点击"联系卖家"
   - 验证跳转到消息页面并创建会话
   - 发送消息
   - 验证消息显示
   - 刷新页面,验证会话和消息持久化
   - 测试轮询(等待 8 秒,验证消息自动刷新)
4. 测试边界情况:
   - 空消息不能发送
   - 网络错误提示
   - 加载状态正确显示

### Step 7: 单元测试(可选)
1. 创建 `stores/chat.spec.js`
2. 测试 store actions

### Step 8: 文档更新
1. 更新 `CHANGELOG.md`

## Test Plan

### 功能测试
- [ ] 会话列表正确加载
- [ ] 点击会话正确加载消息
- [ ] 发送消息成功并显示
- [ ] 轮询正确工作(8秒刷新)
- [ ] 从商品详情页发起会话成功
- [ ] 从店铺详情页发起会话成功
- [ ] 重复创建会话返回现有会话(幂等性)
- [ ] 刷新页面后会话和消息持久化

### UI 测试
- [ ] 加载状态正确显示
- [ ] 发送按钮禁用状态正确
- [ ] 错误提示正确显示
- [ ] 空消息不能发送
- [ ] 消息自动滚动到底部

### 响应式测试
- [ ] 移动端列表/详情切换正常
- [ ] 桌面端双栏布局正常

### 集成测试
- [ ] 前后端联调成功
- [ ] 多用户会话隔离正确
- [ ] 权限校验正确(非参与者无法访问)

## Acceptance Criteria

- [ ] API 模块创建并导出
- [ ] Pinia store 实现并通过基础测试
- [ ] MessagesView 移除占位数据,接入真实 API
- [ ] 发送消息功能启用并正常工作
- [ ] 轮询逻辑正确(进入会话启动,离开停止)
- [ ] 商品详情页"联系卖家"按钮正常工作
- [ ] 店铺详情页"联系店主"按钮正常工作
- [ ] 加载状态和错误处理完善
- [ ] 前后端联调成功,无控制台错误
- [ ] 响应式布局正常
- [ ] `CHANGELOG.md` 已更新

## Documentation Updates Required

- [x] `CHANGELOG.md`

## Risks and Mitigations

### 风险1: 轮询性能
- **风险**: 8秒轮询可能导致大量请求
- **缓解**: 只在活跃会话时轮询,离开页面时停止

### 风险2: 消息顺序
- **风险**: 后端返回倒序,前端需要正序
- **缓解**: 在 store 中 reverse() 处理

### 风险3: 重复消息
- **风险**: 轮询可能导致消息重复显示
- **缓解**: 比较消息数量,只在有新消息时更新

### 风险4: 路由跳转
- **风险**: 从商品/店铺页跳转到消息页可能丢失上下文
- **缓解**: 在 store 中保存 activeConversationId,跳转后自动打开

## Dependencies

**必须先完成:**
- 任务2: `chat-mvp-backend-implementation`

**可并行:**
- 任务1: `messages-ux-redesign` (UI 优化可以在集成前或后进行)

## Final Report Format

```markdown
## Return Report — chat-mvp-frontend-integration

### A. Branch & Commit
- Branch: <name>
- Commit SHA: <sha>
- Files changed: (paste `git diff --stat`)

### B. Implementation Summary
- API 模块: ✓
- Pinia store: ✓
- MessagesView 改造: ✓
- 商品详情页入口: ✓
- 店铺详情页入口: ✓
- 单元测试: ✓/跳过

### C. Test Results
- 功能测试: [通过/失败]
- UI 测试: [通过/失败]
- 响应式测试: [通过/失败]
- 集成测试: [通过/失败]

### D. Integration Points
- 商品详情页按钮位置: <描述>
- 店铺详情页按钮位置: <描述>
- 轮询间隔: 8000ms

### E. Acceptance Criteria Check
- [x/✗] 每项验收标准的检查结果

### F. Known Issues
- 列出任何已知问题或限制

### G. Next Steps
- 建议的后续改进(如未读数统计、WebSocket 等)
```

## Completion Notes

(待执行后填写)
