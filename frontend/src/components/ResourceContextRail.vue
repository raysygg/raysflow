<template>
  <section class="resource-context-rail" :aria-label="label || '当前资源上下文'">
    <div class="context-resource">
      <span class="context-mark" aria-hidden="true">{{ mark }}</span>
      <div class="context-copy">
        <small>{{ label || '当前资源' }}</small>
        <strong>{{ name || '未选择资源' }}</strong>
      </div>
    </div>
    <div class="context-facts">
      <span v-if="state" class="context-state" :class="`tone-${tone}`">{{ state }}</span>
      <span v-if="blockingCount > 0" class="context-blocking">{{ blockingCount }} 个待处理问题</span>
      <span v-else-if="hint" class="context-hint">{{ hint }}</span>
    </div>
    <slot name="action">
      <button v-if="actionLabel" type="button" class="context-action" @click="$emit('action')">{{ actionLabel }}</button>
    </slot>
  </section>
</template>

<script setup>
defineProps({
  label: { type: String, default: '当前资源' },
  name: { type: String, default: '' },
  state: { type: String, default: '' },
  tone: { type: String, default: 'info' },
  blockingCount: { type: Number, default: 0 },
  hint: { type: String, default: '' },
  actionLabel: { type: String, default: '' },
  mark: { type: String, default: 'R' }
})

defineEmits(['action'])
</script>

<style scoped>
.resource-context-rail { display:flex; align-items:center; justify-content:space-between; gap:14px; min-width:0; margin:0 0 var(--space-4); padding:10px 12px; border:1px solid var(--line); background:var(--panel); }
.context-resource,.context-facts { display:flex; align-items:center; gap:9px; min-width:0; }
.context-mark { display:grid; flex:0 0 26px; place-items:center; width:26px; height:26px; border-radius:var(--radius-sm); background:var(--accent-soft); color:var(--accent); font-size:11px; font-weight:800; }
.context-copy { display:grid; min-width:0; gap:2px; }.context-copy small,.context-facts span { color:var(--muted); font-size:10px; }.context-copy strong { overflow:hidden; color:var(--text); font-size:12px; text-overflow:ellipsis; white-space:nowrap; }
.context-state { padding:3px 7px; border:1px solid currentColor; border-radius:var(--radius-sm); }.tone-success { color:var(--success)!important; background:var(--success-soft); }.tone-warning { color:var(--warning)!important; background:var(--warning-soft); }.tone-danger { color:var(--danger)!important; background:var(--danger-soft); }.tone-info { color:var(--accent)!important; background:var(--accent-soft); }.context-blocking { color:var(--danger)!important; }.context-action { flex:0 0 auto; min-height:var(--control-height-sm); padding:0 10px; border:1px solid var(--accent); background:var(--accent); color:#fff; font-size:11px; cursor:pointer; }.context-action:hover { background:var(--accent-deep); }
@media(max-width:680px) { .resource-context-rail { align-items:flex-start; flex-wrap:wrap; }.context-facts { flex:1 1 100%; padding-left:35px; }.context-action { margin-left:auto; } }
</style>
