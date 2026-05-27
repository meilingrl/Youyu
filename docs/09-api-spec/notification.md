# API Spec: notification

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/youyu/backend/controller/notification/NotificationController.java`
  - service: `backend/src/main/java/com/youyu/backend/service/notification/impl/NotificationServiceImpl.java`
  - request sample: `docs/06-http/notification.http`
- Last updated: 2026-05-25

## Scope

This document covers authenticated user notification endpoints under `/api/notifications`.

## Authentication

All endpoints require a logged-in user with `USER` role.

## Response Envelope

All endpoints use the shared `ApiResponse` envelope with `success`, `code`, `message`, `data`, and `traceId`.

## Notification Shape

Notification objects include:

| Field | Type | Notes |
|---|---|---|
| `id` | number | Notification ID |
| `userId` | number | Recipient user ID |
| `type` | string | `order_status`, `review_reminder`, or `system` |
| `title` | string | Display title |
| `body` | string | Display body |
| `actionUrl` | string | Optional app route to open |
| `isRead` | boolean | Read state |
| `createdAt` | string | Creation time |

## Endpoints

### `GET /api/notifications`

Lists notifications for the current user.

Query:
- `page`: optional, default `0`
- `size`: optional, default `20`, capped by service

Response `data`:
- `content`: notification array
- `page`
- `size`
- `total`
- `totalPages`

### `GET /api/notifications/unread-count`

Returns unread notification count for the current user.

Response:

```json
{
  "count": 3
}
```

### `POST /api/notifications/{id}/read`

Marks a single notification as read.

Rules:
- The notification must belong to the current user.

### `POST /api/notifications/read-all`

Marks all notifications for the current user as read.

## Business Integration

Order state transitions create `order_status` notifications for the buyer and, where relevant, the seller. Notification delivery is best-effort and must not block the order or payment state transition.

## Error Cases

- `401`: not logged in
- `403`: notification belongs to another user
- `404`: notification not found
