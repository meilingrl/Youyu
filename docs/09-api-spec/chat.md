# API Spec: chat

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/youyu/backend/controller/chat/ChatController.java`
  - service: `backend/src/main/java/com/youyu/backend/service/chat/impl/ChatServiceImpl.java`
  - request sample: `docs/06-http/chat.http`
- Last updated: 2026-05-30
- Admin console:
  - controller: `backend/src/main/java/com/youyu/backend/controller/chat/AdminSupportChatController.java`
  - service: `backend/src/main/java/com/youyu/backend/service/chat/impl/SupportConsoleServiceImpl.java`

## Scope

This document covers authenticated user chat endpoints under `/api/chat`.

## Authentication

All endpoints require a logged-in user with `USER` role.

## Response Envelope

All endpoints use the shared `ApiResponse` envelope with `success`, `code`, `message`, `data`, and `traceId`.

## Endpoints

### `GET /api/chat/conversations`

Lists conversations for the current user.

Query:
- `page`: optional, default `0`
- `size`: optional, default `20`, capped by service

Response `data.content[]` includes:
- `id`
- `type`
- `productId`
- `shopId`
- `peerUser`
- `unreadCount`
- `isPinned`
- `isMuted`
- `lastMessage`
- `lastMessagePreview`
- `lastMessageType`
- `lastMessageAt`
- `createdAt`

### `POST /api/chat/conversations`

Finds or creates a conversation with another user.

Body:
- `peerUserId`: required
- `productId`: optional
- `shopId`: optional

### `GET /api/chat/conversations/{id}/messages`

Lists messages for a conversation. The current user must be a participant.

Query:
- `page`: optional, default `0`
- `size`: optional, default `20`, capped by service

Response `data.content[]` includes:
- `id`
- `conversationId`
- `senderUserId`
- `body`
- `messageType`: `text` or `image`
- `mediaUrl`
- `productId`
- `orderId`
- `product`
- `order`
- `isRead`
- `readAt`
- `isRecalled`
- `recalledAt`
- `createdAt`

### `POST /api/chat/conversations/{id}/messages`

Sends a message in a conversation.

Body:
- `body`: required for `text`; optional caption for `image`
- `messageType`: optional, default `text`; supported values are `text`, `image`, `product_card`, `order_card`
- `mediaUrl`: required for `image`
- `productId`: required for `product_card`
- `orderId`: required for `order_card`

Validation:
- Text messages require non-empty `body`.
- Image messages require `mediaUrl`.
- `mediaUrl` may start with `http://`, `https://`, or `data:image/`.
- Regular URL length is capped at 512 characters.
- Data-image payload length is capped to support the current 5MB frontend Base64 fallback.
- Product cards require an existing on-sale product.
- Order cards require the current user to be the buyer or seller of the order.

Side effects:
- Updates conversation `lastMessageAt`.
- Increments the recipient's unread count.
- May create an automatic reply message if the recipient enabled auto-reply and the conversation has not received one in the last 24 hours.

### `GET /api/chat/messages/search`

Searches messages in conversations visible to the current user.

Query:
- `keyword`: optional; matches message `body` with a case-insensitive `LIKE`
- `startTime`: optional ISO-like date-time
- `endTime`: optional ISO-like date-time
- `page`: optional, default `0`
- `size`: optional, default `20`, capped by service

Response `data`:
- `content`: message rows with the same message fields as conversation message listing
- `total`
- `totalElements`
- `totalPages`
- `page`
- `number`
- `size`

Notes:
- Results are newest first.
- Conversations soft-deleted by the current user are excluded from search.

### `POST /api/chat/conversations/{id}/pin`

Pins or unpins a conversation for the current user.

Body:
- `pinned`: boolean

Side effects:
- `GET /api/chat/conversations` sorts pinned conversations before unpinned conversations.

### `POST /api/chat/conversations/{id}/mute`

Mutes or unmutes a conversation for the current user.

Body:
- `muted`: boolean

Side effects:
- Muted conversations remain visible.
- Muted conversations do not contribute to the unread total returned by `GET /api/chat/unread-count`.

### `DELETE /api/chat/conversations/{id}`

Soft-deletes a conversation for the current user.

Side effects:
- Clears the current user's unread count for that conversation.
- Removes the conversation from the current user's conversation list and message search.
- Does not remove the conversation for the peer user.

### `POST /api/chat/messages/{id}/recall`

Recalls a message sent by the current user.

Rules:
- The current user must be the sender.
- The message must not already be recalled.
- The message must have been created within the 2-minute recall window.

Side effects:
- Sets `isRecalled` and `recalledAt`.
- Message content remains stored for audit and conflict handling, but the frontend renders a recalled placeholder.

### `GET /api/chat/auto-reply`

Returns auto-reply settings for the current user.

Response `data`:
- `isEnabled`
- `replyContent`
- `updatedAt`

If no row exists, returns disabled default settings.

### `PUT /api/chat/auto-reply`

Creates or updates auto-reply settings for the current user.

Body:
- `isEnabled`: boolean
- `replyContent`: required, trimmed, max 500 characters

### `GET /api/chat/quick-replies`

Lists quick replies owned by the current user.

Ordering:
- `sortOrder` ascending
- `createdAt` ascending

Response `data[]` includes:
- `id`
- `userId`
- `content`
- `sortOrder`
- `createdAt`
- `updatedAt`

### `POST /api/chat/quick-replies`

Creates a quick reply for the current user.

Body:
- `content`: required, trimmed, max 500 characters
- `sortOrder`: optional, default `0`

Response:

```json
{
  "id": 1
}
```

### `PUT /api/chat/quick-replies/{id}`

Updates a quick reply. The current user must own the quick reply.

Body:
- `content`: required, trimmed, max 500 characters
- `sortOrder`: optional, default `0`

### `DELETE /api/chat/quick-replies/{id}`

Deletes a quick reply. The current user must own the quick reply.

### `GET /api/chat/unread-count`

Returns total unread chat messages for the current user.

Response:

```json
{
  "count": 5
}
```

### `POST /api/chat/conversations/{id}/read`

Marks a conversation as read for the current user.

Side effects:
- Marks messages sent by the peer as read.
- Clears the current user's unread count for the conversation.

## Online Customer Service

Support conversations reuse the chat tables with `type = 'support'`. The peer is always the platform CS account (`platform_cs`, seeded on startup if missing). The `support_status` field drives behavior:

**Legacy databases:** `ChatSupportSchemaUpgrader` runs on application startup and additively applies missing chat columns (`support_status`, `assigned_admin_id`, `unread_count_*`, `message_type`, `is_recalled`, etc.) to pre-existing MySQL tables created before this feature. Manual reference: `database/002_support_chat_columns.sql`. `CREATE TABLE IF NOT EXISTS` in `schema.sql` alone does not alter existing tables.

- `ai`: rule-based FAQ bot auto-replies as the platform CS account to user messages.
- `pending`: escalated to a human; the AI stops; the session enters the admin queue.
- `human`: an admin has claimed the session and replies as the platform CS account.
- `closed`: the session is ended.

### `POST /api/chat/support/session`

Starts (or reuses) the current user's support conversation with the platform CS account. Idempotent: an existing conversation is returned; a closed one is reset to `ai`. A first-time session receives an automated greeting.

Response `data` is a conversation object that additionally includes `supportStatus` and `assignedAdminId`.

### `POST /api/chat/conversations/{id}/escalate`

Escalates a support conversation to a human (`ai`/... → `pending`). The current user must be the requester. Inserts a system note from the platform CS account. After escalation the AI no longer auto-replies.

### `POST /api/chat/conversations/{id}/close-support`

Ends the current user's support session (`→ closed`), clears `assigned_admin_id`, and inserts a closing note from the platform CS account. Only the requester (`user_a`) may call this endpoint. While `closed`, the user cannot send messages; `POST /api/chat/support/session` reopens the same conversation (`→ ai`) for a new round of consultation (one durable conversation row per user, by design).

Deleting a support conversation from the message list also ends the session before soft-deleting it for the user.

### Admin online CS console — `/api/admin/support/chat/*`

All endpoints require an admin session with permission `ADMIN_SUPPORT_TICKETS_HANDLE`.

- `GET /api/admin/support/chat/conversations?filter=&page=&size=` — queue list. `filter` ∈ `pending` (待接入), `active` (进行中), `mine` (我处理的), `closed` (已结束). Response `data.content[]` items include `id`, `supportStatus`, `assignedAdminId`, `assignedAdmin`, `requester`, `unreadCount`, `lastMessagePreview`, `lastMessageAt`; `data.counts` carries per-filter totals.
- `GET /api/admin/support/chat/conversations/{id}` — conversation meta + requester context.
- `GET /api/admin/support/chat/conversations/{id}/messages?page=&size=` — message page.
- `POST /api/admin/support/chat/conversations/{id}/claim` — claim/assign to current admin (`→ human`). Rejects if already claimed by another admin.
- `POST /api/admin/support/chat/conversations/{id}/messages` — reply as the platform CS account. Body `{ "body": "..." }`.
- `POST /api/admin/support/chat/conversations/{id}/close` — end the session (`→ closed`).
- `POST /api/admin/support/chat/conversations/{id}/read` — clear the admin-side unread count.

## Error Cases

- `400`: invalid payload, unsupported message type, empty text body, invalid media URL, invalid quick reply content, escalating/closing a non-support or closed session
- `401`: not logged in
- `403`: current user is not a conversation participant or not the quick reply owner; admin lacks `ADMIN_SUPPORT_TICKETS_HANDLE`; support session claimed by another admin
- `404`: conversation or quick reply not found
