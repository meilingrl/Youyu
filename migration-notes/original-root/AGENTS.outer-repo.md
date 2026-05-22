# AGENTS.md

This file gives working guidance to AI coding agents operating in this repository.

## Repository Scope

- Repository root contains the active project `Youyu/`, course materials such as `Final/` and `课件/`, plus requirement documents.
- The active software project is `Youyu/`.
- Unless the human explicitly asks otherwise, treat `Youyu/` as the default work area and do not modify other top-level folders.

## Default Working Rules

- Read the target module README and any nearby docs before editing.
- Prefer the smallest possible change that satisfies the request.
- Do not refactor unrelated code while completing a focused task.
- Do not delete course materials, archived assignments, or generated seed data unless explicitly instructed.
- Keep changes consistent with the repository's existing structure and naming.

## Project Entry Points

If working in `Youyu/`, read these first:

1. `Youyu/CLAUDE.md`
2. `Youyu/README.md`
3. The relevant subproject README:
   - `Youyu/frontend/README.md`
   - `Youyu/backend/README.md`
   - `Youyu/database/README.md`

## Youyu Overview

- Monorepo for a campus marketplace system.
- Frontend: Vue 3 + Vite + Pinia + Element Plus
- Backend: Spring Boot 3.3 + JDBC
- Runtime database: H2
- Target migration direction: MySQL design assets live under `Youyu/database/`

## Youyu Directory Map

- `Youyu/frontend/`: Vue application
- `Youyu/backend/`: Spring Boot application
- `Youyu/database/`: database design and migration reference material
- `Youyu/docs/`: project docs, ADRs, task specs, HTTP request files
- `Youyu/resources/`: static references and supporting materials
- `Youyu/scripts/`: helper scripts
- `Youyu/tests/`: test records or test assets

## Commands

Run commands from the corresponding subdirectory.

### Frontend

- Install: `npm ci`
- Dev server: `npm run dev`
- Build: `npm run build`
- Unit tests: `npm run test`
- E2E tests: `npm run test:e2e`

### Backend

- First-time seeded run: `mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=seed`
- Normal run: `mvnw.cmd spring-boot:run`
- Tests: `mvnw.cmd test`
- Package: `mvnw.cmd clean package`

## Editing Conventions For Youyu

- Preserve the existing frontend/backend split.
- For backend changes, follow the controller -> service -> mapper -> entity layering already documented in `Youyu/CLAUDE.md`.
- Prefer JDBC mapper-based patterns already used in the backend; do not introduce a new ORM layer casually.
- New frontend API calls should go through `frontend/src/api/`.
- Shared frontend state belongs in Pinia stores, not ad hoc globals.
- Keep backend responses aligned with the unified `ApiResponse` envelope whenever you add or change endpoints.

## Database Safety

- Treat `Youyu/backend/src/main/resources/schema.sql` as the runtime schema source.
- Treat files in `Youyu/database/` as design and migration references, not the default live runtime source.
- Do not make destructive schema changes without explicit human approval.
- Avoid touching seed SQL unless the task clearly requires it.

## Task And Documentation Hygiene

- If a task spec exists under `Youyu/docs/dev/tasks/`, follow it strictly.
- If an endpoint changes, update the matching file under `Youyu/docs/http/` when appropriate.
- If a meaningful architecture decision is introduced, add an ADR under `Youyu/docs/decisions/` instead of burying rationale in code comments.
- Keep `Youyu/CHANGELOG.md` updated when completing a substantive task in that project.

## Out Of Scope By Default

- Do not modify `Final/` or `课件/` unless the human explicitly names them.
- Do not rewrite large parts of the project just to improve style.
- Do not change build tools, framework versions, or repository structure unless requested.

## When In Doubt

- Ask which top-level project to work on if the request does not clearly point to `Youyu/`.
- If instructions in a deeper directory conflict with this file, the deeper and more specific instructions win.
