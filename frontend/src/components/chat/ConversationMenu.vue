<script setup>
import { onBeforeUnmount, ref } from 'vue'

const props = defineProps({
  isPinned: {
    type: Boolean,
    default: false
  },
  isMuted: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['pin', 'mute', 'delete'])
const open = ref(false)
const rootRef = ref(null)

function toggleMenu() {
  open.value = !open.value
  if (open.value) {
    window.addEventListener('click', onOutsideClick)
  }
}

function closeMenu() {
  open.value = false
  window.removeEventListener('click', onOutsideClick)
}

function onOutsideClick(event) {
  if (!rootRef.value?.contains(event.target)) {
    closeMenu()
  }
}

function choose(action) {
  emit(action)
  closeMenu()
}

onBeforeUnmount(() => {
  window.removeEventListener('click', onOutsideClick)
})
</script>

<template>
  <div ref="rootRef" class="conversation-menu" @click.stop>
    <button
      type="button"
      class="conversation-menu__trigger"
      :aria-expanded="open"
      aria-label="会话操作"
      @click="toggleMenu"
    >
      ⋯
    </button>
    <div v-if="open" class="conversation-menu__panel" role="menu">
      <button type="button" role="menuitem" @click="choose('pin')">
        {{ isPinned ? '取消置顶' : '置顶会话' }}
      </button>
      <button type="button" role="menuitem" @click="choose('mute')">
        {{ isMuted ? '取消静音' : '静音会话' }}
      </button>
      <button type="button" class="is-danger" role="menuitem" @click="choose('delete')">
        删除会话
      </button>
    </div>
  </div>
</template>

<style scoped>
.conversation-menu {
  position: relative;
  flex-shrink: 0;
}

.conversation-menu__trigger {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #78716c;
  font-size: 20px;
  cursor: pointer;
}

.conversation-menu__panel {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  z-index: 30;
  min-width: 128px;
  padding: 6px;
  border: 1px solid #e7e5e4;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 12px 24px rgba(31, 41, 55, 0.14);
}

.conversation-menu__panel button {
  width: 100%;
  display: block;
  padding: 9px 10px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #1f2937;
  font: inherit;
  font-size: 13px;
  text-align: left;
  cursor: pointer;
}

.conversation-menu__panel button.is-danger {
  color: #dc2626;
}

.conversation-menu__panel button:hover,
.conversation-menu__panel button:focus-visible {
  background: #fff7ed;
  color: #ea580c;
  outline: none;
}
</style>
