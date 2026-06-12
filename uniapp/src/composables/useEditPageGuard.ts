import { onMounted, watch, type MaybeRefOrGetter, type Ref } from 'vue'
import { useFormDirty } from '@/composables/useFormDirty'
import { useUnsavedLeaveGuard } from '@/composables/useUnsavedLeaveGuard'

/** 编辑页：脏检查 + 返回拦截，loading 结束后自动建立基线 */
export function useEditPageGuard(
  getSnapshot: () => unknown,
  options?: { loading?: Ref<boolean>; fallbackUrl?: string },
) {
  const { isDirty, resetBaseline, markClean } = useFormDirty(getSnapshot)
  useUnsavedLeaveGuard(isDirty, { fallbackUrl: options?.fallbackUrl })

  if (options?.loading) {
    watch(options.loading, (val, prev) => {
      if (prev && !val) resetBaseline()
    })
  } else {
    onMounted(() => resetBaseline())
  }

  function leaveAfterSave() {
    markClean()
    setTimeout(() => {
      uni.navigateBack({
        fail: () => {
          /* 栈内仅一页时由调用方自行 redirect */
        },
      })
    }, 400)
  }

  return { isDirty, resetBaseline, markClean, leaveAfterSave }
}
