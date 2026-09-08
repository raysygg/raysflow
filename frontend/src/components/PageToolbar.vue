<template>
  <header class="shared-page-toolbar">
    <div><div v-if="eyebrow" class="eyebrow">{{ eyebrow }}</div><h3>{{ title }}</h3><p v-if="description">{{ description }}</p></div>
    <div class="shared-page-toolbar-actions"><slot name="actions" /></div>
  </header>
</template>

<script setup>
defineProps({
  eyebrow: { type: String, default: '' },
  title: { type: String, required: true },
  description: { type: String, default: '' }
})
</script>

<style scoped>
.shared-page-toolbar { display: flex; align-items: flex-start; justify-content: space-between; gap: var(--space-4, 16px); margin-bottom: var(--space-4, 16px); }
.shared-page-toolbar h3 { margin: 0; color: var(--text); font-size: 18px; }
.shared-page-toolbar p { margin: 5px 0 0; color: var(--muted); font-size: 12px; }
.shared-page-toolbar-actions { display: flex; gap: 8px; flex-wrap: wrap; }
@media (max-width: 680px) { .shared-page-toolbar { flex-direction: column; } }
</style>
