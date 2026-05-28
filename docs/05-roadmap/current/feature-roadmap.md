# 当前功能路线图

本文件只记录当前仍然影响开发排期的功能线。

已经完成并且短期内不会再作为主线推进的功能，不在这里展开历史叙述。

## 当前功能状态

| 功能线 | 状态 | 当前说明 |
|---|---|---|
| 聊天 MVP | 已实现，范围文档已恢复 | 当前范围边界见 `docs/02-requirements/chat-mvp-scope.md`；该文档以当前 runtime code、API spec 和 HTTP smoke collection 为事实源，archived chat tasks 仅作历史参考 |
| 平台调解 | 可进入边界任务 | 聊天范围缺失不再阻塞；下一步由 `docs/08-tasks/active/platform-mediation-boundary-and-contract.md` 定义 report/order/refund/chat visibility/support/mediation 边界 |
| 前端包体积治理 | 已完成第二轮 | 第二轮治理已归档：见 `docs/08-tasks/archived/frontend-bundle-second-pass.md`。管理端表格组件改为管理端布局动态注册，Vite `manualChunks` 已将表格模块留在非首屏预加载的异步 admin chunk 中 |
| 后台管理模块专项 | 已规划，进入任务包调度 | 专项路线见 `docs/05-roadmap/current/admin-module-goal-roadmap.md`；当前 active 任务包覆盖后台入口、聊天范围恢复、调解、可观测、审计、角色权限和全流程种子数据 |

## 已完成但不再作为当前主线展开的能力

以下能力已落地，不再作为当前路线图主体：

- 核心交易链路
- 举报治理闭环
- 偏好设置持久化
- 用户/店铺统计真实化
- 数字商品访问治理
- 热搜基础版、P1、P2、P3
- 商品/店铺评价
- 推荐能力
- 正式 API 规范化当前模块覆盖

这些内容保留在代码、HTTP 验证文件、任务归档和变更记录中追溯，不继续占用当前路线图主体。

## 推荐推进顺序

1. 平台调解边界定义
2. 平台调解实现
3. 后台可观测、审计、角色权限和全流程种子数据

## 依赖关系

- 平台调解依赖聊天 MVP 当前范围文档；`docs/02-requirements/chat-mvp-scope.md` 已恢复，调解边界任务可继续核定 read-only chat visibility
- 当前正式 API 规范已覆盖现有主要模块；后续 UI/UX 确定后按接口变化增量维护
- 前端包体积治理可以并行推进，但应作为横切任务单独管理
- 后台管理模块专项由 `admin-module-goal-roadmap.md` 细化调度，任务级约束仍以 `docs/08-tasks/active/` 为准

## 使用规则

- 当前功能状态发生变化时更新本文件
- 任务级实现细节不写在这里，统一进入 `../../08-tasks/`
- 横切治理项可以出现在这里，但必须有明确任务归属
