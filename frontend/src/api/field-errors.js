/**
 * 将服务端字段错误定位到对应控件，同时保留用户已填写的表单内容。
 */
export function focusApiErrorField(error, root = globalThis.document) {
  const fieldPath = String(error?.fieldPath || '').trim()
  if (!fieldPath || !root?.querySelector) return false

  const escapedAttribute = fieldPath.replace(/\\/g, '\\\\').replace(/"/g, '\\"')
  const escapedId = globalThis.CSS?.escape
    ? globalThis.CSS.escape(fieldPath)
    : fieldPath.replace(/[^a-zA-Z0-9_-]/g, '\\$&')
  const control = root.querySelector(`[data-field-path="${escapedAttribute}"]`)
    || root.querySelector(`[name="${escapedAttribute}"]`)
    || root.querySelector(`#${escapedId}`)
  if (!control) return false

  control.setAttribute?.('aria-invalid', 'true')
  control.focus?.()
  return true
}
