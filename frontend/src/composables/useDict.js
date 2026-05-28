import { ref, shallowRef, unref } from 'vue'
import { listDictDataByType, batchDictData } from '@/api/system/dict'

const cache = new Map()

const LIST_CLASS_TAG = {
  default: 'info',
  success: 'success',
  warning: 'warning',
  danger: 'danger',
  error: 'danger',
  info: 'info',
  primary: 'primary'
}

export function listClassToTagType(listClass) {
  return LIST_CLASS_TAG[listClass || 'default'] || 'info'
}

function mapDictItem(item) {
  return {
    label: item.dictLabel,
    value: item.dictValue,
    raw: item
  }
}

function coerceValue(val, valueType) {
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

function coerceOptions(list, valueType) {
  return list.map((o) => ({
    ...o,
    value: coerceValue(o.value, valueType)
  }))
}

/**
 * 按字典类型加载下拉选项（带内存缓存）
 * @param {string} dictType 字典类型编码
 * @param {{ valueType?: 'number'|'string'|'auto' }} [opts]
 */
export function useDict(dictType, opts = {}) {
  const valueType = opts.valueType || 'auto'
  const options = ref([])
  const loading = ref(false)

  async function load(force = false) {
    const type = unref(dictType)
    if (!type) {
      options.value = []
      return []
    }
    if (!force && cache.has(type)) {
      options.value = coerceOptions(cache.get(type), valueType)
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

  function labelOf(value) {
    return getDictLabel(unref(dictType), value)
  }

  function tagTypeOf(value) {
    return listClassToTagType(getDictListClass(unref(dictType), value))
  }

  return { options, loading, load, labelOf, tagTypeOf }
}

/**
 * 从缓存读取标签（需已 preload 或 load）
 */
export function getDictLabel(dictType, value) {
  if (value === null || value === undefined || value === '') return '-'
  const list = cache.get(dictType) || []
  const hit = list.find((o) => String(o.value) === String(value))
  return hit ? hit.label : String(value)
}

export function getDictListClass(dictType, value) {
  const list = cache.get(dictType) || []
  const hit = list.find((o) => String(o.value) === String(value))
  return hit?.raw?.listClass || 'default'
}

/**
 * 批量预加载多个字典类型
 * @param {string[]} dictTypes
 */
export async function preloadDicts(dictTypes) {
  if (!dictTypes?.length) return {}
  const missing = dictTypes.filter((t) => t && !cache.has(t))
  if (!missing.length) {
    return Object.fromEntries(dictTypes.map((t) => [t, cache.get(t) || []]))
  }
  const res = await batchDictData(missing)
  const map = res.data || res || {}
  Object.keys(map).forEach((type) => {
    cache.set(type, (map[type] || []).map(mapDictItem))
  })
  return map
}

/** 清空字典缓存（类型或数据变更后调用） */
export function clearDictCache(dictType) {
  if (dictType) {
    cache.delete(dictType)
  } else {
    cache.clear()
  }
}

export const dictCache = shallowRef(cache)
