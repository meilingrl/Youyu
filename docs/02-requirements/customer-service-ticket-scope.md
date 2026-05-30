# Customer Service Ticket Scope

## 1. Purpose

This document defines the first durable customer-service feature for Youyu.

The feature adds a platform support-ticket workflow. It is separate from buyer/seller chat, reports, order fulfillment, refunds, and formal mediation.

## 2. Product Boundary

Customer service v1 owns:

- user-created platform support tickets;
- user-visible ticket list and ticket detail;
- admin support queue, assignment, status changes, public replies, and internal notes;
- optional links from a ticket to an order, product, shop, or user context;
- audit-friendly timestamps and handler identity.

Customer service v1 does not own:

- real-time chat, WebSocket, SSE, or polling conversation transport;
- admin participation in buyer/seller chat;
- mediation case creation or platform dispute decisions;
- order, refund, report, product, shop, or user state transitions;
- notification delivery, SLA automation, workload balancing, or group governance.

## 3. Status Model

Tickets use this closed status set:

| Status | Meaning | Allowed next statuses |
|---|---|---|
| `open` | User submitted a ticket and it has not been taken by support. | `in_progress`, `closed` |
| `in_progress` | A support agent is reviewing or replying. | `waiting_user`, `resolved`, `closed` |
| `waiting_user` | Support has replied and is waiting for user input. | `in_progress`, `resolved`, `closed` |
| `resolved` | Support considers the issue handled. | `closed` |
| `closed` | Terminal closed state. | none |

Users may add public replies while a ticket is not `closed`. Admins may add public replies and internal notes while a ticket is not `closed`.

## 4. Category Model

The first category set is:

- `account`
- `order`
- `product`
- `shop`
- `payment`
- `report`
- `other`

Categories help routing only. They do not change ownership of linked business records.

## 5. Data Contract

Add these tables to `backend/src/main/resources/schema.sql`:

```sql
CREATE TABLE IF NOT EXISTS support_tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_no VARCHAR(40) NOT NULL UNIQUE,
    requester_user_id BIGINT NOT NULL,
    category VARCHAR(32) NOT NULL,
    subject VARCHAR(120) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'open',
    priority VARCHAR(32) NOT NULL DEFAULT 'normal',
    related_type VARCHAR(32),
    related_id BIGINT,
    assigned_admin_user_id BIGINT,
    last_replied_by VARCHAR(32),
    last_replied_at TIMESTAMP,
    resolved_at TIMESTAMP,
    closed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_support_ticket_requester FOREIGN KEY (requester_user_id) REFERENCES users(id),
    CONSTRAINT fk_support_ticket_assignee FOREIGN KEY (assigned_admin_user_id) REFERENCES users(id)
);

CREATE INDEX idx_support_tickets_requester_updated ON support_tickets(requester_user_id, updated_at, id);
CREATE INDEX idx_support_tickets_status_updated ON support_tickets(status, updated_at, id);
CREATE INDEX idx_support_tickets_assignee_updated ON support_tickets(assigned_admin_user_id, updated_at, id);

CREATE TABLE IF NOT EXISTS support_ticket_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    sender_user_id BIGINT,
    sender_role VARCHAR(32) NOT NULL,
    message_type VARCHAR(32) NOT NULL DEFAULT 'public_reply',
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_support_ticket_message_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets(id),
    CONSTRAINT fk_support_ticket_message_sender FOREIGN KEY (sender_user_id) REFERENCES users(id)
);

CREATE INDEX idx_support_ticket_messages_ticket_created ON support_ticket_messages(ticket_id, created_at, id);
```

## 6. API Contract

User endpoints:

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/support/tickets` | Create a support ticket. |
| `GET` | `/api/support/tickets` | List current user's tickets with `status`, `page`, and `pageSize`. |
| `GET` | `/api/support/tickets/{ticketId}` | Read current user's ticket detail and public messages. |
| `POST` | `/api/support/tickets/{ticketId}/messages` | Add a public user reply. |

Admin endpoints:

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/admin/support/tickets` | List support tickets with `status`, `category`, `assignedToMe`, `keyword`, `page`, and `pageSize`. |
| `GET` | `/api/admin/support/tickets/{ticketId}` | Read ticket detail, public replies, and internal notes. |
| `PUT` | `/api/admin/support/tickets/{ticketId}/status` | Change status and optionally assign to the current admin. |
| `POST` | `/api/admin/support/tickets/{ticketId}/messages` | Add a public reply or internal note. |

All responses use the existing `ApiResponse<T>` envelope.

## 7. UI Contract

User-facing:

- Add `/app/support` as a user-authenticated page.
- Show create-ticket form, my-ticket list, selected-ticket detail, and public reply form.
- Label the flow as platform customer service, not real-time chat.

Admin-facing:

- Upgrade `/admin/support` from context-only dashboard to a ticket queue plus detail workspace.
- Preserve quick links to reports, orders, mediation, and governance pages as context shortcuts.
- Show status/category filters, assigned/unassigned state, public replies, internal notes, and status actions.

## 8. Testing Expectations

Backend tests must cover:

- user create/list/detail/reply authorization;
- users cannot read or reply to other users' tickets;
- admin list/detail/status/reply/note;
- invalid status transition rejection;
- closed tickets reject further replies.

Frontend verification must cover:

- production build;
- route registration for `/app/support` and `/admin/support`;
- no admin UI calls to `/api/chat/**` for support tickets.

Docs and smoke coverage must update:

- `docs/06-http/support.http`;
- `docs/06-http/admin.http`;
- `docs/09-api-spec/support.md`;
- `docs/09-api-spec/admin.md`;
- `CHANGELOG.md`.
