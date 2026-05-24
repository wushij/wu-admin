import { RECYCLE_MODULES } from '@/constants/recycle-modules'

const STORAGE_KEY = 'uniapp_recycle_detail_payload'

export interface RecycleDetailPayload {
  type: string
  item: Record<string, unknown>
}

export function setRecycleDetailPayload(payload: RecycleDetailPayload) {
  uni.setStorageSync(STORAGE_KEY, JSON.stringify(payload))
}

/** 读取缓存（刷新后仍可复用，不删除） */
export function getRecycleDetailPayload(
  type?: string,
  id?: number,
): RecycleDetailPayload | null {
  try {
    const raw = uni.getStorageSync(STORAGE_KEY)
    if (!raw) return null
    const payload = JSON.parse(String(raw)) as RecycleDetailPayload
    if (type && payload.type !== type) return null
    if (id != null && Number(payload.item?.id) !== id) return null
    return payload
  } catch {
    return null
  }
}

export function clearRecycleDetailPayload() {
  uni.removeStorageSync(STORAGE_KEY)
}

/** 缓存缺失时从回收列表 API 按 id 查找 */
export async function resolveRecycleDetailItem(
  type: string,
  id: number,
): Promise<Record<string, unknown> | null> {
  const cached = getRecycleDetailPayload(type, id)
  if (cached) return cached.item

  const mod = RECYCLE_MODULES.find((m) => m.key === type)
  if (!mod || !id) return null

  let pageNo = 1
  const pageSize = 50
  while (pageNo <= 10) {
    const res = await mod.fetchPage({ pageNo, pageSize })
    const rows = (res.data?.list || []) as Record<string, unknown>[]
    const hit = rows.find((row) => Number(row.id) === id)
    if (hit) {
      setRecycleDetailPayload({ type, item: hit })
      return hit
    }
    const total = res.data?.total || 0
    if (pageNo * pageSize >= total) break
    pageNo++
  }
  return null
}
