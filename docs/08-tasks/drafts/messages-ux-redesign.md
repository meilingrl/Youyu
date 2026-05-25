# Task: Messages UX Redesign

## Metadata

- ID: messages-ux-redesign
- Status: draft
- Owner: unassigned
- Track: feature
- Depends on: `ui-redesign-messages-support` (archived)
- Priority: high
- Planned date: 2026-05-25
- Completed date:

## Objective

重构消息中心界面,解决当前存在的两个核心问题:
1. 点击对话时页面跳回顶部(路由切换导致)
2. 界面缺乏呼吸感、留白不足、视觉层级不清晰,不符合 Youyu 的温暖校园气质

本次重构**仅涉及前端 UI/UX**,不触及后端逻辑,保持占位数据和禁用状态。

## Background

当前 `MessagesView.vue` 存在的问题:

### 问题1: 交互体验问题
- **症状**: 点击会话列表中的对话时,整个页面滚动回顶部
- **根因**: 使用 `router.push()` 切换路由导致整个组件重新挂载
- **影响**: 用户体验割裂,无法流畅浏览会话

### 问题2: 视觉设计问题
根据 `ui-ux-constitution.md` 的设计原则,当前界面违反了多项规范:

**违反的设计原则:**
- ❌ 缺乏呼吸感: 间距过小,信息密度过高
- ❌ 留白不足: 卡片、列表项、消息气泡之间缺乏足够的视觉分隔
- ❌ 过于素净: 缺乏温暖色调,过度使用灰色和白色
- ❌ 层级不清: 会话列表、分类标签、消息气泡的视觉权重不明确
- ❌ 信息过载: Hero 区域、Entry Context、底部说明区域占用过多空间
- ❌ 缺乏动效: 切换会话、展开分类时无过渡动画

**应遵循但未遵循的参考:**
- Notion 的呼吸感和克制排版
- Etsy 的温暖电商感
- Raycast 的毛玻璃和浮层立体感
- Apple 的动画速度曲线

## Design Principles (from ui-ux-constitution.md)

### 核心气质
- 温暖、清爽、有编辑感、愿意逛、可信
- 学习 Etsy 的温暖生活感,不使用科技蓝
- 主色使用赤陶、暖橙、烘焙色
- 背景使用暖白、米白、纸感浅色

### 间距系统
- 大区块之间: 56-80px
- 中型内容组: 32-40px
- 卡片内部: 16-24px
- 行内控件: 8-16px

### 圆角与层次
- 大型精选卡片: 24-32px
- 商品/店铺卡片: 18-24px
- 浮动面板: 20-28px
- 输入框/搜索壳: 999px(胶囊形)

### 动效系统
- 微交互: 140-180ms
- 普通过渡: 220-280ms
- 面板切换: 280-360ms
- 使用加速减速曲线,避免线性动画

## Proposed Design

### 整体布局

```
┌─────────────────────────────────────────────────────────┐
│  [消息中心 - 简洁标题]                                    │
├──────────────┬──────────────────────────────────────────┤
│              │                                          │
│  [分类标签]   │         [会话详情 Header]                │
│  交易 店铺    │         晨曦二手书铺                      │
│  客服 群聊    │                                          │
│              ├──────────────────────────────────────────┤
│  [会话列表]   │                                          │
│              │                                          │
│  ┌─────────┐ │         [消息气泡区域]                    │
│  │ 晨曦书铺 │ │                                          │
│  │ 退款...  │ │         系统: 双方可在此沟通...           │
│  │ 14:20   │ │                                          │
│  └─────────┘ │              店铺: 退款已提交审核...      │
│              │                                          │
│  ┌─────────┐ │         我: 好的,谢谢!                    │
│  │ 实验用品 │ │                                          │
│  │ 试剂...  │ │                                          │
│  └─────────┘ │                                          │
│              ├──────────────────────────────────────────┤
│              │  [输入区域 - 禁用状态]                    │
│              │  💬 消息发送功能即将开放                  │
│              │  [────────────────] [发送]               │
└──────────────┴──────────────────────────────────────────┘
```

### 视觉设计细节

#### 1. 顶部区域
- **移除**: Hero 区域、Entry Context、底部说明区域
- **保留**: 简洁的页面标题 "消息中心" + 小型状态标签
- **间距**: 顶部留白 40px,标题区域高度 80px

#### 2. 侧边栏(会话列表)
**宽度**: 360px(桌面端)

**分类标签设计:**
- 布局: 横向排列,4个标签(交易/店铺/客服/群聊)
- 样式: 圆角 16px,padding 12px 20px
- 默认态: 暖白背景(#FFF7ED),暖灰文字(#78716C)
- 选中态: 暖橙背景(#FED7AA),深橙文字(#EA580C)
- Hover: 轻微抬升 2px,阴影增强
- 间距: 标签之间 12px

**会话列表项:**
- 圆角: 20px
- 内边距: 16px 18px
- 间距: 列表项之间 12px
- 默认背景: 透明
- Hover: 暖白背景(#FFFBF5),轻微抬升 2px
- 选中: 暖橙渐变背景(#FFF7ED → #FED7AA),阴影加深

**会话项内容结构:**
```
[头像圆形 48px] [店铺名称 15px/600]     [时间 12px/灰]
                [消息预览 14px/次要]     [未读徽章]
```

**未读徽章:**
- 圆形,最小宽度 20px,高度 20px
- 背景: 暖橙色(#EA580C)
- 文字: 白色,12px,粗体

#### 3. 主区域(消息详情)

**Header:**
- 高度: 72px
- 背景: 白色,底部边框 1px 暖灰
- 内容: 头像(40px 圆角 12px) + 店铺名称(18px/600)
- 移动端: 添加返回按钮(36px 圆角 10px)

**消息区域:**
- 背景: 纸感浅色(#FAFAF9)
- 内边距: 32px
- 消息气泡间距: 20px

**消息气泡设计:**

*系统消息:*
- 居中对齐,最大宽度 80%
- 圆角: 16px
- 背景: 暖黄色(#FEF3C7)
- 文字: 深棕色(#92400E)
- 内边距: 12px 20px
- 字号: 14px

*对方消息:*
- 左对齐,最大宽度 65%
- 圆角: 20px,左下角 6px
- 背景: 白色
- 文字: 深灰(#1F2937)
- 内边距: 14px 18px
- 字号: 15px
- 阴影: 0 2px 8px rgba(0,0,0,0.04)

*自己消息:*
- 右对齐,最大宽度 65%
- 圆角: 20px,右下角 6px
- 背景: 暖橙渐变(#FB923C → #EA580C)
- 文字: 白色
- 内边距: 14px 18px
- 字号: 15px
- 阴影: 0 2px 12px rgba(234,88,12,0.2)

**输入区域:**
- 背景: 暖白色(#FFFBF5)
- 内边距: 20px 24px
- 顶部边框: 1px 暖灰

*禁用提示:*
- 圆角: 16px
- 背景: 暖橙浅色(#FFF7ED)
- 文字: 14px,暖橙色(#EA580C)
- 内边距: 12px 18px
- 图标: 💬

*输入框:*
- 圆角: 18px(胶囊形)
- 边框: 1px 暖灰
- 背景: 白色
- 禁用态: 文字浅灰,光标禁用

*发送按钮:*
- 圆角: 18px
- 背景: 浅灰(禁用态)
- 内边距: 12px 28px
- 字号: 15px,粗体

#### 4. 动效设计

**会话切换:**
- 时长: 280ms
- 曲线: cubic-bezier(0.4, 0.0, 0.2, 1)
- 效果: 消息区域淡入 + 轻微上移(8px)

**分类切换:**
- 时长: 220ms
- 曲线: ease-out
- 效果: 会话列表淡入淡出

**Hover 动画:**
- 时长: 160ms
- 曲线: ease-out
- 效果: 抬升 2px + 阴影增强

**消息滚动:**
- 自动滚动到底部: smooth behavior
- 新消息出现: 淡入 + 轻微上移

#### 5. 响应式设计

**移动端(<900px):**
- 列表/详情完全切换(不共存)
- 分类标签改为 2x2 网格
- 会话列表项高度增加到 80px
- 消息气泡最大宽度 85%
- 输入区域内边距减少到 16px

**平板端(900-1200px):**
- 侧边栏宽度减少到 320px
- 消息气泡最大宽度 70%

## Technical Solution

### 问题1解决方案: 移除路由依赖

**当前实现(有问题):**
```javascript
function openConversation(conversationId) {
  router.push({
    path: `/app/messages/${conversationId}`,
    query: buildQuery()
  })
}
```

**新实现:**
```javascript
const selectedConversationId = ref(null)

function openConversation(conversationId) {
  selectedConversationId.value = conversationId
  // 不再使用 router.push
}

const activeConversation = computed(() => {
  if (!selectedConversationId.value) return null
  return placeholderConversations.find(c => c.id === selectedConversationId.value)
})
```

**优势:**
- 点击会话时不触发路由变化
- 组件不重新挂载
- 页面滚动位置保持
- 可添加平滑过渡动画

### 问题2解决方案: CSS 变量系统

**新增 CSS 变量(在组件内定义):**
```css
:root {
  /* 温暖色系 */
  --msg-primary: #EA580C;
  --msg-primary-light: #FED7AA;
  --msg-primary-bg: #FFF7ED;
  --msg-warm-white: #FFFBF5;
  --msg-paper: #FAFAF9;
  
  /* 文字颜色 */
  --msg-text-primary: #1F2937;
  --msg-text-secondary: #6B7280;
  --msg-text-tertiary: #9CA3AF;
  
  /* 间距 */
  --msg-space-xs: 8px;
  --msg-space-sm: 12px;
  --msg-space-md: 16px;
  --msg-space-lg: 20px;
  --msg-space-xl: 32px;
  
  /* 圆角 */
  --msg-radius-sm: 12px;
  --msg-radius-md: 16px;
  --msg-radius-lg: 20px;
  --msg-radius-pill: 999px;
  
  /* 阴影 */
  --msg-shadow-soft: 0 2px 8px rgba(0, 0, 0, 0.04);
  --msg-shadow-md: 0 4px 12px rgba(0, 0, 0, 0.08);
  --msg-shadow-primary: 0 2px 12px rgba(234, 88, 12, 0.2);
  
  /* 动画 */
  --msg-transition-fast: 160ms ease-out;
  --msg-transition-base: 220ms ease-out;
  --msg-transition-slow: 280ms cubic-bezier(0.4, 0.0, 0.2, 1);
}
```

## Files to Read

- `frontend/src/views/app/MessagesView.vue` (当前实现)
- `docs/03-architecture/ui-ux-constitution.md` (设计规范)
- `docs/03-architecture/frontend-information-architecture.md` (信息架构)
- `frontend/src/styles/variables.css` (全局变量)
- `frontend/src/components/common/EmptyState.vue` (空状态组件)

## In Scope

### 前端改动
1. **移除路由依赖**
   - 改用本地状态管理会话选择
   - 移除 `openConversation` 中的 `router.push`
   - 保留移动端返回按钮功能

2. **简化页面结构**
   - 移除 Hero 区域
   - 移除 Entry Context 调试区域
   - 移除底部说明区域
   - 保留核心的列表+详情布局

3. **重构视觉样式**
   - 实现温暖色系(暖橙/暖白/纸感)
   - 增加间距和留白
   - 优化圆角和阴影
   - 实现消息气泡渐变背景

4. **添加过渡动画**
   - 会话切换动画
   - Hover 抬升动画
   - 消息滚动动画

5. **响应式优化**
   - 移动端列表/详情切换
   - 平板端布局调整

## Out of Scope

- ❌ 后端 API 开发
- ❌ 真实消息发送功能
- ❌ WebSocket 实时通信
- ❌ 数据库表设计
- ❌ 未读数统计逻辑
- ❌ 消息搜索功能
- ❌ 图片/文件上传
- ❌ 管理端支持页面改造

## Hard Limits

- **不修改**: 后端任何文件
- **不修改**: 路由配置文件(保持现有路由结构)
- **不修改**: 全局样式文件(所有样式在组件内 scoped)
- **不触及**: 占位数据结构(保持现有 `placeholderConversations` 格式)
- **不启用**: 发送消息功能(保持禁用状态)

## Allowed Changes

- `frontend/src/views/app/MessagesView.vue` (完全重写)
- `CHANGELOG.md` (添加变更记录)

## Implementation Steps

### Step 1: 备份与准备
1. 确认当前 git 状态干净
2. 创建新分支 `feat/messages-ux-redesign`
3. 备份当前 `MessagesView.vue` 到临时文件

### Step 2: 重构脚本逻辑
1. 移除路由依赖:
   - 添加 `selectedConversationId` ref
   - 修改 `openConversation` 函数
   - 修改 `activeConversation` computed
   - 保留移动端 `backToList` 功能

2. 添加动画支持:
   - 添加 `messagesEndRef` ref
   - 实现 `scrollToBottom` 函数
   - 在 `openConversation` 中调用滚动

3. 简化分类逻辑:
   - 移除 `buildQuery` 函数
   - 简化 `selectedCategory` 为本地 ref
   - 移除 `entryContext` computed

### Step 3: 重构模板结构
1. 移除冗余区域:
   - 删除 Hero section
   - 删除 Entry Context section
   - 删除底部说明 section

2. 简化布局:
   - 使用 `messages-container` 作为根容器
   - 左侧 `messages-sidebar`
   - 右侧 `messages-main`

3. 优化会话列表:
   - 简化分类标签结构
   - 优化会话项布局
   - 添加未读徽章

4. 优化消息详情:
   - 简化 Header
   - 重构消息气泡结构
   - 优化输入区域

### Step 4: 重写样式系统
1. 定义 CSS 变量
2. 实现侧边栏样式
3. 实现主区域样式
4. 实现消息气泡样式
5. 添加过渡动画
6. 实现响应式断点

### Step 5: 测试验证
1. 启动开发服务器: `npm run dev`
2. 测试桌面端:
   - 点击会话切换(验证不跳回顶部)
   - 切换分类
   - Hover 动画
   - 消息滚动
3. 测试移动端:
   - 列表/详情切换
   - 返回按钮
   - 触摸交互
4. 测试响应式断点

### Step 6: 文档更新
1. 更新 `CHANGELOG.md`
2. 截图对比(可选)

## Test Plan

### 功能测试
- [ ] 点击会话时页面不跳回顶部
- [ ] 会话切换有平滑过渡动画
- [ ] 分类切换正常工作
- [ ] 移动端列表/详情切换正常
- [ ] 返回按钮功能正常
- [ ] 消息自动滚动到底部

### 视觉测试
- [ ] 温暖色系正确应用
- [ ] 间距和留白符合设计规范
- [ ] 圆角和阴影正确
- [ ] 消息气泡渐变背景正确
- [ ] Hover 动画流畅
- [ ] 响应式布局正确

### 浏览器兼容性
- [ ] Chrome/Edge (主要)
- [ ] Firefox
- [ ] Safari (如果可用)

## Acceptance Criteria

- [ ] 点击会话时页面保持在当前位置,不跳回顶部
- [ ] 界面使用温暖色系(暖橙/暖白),不再过于素净
- [ ] 间距和留白符合 `ui-ux-constitution.md` 规范
- [ ] 消息气泡有清晰的视觉层级和渐变背景
- [ ] 会话切换有平滑过渡动画(280ms)
- [ ] Hover 动画流畅(160ms)
- [ ] 移动端列表/详情切换正常
- [ ] 响应式断点正确(900px, 640px)
- [ ] 所有占位数据和禁用状态保持不变
- [ ] 无控制台错误或警告

## Documentation Updates Required

- [x] `CHANGELOG.md`
- [ ] 截图对比(可选)

## Risks and Mitigations

### 风险1: 样式冲突
- **风险**: 新样式可能与全局样式冲突
- **缓解**: 使用 scoped 样式,所有样式在组件内定义

### 风险2: 动画性能
- **风险**: 过多动画可能影响性能
- **缓解**: 只动画 transform 和 opacity,避免布局属性

### 风险3: 响应式断点
- **风险**: 断点可能与其他页面不一致
- **缓解**: 使用 900px 断点与现有页面保持一致

## Final Report Format

```markdown
## Return Report — messages-ux-redesign

### A. Branch & Commit
- Branch: feat/messages-ux-redesign
- Commit SHA: <sha>
- Files changed: (paste `git diff --stat`)

### B. Implementation Summary
- 移除路由依赖: ✓
- 简化页面结构: ✓
- 重构视觉样式: ✓
- 添加过渡动画: ✓
- 响应式优化: ✓

### C. Test Results
- 功能测试: [通过/失败]
- 视觉测试: [通过/失败]
- 浏览器兼容性: [通过/失败]

### D. Screenshots
- 桌面端: [截图链接]
- 移动端: [截图链接]
- 动画演示: [GIF链接]

### E. Acceptance Criteria Check
- [x/✗] 每项验收标准的检查结果

### F. Known Issues
- 列出任何已知问题或限制

### G. Next Steps
- 建议的后续改进
```

## Completion Notes

(待执行后填写)
