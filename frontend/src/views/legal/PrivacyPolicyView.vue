<template>
  <main class="shell-container legal-page">
    <section class="shell-card legal-page__content">
      <span class="eyebrow">法律文档</span>
      <h1>隐私政策</h1>
      <p>最后更新：2026-06-04。版本：演示上线准备基线。</p>

      <h2>我们收集的数据</h2>
      <p>
        当相关功能被使用时，有鱼会保存账号信息、邮箱验证码记录、个人资料、收货地址、学生认证申请、
        偏好设置、订单、支付记录、评价、举报、消息和同意记录。
      </p>

      <h2>数据用途</h2>
      <p>
        当前系统将这些数据用于登录、交易履约、学生认证审核、支付和退款记录、用户偏好、客服处理、
        举报治理以及合规审计留痕。
      </p>

      <h2>第三方处理边界</h2>
      <p>
        当前支付和物流能力仍是模拟或凭证门控的演示流程。真实第三方生产接入需要在上线前另行配置、
        审查并完成验收。
      </p>

      <h2>用户权利</h2>
      <p>
        已登录用户可以通过用户接口申请个人数据导出。账号删除在当前基线中实现为软注销和个人资料匿名化；
        历史订单记录为交易追溯保留，不在本基线中硬删除。
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
