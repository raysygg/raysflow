<template>
  <Teleport to="body">
    <div v-if="promptState.current" class="prompt-overlay" @click.self="close">
      <form ref="dialogRef" class="prompt-dialog" role="dialog" aria-modal="true" aria-labelledby="prompt-dialog-title" @submit.prevent="submit" @keydown.esc="close">
        <h3 id="prompt-dialog-title">{{ promptState.current.title }}</h3>
        <p>{{ promptState.current.message }}</p>
        <input ref="inputRef" v-model="promptState.current.value" class="text-input" :placeholder="promptState.current.placeholder" autocomplete="off" />
        <div class="prompt-actions"><button type="button" class="ghost-btn" @click="close">取消</button><button type="submit" class="primary-btn">{{ promptState.current.confirmText }}</button></div>
      </form>
    </div>
  </Teleport>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { promptState, resolvePrompt } from '../utils/feedback'
const dialogRef = ref(null); const inputRef = ref(null)
const close = () => resolvePrompt(null)
const submit = () => resolvePrompt(promptState.current?.value ?? '')
watch(() => promptState.current, async current => { if (!current) return; await nextTick(); inputRef.value?.focus() }, { flush: 'post' })
const handleEscape = event => { if (event.key === 'Escape' && promptState.current) close() }
onMounted(() => document.addEventListener('keydown', handleEscape)); onUnmounted(() => document.removeEventListener('keydown', handleEscape))
</script>

<style scoped>
.prompt-overlay { position: fixed; inset: 0; z-index: 2101; display: grid; place-items: center; padding: 20px; background: rgba(16,34,56,.42); }.prompt-dialog { width: min(440px, calc(100vw - 32px)); padding: 22px; border: 1px solid var(--line); border-radius: 8px; background: #fff; box-shadow: 0 18px 50px rgba(20,42,66,.2); }.prompt-dialog h3 { margin: 0 0 8px; color: var(--text); font-size: 16px; }.prompt-dialog p { margin: 0 0 14px; color: var(--muted); font-size: 12px; line-height: 1.6; }.prompt-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
</style>
