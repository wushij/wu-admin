import { ref, shallowRef } from 'vue'
import { listDictDataByType, batchDictData } from '@/api/system/dict'

const cache = new Map()

/**
 * 按字典类型加载下拉选项（带内存缓存）
 * @param {string} dictType 字典类型编码
 */
export function useDict(dictType) {
  const options = ref([])
  const loading = ref(false)

  async function load(force = false) {
    if (!dictType) {
      options.value = []
      return []
    }
    if (!force && cache.has(dictType)) {
      options.value = cache.get(dictType)
      return options.value
    }
    loading.value = true
    try {
      const res = await listDictDataByType(dictType)
      const list = (Array.isArray(res.data) ? res.data : []).map((item) => ({
        label: item.dictLabel,
        value: item.dictValue,
        raw: item
      }))
      cache.set(dictType, list)
      options.value = list
      return list
    } finally {
      loading.value = false
    }
  }

  function labelOf(value) {
    const hit = options.value.find((o) => String(o.value) === String(value))
    return hit ? hit.label : value
  }

  return { options, loading, load, labelOf }
}

/**
 * 批量预加载多个字典类型
 * @param {string[]} dictTypes
 */
export async function preloadDicts(dictTypes) {
  if (!dictTypes?.length) return {}
  const res = await batchDictData(dictTypes)
  const map = res.data || res || {}
  Object.keys(map).forEach((type) => {
    cache.set(
      type,
      (map[type] || []).map((item) => ({
        label: item.dictLabel,
        value: item.dictValue,
        raw: item
      }))
    )
  })
  return map
}

/** 清空字典缓存（类型变更后调用） */
export function clearDictCache(dictType) {
  if (dictType) {
    cache.delete(dictType)
  } else {
    cache.clear()
  }
}

export const dictCache = shallowRef(cache)
