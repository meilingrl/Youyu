<script setup>
import ErrorBlock from '@/components/common/ErrorBlock.vue'
import EmptyState from '@/components/common/EmptyState.vue'

defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  },
  columns: {
    type: Array,
    default: () => []
  },
  rows: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: ''
  },
  emptyTitle: {
    type: String,
    default: ''
  },
  emptyDescription: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['retry'])
</script>

<template>
  <div class="page-stack">
    <div class="shell-card shell-hero shell-hero--compact">
      <div>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
      </div>
      <div class="shell-hero__meta">
        <slot name="toolbar" />
      </div>
    </div>

    <div class="shell-card">
      <slot name="summary" />

      <div class="list-shell__filters">
        <slot name="filters" />
      </div>

      <ErrorBlock v-if="error" :message="error" @retry="emit('retry')" />

      <EmptyState
        v-else-if="!loading && rows.length === 0 && emptyTitle"
        :title="emptyTitle"
        :description="emptyDescription"
      />

      <slot v-else name="table">
        <el-table v-loading="loading" :data="rows" style="width: 100%">
          <el-table-column
            v-for="column in columns"
            :key="column.prop"
            :prop="column.prop"
            :label="column.label"
            :min-width="column.minWidth || 120"
          />
        </el-table>
      </slot>
    </div>
  </div>
</template>
