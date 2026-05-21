# AGENTS.md

This file provides repository-wide working guidance for Codex and other AI coding agents operating in this repository.

`CLAUDE.md` remains in place as a project instruction source. This file does not replace it; it aligns cross-agent behavior around the current repository layout and document workflow.

## 1. Default Work Area

- Treat the repository root as the default project root.
- Preserve the existing frontend / backend / database split.
- Do not move, rename, or rewrite `CLAUDE.md` as part of normal task work.
- Do not modify folders outside the current repository root unless the human explicitly asks.

## 2. Read Order Before Work

When starting a task, read in this order unless the task is documentation-only:

1. `AGENTS.md`
2. `CLAUDE.md`
3. `docs/README.md`
4. Relevant roadmap or task documents:
   - `docs/04-standards/development-process.md`
   - `docs/05-roadmap/current/stage-roadmap.md`
   - `docs/05-roadmap/current/feature-roadmap.md`
   - `docs/08-tasks/active/*.md`
5. Relevant module docs:
   - `frontend/README.md`
   - `backend/README.md`
   - `database/README.md` if applicable

## 3. Document System Overview

The documentation system is intentionally split by responsibility:

- `docs/01-product/`
  - Human-facing product and business context documents.
- `docs/02-requirements/`
  - Formal requirements, domain rules, and acceptance boundaries.
- `docs/03-architecture/`
  - Engineering architecture, repository structure, and implementation strategy.
- `docs/04-standards/`
  - Process, testing, contribution, terminology, and document-governance rules.
- `docs/05-roadmap/`
  - Current roadmap documents plus archived planning artifacts.
- `docs/06-http/`
  - Executable `.http` request collections for validation and smoke flows.
- `docs/07-decisions/`
  - ADRs and immutable architecture decisions.
- `docs/08-tasks/`
  - Task execution documents managed through `drafts/`, `active/`, and `archived/`.
- `docs/09-api-spec/`
  - Formal API specification documents when maintained.

## 4. Difference Between Core Planning Documents

Agents must not treat these as interchangeable:

- `docs/04-standards/development-process.md`
  - Development-process and delivery-governance rules.
  - Not a feature roadmap.
- `docs/05-roadmap/current/stage-roadmap.md`
  - Project phase and stage progression.
- `docs/05-roadmap/current/feature-roadmap.md`
  - Feature expansion order, status, and dependency relationships.
- `docs/08-tasks/`
  - Concrete executable tasks with file scope, risks, tests, and completion notes.

## 5. Task Lifecycle

Task documents are never treated as disposable scratch files.

- `docs/08-tasks/drafts/`
  - Not yet approved or not yet ready for implementation.
- `docs/08-tasks/active/`
  - Approved execution tasks that currently define working scope.
- `docs/08-tasks/archived/`
  - Completed or superseded task records retained for traceability.

Rules:

- Do not delete completed task history.
- Move completed tasks to `archived/` and update their status.
- If a task becomes obsolete before implementation, keep a short record instead of silently deleting it.
- If a problem crosses module boundaries, mark the task `Track: cross-cutting` instead of hiding the work inside an unrelated feature task.

## 6. Documentation Update Responsibilities

Use the following responsibility model:

- `CLAUDE.md`
  - Human-maintained primary project instruction file.
  - Agents may update links only when repository structure changes require it.
- `docs/README.md`
  - Update when the document structure or reading order changes.
- `docs/04-standards/*.md`
  - Update when process, testing, or governance rules change.
- `docs/05-roadmap/*.md`
  - Update when stage status, feature order, or planning assumptions change.
- `docs/06-http/*.http`
  - Update smoke tests to cover new or changed endpoints.
- `docs/07-decisions/*.md`
  - Add a new ADR for significant decisions; do not rewrite old ADR outcomes.
- `docs/08-tasks/*.md`
  - Update when a task is created, activated, blocked, completed, or archived.
- `docs/09-api-spec/`
  - Update whenever endpoint contract or response shape changes.
- `CHANGELOG.md`
  - Prepend one new entry per substantive completed task or documentation restructuring.

## 7. Conflict Resolution Order

If documentation and code appear to conflict, resolve in this order:

1. The actual code and runtime behavior
2. Explicit human instructions in the current conversation
3. `CLAUDE.md`
4. `AGENTS.md`
5. Active task documents in `docs/08-tasks/active/`
6. Standards and roadmap docs
7. Archived task documents

If the conflict is meaningful, do not silently pick whichever source is convenient. Update the affected documentation as part of the task or report the mismatch clearly.

## 8. Hard Constraints

- Do not move or rename `CLAUDE.md`.
- Do not reintroduce `AdminDataStore` as new persistent business logic.
- Do not treat archived task docs as current execution specs.
- Do not create duplicate roadmaps for the same purpose.
- Do not keep two active documents that claim to be the single source of truth for the same rule set.
