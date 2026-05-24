import { onShow } from '@dcloudio/uni-app'
import { registerPageShallowFallback, installH5ShallowStackTrapIfNeeded } from '@/utils/navigate-back'

/** 可选：覆盖全局自动推断的 fallback 目标页 */
export function useShallowStackBackFallback(fallbackUrl: string) {
  onShow(() => {
    registerPageShallowFallback(fallbackUrl)
    installH5ShallowStackTrapIfNeeded()
  })
}
