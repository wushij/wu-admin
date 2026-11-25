import { onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getKnowledgeChunkStats,
  pageKnowledgeChunk,
  reindexKnowledgeChunk,
  type KnowledgeChunkPageQuery,
  type KnowledgeChunkVO,
} from '@/api/system/ai-knowledge-chunk'
import type { KnowledgeChunkStatsVO } from '@/api/system/ai-knowledge-base'
import { pageKnowledgeDocument } from '@/api/system/ai-knowledge-document'

export interface DocumentOption {
  id: number
  fileName: string
}

/** 切片列表状态、统计与操作 */
export function useKnowledgeChunks(knowledgeBaseId: () => number, refreshKey?: () => number) {
  const loading = ref(false)
  const list = ref<KnowledgeChunkVO[]>([])
  const total = ref(0)
  const stats = ref<KnowledgeChunkStatsVO | null>(null)
  const documentOptions = ref<DocumentOption[]>([])
  const reindexingId = ref<number | null>(null)

  let pollTimer: number | null = null
  let pollCount = 0

  /** 存在待向量化切片时每 3 秒静默刷新，让后台向量化进度实时可见 */
  function schedulePoll() {
    const hasPending = list.value.some((chunk) => chunk.embedStatus === 'PENDING')
    if (!hasPending) {
      stopPoll()
      return
    }
    if (pollTimer != null) {
      return
    }
    pollCount = 0
    pollTimer = window.setInterval(async () => {
      pollCount += 1
      if (pollCount > 100) {
        stopPoll()
        return
      }
      await refreshAll()
    }, 3000)
  }

  function stopPoll() {
    if (pollTimer != null) {
      window.clearInterval(pollTimer)
      pollTimer = null
    }
  }

  const queryParams = reactive<KnowledgeChunkPageQuery>({
    pageNo: 1,
    pageSize: 10,
    knowledgeBaseId: 0,
    documentId: null,
    keyword: '',
    embedStatus: '',
  })

  async function loadStats() {
    if (!knowledgeBaseId()) return
    try {
      const res = await getKnowledgeChunkStats(knowledgeBaseId())
      stats.value = res.data || null
    } catch {
      stats.value = null
    }
  }

  async function loadDocumentOptions() {
    if (!knowledgeBaseId()) return
    try {
      const res = await pageKnowledgeDocument({
        pageNo: 1,
        pageSize: 200,
        knowledgeBaseId: knowledgeBaseId(),
      })
      documentOptions.value = (res.data?.list || []).map((item) => ({
        id: item.id,
        fileName: item.fileName,
      }))
    } catch {
      documentOptions.value = []
    }
  }

  async function loadData(silent = false) {
    if (!knowledgeBaseId()) return
    if (!silent) loading.value = true
    try {
      queryParams.knowledgeBaseId = knowledgeBaseId()
      const res = await pageKnowledgeChunk({
        pageNo: queryParams.pageNo,
        pageSize: queryParams.pageSize,
        knowledgeBaseId: queryParams.knowledgeBaseId,
        documentId: queryParams.documentId || undefined,
        keyword: queryParams.keyword || undefined,
        embedStatus: queryParams.embedStatus || undefined,
      })
      list.value = res.data?.list || []
      total.value = Number(res.data?.total) || 0
      schedulePoll()
    } finally {
      loading.value = false
    }
  }

  async function refreshAll() {
    await Promise.all([loadData(true), loadStats()])
  }

  function handleQuery() {
    queryParams.pageNo = 1
    loadData()
  }

  function resetQuery() {
    queryParams.documentId = null
    queryParams.keyword = ''
    queryParams.embedStatus = ''
    handleQuery()
  }

  async function handleReindexChunk(chunk: KnowledgeChunkVO) {
    reindexingId.value = chunk.id
    try {
      await reindexKnowledgeChunk(chunk.id)
      ElMessage.success('向量已重建')
      await refreshAll()
    } finally {
      reindexingId.value = null
    }
  }

  function embedMeta(status?: string) {
    const map: Record<string, { label: string; cls: string }> = {
      PENDING: { label: '待向量化', cls: 'is-pending' },
      INDEXED: { label: '已向量化', cls: 'is-success' },
      FAILED: { label: '向量化失败', cls: 'is-failed' },
    }
    return map[String(status || 'PENDING')] || {
      label: String(status || '—'),
      cls: 'is-pending',
    }
  }

  onMounted(async () => {
    await Promise.all([loadData(), loadStats(), loadDocumentOptions()])
  })
  onUnmounted(stopPoll)

  if (refreshKey) {
    watch(refreshKey, () => {
      refreshAll()
      loadDocumentOptions()
    })
  }

  return {
    loading,
    list,
    total,
    stats,
    documentOptions,
    queryParams,
    reindexingId,
    loadData,
    refreshAll,
    handleQuery,
    resetQuery,
    handleReindexChunk,
    embedMeta,
  }
}
