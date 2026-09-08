import { reactive } from 'vue'

/** 全局 Toast 提示通知状态 */
export const feedbackState = reactive({ items: [] })

/** 全局确认对话框状态 */
export const dialogState = reactive({ current: null })

/** 全局文本输入对话框状态 */
export const promptState = reactive({ current: null })

/**
 * 触发全局轻量通知 Toast。
 *
 * @param {string} message 提示文案内容
 * @param {'info'|'success'|'warning'|'danger'} [tone='info'] 提示类型颜色
 * @param {number} [duration=3600] 自动消失时间（毫秒）
 */
export const notify = (message, tone = 'info', duration = 3600) => {
  const id = `${Date.now()}-${Math.random()}`
  feedbackState.items.push({ id, message, tone })
  window.setTimeout(() => dismiss(id), duration)
}

/**
 * 手动关闭指定 ID 的 Toast 消息。
 *
 * @param {string} id 消息唯一 ID
 */
export const dismiss = (id) => {
  feedbackState.items = feedbackState.items.filter(item => item.id !== id)
}

/**
 * 统一站内确认对话框入口。
 * 调用方传入标题与描述，返回异步 Promise 指示用户点击了确认还是取消。
 *
 * @param {Object} options 提示配置
 * @param {string} [options.title='请确认操作'] 对话框标题
 * @param {string} [options.message=''] 详细说明文案
 * @param {string} [options.confirmText='确认'] 确认按钮文字
 * @param {string} [options.cancelText='取消'] 取消按钮文字
 * @param {'info'|'warning'|'danger'} [options.tone='danger'] 操作警示类型
 * @returns {Promise<boolean>} 用户确认返回 true，关闭/取消返回 false
 */
export const confirmAction = ({
  title = '请确认操作',
  message = '',
  confirmText = '确认',
  cancelText = '取消',
  tone = 'danger'
} = {}) => new Promise(resolve => {
  // 同一时间只允许一个确认任务，避免并发操作让前一个 Promise 永远悬挂。
  if (dialogState.current) dialogState.current.resolve(false)
  dialogState.current = { title, message, confirmText, cancelText, tone, resolve }
})

/**
 * 响应确认对话框的操作结果。
 *
 * @param {boolean} confirmed 用户是否点击确认
 */
export const resolveDialog = (confirmed) => {
  const current = dialogState.current
  if (!current) return
  dialogState.current = null
  current.resolve(Boolean(confirmed))
}

/**
 * 统一站内单行文本输入对话框入口。
 *
 * @param {Object} options 输入配置
 * @param {string} [options.title='请输入内容'] 对话框标题
 * @param {string} [options.message=''] 输入提示信息
 * @param {string} [options.placeholder=''] 输入框占位符
 * @param {string} [options.initialValue=''] 初始默认值
 * @param {string} [options.confirmText='保存'] 确认按钮文字
 * @returns {Promise<string|null>} 返回用户输入的字符串，若取消则返回 null
 */
export const promptAction = ({
  title = '请输入内容',
  message = '',
  placeholder = '',
  initialValue = '',
  confirmText = '保存'
} = {}) => new Promise(resolve => {
  if (promptState.current) promptState.current.resolve(null)
  promptState.current = { title, message, placeholder, value: initialValue, confirmText, resolve }
})

/**
 * 响应文本输入对话框的操作结果。
 *
 * @param {string|null} value 输入框中的最终值或 null
 */
export const resolvePrompt = (value) => {
  const current = promptState.current
  if (!current) return
  promptState.current = null
  current.resolve(value)
}

