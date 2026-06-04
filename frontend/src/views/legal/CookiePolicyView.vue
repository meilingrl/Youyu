<template>
  <main class="shell-container legal-page">
    <section class="shell-card legal-page__content">
      <span class="eyebrow">法律文档</span>
      <h1>Cookie 政策</h1>
      <p>最后更新：2026-06-04。版本：演示上线准备基线。</p>

      <h2>必要存储</h2>
      <p>
        登录令牌会保存在本地，以支持已登录页面和接口请求。这是当前单页应用登录流程所需的必要存储。
      </p>

      <h2>功能性存储</h2>
      <p>
        功能性存储可能保存本地搜索历史和界面选择。如果拒绝功能性 Cookie，Cookie 偏好模块会清理本地搜索历史。
      </p>

      <h2>分析偏好</h2>
      <p>
        当前前端提供分析偏好开关，但本任务没有启用任何分析服务商。选择分析偏好只会记录该偏好，供未来接入前审查。
      </p>

      <h2>同意记录</h2>
      <p>
        用户登录后，Cookie 偏好会写入后端同意日志。匿名访客的选择只保存在浏览器本地。
      </p>
    </section>
  </main>
</template>

<style scoped>
.legal-page {
  padding: 32px 0 64px;
}

.legal-page__content {
  display: grid;
  gap: 18px;
}

.legal-page h1 {
  font-size: clamp(28px, 3vw, 42px);
}

.legal-page h2 {
  margin-top: 10px;
}

.legal-page p {
  color: var(--cm-text-secondary);
}
</style>
