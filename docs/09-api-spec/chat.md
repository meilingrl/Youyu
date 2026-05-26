# API Spec: chat

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/youyu/backend/controller/chat/ChatController.java`
  - service: `backend/src/main/java/com/youyu/backend/service/chat/impl/ChatServiceImpl.java`
  - request sample: `docs/06-http/chat.http`
- Last updated: 2026-05-25

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
- `isRead`
- `readAt`
- `createdAt`

### `POST /api/chat/conversations/{id}/messages`

Sends a message in a conversation.

Body:
- `body`: required for `text`; optional caption for `image`
- `messageType`: optional, default `text`; supported values are `text`, `image`
- `mediaUrl`: required for `image`

Validation:
- Text messages require non-empty `body`.
- Image messages require `mediaUrl`.
- `mediaUrl` may start with `http://`, `https://`, or `data:image/`.
- Regular URL length is capped at 512 characters.
- Data-image payload length is capped to support the current 5MB frontend Base64 fallback.

Side effects:
- Updates conversation `lastMessageAt`.
- Increments the recipient's unread count.

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

- `400`: invalid payload, unsupported message type, empty text body, invalid media URL
- `401`: not logged in
- `403`: current user is not a conversation participant
- `404`: conversation not found
