<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from '@/plugins/element-plus-services'
import { publishAdminNotification } from '@/api/modules/admin'
import { resolveErrorMessage } from '@/utils/error-utils'

const publishing = ref(false)
const publishedCount = ref(null)
const form = reactive({
  title: '',
  body: '',
  actionUrl: ''
})

async function publishNotification() {
  if (!form.title.trim() || !form.body.trim() || publishing.value) {
    if (!form.title.trim() || !form.body.trim()) {
      ElMessage.warning('请填写通知标题和正文')
    }
    return
  }

  publishing.value = true
  try {
    const response = await publishAdminNotification({
      title: form.title.trim(),
      body: form.body.trim(),
      actionUrl: form.actionUrl.trim()
    })
    publishedCount.value = Number(response.data?.recipientCount || 0)
    ElMessage.success(`系统通知已发布，共触达 ${publishedCount.value} 位用户`)
    form.title = ''
    form.body = ''
    form.actionUrl = ''
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '通知发布失败，请稍后重试'))
  } finally {
    publishing.value = false
  }
}
</script>

<template>
  <div class="notification-publish page-stack">
    <section class="shell-hero shell-hero--compact">
      <div>
        <span class="shell-hero__eyebrow">平台运营</span>
        <h1>发布通知</h1>
        <p>向所有有效普通用户发布站内系统通知。发布后会立即出现在用户消息中心的通知列表中。</p>
      </div>
    </section>

    <section class="notification-publish__layout">
      <article class="shell-panel notification-publish__form">
        <header>
          <h2>通知内容</h2>
          <p>标题和正文为必填项。跳转地址可留空，也可以填写站内路径，例如 <code>/app/home</code>。</p>
        </header>

        <el-form label-position="top">
          <el-form-item label="通知标题">
            <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="请输入通知标题" />
          </el-form-item>
          <el-form-item label="通知正文">
            <el-input
              v-model="form.body"
              type="textarea"
              :rows="6"
              maxlength="4000"
              show-word-limit
              placeholder="请输入面向用户展示的通知正文"
            />
          </el-form-item>
          <el-form-item label="站内跳转地址">
            <el-input v-model="form.actionUrl" maxlength="512" placeholder="可选，例如 /app/home" />
          </el-form-item>
          <el-button type="primary" :loading="publishing" @click="publishNotification">
            发布系统通知
          </el-button>
        </el-form>
      </article>

      <aside class="shell-panel notification-publish__aside">
        <h2>发布说明</h2>
        <ul>
          <li>当前入口仅发布站内系统通知，不发送短信、邮件或外部推送。</li>
          <li>接收范围固定为状态正常的普通用户，不包含后台账号。</li>
          <li>每次发布都会记录后台审计日志，便于追踪操作。</li>
        </ul>
        <p v-if="publishedCount !== null" class="notification-publish__success">
          最近一次发布已触达 {{ publishedCount }} 位用户。
        </p>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.notification-publish__layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(240px, 320px);
  gap: 18px;
  align-items: start;
}

.notification-publish__form,
.notification-publish__aside {
  display: grid;
  gap: 16px;
}

.notification-publish h2,
.notification-publish p {
  margin: 0;
}

.notification-publish header p,
.notification-publish__aside {
  color: var(--cm-text-secondary);
  line-height: 1.7;
}

.notification-publish__aside ul {
  display: grid;
  gap: 10px;
  margin: 0;
  padding-left: 20px;
}

.notification-publish__success {
  border-radius: 8px;
  background: rgba(var(--cm-primary-rgb), 0.08);
  color: var(--cm-primary);
  padding: 12px;
  font-weight: 700;
}

@media (max-width: 900px) {
  .notification-publish__layout {
    grid-template-columns: 1fr;
  }
}
</style>
