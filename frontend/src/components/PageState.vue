<template>
  <div
    class="shared-page-state"
    :class="[`state-${type}`, { 'has-details': details.length }]"
    :role="type === 'error' || type === 'forbidden' ? 'alert' : 'status'"
    :aria-live="type === 'error' || type === 'forbidden' ? 'assertive' : 'polite'"
  >
    <span class="state-mark" aria-hidden="true">{{ stateMark }}</span>
    <div class="state-copy">
      <strong v-if="title">{{ title }}</strong>
      <span v-if="message">{{ message }}</span>
      <ul v-if="details.length" class="state-details">
        <li v-for="item in details" :key="item">{{ item }}</li>
      </ul>
    </div>
    <button v-if="actionLabel" type="button" class="ghost-btn compact-btn" :disabled="actionDisabled" @click="$emit('action')">
      {{ actionLabel }}
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: { type: String, default: 'empty' },
  title: { type: String, default: '' },
  message: { type: String, default: '' },
  actionLabel: { type: String, default: '' },
  actionDisabled: { type: Boolean, default: false },
  details: { type: Array, default: () => [] },
})

defineEmits(['action'])

const stateMark = computed(() => ({
  loading: '…',
  processing: '…',
  empty: '—',
  forbidden: '!',
  partial: '!',
  error: '!',
  ready: '✓',
}[props.type] || '—'))
</script>

<style scoped>
.shared-page-state { display: flex; align-items: center; justify-content: center; gap: 10px; min-height: 80px; padding: 16px; border: 1px dashed var(--line); border-radius: var(--radius); color: var(--muted); font-size: 12px; }
.state-mark { display: grid; flex: 0 0 24px; place-items: center; width: 24px; height: 24px; border-radius: 50%; background: var(--panel-muted); color: var(--accent); font-weight: 800; }
.state-copy { display: grid; min-width: 0; gap: 4px; }
.state-copy strong { color: var(--text); }
.state-details { margin: 0; padding-left: 16px; color: var(--muted); }
.state-error, .state-forbidden { border-color: #efcfd2; background: #fff8f8; color: var(--danger); }
.state-error .state-mark, .state-forbidden .state-mark { background: #fee2e2; color: var(--danger); }
.state-partial { border-color: #ead9a8; background: #fffaf0; color: #8b691b; }
.state-partial .state-mark { background: #fef3c7; color: #8b691b; }
.state-processing, .state-loading { border-color: #b8dbe3; background: #f5fbfc; }
.state-processing .state-mark::after, .state-loading .state-mark::after { width: 12px; height: 12px; border: 2px solid var(--line); border-top-color: var(--accent); border-radius: 50%; animation: shared-spin .8s linear infinite; content: ''; }
@keyframes shared-spin { to { transform: rotate(360deg); } }
</style>
