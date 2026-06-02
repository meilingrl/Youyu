# API Spec: notification

## Document Info

- Status: active
- Source of truth:
  - controller: `backend/src/main/java/com/youyu/backend/controller/notification/NotificationController.java`
  - service: `backend/src/main/java/com/youyu/backend/service/notification/impl/NotificationServiceImpl.java`
  - request sample: `docs/06-http/notification.http`
- Last updated: 2026-06-02

## Scope

This document covers authenticated user notification endpoints under `/api/notifications`
and the platform-admin system-notification publishing endpoint under `/api/admin/notifications`.

## Authentication

All endpoints require a logged-in user with `USER` role.

`POST /api/admin/notifications` requires an admin role with
`ADMIN_NOTIFICATIONS_PUBLISH`. The current policy grants this permission only to
`ADMIN` and `SUPER_ADMIN`.

## Response Envelope

All endpoints use the shared `ApiResponse` envelope with `success`, `code`, `message`, `data`, and `traceId`.

## Notification Shape

Notification objects include:

| Field | Type | Notes |
|---|---|---|
| `id` | number | Notification ID |
| `userId` | number | Recipient user ID |
| `type` | string | `order_status`, `support_ticket`, `mediation_update`, `review_reminder`, or `system` |
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

### `POST /api/admin/notifications`

Publishes one system notification to all active ordinary users.

Request:

```json
{
  "title": "平台维护提醒",
  "body": "今晚 23:00 至 23:30 将进行短时维护。",
  "actionUrl": "/app/home"
}
```

Rules:
- `title` and `body` are required.
- `actionUrl` is optional and should use an in-app route when supplied.
- Recipients are users with `status=active` and `role=USER`.
- Admin and specialist-admin accounts are not recipients.
- Each publish action records `SYSTEM_NOTIFICATION_PUBLISH` in
  `admin_audit_logs`. The aggregate broadcast uses `targetType=NOTIFICATION`
  and `targetId=0`.

Response `data`:
- `type`: always `system`
- `title`
- `body`
- `actionUrl`
- `recipientCount`

## Business Integration

Order state transitions create `order_status` notifications for the buyer and, where relevant, the seller. Notification delivery is best-effort and must not block the order or payment state transition.

Wave 1 also uses the same notification lane for:

- `support_ticket`
  - emitted when platform support posts a public reply or changes ticket status
  - `actionUrl` targets the durable support-ticket page, currently `/app/support?ticketId={ticketId}`
- `mediation_update`
  - emitted when an order-backed report enters formal mediation, when mediation status changes, and when a final mediation decision is recorded
  - `actionUrl` targets the user-facing order detail page, currently `/app/orders/{orderId}`

## Error Cases

- `401`: not logged in
- `403`: notification belongs to another user
- `403`: current admin role lacks `ADMIN_NOTIFICATIONS_PUBLISH`
- `404`: notification not found
