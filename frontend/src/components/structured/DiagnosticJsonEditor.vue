<template>
  <details class="advanced-diagnostic">
    <summary>开发者诊断：原始配置 JSON</summary>
    <textarea v-model="raw" :aria-invalid="Boolean(error)" spellcheck="false" placeholder="仅用于排查问题，普通配置请使用上方结构化控件。" />
    <div class="diagnostic-actions"><button type="button" class="secondary-btn" @click="format">格式化并校验</button><small v-if="error" class="structured-error">{{ error }}</small><small v-else>结构化值与诊断 JSON 保持同步。</small></div>
  </details>
</template>

<script setup>
import { ref, watch } from 'vue'
const props = defineProps({ modelValue: { type: [Object, Array], default: () => ({}) } })
const emit = defineEmits(['update:modelValue', 'invalid'])
const raw = ref(JSON.stringify(props.modelValue, null, 2)); const error = ref('')
watch(() => props.modelValue, value => { if (!error.value) raw.value = JSON.stringify(value, null, 2) }, { deep: true })
const format = () => { try { const parsed = JSON.parse(raw.value); error.value = ''; raw.value = JSON.stringify(parsed, null, 2); emit('update:modelValue', parsed) } catch (cause) { error.value = `JSON 格式不正确：${cause.message}`; emit('invalid', error.value) } }
</script>

<style scoped src="./structured-editor.css"></style>
