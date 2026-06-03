# Development Process

This document defines the repository's delivery process, task flow, and documentation-update expectations.

It is a process standard, not a stage roadmap.

For stage progression, use `../05-roadmap/current/stage-roadmap.md`.
For launch-preparation sequencing, use `../05-roadmap/current/launch-preparation-roadmap.md`.

## 1. Purpose

The project uses a structured development flow so that requirements, code, tests, API contracts, and documentation evolve together.

The process is designed to prevent:

- undocumented scope expansion
- duplicate or conflicting task specs
- code changes without API/document follow-up
- completed work being lost because the task record was deleted

## 2. Delivery Flow

Each implementation item should move through the following lifecycle:

1. Clarify the need
   - If the item is still exploratory, capture it in a draft task or a current roadmap document that still owns unresolved planning for that area.
2. Create or update a task document
   - Use `docs/08-tasks/TASK_TEMPLATE.md`.
   - Place the task in `docs/08-tasks/drafts/` until scope is stable.
3. Activate the task
   - Move the task to `docs/08-tasks/active/` once it is approved for execution.
4. Implement and verify
   - Keep code, tests, API docs, and the task record aligned.
5. Complete and archive
   - Update completion notes.
   - Move the task to `docs/08-tasks/archived/`.
   - Prepend the relevant `CHANGELOG.md` entry.

## 3. Document Roles

- Standards define enduring process and repository rules.
- Roadmaps define planning direction and sequencing.
- Tasks define concrete implementation work.
- ADRs define significant decisions.
- API files define executable request/response expectations.

These roles must not be merged casually into a single document.

## 4. Pre-Implementation Checklist

Before starting a code task:

1. Read `AGENTS.md` and `CLAUDE.md`.
2. Read `docs/README.md`.
3. Read the active task spec, if one exists.
4. Read the related roadmap and standards documents.
5. Identify file scope before editing.

## 5. During Implementation

- Keep one task responsible for one coherent change set.
- If an issue is outside the task scope:
  - create or reference a separate task
  - or mark it as `Track: cross-cutting`
- Do not silently broaden a task into a second feature.
- When an endpoint or response changes, update the related file in `../06-http/`.

## 6. Completion Rules

A task is not complete until all relevant artifacts are aligned:

- implementation finished
- tests or checks run
- API docs updated if needed
- `CHANGELOG.md` updated if the change is substantive
- task status updated
- task moved to `archived/` when done

## 7. Hard Rules

- `CLAUDE.md` stays in place.
- Completed task docs are archived, not deleted.
- Process rules belong here, not in roadmap docs.
- Stage status and launch-preparation sequencing belong in roadmap docs, not here.
