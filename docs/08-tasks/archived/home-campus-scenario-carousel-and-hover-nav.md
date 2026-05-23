# Task: Home Campus Scenario Carousel and Hover Navigation

## Metadata

- ID: home-campus-scenario-carousel-and-hover-nav
- Status: completed
- Owner: worker-agent
- Track: feature
- Depends on: current homepage redesign feedback pass
- Priority: high
- Planned date: 2026-05-24
- Completed date: 2026-05-24

## Objective

Add two user-facing homepage/interface improvements:

1. A desktop top navigation that stays collapsed until hover or keyboard focus, then reveals with a concise, polished glass navigation surface.
2. A homepage campus-scenario carousel inspired by the Stripe "Latest news" carousel reference video, using 5-6 looping scenario image cards for dorm items, books/materials, digital devices, and adjacent campus marketplace scenarios.

Keep the existing platform-data section exactly in place conceptually and visually. Do not add the business-process section yet.

## Background

Human feedback:

- The platform-data section must remain.
- The business-process section is deferred because the product direction is not settled.
- The design should continue borrowing Stripe-like UI elements.
- Reference video: `C:\Users\Meilingluo\Videos\屏幕录制\Recording 2026-05-24 004342.mp4`.

Observed reference-video traits:

- White editorial surface with a compact section header.
- Top-right square arrow controls with a pale purple accent.
- Horizontal carousel composition: one dominant active card, adjacent narrow preview cards, clean gutters, restrained text.
- Image-first cards, short text below, no heavy decorative framing.
- Smooth cyclic navigation.

## Scope

### 1. Hover-Reveal Top Navigation

Implement a desktop hover/focus-reveal navigation using the existing user app header/navigation system.

Requirements:

- Use existing routes and labels from `appNavigation`.
- Desktop behavior:
  - A slim top trigger area remains visible.
  - Hovering the top area expands the full navigation.
  - Moving the mouse away collapses it.
  - `:focus-within` must also reveal it for keyboard users.
- Mobile behavior:
  - Keep the current mobile menu/bottom navigation behavior.
  - Do not require hover interaction on mobile.
- Visual direction:
  - Clean glass surface, restrained shadow, fine border, Stripe-like precision.
  - Use the existing design tokens where possible.
  - Avoid adding a large fixed bar that competes with the hero.

### 2. Campus Scenario Carousel

Add a new homepage section after the existing platform-data section and before the recommended-products rail.

Content should describe concrete campus marketplace scenes, not platform guarantees.

Use 5-6 looping cards, for example:

- 宿舍用品
- 教材书籍
- 数码设备
- 毕业季闲置
- 社团摊位
- 校园服务

Each card should include:

- A real image or high-quality remote image URL suited to the scene.
- A short title.
- A one-sentence description.
- 2-4 compact tags such as `楼下自提`, `同校面交`, `快递柜`, `考研资料`, `桌面设备`.

Interaction requirements:

- Infinite/cyclic next/previous navigation.
- Top-right arrow controls in the section header.
- Keyboard-accessible card controls or focus states.
- Respect `prefers-reduced-motion`; do not force auto animation for reduced-motion users.
- Mobile layout must remain coherent without horizontal text overflow.

Visual requirements:

- Borrow the Stripe reference composition: dominant active card plus narrow adjacent preview cards.
- Keep cards image-led; side preview cards may show mostly image with minimal or hidden text.
- Use restrained purple/purple-blue accent only as a small control/accent color, not a dominant full-page palette.
- No large marketing explanation block.
- No nested cards.

## Out of Scope

- Do not remove, rewrite, or visually replace the existing platform-data section.
- Do not modify `frontend/src/components/home/HomeStatsNetwork.vue`.
- Do not add the business-process/交易过程 section.
- Do not add the trust/保障 module.
- Do not change backend code, API contracts, database schema, or seed data.
- Do not install or remove dependencies.
- Do not introduce a new UI library.
- Do not alter admin pages.

## Files to Read

- `AGENTS.md`
- `CLAUDE.md`
- `docs/README.md`
- `frontend/README.md`
- `frontend/src/layouts/AppLayout.vue`
- `frontend/src/components/layout/AppHeader.vue`
- `frontend/src/components/layout/MobileNav.vue`
- `frontend/src/components/layout/MobileBottomNav.vue`
- `frontend/src/constants/navigation.js`
- `frontend/src/views/app/HomeView.vue`
- `frontend/src/components/home/HomeFeaturedRail.vue`
- `frontend/src/styles/index.css`
- `frontend/src/styles/variables.css`

## Allowed Changes

- `frontend/src/layouts/AppLayout.vue`
- `frontend/src/components/layout/AppHeader.vue`
- `frontend/src/views/app/HomeView.vue`
- `frontend/src/components/home/HomeCampusScenarioCarousel.vue` (new, if useful)
- `frontend/src/styles/index.css`
- `CHANGELOG.md`
- `docs/08-tasks/active/home-campus-scenario-carousel-and-hover-nav.md` then move to `docs/08-tasks/archived/` on completion

## Implementation Plan

1. Inspect current dirty worktree before editing and avoid reverting unrelated existing changes.
2. Implement the hover/focus reveal behavior in the existing app header path.
3. Add a dedicated campus scenario carousel component or a tightly scoped block in `HomeView.vue`.
4. Insert the carousel after the platform-data section and before `HomeFeaturedRail`.
5. Keep platform-data logic, text, and Canvas component untouched.
6. Add responsive CSS for desktop, tablet, and mobile.
7. Update `CHANGELOG.md`.
8. Run frontend tests/build and archive this task with completion notes.

## Risks

- Hover-only navigation can be inaccessible if `focus-within` and mobile fallbacks are missed.
- A carousel can create layout shift if card dimensions are not stable.
- Remote images can appear inconsistent; use consistent aspect ratios and overlays to keep text legible.
- The current worktree already has homepage-related edits; do not overwrite or revert them.

## Test Plan

- Backend: not required.
- Frontend:
  - `npm test` from `frontend/`.
  - `npm run build` from `frontend/`.
- API validation: not required.
- Manual:
  - Open `/app/home` on desktop width and confirm the top nav reveals on hover and focus.
  - Confirm the platform-data section remains present and functional.
  - Confirm the campus-scenario carousel appears after platform data and before recommendations.
  - Click next/previous arrows through all 5-6 cards and confirm looping behavior.
  - Check mobile width for no text overlap and no hover-only dependency.

## Acceptance Criteria

- [x] Active homepage still contains the existing platform-data section.
- [x] `HomeStatsNetwork.vue` is not modified by this task.
- [x] Desktop top navigation collapses by default and expands on hover/focus.
- [x] Mobile navigation remains usable without hover.
- [x] Campus-scenario carousel has 5-6 scene cards with image, title, description, and tags.
- [x] Carousel loops forward and backward.
- [x] Carousel follows the Stripe reference rhythm: dominant active card plus narrow previews, compact header, top-right arrow controls.
- [x] No business-process or trust/保障 section is added.
- [x] `npm test` passes.
- [x] `npm run build` passes.
- [x] `CHANGELOG.md` is updated.
- [x] This task is archived with completion notes.

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [x] relevant files in `docs/06-http/` - not applicable
- [x] roadmap or standards docs if applicable - not expected
- [x] task status and archive move

## Completion Notes

- Implemented homepage desktop hover/focus reveal through the existing `AppHeader` path with mobile menu behavior left visible on small screens.
- Added `HomeCampusScenarioCarousel.vue` with six looping campus scene cards: 宿舍用品, 教材书籍, 数码设备, 毕业季闲置, 社团摊位, 校园服务.
- Inserted the carousel after the existing platform-data section and before the recommendations rail without changing `HomeStatsNetwork.vue`.
- Did not add the deferred business-process section or trust/保障 module.
- Verification passed: `npm test` from `frontend/`, `npm run build` from `frontend/`, and a Playwright local-preview smoke check for `/app/home` at 1440px and 390px widths.
