<script setup>
const emit = defineEmits(['emoji', 'sticker'])

const emojis = ['馃榾', '馃槀', '馃槉', '馃憤', '馃憣', '馃檹', '馃', '馃憦', '馃帀', '馃摎', '馃挕', '✔', '铃', '馃挰', '❤️', '馃敟']

const stickers = [
  { id: 'ok', label: '收到', icon: '馃憣', color: '#f97316' },
  { id: 'thanks', label: '谢谢', icon: '馃檹', color: '#0f766e' },
  { id: 'checking', label: '我看下', icon: '馃攷', color: '#475569' },
  { id: 'deal', label: '成交', icon: '馃', color: '#16a34a' }
]
</script>

<template>
  <div class="emoji-sticker-panel">
    <section>
      <header>表情</header>
      <div class="emoji-sticker-panel__grid">
        <button
          v-for="emoji in emojis"
          :key="emoji"
          type="button"
          class="emoji-sticker-panel__emoji"
          @mousedown.prevent
          @click="emit('emoji', emoji)"
        >
          {{ emoji }}
        </button>
      </div>
    </section>

    <section>
      <header>表情包</header>
      <div class="emoji-sticker-panel__stickers">
        <button
          v-for="sticker in stickers"
          :key="sticker.id"
          type="button"
          class="emoji-sticker-panel__sticker"
          @mousedown.prevent
          @click="emit('sticker', sticker)"
        >
          <span :style="{ color: sticker.color }">{{ sticker.icon }}</span>
          <strong>{{ sticker.label }}</strong>
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.emoji-sticker-panel {
  width: min(360px, 100%);
  box-sizing: border-box;
  display: grid;
  gap: 12px;
  padding: 12px;
  border: 1px solid #e7e5e4;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(31, 41, 55, 0.12);
}

.emoji-sticker-panel header {
  margin-bottom: 8px;
  color: #57534e;
  font-size: 13px;
  font-weight: 800;
}

.emoji-sticker-panel__grid {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 6px;
}

.emoji-sticker-panel__emoji,
.emoji-sticker-panel__sticker {
  border: none;
  border-radius: 8px;
  background: #f5f5f4;
  cursor: pointer;
}

.emoji-sticker-panel__emoji {
  aspect-ratio: 1;
  font-size: 20px;
}

.emoji-sticker-panel__emoji:hover,
.emoji-sticker-panel__emoji:focus-visible,
.emoji-sticker-panel__sticker:hover,
.emoji-sticker-panel__sticker:focus-visible {
  background: #fff7ed;
  outline: 1px solid #fed7aa;
}

.emoji-sticker-panel__stickers {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.emoji-sticker-panel__sticker {
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #1f2937;
  font: inherit;
}

.emoji-sticker-panel__sticker span {
  font-size: 22px;
}

.emoji-sticker-panel__sticker strong {
  font-size: 14px;
}

@media (max-width: 520px) {
  .emoji-sticker-panel__grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }
}
</style>
