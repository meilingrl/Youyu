# 任务清单：在线客服控制台 + 后台 UI/文案打磨

**Status:** completed (2026-05-31)  
**Branch:** `feat/admin-cs-console-ux-polish`  
**ADR:** `docs/07-decisions/2026-05-30-online-customer-service-on-chat.md`

## 完成摘要

- Task 2：在线客服（用户会话 + FAQ 机器人 + 转人工 + 管理端控制台）已落地；旧工单 API 保留。
- Task 3 + 5：后台侧栏图标统一；移除表格右滑选中行。
- Task 4：后台文案审阅完成。
- 遗留库兼容：`ChatSupportSchemaUpgrader` 在启动时补齐旧 MySQL 聊天表缺失列，修复管理端队列 500。

## 验收

- [x] 用户发起客服、AI 回复、转人工、结束/再次咨询
- [x] 管理端待接入队列认领、回复、结束
- [x] `mvnw.cmd test`、`npm test`、`npm run build` 通过
- [x] `CHANGELOG.md`、`docs/09-api-spec/chat.md`、`docs/06-http/chat.http` 已更新

---

> 以下为原始任务规格（归档保留）。

## 全局约束（所有子 Agent 必须遵守）

- 仅在本 worktree 路径下修改文件，不要触碰主仓库 `E:/Dev/Projects/Youyu`。
- 遵守 `CLAUDE.md` / `AGENTS.md`：分层架构、纯 JDBC mapper、`ApiResponse<T>`、`@LoginRequired`、Pinia + store 边界规范化、`EmptyState/ErrorBlock/SkeletonCard`、Element Plus 既有引入。
- 不使用 `ALTER TABLE`/`DROP TABLE`；新增列一律 `ADD COLUMN IF NOT EXISTS`，新表 `CREATE TABLE IF NOT EXISTS`，保持幂等且向后兼容。
- 不删除既有 `support_tickets` 工单系统及其测试；不改无关业务域。
- 不引入 websocket，不接入真实 LLM，不引入新 UI 库。
- 完成后跑通：后端 `mvnw.cmd test`、前端 `npm test`、`npm run build`，并按 CLAUDE.md 收尾清单更新 `CHANGELOG.md` / `docs/09-api-spec` / `docs/06-http`（涉及接口变更时）。

## Task 2 — 在线客服：用户聊天 + AI 客服 + 转人工

（见原始范围；已实现。）

## Task 3 — 后台左侧导航栏图标视觉协调

（已实现。）

## Task 5 — 去掉「右滑可选中行」交互

（已实现。）

## Task 4 — 后台整体文案改进

（已实现。）

## 执行编排（头 Agent）

- 阶段一：Task 2 + Task 3/5 并行完成。
- 阶段二：Task 4 文案 sweep 完成。
