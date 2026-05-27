# Task: Chat MVP Scope Definition

## Metadata

- ID: chat-mvp-scope-definition
- Status: completed
- Owner: Claude
- Track: feature
- Depends on: archived `ui-redesign-messages-support` shell + current order/report baseline
- Priority: high
- Planned date: 2026-05-22
- Completed date: 2026-05-25

## Objective

Resolve open-question #1 ("聊天 MVP 的最小边界") by producing (a) a concrete scope decision document in `docs/02-requirements/` and (b) an execution-ready implementation task spec in `docs/08-tasks/active/`, so a later agent can build chat MVP without re-arguing scope.

This is a planning task. **Zero application code** is touched.

## Background

`open-questions.md` lists three under-decided dimensions for chat MVP:
- whether MVP is "conversation list + message list + send + polling refresh" or richer
- whether v1 supports conversations seeded from product/shop context
- whether v1 includes unread counts and last-message summaries

The frontend already has a `/app/messages` shell (placeholder) and `/admin/support` entry from the archived `ui-redesign-messages-support` task. The order/report/refund flows already cover after-sales communication, which sets the *floor*: chat MVP must not duplicate them. Mediation is explicitly *out* — that is a separate downstream task.

## Pre-flight Verification (must complete before writing decisions)

1. Read `docs/05-roadmap/current/open-questions.md` items 1 and 2 in full
2. Read `docs/02-requirements/communication-and-after-sales-boundary.md`
3. Read `docs/03-architecture/frontend-information-architecture.md` — locate the "messages" section to see what the frontend information architecture already promised
4. Read `docs/08-tasks/archived/ui-redesign-messages-support.md` — locate the "Deferred" / "Follow-up" section to see what the UI shell explicitly left open
5. `grep -ni "chat\|message\|conversation" backend/src/main/java/com/youyu/backend/entity/ -r` — confirm there is NO chat entity yet
6. `grep -ni "chat\|message" backend/src/main/resources/schema.sql` — confirm there is NO chat table yet
7. `ls frontend/src/views/app/MessagesView.vue frontend/src/views/admin/SupportView.vue` — confirm the placeholders exist

## Files to Read

- `AGENTS.md`, `CLAUDE.md`, `docs/README.md`
- `docs/04-standards/development-process.md`
- `docs/05-roadmap/current/stage-roadmap.md`
- `docs/05-roadmap/current/feature-roadmap.md`
- `docs/05-roadmap/current/open-questions.md`
- `docs/02-requirements/communication-and-after-sales-boundary.md`
- `docs/03-architecture/frontend-information-architecture.md`
- `docs/08-tasks/archived/ui-redesign-messages-support.md`
- `docs/08-tasks/archived/order-after-sales-ux-hardening.md` (to understand the after-sales floor)

## In Scope

Produce two artifacts:

### Artifact 1 — `docs/02-requirements/chat-mvp-scope.md`

A new requirements document with these mandatory sections:

1. **In Scope (MVP)** — explicit enumerated list. Default position to defend in the doc:
   - Conversation list per user
   - Message list per conversation
   - Send text message (text-only; no attachments)
   - Polling refresh (no websocket in MVP)
   - Conversation seeded from product or shop context (carry productId/shopId as conversation seed)
2. **Out of Scope (deferred to v2+)** — explicit enumerated list. Default position:
   - Unread counts and last-message summaries (deferred)
   - Image / file / voice messages
   - Real-time push (websocket/SSE)
   - Read receipts
   - Group conversations
   - Search across messages
   - Block/mute
   - Push notifications
3. **Entities** — concrete table for each:
   - `chat_conversations` (id, type, product_id NULL, shop_id NULL, user_a_id, user_b_id, created_at, last_activity_at)
   - `chat_messages` (id, conversation_id, sender_user_id, body, created_at)
   - Indexes that the runtime queries will need
4. **API surface** — concrete list (method, path, auth, request, response):
   - `GET /api/chat/conversations` — list mine
   - `POST /api/chat/conversations` — find-or-create by `(peerUserId, productId?, shopId?)`
   - `GET /api/chat/conversations/{id}/messages` — paginated
   - `POST /api/chat/conversations/{id}/messages` — send text
   - Polling cadence guidance: 5–10s default for active conversation view; conversation list refresh on focus only
5. **Frontend surfaces** — concrete page list:
   - `/app/messages` (conversation list)
   - `/app/messages/:conversationId` (conversation detail)
   - "Send message" entry points: product detail page (button → finds or creates conversation seeded by productId) and shop detail page (same with shopId)
6. **Non-goals re. mediation/report** — single paragraph explicitly stating mediation goes through `platform-mediation-boundary-definition`, not this task
7. **Performance floor** — explicit: polling must not exceed 1 request per active conversation per 5s; conversation list query bounded by `LIMIT 50`

### Artifact 2 — `docs/08-tasks/active/chat-mvp-implementation.md`

A new active task spec a later agent can dispatch directly. Must use this same task structure (Metadata, Pre-flight Verification, In Scope, Out of Scope, Hard Limits, Allowed Changes, Implementation Steps, Test Plan, Acceptance Criteria, Documentation Updates Required, Final Report Format). Implementation Steps must include:
- backend: migration for two tables, entity classes, mapper interface + JdbcImpl, service interface + impl, controller, integration test class
- frontend: API module under `api/modules/chat.js`, Pinia store `stores/chat.js`, views for conversation list and detail
- product/shop detail page: add "Send message" CTA that calls find-or-create
- smoke test additions to `docs/06-http/` (new `chat.http`)

## Out of Scope (this task)

- Writing actual chat code
- Choosing a websocket library
- Mediation scope
- Modifying schema.sql or seed data
- Touching `/app/messages` or `/admin/support` placeholder code

## Hard Limits

- **Do not** edit any file under `backend/src/` or `frontend/src/`
- **Do not** write the chat backend migration or controller code — just decide its shape in the requirements doc
- **Do not** create `chat.http` content — only reference it in the implementation task as deliverable
- **Do not** weaken the existing after-sales flow ownership; chat is for free-form communication, after-sales remains on order/refund/report flows
- **Do not** expand MVP beyond text + polling without an explicit user override

## Allowed Changes

- `docs/02-requirements/chat-mvp-scope.md` (new)
- `docs/08-tasks/active/chat-mvp-implementation.md` (new)
- `docs/05-roadmap/current/open-questions.md` (mark item 1 resolved with a one-line pointer to `chat-mvp-scope.md`)
- `docs/05-roadmap/current/feature-roadmap.md` (one-line status update)
- `CHANGELOG.md`
- `docs/08-tasks/active/chat-mvp-scope-definition.md` → move to `archived/`

## Implementation Steps

1. Complete pre-flight verification; record findings.
2. Draft `docs/02-requirements/chat-mvp-scope.md` with the seven sections listed under Artifact 1. Keep the default position unless pre-flight findings give specific evidence to deviate; if deviating, write a `## Deviation rationale` block at the top of the file.
3. Draft `docs/08-tasks/active/chat-mvp-implementation.md` using this task as a structural template. Implementation Steps must be concrete enough that a sub-agent can run them without further design decisions.
4. Update `docs/05-roadmap/current/open-questions.md`: replace the body of section "1. 聊天 MVP 的最小边界" with a one-line resolution pointer; remove the unresolved bullets. Section 2 (mediation) stays open.
5. Update `docs/05-roadmap/current/feature-roadmap.md`: mark chat MVP as "scope decided, implementation pending" and link to `chat-mvp-implementation.md`.
6. Prepend a `CHANGELOG.md` block under `### docs`.
7. Move this task file to `archived/` with `Status: archived`, `Completed date`, and a `Delivered` section listing the two new docs.

## Test Plan

- Backend: not required
- Frontend: not required
- API validation: not required
- Manual checks (paste evidence in Final Report):
  - `docs/02-requirements/chat-mvp-scope.md` has all 7 sections; entities table is concrete (column names + types)
  - `docs/08-tasks/active/chat-mvp-implementation.md` has every section a normal task spec has, and a sub-agent could execute it without asking "what should the entity table look like"
  - `open-questions.md` item 1 is resolved; item 2 still open
  - `platform-mediation-boundary-definition` is NOT touched

## Acceptance Criteria

- [ ] `docs/02-requirements/chat-mvp-scope.md` exists with the 7 mandatory sections
- [ ] Entities section lists concrete column names and types for `chat_conversations` and `chat_messages`
- [ ] API surface section lists every endpoint with method/path/auth/request/response
- [ ] Frontend surfaces section names specific routes and CTA entry points
- [ ] Non-goals paragraph explicitly defers mediation and unread/last-message
- [ ] `docs/08-tasks/active/chat-mvp-implementation.md` exists and follows the same task-doc structure as this file (incl. Final Report Format)
- [ ] `open-questions.md` item 1 is resolved with a one-line pointer; item 2 remains
- [ ] `feature-roadmap.md` reflects "scope decided, implementation pending"
- [ ] `CHANGELOG.md` block added
- [ ] This task moved to `archived/`

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/` — not for this task; chat.http belongs to the implementation task
- [x] roadmap docs (`open-questions.md`, `feature-roadmap.md`)
- [x] task status and archive move

## Final Report Format

```markdown
## Return Report — chat-mvp-scope-definition

### A. Branch & Commit
- Branch: <name>
- Commit SHA: <sha>
- Files changed (paste `git diff --stat`)

### B. Pre-flight Findings
- Confirmed no chat entity in backend (grep output line count)
- Confirmed no chat table in schema.sql (grep output)
- Placeholders confirmed: MessagesView.vue, SupportView.vue (file sizes)
- Prior decisions inherited from communication-and-after-sales-boundary.md: <1-line summary>

### C. Implementation Walkthrough
- Step 2 → chat-mvp-scope.md created (line count: <N>)
- Step 3 → chat-mvp-implementation.md created (line count: <N>)
- Step 4 → open-questions.md item 1 resolution line: "<paste line>"
- Step 5 → feature-roadmap.md updated (line number)
- Step 6 → CHANGELOG block added
- Step 7 → task moved to archived

### D. Test Plan Results
- chat-mvp-scope.md section presence:
  - [ ] In Scope (MVP) — paste line count
  - [ ] Out of Scope — paste line count
  - [ ] Entities — paste table column count
  - [ ] API surface — paste endpoint count
  - [ ] Frontend surfaces — paste route count
  - [ ] Non-goals — present yes/no
  - [ ] Performance floor — present yes/no
- chat-mvp-implementation.md sections matching template: <yes/no>
- platform-mediation-boundary-definition file size BEFORE and AFTER (must be identical)

### E. Acceptance Criteria Check
- [x/✗] one bullet per acceptance criterion above, with evidence

### F. Deviations from Spec
- If the doc deviates from the "default position" listed in Artifact 1, paste the rationale section here; "none" otherwise

### G. Out-of-scope Findings
- "none" or specific items

### H. Open Questions / Blockers
- "none" or single question
```

## Completion Notes

(Filled in by sub-agent.)
