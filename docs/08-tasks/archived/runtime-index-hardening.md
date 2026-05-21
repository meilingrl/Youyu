# Task: Runtime Index Hardening

## Metadata

- ID: runtime-index-hardening
- Status: archived
- Owner: meilingrl
- Track: cross-cutting
- Depends on: architecture-performance-hardening
- Priority: high
- Planned date: 2026-05-17
- Completed date: 2026-05-17

## Objective

Bring the runtime schema's index strategy closer to the codebase's actual read paths by adding explicit, non-destructive indexes for the current order, payment, refund, report, and related operational queries.

This task should improve performance predictability and reduce future ambiguity around which access paths are intentionally supported.

## Background

The project already has real query behavior around:

- order list by buyer
- order detail expansion by order id
- payments and refunds by order
- reports by status and recent submission time
- search logs by time window and normalized keyword

However, the runtime schema still only partially communicates this intent through explicit indexes. As the project grows, implicit or incomplete index support becomes a hidden risk:

- queries become slower without any code change
- future agents cannot easily tell whether poor plans are accidental or expected
- small query inefficiencies compound when more pages and modules reuse the same tables

This task is not about broad database redesign. It is about adding explicit schema support for the read paths already present in code.

## Scope

### In Scope

- review current runtime query paths for:
  - `orders`
  - `order_items`
  - `order_fulfillments`
  - `payment_records`
  - `refund_records`
  - `reports`
  - any immediately adjacent table whose active runtime access path is already clear
- add explicit, additive indexes in `backend/src/main/resources/schema.sql`
- add small documentation notes if the indexing intent needs to be made clearer
- run regression verification for existing backend behavior

### In Scope But Only If Justified By Current Code

- search-log index refinement if current code already uses that access path heavily
- index comments in docs if they clarify the rationale for future maintainers

## Out of Scope

- changing service logic or controller behavior
- modifying frontend code
- redesigning product search semantics
- introducing migrations tooling
- destructive schema changes
- column type changes

## Files to Read

- `docs/04-standards/development-process.md`
- `backend/README.md`
- `backend/src/main/resources/schema.sql`
- current service and mapper query paths for:
  - orders
  - payment
  - refund
  - reports
  - search logs if touched
- related backend tests

## Allowed Changes

- `backend/src/main/resources/schema.sql`
- focused backend tests if required to validate schema compatibility
- small supporting documentation updates if the schema strategy needs clarification
- `CHANGELOG.md`

## Parallelization Boundary

This task is designed to be parallel-safe with:

- `admin-query-pagination-hardening`
- `review-order-lookup-hardening`
- `product-list-request-flow-hardening`

### Do Not Touch In This Task

- admin controllers/services/frontend pages
- review service logic
- storefront product list files
- broad product search query code

If an improvement requires service or mapper refactor, record it and hand it back to the appropriate task rather than absorbing it here.

## Required Schema Constraints

- index changes must be additive
- do not delete tables or columns
- do not introduce destructive migration assumptions
- preserve H2 test compatibility as much as practical
- keep index naming explicit and readable

## Implementation Plan

1. Inventory actual read paths from code.
   - list each table
   - list the real where/order-by patterns currently used

2. Compare code paths to current explicit indexes.
   - identify obvious gaps
   - ignore speculative future optimization not supported by current runtime behavior

3. Add focused indexes in `schema.sql`.
   - prefer small, explainable indexes over premature over-indexing
   - align names with table and query intent

4. Verify compatibility.
   - ensure startup and tests still work
   - check that no existing SQL assumptions break

5. Document rationale if needed.
   - if an index exists mainly because of a non-obvious query path, leave a concise note in task completion notes or supporting docs

## Risks

- adding too many indexes without enough evidence
- creating indexes that help one query but slow write-heavy paths unnecessarily
- using syntax that behaves differently between H2 and MySQL
- turning this task into a hidden service-layer refactor

## Test Plan

- Backend:
  - run schema-compatible backend tests
  - verify startup still succeeds in the intended environment
- Frontend:
  - no frontend changes expected
- API validation:
  - no direct API contract changes expected
- Manual:
  - smoke-test order, refund, report, and search-log-backed pages if practical

## Acceptance Criteria

- [x] `schema.sql` explicitly reflects the main current runtime read paths with additive indexes
- [x] No service/controller/frontend refactor was mixed into this task
- [x] Existing behavior remains compatible after schema changes
- [x] The resulting index strategy is understandable and tied to current code, not speculative future architecture

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes

This task owns the schema write scope for the current hardening wave.

Any other parallel task that discovers a schema need should not edit `schema.sql` directly. It should instead:

1. record the need
2. reference this task
3. keep its own change set schema-free

Completed outcome:

- Added 10 additive indexes covering buyer order listing, shop insight aggregation, order detail expansion, payment/refund lookups, report pagination/sorting, and latest verification lookup paths
- Verified compatibility through successful backend test execution on the current branch
- Left non-essential and speculative indexes deferred for future evidence-based follow-up
