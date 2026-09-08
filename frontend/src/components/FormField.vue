<template>
  <label class="workbench-field" :class="{ wide }">
    <span class="field-label">{{ label }}<b v-if="required">必填</b></span>
    <slot />
    <small v-if="help" class="field-help">{{ help }}</small>
    <small v-if="error" class="field-error" role="alert">{{ error }}</small>
  </label>
</template>

<script setup>
defineProps({
  label: { type: String, required: true },
  help: { type: String, default: '' },
  error: { type: String, default: '' },
  required: { type: Boolean, default: false },
  wide: { type: Boolean, default: false }
})
</script>

<style scoped>
.workbench-field { display:grid; gap:6px; min-width:0; }.workbench-field.wide { grid-column:1 / -1; }.field-label { color:var(--text); font-size:12px; font-weight:650; }.field-label b { margin-left:6px; color:var(--danger); font-size:10px; font-weight:500; }.field-help,.field-error { color:var(--muted); font-size:10px; line-height:1.5; }.field-error { color:var(--danger); }
.workbench-field :slotted(input),.workbench-field :slotted(select),.workbench-field :slotted(textarea) { width:100%; min-height:var(--control-height); padding:8px 10px; border:1px solid var(--line); border-radius:var(--radius-sm); background:var(--panel); color:var(--text); font:inherit; }.workbench-field :slotted(textarea) { min-height:84px; resize:vertical; }.workbench-field :slotted(input:focus),.workbench-field :slotted(select:focus),.workbench-field :slotted(textarea:focus) { border-color:var(--accent); box-shadow:var(--focus-ring); outline:0; }
</style>
