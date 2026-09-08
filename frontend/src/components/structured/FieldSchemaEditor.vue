<template>
  <div class="structured-editor">
    <div v-if="!items.length" class="structured-empty">还没有字段，请添加第一个业务字段。</div>
    <article v-for="(item, index) in items" :key="item.key || index" class="structured-row">
      <div class="structured-row-head"><strong>字段 {{ index + 1 }}</strong><div class="row-tools"><button type="button" title="上移字段" :disabled="index === 0" @click="move(index, -1)">↑</button><button type="button" title="下移字段" :disabled="index === items.length - 1" @click="move(index, 1)">↓</button><button type="button" title="删除字段" @click="remove(index)">删除</button></div></div>
      <div class="structured-grid">
        <label><span>字段名</span><input v-model.trim="item.key" required placeholder="例如：问题内容" /></label>
        <label><span>数据类型</span><select v-model="item.type"><option v-for="type in types" :key="type.value" :value="type.value">{{ type.label }}</option></select></label>
        <label class="wide"><span>说明</span><input v-model.trim="item.description" placeholder="告诉使用者应该填写什么" /></label>
        <label class="check-line"><input v-model="item.required" type="checkbox" /> 必填字段</label>
      </div>
      <p v-if="errors[index]" class="structured-error">{{ errors[index] }}</p>
    </article>
    <button type="button" class="secondary-btn" @click="add">+ 添加字段</button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ modelValue: { type: Array, default: () => [] } })
const emit = defineEmits(['update:modelValue'])
const types = [{ value: 'string', label: '文本' }, { value: 'number', label: '数字' }, { value: 'integer', label: '整数' }, { value: 'boolean', label: '开关' }, { value: 'enum', label: '选项' }, { value: 'resource', label: '资源' }]
const items = computed(() => props.modelValue)
const errors = computed(() => items.value.map((item, index) => !item.key ? `字段 ${index + 1} 需要填写名称。` : items.value.some((other, otherIndex) => otherIndex !== index && other.key === item.key) ? '字段名不能重复。' : ''))
const update = value => emit('update:modelValue', value)
const add = () => update([...items.value, { key: '', type: 'string', description: '', required: false }])
const remove = index => update(items.value.filter((_, current) => current !== index))
const move = (index, offset) => { const next = index + offset; if (next < 0 || next >= items.value.length) return; const copy = [...items.value]; [copy[index], copy[next]] = [copy[next], copy[index]]; update(copy) }
</script>

<style scoped src="./structured-editor.css"></style>
