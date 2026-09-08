<template>
  <div class="resource-select">
    <select :value="modelValue || ''" :disabled="disabled || !options.length" @change="$emit('update:modelValue', $event.target.value || null)">
      <option value="">{{ options.length ? placeholder : `暂无${resourceLabel}，请先创建` }}</option>
      <option v-for="option in options" :key="option.value" :value="option.value">{{ option.label }}{{ option.status ? ` · ${option.status}` : '' }}</option>
    </select>
    <div v-if="createLabel || manageLabel" class="actions-row">
      <button v-if="createLabel" type="button" class="link-btn" @click="$emit('create')">+ {{ createLabel }}</button>
      <button v-if="manageLabel" type="button" class="link-btn secondary" @click="$emit('manage')">{{ manageLabel }}</button>
    </div>
    <small v-if="help">{{ help }}</small>
  </div>
</template>

<script setup>
defineProps({
  modelValue: { type: [String, Number], default: '' },
  options: { type: Array, default: () => [] },
  resourceLabel: { type: String, default: '资源' },
  placeholder: { type: String, default: '请选择' },
  createLabel: { type: String, default: '' },
  manageLabel: { type: String, default: '' },
  help: { type: String, default: '' },
  disabled: { type: Boolean, default: false }
})
defineEmits(['update:modelValue', 'create', 'manage'])
</script>

<style scoped>
.resource-select { display: grid; gap: 6px; min-width: 0; }
.resource-select select { width: 100%; min-height: var(--control-height); padding: 8px 10px; border: 1px solid var(--line); border-radius: var(--radius-sm); background: var(--panel); color: var(--text); font: inherit; }
.actions-row { display: flex; align-items: center; gap: 12px; margin-top: 2px; }
.link-btn { width: max-content; padding: 0; border: 0; background: transparent; color: var(--accent); cursor: pointer; font-size: 12px; }
.link-btn.secondary { color: var(--muted); }
.link-btn:hover { text-decoration: underline; }
.resource-select small { color: var(--muted); font-size: 11px; }
</style>

