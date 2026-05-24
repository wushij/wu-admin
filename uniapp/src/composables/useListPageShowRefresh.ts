import { ref, type Ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'

type RefreshFn = (options?: { silent?: boolean }) => void | Promise<void>

/** 列表页返回时静默刷新（跳过首次 onShow，避免与 onMounted 重复请求） */
export function useListPageShowRefresh(
  refresh: RefreshFn,
  options?: { loading?: Ref<boolean>; refreshing?: Ref<boolean> },
) {
  const skipNextShowRefresh = ref(true)
  onShow(async () => {
    if (skipNextShowRefresh.value) {
      skipNextShowRefresh.value = false
      return
    }
    if (options?.loading?.value || options?.refreshing?.value) return
    await refresh({ silent: true })
  })
}
