<template>
  <span class="shared-status-badge" :class="`status-${tone}`" :aria-label="label">{{ label }}</span>
</template>

<script setup>
defineProps({
  label: { type: String, default: '未知' },
  tone: { type: String, default: 'neutral' }
})
</script>

<style scoped>
.shared-status-badge { display: inline-flex; align-items: center; min-height: 22px; padding: 2px 8px; border: 1px solid currentColor; border-radius: var(--radius-sm, 5px); font-size: 11px; font-weight: 700; white-space: nowrap; }
.status-success { color: var(--success); background: var(--success-soft, #effaf4); }
.status-warning { color: var(--warning); background: var(--warning-soft, #fff8e8); }
.status-danger { color: var(--danger); background: var(--danger-soft, #fff3f3); }
.status-info { color: var(--accent); background: var(--accent-soft); }
.status-neutral { color: var(--muted); background: var(--panel-muted); }
</style>
