import { onMounted, ref, unref, type MaybeRef } from 'vue'
import { listDictDataByType, batchDictData } from '@/api/system/dict'
import type { DictDataItem, DictOption } from '@/types/api'

const cache = new Map<string, DictOption[]>()

const LIST_CLASS_TAG: Record<string, 'default' | 'success' | 'warning' | 'danger' | 'primary'> = {
  default: 'default',
  success: 'success',
  warning: 'warning',
  danger: 'danger',
  error: 'danger',
  info: 'default',
  primary: 'primary',
}

function mapItem(item: DictDataItem): DictOption {
  return { label: item.dictLabel, value: item.dictValue, raw: item }
}

export function listClassToTagType(listClass?: string | null) {
  return LIST_CLASS_TAG[listClass || 'default'] || 'default'
}

export function getDictOptions(dictType: string): DictOption[] {
  return cache.get(dictType) || []
}

export function getDictLabel(dictType: string, value?: string | number | null): string {
  if (value == null || value === '') return '—'
  const opts = cache.get(dictType) || []
  const hit = opts.find((o) => String(o.value) === String(value))
  return hit?.label || String(value)
}

export function getDictListClass(dictType: string, value?: string | number | null): string {
  const opts = cache.get(dictType) || []
  const hit = opts.find((o) => String(o.value) === String(value))
  return hit?.raw?.listClass || 'default'
}

export async function preloadDicts(dictTypes: string[]) {
  const missing = dictTypes.filter((t) => t && !cache.has(t))
  if (!missing.length) return
  const res = await batchDictData(missing)
  const map = res.data || {}
  Object.keys(map).forEach((type) => {
    cache.set(type, (map[type] || []).map(mapItem))
  })
}

export function useDict(dictType: MaybeRef<string>) {
  const options = ref<DictOption[]>([])
  const loading = ref(false)

  async function load(force = false) {
    const type = unref(dictType)
    if (!type) {
      options.value = []
      return
    }
    if (!force && cache.has(type)) {
      options.value = cache.get(type) || []
      return
    }
    loading.value = true
    try {
      const res = await listDictDataByType(type)
      const list = (res.data || []).map(mapItem)
      cache.set(type, list)
      options.value = list
    } finally {
      loading.value = false
    }
  }

  onMounted(() => load())

  return {
    options,
    loading,
    load,
    getLabel: (v: string | number) => getDictLabel(unref(dictType), v),
    getEffect: (v: string | number) => listClassToTagType(getDictListClass(unref(dictType), v)),
  }
}
