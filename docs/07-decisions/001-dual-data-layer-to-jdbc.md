# ADR-001: Migrate All Data Access from In-Memory to JDBC

**Date:** 2026-05-11
**Status:** Accepted

## Context

The first working prototype used `AdminDataStore` — an in-memory Spring component — to hold admin credentials, reports, users, products, shops, and verifications. This was fast to build but created several problems:

- Data is lost on every server restart
- Admin data and regular user data live in two incompatible stores, causing subtle inconsistencies
- `AdminServiceImpl` mixes business logic with data sourcing from a Java object, making it hard to query across domains
- The other domain mappers (User, Product, Shop, StudentVerification) already use JDBC with H2, creating a dual-track system

The project is moving from prototype to a development-stable state, so data persistence is required.

## Decision

Replace `AdminDataStore` and all `InMemory*Mapper` implementations with JDBC-backed mappers using `JdbcTemplate` and H2 file-mode database. The `reports` table is added to `schema.sql`. The admin user is seeded into the `users` table. All mappers follow the `Jdbc*Mapper` pattern already established by `JdbcUserMapper`.

Migration is done in two stages:
1. **Iteration 1.1 (Agent-A):** Migrate `InMemoryReportMapper` → `JdbcReportMapper`
2. **Iteration 1.2 (Agent-C):** Remove `AdminDataStore` entirely; admin login uses `UserMapper`

## Consequences

**Positive:** All data persists through restarts. Admin and user data are unified in one H2 database. `AdminDataStore` is deleted, eliminating the dual-track maintenance burden. Seed data is now in SQL files, version-controlled alongside schema.

**Negative:** Seed data must be maintained as SQL (`MERGE INTO ... KEY(id)`) rather than Java code. Future contributors need basic SQL knowledge to add seed rows.

**Neutral:** `AdminServiceImpl` business logic is unchanged; only the data source changes.
