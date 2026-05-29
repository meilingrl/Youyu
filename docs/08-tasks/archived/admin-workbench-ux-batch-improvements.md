# Task: Admin Workbench UX And Batch Improvements

## Metadata

- ID: admin-workbench-ux-batch-improvements
- Status: completed
- Owner: Codex
- Track: cross-cutting
- Depends on: current admin module baseline
- Priority: high
- Planned date: 2026-05-29
- Completed date: 2026-05-29

## Objective

Improve the admin workbench UI copy, navigation, dashboard signal density, table/filter polish, Chinese status display, selected queue batch operations, support workspace framing, and product review detail access.

## Scope

- Admin navigation and layout logout placement
- Admin dashboard zero-value hiding and first-card polish
- Admin list/table shell, filters, spacing, buttons, and status display labels
- Batch operations for selected admin queues
- Support context page reframed as customer-service workspace
- Product review task detail access
- Matching backend admin APIs, API docs, smoke requests, changelog, and task archival

## Out of Scope

- Real-time customer chat
- Full support ticket database model
- Database status value migration
- Batch operations for orders, mediation, or hot-search rules

## Completion Notes

- Moved logout to the admin sidebar footer and shortened admin navigation labels.
- Reworked dashboard cards, hidden zero-value queues/signals, and translated status breakdown labels.
- Added shared admin status-label mapping and applied it across admin tables and detail surfaces.
- Added batch operations for users, verifications, products, review tasks, shops, and reports.
- Added `GET /api/admin/review-tasks/{reviewTaskId}` and a frontend detail drawer for product review materials.
- Reframed `/admin/support` as a customer-service workspace without adding a real-time chat or ticketing model.
- Updated admin API spec, HTTP samples, backend tests, and changelog.
- Follow-up polish: changed all admin navigation labels to two Chinese characters, added sidebar icons, tightened sidebar width, and replaced the logout text action with a circular button beside the database-backed admin account name.
- Follow-up polish: added drag-follow right-swipe selection for batch tables, improved table/batch button styling, and rebuilt `/admin/support` into a queue-based customer-service workbench that hides empty queues.

## Test Results

- `npm test`: passed
- `npm run build`: passed
- `.\mvnw.cmd test -Dtest=AdminGovernanceTest`: passed
- `.\mvnw.cmd test`: passed
- Follow-up `npm test`: passed
- Follow-up `npm run build`: passed
