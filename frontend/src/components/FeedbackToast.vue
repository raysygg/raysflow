<template>
  <div class="feedback-stack" aria-live="polite" aria-atomic="true">
    <div v-for="item in feedbackState.items" :key="item.id" class="feedback-toast" :class="`feedback-${item.tone}`">
      <span>{{ item.message }}</span>
      <button type="button" title="关闭提示" aria-label="关闭提示" @click="dismiss(item.id)">×</button>
    </div>
  </div>
</template>

<script setup>
import { feedbackState, dismiss } from '../utils/feedback'
</script>

<style scoped>
.feedback-stack { position: fixed; right: 24px; bottom: 24px; z-index: 2000; display: flex; width: min(380px, calc(100vw - 32px)); flex-direction: column; gap: 8px; pointer-events: none; }
.feedback-toast { display: flex; align-items: flex-start; gap: 12px; padding: 12px 13px; border: 1px solid #d9e4ed; border-left: 3px solid #5c91bd; border-radius: 8px; background: #fff; box-shadow: 0 12px 32px rgba(24, 49, 75, .16); color: #3d566d; font-size: 12px; line-height: 1.55; pointer-events: auto; }
.feedback-toast span { min-width: 0; flex: 1; }
.feedback-toast button { flex: 0 0 auto; border: 0; background: transparent; color: #8495a4; cursor: pointer; font-size: 18px; line-height: 1; }
.feedback-success { border-left-color: #2c9a69; background: #fbfffc; }
.feedback-success button { color: #27845b; }
.feedback-warning { border-left-color: #c58a2c; background: #fffdf8; }
.feedback-warning button { color: #a77421; }
.feedback-error { border-left-color: #c94b54; background: #fffafa; }
.feedback-error button { color: #ad3f48; }
@media (max-width: 580px) { .feedback-stack { right: 16px; bottom: 16px; } }
</style>
