# Frontend Redesign Safety Standard

## 1. Purpose

本文定义前端 UI/UX 重构期间必须遵守的安全规范。

它解决的问题是：在大规模调整页面结构、视觉、导航和组件时，避免破坏现有后端 API、路由入口、数据字段兼容、认证流程和交易流程。

本规范适用于所有涉及 `frontend/` 的 UI 重构任务，尤其是 `docs/08-tasks/drafts/ui-redesign-*.md` 系列任务。

## 2. Core Rule

UI 重构默认只能改变前端表现层和页面组织方式，不默认改变业务契约。

除非任务明确写出后端范围，否则不得修改：

- 后端接口路径。
- 后端请求参数语义。
- 后端响应 envelope。
- 数据库 schema。
- 认证和权限规则。
- 交易、支付、退款、评价等业务状态机。

现有后端 API 已能支撑大部分前端 UI 重构。风险主要来自前端绕过既有封装、删除旧路由、误用字段、或把尚未实现的聊天/店主能力当成已完成业务。

## 3. API Access Rules

### 3.1 Use Existing API Modules

页面和组件不得直接手写 axios 请求。

必须优先通过：

- `frontend/src/api/client.js`
- `frontend/src/api/modules/*.js`
- Pinia stores

访问后端。

如果需要新增 API 调用，必须先在 `frontend/src/api/modules/` 中增加薄封装，再由 store 或页面调用。

### 3.2 Preserve ApiResponse Envelope

前端必须继续按统一响应结构处理：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "OK",
  "data": {}
}
```

不得在 UI 页面里假设后端直接返回裸数组或裸对象，除非对应 API 模块或 store 已经完成兼容处理。

### 3.3 Keep Normalization Boundaries

商品、个人资料、店铺等跨页面复用数据必须继续经过现有 normalizer 或 store 兼容层。

重点文件：

- `frontend/src/utils/market-normalizers.js`
- `frontend/src/stores/market.js`
- `frontend/src/stores/review.js`
- `frontend/src/stores/recommend.js`
- `frontend/src/stores/auth.js`

页面重构时不得直接把后端字段散落到多个新组件里。如果新页面需要新的展示字段，应优先在 normalizer 或 store 中增加兼容字段。

## 4. Route Migration Rules

### 4.1 Do Not Delete Existing Public Routes Abruptly

信息架构允许新增：

- `/app/explore`
- `/app/trade`
- `/app/messages`
- `/app/me`
- `/app/settings`

但不得直接删除现有重要入口。

必须保留或提供 redirect/alias：

| Existing route | New home |
| --- | --- |
| `/app/products` | `/app/explore` or compatible product browsing route |
| `/app/profile` | `/app/me` |
| `/app/preferences` | `/app/settings/preferences` |
| `/app/seller/products` | `/app/shop/manage/products` |
| `/app/seller/publish` | `/app/shop/manage/publish` |
| `/app/orders` | trade domain, still valid |
| `/app/cart` | trade domain, still valid |

### 4.2 Preserve Deep Links

These routes must keep working during redesign:

- `/app/products/:id`
- `/app/shops/:id`
- `/app/cart`
- `/app/checkout`
- `/app/payments/:orderId`
- `/app/orders`
- `/app/reviews/pending`
- `/app/reviews/mine`
- `/login`
- `/register`

If a route is visually moved into a new domain, its URL should still resolve until a separate migration task explicitly removes it.

### 4.3 Navigation Is Not The Same As Route Availability

Removing a link from top navigation does not mean removing the route.

Many routes should become hidden flow pages or secondary entries while still resolving correctly.

## 5. Store And State Rules

### 5.1 Prefer Store Actions For Shared Data

If data is used by more than one page, fetch it through a store action or shared helper.

Examples:

- Product list and product detail data should go through `marketStore` or product API wrappers.
- Search and suggestions should go through `searchStore`.
- Recommendations should go through `recommendStore`.
- Reviews should go through `reviewStore`.
- Auth/session data should go through `authStore`.

### 5.2 Page-Local State Is For Presentation Only

Page-local state may hold:

- active tab
- selected chip
- drawer open state
- local loading state
- hover/focus/expanded state
- temporary form draft

Page-local state should not duplicate canonical business data if a store already owns it.

## 6. Chat And Message Safety

消息中心是新的一级能力，但当前不能假设完整聊天后端已经存在。

允许：

- 新增消息中心 UI 壳。
- 新增会话列表占位。
- 新增商品、店铺、订单页的“联系”入口。
- 使用明确隔离的 mock/local placeholder 数据。
- 将缺失 API 记录到任务完成说明或后续任务。

禁止：

- 假装实时聊天已经完成。
- 在没有后端支持时提交真实消息。
- 把 mock 会话数据混入生产 store。
- 随手新增后端表结构或 WebSocket 实现，除非任务明确批准。

消息相关后端能力应单独立任务，包括会话模型、消息模型、客服会话、群聊、敏感内容治理和通知机制。

## 7. Shop Owner Identity Safety

店主不是管理员。店主能力属于前台，不属于 `/admin`。

允许：

- 在个人主页展示店主状态。
- 在个人域中提供店铺管理入口。
- 在店铺详情页强化店铺作为对外主体。
- 为缺失的店主状态字段做前端兼容或占位。

禁止：

- 让店主进入管理员后台。
- 把店主管理能力绑定到 `/admin` 路由。
- 假设后端已经提供完整 `isShopOwner`、店铺管理权限、店铺数据统计字段。
- 为了 UI 展示临时改变权限模型。

如果现有 API 无法判断店主状态，应记录为接口补充需求，而不是在前端硬编码长期逻辑。

## 8. Transaction Flow Safety

交易相关页面风险最高，必须保守重构。

涉及以下页面时，不得仅做视觉验证：

- 购物车。
- 结算。
- 支付。
- 订单。
- 售后/退款。
- 举报。
- 评价。

必须保持：

- loading 状态。
- duplicate-submit protection。
- illegal-state feedback。
- error fallback。
- empty state。
- auth guard。
- role guard。

不得隐藏关键交易状态，例如待支付、已支付、已发货、待收货、已完成、退款中、退款完成、待评价。

## 9. Admin Safety

后台专指系统管理员。

后台 UI 重构应保持：

- 现有管理员认证和权限规则。
- 已有列表分页参数。
- 已有处理动作。
- 表格、筛选、详情抽屉或详情页的可用性。

新增客服与消息治理入口时，如后端 API 未完成，应先做 UI 壳或入口占位，并明确标记后续后端任务。

## 10. Verification Checklist

每个前端 UI 重构任务完成前，至少检查：

- [ ] 没有页面直接手写新 axios 请求。
- [ ] 没有删除旧关键路由。
- [ ] 新路由有兼容或跳转策略。
- [ ] 共享数据仍通过 store/API module/normalizer。
- [ ] 没有修改后端 API 契约，或已同步更新 API spec 与 `.http`。
- [ ] 交易相关操作仍有 loading、错误、空态、非法状态反馈。
- [ ] 聊天、客服、店主能力没有假装后端已完成。
- [ ] 移动端核心路径可用。
- [ ] 运行了相关前端测试或说明未运行原因。

## 11. When Backend Changes Are Actually Needed

如果 UI 重构发现后端能力不足，应停止把它伪装成前端问题，并新增或更新任务文档。

典型需要后端任务的情况：

- 需要真实消息发送、会话列表、客服会话或群聊。
- 需要明确店主身份、店铺管理权限、店铺数据统计。
- 需要新的订单状态或售后状态。
- 需要改变 API 响应字段或分页结构。
- 需要新增用户设置项并持久化。
- 需要新增管理员治理动作。

这些改动必须同步：

- 后端实现。
- 前端 API module。
- store 或 normalizer。
- `docs/09-api-spec/`。
- `docs/06-http/`。
- 测试。
