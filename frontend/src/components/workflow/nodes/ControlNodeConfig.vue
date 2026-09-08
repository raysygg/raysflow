<template>
  <div class="node-editor-fields">
    <div class="node-help"><strong>{{ descriptor?.label || node.type }}</strong><p>{{ descriptor?.description || '该控制节点无需额外配置。' }}</p></div>
    <small class="field-help">该节点的拓扑行为由后端节点类型枚举和发布校验决定。</small>
  </div>
</template>

<script setup>
defineProps({ node: { type: Object, required: true }, descriptor: { type: Object, default: null } })
</script>
