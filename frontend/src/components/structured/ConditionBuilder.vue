<template>
  <div class="structured-editor">
    <div v-if="!items.length" class="structured-empty">暂无条件，添加条件后可预览分支规则。</div>
    <div v-for="(item, index) in items" :key="index" class="structured-grid condition-row">
      <select v-model="item.field" aria-label="条件字段"><option value="">选择字段</option><option v-for="field in fields" :key="field.value" :value="field.value">{{ field.label }}</option></select>
      <select v-model="item.operator" aria-label="条件运算符"><option v-for="operator in availableOperators" :key="operator.value" :value="operator.value">{{ operator.label }}</option></select>
      <input v-model="item.value" aria-label="条件值" placeholder="比较值" />
      <button type="button" title="删除条件" @click="remove(index)">删除</button>
    </div>
    <p class="condition-preview">规则预览：{{ preview || '请先选择字段和运算符' }}</p>
    <button v-if="items.length < maxItems" type="button" class="secondary-btn" @click="add">+ 添加条件</button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ modelValue: { type: Array, default: () => [] }, fields: { type: Array, default: () => [] }, operatorValues: { type: Array, default: () => [] }, maxItems: { type: Number, default: Number.MAX_SAFE_INTEGER } })
const emit = defineEmits(['update:modelValue'])
const operators = [{ value: 'equals', label: '等于' }, { value: 'not_equals', label: '不等于' }, { value: 'contains', label: '包含' }, { value: 'gt', label: '大于' }, { value: 'gte', label: '大于等于' }, { value: 'lt', label: '小于' }, { value: 'lte', label: '小于等于' }, { value: 'exists', label: '已填写' }]
const items = computed(() => props.modelValue)
const availableOperators = computed(() => props.operatorValues.length ? operators.filter(operator => props.operatorValues.includes(operator.value)) : operators)
const update = value => emit('update:modelValue', value)
const add = () => update([...items.value, { field: '', operator: availableOperators.value[0]?.value || 'equals', value: '' }])
const remove = index => update(items.value.filter((_, current) => current !== index))
const preview = computed(() => items.value.filter(item => item.field).map(item => `${props.fields.find(field => field.value === item.field)?.label || item.field} ${availableOperators.value.find(operator => operator.value === item.operator)?.label || item.operator}${item.operator === 'exists' ? '' : ` ${item.value || '待填写'}`}`).join(' 且 '))
</script>

<style scoped src="./structured-editor.css"></style>
