<template>
  <div class="node-editor-fields">
    <label>Agent</label>
    <select v-model="node.config.agentId" class="text-input">
      <option value="">请选择要调用的 Agent</option>
      <option v-for="agent in agents" :key="agent.id" :value="String(agent.id)">{{ agent.name }}（{{ agent.code }}）</option>
    </select>
    <small v-if="agents.length === 0" class="field-help">当前租户暂无可用 Agent。</small>
    <small class="field-help">保存稳定的 Agent ID，不使用名称或编码拼接引用。</small>
  </div>
</template>

<script setup>
defineProps({ node: { type: Object, required: true }, agents: { type: Array, default: () => [] } })
</script>
