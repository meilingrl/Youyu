# API Spec: chat

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/youyu/backend/controller/chat/ChatController.java`
  - service: `backend/src/main/java/com/youyu/backend/service/chat/impl/ChatServiceImpl.java`
  - request sample: `docs/06-http/chat.http`
- Last updated: 2026-05-27

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

## Error Cases

- `400`: invalid payload, unsupported message type, empty text body, invalid media URL, invalid quick reply content
- `401`: not logged in
- `403`: current user is not a conversation participant or not the quick reply owner
- `404`: conversation or quick reply not found
