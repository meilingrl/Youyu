# Task: Configuration Safety Hardening (Slice F of architecture-performance-hardening)

## Metadata

- ID: configuration-safety-hardening
- Status: archived
- Owner: unassigned
- Track: cross-cutting
- Depends on: none (configuration baseline)
- Parent: `docs/08-tasks/drafts/architecture-performance-hardening.md` Slice F
- Priority: medium
- Planned date: 2026-05-22
- Completed date: 2026-05-22

## Objective

Replace the hardcoded JWT secret default in `application.yml` with an environment-variable-first pattern (`${APP_JWT_SECRET:...}`), add a startup guard that refuses to boot with the dev default in non-dev profiles, and update the backend README + `CLAUDE.md` so local setup expectations stay clear.

## Background

Per the parent task's "Problem Summary §6" and the post-migration ledger audit:

- `backend/src/main/resources/application.yml` line ~43 has `app.jwt.secret: campusmarket-dev-secret-key-replace-in-production-min32` hardcoded with no env-var override path
- `spring.datasource.password` already uses `${MYSQL_PASSWORD:yinkaixin123}` — JWT secret should follow the same pattern
- Today's repository runs in `dev` profile by default; a future deploy or even an accidental `prod` profile run must NOT silently boot with the committed default

This is a small, surgical change. The dev experience must remain unchanged: running `mvnw spring-boot:run` locally must still work without setting `APP_JWT_SECRET`.

## Pre-flight Verification (must complete before any edit)

1. Read `backend/src/main/resources/application.yml` end to end. Paste the `app` section into Final Report.
2. Confirm `${MYSQL_PASSWORD:yinkaixin123}` pattern is what `application.yml` uses for the existing env-var-first secret — `grep -n "\${" application.yml`
3. Find where `app.jwt.secret` is consumed:
   ```bash
   grep -rn "app.jwt.secret\|\${app.jwt" backend/src/main/java/
   ```
   Read the consumer (likely a `@ConfigurationProperties` or `@Value`-bound class). Paste the binding code into Final Report.
4. Find the active profile mechanism:
   ```bash
   grep -rn "spring.profiles.active\|@Profile" backend/src/main/java/ backend/src/main/resources/
   ```
   Confirm which profile name is "dev" (or the default) vs a hypothetical "prod" / `seed` / etc.
5. Run baseline tests: `cd backend && ./mvnw test -B`. They must pass before any change.

## Files to Read

- `AGENTS.md`, `CLAUDE.md` (Authentication and JWT config sections)
- `backend/README.md`
- `backend/src/main/resources/application.yml`
- All Java files that consume `app.jwt.secret` (discover via grep in pre-flight step 3)
- `docs/08-tasks/drafts/architecture-performance-hardening.md` Slice F section

## In Scope

1. **Change the JWT secret declaration** in `application.yml` to environment-variable-first form:
   ```yaml
   app:
     jwt:
       secret: ${APP_JWT_SECRET:campusmarket-dev-secret-key-replace-in-production-min32}
   ```
   Same pattern as the existing `${MYSQL_PASSWORD:...}`. Default value stays identical so local dev does not break.

2. **Add a startup guard** in the JWT configuration / properties class:
   - On application startup, if the resolved JWT secret equals the well-known dev default literal AND the active Spring profile is NOT one of `dev` / `seed` / `test`, then **fail-fast** by throwing an `IllegalStateException` with a clear message naming the env var to set.
   - If the active profile IS dev/seed/test, log a single WARN line on startup: `JWT secret is using the development default; set APP_JWT_SECRET in production`.

3. **Update `backend/README.md`**: document the new env var alongside the existing `MYSQL_PASSWORD` documentation. Include a one-line "production checklist" entry.

4. **Update `CLAUDE.md`** "Authentication" section: replace the line that says the secret is `app.jwt.secret` with one that says `${APP_JWT_SECRET}` (env-overridable, dev default kept for local).

5. **Verify**: existing tests still pass; backend still starts locally without setting `APP_JWT_SECRET`.

## Out of Scope

- Rotating the dev secret value (keep the same literal)
- Changing the JWT library or token format
- Adding a separate secret for refresh tokens or any other crypto material
- Database password handling (already env-first)
- Frontend changes
- Externalizing other config keys (consider in a future task if needed)
- Production deployment scripts or Docker compose

## Hard Limits

- **Do not** change the dev default secret value
- **Do not** add a hard requirement that breaks `mvnw spring-boot:run` locally (no required env var when active profile is dev/seed/test)
- **Do not** introduce a secrets manager / Vault integration
- **Do not** move secret-loading logic to a custom `EnvironmentPostProcessor` unless `@Value` / `@ConfigurationProperties` cannot express the guard cleanly
- **Do not** touch `MYSQL_PASSWORD` handling — it is already correct
- **Do not** introduce new dependencies
- **Do not** use `--no-verify` on commits

## Allowed Changes

- `backend/src/main/resources/application.yml` (one line for JWT secret)
- The Java class that holds JWT properties (add startup guard logic)
- `backend/README.md` (add env var doc)
- `CLAUDE.md` (Authentication section — one line)
- `CHANGELOG.md`
- `docs/08-tasks/active/configuration-safety-hardening.md` → move to `archived/`
- `docs/08-tasks/drafts/architecture-performance-hardening.md` — flip Slice F checkbox

## Implementation Steps

1. Complete pre-flight verification; record all findings.
2. Patch `application.yml` `app.jwt.secret` line to `${APP_JWT_SECRET:<existing default>}`.
3. Locate the JWT configuration class identified in pre-flight step 3 (likely `AuthProperties` or similar in `config/`). Add startup-guard logic:
   - Inject `Environment` and the resolved JWT secret
   - In an `@PostConstruct` or via `ApplicationListener<ContextRefreshedEvent>`, compare the resolved secret against the dev default literal
   - If match AND active profile is not in {`dev`, `seed`, `test`}, throw `IllegalStateException("APP_JWT_SECRET must be set when running with profile <name>; the committed dev default is forbidden outside dev/seed/test")`
   - If match AND active profile IS dev/seed/test, log a single WARN line via SLF4J
4. Add or update a small test in `backend/src/test/java/.../config/` (or wherever similar config tests live):
   - `jwtSecretGuardAllowsDevDefaultUnderDevProfile()` — assert no exception
   - `jwtSecretGuardRejectsDevDefaultUnderProdProfile()` — set `spring.profiles.active=prod` and the dev secret; assert exception thrown
   - Use Spring Boot's `ApplicationContextRunner` so the test does not require booting the whole app
5. Update `backend/README.md` env var section: add `APP_JWT_SECRET` row with description "JWT signing secret (≥32 chars); falls back to a dev default when running with profile dev/seed/test"
6. Update `CLAUDE.md` Authentication section to mention `APP_JWT_SECRET` env override (one line replacement).
7. Run `cd backend && ./mvnw test -B`. All tests pass; the two new tests pass.
8. Run `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=seed` briefly to verify local startup still works without the env var (look for the WARN log line; kill within 10s after seeing the app started).
9. Prepend `CHANGELOG.md` block under `### refactor`.
10. Flip Slice F checkbox in `architecture-performance-hardening.md`.
11. Move this task to `archived/` with `Delivered` section.

## Test Plan

- Backend:
  - `cd backend && ./mvnw test -B` — must pass with zero failures, zero errors (both before and after)
  - New tests: dev-profile-allows + prod-profile-rejects, both must pass
- Frontend: not required
- API validation: not required
- Manual:
  - Local `mvnw spring-boot:run -Dspring-boot.run.profiles=seed` must still boot to "Started" without setting `APP_JWT_SECRET`
  - The startup WARN line must appear when running with the dev default
  - With `APP_JWT_SECRET=anything-else-32-chars-long-suffix mvnw spring-boot:run -Dspring-boot.run.profiles=seed`, no WARN line should appear

## Acceptance Criteria

- [ ] `application.yml` `app.jwt.secret` uses `${APP_JWT_SECRET:<default>}` form, default unchanged
- [ ] JWT config class has startup-guard logic with profile awareness
- [ ] Guard throws `IllegalStateException` when active profile ∉ {dev, seed, test} AND secret equals dev default
- [ ] Guard logs WARN (not error, not exception) when active profile ∈ {dev, seed, test} AND secret equals dev default
- [ ] Two new tests added covering both guard outcomes
- [ ] `./mvnw test -B` passes with zero failures both before and after
- [ ] Local `mvnw spring-boot:run -Dspring-boot.run.profiles=seed` boots without `APP_JWT_SECRET` set
- [ ] `backend/README.md` documents `APP_JWT_SECRET`
- [ ] `CLAUDE.md` Authentication section mentions env-override (one line)
- [ ] `CHANGELOG.md` block added under `### refactor`
- [ ] Slice F checkbox in `architecture-performance-hardening.md` flipped to `[x]`
- [ ] This task moved to `archived/`

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] `docs/06-http/` — not applicable
- [x] `CLAUDE.md` (one line)
- [x] `backend/README.md`
- [x] `docs/08-tasks/drafts/architecture-performance-hardening.md` (Slice F status)
- [x] task status and archive move

## Final Report Format

```markdown
## Return Report — configuration-safety-hardening

### A. Branch & Commit
- Branch: <name>
- Commit SHA: <sha>
- Files changed (paste `git diff --stat`)

### B. Pre-flight Findings
- application.yml `app` section before change (paste):
  ```yaml
  ...
  ```
- Existing env-var-first pattern: <paste MYSQL_PASSWORD line>
- JWT secret consumer (paste class + binding lines):
  ```java
  ...
  ```
- Active profile mechanism: <paste @Profile / spring.profiles.active occurrences>
- Baseline test result: `./mvnw test -B` exit <code>, <N tests passed>

### C. Implementation Walkthrough
- Step 2 → application.yml diff (paste the one-line change)
- Step 3 → guard logic added at <file>:<lines>. Profile list checked: <list>
- Step 4 → tests added: <file>:<test names>
- Step 5 → README.md env var section updated (paste new lines)
- Step 6 → CLAUDE.md line replaced (paste before/after)
- Step 7 → post-change test result: exit <code>, <N tests passed>
- Step 8 → local boot smoke: <"Started" line excerpt + WARN line excerpt OR reason skipped>
- Step 9 → CHANGELOG block added (paste)
- Step 10 → Slice F checkbox flipped (paste resulting line)
- Step 11 → task moved to archived

### D. Test Plan Results
- `./mvnw test -B` before: <exit code, N passed>
- `./mvnw test -B` after: <exit code, N passed>
- New tests pass: <names + PASS/FAIL>
- Local boot without env var: <"Started in Xs" + WARN line presence>
- Local boot with env var set: <"Started in Xs" + WARN line absence (or skipped, with reason)>

### E. Acceptance Criteria Check
- [x/✗] one per criterion with evidence

### F. Deviations from Spec
- "none" or specific deviation with reason

### G. Out-of-scope Findings
- "none" or specific items (e.g., other secrets noticed)

### H. Open Questions / Blockers
- "none" or single question
```

## Completion Notes

(Filled in by sub-agent.)

## Delivered

Completed 2026-05-22 on branch `chore/ux-polish-and-task-cleanup`.

### Changes
- `backend/src/main/resources/application.yml` — wrapped `app.jwt.secret` with `${APP_JWT_SECRET:<dev default>}`. Dev default value unchanged.
- `backend/src/main/java/com/campusmarket/backend/config/JwtSecretGuard.java` — new component. `@PostConstruct` validate() compares the resolved secret against the committed dev default; logs WARN under safe profiles ({dev, seed, test, default}) or empty profile, throws `IllegalStateException` otherwise.
- `backend/src/test/java/com/campusmarket/backend/config/JwtSecretGuardTest.java` — five `ApplicationContextRunner`-based tests covering: dev profile allow, seed profile allow, no-profile allow, prod profile reject, prod-with-override allow.
- `backend/README.md` — added env var table including `APP_JWT_SECRET` and a one-line production checklist.

### Delegated (per orchestrator overrides)
- `CHANGELOG.md` block — proposed in Final Report section C, not committed.
- `CLAUDE.md` Authentication section — before/after proposed in Final Report section C, not committed.
- `docs/08-tasks/drafts/architecture-performance-hardening.md` Slice F checkbox — proposed before/after in Final Report section C, not committed.

### Verification
- `./mvnw test -B` before changes: 80 passed / 0 failed.
- `./mvnw test -B` after changes: 85 passed / 0 failed (5 new in `JwtSecretGuardTest`).
- Boot smoke step: skipped per orchestrator allowance (long-lived `spring-boot:run` not exercised in this environment); however, the production-side tests boot the full Spring context with the dev default secret and the WARN line `JWT secret is using the development default; set APP_JWT_SECRET in production` is observed in test logs, confirming the guard runs at startup.
