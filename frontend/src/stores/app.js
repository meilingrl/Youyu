import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', () => {
  const keyword = ref('')
  const collapsed = ref(false)
  const scrollY = ref(0)
  const isHeaderCondensed = ref(false)

  const hasKeyword = computed(() => keyword.value.trim().length > 0)

  function setKeyword(value) {
    keyword.value = value
  }

  function toggleCollapsed() {
    collapsed.value = !collapsed.value
  }

  function setScrollY(value) {
    scrollY.value = value
    // 滞后：超过 120px 才收起，低于 72px 才展开，消除临界点抽搐
    if (!isHeaderCondensed.value && value > 120) {
      isHeaderCondensed.value = true
    } else if (isHeaderCondensed.value && value < 72) {
      isHeaderCondensed.value = false
    }
  }

  return {
    keyword,
    collapsed,
    scrollY,
    hasKeyword,
    isHeaderCondensed,
    setKeyword,
    toggleCollapsed,
    setScrollY
  }
})
