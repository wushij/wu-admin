import { ref, computed, reactive, watch } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getRecycleSummary, type RecycleSummary } from '@/api/system/recycle'
import { RECYCLE_MODULES } from '@/constants/recycle-modules'
import { usePageList } from '@/composables/usePageList'
import { usePermission } from '@/composables/usePermission'
import { showConfirm } from '@/utils/app-dialog'
import { formatDateTime } from '@/utils/format'
import { setRecycleDetailPayload } from '@/utils/recycle-detail-cache'
import { formatRecycleFieldValue } from '@/utils/recycle-field'
import { resolveRecycleRowThumb } from '@/utils/recycle-thumb'

export function useRecycleCenter() {
  const { hasPerm } = usePermission()
  const allowed = computed(() => hasPerm('system:recycle:list'))

  const summary = ref<RecycleSummary | null>(null)
  const activeType = ref('')
  const searchForm = reactive<Record<string, string>>({})
  const searchKeyword = ref('')

  const visibleModules = computed(() => {
    if (hasPerm('system:recycle:query')) return RECYCLE_MODULES
    return RECYCLE_MODULES.filter((m) => hasPerm(m.deletePerm))
  })
  const currentModule = computed(() => visibleModules.value.find((m) => m.key === activeType.value))

  const visiblePendingTotal = computed(() => {
    if (!summary.value) return 0
    return visibleModules.value.reduce(
      (sum, mod) => sum + ((summary.value as Record<string, number>)[mod.key] || 0),
      0,
    )
  })

  const activePendingCount = computed(() => {
    if (!summary.value || !activeType.value) return 0
    return (summary.value as Record<string, number>)[activeType.value] || 0
  })

  const canRestore = computed(() => {
    const mod = currentModule.value
    return mod ? hasPerm('system:recycle:restore') || hasPerm(mod.deletePerm) : false
  })

  const canDeletePermanent = computed(() => {
    const mod = currentModule.value
    return mod ? hasPerm('system:recycle:delete') || hasPerm(mod.deletePerm) : false
  })

  const searchFields = computed(() => currentModule.value?.searchFields || [])

  const { list, loading, refreshing, finished, empty, refresh, loadMore } = usePageList<Record<string, unknown>>(
    async (pageNo, pageSize) => {
      const mod = currentModule.value
      if (!mod) return { list: [], total: 0 }
      const params: Record<string, unknown> = { pageNo, pageSize }
      mod.searchFields?.forEach((field) => {
        const val = searchForm[field.key]?.trim()
        if (val) params[field.key] = val
      })
      const res = await mod.fetchPage(params)
      return {
        list: (res.data?.list || []) as Record<string, unknown>[],
        total: res.data?.total || 0,
      }
    },
  )

  function countFor(key: string) {
    if (!summary.value) return 0
    return (summary.value as Record<string, number>)[key] || 0
  }

  function initSearchForm() {
    Object.keys(searchForm).forEach((k) => delete searchForm[k])
    currentModule.value?.searchFields?.forEach((field) => {
      searchForm[field.key] = ''
    })
    searchKeyword.value = ''
  }

  function syncSearchFromKeyword() {
    const fields = searchFields.value
    if (!fields.length) return
    fields.forEach((field, index) => {
      searchForm[field.key] = index === 0 ? searchKeyword.value.trim() : ''
    })
  }

  async function loadSummary() {
    const res = await getRecycleSummary()
    summary.value = res.data || null
  }

  async function handleRefresh() {
    await Promise.all([loadSummary(), refresh()])
  }

  function switchType(key: string) {
    if (activeType.value === key) return
    activeType.value = key
    initSearchForm()
    refresh()
  }

  function handleSearch() {
    syncSearchFromKeyword()
    refresh()
  }

  function resetSearch() {
    initSearchForm()
    refresh()
  }

  function rowTitle(item: Record<string, unknown>) {
    const mod = currentModule.value
    const field = mod?.titleField || 'id'
    const raw = item[field]
    if (raw != null && String(raw).trim()) return String(raw)
    const sub = mod?.subField ? item[mod.subField] : null
    if (sub != null && String(sub).trim()) return String(sub)
    const id = item.id
    return id != null ? `${mod?.label || '记录'} #${id}` : '—'
  }

  function rowSub(item: Record<string, unknown>) {
    const mod = currentModule.value
    const field = mod?.subField
    if (!field) return ''
    const raw = item[field]
    if (raw == null || !String(raw).trim()) return ''
    return String(raw)
  }

  function rowSubLabel() {
    return currentModule.value?.subFieldLabel || '详情'
  }

  function rowPreviewLines(item: Record<string, unknown>) {
    const mod = currentModule.value
    if (!mod?.detailFields?.length) {
      const sub = rowSub(item)
      if (!sub) return []
      return [{ label: rowSubLabel(), value: sub }]
    }
    return mod.detailFields
      .filter(
        (f) =>
          f.prop !== 'id' &&
          f.prop !== mod.titleField &&
          f.prop !== 'updateTime' &&
          f.prop !== 'createTime',
      )
      .slice(0, 2)
      .map((field) => ({
        label: field.label,
        value: formatRecycleFieldValue(item, field),
        field,
      }))
      .filter((line) => line.value !== '—')
  }

  function rowThumb(item: Record<string, unknown>) {
    return resolveRecycleRowThumb(item, currentModule.value)
  }

  function goDetail(item: Record<string, unknown>) {
    const mod = currentModule.value
    const id = Number(item.id)
    if (!mod || !id) return
    setRecycleDetailPayload({ type: mod.key, item })
    uni.navigateTo({
      url: `/pages-sub/system/recycle/detail?type=${mod.key}&id=${id}`,
    })
  }

  function rowMeta(item: Record<string, unknown>) {
    const time = item.updateTime || item.createTime
    if (!time) return ''
    return `删除于 ${formatDateTime(String(time), true)}`
  }

  async function onRestore(item: Record<string, unknown>) {
    const mod = currentModule.value
    const id = Number(item.id)
    if (!mod || !id) return
    const { confirmed } = await showConfirm({
      title: '恢复确认',
      content: `确定恢复该${mod.label}吗？`,
      confirmText: '恢复',
    })
    if (!confirmed) return
    await mod.restore(id)
    uni.showToast({ title: '已恢复', icon: 'success' })
    await Promise.all([loadSummary(), refresh()])
  }

  async function onDelete(item: Record<string, unknown>) {
    const mod = currentModule.value
    const id = Number(item.id)
    if (!mod || !id) return
    const { confirmed } = await showConfirm({
      title: '彻底删除',
      content: '删除后无法恢复，是否继续？',
      confirmText: '删除',
      tone: 'danger',
    })
    if (!confirmed) return
    await mod.deletePermanent(id)
    uni.showToast({ title: '已清除', icon: 'success' })
    await Promise.all([loadSummary(), refresh()])
  }

  watch(visibleModules, (modules) => {
    if (!modules.length) return
    if (!modules.some((m) => m.key === activeType.value)) {
      activeType.value = modules[0].key
      initSearchForm()
      refresh()
    }
  })

  onLoad((options) => {
    const type = String(options?.type || '')
    if (type && visibleModules.value.some((m) => m.key === type)) {
      activeType.value = type
    } else if (visibleModules.value.length) {
      activeType.value = visibleModules.value[0].key
    }
    initSearchForm()
  })

  async function bootstrap() {
    await loadSummary()
    if (activeType.value) await refresh()
  }

  return {
    allowed,
    summary,
    activeType,
    searchKeyword,
    visibleModules,
    currentModule,
    visiblePendingTotal,
    activePendingCount,
    canRestore,
    canDeletePermanent,
    searchFields,
    list,
    loading,
    refreshing,
    finished,
    empty,
    countFor,
    switchType,
    handleSearch,
    resetSearch,
    handleRefresh,
    loadMore,
    rowTitle,
    rowSub,
    rowSubLabel,
    rowPreviewLines,
    rowMeta,
    rowThumb,
    goDetail,
    onRestore,
    onDelete,
    bootstrap,
    refresh,
  }
}
