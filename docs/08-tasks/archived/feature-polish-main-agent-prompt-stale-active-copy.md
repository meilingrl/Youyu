# Main-Agent Launch Prompt: Feature Polish Closeout

Use this prompt to launch the implementation-owning main Agent.

```text
Execute the Youyu feature-polish closeout in E:\Dev\Projects\Youyu.

You are the implementation-owning main Agent. Read repository governance before editing:
1. AGENTS.md
2. CLAUDE.md
3. docs/README.md
4. docs/04-standards/development-process.md
5. docs/05-roadmap/current/stage-roadmap.md
6. docs/05-roadmap/current/launch-preparation-roadmap.md
7. docs/08-tasks/active/feature-polish-closeout-parent.md
8. all child tasks under docs/08-tasks/active/feature-polish-*.md
9. frontend/README.md and backend/README.md

Important current-state warning:
- The originating orchestration pass saw an already dirty working tree on branch chore/docs-cleanup-and-stage-patch.
- Before implementation, run git status --short --branch and inspect current diffs.
- Preserve unrelated user/agent changes. Do not revert them.
- If the tree is still dirty, create an isolated worktree or explicitly separate the existing work before dispatching workers.

Operating mode:
- This is a multi-wave delivery. Do not implement everything in one broad pass.
- Prefer simple tasks first.
- Dispatch workers only with disjoint ownership.
- Do not allow workers to commit.
- Do not push unless the human explicitly requests it.
- Archive tasks only after main-agent diff review and verification.

Recommended branch/worktree:
- Branch: codex/feature-polish-closeout
- Worktree: ..\Youyu-feature-polish-closeout

Wave 1:
- feature-polish-messages-chat-window-layout
- feature-polish-explore-filter-search-sort
- feature-polish-cart-selection-review-images

Wave 2:
- feature-polish-refund-logistics-map-reconciliation
- feature-polish-admin-permission-assignment
- feature-polish-admin-dashboard-metrics

Wave 3:
- feature-polish-admin-data-export

Final:
- feature-polish-integration-verification

Locked interfaces:
- Keep ApiResponse<T> response envelope.
- Keep frontend API wrappers under frontend/src/api/modules/.
- Normalize backend responses at API/store boundary.
- Use existing Element Plus and common components.
- Protect backend endpoints with @LoginRequired; admin mutations must use AdminPermission.
- Product list sort keys must be allowlisted. Proposed values: price_asc, price_desc, sales_desc, newest.
- Logistics/map work must have a graceful fallback when no provider key is configured.
- Export endpoints must not expose passwords, password hashes, JWTs, verification codes, SMTP config, or excessive PII.

Worker prompt requirements:
- Assign exactly one child task.
- List allowed and forbidden files.
- Tell the worker other agents may be editing concurrently.
- Tell the worker not to revert unrelated changes and not to commit.
- Require final response with changed files, checks run, findings, blockers, and contract/doc updates.

Verification minimum:
- cd backend; .\mvnw.cmd test
- cd frontend; npm test
- cd frontend; npm run build
- git diff --check
- Manual/browser checks for messages, explore, cart/reviews/refunds/logistics, admin users, admin dashboard, and export.

Documentation closeout:
- Update CHANGELOG.md.
- Update docs/06-http/*.http for changed endpoints.
- Update docs/09-api-spec/*.md for changed endpoint contracts.
- Fill completion notes.
- Move completed child tasks and parent task to docs/08-tasks/archived/.

Final report:
1. branch and worktree
2. child task status
3. changed-file summary by task
4. verification commands and results
5. manual checks completed
6. scan warnings
7. blockers
8. deferred provider-backed work
9. push/PR status
```

