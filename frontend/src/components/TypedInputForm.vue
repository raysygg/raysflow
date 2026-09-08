<template>
  <div class="typed-input-form">
    <div v-for="field in fields" :key="field.name" class="typed-field">
      <span>{{ fieldLabel(field) }}<b v-if="field.required">必填</b></span>
      <select v-if="isChoice(field)" :value="value(field)" @change="setValue(field, $event.target.value)">
        <option value="">请选择{{ fieldLabel(field) }}</option>
        <option v-for="option in choices(field)" :key="String(option.value)" :value="option.value">{{ option.label }}</option>
      </select>
      <label v-else-if="field.type === 'boolean'" class="boolean-control">
        <input :checked="Boolean(value(field))" type="checkbox" @change="setValue(field, $event.target.checked)" />
        <span>{{ value(field) ? '是' : '否' }}</span>
      </label>
      <input v-else-if="field.type === 'number' || field.type === 'integer'" :value="value(field)" type="number" @input="setValue(field, numberValue(field, $event.target.value))" />
      <textarea v-else-if="field.multiline" :value="value(field)" rows="4" :placeholder="field.description || `请输入${fieldLabel(field)}`" @input="setValue(field, $event.target.value)" />
      <input v-else :value="value(field)" type="text" :placeholder="field.description || `请输入${fieldLabel(field)}`" @input="setValue(field, $event.target.value)" />
      <small v-if="field.description">{{ field.description }}</small>
      <small v-if="errors[field.name]" class="field-error">{{ errors[field.name] }}</small>
    </div>
    <p v-if="fields.length === 0" class="empty-contract">当前应用尚未定义输入字段，请先返回流程设计完善输入契约。</p>
  </div>
</template>

<script setup>
import { reactive, watch } from 'vue'

const props = defineProps({
  fields: { type: Array, default: () => [] },
  modelValue: { type: Object, default: () => ({}) },
  resources: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue'])
const errors = reactive({})

const fieldLabel = field => field.label || field.name || '输入内容'
const value = field => props.modelValue?.[field.name] ?? (field.type === 'boolean' ? false : '')
const isChoice = field => Boolean(field.resourceType) || field.type === 'enum' || (field.options?.length || 0) > 0
const resourceLabel = item => item.label || item.name || item.title || item.modelName || item.orgName || String(item.id ?? item.value)
const choices = field => {
  if (field.resourceType) return (props.resources[field.resourceType] || []).map(item => ({ value: item.id ?? item.value, label: resourceLabel(item) }))
  return (field.options || []).map(option => typeof option === 'object' ? option : { value: option, label: option })
}
const numberValue = (field, raw) => raw === '' ? '' : field.type === 'integer' ? Number.parseInt(raw, 10) : Number(raw)
const setValue = (field, nextValue) => {
  emit('update:modelValue', { ...(props.modelValue || {}), [field.name]: nextValue })
  if (!blank(nextValue)) delete errors[field.name]
}
const blank = value => value === undefined || value === null || value === ''
const validate = () => {
  Object.keys(errors).forEach(key => delete errors[key])
  props.fields.forEach(field => {
    if (field.required && blank(props.modelValue?.[field.name])) errors[field.name] = `请填写${fieldLabel(field)}`
  })
  return Object.keys(errors).length === 0
}
const payload = () => Object.fromEntries(props.fields.map(field => [field.name, value(field)]))

watch(() => props.fields, fields => {
  const next = { ...(props.modelValue || {}) }
  fields.forEach(field => {
    if (field.type === 'boolean' && next[field.name] === undefined) next[field.name] = false
  })
  emit('update:modelValue', next)
}, { immediate: true, deep: true })

defineExpose({ validate, payload })
</script>

<style scoped>
.typed-input-form { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:14px; }
.typed-field { display:grid; align-content:start; gap:7px; min-width:0; }
.typed-field > span { display:flex; align-items:center; gap:7px; color:var(--muted); font-size:12px; }
.typed-field b { color:var(--danger); font-size:10px; font-weight:500; }
.typed-field input,.typed-field select,.typed-field textarea { width:100%; min-width:0; border:1px solid var(--line); border-radius:7px; padding:10px 12px; background:var(--panel-muted); color:var(--text); font:inherit; }
.typed-field textarea { resize:vertical; }
.typed-field small { color:var(--muted); font-size:10px; line-height:1.5; }
.typed-field .field-error { color:var(--danger); }
.boolean-control { display:flex; align-items:center; gap:8px; min-height:42px; padding:9px 11px; border:1px solid var(--line); border-radius:7px; background:var(--panel-muted); }
.boolean-control input { width:auto; padding:0; }
.empty-contract { grid-column:1 / -1; margin:0; padding:14px; border:1px solid #f0d7a9; border-radius:7px; background:#fff9ec; color:#8b601a; line-height:1.6; }
@media (max-width:680px) { .typed-input-form { grid-template-columns:1fr; } }
</style>
