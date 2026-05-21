<script setup>
const governanceLanes = [
  {
    title: '客服会话',
    eyebrow: 'Conversations',
    description: '未来承接用户对客服、平台介入和升级工单，但当前不展示真实消息流。',
    states: ['会话列表待接入', '指派与状态流待定义', '消息内容拉取待接入']
  },
  {
    title: '售后协助',
    eyebrow: 'After Sales',
    description: '与订单、退款、举报记录联动，为平台协助保留统一入口，而不是另造完整售后系统。',
    states: ['订单关联待接入', '协助结论待定义', '与退款/举报联动待补齐']
  },
  {
    title: '群聊治理',
    eyebrow: 'Groups',
    description: '预留粉丝群、优惠群和活动群的治理面，不在本轮落成员管理或群消息能力。',
    states: ['群模型未定义', '成员规则待定义', '治理动作待拆分']
  },
  {
    title: '异常消息',
    eyebrow: 'Risk Signals',
    description: '用于接敏感消息、异常会话和升级警报，后续再与搜索治理、举报和审核规则衔接。',
    states: ['检测规则待接入', '命中记录待接入', '处理闭环待定义']
  }
]

const integrationNeeds = [
  '客服会话列表 API：分页、会话类型、会话状态、最近消息摘要、指派人。',
  '消息详情 API：历史消息、系统事件、已读状态、附件元数据、禁用状态提示。',
  '消息发送 API：文本、图片/附件、售后升级动作，并带重复提交保护。',
  '平台治理 API：异常消息命中、会话升级、群聊治理动作、客服处理结论。'
]

const placeholderQueues = [
  {
    title: '待接入的客服工作台',
    summary: '客服列表、指派和响应 SLA 未来会在这里接入。',
    status: 'API 待接入'
  },
  {
    title: '售后协助面板',
    summary: '订单退款、举报与平台协助需要在统一上下文中查看。',
    status: '订单联动待定义'
  },
  {
    title: '群聊与优惠群治理',
    summary: '群聊状态、封禁与活动管理要等群模型落地后再拆子页。',
    status: '群模型待定义'
  }
]
</script>

<template>
  <div class="page-stack">
    <section class="shell-hero shell-hero--compact admin-support-hero">
      <div>
        <span class="eyebrow">Support Console</span>
        <h1>客服与消息治理入口</h1>
        <p>
          这里先建立 `/admin/support` 的首版后台入口，给客服会话、售后协助、群聊治理和异常消息四条治理方向一个清晰归属。
          当前没有接真实消息 API，因此页面只展示治理结构和接入边界。
        </p>
      </div>
      <div class="shell-inline-actions">
        <el-tag effect="plain" type="warning">后端待接入</el-tag>
        <el-tag effect="plain" type="info">不创建工单系统</el-tag>
      </div>
    </section>

    <section class="shell-card admin-support-alert">
      <strong>当前状态</strong>
      <p>
        后台客服入口已具备导航位置，但不展示真实会话数量、不支持处理消息，也不会伪造客服绩效数据。
        真正接入前请继续以订单、退款、举报等既有治理对象为准。
      </p>
    </section>

    <section class="metric-grid metric-grid--wide">
      <div v-for="lane in governanceLanes" :key="lane.title" class="metric-card admin-support-metric">
        <span>{{ lane.eyebrow }}</span>
        <strong>{{ lane.title }}</strong>
        <small>{{ lane.description }}</small>
      </div>
    </section>

    <section class="admin-support-grid">
      <article
        v-for="lane in governanceLanes"
        :key="lane.title"
        class="shell-card admin-support-lane"
      >
        <div class="admin-support-lane__header">
          <div>
            <span class="admin-support-lane__eyebrow">{{ lane.eyebrow }}</span>
            <h2>{{ lane.title }}</h2>
          </div>
          <el-tag effect="plain" type="info">占位中</el-tag>
        </div>

        <p>{{ lane.description }}</p>

        <ul class="admin-support-lane__list">
          <li v-for="state in lane.states" :key="state">{{ state }}</li>
        </ul>

        <div class="shell-inline-actions">
          <el-button plain disabled>查看队列</el-button>
          <el-button plain disabled>进入详情</el-button>
        </div>
      </article>
    </section>

    <section class="shell-card">
      <div class="section-heading">
        <h2>预留中的后台面板</h2>
      </div>

      <div class="admin-support-queue">
        <article
          v-for="item in placeholderQueues"
          :key="item.title"
          class="admin-support-queue__item"
        >
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.summary }}</p>
          </div>
          <el-tag effect="plain">{{ item.status }}</el-tag>
        </article>
      </div>
    </section>

    <section class="shell-card">
      <div class="section-heading">
        <h2>后续 API / 后端需求</h2>
      </div>

      <div class="admin-support-needs">
        <article
          v-for="item in integrationNeeds"
          :key="item"
          class="admin-support-needs__item"
        >
          {{ item }}
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin-support-hero,
.admin-support-grid,
.admin-support-queue,
.admin-support-needs {
  display: grid;
  gap: 18px;
}

.admin-support-alert {
  border-style: dashed;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 243, 238, 0.9));
}

.admin-support-alert strong {
  display: block;
  margin-bottom: 8px;
  font-size: 15px;
}

.admin-support-alert p,
.admin-support-metric small,
.admin-support-lane p,
.admin-support-needs__item,
.admin-support-queue__item p {
  color: var(--cm-text-secondary);
  line-height: 1.7;
}

.admin-support-metric {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 248, 250, 0.92));
}

.admin-support-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.admin-support-lane {
  display: grid;
  gap: 18px;
  min-height: 280px;
  box-shadow: none;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.82);
}

.admin-support-lane__header,
.admin-support-queue__item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.admin-support-lane__eyebrow {
  color: var(--cm-text-tertiary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.admin-support-lane h2 {
  margin-top: 6px;
}

.admin-support-lane__list {
  margin: 0;
  padding-left: 18px;
  color: var(--cm-text-secondary);
  display: grid;
  gap: 10px;
}

.admin-support-queue__item,
.admin-support-needs__item {
  padding: 18px;
  border-radius: 18px;
  border: 1px solid var(--cm-border);
  background: rgba(255, 255, 255, 0.7);
}

@media (max-width: 960px) {
  .admin-support-grid {
    grid-template-columns: 1fr;
  }

  .admin-support-lane__header,
  .admin-support-queue__item {
    flex-direction: column;
  }
}
</style>
