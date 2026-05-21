# migration-notes

This directory contains artifacts from the original repository layout before the project was migrated to its current structure.

**These files do not represent the current repository.** All paths, CI configs, and AGENTS instructions here are outdated and should not be used as reference.

- `original-root/AGENTS.outer-repo.md` — AGENTS file from the old outer repo where `CampusMarket/` was a subdirectory. Paths like `CampusMarket/backend` no longer apply.
- `original-root/.github/workflows/ci.outer-repo.yml` — CI workflow from the old repo. Uses `working-directory: CampusMarket/backend` which is invalid in the current layout.
- `original-root/最终需求文档.md` — Redirect document pointing to absolute paths on the original machine. Links are broken.

For current project instructions, see `CLAUDE.md` and `AGENTS.md` at the repository root.
