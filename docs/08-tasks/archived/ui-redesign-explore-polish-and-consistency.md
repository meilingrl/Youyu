# Task: Explore Polish And Buyer Surface Consistency

## Metadata

- ID: ui-redesign-explore-polish-and-consistency
- Status: archived
- Owner: Codex
- Track: cross-cutting
- Depends on: `ui-redesign-shell-navigation-foundation`, `ui-redesign-home-explore`, `ui-redesign-product-shop-detail`, `ui-redesign-trade-reviews`, `ui-redesign-profile-settings-shop-identity`
- Priority: high
- Planned date: 2026-05-20
- Completed date: 2026-05-20

## Objective

在首页样板页已经校准完成的前提下，把探索页提升到同等级别的 UI/UX 质量，并对买家侧关键页面做一轮有限、克制、可验证的一致性收口。

## Completion Notes

### 探索页原先失败原因（诊断）

- 页面叠了两层 hero（`ExploreSearchShell` + `explore-overview`），叙事重复且带「返回首页 / 购物车」打断浏览主线。
- 主内容区采用「商品网格 + 右侧说明栏」的后台列表布局，侧栏重复热搜并塞入说明性 bullet，质感接近旧商品列表页。
- 全页 `SkeletonCard` 阻塞搜索壳层，加载时失去 Airbnb 式「入口始终可用」节奏。
- 精选横向区与主网格、精选店铺层级不清，Featured 店铺被压在页面最底部。

### 交付内容

- `ProductListView.vue`：探索路由专用浏览主线（搜索壳常驻 → 结果摘要条 → 横向精选 → 精选店铺 → 全宽商品舞台 + 分页）；旧 `/app/products` 保留简化列表 + 跳转探索。
- `ExploreSearchShell.vue`：中文 eyebrow 与首页语气对齐。
- `index.css`：`.explore-results-bar`、`.explore-curated-rail` 等共享节奏 token；`settings-hero` / `profile-hero` 暖色 hero 背景。
- 一致性：`TradeView` / `SettingsView` eyebrow 中文化；`ProductDetailView` 空态 CTA 改指向探索；`HomeView` 热搜区文案补充与探索衔接。

### 验证

- `npm run test`：30/30 通过（首次偶发 router guard 超时，重跑通过）。
- `npm run build`：通过。
- 浏览器（Vite `127.0.0.1:5175`）：桌面 1280px 与移动 390px 检查 `/app/explore`、`/app/home`、`/app/products/1`；后端未运行时搜索壳与分区结构仍可见，`ErrorBlock` 体面展示（非白屏）。`/app/trade` 需登录，后端 500 时无法完成登录后抽查。

## Acceptance Criteria

- [x] 探索页不再像旧系统商品列表页，而是真正的高质量浏览主场
- [x] 首页与探索页在视觉语言、搜索入口、精选模块和浏览节奏上形成稳定样板
- [x] 关键买家侧页面完成最小必要的一致性收口，而没有重新发散成全站重构
- [x] 没有擅自改后端契约、聊天能力、权限模型或旧路由兼容
- [x] `npm run test` 与 `npm run build` 通过
- [x] 完成桌面端和移动端的真实浏览器验收（后端离线时验证了结构与 error 态）

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] relevant files in `docs/06-http/` if contract usage truly changes
- [ ] roadmap or standards docs if applicable
- [x] task status and archive move
