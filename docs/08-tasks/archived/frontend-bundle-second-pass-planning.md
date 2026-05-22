# Task: Frontend Bundle Second Pass Planning

## Metadata

- ID: frontend-bundle-second-pass-planning
- Status: archived
- Owner: unassigned
- Track: cross-cutting
- Depends on: archived `frontend-bundle-optimization` first pass
- Priority: medium
- Planned date: 2026-05-22
- Completed date: 2026-05-22

## Objective

Take a measured baseline of the current frontend bundle, identify the single highest-leverage bounded slice for a second optimization pass, and produce an execution-ready task spec for that slice.

This is a planning task. **No application code is modified.** The output is one new task file plus a roadmap line update.

## Background

The first pass (archived `frontend-bundle-optimization.md`, 2026-05-16) already shipped:
- Replaced global `ElementPlus` registration with local on-demand registration
- Removed the global `element-plus/dist/index.css` import
- Lazy-loaded `AppLayout` and `AdminLayout`

What remains untouched and likely high-leverage:
- Possible large dependency chunks (chart libs, date libs, vendor groupings)
- Per-route code splitting beyond the layout level
- CSS chunk strategy

Without a measured baseline, the next slice is a guess. This task replaces the guess with evidence.

## Pre-flight Verification (must complete before drafting the new slice)

1. Read `docs/08-tasks/archived/frontend-bundle-optimization.md` — locate the "Deferred" or "Follow-up" section; record what was explicitly left for later
2. Read `frontend/vite.config.js` (or equivalent) — record current `build.rollupOptions`, `manualChunks`, and any plugin configuration
3. Read `frontend/package.json` — list dependencies > 100KB by name (echarts, dayjs/moment, lodash, axios, etc.; use awareness of typical package sizes)
4. Read `frontend/src/main.js` and `frontend/src/plugins/element-plus.js` — record the current Element Plus component set
5. **Build with stats** — run from `frontend/`:
   ```bash
   npm run build -- --mode production
   ```
   Then read every file in `frontend/dist/assets/` and record:
   - Total `.js` size (bytes, sum)
   - Top 5 chunks by size (filename + bytes)
   - Total `.css` size (bytes, sum)
6. **Identify the heaviest still-eager imports** — `grep -rn "^import " frontend/src/main.js frontend/src/router/ frontend/src/layouts/` and list third-party imports that hit the main entry chunk

## Files to Read

- `AGENTS.md`, `CLAUDE.md`, `docs/README.md`
- `frontend/README.md`
- `frontend/package.json`
- `frontend/vite.config.js`
- `frontend/src/main.js`
- `frontend/src/plugins/element-plus.js`
- `frontend/src/router/index.js` and `frontend/src/router/modules/*.js`
- `frontend/src/layouts/*.vue`
- `docs/08-tasks/archived/frontend-bundle-optimization.md`
- `docs/05-roadmap/current/feature-roadmap.md`

## In Scope

Produce one artifact:

### `docs/08-tasks/active/frontend-bundle-second-pass.md`

A new active task spec for the chosen slice. Must contain:

1. **Baseline measurement** — paste from pre-flight step 5: total .js bytes, top 5 chunks, total .css bytes
2. **Chosen slice** — one bounded change picked from the candidate list:
   - (a) Dynamic-import a single heavy third-party dependency that is currently eager
   - (b) Split one over-eager route group via `manualChunks`
   - (c) Move one rarely-used Element Plus component subgroup to lazy registration
   - **Pick exactly ONE.** Justify by citing the baseline numbers.
3. **Target metric** — concrete byte-reduction target for the main entry chunk (e.g., "reduce `index-*.js` from 482KB to ≤ 380KB"). Anchored to a specific filename and number.
4. **Implementation Steps** — concrete numbered steps a sub-agent can follow
5. **Verification** — exact build command + measurement procedure to confirm the target is met
6. **Rollback strategy** — single sentence: how to revert if the change causes a regression
7. **Full task-doc structure** — same sections this file uses (Metadata, Pre-flight, In Scope, Hard Limits, Allowed Changes, Test Plan, Acceptance, Final Report Format)

Also:
- Update `docs/05-roadmap/current/feature-roadmap.md`: replace the vague "前端包体积治理" line with a concrete pointer to `frontend-bundle-second-pass.md`

## Out of Scope (this task)

- Actually modifying any frontend code
- Touching `vite.config.js`
- Re-running the first pass changes
- Performance optimization not related to bundle size (runtime perf, image optimization)
- Backend changes

## Hard Limits

- **Do not** change any file under `frontend/src/` or `frontend/vite.config.js`
- **Do not** install or remove dependencies
- **Do not** pick more than ONE slice for the new task — second pass is one bounded change
- **Do not** propose a slice that requires API contract changes
- **Do not** propose a target without a concrete byte number anchored to a chunk filename

## Allowed Changes

- `docs/08-tasks/active/frontend-bundle-second-pass.md` (new)
- `docs/05-roadmap/current/feature-roadmap.md` (one line)
- `CHANGELOG.md`
- `docs/08-tasks/active/frontend-bundle-second-pass-planning.md` → move to `archived/`

## Implementation Steps

1. Complete all pre-flight steps; record measurements
2. Score each candidate (a/b/c above) using: estimated byte savings × implementation effort. Pick the best ratio.
3. Draft `docs/08-tasks/active/frontend-bundle-second-pass.md` per the structure in "In Scope". Implementation Steps inside the new task must be concrete enough that another sub-agent can execute them without re-planning.
4. Update `feature-roadmap.md` with the pointer line
5. Prepend `CHANGELOG.md` block under `### docs`
6. Move this task to `archived/` with `Status: archived`, `Completed date`, `Delivered` section

## Test Plan

- Backend: not required
- Frontend: build must succeed at least once for measurement (pre-flight step 5)
- API validation: not required
- Manual checks (paste in Final Report):
  - new task spec has all sections listed in "In Scope" point 7
  - target metric in new task spec includes both a chunk filename and a byte number
  - chosen slice is exactly one (count occurrences of "Chosen slice" heading subsections)
  - feature-roadmap.md no longer has the vague line

## Acceptance Criteria

- [ ] `docs/08-tasks/active/frontend-bundle-second-pass.md` exists with all 7 mandatory subsections in its "In Scope"
- [ ] Baseline measurement section includes total .js bytes, top 5 chunks by size, total .css bytes (real numbers from a real build)
- [ ] Chosen slice is exactly one of the three candidates and cites the baseline numbers as justification
- [ ] Target metric is anchored to a specific chunk filename and byte target
- [ ] `feature-roadmap.md` line updated to point at the new task
- [ ] `CHANGELOG.md` block added
- [ ] This task moved to `archived/`

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/` — not applicable
- [x] roadmap docs (`feature-roadmap.md`)
- [x] task status and archive move

## Final Report Format

```markdown
## Return Report — frontend-bundle-second-pass-planning

### A. Branch & Commit
- Branch: <name>
- Commit SHA: <sha>
- Files changed (paste `git diff --stat`)

### B. Pre-flight Findings
- First-pass deferrals: <list from archived task>
- Current vite config relevant lines: <paste>
- Heavy deps (>100KB): <list>
- Build output total .js bytes: <number>
- Build output top 5 chunks:
  1. <filename> — <bytes>
  ...
- Total .css bytes: <number>
- Eager third-party imports in main.js: <list>

### C. Implementation Walkthrough
- Step 2 → candidate scoring table:
  - (a) <expected savings> / <effort>
  - (b) ...
  - (c) ...
  - Chosen: <a/b/c>, reason: <one line>
- Step 3 → new task spec at `docs/08-tasks/active/frontend-bundle-second-pass.md` (line count: <N>)
- Step 4 → feature-roadmap.md line: "<paste line>"
- Step 5 → CHANGELOG block added (paste)
- Step 6 → task moved to archived

### D. Test Plan Results
- New task spec has all 7 In-Scope subsections: <yes/no>
- Target metric line: "<paste exact line>" (must contain a filename and a byte number)
- Exactly one Chosen slice subsection: <count>
- Old vague roadmap line removed: <line that replaced it>

### E. Acceptance Criteria Check
- [x/✗] one per criterion with evidence

### F. Deviations from Spec
- "none" or specific deviation with reason

### G. Out-of-scope Findings
- "none" or specific items

### H. Open Questions / Blockers
- "none" or single question
```

## Completion Notes

Completed 2026-05-22 by sub-agent. See `Delivered` section below.

## Delivered

- `docs/08-tasks/active/frontend-bundle-second-pass.md` created — execution-ready task spec for slice (c): lazy admin component registration
- `docs/05-roadmap/current/feature-roadmap.md` updated — "前端包体积治理" row now points to `frontend-bundle-second-pass.md` with concrete target
- `CHANGELOG.md` updated — `docs` block prepended
- This file moved from `active/` to `archived/` with `Status: archived` and `Completed date: 2026-05-22`

**Measured baseline (2026-05-22 build, commit aa02527 base):**
- Total JS: 816,405 bytes | Total CSS: 243,118 bytes
- element-plus chunk: 296,856 bytes (largest, eagerly preloaded)
- vendor chunk: 224,062 bytes (axios + element-plus transitive deps, eagerly preloaded)
- vue-core chunk: 29,469 bytes
- Initial preloaded JS: 566,115 bytes

**Chosen slice:** (c) — Move `ElTable` + `ElTableColumn` to lazy admin-only plugin; target `element-plus-*.js` ≤ 240,000 bytes
