# 功能开发路线图

本文档基于用户提供的开发计划，按功能完成度组织。

## 功能分组

### 1. 消息功能完善

**任务**：
- [ ] 消息列表分页优化
- [ ] 未读消息计数实时更新
- [ ] 消息发送失败重试机制
- [ ] 图片消息支持
- [ ] 消息通知（站内通知）

---

### 2. 消息、交易、设置 UI/UX 改进

**任务**（需进一步调研细化）：
- [x] 消息中心界面优化
- [x] 交易中心实时状态、订单详情、移动主操作优化
- [ ] 设置页面信息架构重组
- [ ] 移动端适配优化（交易主流程已完成，其他页面待收口）
- [ ] 空状态和错误状态设计统一（交易主流程已完成，其他页面待收口）

---

### 3. 其余功能开发

#### 3.1 优惠券系统

- [ ] 优惠券创建、发放、使用、过期管理
- [ ] 订单结算时优惠券应用
- [ ] 并发控制（防止超发）

第一阶段范围以 `docs/02-requirements/marketing-mvp-scope.md` 为准：

- 店主创建优惠券，管理员审核后发行。
- 用户主动领取优惠券，并在订单预览/下单时使用。
- 仅支持立减券和满减券。
- 建立领取与使用的基础并发控制。

#### 3.2 店铺活动

**数据库**：
**任务**：
- [ ] 活动创建和管理
- [ ] 管理员审核后展示
- [ ] 前台有效活动展示

第一阶段只做活动发布、审核和展示闭环，不做活动规则引擎和活动效果统计。

#### 3.3 数据统计

**任务**：
- [ ] 用户行为分析（浏览、搜索、购买漏斗）
- [ ] 商品销售排行
- [ ] 店铺经营报表
- [ ] 平台运营大盘（GMV、活跃用户）

#### 3.4 用户个性化设置

**任务**：
- [ ] 界面主题切换（亮色/暗色）
- [ ] 语言切换（中文/英文，i18n）
- [ ] 通知偏好设置
- [ ] 隐私设置

---

### 4. 管理员功能和界面开发

**任务**：
- [ ] 管理员权限细化（超级管理员、审核员、客服）
- [ ] 数据统计面板
- [ ] 批量操作（批量下架商品、批量处理举报）
- [ ] 操作日志审计
- [ ] 系统配置管理（热搜词权重、推荐参数）

---

### 5. 网站安全性升级

**参考**：`docs/04-standards/operations-and-deployment.md` § Security Hardening

**任务**：
- [ ] HTTPS 配置
- [ ] JWT 密钥管理（生产环境）
- [ ] CORS 配置
- [ ] 输入验证（JSR-303 Bean Validation）
- [ ] SQL 注入防护审查

---

### 6. 性能优化

**参考**：`docs/03-architecture/performance-and-scalability.md`

**任务**：
- [ ] Redis 缓存（推荐结果、热搜词、商品详情）
- [ ] 异步搜索日志
- [ ] HTTP 缓存头配置
- [ ] 接口限流
- [ ] 数据库索引优化

---

### 7. 基础设施完善

**参考**：`docs/04-standards/operations-and-deployment.md` § Infrastructure Setup

**任务**：
- [ ] 数据库连接池配置
- [ ] 日志配置（生产级别、文件轮转）
- [ ] 健康检查端点
- [ ] 监控告警（Prometheus + Grafana）

---

### 8. 合规性完善

**参考**：`docs/03-architecture/data-management-and-privacy.md`

**任务**：
- [ ] 隐私政策页面
- [ ] 用户协议页面
- [ ] Cookie 政策页面
- [ ] Cookie 同意横幅
- [ ] 注册流程添加协议同意
- [ ] 用户同意日志（`user_consent_logs` 表）

---

### 9. 存储和数据管理优化

**参考**：`docs/03-architecture/data-management-and-privacy.md` § Storage Cost Analysis

**任务**：
- [ ] 图片迁移到对象存储（OSS）
- [ ] 数据归档策略（旧订单、搜索日志）
- [ ] 数据导出功能（用户权利）
- [ ] 账户删除功能（软删除）

---

### 10. 备份系统构建

**任务**：
- [ ] 数据库自动备份脚本
- [ ] 备份恢复测试
- [ ] 备份监控告警

---

### 11. 上线前 Docker 打包管理

**参考**：`docs/04-standards/operations-and-deployment.md` § Containerization

**任务**：
- [ ] Backend Dockerfile
- [ ] Frontend Dockerfile + Nginx 配置
- [ ] Docker Compose 配置
- [ ] CI/CD 部署流程
- [ ] 生产环境配置（`application-prod.yml`）

---

## 参考文档

所有技术细节、验证标准、实施方案参见：
- `docs/03-architecture/data-management-and-privacy.md`
- `docs/03-architecture/performance-and-scalability.md`
- `docs/04-standards/operations-and-deployment.md`

## 2026-05-29 Marketing MVP Note

- The first marketing MVP slice is complete: coupons and shop activities now have owner submission, admin review, and buyer/public participation paths.
- Remaining launch-preparation marketing items are later-stage extensions only: statistics, advanced rules, stacking, and automated selection are still deferred.
