# Goal Prompt: Authentication Upgrade Wave 1

## Metadata

- ID: authentication-upgrade-goal-prompt
- Status: archived
- Owner: main-agent
- Track: cross-cutting
- Depends on: human approval of `authentication-upgrade`
- Priority: high
- Planned date: 2026-05-31
- Completed date: 2026-06-01

## Prompt

You are the main Agent for the Youyu Authentication Upgrade Wave 1.

Repository:

- `E:\Dev\Projects\Youyu`

Create an isolated worktree and topic branch before implementation:

- suggested worktree: `E:\Dev\Projects\Youyu-auth-upgrade`
- suggested branch: `codex/authentication-upgrade`

Primary task:

- `docs/08-tasks/active/authentication-upgrade.md`

Requirement boundary:

- `docs/02-requirements/authentication-upgrade-scope.md`

## Required Read Order

1. `AGENTS.md`
2. `CLAUDE.md`
3. `docs/README.md`
4. `docs/04-standards/development-process.md`
5. `docs/02-requirements/authentication-upgrade-scope.md`
6. `docs/09-api-spec/auth.md`
7. `docs/06-http/auth.http`
8. `docs/08-tasks/active/authentication-upgrade.md`
9. all active authentication-upgrade child tasks
10. relevant frontend/backend module docs

## Locked Interfaces

- Normal registration and student verification remain separate.
- Registration requires a verified real email address but not a campus-domain
  email address.
- Registration must return the user to login and must not issue a JWT.
- Login remains password-based.
- Graphical CAPTCHA is required after three consecutive failed password
  attempts.
- Forgotten-password recovery uses a real emailed code and sets a new password.
- Email delivery uses configured SMTP. Do not implement a dev log-only sender.
- The `test` profile uses a deterministic fake sender without network access.
- Codes are hashed at rest and never returned or logged.
- SMS verification and email passwordless login are Wave 2 work.

## Hard Constraints

- Do not move or rewrite `CLAUDE.md`.
- Do not use archived task docs as current execution specs.
- Do not reintroduce `AdminDataStore`.
- Do not commit SMTP credentials, real recipient addresses, or verification
  codes.
- Do not alter or drop existing auth tables. Keep schema additions additive.
- Do not mix student-verification redesign into this initiative.
- Do not expand into JWT revocation or distributed rate limiting during Wave 1.
- Do not archive a worker task until its diff has been reviewed and verified by
  the main Agent.
- Do not push or merge unless the human explicitly requests it.

## Dispatch Plan

Activate the parent and child tasks only after human approval.

Use disjoint worker ownership:

1. Contract/schema worker:
   - owns `authentication-upgrade-contract-and-schema`
   - owns additive schema, challenge mappers, auth API spec, and HTTP examples
2. Backend worker:
   - starts after contract/schema stabilizes
   - owns `authentication-upgrade-backend`
   - owns SMTP wiring, auth services/controllers/DTOs, and backend auth tests
3. Frontend worker:
   - may begin after API contract stabilizes
   - owns `authentication-upgrade-frontend`
   - owns auth views, route, API adapter, store, and frontend auth tests
4. Main Agent:
   - owns `authentication-upgrade-integration`
   - reviews diffs, resolves narrow overlaps, runs real SMTP verification,
     updates operational docs and FAQ wording, closes tasks, and archives docs

Tell every worker:

- other agents are editing concurrently;
- do not revert other agents' edits;
- do not expand scope;
- do not commit;
- return changed files, checks run, findings, and blockers.

## Verification

Run:

```powershell
cd backend
.\mvnw.cmd test

cd ..\frontend
npm test
npm run build

cd ..
git diff --check
git status --short --branch
```

Then exercise:

1. real registration-email delivery and verified registration
2. registration success returning to login without a session
3. three failed logins followed by required CAPTCHA
4. real forgotten-password email delivery and password reset
5. login using the new password

## Final Report

Report:

1. branch and worktree
2. parent and child task status
3. changed files
4. verification commands and results
5. SMTP manual-verification evidence without secrets or codes
6. scan warnings and blockers
7. deferred Wave 2 work
8. commit, push, and PR status

## Completion Notes

Delivery is complete. Real SMTP registration and password-reset acceptance ran
successfully with locally injected credentials outside chat, and the task is
closed without committing any secrets, recipient addresses, or verification
codes.
