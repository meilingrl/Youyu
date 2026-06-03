import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const HEADER_CONDENSE_SCROLL_Y = 180
const HEADER_EXPAND_SCROLL_Y = 40

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
    // Keep a wide dead zone so condensed layout transitions cannot retrigger at the boundary.
    if (!isHeaderCondensed.value && value > HEADER_CONDENSE_SCROLL_Y) {
      isHeaderCondensed.value = true
    } else if (isHeaderCondensed.value && value < HEADER_EXPAND_SCROLL_Y) {
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
