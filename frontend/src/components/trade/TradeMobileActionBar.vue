<script setup>
defineProps({
  eyebrow: {
    type: String,
    default: ''
  },
  value: {
    type: String,
    default: ''
  },
  helper: {
    type: String,
    default: ''
  },
  actionLabel: {
    type: String,
    required: true
  },
  buttonType: {
    type: String,
    default: 'primary'
  },
  loading: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['primary'])
</script>

<template>
  <div class="trade-mobile-action-bar">
    <div class="trade-mobile-action-bar__panel" aria-label="移动端交易操作">
      <div class="trade-mobile-action-bar__copy">
        <span v-if="eyebrow" class="trade-mobile-action-bar__eyebrow">{{ eyebrow }}</span>
        <strong v-if="value">{{ value }}</strong>
        <p v-if="helper">{{ helper }}</p>
      </div>
      <el-button
        :type="buttonType"
        :loading="loading"
        :disabled="disabled"
        class="trade-mobile-action-bar__button"
        @click="emit('primary')"
      >
        {{ actionLabel }}
      </el-button>
    </div>
  </div>
  <div class="trade-mobile-action-bar__spacer" aria-hidden="true"></div>
</template>

<style scoped>
.trade-mobile-action-bar,
.trade-mobile-action-bar__spacer {
  display: none;
}

@media (max-width: 768px) {
  .trade-mobile-action-bar {
    position: fixed;
    left: 12px;
    right: 12px;
    bottom: calc(96px + env(safe-area-inset-bottom, 0px));
    z-index: 39;
    display: block;
    pointer-events: none;
  }

  .trade-mobile-action-bar__panel {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 12px;
    align-items: center;
    padding: 12px;
    border: 1px solid var(--cm-border);
    border-radius: 22px;
    background: rgba(255, 250, 243, 0.94);
    box-shadow: 0 16px 44px rgba(88, 62, 43, 0.18);
    backdrop-filter: blur(var(--cm-blur-strong));
    -webkit-backdrop-filter: blur(var(--cm-blur-strong));
    pointer-events: auto;
  }

  .trade-mobile-action-bar__copy {
    display: grid;
    gap: 2px;
    min-width: 0;
  }

  .trade-mobile-action-bar__eyebrow {
    color: var(--cm-text-secondary);
    font-size: 12px;
    font-weight: 650;
    line-height: 1.35;
  }

  .trade-mobile-action-bar strong {
    overflow: hidden;
    color: var(--cm-price);
    font-size: 21px;
    line-height: 1.2;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .trade-mobile-action-bar p {
    overflow: hidden;
    color: var(--cm-text-secondary);
    font-size: 12px;
    line-height: 1.35;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .trade-mobile-action-bar__button {
    min-width: 118px;
    min-height: 48px;
    padding-inline: 18px;
  }

  .trade-mobile-action-bar__spacer {
    display: block;
    height: 104px;
  }
}

@media (max-width: 380px) {
  .trade-mobile-action-bar__panel {
    grid-template-columns: 1fr;
  }

  .trade-mobile-action-bar__button {
    width: 100%;
  }
}
</style>
