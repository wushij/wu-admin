import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRecycleSummary, type RecycleSummary } from '@/api/system/recycle'
import { useUserStore } from '@/store/user'
import { hasMenuPermission } from '@/utils/permission'
import { RECYCLE_TYPES, getRecycleType, type RecycleTypeKey } from '../recycle-config'
import { preloadDicts } from '@/composables/useDict'
import { DICT_TYPE } from '@/constants/dict'
import { openAlertDialog } from '@/composables/ui/useAlertDialog'

export function useRecyclePage() {
  const route = useRoute()
  const router = useRouter()
  const userStore = useUserStore()

  const summary = reactive<RecycleSummary>({
    user: 0,
    role: 0,
    menu: 0,
    dept: 0,
    post: 0,
    ticket: 0,
    approval: 0,
    dict: 0,
    dictData: 0,
    announce: 0,
    job: 0,
    jobLog: 0,
    file: 0,
    gen: 0,
    total: 0,
  })

  const activeType = ref<RecycleTypeKey>('user')
  const loading = ref(false)
  const refreshing = ref(false)
  const tableData = ref<Record<string, unknown>[]>([])
  const total = ref(0)
  const searchForm = reactive<Record<string, string>>({})
  const query = reactive({ pageNo: 1, pageSize: 10 })

  const visibleTypes = computed(() => {
    const codes = userStore.userInfo?.permissions || []
    const hasRecycleQuery =
      codes.includes('system:recycle:query') || hasMenuPermission(userStore.menus, 'system:recycle:query')
    if (hasRecycleQuery) return RECYCLE_TYPES
    return RECYCLE_TYPES.filter((t) => hasMenuPermission(userStore.menus, t.deletePermission))
  })

  const currentType = computed(() => getRecycleType(activeType.value))
  const activePendingCount = computed(() => summary[activeType.value] ?? 0)
  const visiblePendingTotal = computed(() =>
    visibleTypes.value.reduce((sum, type) => sum + (summary[type.key] ?? 0), 0),
  )

  function hasPermCode(code: string) {
    const codes = userStore.userInfo?.permissions || []
    return codes.includes(code) || hasMenuPermission(userStore.menus, code)
  }

  const canRestore = computed(() => {
    const perm = currentType.value?.deletePermission
    if (!perm) return false
    return hasPermCode('system:recycle:restore') || hasMenuPermission(userStore.menus, perm)
  })

  const canDeletePermanent = computed(() => {
    const perm = currentType.value?.deletePermission
    if (!perm) return false
    return hasPermCode('system:recycle:delete') || hasMenuPermission(userStore.menus, perm)
  })

  function initSearchForm() {
    Object.keys(searchForm).forEach((k) => delete searchForm[k])
    currentType.value?.searchFields?.forEach((f) => {
      searchForm[f.key] = ''
    })
  }

  function switchType(key: RecycleTypeKey) {
    if (activeType.value === key) return
    activeType.value = key
    query.pageNo = 1
    initSearchForm()
    router.replace({ query: { tab: key } })
    loadList()
  }

  async function loadSummary() {
    try {
      const res = await getRecycleSummary()
      Object.assign(summary, res.data)
    } catch (e) {
      console.error(e)
    }
  }

  async function handleRefresh() {
    if (refreshing.value) return
    refreshing.value = true
    const startedAt = Date.now()
    const minSpinMs = 500
    try {
      await Promise.all([loadSummary(), loadList()])
    } finally {
      const remain = minSpinMs - (Date.now() - startedAt)
      if (remain > 0) {
        await new Promise((resolve) => setTimeout(resolve, remain))
      }
      refreshing.value = false
    }
  }

  async function loadList() {
    const type = currentType.value
    if (!type) return
    loading.value = true
    try {
      const params: Record<string, unknown> = {
        pageNo: query.pageNo,
        pageSize: query.pageSize,
      }
      type.searchFields?.forEach((f) => {
        const val = searchForm[f.key]?.trim()
        if (val) params[f.key] = val
      })
      const res = await type.fetchPage(params)
      tableData.value = (res.data?.list || []) as Record<string, unknown>[]
      total.value = res.data?.total || 0
    } catch (e) {
      console.error(e)
    } finally {
      loading.value = false
    }
  }

  function handleSearch() {
    query.pageNo = 1
    loadList()
  }

  function resetSearch() {
    initSearchForm()
    handleSearch()
  }

  function resolveRowTargetName(row: Record<string, unknown>): string {
    const candidate =
      row.username ||
      row.name ||
      row.postName ||
      row.title ||
      row.dictName ||
      row.dictLabel ||
      row.jobName ||
      row.originalName ||
      row.tableName ||
      row.formNo ||
      row.ticketNo ||
      `ID #${row.id}`
    return String(candidate)
  }

  async function handleRestore(row: Record<string, unknown>) {
    const type = currentType.value
    if (!type || row.id == null) return
    await type.restore(Number(row.id))
    ElMessage.success(`已恢复${type.label}「${resolveRowTargetName(row)}」`)
    await loadSummary()
    await loadList()
  }

  async function handlePermanentDelete(row: Record<string, unknown>) {
    const type = currentType.value
    if (!type || row.id == null) return
    const targetName = resolveRowTargetName(row)
    await openAlertDialog({
      title: `彻底清除${type.label}`,
      targetName,
      message: `彻底清除后该${type.label}数据将从数据库物理销毁，无法再次恢复。请确认业务已无强依赖。`,
      severity: 'irreversible',
      confirmText: '彻底清除',
      onConfirm: async () => {
        await type.deletePermanent(Number(row.id))
        ElMessage.success('已彻底清除')
        await loadSummary()
        await loadList()
      },
    })
  }

  watch(
    visibleTypes,
    (types) => {
      if (!types.length) return
      const tab = route.query.tab as string
      const matched = types.find((t) => t.key === tab)
      if (matched) {
        activeType.value = matched.key
      } else if (!types.some((t) => t.key === activeType.value)) {
        activeType.value = types[0].key
      }
    },
    { immediate: true },
  )

  onMounted(async () => {
    await preloadDicts([DICT_TYPE.TICKET_STATUS, DICT_TYPE.APPROVAL_FORM_TYPE])
    await loadSummary()
    initSearchForm()
    await loadList()
  })

  return {
    summary,
    activeType,
    loading,
    refreshing,
    tableData,
    total,
    searchForm,
    query,
    visibleTypes,
    currentType,
    activePendingCount,
    visiblePendingTotal,
    canRestore,
    canDeletePermanent,
    switchType,
    handleRefresh,
    loadList,
    handleSearch,
    resetSearch,
    handleRestore,
    handlePermanentDelete,
  }
}
