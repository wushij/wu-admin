import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  deleteKnowledgeDocument,
  pageKnowledgeDocument,
  reparseKnowledgeDocument,
  reindexKnowledgeDocument,
  updateKnowledgeDocumentStatus,
  type KnowledgeDocumentPageQuery,
  type KnowledgeDocumentVO,
} from '@/api/system/ai-knowledge-document'
import { openAlertDialog } from '@/composables/ui/useAlertDialog'

const PARSE_META: Record<string, { label: string; cls: string }> = {
  PENDING: { label: '待解析', cls: 'is-pending' },
  PARSING: { label: '解析中', cls: 'is-running' },
  SUCCESS: { label: '已解析', cls: 'is-success' },
  FAILED: { label: '解析失败', cls: 'is-failed' },
}

const INDEX_META: Record<string, { label: string; cls: string }> = {
  PENDING: { label: '待向量化', cls: 'is-pending' },
  INDEXING: { label: '向量化中', cls: 'is-running' },
  INDEXED: { label: '已向量化', cls: 'is-success' },
  PARTIAL: { label: '部分完成', cls: 'is-warning' },
  FAILED: { label: '向量化失败', cls: 'is-failed' },
}

/** 文档列表状态与操作；处理中的文档会自动轮询刷新进度 */
export function useKnowledgeDocuments(knowledgeBaseId: () => number, refreshKey?: () => number) {
  const loading = ref(false)
  const list = ref<KnowledgeDocumentVO[]>([])
  const total = ref(0)
  const queryParams = reactive<KnowledgeDocumentPageQuery>({
    pageNo: 1,
    pageSize: 10,
    knowledgeBaseId: 0,
    fileName: '',
    parseStatus: '',
    indexStatus: '',
  })

  let timer: number | null = null
  let pollCount = 0

  function parseMeta(status?: string) {
    return PARSE_META[String(status || 'PENDING')] || { label: String(status || '—'), cls: 'is-pending' }
  }

  function indexMeta(status?: string) {
    return INDEX_META[String(status || 'PENDING')] || { label: String(status || '—'), cls: 'is-pending' }
  }

  async function loadData(silent = false) {
    if (!knowledgeBaseId()) return
    if (!silent) loading.value = true
    try {
      queryParams.knowledgeBaseId = knowledgeBaseId()
      const res = await pageKnowledgeDocument({
        pageNo: queryParams.pageNo,
        pageSize: queryParams.pageSize,
        knowledgeBaseId: queryParams.knowledgeBaseId,
        fileName: queryParams.fileName || undefined,
        parseStatus: queryParams.parseStatus || undefined,
        indexStatus: queryParams.indexStatus || undefined,
      })
      list.value = res.data?.list || []
      total.value = Number(res.data?.total) || 0
      schedulePoll()
    } finally {
      loading.value = false
    }
  }

  /** 存在处理中的文档时每 3 秒静默刷新，最多轮询 5 分钟 */
  function schedulePoll() {
    const hasProcessing = list.value.some(
      (item) =>
        item.parseStatus === 'PENDING' ||
        item.parseStatus === 'PARSING' ||
        item.indexStatus === 'PENDING' ||
        item.indexStatus === 'INDEXING',
    )
    if (!hasProcessing) {
      stopPoll()
      return
    }
    if (timer != null) return
    pollCount = 0
    timer = window.setInterval(() => {
      pollCount += 1
      if (pollCount > 100) {
        stopPoll()
        return
      }
      loadData(true)
    }, 3000)
  }

  function stopPoll() {
    if (timer != null) {
      window.clearInterval(timer)
      timer = null
    }
  }

  function handleQuery() {
    queryParams.pageNo = 1
    loadData()
  }

  function resetQuery() {
    queryParams.fileName = ''
    queryParams.parseStatus = ''
    queryParams.indexStatus = ''
    handleQuery()
  }

  async function handleReparse(row: KnowledgeDocumentVO) {
    await reparseKnowledgeDocument(row.id)
    ElMessage.success('已提交重新解析')
    loadData(true)
  }

  async function handleReindex(row: KnowledgeDocumentVO) {
    await reindexKnowledgeDocument(row.id)
    ElMessage.success('已提交重新切片')
    loadData(true)
  }

  async function toggleStatus(row: KnowledgeDocumentVO, enabled: boolean) {
    await updateKnowledgeDocumentStatus(row.id, enabled ? 1 : 0)
    ElMessage.success(enabled ? '已启用，正在重建向量' : '已停用，已移出向量索引')
    loadData(true)
  }

  async function handleDelete(row: KnowledgeDocumentVO) {
    await openAlertDialog({
      title: '删除文档',
      targetName: row.fileName,
      message: '将同时删除该文档的全部切片与向量数据，删除后不可恢复。',
      severity: 'irreversible',
      confirmText: '永久删除',
      onConfirm: async () => {
        await deleteKnowledgeDocument(row.id)
        ElMessage.success('文档已删除')
        loadData()
      },
    })
  }

  const hasData = computed(() => list.value.length > 0)

  onMounted(() => loadData())
  onUnmounted(stopPoll)

  if (refreshKey) {
    // 父级上传或重建索引后递增刷新标识，这里静默重载列表
    watch(refreshKey, () => loadData(true))
  }

  return {
    loading,
    list,
    total,
    queryParams,
    hasData,
    parseMeta,
    indexMeta,
    loadData,
    handleQuery,
    resetQuery,
    handleReparse,
    handleReindex,
    toggleStatus,
    handleDelete,
  }
}
