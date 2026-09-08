<template>
  <div class="node-editor-fields">
    <div class="node-help"><strong>人工审批</strong><p>流程到达此节点后，审批组中的用户会收到待办。</p></div>
    <label>审批标题</label><input v-model.trim="node.config.approvalTitle" class="text-input" autocomplete="off" spellcheck="false" placeholder="例如：经理审批候选人录用申请" />
    <label>审批说明</label><textarea v-model.trim="node.config.approvalDescription" class="textarea-input" autocomplete="off" spellcheck="false" placeholder="说明审批人需要重点判断的业务事项" />
    <label>审批组</label>
    <select v-model="node.config.approvalGroupId" class="text-input"><option value="">请选择负责团队</option><option v-for="unit in organizationUnits" :key="unit.id" :value="String(unit.id)">{{ unit.orgName }}</option></select>
    <small v-if="organizationUnits.length === 0" class="field-help">当前租户暂无可用审批组或组织权限已被撤销。</small>
    <label>风险级别</label>
    <select v-model="node.config.riskLevel" class="text-input"><option value="LOW">低风险</option><option value="MEDIUM">中风险</option><option value="HIGH">高风险</option></select>
  </div>
</template>

<script setup>
defineProps({ node: { type: Object, required: true }, organizationUnits: { type: Array, default: () => [] } })
</script>
