<script setup>
import { reactive } from 'vue'
import { useConsentStore } from '@/stores/consent'

const consentStore = useConsentStore()
const choices = reactive({
  functional: true,
  analytics: false
})

async function acceptSelected() {
  await consentStore.saveCookieConsent(choices)
}

async function acceptNecessaryOnly() {
  choices.functional = false
  choices.analytics = false
  await consentStore.saveCookieConsent(choices)
}
</script>

<template>
  <section v-if="!consentStore.hasCookieChoice" class="cookie-consent" aria-label="Cookie 同意设置">
    <div class="cookie-consent__copy">
      <strong>Cookie 偏好</strong>
      <p>
        有鱼使用必要存储维持登录状态。功能性存储用于保留本地搜索历史和界面选择；分析类 Cookie
        默认不启用，只有在你选择后才记录偏好。
      </p>
      <router-link to="/legal/cookie-policy">查看 Cookie 政策</router-link>
    </div>
    <div class="cookie-consent__controls">
      <el-checkbox v-model="choices.functional">功能性存储</el-checkbox>
      <el-checkbox v-model="choices.analytics">分析偏好</el-checkbox>
      <div class="cookie-consent__actions">
        <el-button plain :loading="consentStore.saving" @click="acceptNecessaryOnly">
          仅必要项
        </el-button>
        <el-button type="primary" :loading="consentStore.saving" @click="acceptSelected">
          保存选择
        </el-button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.cookie-consent {
  position: fixed;
  z-index: 80;
  right: 20px;
  bottom: 20px;
  width: min(520px, calc(100vw - 40px));
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid var(--cm-border);
  border-radius: 8px;
  background: rgba(255, 253, 249, 0.98);
  box-shadow: var(--cm-shadow-md);
}

.cookie-consent__copy,
.cookie-consent__controls {
  display: grid;
  gap: 10px;
}

.cookie-consent__copy p {
  color: var(--cm-text-secondary);
  line-height: 1.6;
}

.cookie-consent__copy a {
  width: fit-content;
  color: var(--cm-primary);
  font-weight: 700;
}

.cookie-consent__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

@media (max-width: 640px) {
  .cookie-consent {
    right: 12px;
    bottom: 12px;
    width: calc(100vw - 24px);
  }
}
</style>
