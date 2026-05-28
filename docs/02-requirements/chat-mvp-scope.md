# Chat MVP Scope

## 1. Purpose

本文档恢复当前聊天 MVP 的正式需求边界，用作后续 support console、platform mediation 和 admin module 工作的依赖事实源。

本文档不是新的实现任务，不要求修改代码、schema、seed、API 或前端运行时。当前事实以 runtime code、schema、API spec 和 HTTP smoke collection 为准；archived chat task 只作为历史参考。

## 2. Runtime Truth Sources

本次恢复核对过以下当前事实源：

- `backend/src/main/resources/schema.sql`
- `backend/src/main/java/com/youyu/backend/controller/chat/ChatController.java`
- `backend/src/main/java/com/youyu/backend/controller/chat/QuickReplyController.java`
- `backend/src/main/java/com/youyu/backend/service/chat/ChatService.java`
- `backend/src/main/java/com/youyu/backend/service/chat/impl/ChatServiceImpl.java`
- `backend/src/main/java/com/youyu/backend/mapper/chat/impl/JdbcChatConversationMapper.java`
- `backend/src/main/java/com/youyu/backend/mapper/chat/impl/JdbcChatMessageMapper.java`
- `frontend/src/api/modules/chat.js`
- `frontend/src/stores/chat.js`
- `frontend/src/views/app/MessagesView.vue`
- `frontend/src/views/app/ProductDetailView.vue`
- `frontend/src/views/app/ShopView.vue`
- `docs/09-api-spec/chat.md`
- `docs/06-http/chat.http`
- `backend/src/main/java/com/youyu/backend/controller/notification/NotificationController.java`
- `backend/src/main/java/com/youyu/backend/service/notification/impl/NotificationServiceImpl.java`
- `docs/09-api-spec/notification.md`
- `docs/06-http/notification.http`
- `docs/02-requirements/admin-support-console-scope.md`

## 3. Chat MVP Owns

Chat MVP owns authenticated user-to-user marketplace messaging under `/api/chat/**` and the front-office message center under `/app/messages`.

Current implemented scope:

1. One-to-one conversations between two users.
2. Conversation creation by `(peerUserId, productId?, shopId?)`.
3. Conversation types: `direct`, `product_inquiry`, `shop_inquiry`.
4. Conversation list for the current participant, including peer user summary, last-message preview, unread count, pinned state, muted state, and last activity time.
5. Message list for a participant-scoped conversation.
6. Message send for `text`, `image`, `product_card`, and `order_card`.
7. Product-card messages that reference an existing on-sale product and return a product summary.
8. Order-card messages that require the sender to be the buyer or seller of the order and return an order summary.
9. Unread count, mark-as-read behavior, and per-user muted unread suppression.
10. Message search across conversations visible to the current user, excluding conversations soft-deleted by that user.
11. Per-user conversation pin, mute, and soft-delete.
12. Sender-only message recall inside the current 2-minute recall window.
13. User-owned quick replies.
14. User-owned auto-reply settings and automatic reply insertion with 24-hour per-conversation throttling.
15. Frontend polling for active conversation refresh. Current store default is 8 seconds.

Chat MVP may reference products, shops, and orders as communication context. Those references do not transfer ownership of product governance, shop governance, order fulfillment, refund handling, or report processing into chat.

## 4. Data Entities

Current chat-owned tables:

| Table | Ownership | Key fields |
|---|---|---|
| `chat_conversations` | chat conversation membership and per-user conversation state | `id`, `type`, `product_id`, `shop_id`, `user_a_id`, `user_b_id`, `unread_count_a`, `unread_count_b`, `is_pinned_a`, `is_pinned_b`, `is_muted_a`, `is_muted_b`, `deleted_by_a_at`, `deleted_by_b_at`, `auto_replied_to_a_at`, `auto_replied_to_b_at`, `last_message_at`, `created_at` |
| `chat_messages` | chat message timeline | `id`, `conversation_id`, `sender_user_id`, `body`, `is_read`, `read_at`, `message_type`, `media_url`, `product_id`, `order_id`, `is_recalled`, `recalled_at`, `created_at` |
| `quick_replies` | user-owned reusable reply text | `id`, `user_id`, `content`, `sort_order`, `created_at`, `updated_at` |
| `auto_reply_settings` | user-owned auto-reply settings | `id`, `user_id`, `is_enabled`, `reply_content`, `created_at`, `updated_at` |

Related but not chat-owned:

| Table | Owner | Relationship to chat |
|---|---|---|
| `notifications` | notification module | May be surfaced near the message center UI, but is not chat message persistence. |
| `orders`, `order_items`, `refund_records` | order/refund modules | Order-card messages may reference orders. Order state and refund decisions stay outside chat. |
| `reports` | report governance module | Reports may later be used to locate dispute context. Report handling stays outside chat. |

## 5. API Surface

Current chat API is participant-scoped. It does not provide admin browse, admin send, or support-agent participation endpoints.

| Method | Path | Owned behavior |
|---|---|---|
| `GET` | `/api/chat/conversations` | List conversations visible to the current participant. |
| `POST` | `/api/chat/conversations` | Find or create a one-to-one conversation. |
| `GET` | `/api/chat/conversations/{id}/messages` | List messages for a participant-scoped conversation. |
| `POST` | `/api/chat/conversations/{id}/messages` | Send text, image, product-card, or order-card message. |
| `GET` | `/api/chat/messages/search` | Search messages visible to the current participant. |
| `GET` | `/api/chat/unread-count` | Return total unread chat count for the current participant. |
| `POST` | `/api/chat/conversations/{id}/read` | Mark a conversation read for the current participant. |
| `POST` | `/api/chat/conversations/{id}/pin` | Pin or unpin a conversation for the current participant. |
| `POST` | `/api/chat/conversations/{id}/mute` | Mute or unmute a conversation for the current participant. |
| `DELETE` | `/api/chat/conversations/{id}` | Soft-delete a conversation for the current participant. |
| `POST` | `/api/chat/messages/{id}/recall` | Recall the current user's own message within the allowed window. |
| `GET` | `/api/chat/auto-reply` | Read current user's auto-reply settings. |
| `PUT` | `/api/chat/auto-reply` | Update current user's auto-reply settings. |
| `GET` | `/api/chat/quick-replies` | List current user's quick replies. |
| `POST` | `/api/chat/quick-replies` | Create current user's quick reply. |
| `PUT` | `/api/chat/quick-replies/{id}` | Update current user's quick reply. |
| `DELETE` | `/api/chat/quick-replies/{id}` | Delete current user's quick reply. |

## 6. Frontend Surfaces

Current user-facing chat surfaces:

- `/app/messages`: message center with conversation list, conversation detail, message search, notifications category embedding, and mobile list/detail behavior.
- Product detail page: `联系卖家` creates or opens a product-seeded conversation; `分享到聊天` sends a product-card message to an existing conversation.
- Shop detail page: `联系店主` creates or opens a shop-seeded conversation; `消息入口` opens the message center with shop context.
- Order-related contact flows may send order-card messages when the user is an order participant.

The message center is not an admin console. It must not expose admin mediation, support assignment, report processing, or admin conversation participation.

## 7. Performance And Transport Boundary

Current MVP transport is polling, not WebSocket or SSE.

Requirements:

1. Active conversation polling must not run more frequently than once every 5 seconds.
2. Current frontend default is 8 seconds for active conversation polling.
3. Polling must stop when leaving the active conversation view.
4. Conversation list page size is capped at 50 by service behavior.
5. Message list and search page size are capped at 100 by service behavior.

WebSocket, SSE, push delivery, full-text search infrastructure, and object-storage-backed media upload are outside current MVP ownership unless a future task explicitly changes the contract.

## 8. Non-Goals

Chat MVP does not own:

1. Admin chat endpoints.
2. Admin participation in buyer/seller conversations.
3. Three-party support chat.
4. Group chat or group governance.
5. Support-ticket persistence, assignment, SLA, internal notes, or disposition workflow.
6. Platform mediation case creation, evidence review, decision recording, appeals, or enforcement.
7. Report processing or report-to-mediation escalation.
8. Order fulfillment, refund approval, or after-sales decision records.
9. Abnormal-message detection, automated moderation, or admin message-risk queues.
10. Notification composition by admins.

## 9. Responsibility Separation

| Area | Owns | Does not own |
|---|---|---|
| Chat MVP | User-to-user conversations, participant-scoped messages, message center UX, chat-specific per-user states. | Admin participation, mediation decisions, support-ticket workflow, report/order/refund state changes. |
| Support console | Admin context gathering, triage links, existing report/order/user/shop/product/search context summaries. | Calling `/api/chat/**`, sending user messages, becoming a chat participant, deciding disputes. |
| Platform mediation | Formal dispute case handling, report escalation, decision records, evidence references, enforcement expectations. | General user chat, support-ticket workflow, free-form admin messaging inside buyer/seller conversations. |

Support console may link operators to existing owner pages and summarize existing admin-owned records. It must not become a hidden chat owner.

## 10. Mediation V1 Chat Visibility Rule

Future mediation v1 may provide admins read-only visibility into buyer/seller chat context only when that context is directly related to a mediation case, source report, or disputed order.

Hard constraints for mediation v1:

1. Admins must not join user conversations.
2. Admins must not send messages into buyer/seller conversations.
3. Admins must not create, pin, mute, soft-delete, recall, mark read, or otherwise mutate user chat state.
4. Admin-visible chat context must be scoped to the related dispute context, not global chat browsing.
5. Any future admin read endpoint should be defined under the mediation/admin contract, not by reusing `/api/chat/**` as an admin surface.
6. If redaction or audit requirements are added, platform mediation owns that contract; chat MVP remains the source of the underlying conversation records.

This rule unblocks the platform mediation boundary task from the missing chat-scope-document blocker, but it does not implement mediation behavior.

## 11. Code Vs Archived-Doc Conflicts

Runtime code/API/spec/http wins over archived task text. The following historical statements are now outdated:

| Archived statement | Current runtime truth |
|---|---|
| Early chat scope deferred unread counts and last-message summaries. | Unread counts, read marking, last-message preview/type, and muted unread suppression are implemented. |
| Early chat scope was text-only. | Runtime supports `text`, `image`, `product_card`, and `order_card`. |
| Early chat scope deferred image messages. | Image messages are implemented with URL/data-image validation and frontend preview. |
| Early chat scope deferred message search. | `GET /api/chat/messages/search` is implemented. |
| Early chat scope deferred conversation management. | Pin, mute, and per-user soft-delete are implemented. |
| Early chat scope deferred message recall. | Sender-only recall is implemented with a 2-minute window. |
| Early chat scope deferred auto-reply and quick replies. | Quick replies and auto-reply settings/trigger behavior are implemented. |
| UI support shell treated chat backend as absent. | Chat backend and frontend integration now exist; `/admin/support` remains separate and must not call user chat endpoints. |

No conflict was found that requires changing runtime code as part of this recovery task.
