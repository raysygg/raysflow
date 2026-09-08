<template>
  <div class="structured-editor">
    <div v-if="!items.length" class="structured-empty">暂无赋值关系，请添加来源和目标字段。</div>
    <div v-for="(item, index) in items" :key="index" class="structured-grid assignment-row">
      <label><span>来源</span><select v-model="item.source"><option value="">选择来源</option><option v-for="field in sources" :key="field.value" :value="field.value">{{ field.label }} · {{ field.type || '未知类型' }}</option></select></label>
      <label><span>目标</span><select v-model="item.target"><option value="">选择目标</option><option v-for="field in targets" :key="field.value" :value="field.value">{{ field.label }} · {{ field.type || '未知类型' }}</option></select></label>
      <button type="button" title="删除赋值关系" @click="remove(index)">删除</button>
      <small v-if="item.source && item.target && incompatible(item)" class="structured-error">来源和目标类型不兼容，请先转换数据类型。</small>
    </div>
    <button type="button" class="secondary-btn" @click="add">+ 添加赋值关系</button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ modelValue: { type: Array, default: () => [] }, sources: { type: Array, default: () => [] }, targets: { type: Array, default: () => [] } })
const emit = defineEmits(['update:modelValue'])
const items = computed(() => props.modelValue)
const update = value => emit('update:modelValue', value)
const add = () => update([...items.value, { source: '', target: '' }])
const remove = index => update(items.value.filter((_, current) => current !== index))
const incompatible = item => { const source = props.sources.find(field => field.value === item.source); const target = props.targets.find(field => field.value === item.target); return source?.type && target?.type && source.type !== target.type && source.type !== 'any' && target.type !== 'any' }
</script>

<style scoped src="./structured-editor.css"></style>
