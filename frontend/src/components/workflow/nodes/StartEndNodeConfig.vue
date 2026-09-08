<template>
  <div class="node-editor-fields"><div class="node-help"><strong>{{ descriptor?.label || node.type }}</strong><p>{{ descriptor?.description || '该节点由图契约管理，无额外配置。' }}</p></div></div>
</template>

<script setup>
defineProps({ node: { type: Object, required: true }, descriptor: { type: Object, default: null } })
</script>
