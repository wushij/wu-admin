import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  deleteKnowledgeBase,
  getKnowledgeBase,
  getKnowledgeChunkStats,
  type KnowledgeBaseVO,
  type KnowledgeChunkStatsVO,
} from '@/api/system/ai-knowledge-base'
import { reindexKnowledgeBase } from '@/api/system/ai-knowledge-document'
import { openAlertDialog } from '@/composables/ui/useAlertDialog'

/** 知识库详情页壳状态：知识库信息、切片统计与顶部操作 */
export function useKnowledgeDetail() {
  const route = useRoute()
  const router = useRouter()

  const knowledgeBaseId = computed(() => Number(route.params.id))
  const loading = ref(false)
  const detail = ref<KnowledgeBaseVO | null>(null)
  const stats = ref<KnowledgeChunkStatsVO | null>(null)
  const reindexing = ref(false)

  async function loadDetail() {
    if (!knowledgeBaseId.value) return
    loading.value = true
    try {
      const [detailRes, statsRes] = await Promise.all([
        getKnowledgeBase(knowledgeBaseId.value),
        getKnowledgeChunkStats(knowledgeBaseId.value),
      ])
      detail.value = detailRes.data || null
      stats.value = statsRes.data || null
    } finally {
      loading.value = false
    }
  }

  async function handleReindexAll() {
    await openAlertDialog({
      title: '重建知识库索引',
      targetName: detail.value?.name || `知识库 #${knowledgeBaseId.value}`,
      message: '将重新切分全部文档并重新生成向量索引，期间检索结果可能不完整，已有切片会被替换。',
      severity: 'warning',
      confirmText: '开始重建',
      onConfirm: async () => {
        reindexing.value = true
        try {
          await reindexKnowledgeBase(knowledgeBaseId.value)
          ElMessage.success('已提交重建任务，可在切片列表查看进度')
          window.setTimeout(loadDetail, 1500)
        } finally {
          reindexing.value = false
        }
      },
    })
  }

  async function handleDelete() {
    const current = detail.value
    if (!current) return
    await openAlertDialog({
      title: '删除知识库',
      targetName: current.name,
      message: `将同时删除 ${current.documentCount ?? 0} 篇文档、${current.chunkCount ?? 0} 个切片及全部向量数据，删除后不可恢复。`,
      severity: 'irreversible',
      confirmText: '永久删除',
      onConfirm: async () => {
        await deleteKnowledgeBase(current.id)
        ElMessage.success('知识库已删除')
        router.replace('/ai/knowledge')
      },
    })
  }

  function goBack() {
    router.push('/ai/knowledge')
  }

  const indexStatusMeta = computed(() => {
    const raw = String(detail.value?.indexStatus || 'IDLE')
    const map: Record<string, { label: string; cls: string }> = {
      IDLE: { label: '待索引', cls: 'is-idle' },
      INDEXING: { label: '索引中', cls: 'is-indexing' },
      INDEXED: { label: '已就绪', cls: 'is-ready' },
      FAILED: { label: '索引异常', cls: 'is-failed' },
    }
    return map[raw] || { label: raw, cls: 'is-idle' }
  })

  onMounted(loadDetail)

  return {
    knowledgeBaseId,
    loading,
    detail,
    stats,
    reindexing,
    indexStatusMeta,
    loadDetail,
    handleReindexAll,
    handleDelete,
    goBack,
  }
}
