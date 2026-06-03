# Roadmap Directory

This directory now separates current planning artifacts from historical roadmap artifacts.

## Structure

### `current/`

Use this directory for roadmap documents that still guide current work:

- `stage-roadmap.md`
- `launch-preparation-roadmap.md`
- `message-center-roadmap.md` when that module still needs current planning follow-up

These files should be short-lived, practical, and tightly aligned with the current codebase state.

### `archived/`

Use this directory for planning documents that still have traceability value but no longer define current decisions:

- historical MVP boundary documents
- completed feature roadmaps and resolved question trackers
- superseded phase plans
- old feature-sequencing notes
- planning documents whose value is now mainly traceability

## Rules

- Keep only currently effective roadmap documents in `current/`.
- If a file mostly describes what has already been completed, it probably belongs in `archived/`, not `current/`.
- Move outdated planning artifacts to `archived/` instead of leaving them mixed with active roadmap files.
- Do not use `archived/` documents as the current source of truth.
- If a document mainly defines implementation scope rather than roadmap direction, consider whether it belongs in tasks, requirements, or standards instead.
