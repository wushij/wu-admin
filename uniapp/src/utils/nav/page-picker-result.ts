const STORAGE_KEY = 'uniapp_page_picker_result'

export interface PagePickerResult {
  kind: string
  id?: number | null
  label?: string
  ids?: number[]
  labelText?: string
}

export function clearPagePickerResult() {
  try {
    uni.removeStorageSync(STORAGE_KEY)
  } catch {
    /* ignore */
  }
}

export function setPagePickerResult(result: PagePickerResult) {
  uni.setStorageSync(STORAGE_KEY, JSON.stringify(result))
}

export function consumePagePickerResult(kind?: string): PagePickerResult | null {
  try {
    const raw = uni.getStorageSync(STORAGE_KEY)
    if (!raw) return null
    const result = JSON.parse(String(raw)) as PagePickerResult
    if (kind && result.kind !== kind) return null
    uni.removeStorageSync(STORAGE_KEY)
    return result
  } catch {
    uni.removeStorageSync(STORAGE_KEY)
    return null
  }
}
