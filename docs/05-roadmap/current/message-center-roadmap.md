# 消息中心功能路线图

本文档为头 Agent 提供消息中心功能的完整规划,用于协调多个子 Agent 并行开发。

---

## P2 Completion Update

- Date: 2026-05-27
- Status: P2 experience optimization is complete.
- Completed scope: message search, conversation pin/mute/delete, message recall, auto-reply settings and trigger flow.
- Verification: backend tests, frontend tests, and frontend production build passed.

## P3 UI Polish Update

- Date: 2026-05-27
- Status: message-center UI polish pass is complete.
- Completed scope: clearer conversation rows, swipe conversation actions, emoji/sticker composer entry, scoped custom quick replies, floating multi/range date filter, localized notification UI/data, improved recall hover action, and bubble hover timestamps.
- Task record: `docs/08-tasks/archived/message-center-ui-polish.md`
- Verification: frontend tests, frontend production build, and targeted backend chat controller tests passed.

## 当前状态

**已完成**：
- ✅ 基础聊天 MVP（一对一聊天、会话列表、消息列表、发送消息）
- ✅ 用户认证和权限控制
- ✅ 轮询机制（8秒刷新）
- ✅ 响应式设计（桌面/移动端）
- ✅ 未读消息计数和已读状态
- ✅ 图片消息
- ✅ 站内通知系统
- ✅ 商品卡片消息
- ✅ 订单卡片消息
- ✅ 快捷回复
- ✅ 消息搜索
- ✅ 会话管理（置顶、删除、静音）
- ✅ 消息撤回
- ✅ 自动回复
- ✅ 消息界面 UI/UX 优化

**待开发功能**：
- 暂无已批准的消息中心后续任务

---

## 功能优先级

### P0 - 核心体验（1-2天）

电商消息系统的最基础需求,缺失会严重影响用户体验。

| 任务 ID | 功能 | 业务价值 | 预计工时 |
|---------|------|----------|----------|
| task-01 | 未读消息计数 + 已读状态（已完成） | 用户需要知道是否有新消息 | 6-8h |
| task-02 | 图片消息支持（已完成） | 买家发送问题截图,卖家发送商品细节图 | 6-8h |
| task-05 | 站内通知系统（已完成） | 订单状态变更、评价提醒等重要信息推送 | 8-10h |

**并行策略**: 3 个任务完全独立,可分配给 3 个子 Agent 同时开发。

---

### P1 - 电商特色（3-5天）

电商场景的特色需求,显著提升买卖双方沟通效率。

| 任务 ID | 功能 | 业务价值 | 预计工时 |
|---------|------|----------|----------|
| task-03 | 商品卡片消息（已完成） | 快速分享商品链接,提升转化率 | 6-8h |
| task-04 | 订单卡片消息（已完成） | 关联订单,方便售后沟通 | 6-8h |
| task-06 | 快捷回复（已完成） | 卖家常用语库,提升响应速度 | 6-8h |

**并行策略**:
- task-03 和 task-04 依赖 task-02,需等 task-02 完成后才能开始
- task-06 独立,可与 task-03/task-04 并行

---

### P2 - 体验优化（1-2周）

提升用户体验,但不影响核心交易流程。

| 任务 ID | 功能 | 业务价值 | 预计工时 |
|---------|------|----------|----------|
| task-08 | 消息搜索（已完成） | 按关键词、时间筛选历史消息 | 8-10h |
| task-09 | 会话管理（已完成） | 置顶、删除、静音会话 | 6-8h |
| task-10 | 消息撤回（已完成） | 2分钟内可撤回错误消息 | 4-6h |
| task-11 | 自动回复（已完成） | 卖家离线时自动回复 | 6-8h |

**并行策略**:
- task-08, task-09, task-10 完全独立,可并行
- task-11 依赖 task-06,需等 task-06 完成

---

## 任务依赖关系图

```
P0 阶段（3个任务,完全并行）:
├─ task-01: 未读消息计数 + 已读状态 [独立]
├─ task-02: 图片消息支持 [独立] ← 关键路径,后续任务依赖
└─ task-05: 站内通知系统 [独立]

P1 阶段（3个任务,部分并行）:
├─ task-03: 商品卡片消息 [已完成]
├─ task-04: 订单卡片消息 [已完成]
└─ task-06: 快捷回复 [已完成]

P2 阶段（4个任务,部分并行）:
├─ task-08: 消息搜索 [已完成]
├─ task-09: 会话管理 [已完成]
├─ task-10: 消息撤回 [已完成]
└─ task-11: 自动回复 [已完成]
```

---

## 头 Agent 工作流程

### 1. 启动阶段

**输入**: 用户指令"开始 P0 阶段开发"

**头 Agent 行动**:
1. 读取本 Roadmap,确认 P0 阶段包含 task-01, task-02, task-05
2. 检查依赖关系,确认 3 个任务可并行
3. 为每个任务创建子 Agent:
   - 子 Agent A: 负责 task-01
   - 子 Agent B: 负责 task-02
   - 子 Agent C: 负责 task-05
4. 向每个子 Agent 分配任务文档路径
5. 启动 3 个子 Agent 并行工作

### 2. 监控阶段

**头 Agent 职责**:
- 接收子 Agent 的进度报告
- 检查子 Agent 是否遇到阻塞
- 协调子 Agent 之间的冲突（如修改同一文件）
- 更新任务状态

### 3. 验收阶段

**头 Agent 职责**:
- 接收子 Agent 的完成报告
- 验证验收标准是否满足
- 运行集成测试
- 标记任务为已完成

### 4. 下一阶段

**头 Agent 行动**:
- 检查依赖关系,确认哪些任务可以开始
- 分配新任务给子 Agent
- 重复监控和验收流程

---

## 验收标准

### P0 阶段整体验收

**功能验收**:
- [ ] 导航栏显示未读消息红点（数字）
- [ ] 会话列表显示每个会话的未读计数
- [ ] 打开会话自动标记已读,红点消失
- [ ] 可以发送图片消息,点击图片放大预览
- [ ] 订单状态变更时自动发送站内通知
- [ ] 通知列表页面显示所有通知,点击跳转

**技术验收**:
- [ ] 所有后端测试通过（`mvnw.cmd test`）
- [ ] 所有前端测试通过（`npm test`）
- [ ] 无 ESLint/TypeScript 错误
- [ ] API 文档已更新（`docs/09-api-spec/`）
- [ ] HTTP 测试文件已更新（`docs/06-http/`）
- [ ] CHANGELOG.md 已更新

### P1 阶段整体验收

**功能验收**:
- [x] 商品详情页可以分享商品到聊天
- [x] 消息气泡正确显示商品卡片（图片、标题、价格）
- [x] 订单详情页可以联系卖家/买家,自动发送订单卡片
- [x] 卖家可以使用快捷回复（预设话术）

**技术验收**:
- [x] 所有测试通过
- [x] 文档已更新

### P2 阶段整体验收

**功能验收**:
- [x] 可以搜索历史消息（按关键词、时间）
- [x] 可以置顶、删除、静音会话
- [x] 可以撤回 2 分钟内的消息
- [x] 卖家离线时自动回复

**技术验收**:
- [x] 所有测试通过
- [x] 文档已更新

### P3 UI/UX 优化验收

**功能验收**:
- [x] 左侧会话行层级更清晰，并支持滑动置顶、静音和删除。
- [x] 表情、表情包入口可用，上传图片不会自动带文件名说明。
- [x] 快捷回复只展示当前场景预设和用户自定义内容，支持新增/删除。
- [x] 消息搜索日期筛选使用悬浮日历，支持多选和范围选择。
- [x] 通知页面使用中文 UI 和后端 seed 示例数据。
- [x] 撤回入口可点击性提升，气泡 hover 展示时间戳。

**技术验收**:
- [x] `frontend/npm test` 通过
- [x] `frontend/npm run build` 通过
- [x] targeted backend chat controller tests 通过

---

## 技术架构约束

所有子 Agent 必须遵守以下约束:

### 数据库设计
1. **幂等性**: 使用 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`
2. **向后兼容**: 新增字段使用默认值
3. **性能优化**: 使用冗余字段避免实时统计
4. **索引策略**: 为高频查询字段添加索引

### 后端实现
1. **纯 JDBC**: 使用 `JdbcTemplate`,不使用 ORM
2. **事务管理**: 使用 `@Transactional`
3. **权限验证**: 验证用户是否为会话/订单参与者
4. **错误处理**: 使用 `BusinessException`, `ForbiddenException`, `UnauthorizedException`

### 前端实现
1. **状态管理**: 使用 Pinia Composition API
2. **数据规范化**: 在 store 边界转换 snake_case → camelCase
3. **错误处理**: 使用 `utils/error-utils.js`
4. **空状态处理**: 使用 `EmptyState.vue`, `ErrorBlock.vue`, `SkeletonCard.vue`

---

## 性能指标

所有功能必须满足以下性能要求:

- 发送消息: < 500ms
- 加载会话列表: < 1s
- 加载消息列表: < 1s
- 搜索消息: < 2s

---

## 冲突解决策略

### 文件修改冲突

如果多个子 Agent 需要修改同一文件:

1. **优先级**: P0 > P1 > P2
2. **协调**: 头 Agent 协调修改顺序
3. **合并**: 头 Agent 负责合并冲突

### 数据库表冲突

如果多个子 Agent 需要修改同一张表:

1. **合并 DDL**: 头 Agent 合并所有 `ALTER TABLE` 语句
2. **验证**: 确保字段名不冲突
3. **索引**: 合并索引定义

---

## 参考文档

- **任务详情**: `docs/08-tasks/drafts/task-*.md`
- **架构约束**: `docs/03-architecture/`
- **API 规范**: `docs/09-api-spec/`
- **HTTP 测试**: `docs/06-http/`

---

## 更新日志

| 日期 | 变更 | 负责人 |
|------|------|--------|
| 2026-05-27 | 完成消息中心 P3 UI/UX 优化并归档任务 | Codex |
| 2026-05-27 | 完成 P2 体验优化并同步验收状态 | Codex |
| 2026-05-25 | 重写为头 Agent 协调文档 | Claude |
