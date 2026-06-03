# Database

This directory stores the database design assets for `Youyu`.

## Current contents

- `001_mvp_init.sql`: first-pass MySQL 8 design/init script for the MVP migration target
- `002_support_chat_columns.sql`: manual reference for online CS columns on existing `chat_conversations` / related chat fields (also applied automatically by `ChatSupportSchemaUpgrader` on backend startup)
- `003_payment_gateway_foundation.sql`: additive payment gateway fields and callback replay table for existing MySQL databases (also applied automatically by `PaymentSchemaUpgrader` on backend startup)
- `004_payment_refund_consistency.sql`: additive refund diagnostic fields for gateway-backed refund completion (also applied automatically by `PaymentSchemaUpgrader` on backend startup)
- `mvp-data-dictionary.md`: table and field notes for the MVP
- `mvp-db-development-status.md`: current database readiness assessment

## Current role in the project

The current application runtime does **not** start from `database/001_mvp_init.sql`.

Current runtime baseline:

- Default local/dev database: MySQL 8 at `localhost:3306/youyu`
- Default backend schema source: `backend/src/main/resources/schema.sql`
- Default data access style: JDBC through `JdbcTemplate` and mapper implementations
- Test database: H2 in-memory

Target route:

- MySQL is the course-aligned runtime database.
- `001_mvp_init.sql` should be treated as a MySQL 8 design asset and migration reference.
- MyBatis is a future/target data access option, not the current default implementation.
- Do not perform a MyBatis or migration-tool transition from this directory alone; create a dedicated task or ADR that covers compatibility, test strategy, seed data, and rollback expectations.

The database model is already sufficient to support ongoing MVP development for:

- user and student verification
- product publishing and review
- shop opening and capability profiles
- cart, order, fulfillment, payment, and refund skeletons
- report, credit, and restriction records
- digital asset delivery

For current-stage MVP closure, new agents should prefer additive schema changes that keep MySQL runtime behavior and H2/JDBC tests passing, and update this directory only when database semantics or migration notes change.

For an existing MySQL database, the backend now auto-applies the payment
compatibility additions from `003_payment_gateway_foundation.sql` and
`004_payment_refund_consistency.sql` on startup. The numbered SQL files remain
the manual reference and fallback path when startup automation is unavailable.
