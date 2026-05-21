# Agent Dispatch Prompts For Current Unfinished Work

Use these prompts with the matching task files in `docs/08-tasks/active/`.

Common requirements for every prompt:

- read `AGENTS.md`, `CLAUDE.md`, `docs/README.md`, and the assigned task file first
- stay inside the task's `Allowed Changes`
- do not silently widen scope
- before finishing, update the task doc itself:
  - set real completion metadata
  - refresh `Completion Notes`
  - update checklist items
  - move the task to `docs/08-tasks/archived/` if complete
- prepend `CHANGELOG.md` if the task makes a substantive change

## Wave 1

### `roadmap-hot-search-state-reconciliation.md`

```text
Read AGENTS.md, CLAUDE.md, docs/README.md, docs/04-standards/development-process.md, docs/05-roadmap/current/stage-roadmap.md, docs/05-roadmap/current/feature-roadmap.md, docs/05-roadmap/current/open-questions.md, and docs/08-tasks/active/roadmap-hot-search-state-reconciliation.md.

Goal: reconcile roadmap/open-questions state with the actual completed hot-search P3 delivery and current next-step priorities. This is a docs-only task. Do not implement product code. Keep changes limited to roadmap/open-questions/task/changelog files allowed by the task doc.

Before finishing, update the task record and archive it if complete.
```

### `api-spec-review-module-standardization.md`

```text
Read AGENTS.md, CLAUDE.md, docs/README.md, docs/04-standards/development-process.md, docs/05-roadmap/current/feature-roadmap.md, docs/09-api-spec/README.md, docs/09-api-spec/API_SPEC_TEMPLATE.md, docs/06-http/review.http, backend review controllers/services, and docs/08-tasks/active/api-spec-review-module-standardization.md.

Goal: produce the formal review API spec and correct direct review-controller to review.http drift if found. Stay contract-focused. Do not refactor backend behavior unless a tiny code fix is required to resolve an exposed contract defect and the task doc still permits it.

Before finishing, update the task record and archive it if complete.
```

### `registration-flow-contract-diagnosis.md`

```text
Read AGENTS.md, CLAUDE.md, docs/README.md, frontend/README.md, backend/README.md, the auth HTTP assets, auth controller/service code, frontend auth/register views or forms, and docs/08-tasks/active/registration-flow-contract-diagnosis.md.

Goal: diagnose why registration can reach the backend with username=null and make the smallest fix that restores a valid contract. Tie the root cause to code or runtime behavior, not guesses. Do not broaden into general auth redesign.

Run the checks listed in the task doc, then update the task record and archive it if complete.
```

### `seller-publish-loading-diagnosis.md`

```text
Read AGENTS.md, CLAUDE.md, docs/README.md, frontend/README.md, backend/README.md, seller publish/store/api/router files, and docs/08-tasks/active/seller-publish-loading-diagnosis.md.

Goal: find the real cause of the seller publish page hanging in loading state and apply the smallest reliable fix. Separate frontend-state bugs from backend/API failure modes with evidence.

Run the checks listed in the task doc, then update the task record and archive it if complete.
```

## Wave 2

### `chat-mvp-scope-definition.md`

```text
Read AGENTS.md, CLAUDE.md, docs/README.md, docs/05-roadmap/current/stage-roadmap.md, docs/05-roadmap/current/feature-roadmap.md, docs/05-roadmap/current/open-questions.md, docs/02-requirements/communication-and-after-sales-boundary.md, docs/03-architecture/frontend-information-architecture.md, docs/08-tasks/active/chat-mvp-scope-definition.md, and the archived messages/support UI task.

Goal: turn the current chat MVP uncertainty into an executable scope/task spec without building chat code. Define the minimum first version, dependencies, and explicit out-of-scope boundary.

Before finishing, update the task record and archive it if complete.
```

### `api-spec-report-module-standardization.md`

```text
Read AGENTS.md, CLAUDE.md, docs/README.md, docs/04-standards/development-process.md, docs/05-roadmap/current/feature-roadmap.md, docs/09-api-spec/README.md, docs/09-api-spec/API_SPEC_TEMPLATE.md, docs/06-http/report.http, backend report/admin controllers and services, and docs/08-tasks/active/api-spec-report-module-standardization.md.

Goal: produce the formal report API spec and correct direct report-controller to report.http drift if found. Stay contract-focused and avoid unrelated governance refactors.

Before finishing, update the task record and archive it if complete.
```

### `frontend-bundle-second-pass-planning.md`

```text
Read AGENTS.md, CLAUDE.md, docs/README.md, docs/05-roadmap/current/stage-roadmap.md, docs/05-roadmap/current/feature-roadmap.md, frontend/README.md, the archived frontend-bundle-optimization task, and docs/08-tasks/active/frontend-bundle-second-pass-planning.md.

Goal: plan the next small bundle-governance slice after the first pass. Baseline the current state, identify the highest-value low-risk next step, and write an execution-ready plan. Do not land a broad refactor in this task.

Before finishing, update the task record and archive it if complete.
```

### `user-facing-enum-label-normalization.md`

```text
Read AGENTS.md, CLAUDE.md, docs/README.md, frontend/README.md, the task doc, and the listed storefront/seller views that still render raw snake_case values.

Goal: normalize user-facing enum/status/fulfillment labels into readable Chinese on the scoped pages, using a shared mapping approach rather than ad hoc inline fixes. Do not edit admin governance pages owned by another task.

Run the checks listed in the task doc, then update the task record and archive it if complete.
```

## Wave 3

### `admin-governance-action-consistency.md`

```text
Read AGENTS.md, CLAUDE.md, docs/README.md, frontend/README.md, backend/README.md, admin governance views/controllers/services, and docs/08-tasks/active/admin-governance-action-consistency.md.

Goal: make verification/report/shop governance actions consistent and explainable without inventing new business states. If current backend semantics are the cause, fix the minimum backend/frontend contract needed and document it.

Run the checks listed in the task doc, then update the task record and archive it if complete.
```

### `preference-theme-capability-gap.md`

```text
Read AGENTS.md, CLAUDE.md, docs/README.md, frontend/README.md, settings/preferences/theme-related frontend files, and docs/08-tasks/active/preference-theme-capability-gap.md.

Goal: audit the current preference/theme capability gap and land the smallest honest improvement. Either implement the missing supported capability cleanly or downgrade the UI so it no longer overpromises unsupported behavior.

Run the checks listed in the task doc, then update the task record and archive it if complete.
```

### `review-entry-and-seed-flow-bridge.md`

```text
Read AGENTS.md, CLAUDE.md, docs/README.md, frontend/README.md, backend/README.md, review/order/product-detail files, seed data references if needed, and docs/08-tasks/active/review-entry-and-seed-flow-bridge.md.

Goal: make the review flow visible and testable with the minimum safe scope: add a product-detail review entry affordance and make sure there is at least one realistic path to exercise completed-order review behavior in local/demo flow.

Do not widen into a full review-system redesign. Before finishing, update the task record and archive it if complete.
```

## Wave 4

### `platform-mediation-boundary-definition.md`

```text
Read AGENTS.md, CLAUDE.md, docs/README.md, docs/05-roadmap/current/open-questions.md, docs/02-requirements/communication-and-after-sales-boundary.md, docs/08-tasks/active/chat-mvp-scope-definition.md if already completed, and docs/08-tasks/active/platform-mediation-boundary-definition.md.

Goal: define the boundary between chat, report, and platform mediation as an executable docs/spec task. This is a docs-only task and should not start until the chat MVP scope task is complete enough to reference.

Before finishing, update the task record and archive it if complete.
```
