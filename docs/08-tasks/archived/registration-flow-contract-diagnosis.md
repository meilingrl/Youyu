# Task: Registration Flow Contract Diagnosis

## Metadata

- ID: registration-flow-contract-diagnosis
- Status: archived
- Owner: unassigned
- Track: feature
- Depends on: current auth frontend/backend baseline
- Priority: high
- Planned date: 2026-05-20
- Completed date: 2026-05-20

## Objective

Diagnose why registration requests can hit the backend with `username = null` and make the smallest reliable fix so registration follows a valid frontend-to-backend contract again.

## Background

The raw issue backlog includes a real validation failure from `AuthController.register(...)` where `RegisterRequest.username` arrives as null. This is a contract bug, not just a copy issue.

## Scope

- reproduce or trace the null-username path
- identify whether the cause is frontend payload shaping, field naming, form binding, request DTO expectations, or another contract mismatch
- apply the minimum fix and verify registration flow behavior

## Out of Scope

- full auth redesign
- login/privilege/session cleanup unrelated to this bug
- unrelated form restyling

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `frontend/README.md`
- `backend/README.md`
- `docs/06-http/auth.http`
- `frontend/src/views/auth/LoginView.vue`
- any register view/component and auth store/api modules
- `backend/src/main/java/com/campusmarket/backend/controller/auth/AuthController.java`
- register request DTO and auth service files

## Allowed Changes

- relevant auth frontend files
- relevant auth backend files
- `docs/06-http/auth.http` if contract examples need correction
- `docs/09-api-spec/auth.md` only if the contract changes
- `CHANGELOG.md`
- task lifecycle files under `docs/08-tasks/`

## Implementation Plan

1. Reproduce or statically trace the registration payload path until the null field source is concrete.
2. Fix the narrowest broken contract point.
3. Verify the registration path and update docs only if the real contract changed.

## Risks

- fixing symptoms instead of the true payload mismatch
- widening into general auth cleanup

## Test Plan

- Backend:
  - run focused auth tests if present
- Frontend:
  - run relevant frontend tests if touched
- API validation:
  - confirm `auth.http` still matches runtime truth
- Manual:
  - verify registration no longer submits a null username

## Acceptance Criteria

- [x] The root cause of the null-username registration failure is identified concretely
- [x] A minimal fix is applied at the correct contract boundary
- [x] Registration is verified after the fix
- [x] Documentation is updated if and only if runtime contract changed

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/` — not needed; contract unchanged
- [x] roadmap or standards docs if applicable — not needed
- [x] task status and archive move

## Completion Notes

**Root cause**: Field-name mismatch between frontend and backend.

The frontend `LoginView.vue` registerForm uses `account` as the field name for the login identifier:
```js
const registerForm = reactive({
  nickname: '',
  account: '',      // <-- this is the username
  password: '',
  confirmPassword: ''
})
```

The frontend API module `register()` in `frontend/src/api/modules/auth.js` passed the payload straight through to `POST /api/auth/register` with no field remapping. The backend `RegisterRequest.java` DTO expects the JSON key `username` (annotated `@NotBlank`). Since the JSON body contained `account` but no `username`, Spring deserialized `username = null`, triggering the `@NotBlank` validation failure.

Note that `login()` in the same module already had the correct pattern — it maps `payload.loginId || payload.account` — but `register()` was never given the same treatment.

**Fix**: Added field-name mapping in `frontend/src/api/modules/auth.js` register():
```js
export function register(payload) {
  return service.post('/auth/register', {
    username: payload.username || payload.account || '',
    password: payload.password || '',
    nickname: payload.nickname || '',
    phone: payload.phone || undefined,
    email: payload.email || undefined
  })
}
```

This mirrors the existing login() contract-adapter pattern. No backend changes needed; no contract changes — the runtime API contract was always correct, the frontend simply wasn't fulfilling it.

**Verification**:
- Frontend tests: 7 files, 30 tests, all passing
- Backend tests: 99 tests, 0 failures, 0 errors
- The `auth.http` smoke test already uses `username` (correct), so no update needed
- The API contract is unchanged — frontend now sends what backend expects
