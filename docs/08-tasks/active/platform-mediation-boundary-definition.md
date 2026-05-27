# Task: Platform Mediation Boundary Definition

## Metadata

- ID: platform-mediation-boundary-definition
- Status: blocked (archived `chat-mvp-scope-definition` exists, but required `docs/02-requirements/chat-mvp-scope.md` is missing)
- Owner: unassigned
- Track: feature
- Depends on: archived `chat-mvp-scope-definition` plus `docs/02-requirements/chat-mvp-scope.md` (the delivered scope doc anchors the mediation boundary)
- Priority: medium
- Planned date: 2026-05-22
- Completed date:

## Objective

Define the execution boundary between (1) the existing report/governance flow, (2) the future chat MVP, and (3) platform-driven mediation — so a later agent can implement mediation without re-arguing where its responsibilities start and end.

Produce two artifacts: a requirements scope document and an execution-ready implementation task spec. **No application code is changed.**

## Background

The report flow already exists (`POST /api/reports`, admin processing, archived `order-after-sales-ux-hardening` extended its visibility). The chat MVP implementation now exists in code, and `docs/08-tasks/archived/chat-mvp-scope-definition.md` exists, but the required delivered scope artifact `docs/02-requirements/chat-mvp-scope.md` is not present in this worktree. Mediation remains blocked until that scope artifact is restored or recreated. `open-questions.md` item 2 captures the three pending decisions:

- whether mediation rides on top of a chat conversation or stands alone
- whether mediation is parallel to report or a state escalation of it
- whether mediation reuses the existing report admin pages or needs new surfaces

This task answers those three questions and writes them down concretely.

## Pre-flight Verification (must complete before drafting)

1. **Confirm dependency satisfied** — `ls docs/08-tasks/archived/chat-mvp-scope-definition.md` AND `ls docs/02-requirements/chat-mvp-scope.md`. If either is missing, **stop immediately** and report blockage (do not write anything; this task stays blocked).
2. Read `docs/02-requirements/chat-mvp-scope.md` — capture the entities and API surface chat MVP will own
3. Read `docs/02-requirements/communication-and-after-sales-boundary.md`
4. Read `docs/05-roadmap/current/open-questions.md` item 2 in full
5. `grep -n "report\|Report" backend/src/main/java/com/youyu/backend/controller/report/ReportController.java backend/src/main/java/com/youyu/backend/controller/admin/AdminController.java` — list current report endpoints and their statuses
6. Read `backend/src/main/resources/schema.sql` `reports` table — capture column set (especially status enum values)
7. `ls frontend/src/views/admin/` and identify the current report admin view (`ReportManageView.vue` or similar). Read it to understand the existing action set.

## Files to Read

- `AGENTS.md`, `CLAUDE.md`, `docs/README.md`
- `docs/02-requirements/chat-mvp-scope.md` (produced by predecessor task)
- `docs/02-requirements/communication-and-after-sales-boundary.md`
- `docs/05-roadmap/current/open-questions.md`
- `docs/05-roadmap/current/feature-roadmap.md`
- `docs/08-tasks/archived/chat-mvp-scope-definition.md`
- `docs/08-tasks/archived/order-after-sales-ux-hardening.md`
- `docs/08-tasks/archived/ui-redesign-messages-support.md`
- `backend/src/main/java/com/youyu/backend/controller/report/ReportController.java`
- `backend/src/main/java/com/youyu/backend/controller/admin/AdminController.java` (report-related methods only)
- `backend/src/main/resources/schema.sql` (search for `reports`)
- existing admin report view in `frontend/src/views/admin/`

## In Scope

Produce two artifacts:

### Artifact 1 — `docs/02-requirements/platform-mediation-scope.md`

Required sections:

1. **What mediation is** — single paragraph: platform-driven resolution of disputed transactions where buyer and seller cannot self-resolve via order remarks, refund flow, or report. Mediation results in a binding admin decision (refund / no-refund / partial / order cancellation), not just a status change.
2. **What mediation is not** — explicit list:
   - Not a general support chat (that lives in chat MVP)
   - Not a report (report is buyer's accusation; mediation is admin's resolution work)
   - Not a refund flow (refund is the action; mediation is the deliberation)
3. **Relation to report**: pick ONE of the two models and justify:
   - **Model A: state escalation** — `reports.status` gains a `mediating` state; an `admin_decision` field captures the outcome; same table, same endpoints + admin actions
   - **Model B: parallel entity** — new `mediation_cases` table referencing `report_id`; new endpoints + admin surfaces
   - Default position: Model A (less surface area). Deviate only if pre-flight findings show concrete reasons.
4. **Relation to chat MVP**: pick ONE:
   - **Model X: mediation has no chat** — admin reads order context, refund records, and report description; renders a decision via existing admin form
   - **Model Y: mediation reuses chat MVP conversation** — admin participates as a third party in a chat conversation seeded from the report's order
   - Default position: Model X for v1 (chat MVP entities don't yet support 3-party conversations). Document Model Y as v2 candidate.
5. **Entities & schema changes** — concrete diff against current `reports` table (if Model A) or full schema for new table (if Model B)
6. **API surface** — list every new/changed endpoint with method, path, auth (admin only), request, response
7. **Admin surfaces** — concrete list of new buttons / pages / dialogs in `ReportManageView.vue` (or the new mediation admin view if Model B)
8. **Non-goals** — explicit:
   - No buyer-facing UI for mediation in v1 (status visibility only via order detail)
   - No automated decision logic; admin decides
   - No appeals flow

### Artifact 2 — `docs/08-tasks/active/platform-mediation-implementation.md`

Execution-ready task spec using this file's structure. Implementation Steps must include:
- backend schema migration (if Model B) or column additions (if Model A)
- mapper / service / controller / DTO updates
- integration tests covering: state transitions, admin-only access, idempotency of decision recording
- frontend admin view updates: new buttons, decision dialog, state badges
- `.http` smoke updates in `docs/06-http/admin.http`
- spec update in `docs/09-api-spec/admin.md` (or a new module spec if Model B)

## Out of Scope (this task)

- Writing the mediation code
- Modifying schema.sql or seed
- Touching report or chat controller code
- Buyer-side UI work
- Designing an appeals flow

## Hard Limits

- **Do not** start work if pre-flight step 1 fails (chat-mvp not yet archived) — exit with blocked status
- **Do not** edit any file under `backend/src/` or `frontend/src/`
- **Do not** propose entity changes without recording them as a concrete schema diff
- **Do not** invent a buyer-facing mediation UI in v1 (defer to v2 explicitly)
- **Do not** pick both Model A *and* Model B; pick one
- **Do not** pick Model Y unless chat-mvp-scope explicitly supports 3-party conversations

## Allowed Changes

- `docs/02-requirements/platform-mediation-scope.md` (new)
- `docs/08-tasks/active/platform-mediation-implementation.md` (new)
- `docs/05-roadmap/current/open-questions.md` (mark item 2 resolved)
- `docs/05-roadmap/current/feature-roadmap.md` (one-line status)
- `CHANGELOG.md`
- `docs/08-tasks/active/platform-mediation-boundary-definition.md` → move to `archived/`

## Implementation Steps

1. Complete pre-flight; if step 1 fails, stop and report blocked
2. Score Models A vs B on: implementation effort × surface area × reversibility. Default to A unless score forces B.
3. Score Models X vs Y similarly. Default to X.
4. Draft `docs/02-requirements/platform-mediation-scope.md` with all 8 sections
5. Draft `docs/08-tasks/active/platform-mediation-implementation.md`. Implementation Steps inside must reflect the picked Model A or B + Model X or Y.
6. Update `open-questions.md` item 2 → one-line resolution pointer
7. Update `feature-roadmap.md` mediation line → "scope decided, implementation pending"
8. Prepend `CHANGELOG.md` block under `### docs`
9. Move this task to `archived/`

## Test Plan

- Backend: not required
- Frontend: not required
- API validation: not required
- Manual checks (paste in Final Report):
  - chat-mvp-scope.md was read successfully (was the pre-flight artifact present?)
  - exactly one of Model A / B chosen
  - exactly one of Model X / Y chosen
  - mediation scope doc has all 8 sections
  - implementation task spec has all template sections
  - open-questions item 2 resolved; item 1 was already resolved by predecessor

## Acceptance Criteria

- [ ] Pre-flight step 1 passed (chat-mvp-scope-definition archived + scope doc present)
- [ ] `docs/02-requirements/platform-mediation-scope.md` exists with the 8 mandatory sections
- [ ] Model A or B chosen with written justification citing pre-flight findings
- [ ] Model X or Y chosen with written justification citing chat-mvp-scope.md content
- [ ] Schema diff (Model A) or new-table spec (Model B) is concrete (column names + types)
- [ ] API surface section lists every new/changed endpoint
- [ ] Admin surfaces section names specific UI changes in the named view
- [ ] `docs/08-tasks/active/platform-mediation-implementation.md` exists, structurally complete
- [ ] `open-questions.md` item 2 resolved
- [ ] `feature-roadmap.md` mediation line updated
- [ ] `CHANGELOG.md` block added
- [ ] This task moved to `archived/`

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] `docs/06-http/` — not for this task
- [x] roadmap docs (`open-questions.md`, `feature-roadmap.md`)
- [x] task status and archive move

## Final Report Format

```markdown
## Return Report — platform-mediation-boundary-definition

### A. Branch & Commit
- Branch: <name>
- Commit SHA: <sha>
- Files changed (paste `git diff --stat`)

### B. Pre-flight Findings
- chat-mvp-scope-definition.md archived: <yes/no — block here if no>
- platform-mediation-scope.md is being written referencing chat-mvp-scope.md entities: <list>
- Current report controller endpoints: <list with statuses>
- Current report admin view: <filename> — current action set: <list>
- reports table columns: <list>

### C. Implementation Walkthrough
- Step 2 → A vs B scoring: A=<score>, B=<score>. Chosen: <A/B>. Reason: <one line>
- Step 3 → X vs Y scoring: X=<score>, Y=<score>. Chosen: <X/Y>. Reason: <one line>
- Step 4 → mediation scope doc at docs/02-requirements/platform-mediation-scope.md (lines: <N>)
- Step 5 → implementation task at docs/08-tasks/active/platform-mediation-implementation.md (lines: <N>)
- Step 6 → open-questions item 2 resolution line: "<paste>"
- Step 7 → feature-roadmap.md mediation line: "<paste>"
- Step 8 → CHANGELOG block added
- Step 9 → task moved to archived

### D. Test Plan Results
- platform-mediation-scope.md section presence (all 8): <list with yes/no>
- Schema diff or new-table spec is concrete (column names + types): <yes/no, paste sample>
- Exactly one A/B chosen: <count of section headings>
- Exactly one X/Y chosen: <count>
- platform-mediation-implementation.md sections matching template: <yes/no>

### E. Acceptance Criteria Check
- [x/✗] one per criterion with evidence

### F. Deviations from Spec
- If A and B mixed, or X and Y both included, paste the rationale; "none" otherwise

### G. Out-of-scope Findings
- "none" or specific items

### H. Open Questions / Blockers
- "none" or single question
```

## Completion Notes

(Filled in by sub-agent.)
