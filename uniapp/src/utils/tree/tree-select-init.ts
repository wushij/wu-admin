const POST_IDS_KEY = 'uniapp_tree_select_post_ids'
const DEPT_IDS_KEY = 'uniapp_tree_select_dept_ids'

export function setTreeSelectPostIds(ids: number[]) {
  uni.setStorageSync(POST_IDS_KEY, JSON.stringify(ids))
}

export function getTreeSelectPostIds(): number[] {
  try {
    const raw = uni.getStorageSync(POST_IDS_KEY)
    if (!raw) return []
    const ids = JSON.parse(String(raw)) as number[]
    return Array.isArray(ids) ? ids.filter((id) => Number.isFinite(id) && id > 0) : []
  } catch {
    return []
  }
}

export function setTreeSelectDeptIds(ids: number[]) {
  uni.setStorageSync(DEPT_IDS_KEY, JSON.stringify(ids))
}

export function getTreeSelectDeptIds(): number[] {
  try {
    const raw = uni.getStorageSync(DEPT_IDS_KEY)
    if (!raw) return []
    const ids = JSON.parse(String(raw)) as number[]
    return Array.isArray(ids) ? ids.filter((id) => Number.isFinite(id) && id > 0) : []
  } catch {
    return []
  }
}
