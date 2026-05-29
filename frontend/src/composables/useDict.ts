import { ref, shallowRef, unref, type Ref, type MaybeRef } from 'vue'
import { listDictDataByType, batchDictData } from '@/api/system/dict'
import type { DictDataItem, DictOption } from '@/types/api'

const cache = new Map<string, DictOption[]>()

const LIST_CLASS_TAG: Record<string, string> = {
  default: 'info',
  success: 'success',
  warning: 'warning',
  danger: 'danger',
  error: 'danger',
  info: 'info',
  primary: 'primary',
}

export function listClassToTagType(listClass?: string | null): string {
  return LIST_CLASS_TAG[listClass || 'default'] || 'info'
}

function mapDictItem(item: DictDataItem): DictOption {
  return {
    label: item.dictLabel,
    value: item.dictValue,
    raw: item,
  }
}

type ValueType = 'number' | 'string' | 'auto'

function coerceValue(val: unknown, valueType: ValueType): string | number | unknown {
  if (val === null || val === undefined || val === '') return val
  if (valueType === 'number') {
    const n = Number(val)
    return Number.isNaN(n) ? val : n
  }
  if (valueType === 'string') return String(val)
  const n = Number(val)
  if (!Number.isNaN(n) && String(n) === String(val).trim()) return n
  return val
}

function coerceOptions(list: DictOption[], valueType: ValueType): DictOption[] {
  return list.map(o => ({
    ...o,
    value: coerceValue(o.value, valueType) as string | number,
  }))
}

export interface UseDictOptions {
  valueType?: ValueType
}

/**
 * 按字典类型加载下拉选项（带内存缓存）
 */
export function useDict(dictType: MaybeRef<string>, opts: UseDictOptions = {}) {
  const valueType = opts.valueType || 'auto'
  const options = ref<DictOption[]>([])
  const loading = ref(false)

  async function load(force = false) {
    const type = unref(dictType)
    if (!type) {
      options.value = []
      return []
    }
    if (!force && cache.has(type)) {
      options.value = coerceOptions(cache.get(type)!, valueType)
      return options.value
    }
    loading.value = true
    try {
      const res = await listDictDataByType(type)
      const list = (Array.isArray(res.data) ? res.data : []).map(mapDictItem)
      cache.set(type, list)
      options.value = coerceOptions(list, valueType)
      return options.value
    } finally {
      loading.value = false
    }
  }

  function labelOf(value: unknown) {
    return getDictLabel(unref(dictType), value)
  }

  function tagTypeOf(value: unknown) {
    return listClassToTagType(getDictListClass(unref(dictType), value))
  }

  return { options, loading, load, labelOf, tagTypeOf }
}

/** 从缓存读取标签（需已 preload 或 load） */
export function getDictLabel(dictType: string, value: unknown): string {
  if (value === null || value === undefined || value === '') return '-'
  const list = cache.get(dictType) || []
  const hit = list.find(o => String(o.value) === String(value))
  return hit ? hit.label : String(value)
}

export function getDictListClass(dictType: string, value: unknown): string {
  const list = cache.get(dictType) || []
  const hit = list.find(o => String(o.value) === String(value))
  return hit?.raw?.listClass || 'default'
}

/** 批量预加载多个字典类型 */
export async function preloadDicts(dictTypes: string[]) {
  if (!dictTypes?.length) return {} as Record<string, DictOption[]>
  const missing = dictTypes.filter(t => t && !cache.has(t))
  if (!missing.length) {
    return Object.fromEntries(dictTypes.map(t => [t, cache.get(t) || []]))
  }
  const res = await batchDictData(missing)
  const map = (res.data || res || {}) as Record<string, DictDataItem[]>
  Object.keys(map).forEach(type => {
    cache.set(type, (map[type] || []).map(mapDictItem))
  })
  return map
}

/** 清空字典缓存（类型或数据变更后调用） */
export function clearDictCache(dictType?: string) {
  if (dictType) {
    cache.delete(dictType)
  } else {
    cache.clear()
  }
}

export const dictCache: Ref<Map<string, DictOption[]>> = shallowRef(cache)
