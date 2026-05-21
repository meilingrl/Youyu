# Task: UI Redesign Profile Settings And Shop Identity

## Metadata

- ID: ui-redesign-profile-settings-shop-identity
- Status: completed
- Owner:
- Track: feature
- Depends on: `ui-redesign-shell-navigation-foundation`
- Priority: high
- Planned date:
- Completed date: 2026-05-17

## Objective

重构个人主页、设置中心和店主身份体验，使个人主页与偏好/设置分离，并让店铺成为店主对外主体。

## Background

新的信息架构确认：个人主页回答“我是谁”，设置中心回答“我要改什么配置”。普通用户以个人主页为主，店主用户以店铺为对外主体，店主能力属于前台个人域和店铺域，而不是管理员后台。

## Scope

- 个人主页 `/app/me`
- 设置中心 `/app/settings`
- 偏好设置、地址、安全、通知、隐私等设置分区
- 学生认证入口与状态
- 店主状态与店铺管理入口
- 店铺数据摘要
- 非店主用户的开店/发布引导

## Out of Scope

- 店铺详情页完整重构
- 商品发布表单完整业务重构
- 管理员后台
- 数据库 schema 或后端契约修改
- 完整开店申请前台流程

## Files Read

- `docs/03-architecture/ui-ux-constitution.md`
- `docs/03-architecture/frontend-information-architecture.md`
- `docs/04-standards/frontend-redesign-safety.md`
- `docs/02-requirements/user-preferences-and-profile-insights.md`
- `frontend/src/views/app/ProfileView.vue`
- `frontend/src/views/app/PreferenceSettingsView.vue`
- `frontend/src/views/app/VerificationView.vue`
- `frontend/src/views/app/SellerProductsView.vue`
- `frontend/src/views/app/SellerPublishView.vue`
- `frontend/src/stores/auth.js`
- `frontend/src/stores/market.js`
- `frontend/src/api/modules/user.js`
- `frontend/src/api/modules/shop.js`

## Changes Delivered

1. 将个人主页重构为身份总览页，只保留头像、昵称、认证状态、校园身份、交易/收藏/评价摘要，以及普通用户或店主用户各自的关键入口。
2. 将设置中心重构为独立配置域，提供偏好、地址、安全、通知、隐私和退出登录的结构化入口。
3. 接入现有 `GET /api/shops/mine`，用真实店铺数据识别店主状态，并把店铺管理能力统一收敛到前台 `/app/shop/manage/*` 路径。
4. 保留旧路由兼容，不修改 `/admin` 权限模型，不改动后端 API 契约和数据库 schema。

## Risks And Follow-Up

- 当前店主识别已经优先使用真实 `/api/shops/mine` 数据，但更细的店主状态字段、经营权限细分和完整开店前台流程仍需要后续补充。
- 地址管理、账号安全、通知、隐私目前已迁入设置中心结构，但除偏好设置外仍以现有后端支持范围为边界，没有强行新增未落地配置项。
- 卖家商品/发布页已经切换到新的店铺管理语义，但更完整的店铺资料、订单管理和审核阶段说明仍可继续增强。

## Verification

- Frontend: `npm run test`
- Frontend build: `npm run build`
- Manual:
  - verified `/app/me` for a non-owner account (`mock-1003-USER`) shows growth guidance and no owner-management block
  - verified `/app/me` for an owner account (`mock-1001-USER`) shows shop-management content and public shop entry
  - verified `/app/settings`
  - verified `/app/settings/preferences`
  - verified `/app/verification`
  - verified `/app/shop/manage/products`
  - verified `/app/shop/manage/publish`

## Completion Notes

- 使用现有 `/api/shops/mine` 完成了店主身份安全兼容，避免了长期前端硬编码。
- 设置中心和个人主页已经完成清晰分工。
- 仍依赖未来后端补字段的店主能力包括：更完整的开店申请流程状态、更多经营权限/审核阶段字段、店主专属设置项。
