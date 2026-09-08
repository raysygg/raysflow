// 应用产品主模型的前端常量，工作区、目录和创建向导统一使用这里的展示定义。
export const WORKSPACE_TABS = Object.freeze([
  { key: 'runtime', label: '概览', hint: '查看应用状态和下一步' },
  { key: 'flow', label: '构建', hint: '配置运行流程和入口' },
  { key: 'knowledge', label: '知识', hint: '配置知识检索' },
  { key: 'test', label: '测试', hint: '验证真实输入' },
  { key: 'release', label: '发布', hint: '通过质量门禁' },
  { key: 'diagnostics', label: '运营', hint: '查看运行记录和健康状态' }
])

export const APPLICATION_STATUS_META = Object.freeze({
  ONLINE: { label: '可运行', tone: 'success', health: '运行正常' },
  ACTIVE: { label: '可运行', tone: 'success', health: '运行正常' },
  DRAFT: { label: '草稿', tone: 'warning', health: '等待发布' }
})
