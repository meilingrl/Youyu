# AGENTS.md

This file gives working guidance to AI coding agents operating in this repository.

## Repository Scope

- Repository root contains the active project `CampusMarket/`, course materials such as `Final/` and `课件/`, plus requirement documents.
- The active software project is `CampusMarket/`.
- Unless the human explicitly asks otherwise, treat `CampusMarket/` as the default work area and do not modify other top-level folders.

## Default Working Rules

- Read the target module README and any nearby docs before editing.
- Prefer the smallest possible change that satisfies the request.
- Do not refactor unrelated code while completing a focused task.
- Do not delete course materials, archived assignments, or generated seed data unless explicitly instructed.
- Keep changes consistent with the repository's existing structure and naming.

## Project Entry Points

If working in `CampusMarket/`, read these first:

1. `CampusMarket/CLAUDE.md`
2. `CampusMarket/README.md`
3. The relevant subproject README:
   - `CampusMarket/frontend/README.md`
   - `CampusMarket/backend/README.md`
   - `CampusMarket/database/README.md`

## CampusMarket Overview

- Monorepo for a campus marketplace system.
- Frontend: Vue 3 + Vite + Pinia + Element Plus
- Backend: Spring Boot 3.3 + JDBC
- Runtime database: H2
- Target migration direction: MySQL design assets live under `CampusMarket/database/`

## CampusMarket Directory Map

- `CampusMarket/frontend/`: Vue application
- `CampusMarket/backend/`: Spring Boot application
- `CampusMarket/database/`: database design and migration reference material
- `CampusMarket/docs/`: project docs, ADRs, task specs, HTTP request files
- `CampusMarket/resources/`: static references and supporting materials
- `CampusMarket/scripts/`: helper scripts
- `CampusMarket/tests/`: test records or test assets

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

## Editing Conventions For CampusMarket

- Preserve the existing frontend/backend split.
- For backend changes, follow the controller -> service -> mapper -> entity layering already documented in `CampusMarket/CLAUDE.md`.
- Prefer JDBC mapper-based patterns already used in the backend; do not introduce a new ORM layer casually.
- New frontend API calls should go through `frontend/src/api/`.
- Shared frontend state belongs in Pinia stores, not ad hoc globals.
- Keep backend responses aligned with the unified `ApiResponse` envelope whenever you add or change endpoints.

## Database Safety

- Treat `CampusMarket/backend/src/main/resources/schema.sql` as the runtime schema source.
- Treat files in `CampusMarket/database/` as design and migration references, not the default live runtime source.
- Do not make destructive schema changes without explicit human approval.
- Avoid touching seed SQL unless the task clearly requires it.

## Task And Documentation Hygiene

- If a task spec exists under `CampusMarket/docs/dev/tasks/`, follow it strictly.
- If an endpoint changes, update the matching file under `CampusMarket/docs/http/` when appropriate.
- If a meaningful architecture decision is introduced, add an ADR under `CampusMarket/docs/decisions/` instead of burying rationale in code comments.
- Keep `CampusMarket/CHANGELOG.md` updated when completing a substantive task in that project.

## Out Of Scope By Default

- Do not modify `Final/` or `课件/` unless the human explicitly names them.
- Do not rewrite large parts of the project just to improve style.
- Do not change build tools, framework versions, or repository structure unless requested.

## When In Doubt

- Ask which top-level project to work on if the request does not clearly point to `CampusMarket/`.
- If instructions in a deeper directory conflict with this file, the deeper and more specific instructions win.
