/**
 * 前端运行时地址配置。
 * 同域部署使用相对路径；前后端不同域名时，通过 Vite 环境变量覆盖地址。
 */
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
const defaultWsBaseUrl = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws`
const wsBaseUrl = import.meta.env.VITE_WS_BASE_URL || defaultWsBaseUrl

export { apiBaseUrl, wsBaseUrl }
