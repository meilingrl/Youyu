# Task: Frontend Bundle Second Pass — Admin Component Lazy Registration

## Metadata

- ID: frontend-bundle-second-pass
- Status: active
- Owner: unassigned
- Track: cross-cutting
- Depends on: archived `frontend-bundle-optimization` (first pass), archived `frontend-bundle-second-pass-planning`
- Priority: medium
- Planned date: 2026-05-22
- Completed date:

## Objective

Reduce the initial JS preload for non-admin users by moving admin-only Element Plus components (`ElTable`, `ElTableColumn`) out of the eager registration path and into a lazy admin-only plugin loaded only when the admin layout mounts.

## Background

The first pass (archived `frontend-bundle-optimization.md`, 2026-05-16) narrowed Element Plus registration to 27 specific components and eliminated the full stylesheet import. The second planning task (archived `frontend-bundle-second-pass-planning.md`, 2026-05-22) took a measured baseline and identified the highest-leverage remaining slice.

**Baseline measurement (2026-05-22 build):**

| Metric | Value |
|--------|-------|
| Total .js bytes (all chunks) | 816,405 bytes |
| Total .css bytes (all chunks) | 243,118 bytes |
| Eagerly preloaded JS (index.html preloads) | 566,115 bytes |

**Top 5 JS chunks by size:**

| Rank | Filename | Bytes |
|------|----------|-------|
| 1 | `element-plus-qknwiqM9.js` | 296,856 bytes |
| 2 | `vendor-DQb-f25d.js` | 224,062 bytes |
| 3 | `vue-core-B_-2JvuZ.js` | 29,469 bytes |
| 4 | `OrdersView-xSy5g5mr.js` | 18,696 bytes |
| 5 | `app-m-ZJDvh6.js` | 15,728 bytes |

**Chosen slice:** (c) — Move admin-only Element Plus components (`ElTable`, `ElTableColumn`) from the eager plugin registration to a lazy admin-only plugin that loads only when `AdminLayout.vue` mounts.

**Justification:** All route-level views are already individually code-split. The vendor chunk (224 KB) consists entirely of mandatory transitive deps of element-plus (`@vueuse/core`, `@floating-ui/dom`, `lodash`, `axios`) that cannot be further split without API contract changes. The main entry `app-m-ZJDvh6.js` is only 15.7 KB. The only remaining controllable slice in the initial preload is the `element-plus` chunk (296.9 KB). Within that chunk, `ElTable` and `ElTableColumn` are exclusively used via `ListPageShell.vue`, which is itself only used in admin views (confirmed: zero occurrences in `src/views/app/` or `src/components/`). Moving these two components out of eager registration will shrink the `element-plus` chunk for the 100% of users who never visit an admin route on first load.

## Target Metric

Reduce `element-plus-*.js` from **296,856 bytes** to **≤ 240,000 bytes** (a ≥ 56,856 byte reduction, ~19%), measured on a production build after this change. The exact filename hash will change; the target applies to whichever single chunk contains the bulk of the element-plus component code.

Secondary: reduce eagerly preloaded JS from 566,115 bytes to ≤ 510,000 bytes.

## Files to Read

- `AGENTS.md`, `CLAUDE.md`
- `frontend/vite.config.js`
- `frontend/src/main.js`
- `frontend/src/plugins/element-plus.js`
- `frontend/src/plugins/element-plus-services.js`
- `frontend/src/layouts/AdminLayout.vue`
- `frontend/src/layouts/AppLayout.vue`
- `frontend/src/components/shell/ListPageShell.vue`
- `frontend/src/router/modules/admin.js`

## Pre-flight Verification

1. Run `npm test` from `frontend/` — confirm zero failures before touching any file.
2. Run `npm run build` from `frontend/` — capture baseline `element-plus-*.js` byte count from build output. It should match ~296,856 bytes. If it differs by more than 5 KB, record the actual number and update the target metric proportionally before proceeding.
3. Confirm `ElTable` and `ElTableColumn` are not imported or referenced anywhere outside `frontend/src/plugins/element-plus.js` and `frontend/src/components/shell/ListPageShell.vue`. Search command: `grep -rn "ElTable\|el-table" frontend/src/views/app/ frontend/src/components/ --include="*.vue"` — must return zero hits in `views/app/`.

## In Scope

### Changes

1. **`frontend/src/plugins/element-plus.js`** — remove `ElTable`, `ElTableColumn`, and their style imports from the eager registration list.
2. **`frontend/src/plugins/element-plus-admin.js`** (new file) — create a new plugin that registers `ElTable` and `ElTableColumn` with their CSS, following the exact same pattern as `element-plus.js`. This file must also export `installElementPlusAdmin`.
3. **`frontend/src/layouts/AdminLayout.vue`** — call `installElementPlusAdmin(app)` (or the current Vue app instance) during `onMounted` or via a plugin pattern so admin components register before any admin child route renders. See Implementation Steps for the precise pattern.

### Documentation updates

4. **`CHANGELOG.md`** — prepend one block under `### frontend`.
5. **`docs/05-roadmap/current/feature-roadmap.md`** — update the "前端包体积治理" row to reflect second pass completion.
6. **`docs/08-tasks/active/frontend-bundle-second-pass.md`** (this file) — move to `archived/` with `Status: archived`, `Completed date`, and `Delivered` section.

## Out of Scope

- Any change to `vite.config.js` or `rollupOptions`
- Changes to any view file other than `AdminLayout.vue`
- Moving any components other than `ElTable` and `ElTableColumn`
- Changes to backend code or API contracts
- CSS custom property or theming changes
- Installing or removing any npm dependency

## Hard Limits

- **Do not** modify any file under `frontend/src/views/`
- **Do not** modify `frontend/vite.config.js`
- **Do not** install or remove dependencies
- **Do not** move more than the two table components in a single commit
- **Do not** bypass test or build steps
- **Do not** modify `schema.sql` or any backend file

## Allowed Changes

- `frontend/src/plugins/element-plus.js`
- `frontend/src/plugins/element-plus-admin.js` (new)
- `frontend/src/layouts/AdminLayout.vue`
- `CHANGELOG.md`
- `docs/05-roadmap/current/feature-roadmap.md`
- `docs/08-tasks/active/frontend-bundle-second-pass.md` (this file → move to `archived/`)

## Implementation Steps

### Step 1 — Run pre-flight checks

```bash
cd frontend
npm test           # must pass with zero failures
npm run build      # capture element-plus-*.js byte count from output
```

Record the `element-plus-*.js` chunk size from the build output line. Confirm it is ~296,856 bytes.

### Step 2 — Remove table components from the eager plugin

Edit `frontend/src/plugins/element-plus.js`:

Remove these two import lines:
```js
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
```

Remove these two style import lines:
```js
import 'element-plus/es/components/table/style/css'
import 'element-plus/es/components/table-column/style/css'
```

Remove `ElTable` and `ElTableColumn` from the `components` array.

Do not change anything else in this file.

### Step 3 — Create the admin-only plugin

Create a new file `frontend/src/plugins/element-plus-admin.js` with the following content (following the same pattern as the existing plugin):

```js
import { ElTable, ElTableColumn } from 'element-plus/es/components/table/index.mjs'
import 'element-plus/es/components/table/style/css'
import 'element-plus/es/components/table-column/style/css'

const adminComponents = [
  ElTable,
  ElTableColumn
]

export function installElementPlusAdmin(app) {
  adminComponents.forEach((component) => {
    app.use(component)
  })
}
```

### Step 4 — Lazy-load the admin plugin from AdminLayout

Read `frontend/src/layouts/AdminLayout.vue` in full before editing.

In `AdminLayout.vue`, add a dynamic import and registration in the `<script setup>` block using `onBeforeMount`:

```js
import { onBeforeMount, getCurrentInstance } from 'vue'

onBeforeMount(async () => {
  const { installElementPlusAdmin } = await import('@/plugins/element-plus-admin')
  const app = getCurrentInstance().appContext.app
  installElementPlusAdmin(app)
})
```

If `AdminLayout.vue` already uses `onBeforeMount` or has `<script setup>`, integrate into the existing setup block rather than creating a second one.

**Important:** `onBeforeMount` fires before child components render, so admin route children will have the table components registered by the time they mount. If any admin sub-route renders synchronously before the dynamic import resolves (unlikely, but possible during HMR), a short `v-if` gate on the layout content can be added — see "Rollback / contingency" below.

### Step 5 — Rebuild and measure

```bash
cd frontend
npm run build
```

From the build output:
- Locate the new `element-plus-*.js` chunk size — must be ≤ 240,000 bytes
- Locate any new `element-plus-admin-*.js` chunk — its size is expected to be ~25,000–55,000 bytes
- Confirm the new chunk is NOT in `index.html` as a `<link rel="modulepreload">` (it should not be, because it is loaded dynamically)
- Sum all eagerly preloaded JS — must be ≤ 510,000 bytes

### Step 6 — Run tests

```bash
cd frontend
npm test
```

All tests must pass with zero failures.

### Step 7 — Manual smoke check (record in Final Report)

Start the dev server (`npm run dev`) with a running backend, or use `npm run preview` after build:

1. Open `/app/home` — confirm no console errors about missing ElTable component
2. Navigate to `/app/explore` — confirm product list renders correctly
3. Log in as admin (`admin` / `admin123`) and navigate to `/admin/users` — confirm the user management table renders correctly
4. Navigate to `/admin/products` — confirm table renders correctly

### Step 8 — Documentation updates

- Prepend `CHANGELOG.md` block
- Update `feature-roadmap.md` row
- Move this task file to `archived/` with `Status: archived`, `Completed date: <date>`, and a `Delivered` section listing the file changes

## Rollback Strategy

Revert by re-adding `ElTable` and `ElTableColumn` (and their style imports) to `frontend/src/plugins/element-plus.js`, removing the `onBeforeMount` block from `AdminLayout.vue`, and deleting `frontend/src/plugins/element-plus-admin.js`.

## Test Plan

- Backend: not required
- Frontend unit tests: `npm test` — zero failures before and after change
- Build: `npm run build` — chunk size target met (element-plus chunk ≤ 240,000 bytes)
- API validation: not required
- Manual smoke check:
  - `/app/home` — no missing component errors in console
  - `/admin/users` — ElTable renders with data rows
  - `/admin/products` — ElTable renders with data rows
  - Check `dist/index.html` after build — no `modulepreload` for the new admin plugin chunk

## Acceptance Criteria

- [ ] `frontend/src/plugins/element-plus.js` no longer imports `ElTable` or `ElTableColumn`
- [ ] `frontend/src/plugins/element-plus-admin.js` exists and exports `installElementPlusAdmin`
- [ ] `frontend/src/layouts/AdminLayout.vue` dynamically imports and calls `installElementPlusAdmin` before children mount
- [ ] `npm run build` produces `element-plus-*.js` ≤ 240,000 bytes
- [ ] New admin chunk is NOT listed as `modulepreload` in `dist/index.html`
- [ ] `npm test` passes with zero failures
- [ ] `CHANGELOG.md` block added
- [ ] `feature-roadmap.md` updated
- [ ] This task archived

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] `docs/06-http/` — not applicable
- [x] `docs/05-roadmap/current/feature-roadmap.md`
- [x] task status and archive move

## Final Report Format

```markdown
## Return Report — frontend-bundle-second-pass

### A. Branch & Commit
- Branch: <name>
- Commit SHA: <sha>
- Files changed (paste `git diff --stat`)

### B. Baseline vs. Result
- Pre-change element-plus chunk: ~296,856 bytes
- Post-change element-plus chunk: <bytes> — target ≤ 240,000 bytes
- New admin plugin chunk: <filename> — <bytes>
- New admin chunk in index.html modulepreload: yes/no (must be no)
- Eagerly preloaded JS before: ~566,115 bytes
- Eagerly preloaded JS after: <bytes> — target ≤ 510,000 bytes
- `npm test` result: pass/fail

### C. Manual Smoke Check Results
- /app/home — no console errors: yes/no
- /admin/users — ElTable renders: yes/no
- /admin/products — ElTable renders: yes/no

### D. Acceptance Criteria Check
- [x/✗] one per criterion with evidence

### E. Deviations from Spec
- "none" or specific deviation with reason

### F. Out-of-scope Findings
- "none" or specific items

### G. Open Questions / Blockers
- "none" or single question
```

## Completion Notes

(Filled in by sub-agent.)
