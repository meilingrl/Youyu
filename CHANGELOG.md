## [2026-06-03] - Frontend Production Copy Polish

### changed
- Reworked visible frontend copy that still referenced development-stage concepts such as backend interfaces, fields, placeholder assets, drafts, and raw IDs.
- Normalized product, report, mediation, seller, shop, login, and support labels so user-facing pages use formal Chinese business wording instead of internal enum or database-style terms.

### verify
- `npm run build` in `frontend/` pass.
- `npm test` in `frontend/` pass, 62 tests.

---

## [2026-06-03] - Invalid Entity ID Guard

### fixed
- Prevented frontend requests with path segments such as `NaN`, `undefined`, or non-numeric ids from reaching Spring `Long` bindings by adding shared entity-id validation, route `beforeEnter` guards, API-module early rejection, and Axios query-param sanitization (notably explore `categoryId`).
- Mapped backend `MethodArgumentTypeMismatchException` to HTTP 400 instead of logging an unhandled 500 when invalid query/path values still slip through.

### verify
- `npm test` in `frontend/` (includes new `id-utils` unit tests).

---

## [2026-06-03] - Roadmap Archive Cleanup

### changed
- Archived `docs/05-roadmap/current/feature-roadmap.md`, `docs/05-roadmap/current/open-questions.md`, and `docs/05-roadmap/current/admin-module-goal-roadmap.md` after they stopped defining current work.
- Archived `docs/08-tasks/drafts/messages-ux-redesign.md` by user decision.
- Updated `AGENTS.md`, `docs/README.md`, `docs/05-roadmap/README.md`, and standards docs so the current reading order now points to `stage-roadmap.md` plus `launch-preparation-roadmap.md` instead of the archived feature/question documents.

---

## [2026-06-03] - Roadmap And Task Closeout Sync

### changed
- Archived `payment-upgrade.md`, `payment-upgrade-verification-and-docs.md`, `payment-upgrade-main-agent-prompt.md`, and `preference-theme-capability-gap.md` after user-confirmed payment sandbox acceptance and theme-work cancellation.
- Updated the current stage and feature roadmaps to reflect that the present F0-F6 functional roadmap has been completed and that the repository is now focused on launch preparation plus future expansion planning.

---

## [2026-06-03] - Task Lifecycle Metadata Sync

### changed
- Corrected archived task metadata for `execution-wave-plan-2026-05-20.md` and `seller-publish-loading-diagnosis.md` so their status now matches the archived lifecycle state.

---

## [2026-06-03] - Payment Upgrade Verification Follow-Up

### changed
- Fixed the local `mock-success` failure on pre-existing MySQL databases by adding a startup-time `PaymentSchemaUpgrader` instead of relying on manual schema repair.
- Removed the temporary `ALTER TABLE` fallback from `backend/src/main/resources/schema.sql` so the baseline schema stays compliant with repository DDL rules.
- Clarified the Alipay sandbox verification boundary and documented that QR checkout must use the sandbox wallet / sandbox buyer flow rather than the regular production Alipay app.

### verify
- Backend `.\\mvnw.cmd test "-Dtest=PaymentGatewayFoundationTest,PaymentRefundConsistencyTest,PaymentServiceImplTest,YouyuBackendApplicationTests"` pass, 38 tests.
- `git diff --check` pass.

---

## [2026-06-02] - Analytics Visualization Enhancement (ECharts Upgrade)

### added
- Added ECharts-based chart visualization to admin dashboard: order status donut, mediation progress donut with preserved navigation links.
- Created reusable chart wrapper components: `ChartPie.vue`, `ChartBar.vue` under `frontend/src/components/common/`.
- Enhanced user profile spend visualization with tab switcher (月度/年度/分类) and ECharts charts.
- Added `seed/seed-zhangsan-spend.sql` so demo user `zhangsan` has multi-month completed paid orders for spend chart acceptance.

### changed
- Admin dashboard "订单状态" and "调解进度" sections now use donut charts instead of breakdown row lists.
- User profile "支出统计" section upgraded from InsightBarList-only to ECharts tab view with explicit data-source note.
- Fixed profile spend aggregation to read `orderStatus`, `paymentStatus`, `payableAmount`, and `submittedAt` from `GET /api/orders`, and aligned yearly/category payloads with `SpendChart.vue` (`totalSpend`, `{ label, amount }`).

### notes
- Chart library: Apache ECharts + vue-echarts (already present on master; reused by new chart wrappers).
- Monthly/yearly/category spend charts are frontend-derived from existing order list API; UI notes "基于近期订单统计".

### verify
- Frontend `npm run build` pass (exit 0).

---

## [2026-06-02] - Admin Notification Publishing And Support Recovery

### added
- Added an admin-only `/admin/notifications` publishing page and `POST /api/admin/notifications` broadcast endpoint for in-app system notifications.
- Added audit logging for system-notification broadcasts and backend coverage for publish permissions, persistence, and validation.

### changed
- Fixed the `/admin/support` blank-page regression caused by using ticket filter constants before initialization.
- Completed the user message-center notification type labels for support tickets and mediation updates.
- Repaired the remaining corrupted message-center empty-state icons and clarified the notification empty state.

### verify
- Backend `.\\mvnw.cmd test "-Dtest=AdminNotificationPublishTest,SupportTicketTest,SupportChatTest"` pass, 15 tests.
- Backend `.\\mvnw.cmd test` pass, 196 tests.
- Frontend `npm test -- --run` pass, 53 tests.
- Frontend `npm run build` pass.
- `git diff --check` pass.
- Browser smoke pass for `/admin/support`, `/admin/notifications`, and `/app/messages?category=notifications`.

---

## [2026-06-02] - Analytics Visualization Wave

### added
- Added `frontend/src/components/common/InsightBarList.vue` as a reusable lightweight analytics bar-list component for real metric snapshots.

### changed
- Reworked `ProfileView.vue` to visualize real user spending with category subtotals and recent completed paid-order summaries.
- Extended `ShopView.vue` to surface monthly shop income, completed paid-order volume, repeat buyers, and hot-product income.
- Added admin completed-paid sales analytics for product-category share and shop rankings, then surfaced both on `DashboardView.vue` while keeping the operational workbench intact.
- Corrected shop insight semantics so `monthlyOrderCount` counts completed paid orders and `repeatBuyerCount` counts distinct repeat buyers.
- Parked the theme-capability-gap task and archived the analytics visualization execution/task records after implementation closeout.

### verify
- Backend `.\\mvnw.cmd test` pass, 193 tests.
- Backend `.\\mvnw.cmd test "-Dtest=AdminGovernanceTest,YouyuBackendApplicationTests"` pass after the final hot-product query correction, 55 tests.
- Frontend `npm test -- --run` pass, 51 tests.
- Frontend `npm run build` pass.
- `git diff --check` pass.

---

## [2026-06-02] - Support Entry Routing And Copy Follow-Up

### changed
- Rebuilt the user message center support lane into a single clean implementation so `/app/messages` defaults back to trade conversations unless support is explicitly requested.
- Restored a working handoff between `/app/messages` and `/app/support`, including clear support-ticket and online-customer-service entry actions.
- Rewrote the support-facing user/admin copy and chat helper surfaces to remove garbled text and replace draft-stage wording with launch-ready product language.
- `/app/support` now stays inside the shared shell spacing standard instead of hugging the viewport edges.

### verify
- Frontend `frontend\\npm run build` pass.
- Repo text scan across the support/message surfaces found no remaining mojibake markers in the updated files.
- `git diff --check` pass.

---

## [2026-06-01] - Wave 1A Support And After-Sales Continuity

### added
- Added explicit dual-workspace admin support handling on `/admin/support`, separating online CS chat from support-ticket queue/detail handling while keeping one admin entry route.
- Added order after-sales visibility fields across buyer and admin order detail responses: `relatedReports`, `mediationSummary`, and `afterSalesSummary`.
- Added backend coverage for admin support-ticket keyword search by `relatedId` and order-detail after-sales context visibility.

### changed
- Admin support now gives user-created tickets a first-class handling lane alongside the existing online CS console instead of leaving ticket handling implicit.
- Buyer and admin order detail pages now show refund timeline details, report-backed mediation context, and guidance text without pretending Wave 1 already upgraded the payment gateway.
- Order detail guidance now keeps user-side after-sales visibility and admin-side handling continuity aligned through support, reports, and mediation entry points.
- Existing in-app notifications now cover support-ticket public replies/status changes and mediation escalation/status/decision updates, with action links landing on real support or order pages.

### verify
- Backend `mvnw.cmd test "-Dtest=SupportTicketTest,OrderAfterSalesDetailTest,AdminMediationTest,SupportChatTest"` pass, 19 tests.
- Backend `mvnw.cmd test` pass, 193 tests.
- Frontend `npm run build` pass.

---

## [2026-06-01] - Wave 0 Favorites And Review Summary Closeout

### added
- Added real favorites closure with `GET /api/favorites`, `POST /api/favorites`, and `DELETE /api/favorites/{productId}` plus backend persistence through `product_favorites`.

### changed
- Frontend favorites store and API module now use the frozen REST contract instead of a toggle-only endpoint.
- Product and shop review summaries now return real aggregated score distributions while preserving `avgScore`, `reviewCount`, and `distribution`.
- Favorites and review summary HTTP/API docs now match the accepted Wave 0 implementation, including invalid favorites payload handling and always-present 1-5 review buckets.

### verify
- Backend `mvnw.cmd test -Dtest=FavoritesIntegrationTest` pass, 3 tests.
- Backend `mvnw.cmd test -Dtest=ReviewIntegrationTest` pass, 14 tests.
- Backend `mvnw.cmd test` pass, 191 tests.
- Frontend `npx vitest run src/stores/__tests__/market.test.js` pass, 4 tests.
- Frontend `npm run build` pass.
- `git diff --check` pass.

---

## [2026-06-01] - Personalization Address And Avatar Hardening

### added
- Added user address editing and deletion through `PUT /api/users/addresses/{addressId}` and `DELETE /api/users/addresses/{addressId}`.
- Added frontend address edit, cancel, delete confirmation, and refreshed address list behavior in settings.

### changed
- Avatar upload now validates image file signatures for JPEG, PNG, and WebP instead of trusting MIME type alone.
- Successful avatar replacement now removes the previous local avatar file when it belongs to the configured avatar upload directory.

### verify
- Backend `mvnw.cmd -Dtest=UserProfileTest test` pass, 18 tests.
- Backend `mvnw.cmd test` pass, 188 tests.
- Frontend `npm test -- --run` pass, 49 tests.
- Frontend `npm run build` pass.

---

## [2026-06-01] - Authentication Upgrade Wave 1 Accepted

### added
- Added SMTP-backed registration and forgotten-password email verification with
  environment-only mail configuration and a deterministic network-free test
  sender.
- Added hashed email-code and CAPTCHA challenges, login-failure counters,
  resend cooldowns, attempt limits, source limits, and CAPTCHA escalation after
  three consecutive password failures.
- Added public `/forgot-password`, registration email-code controls, CAPTCHA
  refresh handling, API contract docs, and executable HTTP examples.

### changed
- Registration now requires a verified email code and returns the new user
  without issuing a JWT.
- Password recovery is enumeration-safe and remains separate from student
  identity verification.
- Updated SMTP operator guidance and support FAQ wording.

### verify
- Backend `mvnw.cmd test` - 178 tests passed.
- Frontend `npm test` - 49 tests passed; `npm run build` succeeds.
- `git diff --check`, changed-file whitespace audit, and SMTP-address/default
  scans pass.
- Manual SMTP acceptance passed for registration email delivery, verified
  registration without JWT issuance, password-reset email delivery, login with
  the new password, and CAPTCHA escalation after repeated failures.

## [2026-05-31] - Personalization Settings Profile And Defaults

### added
- Added authenticated nickname editing via `PATCH /api/users/profile`; login ID / `username` remains immutable.
- Added real avatar upload via `POST /api/users/me/avatar` with multipart field `file`, 10 MB limit, JPEG/PNG/WebP validation, persisted public avatar URL, and `/uploads/avatars/**` serving.
- Added `PUT /api/users/me/email` as an email-binding entry that validates format and uniqueness while email-code verification and email login remain future work.
- Added settings entries for profile/avatar, email binding, address management, and default preference controls.

### changed
- Fixed browser avatar upload by letting the client generate the multipart boundary and by proxying `/uploads/**` during local frontend development.
- Default sort now affects product-list ordering on the frontend; default address/fulfillment/payment preferences are applied or displayed in checkout/payment flows.
- Preference UI no longer exposes theme controls in this release.
- Localized personalization settings UI copy so users do not see delivery-phase wording.

### verify
- Backend `mvnw.cmd test` pass, 175 tests.
- Frontend `npm test -- --run` pass, 39 tests.
- Frontend `npm run build` pass.

---

## [2026-05-31] - Payment Gateway Upgrade Foundation

### added
- Introduced a replaceable payment gateway router with default local mock mode and opt-in Alipay sandbox QR payment via `alipay.trade.precreate`.
- Added RSA2 asynchronous callback verification, amount checks, replay protection, payment attempt timeout/retry handling, and a resume endpoint that regenerates a lost QR entry without creating a duplicate payment record.
- Added additive MySQL migration references `003_payment_gateway_foundation.sql` and `004_payment_refund_consistency.sql`, root `.env.example` placeholders, Compose variable forwarding, and `scripts/start-backend-local.ps1`.

### changed
- Routed admin refund completion through the selected successful payment gateway with idempotent completion and diagnostic failure state.
- Updated the buyer payment page with backend-driven payment methods, failure/cancellation/timeout feedback, retry actions, mobile continuity, and local QR rendering without buyer-facing mock/test wording.
- Restricted Vitest discovery to project sources so dependency test files are not collected after adding QR rendering.

### verify
- Payment/refund backend suite `mvnw.cmd test '-Dtest=PaymentGatewayFoundationTest,PaymentRefundConsistencyTest,AlipaySandboxGatewayServiceImplTest,PaymentServiceImplTest'` - 13 tests passed.
- Frontend `npm test -- --run` - 44 tests passed; `npm run build` succeeds.
- Full backend `mvnw.cmd test` - 184 tests passed after interrupted-delivery audit hardening. One earlier run reproduced the pre-existing transient `SupportChatTest` same-timestamp ordering failure; repeat full runs passed.
- Real Alipay sandbox checkout/callback remains pending local credential and public HTTPS callback configuration.
- Interrupted-delivery audit added synchronous Alipay API response-signature verification before provider payloads are accepted.

---

## [2026-05-31] - Online Customer Service Console (Chat + UX Polish)

### added
- In-app online CS on chat: `POST /api/chat/support/session`, rule-based FAQ bot (`SupportFaqKnowledgeBase`), `POST /api/chat/conversations/{id}/escalate`, user `POST /api/chat/conversations/{id}/close-support`.
- Admin `/admin/support` rebuilt as real-time CS console (`/api/admin/support/chat/*`, filters 待接入/进行中/我处理的/已结束, claim/reply/close).
- `chat_conversations.support_status` / `assigned_admin_id`, seeded `platform_cs`, ADR `docs/07-decisions/2026-05-30-online-customer-service-on-chat.md`.

### fixed
- `ChatSupportSchemaUpgrader` migrates legacy MySQL/H2 chat tables (missing `message_type`, `is_recalled`, `unread_count_*`, etc.) so admin queue no longer returns 500 on old databases.
- Escalation persists `pending` reliably; admin queue includes legacy `direct` + `platform_cs` threads; user can end/restart sessions; admin permissions aligned (`ADMIN_SUPPORT_TICKETS_HANDLE`).
- Admin sidebar icon visual harmony; removed admin table row swipe-select (`admin-row-swipe-selection.js`); admin copy sweep (no dev-draft hero text).

### verify
- Backend `mvnw.cmd test` — pass (`SupportChatTest` included).
- Frontend `npm test` + `npm run build` — pass.

---

## [2026-05-31] - User Can End and Restart Support Sessions

### fixed
- Added `POST /api/chat/conversations/{id}/close-support` so users can end an online CS session; closed sessions block new messages with a clear prompt to restart.
- `POST /api/chat/support/session` now fully reopens closed sessions (`ai` + clears `assigned_admin_id`); sidebar button shows「继续咨询」/「联系在线客服」; chat header adds「结束会话」and「再次咨询」.
- Deleting a support conversation from the list ends the session (not just hide) so admins are not left with stale pending queues.

### verify
- Backend `mvnw.cmd test -Dtest=SupportChatTest` — pass.

---

## [2026-05-31] - Fix Admin Pending Queue After 转人工

### fixed
- Escalation now fails fast if `support_status` cannot be persisted (instead of silently leaving the session on `ai` while the UI shows 待接入).
- Escalation increments unread for the platform CS account (`user_b`) so admin queue badges reflect new work.
- Startup backfill recovers support conversations that already have the 转人工 system message but were stuck on `support_status = 'ai'`.
- User chat store refetches conversations after escalate; admin `/admin/support` route requires `ADMIN_SUPPORT_TICKETS_HANDLE` (matches backend).

### verify
- Backend `mvnw.cmd test -Dtest=SupportChatTest` — pass.

---

## [2026-05-31] - Fix Missing Support Chat Columns on Existing MySQL

### fixed
- Added `ChatSupportSchemaUpgrader` to apply `support_status` and `assigned_admin_id` (plus FK/indexes) to pre-existing MySQL `chat_conversations` tables on startup; `CREATE TABLE IF NOT EXISTS` alone does not add new columns.
- Startup upgrader now also seeds the `platform_cs` account when missing (normal dev run does not load `seed/data.sql`, which caused `POST /api/chat/support/session` to fail).
- Reset JDBC schema-availability cache after migration; improved support-session lookup and frontend error messaging.
- Added manual migration reference `database/002_support_chat_columns.sql`.

### verify
- Backend `mvnw.cmd test` — 163 tests, 0 failures.

---

## [2026-05-30] - Online Customer Service (Chat + AI Bot + Escalation)

### added
- Added an in-app online customer-service capability on top of the existing chat system: users can start an idempotent support session from the message center 客服 tab (`POST /api/chat/support/session`).
- Added a rule-based FAQ bot (`SupportFaqKnowledgeBase`) that auto-replies as the platform CS account to support messages while a session is in AI-handling state, with an escalate fallback. No external LLM.
- Added 转人工 escalation (`POST /api/chat/conversations/{id}/escalate`); escalated sessions stop the AI and surface in the admin queue.
- Rebuilt `/admin/support` as an online customer-service console (queue with 待接入/进行中/我处理的/已结束 filters, chat thread + composer with support quick replies, requester context, claim/close actions) backed by `/api/admin/support/chat/*` (permission `ADMIN_SUPPORT_TICKETS_HANDLE`).
- Added support state columns (`support_status`, `assigned_admin_id`) and indexes to `chat_conversations`, a seeded `platform_cs` account, and an example support conversation. Legacy support-ticket system kept intact.

### changed
- Message center 客服 tab now recognizes the proper `support` conversation type instead of the previous `type==='direct'` hack.

### verify
- Backend `mvnw.cmd test` — 163 tests, 0 failures.
- Frontend `npm test` — 39 tests passing; `npm run build` succeeds.

---

## [2026-05-30] - Launch Foundation Staging Rehearsal Baseline

### added
- Added schema-only staging Compose startup, explicit demo seed overlay, backend and frontend images, Nginx same-origin API proxy, and an optional HTTPS template.
- Defaulted rehearsal host ports to `13306`, `18080`, and `18081` so Compose can coexist with common local MySQL and application ports.
- Added guarded MySQL backup and restore scripts, minimal Actuator DB health, MDC trace IDs, console logging, dependency and secret scanning CI, and k6 baseline smoke scripts.
- Added a staging rehearsal runbook with verified integration evidence and explicit production blockers.

### verify
- Backend tests: 162 passed.
- Frontend tests: 39 passed; production build completed.
- Default Compose, demo overlay, backup restore, gitleaks, npm audit, and k6 smoke were exercised locally.

---

## [2026-05-30] - Customer Service Ticket MVP

### added
- Added durable customer-service tickets with user create/list/detail/reply APIs and admin queue/detail/status/reply/internal-note APIs.
- Added `/app/support` for asynchronous user support tickets and rebuilt `/admin/support` as a ticket queue/detail workspace.
- Added support-ticket schema, seed examples, backend coverage, API spec, and HTTP smoke coverage.

### verify
- Backend and frontend verification pending final head-Agent run.

---

## [2026-05-30] - Roadmap Split for Feature Completion and Launch Preparation

### docs
- Reworked the feature roadmap into an F0-F6 function-completion sequence covering contract gaps, support/after-sales, payment upgrade, analytics visualization, personalization, operations, and UI closeout.
- Reworked the launch-preparation roadmap into an L0-L7 infrastructure, security, compliance, deployment, monitoring, and release-readiness checklist.
- Updated the stage roadmap to reference the separated feature and launch routes instead of mixing product work with deployment hardening.

---

## [2026-05-29] - Admin Workbench Polish Follow-up

### changed
- Tightened the admin sidebar with two-character labels, icons, narrower layout, database-backed account display, and a circular logout button.
- Reworked admin table action buttons and batch-action spacing, and added drag-follow right-swipe row selection for batch-capable admin tables.
- Rebuilt the customer-service page as a queue-based customer-service workspace with non-empty queues only and Chinese status labels.

### verify
- `npm test`
- `npm run build`

---

## [2026-05-29] - Admin Workbench UX And Batch Improvements

### changed
- Polished admin navigation, left-sidebar logout placement, dashboard zero-value display, table filters, table spacing, Chinese status labels, mediation copy, and the customer-service workspace framing.
- Added product review-task detail access so admins can open submitted material before approving or rejecting it.

### added
- Added batch endpoints and UI actions for users, student verifications, products, product review tasks, shops, and reports.
- Updated admin API docs and HTTP smoke samples for the new batch and review-detail endpoints.

### verify
- `npm test`
- `npm run build`
- `.\mvnw.cmd test -Dtest=AdminGovernanceTest`
- `.\mvnw.cmd test`

---

## [2026-05-29] - Marketing MVP Foundation

### feat
- Added shop-owner fixed and threshold coupons with admin review before buyer claim.
- Added shop-owner activities with admin review before public shop display.
- Added buyer coupon claim, owned-coupon list, checkout selection, and one-coupon order preview/create integration with server-side revalidation.
- Persisted actual order discount and coupon application snapshots, with duplicate claim, over-claim, duplicate-use, and cross-shop protections.
- Added buyer, seller, and admin marketing frontend entries without adding duplicate top-level navigation.

### docs
- Added marketing API spec and HTTP smoke examples.
- Updated order/admin API specs and order smoke examples for coupon usage.

### test
- Added backend marketing lifecycle and permission coverage.
- Verified backend tests, frontend unit tests, and frontend production build.

---

## [2026-05-29] - Roadmap and Active Task Sync

### docs
- Replaced the stale launch-preparation roadmap content with the current Phase 0-4 上线准备计划.
- Archived the completed admin module head-agent prompt so active tasks only represent current execution scope.
- Aligned the stage roadmap reference with the Phase 0-4 launch-preparation plan.

---

## [2026-05-28] - Seed Profile Startup Environment

### docs
- Corrected seed-profile startup docs to use `SPRING_PROFILES_ACTIVE=seed`, including the PowerShell form `$env:SPRING_PROFILES_ACTIVE="seed"`.

---

## [2026-05-28] - Seed Profile Startup Docs

### docs
- Clarified seed-profile startup as an application argument via `--spring.profiles.active=seed`, including the Maven wrapper argument form and IDE program argument.

---

## [2026-05-28] - Admin Seed Login Fix

### fix
- Treated specialist admin roles as admin sessions on the login page so `superadmin`, `reviewer`, `supportagent`, `operator`, and `orderadmin` enter the admin workbench instead of the user app.

### test
- Expanded backend seed admin login coverage across all seeded admin roles.
- Added frontend auth-store coverage for specialist admin sessions.
- Added admin full-flow HTTP login smoke requests for seeded admin accounts.

---

## [2026-05-28] - Admin Module Goal Roadmap Closeout

### docs
- Updated the admin module goal roadmap baseline to reflect completed mediation, backend-enforced staff roles, and full-flow seed verification coverage.

### verify
- Verified the repository state after the final seed workflow commit.

---

## [2026-05-28] - Seed Full Admin Flow

### test
- Added seeded admin audit-log examples and a full admin-flow HTTP walkthrough covering dashboard, review queues, reports, orders, mediation, search governance, audit logs, and role permission smoke checks.

### docs
- Documented seeded credentials and end-to-end local admin verification steps in `docs/06-http/admin-full-flow.http`.

### verify
- Verified backend tests, frontend tests, frontend production build, and diff checks.

---

## [2026-05-28] - Admin Role Permission Model

### feat
- Added five admin staff roles with backend capability enforcement and legacy `ADMIN` full-access compatibility.
- Restricted admin governance, order, mediation, search, and audit endpoints by backend permission.
- Aligned frontend admin route guards and sidebar visibility with the role-permission matrix.
- Added seed staff accounts for local role verification.

### docs
- Documented admin role semantics in API specs, HTTP smoke examples, and backend seed credentials.

### test
- Added backend role-permission coverage and frontend guard coverage.
- Verified backend tests, frontend tests, and frontend production build.

---

## [2026-05-28] - Admin Audit Log Foundation

### feat
- Added durable `admin_audit_logs` persistence and an admin-only `/api/admin/audit-logs` read endpoint.
- Recorded audit events for high-risk admin mutations including user status, product status, review decisions, shop status, report processing, and search governance rules.

### docs
- Documented the audit-log contract and added admin HTTP smoke examples.

### test
- Added backend coverage for audit writes and admin-only audit reads.
- Verified backend tests.

---

## [2026-05-28] - Admin Dashboard Observability

### feat
- Extended `/api/admin/dashboard` with stable live observability fields for pending queues, governance signals, order status, mediation status, and unavailable metrics.
- Reworked `/admin/dashboard` into a real pending-work monitor backed by backend counts and routed metric cards.

### docs
- Documented the dashboard contract in `docs/09-api-spec/admin.md` and updated the admin HTTP dashboard smoke notes.

### test
- Added backend dashboard contract coverage.
- Verified backend tests, frontend tests, and frontend production build.

---

## [2026-05-28] - Platform Mediation Implementation

### feat
- Added durable `mediation_cases` persistence, seed cases, and admin-only mediation escalation/list/detail/status/decision APIs.
- Implemented idempotent report escalation for order-backed reports and write-once final decisions.
- Exposed mediation-scoped read-only chat context by related order without adding admin chat participation.
- Added `/admin/mediation` list/detail UI and report-page escalation action for eligible order reports.

### docs
- Added mediation API specification and admin HTTP smoke examples.

### test
- Added backend mediation controller coverage for escalation, idempotency, list/detail, status transitions, decisions, authorization, and read-only chat context.

---

## [2026-05-28] - Platform Mediation Boundary Contract

### docs
- Added `docs/02-requirements/platform-mediation-scope.md` defining report escalation into `mediation_cases`, v1 statuses, decision categories, read-only chat visibility, and API/data/admin/seed/test expectations.
- Updated the platform mediation implementation task so it can be dispatched without additional product decisions.
- Updated current roadmaps to mark mediation boundary definition complete and implementation as the next step.
- Aligned support-console scope and open questions with the accepted chat and mediation requirement documents.

### test
- Ran `git diff --check`.

---

## [2026-05-28] - Chat MVP Scope Recovery

### docs
- Restored `docs/02-requirements/chat-mvp-scope.md` from current runtime/API/spec/http truth.
- Clarified chat MVP, support console, and platform mediation ownership boundaries.
- Documented mediation v1 chat visibility as read-only dispute context only, with no admin participation or sending in buyer/seller conversations.
- Updated current roadmaps so platform mediation is no longer blocked by the missing chat scope artifact.

---

## [2026-05-28] - Admin Entry Workbench Navigation

### feat
- Routed authenticated admin default entry and login fallback to `/admin/dashboard`.
- Kept ordinary user default entry in `/app/home` and blocked non-admin direct access to `/admin/*`.
- Reworded admin navigation, topbar, and dashboard surfaces around governance queues and workbench tasks.

### test
- Added router guard coverage for admin and non-admin default entry behavior.
- Ran `frontend\npm test`.
- Ran `frontend\npm run build`.

---

## [2026-05-28] - Admin Module Goal Continuation Plan

### docs
- Added the admin module goal roadmap for the next head Agent.
- Added the Codex CLI head Agent prompt for continuing long-running admin module dispatch.
- Added active task specs for admin entry, chat scope recovery, mediation, dashboard observability, audit logs, role permissions, and full-flow seed data.
- Replaced the blocked platform mediation boundary task with a broader boundary-and-contract task to avoid duplicate active sources of truth.
- Updated the current feature roadmap to point at the admin module goal roadmap and task package.

---

## [2026-05-27] - Frontend Bundle Second Pass

### frontend
- Moved admin table component registration into an admin-only Element Plus plugin loaded by `AdminLayout`.
- Gated admin child route rendering until the admin Element Plus table plugin finishes registering.
- Routed Element Plus table/table-column modules and styles into the async admin plugin chunk instead of the eager `element-plus` chunk.

### docs
- Updated the active bundle task notes and current feature roadmap with final chunk metrics.

### test
- Ran `frontend\npm test`.
- Ran `frontend\npm run build`.
- Checked `frontend\dist\index.html` for admin table chunk `modulepreload`.
- Ran preview smoke for `/app/home`, `/admin/users`, and `/admin/products` with mocked admin API responses.

---

## [2026-05-27] - Admin Support Context Dashboard

### feat
- Replaced the reserved `/admin/support` placeholder with a frontend-only support context dashboard.
- Surfaced report, order/refund, governance, and search context from existing admin-owned APIs only.
- Kept mediation, admin chat, notifications, group governance, and abnormal-message lanes visibly blocked or missing.

### docs
- Updated the active admin support console task completion notes with verification results and residual limitations.

### test
- Ran `frontend\npm test`.
- Ran `frontend\npm run build`.
- Ran `git diff --check`.

---

## [2026-05-27] - Admin Support Console Scope Contract

### docs
- Added the admin support console scope contract, including lane ownership, reusable endpoints, missing endpoints, non-goals, and mediation blockage.
- Created the implementation-ready frontend-only support context dashboard task.
- Clarified that `/admin/support` is a frontend route and does not currently define an `/api/admin/support/**` namespace.

---

## [2026-05-27] - Admin Governance Action Consistency

### fix
- Added explicit admin governance allow-lists for user, product, report, and shop status mutations.
- Validated shop `status` / `reviewStatus` combinations and propagated the current admin reviewer through shop status updates.
- Aligned admin product, report, verification, and review-task action columns with supported backend transitions.

### docs
- Clarified admin API accepted values and added negative admin HTTP smoke examples for unsupported governance states.
- Recorded the state/action matrix in the active task completion notes.

### test
- Added backend regression coverage for invalid user, product, shop, and report governance transitions.

---

## [2026-05-27] - Admin Module Goal Task Scoping

### docs
- Scoped the admin governance action-consistency task into an execution-ready cross-cutting backend/frontend contract task.
- Added a documentation-only support-console contract task to keep `/admin/support` development bounded before implementation.
- Corrected current roadmap and mediation-task notes so platform mediation remains blocked on the missing `chat-mvp-scope.md` artifact, not merely on archived task status.

---

## [2026-05-27] - Message Center Interaction Corrections

### frontend
- Reworked message search date filtering into a compact trigger with a floating multi-select/range calendar.
- Centered and constrained the emoji/sticker panel so it no longer spills past the composer.
- Replaced the hard-to-hit recall button with a hover action rail beside message bubbles.
- Added hover timestamps for message bubbles.

### test
- Verified frontend unit tests and frontend production build.

---

## [2026-05-27] - Message Center UI Polish Follow-up

### frontend
- Removed uploaded image file names from image-message captions.
- Constrained the emoji/sticker panel width so it stays inside the composer area.
- Replaced two separate date inputs in message search with a single continuous date-range calendar.
- Localized and tightened the notification center UI copy.

### data
- Expanded message-center notification seed rows with Chinese order, review, and system examples.

### test
- Verified frontend unit tests, frontend production build, and targeted backend chat controller tests.

---

## [2026-05-27] - Message Center UI Swipe and Composer Polish

### frontend
- Reworked the message conversation list with clearer row hierarchy, active state, category tags, and swipe actions for pin, mute, and delete.
- Aligned self-message bubbles with the existing restrained message-center palette.
- Added emoji insertion and lightweight sticker sending from the composer.
- Updated quick replies to show only current-scenario presets plus user custom replies, with inline custom reply creation and deletion.

### data
- Added Zhang San custom quick-reply seed rows to `seed-chat-data.sql` for acceptance testing.

### test
- Verified frontend unit tests, frontend production build, and targeted backend chat controller tests.

---

## [2026-05-27] - Message Center P2 Experience Tools

### feat
- Added chat message search with keyword/time filtering, pagination, and deleted-conversation filtering.
- Added per-user conversation pin, mute, and soft-delete management.
- Added 2-minute sender-only message recall with recalled-message rendering metadata.
- Added auto-reply settings and automatic reply insertion with 24-hour per-conversation throttling.
- Expanded Zhang San message seed data with 16 conversations and 44 messages for unread, preview, search, and bubble-direction testing.

### frontend
- Rebuilt the message center with search, conversation action menu, differentiated left/right bubbles, stable conversation switching, and full list previews.
- Added categorized quick replies for buyer, seller, support, and custom scenarios.
- Added the auto-reply settings page and route.

### docs
- Updated chat API spec and HTTP smoke requests for P2 endpoints.
- Archived completed P2 message-center task documents.

### test
- Added backend controller coverage for message search, pin/mute/delete, recall, and auto-reply trigger behavior.
- Verified `backend\\mvnw.cmd test`, `frontend\\npm test`, and `frontend\\npm run build`.

---

## [2026-05-26] - Message Center P1 E-commerce Tools

### feat
- Added product card chat messages with product validation, nested product summaries, product detail sharing, and chat bubble rendering.
- Added order card chat messages with participant validation, nested order summaries, order contact actions, and chat bubble rendering.
- Added authenticated quick reply CRUD endpoints under `/api/chat/quick-replies`.
- Added seller quick reply panel in the message composer with default replies when no custom replies exist.
- Added quick reply API/store integration and click-to-fill composer behavior.

### backend
- Extended chat message persistence for `product_id` and `order_id` card-message references.
- Exposed order participant fields in order list items so the frontend can create the correct chat conversation before sending an order card.

### docs
- Updated message center roadmap, chat HTTP smoke collection, and chat API spec for P1 card messages and quick replies.
- Archived completed P1 task documents.

### test
- Added backend controller coverage for product card, order card, and quick reply flows.
- Verified backend tests, frontend tests, and frontend production build.

---

## [2026-05-26] - Message Center Acceptance Fixes

### fix
- Added seed unread chat counters and notification rows so red-dot/read-state behavior is visible in acceptance data.
- Moved notifications out of top-level app navigation and embedded them under the message center.
- Stabilized conversation switching by scrolling only the message thread container and removing thread/bubble entrance animations.

### test
- Added backend coverage for chat unread count and mark-read behavior.
- Verified backend tests, frontend tests, and frontend production build.

---

## [2026-05-25] - Message Center P0 Completion

### feat
- Added chat unread counts, conversation read-state tracking, and global message unread badges.
- Added image messages with Base64 fallback upload, image bubbles, failure placeholders, and full-screen preview.
- Added in-app notification module with unread counts, notification list, read/read-all actions, and order-status notification hooks.

### backend
- Extended chat conversation/message persistence for unread counts, read timestamps, message type, and media payloads.
- Added notification controller, service, mapper, entity, and schema table.
- Integrated order and mock-payment status transitions with best-effort notification delivery.

### frontend
- Updated message center store and view for unread synchronization and image messages.
- Added notification API module, Pinia store, route, and notification list page.
- Added desktop and mobile navigation badges for chat and notifications.

### docs
- Added chat and notification API specs.
- Updated chat and notification HTTP smoke collections.
- Archived completed P0 message-center task documents.

### test
- Sub-agents reported passing backend tests, frontend tests, and frontend build before final integration.
- Final integrated verification rerun is recorded in the task completion response.

---

## [2026-05-25] - System Architecture and Launch Preparation Roadmap

### docs
- Created comprehensive data management and privacy compliance guide (`docs/03-architecture/data-management-and-privacy.md`)
- Created performance and scalability guide with optimization roadmap (`docs/03-architecture/performance-and-scalability.md`)
- Created operations and deployment standards document (`docs/04-standards/operations-and-deployment.md`)
- Created launch preparation roadmap with 4-phase implementation plan (`docs/05-roadmap/current/launch-preparation-roadmap.md`)
- Updated documentation index to include new architecture and standards documents
- Updated stage and feature roadmaps to reflect launch preparation focus

### architecture
- Documented data storage strategy and browser storage limitations
- Defined privacy compliance requirements (PIPL, user rights, legal documents)
- Outlined performance baselines and known bottlenecks (recommendation engine, media storage, search logging)
- Established three-phase optimization roadmap (pre-launch, post-launch, scale-out)
- Specified security hardening requirements (HTTPS, JWT secrets, CORS, input validation)
- Defined deployment architecture with containerization and CI/CD enhancement

### standards
- Established pre-launch deployment checklist with security requirements
- Documented backup strategy, logging configuration, and monitoring setup
- Created operational procedures (deployment runbook, incident response, database migration)
- Defined compliance requirements (ICP filing, data residency, cost optimization)
- Added cost estimation for three growth phases (¥400/month → ¥1000/month → ¥3800/month)

### compliance
- Specified required legal documents (privacy policy, user agreement, cookie policy)
- Defined user rights implementation (data export, account deletion, consent logging)
- Outlined database schema additions for consent tracking and soft deletion
- Documented frontend changes for cookie consent and legal document pages

### roadmap
- Established 4-phase launch preparation plan (13 weeks total):
  - Phase 0 (P0, 2-3 weeks): Security hardening, privacy compliance, infrastructure, containerization
  - Phase 1 (P1, 3-4 weeks): Performance optimization, messaging features, user personalization
  - Phase 2 (P2, 3-4 weeks): Admin features, marketing (coupons, promotions), data analytics
  - Phase 3 (P3, 2-3 weeks): UI/UX improvements, frontend bundle optimization
  - Phase 4 (P0, 1 week): Full testing, operations documentation, compliance verification
- Defined parallel work streams and dependency relationships
- Estimated cloud service costs: ¥400/month (Phase 0-1) → ¥1000/month (Phase 2-3) → ¥1000-3800/month (post-launch)
- Identified high-risk items: Redis caching strategy, OSS migration, coupon concurrency, UI/UX requirements clarity

### agent-protocol
- Updated `CLAUDE.md` to require agents read system architecture constraints before starting tasks
- Added references to data management, performance, and operations documents in pre-task checklist
- Updated stage roadmap to reflect "launch preparation and engineering governance" phase
- Updated feature roadmap with current status of all major features (security, compliance, performance, admin, marketing, UI/UX)

---

## [2026-05-25] - Home and Explore UX Bundle Update

### feat
- Added campus scenario carousel to homepage with 6 scenario cards (dorm essentials, study tools, campus life, digital products, second-hand market, campus services)
- Implemented Stripe-inspired carousel design with gradient backgrounds and smooth animations
- Added desktop hover navigation menu to app header with category-based navigation
- Enhanced explore search shell with improved spacing and visual hierarchy
- Improved homepage stats network animation with better particle distribution

### frontend
- Created `HomeCampusScenarioCarousel.vue` component with responsive carousel (612 lines)
- Updated `AppHeader.vue` with hover navigation menu for desktop
- Enhanced `HomeStatsNetwork.vue` particle animation system
- Refined `ExploreSearchShell.vue` spacing and layout
- Updated `HomeView.vue` to integrate campus scenario carousel
- Added CSS utilities for carousel animations and transitions

### ux
- Carousel auto-advances every 4 seconds with smooth transitions
- Hover navigation reveals on desktop, mobile uses existing menu
- Scenario cards feature gradient backgrounds matching Youyu warm palette
- Responsive design: 3 cards on desktop, 2 on tablet, 1 on mobile
- Improved visual hierarchy with consistent spacing (56-80px block spacing)

### test
- Verified carousel auto-advance and manual navigation
- Verified hover navigation on desktop and mobile menu fallback
- Verified responsive behavior at 900px and 640px breakpoints
- All frontend tests passing

---

## [2026-05-25] - Chat seed data and store fixes

### feat
- Added chat seed data with 8 conversations and 24 messages for testing
- Seed data includes product inquiries, shop inquiries, and direct conversations
- Conversations between existing seed users (zhangsan, lisi, wangwu, zhaoliu)

### fix
- Fixed chat store API response data access (response.data.data → response.data)
- Corrected user IDs in seed data to match existing users (1001-1004)

### backend
- Added `seed/data-chat.sql` with idempotent INSERT statements
- Seed data loads only with profile `seed`
- Conversations span multiple days for realistic testing

### test
- Verified seed data loads without errors
- Verified conversations appear in messages view
- Verified message timestamps and ordering

---

## [2026-05-27] - Trade mobile primary action bar

### feat
- Added a reusable mobile-only trade action bar for cart, checkout, and payment flows.
- Cart mobile now keeps selected count, selected amount, and the `去结算` action fixed above the bottom navigation.
- Checkout mobile now keeps payable amount, fulfillment context, and the guarded `提交订单` action fixed above the bottom navigation.
- Payment mobile now keeps payable amount, payment/order status, and the payable-only mock payment action fixed above the bottom navigation.

### docs
- Completed and archived `docs/08-tasks/archived/trade-mobile-primary-action-bar.md`.

### test
- Head-agent verification after installing frontend dependencies: `npm test` passed with 7 test files and 30 tests.
- `npm run build` passed.

---

## [2026-05-27] - Cart and payment API spec alignment

### docs
- Added formal cart and payment API specifications for the current trade-flow endpoints.
- Split cart and payment HTTP smoke examples into dedicated `cart.http` and `payment.http` files.
- Corrected `order.http` to remove unsupported status-filter examples and include current order creation requirements.
- Archived the completed cart/payment API spec alignment task record.

---

## [2026-05-27] - Review entry and seed demo bridge

### feat
- Added an honest product-detail review entry that routes users to `/app/reviews/pending` without implying review eligibility before completed purchase validation.

### docs
- Updated the review HTTP validation asset with the seedbuyer completed-order path for exercising pending/product review behavior.
- Archived the review-entry task record with completion notes.

### test
- `backend\\.\\mvnw.cmd test` passed with 100 tests.
- Head-agent verification after installing frontend dependencies: `npm test` passed with 7 test files and 30 tests.
- `npm run build` passed.

---

## [2026-05-27] - Order detail dedicated experience

### feat
- Added `/app/orders/:orderId` as an authenticated buyer order-detail route with direct refresh support.
- Moved buyer-side order detail sections into `OrderDetailView.vue`, covering status, items, fulfillment, digital assets, payments, refunds, reports, and message entry points.
- Added mobile-stable primary actions for pay, cancel, receipt confirmation, offline confirmation, and refund actions while preserving `availableActions` gating.

### refactor
- Simplified `OrdersView.vue` back to list, metrics, filters, payment shortcut, and detail-route navigation.
- Preserved `/app/orders?orderId=` compatibility by redirecting legacy query links to the dedicated detail route.

### test
- Head-agent verification after installing frontend dependencies: `npm test` passed with 7 test files and 30 tests.
- `npm run build` passed.

### docs
- Archived the completed order-detail task record with implementation and verification notes.

---

## [2026-05-27] - Trade center live status dashboard

### feat
- Reworked `/app/trade` into a live transaction dashboard using the existing cart, order, and pending-review APIs.
- Added real counts for cart items, selected cart items, pending payment, pending receipt, refund in progress, completed orders, and pending reviews.
- Added prioritized next-action cards plus partial error, retry, loading, and all-empty states while preserving quick navigation to cart, orders, pending reviews, and my reviews.

### docs
- Completed and archived `docs/08-tasks/archived/trade-center-live-status-dashboard.md`.

### test
- Head-agent verification after installing frontend dependencies: `npm test` passed with 7 test files and 30 tests.
- `npm run build` passed.

---

## [2026-05-25] - Chat MVP Frontend Integration

### feat
- Integrated chat messaging frontend with backend API
- Implemented real-time message sending and receiving with 8-second polling
- Added "Contact Seller" button on product detail pages
- Added "Contact Shop Owner" button on shop detail pages
- Removed placeholder data and connected to live backend endpoints

### frontend
- Created API module: `api/modules/chat.js` with 4 endpoint functions
- Created Pinia store: `stores/chat.js` with conversation and message management
- Transformed `MessagesView.vue` to use real API data
- Updated `ProductDetailView.vue` with conversation creation on "Contact Seller"
- Updated `ShopView.vue` with conversation creation on "Contact Shop Owner"
- Implemented polling logic: starts on conversation open, stops on conversation close
- Added loading states with skeleton screens
- Added error handling with ElMessage notifications

### api
- `getConversations(params)` - Fetch conversation list with pagination
- `createConversation(data)` - Find or create conversation (idempotent)
- `getMessages(conversationId, params)` - Fetch messages with pagination
- `sendMessage(conversationId, data)` - Send text message

### store
- State: conversations, activeConversationId, messages, loading, sending
- Actions: fetchConversations, findOrCreateConversation, fetchMessages, sendMessage
- Polling: startPolling (8s interval), stopPolling
- Message order handling: backend returns descending, frontend reverses to ascending

### ux
- Empty message validation (cannot send empty messages)
- Send button shows loading state during message send
- Messages auto-scroll to bottom on new message
- Conversation list shows formatted timestamps (HH:mm, "昨天", MM-DD)
- Mobile responsive: list/detail view switching
- Desktop: auto-select first conversation on load

### integration
- Product detail page: "Contact Seller" creates conversation with (sellerId, productId, null)
- Shop detail page: "Contact Shop Owner" creates conversation with (ownerId, null, shopId)
- Both redirect to `/app/messages` after conversation creation
- Error handling: shows user-friendly messages on API failures

### constraints
- MVP scope: No unread counts (backend doesn't provide), no WebSocket (polling only)
- Polling only active when conversation is open
- Category filtering not yet implemented (all conversations show in "trade" category)
- Group chat UI placeholder only (not functional)

---

## [2026-05-25] - Chat MVP Backend Implementation

### feat
- Implemented chat messaging backend MVP with conversation management and text messaging
- Added two database tables: `chat_conversations` and `chat_messages`
- Created complete backend architecture: entities, mappers, services, and controllers
- Implemented 4 REST API endpoints:
  - GET `/api/chat/conversations` - List user conversations with pagination
  - POST `/api/chat/conversations` - Find or create conversation (idempotent)
  - GET `/api/chat/conversations/{id}/messages` - Get messages with pagination
  - POST `/api/chat/conversations/{id}/messages` - Send text message
- Added conversation types: `direct`, `product_inquiry`, `shop_inquiry`
- Implemented permission checks (only participants can access conversations)
- Added automatic `last_message_at` update on message send
- Idempotent conversation creation with unique constraint on (user_a_id, user_b_id, product_id, shop_id)

### backend
- Entity classes: `ChatConversation.java`, `ChatMessage.java`
- Mapper interfaces: `ChatConversationMapper`, `ChatMessageMapper`
- Mapper implementations: `JdbcChatConversationMapper`, `JdbcChatMessageMapper`
- Service: `ChatService` interface + `ChatServiceImpl`
- Controller: `ChatController` with DTOs (`CreateConversationRequest`, `SendMessageRequest`)
- Database schema: Added foreign keys to users, products, shops tables
- Indexes: Composite indexes on (user_a_id, last_message_at) and (user_b_id, last_message_at)

### test
- Created comprehensive integration test suite: `ChatControllerTest` with 14 test cases
- All tests passing (100/100 tests in full suite)
- Test coverage: conversation creation, idempotency, message sending, pagination, permission checks, validation
- HTTP smoke tests: `docs/06-http/chat.http` with 14 request examples

### docs
- Task document: `docs/08-tasks/archived/chat-mvp-backend-implementation.md`

### constraints
- MVP scope: Text messages only, no WebSocket, no unread counts, no file attachments
- Message body max length: 2000 characters
- Pagination limits: conversations (max 50/page), messages (max 100/page)

---

## [2026-05-25] - Messages UX redesign

### feat
- Redesigned messages center with warm color system (warm orange #EA580C, warm white #FFFBF5, paper #FAFAF9)
- Removed router dependency for conversation switching to prevent page scroll-to-top issue
- Implemented local state management for conversation selection
- Added smooth transition animations (280ms for conversation switching, 160ms for hover)
- Increased spacing and breathing room following ui-ux-constitution.md guidelines
- Applied gradient backgrounds to message bubbles (warm orange gradient for self messages)
- Simplified page structure by removing Hero, Entry Context, and bottom explanation sections
- Enhanced visual hierarchy with proper border radius system (16-20px cards, 18px inputs)
- Improved responsive design with proper mobile/tablet breakpoints (900px, 640px)

### fix
- Fixed page jumping to top when clicking conversations (removed router.push, using local state)
- Fixed visual hierarchy and spacing issues (increased block spacing to 56-80px)
- Fixed lack of warmth in color palette (replaced gray tones with warm colors)

### docs
- Task document: `docs/08-tasks/drafts/messages-ux-redesign.md`

### test
- Dev server running at http://localhost:5173
- Verified conversation switching without page scroll
- Verified warm color system application
- Verified spacing and breathing room
- Verified message bubble gradients
- Verified smooth animations
- Verified responsive behavior at 900px and 640px breakpoints

## [2026-05-24] - Home campus scenario carousel and hover navigation

### feat
- Added a desktop hover/focus-reveal homepage navigation using the existing app header routes and mobile menu fallback
- Added a Stripe-reference-inspired campus scenario carousel after the platform-data section and before the recommendation rail

### docs
- Archived `docs/08-tasks/archived/home-campus-scenario-carousel-and-hover-nav.md` with completion notes

### test
- Ran `frontend\npm test`
- Ran `frontend\npm run build`
- Ran a Playwright local-preview smoke check for `/app/home` at desktop and mobile widths

## [2026-05-24] - Home quick entry card buttons

### feat
- Made the three homepage quick-entry cards full-card navigation buttons instead of requiring users to click the smaller action text
- Preserved the existing card layout and added a visible keyboard focus state

### docs
- Archived `docs/08-tasks/archived/home-quick-entry-card-buttons.md` for this feedback pass

### test
- Ran `frontend\npm test`
- Ran `frontend\npm run build`
- Ran a Playwright local-preview smoke check for full-card button rendering and exploration-card navigation

## [2026-05-24] - Home platform data animation tuning

### fix
- Tuned the homepage platform-data Canvas so the student network emits straight lines from one shared point
- Tuned the covered-region scene so network arcs start from one shared left-side hub while preserving the globe fan structure
- Slowed shop-scene moving particles by 50% and restored persistent static endpoint dots on both sides of each line

### docs
- Archived `docs/08-tasks/archived/home-platform-data-origin-and-speed-tuning.md` for this feedback pass

### test
- Ran `frontend\npm test`
- Ran `frontend\npm run build`
- Ran a Playwright local-preview smoke check for four metric tabs and nonblank Canvas rendering

## [2026-05-23] - CI pull request checkout hardening

### fix
- Updated CI checkout steps to use the pull request head SHA explicitly, avoiding a Playwright smoke job failure when GitHub Actions could not fetch the temporary PR merge ref

### test
- Inspected failed GitHub Actions run `26327502015`
- Ran `git diff --check`

## [2026-05-23] - Home platform data visual polish

### fix
- Removed extra static Canvas anchor particles from the homepage platform-data network scenes
- Removed the product-scene fixed endpoint rows and horizontal guide-line treatment so the stage no longer reads as having repeated baseline lines
- Adjusted the student scene to use straight network segments, removed the shop scene's fixed endpoint/throat particles, and restored the product scene's upper/lower endpoint rows
- Softened the platform-data section boundary with a shared warm background gradient and Canvas edge fades
- Loosened the authenticated-student network and transition cluster so particles no longer gather into a tight center ring
- Reworked the covered-region scene toward a Stripe-like hemisphere fan with a left-side connection hub while keeping the warm Youyu palette

### docs
- Archived `docs/08-tasks/archived/home-platform-data-visual-polish.md` for the feedback polish pass

### test
- Ran `frontend\\npm test`
- Ran `frontend\\npm run build`
- Ran Playwright visual smoke checks for the desktop Canvas themes, mobile static rendering, and reduced-motion static rendering

## [2026-05-23] - API spec recommend and shop completion

### docs
- Added `docs/09-api-spec/recommend.md` covering home and also-bought recommendation endpoints
- Added `docs/09-api-spec/shop.md` covering public shop detail, shop insight, current-user shop, shop applications, and shop-hosted review endpoints
- Added `docs/06-http/shop.http` as the dedicated shop validation collection
- Updated API spec indexes and current roadmap status so formal API specs are complete for current modules; future spec maintenance is deferred until UI/UX decisions create endpoint changes
- Archived `docs/08-tasks/active/api-spec-standardization-follow-up.md` after delivery

## [2026-05-23] - Home platform data interaction refinement

### feat
- Refined the homepage `平台数据` metric labels into a connected strip with a sliding active highlight and active-cycle progress line
- Reworked `HomeStatsNetwork.vue` around a single morphing curve/particle field so themes share one underlying system and collapse into a center cluster before expanding into the next scene
- Added desktop pointer interaction for the Canvas network so nearby lines and particles respond to cursor movement without replacing the native cursor or drawing pointer rings
- Strengthened the four scene silhouettes with structural guide lines, especially the `覆盖地区` hemisphere grid and cross-region arcs

### docs
- Archived `docs/08-tasks/archived/home-platform-data-interaction-refinement.md` for the follow-up interaction pass

### test
- Ran `frontend\\npm test`
- Ran `frontend\\npm run build`
- Ran Playwright smoke checks for sliding metric highlight, center-collapse theme transition, pointer interaction, mobile static rendering, mobile overflow, and reduced-motion static rendering

## [2026-05-23] - Document reality calibration

### docs
- Restored current roadmap state so hot-search P3 is treated as completed instead of pending, matching archived task records and current code
- Corrected database baseline docs to state MySQL is the local/dev runtime database and H2 is test-only
- Updated API specification planning status: formal specs are in progress with `recommend` and `shop` remaining, and activated the follow-up task for those modules

### test
- Ran `backend\\mvnw.cmd -Dtest=YouyuBackendApplicationTests#searchSuggestionUsesLogPrefixAndDoesNotCreateExtraLogs+searchSuggestionHonorsGovernanceAndPinnedOrdering test`

## [2026-05-23] - Home platform data network stage

### feat
- Replaced the homepage `平台数据` card grid with four clickable metric labels and a full-width Canvas stage for the campus transaction network
- Added `frontend/src/components/home/HomeStatsNetwork.vue` with native Canvas 2D themes for authenticated students, campus shops, listed products, and covered regions
- Added 10-second metric auto-rotation, click-to-switch behavior, and static Canvas rendering for mobile and reduced-motion states

### docs
- Archived `docs/08-tasks/archived/home-platform-data-network-stage.md` as the task record for the homepage platform data redesign

### test
- Ran `backend\\mvnw.cmd test`
- Ran `frontend\\npm test`
- Ran `frontend\\npm run build`
- Ran Playwright smoke checks for desktop canvas animation, metric click switching, mobile static rendering, mobile overflow, and reduced-motion static rendering

## [2026-05-23] - Repository-wide project rename to Youyu

### chore
- Renamed the project identity across repository docs, frontend app branding, backend metadata, environment titles, migration notes, and archived task/history references so the repo consistently uses `Youyu`
- Renamed the backend Java package/application entry points to `com.youyu.backend.YouyuBackendApplication`, including Maven main-class configuration and Spring test references
- Updated runtime naming defaults such as datasource/schema names, Spring application identifiers, JWT dev secret labels, local storage keys, and CI database configuration to the `youyu` naming scheme

### test
- Ran repository-wide search verification for legacy project-name variants with zero remaining hits

## [2026-05-23] - CI smoke alignment and JWT profile guard fix

### fix
- Tightened `backend/src/main/java/com/youyu/backend/config/JwtSecretGuard.java` so the development JWT secret is allowed only when all active profiles are from the safe set, preventing mixed profile combinations like `dev,prod` from bypassing the startup guard
- Added a mixed-profile regression case in `backend/src/test/java/com/youyu/backend/config/JwtSecretGuardTest.java`
- Added a stable `data-testid` to homepage featured product cards in `frontend/src/components/home/HomeFeaturedRail.vue` and aligned `frontend/e2e/smoke.spec.js` to assert against that stable hook instead of brittle presentational class names

### test
- Ran `backend\\mvnw.cmd test`
- Ran `frontend\\npm test`
- Ran `frontend\\npm run build`
- Ran `npx playwright test e2e/smoke.spec.js --project=api-smoke -g "frontend home page loads and renders products"`

## [2026-05-23] - Explore sticky condensed search shell

### feat
- Reworked `frontend/src/components/explore/ExploreSearchShell.vue` into a segmented pill-style search and filter shell with a dedicated search action, inline category/type chips, and a stronger visual match to the browse-first discovery pattern
- Added a sticky condensed state in `frontend/src/views/app/ProductListView.vue` so the explore search shell stays pinned under the app header and smoothly compresses while scrolling

### test
- Ran `npm test`
- Ran `npm run build`

## [2026-05-22] - Home featured rail centered loop interaction

### feat
- Reworked `frontend/src/components/home/HomeFeaturedRail.vue` so wheel and arrow navigation now move the homepage featured rail directly between centered cards instead of allowing free intermediate horizontal scroll states
- Converted the featured rail into a seamless loop using duplicated card buffers, keeping the active card in a centered presentation without visible start or end boundaries
- Removed desktop drag-scroll behavior from the rail so the interaction stays in discrete centered states and preserves the existing motion styling between cards

### test
- Ran `npm test`
- Ran `npm run build`

## [2026-05-22] - Explore infinite scroll and bookmark rail

### feat
- Reworked `frontend/src/views/app/ProductListView.vue` from explicit pagination into an infinite browse flow that accumulates paged API results locally, auto-loads additional items near the bottom, and keeps existing filter/query behavior intact
- Added a desktop-side custom bookmark rail on the explore page with progress feedback, double-click save, and click-to-restore behavior so long browsing sessions can resume from a marked position

### docs
- Archived `docs/08-tasks/active/explore-infinite-scroll-bookmark-ux.md` as a completed task record after implementation

### test
- Ran `npm test`
- Ran `npm run build`

## [2026-05-22] - Explore page UX redesign

### feat
- Reworked `frontend/src/views/app/ProductListView.vue` into a browse-first explore surface with a full-width results flow, compact filter summary bar, and conditional featured shops above the grid
- Rebuilt `frontend/src/components/explore/ExploreSearchShell.vue` into a four-row discovery control with inline category/type chips, compact history and hot-search hints, and an active-filter bar without hero prose or nested cards

### docs
- Archived `docs/08-tasks/active/explore-page-ux-redesign.md` as a completed task record after implementation

### test
- Ran `npm test`
- Ran `npm run build`

## [2026-05-22] - Home featured rail redesign

### feat
- Rebuilt `frontend/src/views/app/HomeView.vue` into a lighter three-part homepage with an editorial hero, inline hot-search chips, a horizontally scrolling featured rail, and a compact trust strip
- Added `frontend/src/components/home/HomeFeaturedRail.vue` with variable-width lead/accent cards, full-bleed imagery, bottom gradient overlays, shimmer loading skeletons, and hover motion tuned to the existing design tokens
- Simplified homepage data loading to use only `recommendStore.loadHomeRecommend(8)` and `searchStore.loadHotKeywords()`, while preserving the existing search suggestion flow and product/explore navigation contracts

### docs
- Archived `docs/08-tasks/active/home-featured-rail-redesign.md` as a completed task record after implementation

### test
- Ran `npm test`
- Ran `npm run build`

## [2026-05-22] - Non-UI/UX task spec refinement for sub-agent dispatch

### docs
- Rewrote 5 existing non-UI/UX task specs to a stricter dispatch-ready structure adding **Pre-flight Verification**, **Hard Limits**, concrete numbered **Implementation Steps**, exact **Test Plan commands**, and a mandatory **Final Report Format** the sub-agent must produce verbatim. Verification is now checkable from the agent's return report alone.
  - `active/api-spec-report-module-standardization.md`
  - `active/chat-mvp-scope-definition.md`
  - `active/frontend-bundle-second-pass-planning.md`
  - `active/platform-mediation-boundary-definition.md` (kept blocked on chat-mvp; pre-flight step 1 fails-fast if dependency unsatisfied)
  - `drafts/api-spec-standardization-follow-up.md` (narrowed scope to `recommend` + `shop` modules; `report` carved out to its own task)
- Split `architecture-performance-hardening` Wave 2 into two dispatchable child active tasks following the wave-1 pattern:
  - `active/product-search-path-hardening.md` (Slice D — composite indexes + ADR for substring-search tradeoff; SQL bodies must stay byte-identical)
  - `active/configuration-safety-hardening.md` (Slice F — env-var-first JWT secret + profile-aware startup guard; dev default preserved)
- Added a "Children Tasks" pointer block to the parent draft so dispatch order stays traceable

### task-doc convention introduced
- Every refined/new task now ends with a "Final Report Format" block. The sub-agent must paste this back filled-in with: branch+commit, pre-flight findings, per-step evidence, test command exit codes, acceptance-criteria check, deviations, out-of-scope findings, and open questions/blockers. Reviewer can verify each acceptance bullet against the report without re-running everything.

---



### docs
- Cross-referenced every file in `docs/08-tasks/active/` and `docs/08-tasks/drafts/` against `CHANGELOG.md` and the actual codebase to surface tasks that the migration left out of sync
- Removed four pre-completion duplicates whose authoritative completed versions already lived in `archived/` (drafts/active copies were stale migration leftovers, not new work): `active/comment-completeness-sprint.md`, `active/order-after-sales-ux-hardening.md`, `drafts/test-foundation-expansion.md`, `drafts/ui-redesign-shell-navigation-foundation.md`
- Moved `user-facing-enum-label-normalization.md` from `active/` to `archived/` after verifying delivery: `frontend/src/components/trade/trade-meta.js` provides `getOrderStatusMeta()` and `getPaymentStatusMeta()`, consumed by `OrdersView.vue` and `PaymentView.vue`; `SellerProductsView.vue` uses an inline `statusLabel()` covering its distinct seller-side statuses
- Updated `docs/08-tasks/drafts/architecture-performance-hardening.md` to mark Slices A/B/C/E as delivered (children archived) and flag Slices D (product search path) and F (JWT secret default) as still open

### verified-but-unchanged
- Remaining `active/` tasks confirmed still incomplete (kept in place): `admin-governance-action-consistency`, `api-spec-report-module-standardization` (HTTP collection aligned, formal spec `docs/09-api-spec/report.md` still missing), `chat-mvp-scope-definition`, `frontend-bundle-second-pass-planning`, `platform-mediation-boundary-definition` (blocked on chat MVP), `preference-theme-capability-gap` (theme radio rendered but `styles/variables.css` has no dark-mode tokens), `review-entry-and-seed-flow-bridge` (no review entry on product detail; seed orders all `pending_payment`)
- Remaining `drafts/` task `api-spec-standardization-follow-up` still relevant — `docs/09-api-spec/` is missing `report`, `recommend`, and `shop` module specs

---


- `CLAUDE.md`: expanded backend directory tree to sub-package level; expanded frontend `components/` to reflect actual subdirectories (`explore/`, `shell/`, `trade/`); corrected `/app/*` route count from 16 to 19 and `/admin/*` from 9 to 10; added route guard detail (`public` meta, `?redirect=` behavior); expanded backend Key Conventions (exception types, new module checklist, `@LoginRequired` usage, `ApiResponse` return pattern); expanded frontend Key Conventions (normalization boundary, error-utils, Element Plus constraint, route meta shape); updated `docs/06-http/` and `docs/09-api-spec/` module lists to reflect actual files; added frontend unit test step to post-task checklist
- `backend/README.md`: rewritten from early scaffold description to current state — MySQL setup, full command reference, seed data layout, auth/mock token usage
- `frontend/README.md`: rewritten from scaffold stub to current directory structure, component inventory, and key conventions
- `tests/README.md`: replaced placeholder with actual test distribution (backend `src/test/`, frontend `__tests__/`, E2E `tests/e2e/`)
- `docs/README.md`: updated `09-api-spec` module list from 3 to 7 entries; removed deleted `ui-improvement-spec` from `03-architecture` description
- `migration-notes/README.md`: added new file explaining the directory is pre-migration historical archive with broken paths — not current reference material

### removed
- `docs/03-architecture/ui-improvement-spec.md`: deleted — file encoding was corrupted (GBK/UTF-8 mismatch), content unreadable by agents; described UI improvements that have since been implemented

### archived
- `docs/08-tasks/drafts/issue.md` → `archived/`: raw bug/issue notes, already decomposed into concrete active tasks
- `docs/08-tasks/active/execution-wave-plan-2026-05-20.md` → `archived/`: planning meta-task, wave breakdown already executed
- `docs/08-tasks/active/agent-prompts-wave-plan-2026-05-20.md` → `archived/`: agent dispatch prompts for the same wave, no longer active

---

## [2026-05-20] - Seller publish loading diagnosis and fix

### fix
- Backend: Added missing `findOrderContextByOrderItemId` implementation in `JdbcReviewMapper` (backend wouldn't compile — root cause of all API unavailability)
- Frontend (market store): `loadProducts()` now derives `categories` from loaded product data via `categoriesFromProducts()`, keeping categories in sync with the API
- Frontend (SellerPublishView): Removed stale `!marketStore.categories.length` guard that prevented `loadProducts()` from ever running on the publish page (guard was always false due to hardcoded DEFAULT_CATEGORIES)

### diagnosis
- `SellerPublishView.vue` has no component-level loading state — it renders unconditionally; the "loading hang" was the page showing stale defaults while API calls silently failed
- Separated three converging causes: backend compilation error (server-side), `loadProducts` not updating categories (data gap), and guard preventing API call (frontend state)

---

## [2026-05-19] - Comment completeness sprint (W1-W8)

### docs
- Added 48 WHY-type comments across 5 backend service/mapper files: ReviewServiceImpl (denormalized rating cache, defensive JDBC unwrap, MVP distribution TODO), JdbcReviewMapper (anti-join semantics, COALESCE NULL handling, LEFT JOIN graceful degradation), PaymentServiceImpl (state machine constraints, idempotency guards, dual-state sync, fulfillment branching), OrderServiceImpl (25 locations — order lifecycle state machine, address snapshot, offline double-confirm, digital asset access timeline, action builder contract, refund policy, ensureProductPurchasable conditions), TransactionDataStore (PersistentMap auto-persist trap, serial number format, seed data dev-only warning, silent JSON degradation)
- Added 50 JSDoc blocks across 8 frontend files: all exported functions in stores (market 24, review 8, recommend 2, auth 4) and utils (market-normalizers 4, auth 4, storage 3, error-utils 1) documented with @param / @returns / @sideEffects
- Added `docs/08-tasks/active/comment-completeness-sprint.md` — parallel work item spec for the sprint

### principle
- Comments explain WHY, not WHAT — design intent, non-obvious constraints, business rules, SQL gotchas
- Self-documenting code left as-is; only non-obvious behavior documented
- Pure comment changes, zero logic modifications

---

## [2026-05-19] - UI follow-ups and brand asset organization

### fix
- Restored UTF-8 Chinese copy in `ProfileView.vue` (encoding corruption replaced labels with `?`)
- Fixed Mojibake navigation labels in `CHANGELOG.md` (首页 / 探索 / 消息 / 交易 / 我的)

### docs
- Added the shell navigation foundation draft and organized brand asset guidance under `resources/assets/brand/`

---

## [2026-05-19] - UI/UX copy polish: replace meta-descriptive text with user-facing content

### changed
- HomeView: hero title, description, section titles, trust items, guide cards all rewritten from design-document language to natural product copy
- TradeView: rewritten metrics, description, and overview cards from implementation jargon to user-relevant navigation
- OrdersView: metrics helper text and page description updated to user language
- CartView: page description and empty state text updated
- MessagesView: placeholder conversations rewritten with realistic chat content; group descriptions, input area hints, and section titles humanized
- ProfileView: completely rewritten to a clean identity + stats + quick-links structure without reserved-metric components
- SettingsView: developer badges ("已迁移结构", "复用偏好页", "待后端字段") replaced with user-friendly labels; alert descriptions humanized
- PreferenceSettingsView: three PageSection descriptions rewritten
- ProductDetailView: three PageSection descriptions and ElMessage texts updated
- ShopView: six PageSection descriptions, button labels ("预留" removed), formatMoney/formatMetric fallback changed from "待接入" to "--"
- SellerProductsView: page description rewritten
- SellerPublishView: page description rewritten
- PendingReviewsView: page description rewritten

### principle
- All user-visible text must serve the user, not explain design decisions
- "待接入" replaced with "--" for data placeholders
- "(预留)" removed from disabled button labels
- Developer-facing status tags in admin views intentionally kept as-is

---

## [2026-05-19] - Code quality cleanup sprint (W1-W9)

### fix
- Removed dead code: `toKeywordSet()` in SearchServiceImpl, `boolOrNull()` in JdbcSearchGovernanceMapper
- Fixed `bool()` null default from `true` to `false` in JdbcSearchGovernanceMapper (NULL is_active no longer silently enables governance rules)
- Unified `update()` method contract in JdbcSearchGovernanceMapper to accept only camelCase keys
- Replaced SQL `.formatted()` anti-pattern with safe string concatenation in JdbcRecommendMapper
- Removed 5 redundant response field aliases from JdbcRecommendMapper.toApiMap() (price, cover, coverUrl, type, publishedAt)
- Added try/catch isolation to `recordKeywordSearch` so DB failures don't block search responses
- Added `@Transactional` to create/update/delete governance rule methods in SearchServiceImpl

### refactor
- Extracted shared `MapperTypeConverters` utility class (string, toInt, toLong, toDouble, first) — eliminates duplicated helpers across 4 Mapper files
- Merged duplicate query method pairs in JdbcSearchLogMapper (findRecentDailyAggregates, findTopKeywordsForRecentWindow) into shared internal methods
- Extracted `createAsyncAction` helper for frontend Pinia stores — unified loading/error pattern across search, review, and recommend stores
- Unified all frontend error messages to Chinese

### docs
- Added `docs/04-standards/code-quality-standards.md` — coding standards covering comments, Mapper patterns, Service resilience, Store async patterns, dead code, SQL safety, boolean defaults, API response fields, and review checklist
- Added `docs/08-tasks/drafts/code-quality-cleanup-sprint.md` — detailed work item breakdown for the cleanup sprint

### test
- Frontend store tests compatible with refactored async pattern (exported names preserved)

## [2026-05-18] - Home and explore UI/UX redesign

### changed
- Reworked `HomeView` into a light platform-introduction entry with a focused hero, explore-first CTA path, platform metrics, trust guidance, hot-search preview, and buyer/seller onboarding instead of a dominant full product list
- Reworked `ProductListView` into the main explore surface for both `/app/explore` and the compatible `/app/products` route, keeping the existing route-driven keyword, category, product-type, and pagination flow intact
- Added shared explore UI components for the redesign: `ExploreSearchShell`, `ExploreProductCard`, and `FeaturedShopsSection`
- Upgraded `SearchSuggestInput` and `HotSearchList` presentation so search suggestions, focus elevation, and hot-keyword chips feel like one warm shared surface rather than a plain form control
- Kept all data access on the existing API modules and Pinia stores; no backend API contract changes were introduced

### test
- Ran `npm test`
- Ran `npm run build`
- Performed browser-based manual UI checks with mocked API responses for home, explore, search suggestions, category filtering, featured shops, empty state, error state, and mobile overflow safety

### deferred
- Explore and home visual quality still depends on future richer real product images; the current redesign stabilizes image containers and crop behavior so imperfect source images remain usable
- Featured shop richness still depends on future backend/shop profile depth; for now it is compatibly derived from existing public product and shop fields without adding new endpoints
# Changelog

All notable changes to this project are documented here. AI agents prepend a new block at the top after each completed iteration.

## [2026-05-17] - Profile settings and shop identity redesign

### feat
- Rebuilt `ProfileView` as an identity-and-summary page that separates personal identity, trade/favorite/review summaries, non-owner growth guidance, and owner-facing shop management from account settings
- Connected the frontend to the existing `GET /api/shops/mine` endpoint so owner-state handling uses current backend shop data instead of long-term frontend hardcoding
- Reworked `SettingsView` into a real settings center with sections for preferences, addresses, security, notifications, privacy, and logout
- Refreshed `PreferenceSettingsView`, `VerificationView`, `SellerProductsView`, and `SellerPublishView` so they align with the new `/app/me`, `/app/settings`, and `/app/shop/manage/*` information architecture

### refactor
- Added `normalizeShop()` plus address/verification compatibility cleanup in `market-normalizers.js`
- Extended `marketStore` with `ownedShop` state and `loadMyShop()` to share shop-owner context across profile, settings, and seller-management pages
- Switched primary seller-entry navigation inside the updated views from legacy `/app/seller/*` paths to `/app/shop/manage/*` while keeping router compatibility intact

### test
- Ran `npm run test`
- Ran `npm run build`
- Manually verified with browser automation against local frontend/backend that `/app/me`, `/app/settings`, `/app/settings/preferences`, `/app/verification`, `/app/shop/manage/products`, and `/app/shop/manage/publish` render with the expected new role-specific headings and actions

## [2026-05-17] - Messages center and admin support entry first-pass UI

### changed
- Rebuilt `frontend/src/views/app/MessagesView.vue` into a responsive message-center shell with trade, shop, support, and group categories, conversation detail placeholders, disabled composer state, and mobile list/detail switching
- Added future deep-link conventions from product, shop, and order flows into `/app/messages` using explicit route query context instead of pretending real chat already exists
- Added `frontend/src/views/admin/SupportView.vue` and `/admin/support` as the first restrained admin entry for customer support, after-sales assistance, group governance, and abnormal-message handling

### docs
- Archived `docs/08-tasks/drafts/ui-redesign-messages-support.md` as `docs/08-tasks/archived/ui-redesign-messages-support.md` with completion notes and follow-up backend/API requirements

### test
- Pending in this run: `npm run test`
- Pending in this run: `npm run build`
- Pending in this run: manual browser verification for `/app/messages` and `/admin/support`

## [2026-05-17] - Trade and reviews UI UX redesign

### changed
- Consolidated the cart, checkout, payment, orders, pending reviews, and my reviews pages into a clearer trade-center experience while keeping legacy routes such as `/app/cart`, `/app/orders`, and `/app/reviews/*` available
- Added trade-specific shared UI pieces for transaction navigation, status pills, order cards, review cards, and transaction metric strips so the trade flow now reads as one continuous loop instead of isolated pages
- Reworked high-risk transaction screens to keep explicit loading, duplicate-submit protection, empty/error feedback, illegal-state messaging, and visible payment, refund, report, receipt-confirmation, and review actions

### docs
- Archived `docs/08-tasks/drafts/ui-redesign-trade-reviews.md` as a completed trade-domain task record with implementation and verification notes

### test
- Ran `npm test`
- Ran `npm run build`

## [2026-05-17] - Product and shop detail UI UX redesign

### changed
- Rebuilt `ProductDetailView` into a stronger purchase-decision page with gallery-first product media, a sticky decision column, clearer transaction facts, visible seller and shop entry, and primary actions for buy, cart, favorite, and contact
- Rebuilt `ShopView` so the shop acts as the seller's public-facing subject, emphasizing shop introduction, selected owner identity, trust and trade signals, public goods, reviews, and explicitly reserved follow or group entry points
- Extended frontend product and shop compatibility shaping in `market-normalizers.js` and `market.js` so new presentation fields stay centralized instead of scattering raw backend fields across multiple detail templates
- Replaced unresolved `v-loading` usage in both detail pages with explicit skeleton loading states so runtime loading behavior no longer depends on a missing global directive

### docs
- Archived `docs/08-tasks/drafts/ui-redesign-product-shop-detail.md` as a completed task record with implementation notes and validation blockers

### test
- Ran `npm run test`
- Ran `npm run build`
- Manually verified public detail routes `/app/products/1` and `/app/shops/1` under frontend-only local runtime, confirming error-state rendering while the backend API was offline
- Confirmed that contact/message entry remains route-based and backend-safe; unauthenticated message access still stays behind route auth
## [2026-05-17] - UI redesign shell navigation foundation

### changed
- Added the warm Youyu design-token baseline with terracotta/orange primary colors, warm paper surfaces, glass, shadow, radius, and motion variables
- Updated the app shell header, mobile drawer, and mobile bottom navigation to match the 首页 / 探索 / 消息 / 交易 / 我的 information architecture
- Preserved legacy app route compatibility while introducing `/app/explore`, `/app/trade`, `/app/messages`, `/app/me`, and `/app/settings`

### test
- Ran `npm run test`
- Ran `npm run build`

## [2026-05-17] - Frontend redesign safety standard

### docs
- Added `docs/04-standards/frontend-redesign-safety.md` to control UI redesign risks around API modules, stores, normalizers, route compatibility, chat placeholders, shop-owner identity, transaction flows, and admin support scope
- Updated `docs/README.md` to list the frontend redesign safety standard under repository standards

## [2026-05-17] - Admin query pagination hardening

### refactor
- Replaced admin-side `findAll()` -> `stream().filter().sorted()` in-memory list assembly with SQL-backed filtering, sorting, counting, and pagination across all 6 target admin list endpoints (`/api/admin/users`, `/verifications`, `/products`, `/review-tasks`, `/shops`, `/reports`)
- Added `findXxxPaged` and `countXxx` methods to UserMapper, StudentVerificationMapper, ProductMapper, ProductReviewTaskMapper, ShopMapper, ReportMapper and their JDBC implementations
- Optimized admin dashboard to use COUNT queries instead of `findAll()` for metric cards and todo counts

### changed
- Admin list endpoints now accept `page` (default 1) and `pageSize` (default 10, max 100) query parameters
- Response contract changed from `{ items, summary }` to `{ items, total, page, pageSize }` for all 6 list endpoints
- Frontend admin pages now render `el-pagination` with page switching, size selection, and total display
- Removed in-memory summary tag breakdowns from admin list pages (replaced by pagination total)

### docs
- Updated `docs/06-http/admin.http` with pagination query examples
- Updated `docs/09-api-spec/admin.md` with page/pageSize parameters and response fields

## [2026-05-17] - Review order lookup hardening

### refactor
- Replaced full-order scan in `ReviewServiceImpl.submitProductReview()` with a single bounded JOIN query (`order_items` + `orders`) that resolves `orderItemId` -> `{orderId, buyerUserId, orderStatus, productId}` in one lookup
- Removed `findOrderByOrderItemId()` private method that iterated `listOrders()` -> `findOrderItems()` across all orders

### feat
- Added `ReviewMapper.findOrderContextByOrderItemId(Long)` for direct order-item -> order context resolution
- Implemented in `JdbcReviewMapper` as `SELECT o.id, o.buyer_user_id, o.order_status, oi.product_id FROM order_items oi JOIN orders o ON oi.order_id = o.id WHERE oi.id = ?`

### test
- Added `submitProductReviewNonexistentOrderItemReturnsNotFound` to verify 404 on non-existent order item
- Added `submitProductReviewForNonCompletedOrderFails` to verify BUSINESS_ERROR when the order is not completed

### handoff
- The direct lookup query `WHERE oi.id = ?` on `order_items.id` uses the PK index, so no additional index is needed for the query itself
- The JOIN on `orders.id` is also PK-covered, so no extra index is needed there either
- If future features need bulk order-item -> product resolution, consider `idx_order_items_order_product ON order_items(order_id, product_id)`, but the current single-item lookup is already optimal

## [2026-05-17] - Runtime index hardening

### feat
- Added `idx_orders_buyer ON orders(buyer_user_id, submitted_at, id)` to support buyer order listing (`WHERE buyer_user_id = ? ORDER BY submitted_at DESC, id DESC`)
- Added `idx_orders_shop_status ON orders(shop_id, order_status, payment_status, completed_at)` to support shop monthly insight aggregations over completed and paid orders
- Added `idx_order_items_order ON order_items(order_id, id)` to support order detail expansion (`WHERE order_id = ? ORDER BY id`)
- Added `idx_order_items_product ON order_items(product_id)` to support recommendation and hot-product joins on `product_id`
- Added `idx_payment_records_order ON payment_records(order_id, initiated_at, id)` to support order-scoped payment lookups (`WHERE order_id = ? ORDER BY initiated_at, id`)
- Added `idx_refund_records_order ON refund_records(order_id, applied_at, id)` to support order-scoped refund lookups (`WHERE order_id = ? ORDER BY applied_at, id`)
- Added `idx_reports_submitted ON reports(submitted_at)` to support report listing sort (`ORDER BY submitted_at DESC`)
- Added `idx_reports_status_submitted ON reports(status, submitted_at)` to support SQL-backed admin report pagination with status filtering and submitted-time ordering
- Added `idx_student_verifications_status_submitted ON student_verifications(verification_status, submitted_at, id)` to support SQL-backed admin verification pagination with status filtering and reverse submitted-time ordering
- Added `idx_student_verifications_user_submitted ON student_verifications(user_id, submitted_at, id)` to support the latest-verification correlated subquery used in SQL-backed admin user listing
- All indexes are additive and schema-compatible with both H2 and MySQL; `spring.sql.init.continue-on-error=true` keeps repeated initialization idempotent

### deferred
- No `idx_orders_seller` was added because no seller-scoped order listing query exists in the current code
- No dedicated `users(status, created_at)` style index was added because the current admin-user query cost is driven more by latest-verification lookup than by low-cardinality status filtering
- No `reports(target_type, submitted_at)` style index was added because target-type filtering is optional and less central than status-backed pagination
- No extra product / shop / review-task sort indexes were added because existing filter indexes plus limited page windows are sufficient for the current SQL-backed admin queries
- No search_logs index changes were needed because existing `idx_search_logs_created_at` and `idx_search_logs_normalized_keyword` already cover the current aggregate and prefix-filter patterns
- No cart_items indexes were added because that table is adjacent but outside this hardening slice

## [2026-05-17] - Product list request flow hardening

### fix
- Removed the avoidable bootstrap `loadProducts()` call in `ProductListView`; initial page load now performs a single route-driven request instead of fetching an extra unfiltered product page first
- Removed the `preserveCategories` workaround from `market.js` `setProducts()` / `loadProducts()`, since the old double-fetch pattern is gone

### refactor
- `setProducts()` in `market.js` now updates only product result state and no longer mutates category metadata as a side effect
- `DEFAULT_CATEGORIES` in the market store now act as the stable category-chip source for the product list, fully decoupling storefront filter chips from the currently loaded result set while preserving the single-request route-driven flow

## [2026-05-17] - Hardening task wave decomposition

### docs
- Added `docs/08-tasks/active/admin-query-pagination-hardening.md` as the admin-query modernization slice with explicit pagination contract, write-scope limits, and parallelization constraints
- Added `docs/08-tasks/active/review-order-lookup-hardening.md` as the focused review-path lookup task to remove full-order scan behavior without mixing in schema or frontend work
- Added `docs/08-tasks/active/runtime-index-hardening.md` as the schema-owned indexing slice for current runtime query paths
- Added `docs/08-tasks/active/product-list-request-flow-hardening.md` as the storefront-only request-flow cleanup slice
- Updated `docs/08-tasks/drafts/architecture-performance-hardening.md` with wave-1 decomposition and cross-task ownership rules so the hardening work can be distributed to parallel child agents safely

## [2026-05-17] - Architecture and performance hardening draft

### docs
- Added `docs/08-tasks/drafts/architecture-performance-hardening.md` to capture the mid-stage architecture and performance review as an execution-ready cross-cutting hardening task
- Documented the main hidden risks around admin full-table reads, review-path order scans, product search scalability, order-related indexing gaps, frontend product-list double fetch, and configuration safety defaults
- Split the proposed remediation into bounded implementation slices so later execution can stay targeted instead of turning into an unfocused broad refactor

## [2026-05-17] - UI/UX constitution draft

### docs
- Added `docs/03-architecture/ui-ux-constitution.md` to capture the redesigned Youyu visual direction, interaction rules, motion system, responsive principles, and AI execution constraints
- Added `docs/03-architecture/frontend-information-architecture.md` to define the new front-office page structure, route distribution, navigation principles, shop-owner identity model, messages entry, trade entry, and admin support area
- Added parallel UI redesign draft tasks for shell/navigation, home/explore, product/shop detail, trade/reviews, messages/support, and profile/settings/shop identity work
- Updated `docs/README.md` so the architecture document area explicitly includes UI/UX constitution guidance

## [2026-05-16] - test-foundation-expansion

### feat
- Added `BackendTestBase` shared test base class with common flow helpers (`addToCart`, `createOrder`, `initiateAndPay`) and search governance utilities
- Added `AdminGovernanceTest` (21 tests) covering admin dashboard, user management, verification review, product/shop management, search governance CRUD, and auth guards
- Added `CartEdgeCaseTest` (6 tests) covering quantity merge, update, remove, not-found, empty cart, and auth checks
- Added `PaymentEdgeCaseTest` (6 tests) covering non-existent order, cross-user payment, double initiation, bogus payment, gateway info, and auth checks
- Added `UserProfileTest` (8 tests) covering profile, verification status, addresses CRUD, default address, logout, and auth/me
- Added `app.test.js` store test (5 tests) covering keyword state, `hasKeyword` computed, and `collapsed` toggle
- Added E2E specs: `admin-governance.spec.js` (18 tests), `offline-order.spec.js` (1 full-flow test), `cart-edge-cases.spec.js` (4 tests)
- Updated `playwright.config.js` with three new project entries for new E2E specs

### refactor
- Extracted common test helpers from `YouyuBackendApplicationTests` into `BackendTestBase` to reduce duplication

### test
- Backend: 58 -> 78 tests (+20 new test methods, 0 failures)
- Frontend: 6 -> 7 test files (30 tests, all passing)
- E2E: 1 -> 4 spec files (21 -> 42 total tests)

### feat
- Order detail drawer: buyer-side report submission dialog with reason selection and description
- Order detail drawer: buyerNote display and payment records section (buyer + admin views)
- Responsive drawer width: 100% on mobile (<768px), fixed width on desktop (buyer 680px, admin 720px)

### fix
- Duplicate-submit protection: action buttons disabled with loading state during async operations
- Backend: duplicate refund rejected when pending/in-progress refund exists
- Backend: duplicate payment creation rejected for orders with active payment
- Backend: already-success payment rejects repeat mock-success calls
- Checkout: client-side validation for offline meet time format and required fields
- Payment: buttons disabled when order not in `pending_payment` status; post-payment navigation added

### docs
- Smoke tests: added duplicate refund, duplicate payment, duplicate mock-success cases to `order.http`
- Task spec: order-after-sales-ux-hardening moved from drafts to active, completion notes added

Format per block:
```
## [YYYY-MM-DD] - <iteration id or short description>

### feat
- ...

### fix
- ...

### refactor
- ...

### docs
- ...
```

## [2026-05-16] - Admin governance UI consistency

### feat
- Added `EmptyState` and `ErrorBlock` rendering to `ListPageShell` — all 8 admin list pages now show empty/error feedback instead of silent blank tables
- Created `frontend/src/utils/error-utils.js` — shared `resolveErrorMessage` utility, eliminating 8 copy-pasted copies

### refactor
- Refactored `OrderManageView` to use `ListPageShell` with summary/filters/table slots, matching the pattern used by all other admin list pages
- All 9 admin pages now import `resolveErrorMessage` from the shared utility instead of inline definitions

## [2026-05-16] - Reserved-state realism upgrade

### feat
- Created `ReservedMetricCard.vue` — reusable component that replaces blunt `待接入` text with metric label, scope definition, data source, and muted status badge; uses dashed-border muted card style to visually distinguish reserved metrics from live data
- Created `ReservedPanel.vue` — reusable component that replaces plain `placeholder-block` divs with structured icon + title + description layout for reserved content areas
- Updated `ShopView.vue` — 4 insight metric cards and hot-products panel now render `ReservedMetricCard` / `ReservedPanel` when `shopInsightReserved` is true, showing metric definitions and data sources instead of bare `待接入` text
- Updated `ProfileView.vue` — 4 user insight metric cards and 2 content panels (recent browses, favorite preferences) now render the new components when `userInsightReserved` is true
- Added `viewCountSummary` and `favoriteCountSummary` definitions to `shopInsightMetricDefinitions`

### refactor
- No backend changes; live data branches in both views remain unchanged; `el-alert` banners and status badges preserved

## [2026-05-16] - Tighten formal API specification workflow

### docs
- Updated `docs/09-api-spec/README.md` to separate runtime truth, formal spec, and `.http` validation layers, and to define the current hand-written maintenance workflow
- Expanded `docs/09-api-spec/API_SPEC_TEMPLATE.md` with fixed sections for response envelope, `ResultCode`/HTTP mapping, `BUSINESS_ERROR` semantics, field tables, HTTP asset mapping, and known drift tracking
- Added `docs/09-api-spec/user.md` and `docs/09-api-spec/admin.md` as new formal API spec samples aligned to current controllers and request DTOs
- Corrected direct method/path drift in `docs/06-http/product.http`, `docs/06-http/order.http`, and `docs/06-http/admin.http`
- Added `docs/08-tasks/drafts/api-spec-standardization-follow-up.md` to guide the next round of module-by-module API spec expansion

## [2026-05-16] - Hot search enhancement P3: suggestions and aggregation optimization

### feat
- Added `GET /api/search/suggest` for prefix-based search suggestions derived from recent `search_logs`
- Extended the frontend search store and search inputs on Home and Product List pages with debounced suggestion loading, local error fallback, and selection-driven navigation
- Kept search suggestions under the existing search governance model so hidden and blocked terms are filtered consistently with the hot-search ranking

### refactor
- Optimized hot-search aggregation by moving representative-keyword selection and daily aggregation into `SearchLogMapper`, reducing Java-side regrouping while keeping `/api/search/hot` response semantics unchanged
- Consolidated active governance-rule loading into one snapshot read before applying hide, block, and pin behavior

### test
- Added backend regression coverage for suggestion behavior, governed filtering, pinned ordering, and hot-search recency weighting
- Extended frontend search-store tests for suggestion loading, blank-input clearing, and stale-response protection
- Added a search-suggestion API smoke case to `frontend/e2e/smoke.spec.js`

### docs
- Updated `docs/06-http/search.http` with suggestion and governance validation examples
- Added `docs/09-api-spec/search.md` and linked it from the API spec index
- Archived the P3 task record with completion notes and verification summary

## [2026-05-16] - Frontend bundle optimization first pass

### refactor
- Replaced global `ElementPlus` registration in `frontend/src/main.js` with a local on-demand plugin that registers only the Element Plus components currently used by the project
- Removed the global `element-plus/dist/index.css` import and switched to component-level Element Plus style imports
- Lazy-loaded `AppLayout` and `AdminLayout` from the router so admin shell code is no longer bundled into the main entry by default
- Updated frontend views to consume shared `ElMessage` and `ElMessageBox` exports from the local plugin instead of importing services directly from `element-plus`

### docs
- Archived `docs/08-tasks/archived/frontend-bundle-optimization.md` as the first recorded delivery slice for this cross-cutting governance item
- Updated `docs/05-roadmap/current/feature-roadmap.md` to reflect that frontend bundle governance has moved from not-started to in-progress with a completed first pass

## [2026-05-16] - Add first formal API specifications

### docs
- Added `docs/09-api-spec/auth.md`, `product.md`, and `order.md` as the first formal API contract documents based on current controllers and request samples
- Expanded `docs/09-api-spec/API_SPEC_TEMPLATE.md` into a directly usable module-spec template instead of a minimal placeholder
- Updated `docs/09-api-spec/README.md` and `docs/README.md` so the API specification area now has concrete module entry points

## [2026-05-16] - Documentation system restructure and AGENTS introduction

### docs
- Reorganized `docs/` into `01-product` through `08-tasks`, separating product, requirements, architecture, standards, roadmap, API, decisions, and task records
- Replaced the old `docs/ROADMAP.md` semantics with `docs/04-standards/development-process.md` and added `docs/05-roadmap/stage-roadmap.md` plus `feature-roadmap.md`
- Added `Youyu/AGENTS.md` as the repository-wide AI execution guide without moving `CLAUDE.md`
- Introduced `docs/08-tasks/README.md`, `TASK_TEMPLATE.md`, archived task records, and draft-task storage for ongoing work such as hot-search P3
- Updated `CLAUDE.md`, `README.md`, `docs/README.md`, and related references to the new document paths

## [2026-05-16] - Roadmap folder cleanup

### docs
- Refined `docs/05-roadmap/` into `current/` and `archived/` so active roadmap files are separated from historical MVP planning artifacts
- Moved `stage-roadmap.md`, `feature-roadmap.md`, and `open-questions.md` into `docs/05-roadmap/current/`
- Moved `mvp-scope.md` and `mvp-database-cut.md` into `docs/05-roadmap/archived/`
- Added `docs/05-roadmap/README.md` and updated references across standards, task docs, AGENTS, and the docs index

## [2026-05-16] - Separate HTTP collections from API specifications

### docs
- Renamed `docs/06-api/` to `docs/06-http/` because the existing `.http` files are request collections rather than formal API specifications
- Added `docs/09-api-spec/` with a README and template for future real API contract documents
- Updated repository guidance and task/standards references so HTTP validation assets and formal API specs are no longer conflated

## [2026-05-16] - Tighten current roadmap to match actual project state

### docs
- Rewrote `docs/05-roadmap/current/stage-roadmap.md` to focus only on the current phase, next priorities, and active risks
- Rewrote `docs/05-roadmap/current/feature-roadmap.md` to track only still-relevant feature lines instead of repeating completed history
- Rewrote `docs/05-roadmap/current/open-questions.md` to keep only decisions that still affect upcoming work
- Clarified in `docs/05-roadmap/README.md` that completed history belongs in `archived/`, not `current/`

## [2026-05-15] - Hot search enhancement P2: admin governance and search pagination

### feat
- Added `search_governance_rules` table with SENSITIVE_WORD, STOP_WORD, HIDE_KEYWORD, and PIN_KEYWORD rule types
- Added admin CRUD endpoints for governance rules (`/api/admin/search/governance-rules`)
- Public hot-search ranking (`GET /api/search/hot`) now automatically filters blocked/hidden terms and applies pin ordering
- Added paginated search log browser endpoint (`/api/admin/search/logs`) for admin review
- Product search (`GET /api/products`) now supports server-side pagination via `page` and `pageSize` params, returning `{ items, total, page, pageSize }`
- ProductListView replaced IntersectionObserver infinite-scroll with `<el-pagination>` for page-number pagination
- Added admin HotSearchGovernView with rule CRUD, enable/disable toggle, and search log browser dialog

### test
- Backend integration tests: governance rules CRUD, HIDE_KEYWORD filtering on hot-search output (with cleanup), product search pagination metadata, and search log browser pagination
- Updated existing product-list tests and Playwright e2e smoke tests for the new paginated response shape

### docs
- Updated `docs/http/search.http` with paginated search, governance CRUD, and log browser examples

## [2026-05-15] - Product & shop review system

### feat
- Added `reviews` and `shop_reviews` tables with UNIQUE constraints preventing duplicate reviews
- Added `POST /api/reviews/products` and `POST /api/reviews/shops` for authenticated buyer review submission
- Added `GET /api/reviews/pending` listing completed-but-unreviewed order items
- Added `GET /api/reviews/mine` returning user's product and shop reviews
- Added `GET /api/products/{id}/reviews` and `GET /api/products/{id}/review-summary` public endpoints
- Added `GET /api/shops/{id}/reviews` and `GET /api/shops/{id}/review-summary` public endpoints
- Rating aggregates (`rating_score`, `review_count`) update synchronously on review submission
- Backend integration tests covering success, duplicate rejection, score validation, auth, ownership, and public access (12 tests)
- Frontend: `ReviewForm`, `ReviewList`, `RatingSummary` reusable components
- Frontend: `PendingReviewsView` and `MyReviewsView` pages with routing
- Frontend: `ProductDetailView` and `ShopView` now display review summaries and lists
- Frontend: Pinia `review` store with Vitest tests (5 tests)
- Added `docs/http/review.http` with endpoint examples

## [2026-05-15] - Product recommendations: cold-start, personalized, and also-bought

### feat
- Added `GET /api/recommend/home` public endpoint with hybrid cold-start/personalized recommendation
- Added `GET /api/recommend/also-bought/{productId}` public endpoint for co-purchase recommendations
- Cold-start strategy: popularity score = `view_count * 0.6 + sold_count * 10 * 0.4`
- Personalized strategy: fetches hot products from user's purchased categories, falls back to cold-start on empty history
- Also-bought strategy: `order_items` self-join counting co-occurrence in completed/paid orders
- Frontend HomeView now uses `recommendStore.homeRecommendList` instead of client-side product slicing
- Frontend ProductDetailView now shows "also-bought" recommendation section
- New `RecommendMapper` / `JdbcRecommendMapper` with three deterministic SQL queries
- New `RecommendService` / `RecommendServiceImpl` with hybrid strategy orchestration
- New `RecommendController` serving two public GET endpoints under `/api/recommend`

### test
- Added 5 backend integration tests: public cold-start, unavailable product exclusion, personalized for logged-in user, also-bought structure validation, also-bought empty result handling
- Added 4 frontend store tests: home recommend success/failure, also-bought success/failure
- Added `docs/http/recommend.http` with 8 request scenarios

### docs
- Created `docs/dev/tasks/RECOMMEND_FEATURE_DESIGN.md` as feature design document

## [2026-05-15] - Hot search enhancement P1: Chinese UI, encoding repair, filter exposure, and smoke tests

### feat
- Restored hot-search related UI copy to project-standard Chinese across HomeView, ProductListView, HotSearchList, search store, and market store fallback categories
- Exposed product type filter (digital/physical/service) in ProductListView alongside existing keyword and category filters
- Product type filter participates in route-query state so combined filter URLs remain shareable

### fix
- Repaired `CHANGELOG.md` encoding: replaced corrupted em dash sequences (`鈥?` → `- `) and cleaned up old entries

### test
- Added Playwright API smoke tests for hot search endpoint, keyword/category/productType filtering, combined filter query, and blank keyword handling
- Updated `docs/http/search.http` with productType and combined filter examples

### docs
- `HOT_SEARCH_ENHANCEMENT_P1.md` status updated to reflect completed delivery

## [2026-05-15] - Hot search enhancement planning documents

### docs
- Rewrote `docs/dev/新功能规划.md` as a clean UTF-8 master planning document for next-stage feature work
- Clarified that hot search v1 is complete and that enhancement work is split into P1, P2, and P3 task specs
- Added `HOT_SEARCH_ENHANCEMENT_OVERVIEW.md`, `HOT_SEARCH_ENHANCEMENT_P1.md`, `HOT_SEARCH_ENHANCEMENT_P2.md`, and `HOT_SEARCH_ENHANCEMENT_P3.md`
- Marked the archived role of `HOT_SEARCH_FEATURE_TASK.md` as the completed baseline task
- Updated `CURRENT_STAGE_WORK_PLAN.md` to unify completion markers and next-step pointers for completed and planned workstreams

## [2026-05-13] - MVP closure: MySQL full migration and automated smoke/CI pipeline

### feat
- Full MySQL 8 migration: replaced H2 runtime with `mysql-connector-j`, updated datasource config
- Schema adapted for MySQL: `CLOB` to `TEXT`, `CREATE INDEX IF NOT EXISTS` to conditional-safe MySQL equivalents, and `DROP INDEX IF EXISTS` to migration-safe handling
- Seed data adapted for MySQL: `MERGE INTO` replaced with `INSERT ... ON DUPLICATE KEY UPDATE`
- Java mappers adapted for MySQL-compatible date handling and upsert behavior
- Added Playwright smoke test suite covering public browse, digital purchase, logistics, admin review, report governance, error cases, and browser happy path
- Added GitHub Actions CI workflow and `test:e2e`, `test:e2e:ui`, and `test:all` npm scripts

### test
- Playwright API-level smoke tests run against the real backend with seed tokens
- Vitest excludes `e2e/**` from unit-test collection
- Backend tests continue to use in-memory H2 for fast CI execution

### refactor
- `JdbcSearchLogMapper.findRecentAggregates` computes the cutoff date in Java instead of dialect-specific SQL date arithmetic

## [2026-05-13] - Phase 3 feature 1: hot search and server-side keyword logging

### feat
- Added `search_logs` persistence with normalized keyword, optional user id, result count, and created time for product-search analytics
- Extended public `GET /api/products` to support backend keyword and category filtering while preserving the existing public browsing route
- Added public `GET /api/search/hot` with a 7-day weighted hot-keyword ranking
- Added a dedicated frontend search store, recent-search history, and a reusable hot-search list component
- Home page now shows hot keywords and supports direct keyword jumps into the product list
- Product list search is now route-query driven so keyword/category filters can be refreshed or shared reproducibly

### test
- Added backend integration coverage for keyword search, hot-search ranking, and public category filtering
- Added frontend Vitest coverage for hot-search loading and recent-search history behavior

### docs
- Refined `docs/dev/tasks/CURRENT_STAGE_WORK_PLAN.md` with a concrete hot-search task definition and recorded the completed delivery slice
- Added `docs/dev/tasks/HOT_SEARCH_FEATURE_TASK.md`
- Added `docs/http/search.http`

## [2026-05-13] - MVP closure: governance, insights, delivery, and tests

### feat
- Added user-side report submission through `POST /api/reports`, persisted through JDBC and visible in the existing admin report workflow
- Added persistent `user_preferences` storage with JDBC read/upsert support for `GET` / `PUT /api/users/me/preference`
- User insight snapshot now uses real completed-order aggregation for total spend, purchased item count, recent purchases, and category preference summary
- Shop insight snapshot now uses real completed-order and product aggregation for monthly sales, monthly orders, hot products, views, favorites, and repeat buyers
- Added `digital_access_logs` table and JDBC mapper for digital asset access audit persistence
- Added `accessDigitalAsset` to `OrderService`, validating buyer ownership, digital order type, receipt completion, and `full_access` status before granting access
- Added `GET /api/orders/{orderId}/assets/{assetId}/access` endpoint with server-side authorization
- Order detail now includes `digitalAccessLogs` array showing full access audit trail
- Added lightweight frontend report API module and authorized digital asset access API call

### fix
- Full digital assets now require backend-authorized access through the new endpoint; preview assets remain visible in order detail without access logging
- Digital asset section in `OrdersView` now shows an authorized "access resource" action instead of exposing raw full-asset URLs
- Frontend auth store now normalizes session roles to lowercase so `USER` / `ADMIN` API payloads work with route guards

### refactor
- Replaced user preference process-memory storage with mapper-backed database persistence
- Centralized digital asset access through the order service instead of deriving access behavior in the frontend

### test
- Added backend integration coverage for preference default read, update, and second read consistency
- Added backend integration coverage for report submit, invalid report target, admin visibility, and admin processing
- Added backend integration test `digitalAssetAccessGovernanceEnforcesOwnershipAndLogsAccess`, covering pre-receipt block, non-buyer 403, post-receipt access, and access log visibility
- Added backend integration coverage for real user and shop insight aggregation
- Added frontend Vitest coverage for auth store, market store product loading, and router guard access control

### docs
- Added `docs/dev/tasks/CURRENT_STAGE_WORK_PLAN.md` for AI-agent execution planning
- Updated roadmap and engineering docs to align the current stage as MVP closure on H2 file database + JDBC, with MySQL/MyBatis kept as course and migration direction
- Added `docs/http/report.http`
- Updated `docs/http/auth.http` and `docs/http/order.http` for preference and digital asset access flows
## [2026-05-21] - Repository bootstrap hardening for Youyu

### docs
- Rewrote the repository root `README.md` as a proper standalone project entry for `Youyu`, including stack overview, quick start, repository structure, documentation entry points, and migration notes from the older course workspace

### ci
- Hardened `.github/workflows/ci.yml` with manual trigger support, workflow concurrency control, explicit read-only permissions, job timeouts, healthier service readiness checks, and failure artifact uploads for Playwright runtime logs

### chore
- Expanded root `.gitignore` to cover common local build outputs, editor files, OS files, Vite artifacts, and local environment overrides
- Added `.editorconfig` for cross-editor formatting consistency
- Added `.gitattributes` for safer line-ending behavior across Windows and Linux
- Added `.github/pull_request_template.md` to standardize summary, testing, and documentation expectations for protected-branch development

---
