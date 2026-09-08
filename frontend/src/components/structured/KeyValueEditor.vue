<template>
  <div class="structured-editor">
    <div v-if="!items.length" class="structured-empty">暂无配置项，点击下方按钮添加。</div>
    <div v-for="(item, index) in items" :key="index" class="structured-inline-row">
      <input v-model.trim="item.key" placeholder="名称" aria-label="配置名称" />
      <input v-model="item.value" placeholder="值" aria-label="配置值" />
      <button type="button" title="删除配置项" @click="remove(index)">删除</button>
      <small v-if="!item.key || duplicate(item.key, index)" class="structured-error">名称不能为空且不能重复</small>
    </div>
    <button type="button" class="secondary-btn" @click="add">+ 添加配置项</button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ modelValue: { type: Array, default: () => [] } })
const emit = defineEmits(['update:modelValue'])
const items = computed(() => props.modelValue)
const update = value => emit('update:modelValue', value)
const add = () => update([...items.value, { key: '', value: '' }])
const remove = index => update(items.value.filter((_, current) => current !== index))
const duplicate = (key, index) => items.value.some((item, current) => current !== index && item.key === key)
</script>

<style scoped src="./structured-editor.css"></style>
