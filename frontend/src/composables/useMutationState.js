import { computed, ref } from 'vue'
import { focusApiErrorField } from '../api/field-errors.js'

/**
 * 为页面变更操作提供按键隔离的防重复提交和服务端刷新钩子。
 */
export function useMutationState() {
  const pendingKeys = ref(new Set())
  const inFlight = new Map()

  const isPending = key => pendingKeys.value.has(key)

  const run = (key, mutation, refresh) => {
    if (inFlight.has(key)) return inFlight.get(key)

    pendingKeys.value = new Set([...pendingKeys.value, key])
    const promise = Promise.resolve()
      .then(mutation)
      .then(async result => {
        if (refresh) await refresh(result)
        return result
      })
      .catch(error => {
        focusApiErrorField(error)
        throw error
      })
      .finally(() => {
        inFlight.delete(key)
        const next = new Set(pendingKeys.value)
        next.delete(key)
        pendingKeys.value = next
      })

    inFlight.set(key, promise)
    return promise
  }

  return {
    pendingKeys: computed(() => [...pendingKeys.value]),
    anyPending: computed(() => pendingKeys.value.size > 0),
    isPending,
    run,
  }
}
