<template>
  <Teleport to="body">
    <div v-if="dialogState.current" class="confirm-overlay" @click.self="close">
      <section ref="dialogRef" class="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="confirm-dialog-title" tabindex="-1" @keydown.esc="close">
        <div class="confirm-icon" :class="`confirm-${dialogState.current.tone}`" aria-hidden="true">!</div>
        <div class="confirm-copy">
          <h3 id="confirm-dialog-title">{{ dialogState.current.title }}</h3>
          <p>{{ dialogState.current.message }}</p>
        </div>
        <div class="confirm-actions">
          <button ref="cancelRef" type="button" class="ghost-btn" @click="close">{{ dialogState.current.cancelText }}</button>
          <button type="button" class="primary-btn" :class="{ 'danger-confirm-btn': dialogState.current.tone === 'danger' }" @click="confirm">{{ dialogState.current.confirmText }}</button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { dialogState, resolveDialog } from '../utils/feedback'

const dialogRef = ref(null)
const cancelRef = ref(null)
const close = () => resolveDialog(false)
const confirm = () => resolveDialog(true)
watch(() => dialogState.current, async current => {
  if (!current) return
  await nextTick()
  dialogRef.value?.focus()
  cancelRef.value?.focus()
}, { flush: 'post' })
const handleEscape = event => { if (event.key === 'Escape' && dialogState.current) close() }
onMounted(() => document.addEventListener('keydown', handleEscape))
onUnmounted(() => document.removeEventListener('keydown', handleEscape))
</script>

<style scoped>
.confirm-overlay { position: fixed; inset: 0; z-index: 2100; display: grid; place-items: center; padding: 20px; background: rgba(16, 34, 56, .42); }
.confirm-dialog { display: grid; grid-template-columns: auto 1fr; gap: 14px; width: min(440px, calc(100vw - 32px)); padding: 22px; border: 1px solid var(--line); border-radius: 8px; background: #fff; box-shadow: 0 18px 50px rgba(20, 42, 66, .2); outline: none; }
.confirm-icon { display: grid; place-items: center; width: 32px; height: 32px; border-radius: 50%; background: var(--danger-soft); color: var(--danger); font-weight: 800; }
.confirm-warning { background: var(--warning-soft); color: var(--warning); }
.confirm-copy h3 { margin: 2px 0 7px; color: var(--text); font-size: 16px; }.confirm-copy p { margin: 0; color: var(--muted); font-size: 12px; line-height: 1.7; }
.confirm-actions { grid-column: 1 / -1; display: flex; justify-content: flex-end; gap: 8px; margin-top: 5px; }.danger-confirm-btn { border-color: var(--danger); background: var(--danger); }.danger-confirm-btn:hover { background: #a93640; }
</style>
