# API Spec: support

## Document Info

- Status: active
- Source of truth:
  - requirement boundary: `docs/02-requirements/customer-service-ticket-scope.md`
  - shared response / error handling: `backend/src/main/java/com/youyu/backend/common/api/ApiResponse.java`
  - shared response / error handling: `backend/src/main/java/com/youyu/backend/controller/advice/GlobalExceptionHandler.java`
  - request sample: `docs/06-http/support.http`
  - admin request sample: `docs/06-http/admin.http`
  - related task: `docs/08-tasks/active/customer-service-ticket-mvp.md`
- Last updated: 2026-06-01

## Scope

This document covers the customer-service ticket MVP:

- authenticated user endpoints under `/api/support/tickets`;
- admin support-ticket endpoints under `/api/admin/support/tickets`;
- ticket fields, message fields, status transitions, role visibility, and error boundaries.

Support tickets are separate from buyer/seller chat, reports, order/refund handling, product/shop/user governance, and formal mediation. A support ticket may link to an order, product, shop, or user as context, but it does not mutate those owner records.

## Authentication And Roles

- User endpoints require an authenticated `USER`.
- Admin endpoints require an admin staff role with support-ticket access. Current MVP examples use `ADMIN`, `SUPER_ADMIN`, and `SUPPORT_AGENT`.
- Users may only list, view, and reply to their own tickets.
- Admins may list and view all support tickets, change ticket status, add public replies, and add internal notes.
- `/admin/support` remains the single admin route, but it now hosts two explicit workspaces:
  - support-ticket handling backed by `/api/admin/support/tickets/**`
  - online customer-service chat backed by `/api/admin/support/chat/**`
- The support-ticket workspace must not call `/api/chat/**` as its owner API. The online-CS workspace is documented separately in `docs/09-api-spec/chat.md`.

## Response Envelope

All endpoints use the unified response envelope:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Request succeeded",
  "data": {},
  "traceId": "..."
}
```

## Domain Model

### Ticket Status

| Status | Meaning | Allowed next statuses |
|---|---|---|
| `open` | User submitted a ticket and support has not taken it. | `in_progress`, `closed` |
| `in_progress` | A support agent is reviewing or replying. | `waiting_user`, `resolved`, `closed` |
| `waiting_user` | Support replied and is waiting for user input. | `in_progress`, `resolved`, `closed` |
| `resolved` | Support considers the issue handled. | `closed` |
| `closed` | Terminal closed state. | none |

Closed tickets reject further public replies, internal notes, and status changes except idempotent handling if the backend explicitly supports it.

### Ticket Categories

Supported category values:

- `account`
- `order`
- `product`
- `shop`
- `payment`
- `report`
- `other`

Categories route support work only. They do not transfer ownership of linked business records.

### Priority

The MVP default priority is `normal`. If the backend accepts a priority field, the accepted set should remain bounded and documented before UI expansion. Unknown priority values must be rejected or normalized consistently by the backend.

### Related Context

| Field | Type | Notes |
|---|---|---|
| `relatedType` | string or null | Optional context owner. Expected values are `order`, `product`, `shop`, `user`, `report`, or `other` when present. |
| `relatedId` | number or null | Optional owner-record ID. It is context only and does not authorize state mutation in the owner module. |

## Shared Field Shapes

### Ticket Summary

| Field | Type | Notes |
|---|---|---|
| `id` | number | Internal ticket ID. |
| `ticketNo` | string | Stable human-readable ticket number. |
| `requesterUserId` | number | User who created the ticket. |
| `requesterName` | string or null | Optional display field for admin queues. |
| `category` | string | One of the supported category values. |
| `subject` | string | Ticket title, max 120 characters. |
| `content` | string | Initial user description. |
| `status` | string | One of the closed status set. |
| `priority` | string | Defaults to `normal`. |
| `relatedType` | string or null | Optional context type. |
| `relatedId` | number or null | Optional context ID. |
| `assignedAdminUserId` | number or null | Assigned support/admin user. |
| `assignedAdminName` | string or null | Optional display field. |
| `lastRepliedBy` | string or null | Expected values include `user`, `admin`, or null before replies. |
| `lastRepliedAt` | string or null | ISO-like backend timestamp. |
| `resolvedAt` | string or null | Set when entering `resolved`. |
| `closedAt` | string or null | Set when entering `closed`. |
| `createdAt` | string | Creation timestamp. |
| `updatedAt` | string | Last ticket update timestamp. |

### Ticket Detail

Ticket detail returns the ticket fields plus messages:

| Field | Type | Notes |
|---|---|---|
| `ticket` | object | Ticket summary/detail object. |
| `messages` | array | Public messages for users; public messages plus internal notes for admins. |

### Message

| Field | Type | Notes |
|---|---|---|
| `id` | number | Message ID. |
| `ticketId` | number | Parent ticket ID. |
| `senderUserId` | number or null | Sender user/admin ID when available. |
| `senderRole` | string | Expected values include `user`, `admin`, or support staff role labels. |
| `messageType` | string | `public_reply` or `internal_note`. |
| `content` | string | Message content. |
| `createdAt` | string | Creation timestamp. |
| `sender` | object | Optional nested sender object with `id`, `username`, and `nickname`. UI display names should be derived from `sender.nickname`, `sender.username`, or `senderRole`. |

Users can see only `public_reply` messages. Admin detail includes both `public_reply` and `internal_note`.

## User Endpoints

### `POST /api/support/tickets`

Create a support ticket for the authenticated user.

#### Request

| Field | Required | Type | Notes |
|---|---|---|---|
| `category` | yes | string | One of the supported category values. |
| `subject` | yes | string | Non-blank, max 120 characters. |
| `content` | yes | string | Non-blank initial description. |
| `relatedType` | no | string | Optional context owner. |
| `relatedId` | no | number | Required only when the implementation requires an ID for the supplied `relatedType`. |

#### Response

- `data.ticket` is the created ticket.
- `data.messages` contains the initial user public message.

#### Error Cases

- `400`: invalid category, missing subject/content, subject too long, invalid related context, or malformed body.
- `401`: missing token or invalid token.
- `403`: authenticated principal is not allowed to create user tickets.

### `GET /api/support/tickets`

List the current user's tickets.

#### Query

| Field | Required | Type | Notes |
|---|---|---|---|
| `status` | no | string | Optional ticket status filter. |
| `page` | no | number | Default `1`. |
| `pageSize` | no | number | Default `10`, max should stay bounded. |

#### Response

| Field | Type | Notes |
|---|---|---|
| `items` | array | Ticket summaries visible to the current user. |
| `total` | number | Total matching records. |
| `page` | number | Current page number. |
| `pageSize` | number | Current page size. |

#### Error Cases

- `400`: invalid status or pagination values.
- `401`: missing token or invalid token.

### `GET /api/support/tickets/{ticketId}`

Read one of the current user's tickets and public messages.

#### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `ticketId` | yes | number | Target ticket ID. |

#### Response

- `data.ticket` is the ticket detail.
- `data.messages` contains public messages only.

#### Error Cases

- `401`: missing token or invalid token.
- `403`: ticket belongs to another user.
- `404`: ticket does not exist.

### `POST /api/support/tickets/{ticketId}/messages`

Add a public user reply.

#### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `ticketId` | yes | number | Target ticket ID. |

#### Body

| Field | Required | Type | Notes |
|---|---|---|---|
| `content` | yes | string | Non-blank reply content. |

`messageType` is not accepted from user requests; user replies are always public replies.

#### Response

- `data.ticket` is the refreshed ticket.
- `data.messages` contains public messages only and must not expose internal notes to users.

#### Error Cases

- `400`: blank content or closed-ticket reply.
- `401`: missing token or invalid token.
- `403`: ticket belongs to another user.
- `404`: ticket does not exist.

## Admin Endpoints

### `GET /api/admin/support/tickets`

List support tickets for the admin queue.

#### Query

| Field | Required | Type | Notes |
|---|---|---|---|
| `status` | no | string | Optional status filter. |
| `category` | no | string | Optional category filter. |
| `assignedToMe` | no | boolean | When true, returns tickets assigned to the current admin. |
| `keyword` | no | string | Search ticket number, subject, content, requester display fields, or `relatedId` when supported. |
| `page` | no | number | Default `1`. |
| `pageSize` | no | number | Default `10`, max should stay bounded. |

#### Response

| Field | Type | Notes |
|---|---|---|
| `items` | array | Admin ticket summaries. |
| `total` | number | Total matching records. |
| `page` | number | Current page number. |
| `pageSize` | number | Current page size. |

#### Error Cases

- `400`: invalid status, category, boolean, keyword, or pagination values.
- `401`: missing token or invalid token.
- `403`: current user lacks admin support-ticket access.

### `GET /api/admin/support/tickets/{ticketId}`

Read support-ticket detail, public replies, and internal notes.

#### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `ticketId` | yes | number | Target ticket ID. |

#### Response

- `data.ticket` is the ticket detail.
- `data.messages` contains `public_reply` and `internal_note` messages.

#### Error Cases

- `401`: missing token or invalid token.
- `403`: current user lacks admin support-ticket access.
- `404`: ticket does not exist.

### `PUT /api/admin/support/tickets/{ticketId}/status`

Change ticket status and optionally assign it to the current admin.

#### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `ticketId` | yes | number | Target ticket ID. |

#### Body

| Field | Required | Type | Notes |
|---|---|---|---|
| `status` | yes | string | Target status from the closed status set. |
| `assignToMe` | no | boolean | When true, sets `assignedAdminUserId` to the current admin. |

#### Response

- `data.ticket` is the updated ticket.

#### Error Cases

- `400`: invalid status, invalid transition, malformed body, or closed-ticket mutation.
- `401`: missing token or invalid token.
- `403`: current user lacks admin support-ticket access.
- `404`: ticket does not exist.

### `POST /api/admin/support/tickets/{ticketId}/messages`

Add a public admin reply or an internal note.

#### Path

| Field | Required | Type | Notes |
|---|---|---|---|
| `ticketId` | yes | number | Target ticket ID. |

#### Body

| Field | Required | Type | Notes |
|---|---|---|---|
| `messageType` | yes | string | `public_reply` or `internal_note`. |
| `content` | yes | string | Non-blank message content. |

Public admin replies should set `lastRepliedBy=admin` and can move the ticket to `waiting_user` when the service implements that transition. Internal notes are visible only to admins and must not change owner-module state.

#### Response

- `data.ticket` is the refreshed ticket.
- `data.messages` contains public replies and internal notes for admins.

#### Error Cases

- `400`: invalid message type, blank content, or closed-ticket message.
- `401`: missing token or invalid token.
- `403`: current user lacks admin support-ticket access.
- `404`: ticket does not exist.

## Error Semantics

| HTTP status | `ResultCode` | Meaning |
|---|---|---|
| `400` | `BAD_REQUEST` | Invalid request parameters, validation failure, invalid status transition, or closed-ticket reply. |
| `401` | `UNAUTHORIZED` | Missing token or invalid token. |
| `403` | `FORBIDDEN` | Ticket ownership violation or missing admin support permission. |
| `404` | `NOT_FOUND` | Ticket does not exist. |
| `500` | `INTERNAL_SERVER_ERROR` | Unhandled server-side failure. |

Current backend behavior may return `200 OK` with `success=false`, `code="BUSINESS_ERROR"`, and a business-facing message for some domain rejections. Acceptance should verify the final implementation consistently exposes invalid status transitions and closed-ticket replies as failure envelopes.

## HTTP Asset Mapping

- User and mixed support smoke: `docs/06-http/support.http`
- Admin panel smoke: `docs/06-http/admin.http`
- Admin module index and link: `docs/09-api-spec/admin.md`

## Known Drift Or Follow-Up Notes

- This spec has been aligned with the final MVP controller responses as of 2026-05-30.
