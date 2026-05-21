# Task System

This directory contains executable task records.

Task documents are not disposable notes. They are the execution history of the repository.

## Lifecycle

- `drafts/`
  - task idea exists, but scope is not approved or not yet execution-ready
- `active/`
  - task is approved and defines current working scope
- `archived/`
  - task is completed or superseded and retained for traceability

## Rules

- Do not delete completed tasks.
- Move completed tasks to `archived/`.
- If a task is blocked or postponed, keep it in `drafts/` or `active/` with a clear status instead of dropping it silently.
- If a task spans multiple modules, use `Track: cross-cutting`.

## Naming

Use English kebab-case filenames.

Recommended pattern:

```text
<topic>-<qualifier>.md
```

Examples:

- `chat-mvp.md`
- `frontend-bundle-optimization.md`
- `hot-search-enhancement-p3.md`

## Required Template

Use `TASK_TEMPLATE.md` for all new tasks.
