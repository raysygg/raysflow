<template>
  <div class="workbench-tabs" role="tablist" :aria-label="label">
    <button v-for="item in items" :key="item.value" type="button" role="tab" :tabindex="modelValue === item.value ? 0 : -1" :aria-selected="modelValue === item.value" :class="{ active: modelValue === item.value }" @click="$emit('update:modelValue', item.value)" @keydown="moveFocus($event, item.value)">
      <span>{{ item.label }}</span><small v-if="item.count !== undefined">{{ item.count }}</small>
    </button>
  </div>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: String, required: true },
  items: { type: Array, default: () => [] },
  label: { type: String, default: '页面分区' }
})
const emit = defineEmits(['update:modelValue'])
const moveFocus = (event, currentValue) => {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return
  event.preventDefault()
  const buttons = [...event.currentTarget.parentElement.querySelectorAll('[role="tab"]')]
  const current = props.items.findIndex(item => item.value === currentValue)
  const target = event.key === 'Home' ? 0 : event.key === 'End' ? buttons.length - 1 : (current + (event.key === 'ArrowRight' ? 1 : -1) + buttons.length) % buttons.length
  buttons[target]?.focus()
  emit('update:modelValue', props.items[target]?.value)
}
</script>

<style scoped>
.workbench-tabs { display:flex; gap:2px; min-height:var(--control-height); padding:3px; border:1px solid var(--line); background:var(--panel-muted); overflow:auto; }
.workbench-tabs button { display:inline-flex; align-items:center; justify-content:center; gap:6px; min-width:92px; min-height:var(--control-height-sm); padding:0 12px; border:1px solid transparent; background:transparent; color:var(--muted); font-size:12px; cursor:pointer; white-space:nowrap; }.workbench-tabs button:hover { color:var(--text); }.workbench-tabs button.active { border-color:var(--line); background:var(--panel); color:var(--text); font-weight:700; box-shadow:var(--shadow); }.workbench-tabs small { color:var(--muted); font-size:10px; }
</style>
