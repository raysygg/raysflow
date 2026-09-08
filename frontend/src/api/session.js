export const ACCESS_TOKEN_KEY = 'accessToken'

export const SESSION_KEYS = Object.freeze([
  ACCESS_TOKEN_KEY,
  'username',
  'nickname',
  'role',
  'tenantId',
  'tenantCode',
  'tenantName',
  'workspaceMenus',
  'runtime-application-draft',
])

/** 清除登录、租户、服务端菜单和开发草稿上下文，保留主题等个人偏好。 */
export function clearLocalSession(storage = globalThis.localStorage) {
  if (!storage) return
  SESSION_KEYS.forEach(key => storage.removeItem(key))
}
