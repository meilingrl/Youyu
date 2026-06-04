<template>
  <main class="shell-container legal-page">
    <section class="shell-card legal-page__content">
      <span class="eyebrow">法律文档</span>
      <h1>用户协议</h1>
      <p>最后更新：2026-06-04。版本：演示上线准备基线。</p>

      <h2>服务范围</h2>
      <p>
        有鱼是面向学生学习和宿舍生活商品的校园交易平台演示系统，提供商品发布、浏览、下单、评价、
        客服和治理流程。
      </p>

      <h2>用户责任</h2>
      <p>
        用户应提供真实准确的注册和认证信息，妥善保管账号凭证，并避免发布违禁、仿冒、不安全或误导性的
        商品与消息。
      </p>

      <h2>交易记录</h2>
      <p>
        订单、支付记录、退款、履约事实和评价记录会为平台交易追溯而保留。除非另行完成真实支付配置，
        当前支付行为仍为模拟或沙箱导向实现。
      </p>

      <h2>账号关闭</h2>
      <p>
        用户可以通过已实现的账号接口申请关闭账号。当前基线会关闭并匿名化账号，同时保留历史交易记录。
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
