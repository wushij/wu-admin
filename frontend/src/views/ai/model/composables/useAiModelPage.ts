import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useAiWuStore } from '@/store/aiWu'
import {
  pageAiModel,
  createAiModel,
  updateAiModel,
  deleteAiModel,
  setDefaultAiModel,
  testAiModel,
  listAiModels,
  type AiModelVO,
  type AiModelSaveDTO,
  type AiModelPageQuery,
  type AiProvider,
} from '@/api/system/ai-model'
import { PROVIDER_META, providerMeta } from '../constants/aiModelMeta'
import { openAlertDialog } from '@/composables/ui/useAlertDialog'
import { showSnackbar } from '@/composables/ui/useSnackbar'

export function useAiModelPage() {
  const userStore = useUserStore()
  const canUpdate = computed(() =>
    (userStore.userInfo?.permissions || []).some(
      (p: string) => p === '*:*:*' || p === 'system:ai-model:update',
    ),
  )

  const loading = ref(false)
  const total = ref(0)
  const list = ref<AiModelVO[]>([])
  const testingId = ref<number | null>(null)
  const testResults = reactive<Record<number, number>>({})

  const queryParams = reactive<AiModelPageQuery>({
    pageNo: 1,
    pageSize: 8,
    name: '',
    provider: '',
    modelType: '',
    status: null,
  })

  async function loadData() {
    loading.value = true
    try {
      const res = await pageAiModel({
        pageNo: queryParams.pageNo,
        pageSize: queryParams.pageSize,
        name: queryParams.name || undefined,
        provider: queryParams.provider || undefined,
        modelType: queryParams.modelType || undefined,
        status: queryParams.status,
      })
      list.value = res.data?.list || []
      total.value = Number(res.data?.total) || 0
    } finally {
      loading.value = false
    }
  }

  function handleQuery() {
    queryParams.pageNo = 1
    loadData()
  }

  function resetQuery() {
    queryParams.name = ''
    queryParams.provider = ''
    queryParams.modelType = ''
    queryParams.status = null
    handleQuery()
  }

  const dialogVisible = ref(false)
  const submitting = ref(false)
  const formTesting = ref(false)
  const fetchingModels = ref(false)
  const fetchedModels = ref<string[]>([])
  const formRef = ref<FormInstance>()
  const editHasKey = ref(false)
  const editIsDefault = ref(false)

  const defaultForm = (): AiModelSaveDTO & { temperature: number; maxTokens: number } => ({
    id: undefined,
    name: '',
    provider: 'deepseek',
    protocol: 'openai',
    modelType: 'chat',
    modelName: PROVIDER_META.deepseek.models[0] || 'deepseek-v4.1-flash',
    baseUrl: PROVIDER_META.deepseek.baseUrl,
    pathTemplate: '',
    extraHeaders: '',
    apiKey: '',
    temperature: 0.7,
    maxTokens: 8192,
    dimension: undefined,
    batchSize: 16,
    systemPrompt: '',
    isDefault: 0,
    status: 1,
    remark: '',
  })

  const form = reactive(defaultForm())

  const effectiveBaseUrlPlaceholder = computed(() => {
    if (form.provider === 'custom') {
      return form.protocol === 'anthropic'
        ? '如：https://api.anthropic.com'
        : '如：https://api.example.com/v1'
    }
    return `如：${providerMeta(form.provider).baseUrl || 'https://api.example.com/v1'}`
  })

  const rules: FormRules = {
    provider: [{ required: true, message: '请选择供应商', trigger: 'change' }],
    name: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
    modelName: [{ required: true, message: '请输入模型名称', trigger: 'change' }],
    baseUrl: [{ required: true, message: '请输入接口地址', trigger: 'blur' }],
    dimension: [
      {
        validator: (_rule, value, callback) => {
          if (form.modelType !== 'embedding') {
            callback()
            return
          }
          if (!value || Number(value) <= 0) {
            callback(new Error('请输入向量维度'))
            return
          }
          callback()
        },
        trigger: 'change',
      },
    ],
  }

  const modelOptions = computed(() =>
    Array.from(
      new Set([...fetchedModels.value, ...providerMeta(form.provider).models].filter(Boolean)),
    ),
  )

  function selectProvider(provider: AiProvider) {
    if (form.provider === provider) return
    form.provider = provider
    const meta = providerMeta(provider)
    form.baseUrl = meta.baseUrl
    form.modelName = meta.models[0] || ''
    form.protocol = 'openai'
    form.pathTemplate = ''
    form.extraHeaders = ''
    fetchedModels.value = []
    nextTick(() => formRef.value?.clearValidate())
  }

  function onProtocolChange() {
    nextTick(() => formRef.value?.clearValidate())
  }

  function openDialog(row?: AiModelVO) {
    Object.assign(form, defaultForm())
    editHasKey.value = false
    editIsDefault.value = false
    fetchedModels.value = []
    if (row) {
      Object.assign(form, {
        id: row.id,
        name: row.name,
        provider: row.provider,
        protocol: row.protocol || 'openai',
        modelType: row.modelType || 'chat',
        modelName: row.modelName,
        baseUrl: row.baseUrl,
        pathTemplate: row.pathTemplate || '',
        extraHeaders: row.extraHeaders || '',
        apiKey: '',
        temperature: row.temperature ?? 0.7,
        maxTokens: row.maxTokens ?? 8192,
        dimension: row.dimension || undefined,
        batchSize: row.batchSize ?? 16,
        systemPrompt: row.systemPrompt || '',
        isDefault: row.isDefault ?? 0,
        status: row.status ?? 1,
        remark: row.remark || '',
      })
      editHasKey.value = !!row.hasApiKey
      editIsDefault.value = row.isDefault === 1
    }
    dialogVisible.value = true
    nextTick(() => formRef.value?.clearValidate())
  }

  function resetForm() {
    formRef.value?.clearValidate()
  }

  async function handleSubmit() {
    await formRef.value?.validate()
    if (!form.id && !form.apiKey?.trim()) {
      ElMessage.warning('请输入 API Key')
      return
    }
    submitting.value = true
    try {
      const data: AiModelSaveDTO = { ...form, apiKey: form.apiKey || undefined }
      if (form.id) {
        await updateAiModel(data)
        ElMessage.success('修改成功')
      } else {
        await createAiModel(data)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  }

  async function handleTest(row: AiModelVO) {
    testingId.value = row.id
    try {
      const res = await testAiModel({ id: row.id })
      testResults[row.id] = Number(res.data) || 0
      ElMessage.success(`连接正常，延迟 ${res.data}ms`)
    } catch {
      testResults[row.id] = -1
    } finally {
      testingId.value = null
    }
  }

  async function handleFormTest() {
    const valid = await formRef.value
      ?.validateField(['provider', 'modelName', 'baseUrl', 'dimension'])
      .catch(() => false)
    if (valid === false) return
    if (!form.id && !form.apiKey?.trim()) {
      ElMessage.warning('请输入 API Key')
      return
    }
    formTesting.value = true
    try {
      const res = await testAiModel({
        id: form.id,
        provider: form.provider,
        protocol: form.protocol,
        modelType: form.modelType,
        dimension: form.dimension,
        modelName: form.modelName,
        baseUrl: form.baseUrl,
        pathTemplate: form.pathTemplate,
        extraHeaders: form.extraHeaders,
        apiKey: form.apiKey || undefined,
      })
      ElMessage.success(`连接正常，延迟 ${res.data}ms`)
    } finally {
      formTesting.value = false
    }
  }

  async function handleFetchModels() {
    const valid = await formRef.value?.validateField(['provider', 'baseUrl']).catch(() => false)
    if (valid === false) return
    if (!form.id && !form.apiKey?.trim()) {
      ElMessage.warning('请先输入 API Key')
      return
    }
    fetchingModels.value = true
    try {
      const res = await listAiModels({
        id: form.id,
        provider: form.provider,
        protocol: form.protocol,
        baseUrl: form.baseUrl,
        pathTemplate: form.pathTemplate,
        extraHeaders: form.extraHeaders,
        apiKey: form.apiKey || undefined,
      })
      const models = (res.data || []).filter((item) => !!item)
      if (models.length === 0) {
        ElMessage.warning('接口未返回可用模型，请手动输入模型名称')
        return
      }
      fetchedModels.value = models
      if (!form.modelName || !models.includes(form.modelName)) {
        form.modelName = models[0]
        nextTick(() => formRef.value?.clearValidate('modelName'))
      }
      ElMessage.success(`获取到 ${models.length} 个模型`)
    } finally {
      fetchingModels.value = false
    }
  }

  async function handleSetDefault(row: AiModelVO) {
    const prevDefault = list.value.find((m) => m.isDefault === 1)
    await setDefaultAiModel(row.id)
    loadData()
    useAiWuStore().loadModels()
    showSnackbar({
      message: `已将「${row.name}」设为默认模型`,
      variant: 'success',
      actionLabel: prevDefault ? '撤销' : undefined,
      onAction: prevDefault
        ? async () => {
            await setDefaultAiModel(prevDefault.id)
            ElMessage.success(`已恢复默认模型为「${prevDefault.name}」`)
            loadData()
            useAiWuStore().loadModels()
          }
        : undefined,
    })
  }

  async function toggleStatus(row: AiModelVO, enabled: boolean) {
    if (row.isDefault === 1 && !enabled) {
      ElMessage.warning('默认模型不能停用，请先设置其他默认模型')
      return
    }
    await updateAiModel({
      id: row.id,
      name: row.name,
      provider: row.provider,
      protocol: row.protocol || 'openai',
      modelType: row.modelType || 'chat',
      modelName: row.modelName,
      baseUrl: row.baseUrl,
      pathTemplate: row.pathTemplate,
      extraHeaders: row.extraHeaders,
      temperature: row.temperature,
      maxTokens: row.maxTokens,
      dimension: row.dimension,
      batchSize: row.batchSize,
      systemPrompt: row.systemPrompt,
      isDefault: row.isDefault,
      status: enabled ? 1 : 0,
      remark: row.remark,
    })
    ElMessage.success(enabled ? '已启用' : '已停用')
    loadData()
  }

  async function handleDelete(row: AiModelVO) {
    await openAlertDialog({
      title: '永久删除模型配置',
      targetName: `${row.name} (${row.modelName})`,
      message: '删除后该模型配置与已加密保存的 API Key 将被永久移除，不可恢复。',
      severity: 'irreversible',
      confirmText: '永久删除',
      onConfirm: async () => {
        await deleteAiModel(row.id)
        ElMessage.success('已永久删除')
        loadData()
      },
    })
  }

  onMounted(() => loadData())

  return {
    canUpdate,
    loading,
    total,
    list,
    testingId,
    testResults,
    queryParams,
    dialogVisible,
    submitting,
    formTesting,
    fetchingModels,
    formRef,
    editHasKey,
    editIsDefault,
    form,
    effectiveBaseUrlPlaceholder,
    rules,
    modelOptions,
    loadData,
    handleQuery,
    resetQuery,
    selectProvider,
    onProtocolChange,
    openDialog,
    resetForm,
    handleSubmit,
    handleTest,
    handleFormTest,
    handleFetchModels,
    handleSetDefault,
    toggleStatus,
    handleDelete,
  }
}
