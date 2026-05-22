# Task: API Spec Report Module Standardization

## Metadata

- ID: api-spec-report-module-standardization
- Status: active
- Owner: unassigned
- Track: cross-cutting
- Depends on: none (report endpoints already exist in code)
- Priority: medium
- Planned date: 2026-05-22
- Completed date:

## Objective

Produce `docs/09-api-spec/report.md` as a formal contract document for both user-side report submission and admin-side report processing, and verify `docs/06-http/report.http` matches current controller behavior.

## Background

The report domain has been in production-ready shape since 2026-05-13: `POST /api/reports` (user-side submit) and the admin processing endpoints under `AdminController` exist with mapper-backed persistence. The HTTP collection `docs/06-http/report.http` is aligned. The formal contract document is the last missing piece — adding it brings report to parity with auth/product/order/admin/user/review/search modules that already have formal specs.

## Pre-flight Verification (must complete before any write)

Run and paste into Final Report section B:

1. `ls docs/09-api-spec/` — confirm `report.md` is absent
2. Read `docs/09-api-spec/API_SPEC_TEMPLATE.md` end-to-end
3. Read `docs/09-api-spec/admin.md` and `docs/09-api-spec/user.md` to internalize the house style (response envelope phrasing, error semantics section, `.http` cross-reference table)
4. `grep -n "^\s*@\(Get\|Post\|Put\|Delete\|Patch\)Mapping\|@LoginRequired" backend/src/main/java/com/campusmarket/backend/controller/report/ReportController.java`
5. `grep -n "report" backend/src/main/java/com/campusmarket/backend/controller/admin/AdminController.java` — capture admin-side report endpoints
6. Read `backend/src/main/java/com/campusmarket/backend/service/report/impl/ReportServiceImpl.java` to confirm response field set
7. Read `docs/06-http/report.http` and diff against the endpoint inventory from steps 4-5

## Files to Read

- `AGENTS.md`, `CLAUDE.md`
- `docs/09-api-spec/README.md`
- `docs/09-api-spec/API_SPEC_TEMPLATE.md`
- `docs/09-api-spec/admin.md` (as style reference)
- `docs/09-api-spec/user.md` (as style reference)
- `docs/06-http/report.http`
- `backend/src/main/java/com/campusmarket/backend/controller/report/ReportController.java`
- `backend/src/main/java/com/campusmarket/backend/controller/admin/AdminController.java` (filter to report-related methods)
- `backend/src/main/java/com/campusmarket/backend/service/report/impl/ReportServiceImpl.java`
- `backend/src/main/java/com/campusmarket/backend/mapper/report/impl/JdbcReportMapper.java` (only for field shape; do not edit)
- `backend/src/main/java/com/campusmarket/backend/common/api/ResultCode.java` (error codes referenced by spec)

## In Scope

- Create `docs/09-api-spec/report.md` covering every report endpoint exposed today
- Patch `docs/06-http/report.http` if (and only if) the audit in pre-flight finds drift in method/path/role/request body
- Update `docs/09-api-spec/README.md` module list to include `report`
- Append `CHANGELOG.md` block

## Out of Scope

- Backend code change (this is a documentation task)
- Report workflow redesign or new endpoints
- Admin governance UI consistency work (tracked by `admin-governance-action-consistency`)
- Mediation scope (tracked by `platform-mediation-boundary-definition`)
- Specs for other modules (`recommend`, `shop`) — those belong to `api-spec-standardization-follow-up`

## Hard Limits

- **Do not** edit any file under `backend/src/`
- **Do not** introduce OpenAPI / Swagger generation
- **Do not** rename existing endpoints or alter response field names; spec must reflect runtime truth, not a proposed shape
- **Do not** touch other `docs/09-api-spec/*.md` files except `README.md` (module index)
- **Do not** add the spec entry to `docs/06-http/report.http` if the audit shows alignment is fine — only patch drift

## Allowed Changes

- `docs/09-api-spec/report.md` (new file)
- `docs/09-api-spec/README.md` (module index line)
- `docs/06-http/report.http` (drift correction only)
- `CHANGELOG.md`
- `docs/08-tasks/active/api-spec-report-module-standardization.md` → move to `archived/` with completion notes

## Implementation Steps

1. Complete every item in **Pre-flight Verification** and record findings.
2. Build the endpoint inventory as a table (method | path | auth | role | request body type | response data shape | error codes). Use `ReportController` and the report-related methods in `AdminController` as the only sources of truth.
3. Draft `docs/09-api-spec/report.md` using `API_SPEC_TEMPLATE.md`. Required sections per house style (see `admin.md`):
   - Module overview (one paragraph)
   - Shared response envelope reference
   - Per-endpoint sections with: HTTP method + path, auth requirement, request fields table, response `data` shape, possible error codes (with HTTP status + `ResultCode`), and the matching `.http` block reference (line range in `report.http`)
   - Known drift / open issues section (empty if none)
4. If pre-flight audit found drift in `report.http`, patch the minimal lines required. Do not reformat unaffected lines.
5. Update `docs/09-api-spec/README.md` to list `report` in the module index, in alphabetical position.
6. Prepend a `CHANGELOG.md` block dated 2026-05-22 (or current date) under `### docs` describing the new spec.
7. Move this task file from `active/` to `archived/`, updating `Status: archived` and `Completed date:`, and add `## Delivered` section with: created files, patched files (if any), and a one-line endpoint count summary.

## Test Plan

- Backend: not required
- Frontend: not required
- API validation:
  - `grep -E "^(GET|POST|PUT|DELETE|PATCH) " docs/06-http/report.http` — every path must appear in the new spec
  - `grep -n "/api/reports\|/api/admin/reports" backend/src/main/java/com/campusmarket/backend/controller/**/*.java` — every path returned must appear in the new spec
- Manual: read the new spec end-to-end; each endpoint section answers the questions a frontend developer would ask before writing a fetch call

## Acceptance Criteria

- [ ] `docs/09-api-spec/report.md` exists, follows `API_SPEC_TEMPLATE.md` structure, and matches the tone of `admin.md` / `user.md`
- [ ] Every endpoint exposed by `ReportController` is documented
- [ ] Every report-related endpoint in `AdminController` is documented under the same module
- [ ] Each endpoint section names: method, path, auth (`@LoginRequired` + role if present), request body fields, response `data` shape, error codes
- [ ] `docs/09-api-spec/README.md` lists `report` in the module index
- [ ] `docs/06-http/report.http` matches controller truth (no method/path/auth drift)
- [ ] `CHANGELOG.md` has a new dated block under `### docs` describing the spec addition
- [ ] This task file is moved to `archived/` with `Status: archived` and a `Delivered` section

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] `docs/09-api-spec/README.md` (module index)
- [ ] roadmap or standards docs — not required for this task
- [x] task status and archive move

## Final Report Format

The sub-agent must produce this report verbatim (filled in) when handing back.

```markdown
## Return Report — api-spec-report-module-standardization

### A. Branch & Commit
- Branch: <branch name>
- Commit SHA: <sha>
- Files changed (paste `git diff --stat HEAD~1` output here)

### B. Pre-flight Findings
- ls confirmation: <output line showing report.md absent>
- Endpoints discovered in ReportController:
  - <METHOD PATH — auth>
  - ...
- Report-related endpoints discovered in AdminController:
  - <METHOD PATH — auth>
  - ...
- Drift found in report.http (if any): <list lines or "none">

### C. Implementation Walkthrough
- Step 1 → <one line>
- Step 2 → endpoint inventory table written to spec (paste 1-line summary: N endpoints)
- Step 3 → spec drafted at docs/09-api-spec/report.md (line count: <N>)
- Step 4 → http drift patched / no drift
- Step 5 → README.md updated at line <N>
- Step 6 → CHANGELOG block prepended (paste the block)
- Step 7 → task file moved to archived

### D. Test Plan Results
- `grep -E "^(GET|POST|PUT|DELETE|PATCH) " docs/06-http/report.http` → <N paths>
- `grep -n "/api/reports\|/api/admin/reports" backend/.../controller/**/*.java` → <N paths>
- Spec coverage: <each path covered yes/no>

### E. Acceptance Criteria Check
- [x/✗] report.md exists and follows template — <evidence>
- [x/✗] every ReportController endpoint documented — <list>
- [x/✗] every AdminController report endpoint documented — <list>
- [x/✗] auth/method/path/request/response/errors present per endpoint — <evidence>
- [x/✗] README.md module index updated — <line>
- [x/✗] report.http drift resolved or "none found" — <evidence>
- [x/✗] CHANGELOG block added — <date>
- [x/✗] task archived with Delivered section — <commit SHA snippet>

### F. Deviations from Spec
- <list anything not following the spec, with reason; "none" if none>

### G. Out-of-scope Findings
- <issues noticed but not fixed; "none" if none>

### H. Open Questions / Blockers
- <single question or "none">
```

## Completion Notes

(Filled in by sub-agent after work is done.)
