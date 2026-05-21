# Task: UI Redesign Shell And Navigation Foundation

## Metadata

- ID: ui-redesign-shell-navigation-foundation
- Status: draft
- Owner:
- Track: cross-cutting
- Depends on: `docs/03-architecture/ui-ux-constitution.md`, `docs/03-architecture/frontend-information-architecture.md`
- Priority: high
- Planned date:
- Completed date:

## Objective

建立下一轮 UI/UX 重构的共享基础：设计 token、前台轻量导航、移动端底部导航、路由别名策略、全局交互动效变量和页面壳层规则。

## Background

当前前台导航更像功能列表，且全局样式仍以旧版视觉系统为主。新的信息架构要求首页轻量化，一级入口收敛为首页、探索、消息、交易、我的，并支持响应式多端使用。

本任务作为 UI 重构的基础任务，应尽量先完成，或至少由一个 Agent 独占共享文件修改权，避免后续页面任务冲突。

## Scope

- 前台一级导航结构。
- 移动端底部导航结构。
- 全局设计 token 初版。
- 全局动效变量初版。
- 路由兼容和新路由入口。
- App shell 与 Admin shell 的基础分层。

## Out of Scope

- 具体页面重写。
- 消息真实业务实现。
- 后台完整客服功能实现。
- 新 UI 框架引入。
- 大规模后端接口变更。

## Files to Read

- `../../03-architecture/ui-ux-constitution.md`
- `../../03-architecture/frontend-information-architecture.md`
- `../../../frontend/README.md`
- `../../../frontend/src/router/index.js`
- `../../../frontend/src/router/modules/app.js`
- `../../../frontend/src/router/modules/admin.js`
- `../../../frontend/src/constants/navigation.js`
- `../../../frontend/src/layouts/AppLayout.vue`
- `../../../frontend/src/layouts/AdminLayout.vue`
- `../../../frontend/src/components/layout/AppHeader.vue`
- `../../../frontend/src/components/layout/MobileBottomNav.vue`
- `../../../frontend/src/styles/variables.css`
- `../../../frontend/src/styles/index.css`

## Allowed Changes

- `frontend/src/router/**`
- `frontend/src/constants/navigation.js`
- `frontend/src/layouts/AppLayout.vue`
- `frontend/src/layouts/AdminLayout.vue`
- `frontend/src/components/layout/**`
- `frontend/src/styles/variables.css`
- `frontend/src/styles/index.css`
- minimal route guard updates if needed
- related docs and changelog

## Implementation Plan

1. Define the new front-office navigation labels: 首页、探索、消息、交易、我的.
2. Add or alias `/app/explore`, `/app/trade`, `/app/messages`, `/app/me`, `/app/settings` without breaking existing routes.
3. Refactor `AppHeader` into a lightweight, less dominant shell suitable for the homepage and inner pages.
4. Update mobile bottom navigation to match 首页 / 探索 / 消息 / 交易 / 我的.
5. Add first-pass warm Etsy-influenced tokens and motion variables without rewriting every page.
6. Keep admin shell restrained and prepare space for support/customer-service navigation.

## Risks

- Shared file conflicts with page-level UI tasks.
- Route aliasing may break active route highlighting.
- Navigation copy may regress because some existing files have encoding issues.
- Changing global tokens may unintentionally affect existing pages before page-specific redesigns land.

## Test Plan

- Backend: not required.
- Frontend: run `npm run test`.
- API validation: not required.
- Manual: run frontend dev server and verify desktop and mobile navigation across home, explore/products, cart/orders, profile/me, and admin.

## Acceptance Criteria

- [ ] Frontend has a clear route/navigation foundation for 首页、探索、消息、交易、我的.
- [ ] Existing important routes still resolve.
- [ ] Mobile bottom navigation follows the new information architecture.
- [ ] Global tokens move toward the warm first theme without breaking readability.
- [ ] Shared files are documented as owned by this foundation task.

## Documentation Updates Required

- [ ] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/`
- [ ] roadmap or standards docs if applicable
- [ ] task status and archive move

## Completion Notes

