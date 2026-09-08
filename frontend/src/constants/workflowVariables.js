// 流程变量引用只允许使用四个稳定命名空间，设计器不得生成裸变量名。
export const WORKFLOW_VARIABLES = Object.freeze({
  INPUT: 'variables.input',
  USER_MESSAGE: 'variables.user_message',
  QUERY: 'variables.query'
})

export const nodeOutputReference = nodeId => `nodes.${nodeId}.output`

