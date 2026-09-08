<template>
  <div class="structured-editor">
    <div v-if="!items.length" class="structured-empty">暂无列表项，请添加第一项。</div>
    <div v-for="(item, index) in items" :key="index" class="structured-inline-row list-row">
      <input :value="displayValue(item)" :placeholder="`第 ${index + 1} 项`" :aria-label="`列表第 ${index + 1} 项`" @input="updateItem(index, $event.target.value)" />
      <div class="row-tools"><button type="button" title="上移列表项" :disabled="index === 0" @click="move(index, -1)">↑</button><button type="button" title="下移列表项" :disabled="index === items.length - 1" @click="move(index, 1)">↓</button><button type="button" title="删除列表项" @click="remove(index)">删除</button></div>
    </div>
    <button type="button" class="secondary-btn" @click="add">+ 添加列表项</button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ modelValue: { type: Array, default: () => [] } })
const emit = defineEmits(['update:modelValue'])
const items = computed(() => props.modelValue)
const displayValue = item => typeof item === 'string' ? item : item?.label || item?.value || ''
const updateItem = (index, value) => { const copy = [...items.value]; copy[index] = value; emit('update:modelValue', copy) }
const add = () => emit('update:modelValue', [...items.value, ''])
const remove = index => emit('update:modelValue', items.value.filter((_, current) => current !== index))
const move = (index, offset) => { const next = index + offset; if (next < 0 || next >= items.value.length) return; const copy = [...items.value]; [copy[index], copy[next]] = [copy[next], copy[index]]; emit('update:modelValue', copy) }
</script>

<style scoped src="./structured-editor.css"></style>
