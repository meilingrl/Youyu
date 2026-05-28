# Task: Chat MVP Scope Recovery

## Metadata

- ID: chat-mvp-scope-recovery
- Status: completed
- Owner: Codex
- Track: cross-cutting
- Depends on: archived `chat-mvp-scope-definition`
- Priority: high
- Planned date: 2026-05-28
- Completed date: 2026-05-28

## Objective

Restore or recreate the missing `docs/02-requirements/chat-mvp-scope.md` so platform mediation can depend on an accepted current requirements artifact instead of an archived task.

## Background

The roadmap and active mediation task identify a document gap: `docs/08-tasks/archived/chat-mvp-scope-definition.md` exists, but its expected delivered artifact `docs/02-requirements/chat-mvp-scope.md` is missing. Repository rules forbid treating archived tasks as current execution specs.

## Scope

- Read existing chat implementation and archived chat tasks.
- Produce a current requirement document for chat MVP scope.
- Clearly define what chat owns and what it does not own.
- Include the mediation-relevant rule that admins may have read-only access to related chat context in a future mediation feature, but must not join or send messages in user conversations for v1 mediation.

## Out of Scope

- Chat code changes.
- New admin chat endpoints.
- Mediation implementation.
- Group chat or three-party support chat.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `docs/05-roadmap/current/admin-module-goal-roadmap.md`
- `docs/08-tasks/archived/chat-mvp-scope-definition.md`
- `docs/08-tasks/archived/ui-redesign-messages-support.md`
- `backend/src/main/resources/schema.sql` chat tables
- backend chat controller/service files
- frontend chat/message views and API modules
- `docs/09-api-spec/` chat-related specs if present
- `docs/06-http/` chat-related HTTP files if present

## Allowed Changes

- `docs/02-requirements/chat-mvp-scope.md`.
- `docs/05-roadmap/current/feature-roadmap.md` status line if the scope gap is resolved.
- `docs/05-roadmap/current/admin-module-goal-roadmap.md` only if dependency wording changes.
- `CHANGELOG.md`.
- This task document lifecycle updates.

## Implementation Plan

1. Confirm the missing artifact and inspect current chat runtime behavior.
2. Reconstruct the requirements scope from code and archived task records.
3. Document MVP entities, user-visible behavior, API ownership, admin visibility limitations, and non-goals.
4. Update roadmap status so mediation can proceed.

## File Scope

This is a documentation recovery task. Do not edit frontend, backend, database schema, seed data, or API runtime files.

## API / Data Contract Impact

No API change is expected. The document should describe current chat contracts rather than invent new ones.

If current code and archived docs conflict, runtime behavior wins and the mismatch must be recorded.

## Risks

- Accidentally turning an archived task into the current spec without checking code.
- Expanding chat into admin support or group governance.
- Leaving mediation-relevant chat visibility ambiguous.

## Verification Plan

- Documentation review: confirm `docs/02-requirements/chat-mvp-scope.md` exists.
- Documentation review: confirm mediation-relevant admin read-only visibility is explicitly constrained.
- Run `git diff --check`.
- No backend/frontend test is required unless code is changed, which is out of scope.

## Acceptance Criteria

- [x] `docs/02-requirements/chat-mvp-scope.md` exists.
- [x] The document separates chat MVP, support console, and platform mediation responsibilities.
- [x] Admin mediation v1 chat visibility is documented as read-only context only.
- [x] No code or schema files are changed.
- [x] Relevant roadmap dependency status is updated.
- [x] `CHANGELOG.md` is updated.
- [x] `git diff --check` passes.
- [x] Completion notes are filled before archive.

## Sub-agent Instructions

- Do not implement chat or mediation behavior.
- Do not add admin chat endpoints.
- Use code as runtime truth when reconstructing scope.
- Return the created document path, any code/doc conflicts found, and whether platform mediation is unblocked.

## Feedback To Head Agent

Return:

- created requirement document path;
- code versus archived-doc conflicts found;
- mediation unblock status;
- verification commands and results;
- unresolved chat scope risks.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/` if no API changes are expected
- [x] roadmap or standards docs if applicable
- [x] task status and archive move

## Completion Notes

Completed by Codex on 2026-05-28.

Created `docs/02-requirements/chat-mvp-scope.md` using current runtime code, schema, API spec, and HTTP smoke collection as truth sources. The recovered scope records the implemented user-to-user chat surface, including conversations, message list/send, unread/read, image messages, product/order cards, message search, pin/mute/delete, recall, quick replies, and auto-reply behavior.

The document explicitly separates chat MVP, support console, and platform mediation. It states that future mediation v1 may expose related buyer/seller chat context only as read-only dispute context, and that admins must not join, send, or mutate user conversations.

Updated `docs/05-roadmap/current/feature-roadmap.md` and `docs/05-roadmap/current/admin-module-goal-roadmap.md` so platform mediation is no longer blocked by the missing chat scope artifact. Updated `CHANGELOG.md`.

Archived task records were used only as historical references. Runtime/API/spec/http conflicts found and documented in the recovered requirement: early archived scope deferred unread counts, last-message previews, image messages, search, conversation management, recall, quick replies, and auto-reply, but the current runtime implements them. No runtime code or schema changes were made.

Verification: `git diff --check` passed.
