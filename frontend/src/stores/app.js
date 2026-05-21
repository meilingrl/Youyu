import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', () => {
  const keyword = ref('')
  const collapsed = ref(false)

  const hasKeyword = computed(() => keyword.value.trim().length > 0)

  function setKeyword(value) {
    keyword.value = value
  }

  function toggleCollapsed() {
    collapsed.value = !collapsed.value
  }

  return {
    keyword,
    collapsed,
    hasKeyword,
    setKeyword,
    toggleCollapsed
  }
})
