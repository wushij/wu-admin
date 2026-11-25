// #ifdef H5
import * as uniH5Runtime from '@dcloudio/uni-h5'

/**
 * uni-app 5.x（3.0.0-50304…）的 H5 生产构建会把业务代码里的 `uni.xxx` 静态编译成
 * `import { xxx } from '@dcloudio/uni-h5'`，全局 `window.uni` 只保留一个空占位对象。
 *
 * 应用自身代码不受影响，但第三方库（如 luch-request 4）运行时读的是全局 `uni`，
 * 于是 `uni.request` 为 undefined，抛
 * `uni.request is not supported in the current runtime`。
 *
 * 这里在应用启动最早期把运行时 API 重新挂回全局，只补齐缺失项，不覆盖已有实现。
 */
const globalScope = window as typeof window & { uni?: Record<string, unknown> }
const uniGlobal = globalScope.uni ?? (globalScope.uni = {})

for (const [name, api] of Object.entries(uniH5Runtime)) {
  if (typeof api !== 'function') continue
  if (typeof uniGlobal[name] === 'function') continue
  uniGlobal[name] = api as (...args: unknown[]) => unknown
}
// #endif
