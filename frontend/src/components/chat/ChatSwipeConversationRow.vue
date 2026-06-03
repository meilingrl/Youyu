<script setup>
import { computed, ref } from 'vue'

const ZONE = 92
const THRESHOLD = 28

const props = defineProps({
  item: {
    type: Object,
    required: true
  },
  active: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['select', 'pin', 'mute', 'delete'])

const offset = ref(0)
const dragging = ref(false)
const leftActionsOpen = ref(false)
let startX = 0
let startY = 0
let pointerId = null

const rowStyle = computed(() => ({
  transform: `translateX(${offset.value}px)`
}))

const rightActionOpacity = computed(() => Math.min(Math.max(offset.value / THRESHOLD, 0), 1))
const leftActionOpacity = computed(() => Math.min(Math.max(Math.abs(offset.value) / THRESHOLD, leftActionsOpen.value ? 1 : 0), 1))

function clamp(value) {
  return Math.max(-ZONE, Math.min(ZONE, value))
}

function onPointerDown(event) {
  if (event.button !== undefined && event.button !== 0) return
  dragging.value = true
  pointerId = event.pointerId
  startX = event.clientX
  startY = event.clientY
  event.currentTarget.setPointerCapture?.(event.pointerId)
}

function onPointerMove(event) {
  if (!dragging.value || pointerId !== event.pointerId) return
  const deltaX = event.clientX - startX
  const deltaY = event.clientY - startY
  if (Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > 10) return
  offset.value = clamp(deltaX)
}

function onPointerUp(event) {
  if (!dragging.value || pointerId !== event.pointerId) return
  dragging.value = false
  event.currentTarget.releasePointerCapture?.(event.pointerId)

  if (offset.value > THRESHOLD) {
    emit('pin')
    closeActions()
    return
  }

  if (offset.value < -THRESHOLD) {
    leftActionsOpen.value = true
    offset.value = -ZONE
    return
  }

  if (!leftActionsOpen.value) {
    offset.value = 0
  } else {
    offset.value = -ZONE
  }
}

function closeActions() {
  leftActionsOpen.value = false
  offset.value = 0
}

function select() {
  if (leftActionsOpen.value) {
    closeActions()
    return
  }
  emit('select')
}

function choose(action) {
  emit(action)
  closeActions()
}
</script>

<template>
  <article
    class="chat-swipe-row"
    :class="{ 'is-active': active, 'is-pinned': item.isPinned, 'is-muted': item.isMuted, 'is-dragging': dragging }"
  >
    <div class="chat-swipe-row__underlay chat-swipe-row__underlay--pin" :style="{ opacity: rightActionOpacity }">
      <span>{{ item.isPinned ? '取消置顶' : '置顶' }}</span>
    </div>

    <div class="chat-swipe-row__underlay chat-swipe-row__underlay--left" :style="{ opacity: leftActionOpacity }">
      <button type="button" @click.stop="choose('mute')">
        {{ item.isMuted ? '取消静音' : '静音' }}
      </button>
      <button type="button" class="is-danger" @click.stop="choose('delete')">删除</button>
    </div>

    <button
      type="button"
      class="chat-swipe-row__surface"
      :style="rowStyle"
      @click="select"
      @pointerdown="onPointerDown"
      @pointermove="onPointerMove"
      @pointerup="onPointerUp"
      @pointercancel="closeActions"
    >
      <span class="chat-swipe-row__avatar">{{ item.title.slice(0, 1) }}</span>
      <span class="chat-swipe-row__content">
        <span class="chat-swipe-row__top">
          <strong>{{ item.title }}</strong>
          <time>{{ item.time }}</time>
        </span>
        <span class="chat-swipe-row__preview">{{ item.preview }}</span>
        <span class="chat-swipe-row__meta">
          <span v-if="item.categoryLabel">{{ item.categoryLabel }}</span>
          <span v-if="item.isPinned">置顶</span>
          <span v-if="item.isMuted">静音</span>
        </span>
      </span>
      <span v-if="item.unread" class="chat-swipe-row__unread">{{ item.unread }}</span>
    </button>
  </article>
</template>

<style scoped>
.chat-swipe-row {
  position: relative;
  flex: 0 0 auto;
  overflow: hidden;
  border-radius: 12px;
  background: #f5f5f4;
}

.chat-swipe-row__underlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  pointer-events: none;
  transition: opacity 0.16s ease;
}

.chat-swipe-row__underlay--pin {
  justify-content: flex-start;
  padding-left: 16px;
  background: #fef3c7;
  color: #92400e;
  font-size: 13px;
  font-weight: 800;
}

.chat-swipe-row__underlay--left {
  justify-content: flex-end;
  gap: 6px;
  padding-right: 8px;
  background: #f1f5f9;
  pointer-events: auto;
}

.chat-swipe-row__underlay button {
  height: 34px;
  min-width: 52px;
  border: none;
  border-radius: 8px;
  background: #e2e8f0;
  color: #334155;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.chat-swipe-row__underlay button.is-danger {
  background: #fee2e2;
  color: #b91c1c;
}

.chat-swipe-row__surface {
  position: relative;
  z-index: 1;
  width: 100%;
  box-sizing: border-box;
  display: flex;
  gap: 12px;
  align-items: center;
  min-height: 78px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: #fff;
  color: #1f2937;
  text-align: left;
  cursor: pointer;
  touch-action: pan-y;
  transition: transform 0.18s ease, border-color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;
}

.chat-swipe-row.is-dragging .chat-swipe-row__surface {
  transition: none;
}

.chat-swipe-row.is-active .chat-swipe-row__surface,
.chat-swipe-row__surface:hover,
.chat-swipe-row__surface:focus-visible {
  border-color: #fed7aa;
  background: #fffbf5;
  box-shadow: 0 4px 14px rgba(31, 41, 55, 0.08);
  outline: none;
}

.chat-swipe-row.is-pinned .chat-swipe-row__surface {
  border-color: #fdba74;
}

.chat-swipe-row.is-muted .chat-swipe-row__surface {
  background: #fafaf9;
}

.chat-swipe-row__avatar {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: #f1f5f9;
  color: #334155;
  font-weight: 800;
}

.chat-swipe-row.is-active .chat-swipe-row__avatar {
  background: rgba(234, 88, 12, 0.12);
  color: #c2410c;
}

.chat-swipe-row__content {
  min-width: 0;
  display: grid;
  gap: 5px;
  flex: 1 1 auto;
}

.chat-swipe-row__top,
.chat-swipe-row__meta {
  display: flex;
  align-items: center;
}

.chat-swipe-row__top {
  justify-content: space-between;
  gap: 8px;
}

.chat-swipe-row__top strong,
.chat-swipe-row__preview {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-swipe-row__top strong {
  min-width: 0;
  font-size: 15px;
}

.chat-swipe-row__top time,
.chat-swipe-row__preview {
  color: #78716c;
  font-size: 13px;
}

.chat-swipe-row__top time,
.chat-swipe-row__unread {
  flex-shrink: 0;
}

.chat-swipe-row__meta {
  gap: 6px;
  min-height: 18px;
}

.chat-swipe-row__meta span {
  padding: 2px 6px;
  border-radius: 999px;
  background: #f5f5f4;
  color: #78716c;
  font-size: 11px;
  font-weight: 700;
}

.chat-swipe-row__unread {
  min-width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 0 6px;
  background: #dc2626;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
}
</style>
