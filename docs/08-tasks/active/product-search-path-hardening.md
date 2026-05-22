# Task: Product Search Path Hardening (Slice D of architecture-performance-hardening)

## Metadata

- ID: product-search-path-hardening
- Status: active
- Owner: unassigned
- Track: cross-cutting
- Depends on: archived `runtime-index-hardening` (baseline schema indexes) + archived `admin-query-pagination-hardening` (paginated response contract)
- Parent: `docs/08-tasks/drafts/architecture-performance-hardening.md` Slice D
- Priority: medium
- Planned date: 2026-05-22
- Completed date:

## Objective

Reduce growth risk on the public product search path without changing user-visible search semantics. Concretely: add the smallest set of composite indexes that materially help `findPublicByFiltersPaged` + `countPublicByFilters`, and document the behavioral tradeoff for any heavier alternative deferred.

This is a **bounded local change**, not a rewrite of search.

## Background

Per the parent task's "Problem Summary §3":

> The product list search path has already moved to server-side pagination, which is a good baseline, but the keyword matching strategy still relies on multiple `LOWER(...) LIKE '%keyword%'` conditions across several columns, plus a second count query.

Today's `JdbcProductMapper.findPublicByFiltersPaged()` applies filters on `status`, `product_type`, `category_id`, and a `LIKE` keyword scan across `name` / `description` / similar columns. Existing schema indexes cover individual columns at best; there is no composite index reflecting the actual filter combination most public traffic uses.

Substring `LIKE '%kw%'` is index-skipping by nature, so the goal here is **not** to make keyword matching index-backed — it is to make the *non-keyword filter combination* fast enough that the residual scan cost is small.

## Pre-flight Verification (must complete before any edit)

1. Read `backend/src/main/java/com/campusmarket/backend/mapper/product/impl/JdbcProductMapper.java` — capture the exact WHERE clauses in `findPublicByFiltersPaged` and `countPublicByFilters`. Paste the SQL strings into Final Report section B.
2. Read `backend/src/main/resources/schema.sql` `products` table definition AND every `CREATE INDEX ... ON products`. Paste current index list.
3. Run a quick reasoning check: the filter combinations the public route uses are which 2-3 of {status, product_type, category_id, keyword}? Confirm by reading `ProductServiceImpl.listPublic` (or equivalent).
4. **Capture before-numbers**:
   ```bash
   cd backend && ./mvnw test -Dtest=*Search* -B 2>&1 | tail -30
   ```
   Note baseline pass/fail (tests must already be green).
5. Run an EXPLAIN against the H2 test DB *or* against a local MySQL with the perf catalog (3500+ products):
   - Document the EXPLAIN plan for a representative query like `WHERE status = 'online' AND product_type = 'physical' AND category_id = 1 ORDER BY ... LIMIT 20 OFFSET 0`
   - If EXPLAIN access is not feasible in this environment, mark step skipped in Final Report and note the limitation

## Files to Read

- `AGENTS.md`, `CLAUDE.md`
- `backend/src/main/java/com/campusmarket/backend/mapper/product/impl/JdbcProductMapper.java`
- `backend/src/main/java/com/campusmarket/backend/mapper/product/ProductMapper.java`
- `backend/src/main/java/com/campusmarket/backend/service/product/impl/ProductServiceImpl.java`
- `backend/src/main/java/com/campusmarket/backend/controller/product/ProductController.java`
- `backend/src/main/resources/schema.sql`
- `docs/08-tasks/archived/runtime-index-hardening.md` (for the additive-index pattern)
- `docs/08-tasks/drafts/architecture-performance-hardening.md` Slice D section

## In Scope

1. Add 1–3 composite indexes on `products` that match the actual public-listing filter combinations. Most likely candidates (subject to pre-flight evidence):
   - `idx_products_status_type_category ON products(status, product_type, category_id)`
   - `idx_products_status_sort ON products(status, <whatever the ORDER BY column is>, id)` for the paginated listing
2. Indexes must be additive only (no DROP, no ALTER on existing tables) and use the existing `CREATE INDEX IF NOT EXISTS`-equivalent pattern used by `runtime-index-hardening`
3. Document the keyword-search tradeoff in `docs/07-decisions/`:
   - Create `docs/07-decisions/2026-05-XX-product-search-keyword-strategy.md` (ADR)
   - Explain why substring `LIKE` is kept, what alternatives were considered (MySQL `FULLTEXT`, application-level inverted index), and what would trigger a future change
4. Add a short note to `CLAUDE.md` "Database" section mentioning the new composite indexes if they materially shift the conventional index strategy (one line, optional)
5. Verify existing product/search tests still pass

## Out of Scope

- Changing the SQL in `JdbcProductMapper` (keep `LIKE '%kw%'` exactly as-is)
- Adding MySQL `FULLTEXT` indexes (defer; document in ADR)
- Application-level search engine (Elasticsearch, MeiliSearch, Lucene)
- Frontend changes
- Changing the response contract
- New endpoints
- Touching seed data

## Hard Limits

- **Do not** use `ALTER TABLE` or `DROP INDEX` in `schema.sql` (forbidden by repo standards)
- **Do not** modify any SQL in mapper implementations
- **Do not** change `findPublicByFiltersPaged` or `countPublicByFilters` method signatures
- **Do not** introduce a search-engine dependency
- **Do not** add more than 3 new indexes; if pre-flight suggests >3 are needed, file a follow-up task instead
- **Do not** alter ORDER BY columns (those drive index design but must remain whatever they are today)
- **Do not** use `--no-verify` on commits

## Allowed Changes

- `backend/src/main/resources/schema.sql` — additive `CREATE INDEX IF NOT EXISTS` statements only
- `docs/07-decisions/<date>-product-search-keyword-strategy.md` (new ADR)
- `CHANGELOG.md`
- `CLAUDE.md` — optional one-line index note
- `docs/08-tasks/active/product-search-path-hardening.md` → move to `archived/`
- `docs/08-tasks/drafts/architecture-performance-hardening.md` — flip Slice D checkbox to `[x]`

## Implementation Steps

1. Complete pre-flight; record SQL strings, current indexes, EXPLAIN evidence (or skip note).
2. From pre-flight findings, design the composite indexes (1–3). Document the column order rationale in the ADR draft.
3. Append the new `CREATE INDEX IF NOT EXISTS` statements to `schema.sql` in the same style as `runtime-index-hardening` already used. Each index gets a one-line comment naming the query path it supports.
4. Write `docs/07-decisions/<YYYY-MM-DD>-product-search-keyword-strategy.md` containing:
   - Context (current `LIKE` scan, why it works today)
   - Decision (keep substring matching; add composite indexes for non-keyword filter columns)
   - Considered alternatives (FULLTEXT, application search, trigram) with rejection reason for each
   - Trigger for revisiting (e.g., "catalog > 50k products and search p95 > 500ms")
5. Run `cd backend && mvnw.cmd test -B` (Windows) or `./mvnw test -B` (mac/linux). All tests must remain green.
6. Optional: re-run the EXPLAIN from pre-flight against the same query and paste before/after plans in Final Report.
7. If you added the optional `CLAUDE.md` note, keep it to one line under the Database section.
8. Prepend `CHANGELOG.md` block under `### refactor` (because schema-only).
9. Flip Slice D checkbox in `architecture-performance-hardening.md` from `[ ]` to `[x]` with a one-line completion note (link to ADR + the new indexes).
10. Move this task file to `archived/` with `Delivered` section.

## Test Plan

- Backend:
  - `cd backend && ./mvnw test -B` — must pass with zero failures and zero errors (run before and after; report both exit codes)
  - Search-related tests specifically must still pass
- Frontend: not required
- API validation:
  - `grep -n "CREATE INDEX" backend/src/main/resources/schema.sql | grep products` — every new index visible
  - Hit `GET /api/products?status=online&productType=physical&page=1&pageSize=20` against a running backend (optional if feasible); expected: 200 OK with `{ items, total, page, pageSize }` envelope unchanged
- Manual:
  - Confirm ADR exists and reads cleanly
  - Confirm parent task Slice D checkbox flipped

## Acceptance Criteria

- [ ] 1–3 new composite indexes on `products` added to `schema.sql`, each with a one-line comment naming its query path
- [ ] No existing index removed; no `ALTER TABLE`; no `DROP INDEX`
- [ ] `JdbcProductMapper.findPublicByFiltersPaged` and `countPublicByFilters` SQL bodies are byte-identical before and after (verified via `git diff`)
- [ ] ADR created at `docs/07-decisions/<date>-product-search-keyword-strategy.md` with all four ADR sections (Context, Decision, Alternatives, Trigger)
- [ ] `./mvnw test -B` passes with zero failures both before and after the change
- [ ] `CHANGELOG.md` block added under `### refactor`
- [ ] Slice D checkbox in `architecture-performance-hardening.md` flipped to `[x]`
- [ ] This task moved to `archived/`

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] `docs/06-http/` — not applicable
- [x] `docs/07-decisions/` (new ADR)
- [x] `docs/08-tasks/drafts/architecture-performance-hardening.md` (Slice D status)
- [x] task status and archive move

## Final Report Format

```markdown
## Return Report — product-search-path-hardening

### A. Branch & Commit
- Branch: <name>
- Commit SHA: <sha>
- Files changed (paste `git diff --stat`)

### B. Pre-flight Findings
- findPublicByFiltersPaged WHERE clause (paste exact SQL):
  ```sql
  ...
  ```
- countPublicByFilters WHERE clause (paste exact SQL):
  ```sql
  ...
  ```
- Existing indexes on products (paste from schema.sql):
  - <line>
- Public route filter combination observed in ProductServiceImpl: <e.g. status+product_type+category_id+keyword>
- Baseline test run: <"./mvnw test -B" exit code> with <N tests passed>
- EXPLAIN plan before (or "skipped, reason: <reason>"):
  ```
  ...
  ```

### C. Implementation Walkthrough
- Step 2 → indexes designed:
  - `idx_<name>` ON products(<cols>) — supports: <query path>
  - ...
- Step 3 → schema.sql additions at lines <range>
- Step 4 → ADR at `docs/07-decisions/<filename>` (lines: <N>)
- Step 5 → post-change test run: <exit code> with <N tests passed>
- Step 6 → EXPLAIN plan after (or "skipped"):
  ```
  ...
  ```
- Step 8 → CHANGELOG block added (paste)
- Step 9 → parent task Slice D checkbox flipped (paste resulting line)
- Step 10 → task moved to archived

### D. Test Plan Results
- `./mvnw test -B` before: <exit code, N passed>
- `./mvnw test -B` after: <exit code, N passed>
- `grep -n "CREATE INDEX" schema.sql | grep products` → <N existing + M new = total>
- JdbcProductMapper SQL unchanged: <git diff confirms — paste line>

### E. Acceptance Criteria Check
- [x/✗] one per criterion with evidence

### F. Deviations from Spec
- "none" or specific deviation with reason

### G. Out-of-scope Findings
- "none" or specific items (do not fix here)

### H. Open Questions / Blockers
- "none" or single question
```

## Completion Notes

(Filled in by sub-agent.)
