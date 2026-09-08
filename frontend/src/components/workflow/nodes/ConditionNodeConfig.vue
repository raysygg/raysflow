<template>
  <div class="node-editor-fields">
    <div class="node-help"><strong>条件分支</strong><p>根据输入匹配规则选择“满足”和“不满足”两条路径。</p></div>
    <label>判断操作</label>
    <select v-model="node.config.operator" class="text-input">
      <option value="CONTAINS">输入包含文本</option>
      <option value="EQUALS">输入完全相等</option>
      <option value="NOT_EMPTY">输入不为空</option>
    </select>
    <label v-if="node.config.operator !== 'NOT_EMPTY'">匹配文本</label>
    <input v-if="node.config.operator !== 'NOT_EMPTY'" v-model.trim="node.config.value" class="text-input" autocomplete="off" spellcheck="false" placeholder="填写判断文本" />
    <label>满足时后继节点（是）</label>
    <select v-model="node.config.trueTarget" class="text-input"><option value="">请选择节点</option><option v-for="candidate in candidates" :key="candidate.id" :value="candidate.id">{{ candidate.name }}</option></select>
    <label>不满足时后继节点（否）</label>
    <select v-model="node.config.falseTarget" class="text-input"><option value="">请选择节点</option><option v-for="candidate in candidates" :key="candidate.id" :value="candidate.id">{{ candidate.name }}</option></select>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ node: { type: Object, required: true }, nodes: { type: Array, default: () => [] } })
const candidates = computed(() => props.nodes.filter(item => item.id !== props.node.id))
</script>
