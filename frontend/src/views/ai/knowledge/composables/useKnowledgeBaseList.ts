import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  createKnowledgeBase,
  deleteKnowledgeBase,
  listEmbeddingModels,
  pageKnowledgeBase,
  updateKnowledgeBase,
  type EmbeddingModelOption,
  type KnowledgeBasePageQuery,
  type KnowledgeBaseSaveDTO,
  type KnowledgeBaseVO,
} from '@/api/system/ai-knowledge-base'
import { openAlertDialog } from '@/composables/ui/useAlertDialog'

/** 知识库列表页状态与操作 */
export function useKnowledgeBaseList() {
  const router = useRouter()

  const loading = ref(false)
  const total = ref(0)
  const list = ref<KnowledgeBaseVO[]>([])
  const queryParams = reactive<KnowledgeBasePageQuery>({
    pageNo: 1,
    pageSize: 9,
    name: '',
    status: null,
    indexStatus: '',
  })

  const embeddingModels = ref<EmbeddingModelOption[]>([])

  async function loadData() {
    loading.value = true
    try {
      const res = await pageKnowledgeBase({
        pageNo: queryParams.pageNo,
        pageSize: queryParams.pageSize,
        name: queryParams.name || undefined,
        status: queryParams.status,
        indexStatus: queryParams.indexStatus || undefined,
      })
      list.value = res.data?.list || []
      total.value = Number(res.data?.total) || 0
    } finally {
      loading.value = false
    }
  }

  async function loadEmbeddingModels() {
    try {
      const res = await listEmbeddingModels()
      embeddingModels.value = res.data || []
    } catch {
      embeddingModels.value = []
    }
  }

  function handleQuery() {
    queryParams.pageNo = 1
    loadData()
  }

  function resetQuery() {
    queryParams.name = ''
    queryParams.status = null
    queryParams.indexStatus = ''
    handleQuery()
  }

  const dialogVisible = ref(false)
  const submitting = ref(false)
  const formRef = ref<FormInstance>()
  const form = reactive<KnowledgeBaseSaveDTO>(defaultForm())

  function defaultForm(): KnowledgeBaseSaveDTO {
    return {
      id: undefined,
      name: '',
      description: '',
      embeddingModelId: 0,
      chunkSize: 800,
      chunkOverlap: 128,
      status: 1,
      sort: 0,
    }
  }

  const rules: FormRules = {
    name: [
      { required: true, message: '请输入知识库名称', trigger: 'blur' },
      { max: 128, message: '名称不能超过 128 个字符', trigger: 'blur' },
    ],
  }

  function openDialog(row?: KnowledgeBaseVO) {
    Object.assign(form, defaultForm())
    if (row) {
      Object.assign(form, {
        id: row.id,
        name: row.name,
        description: row.description || '',
        embeddingModelId: row.embeddingModelId || 0,
        chunkSize: row.chunkSize ?? 800,
        chunkOverlap: row.chunkOverlap ?? 128,
        status: row.status ?? 1,
        sort: row.sort ?? 0,
      })
    }
    dialogVisible.value = true
    formRef.value?.clearValidate()
  }

  async function handleSubmit() {
    await formRef.value?.validate()
    submitting.value = true
    try {
      const payload: KnowledgeBaseSaveDTO = { ...form }
      if (payload.id) {
        await updateKnowledgeBase(payload)
        ElMessage.success('修改成功')
      } else {
        await createKnowledgeBase(payload)
        ElMessage.success('创建成功，可进入知识库上传文档')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  }

  function openDetail(row: KnowledgeBaseVO) {
    router.push(`/ai/knowledge/${row.id}`)
  }

  async function handleDelete(row: KnowledgeBaseVO) {
    await openAlertDialog({
      title: '删除知识库',
      targetName: row.name,
      message: `将同时删除该知识库下的 ${row.documentCount ?? 0} 篇文档、${row.chunkCount ?? 0} 个切片及全部向量数据，删除后不可恢复。`,
      severity: 'irreversible',
      confirmText: '永久删除',
      onConfirm: async () => {
        await deleteKnowledgeBase(row.id)
        ElMessage.success('知识库已删除')
        loadData()
      },
    })
  }

  const hasEmbeddingModel = computed(() => embeddingModels.value.length > 0)

  onMounted(() => {
    loadData()
    loadEmbeddingModels()
  })

  return {
    loading,
    total,
    list,
    queryParams,
    embeddingModels,
    hasEmbeddingModel,
    dialogVisible,
    submitting,
    formRef,
    form,
    rules,
    loadData,
    handleQuery,
    resetQuery,
    openDialog,
    handleSubmit,
    openDetail,
    handleDelete,
  }
}
