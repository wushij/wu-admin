import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { listDictType } from '@/api/system/dict'
import {
  dbTableList,
  importGenTables,
  pageGenTable,
  getGenTable,
  updateGenTable,
  deleteGenTable,
  previewGenCode,
  previewGenerateFiles,
  generateToProject,
  previewRemoveFiles,
  removeGeneratedCode,
  syncGenTable,
  downloadGenCodeUrl,
  type GenTable,
  type DatabaseTable,
} from '@/api/tool/gen'

const JAVA_TYPES = ['Long', 'Integer', 'String', 'Double', 'BigDecimal', 'LocalDateTime', 'LocalDate', 'Boolean']
const QUERY_TYPES = [
  { label: '=', value: 'EQ' },
  { label: '!=', value: 'NE' },
  { label: '>', value: 'GT' },
  { label: '<', value: 'LT' },
  { label: 'LIKE', value: 'LIKE' },
]
const HTML_TYPES = [
  { label: '文本框', value: 'input' },
  { label: '文本域', value: 'textarea' },
  { label: '下拉框', value: 'select' },
  { label: '日期时间', value: 'datetime' },
]

export function useGenPage() {
  const loading = ref(false)
  const total = ref(0)
  const tableData = ref<GenTable[]>([])
  const selectedIds = ref<number[]>([])
  const queryParams = reactive({ pageNo: 1, pageSize: 10, tableName: '' })

  const importVisible = ref(false)
  const importLoading = ref(false)
  const dbTables = ref<DatabaseTable[]>([])
  const selectedTableNames = ref<string[]>([])
  const importQuery = reactive({ pageNo: 1, pageSize: 10, tableName: '' })
  const importTotal = ref(0)

  const editVisible = ref(false)
  const editForm = reactive<GenTable>({
    tableName: '', tableComment: '', className: '', packageName: '', moduleName: '',
    businessName: '', functionName: '', author: '', genType: 'crud', frontType: 'element-plus',
    formLayout: 'vertical', columns: [],
  })
  const dictTypeOptions = ref<{ label: string; value: string }[]>([])

  const previewVisible = ref(false)
  const previewTab = ref('')
  const previewCodes = ref<Record<string, string>>({})

  const generateVisible = ref(false)
  const generateType = ref<'project' | 'download' | ''>('')
  const generateLoading = ref(false)
  const currentGenId = ref<number | null>(null)

  const previewFilesVisible = ref(false)
  const previewFiles = ref<string[]>([])
  const previewAction = ref<'generate' | 'remove'>('generate')
  const previewFilesLoading = ref(false)

  const resultVisible = ref(false)
  const resultType = ref<'generate' | 'remove'>('generate')
  const resultFiles = ref<string[]>([])

  async function loadData() {
    loading.value = true
    try {
      const res = await pageGenTable(queryParams)
      tableData.value = res.data?.list || []
      total.value = res.data?.total || 0
    } finally {
      loading.value = false
    }
  }

  function handleQuery() {
    queryParams.pageNo = 1
    loadData()
  }

  function resetQuery() {
    queryParams.tableName = ''
    handleQuery()
  }

  function onSelectionChange(rows: GenTable[]) {
    selectedIds.value = rows.map((r) => r.id!).filter(Boolean)
  }

  async function openImport() {
    importVisible.value = true
    selectedTableNames.value = []
    await loadDbTables()
  }

  async function loadDbTables() {
    importLoading.value = true
    try {
      const res = await dbTableList(importQuery)
      dbTables.value = res.data?.list || []
      importTotal.value = res.data?.total || 0
    } finally {
      importLoading.value = false
    }
  }

  async function handleImport() {
    if (!selectedTableNames.value.length) return
    await importGenTables(selectedTableNames.value)
    ElMessage.success('导入成功')
    importVisible.value = false
    loadData()
  }

  async function openEdit(row: GenTable) {
    const res = await getGenTable(row.id!)
    Object.assign(editForm, res.data || {})
    editForm.columns = res.data?.columns || []
    if (!dictTypeOptions.value.length) {
      const dictRes = await listDictType()
      dictTypeOptions.value = (dictRes.data || []).map((d) => ({
        label: `${d.dictName} (${d.dictType})`,
        value: d.dictType,
      }))
    }
    editVisible.value = true
  }

  async function saveEdit() {
    await updateGenTable(editForm)
    ElMessage.success('保存成功')
    editVisible.value = false
    loadData()
  }

  async function handleDelete(row: GenTable) {
    await ElMessageBox.confirm(`确认删除表「${row.tableName}」的配置？`, '提示', { type: 'warning' })
    await deleteGenTable([row.id!])
    ElMessage.success('删除成功')
    loadData()
  }

  async function batchDelete() {
    if (!selectedIds.value.length) return
    await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 项？`, '提示', { type: 'warning' })
    await deleteGenTable(selectedIds.value)
    ElMessage.success('删除成功')
    selectedIds.value = []
    loadData()
  }

  async function handlePreview(row: GenTable) {
    const res = await previewGenCode(row.id!)
    previewCodes.value = res.data || {}
    previewTab.value = Object.keys(previewCodes.value)[0] || ''
    previewVisible.value = true
  }

  function openGenerate(row: GenTable) {
    currentGenId.value = row.id!
    generateType.value = ''
    generateVisible.value = true
  }

  function batchGenerate() {
    if (selectedIds.value.length === 1) {
      openGenerate({ id: selectedIds.value[0] } as GenTable)
    } else if (selectedIds.value.length > 1) {
      generateType.value = 'download'
      currentGenId.value = null
      generateVisible.value = true
    }
  }

  async function confirmGenerate() {
    if (!generateType.value) return
    if (generateType.value === 'download') {
      const ids = currentGenId.value ? [currentGenId.value] : selectedIds.value
      await downloadZip(ids)
      generateVisible.value = false
      return
    }
    if (!currentGenId.value) return
    previewAction.value = 'generate'
    previewFilesLoading.value = true
    previewFilesVisible.value = true
    try {
      const res = await previewGenerateFiles(currentGenId.value)
      previewFiles.value = res.data || []
    } finally {
      previewFilesLoading.value = false
    }
  }

  async function executeGenerateOrRemove() {
    if (!currentGenId.value) return
    generateLoading.value = true
    try {
      if (previewAction.value === 'generate') {
        const res = await generateToProject(currentGenId.value, true)
        resultFiles.value = res.data || []
        resultType.value = 'generate'
      } else {
        const res = await removeGeneratedCode(currentGenId.value)
        resultFiles.value = res.data || []
        resultType.value = 'remove'
      }
      previewFilesVisible.value = false
      generateVisible.value = false
      resultVisible.value = true
    } finally {
      generateLoading.value = false
    }
  }

  async function handleRemoveCode(row: GenTable) {
    currentGenId.value = row.id!
    previewAction.value = 'remove'
    previewFilesLoading.value = true
    previewFilesVisible.value = true
    try {
      const res = await previewRemoveFiles(row.id!)
      previewFiles.value = res.data || []
    } finally {
      previewFilesLoading.value = false
    }
  }

  async function handleSync(row: GenTable) {
    await ElMessageBox.confirm(
      `将从数据库重新读取表「${row.tableName}」的字段结构并更新生成配置：新增字段会自动加入，已删字段会从配置中移除，已有字段的类型会更新。不会修改业务表数据。`,
      '同步确认',
      { type: 'warning', confirmButtonText: '确认同步', cancelButtonText: '取消' },
    )
    await syncGenTable(row.id!)
    ElMessage.success('同步成功')
    loadData()
  }

  async function downloadZip(ids: number[]) {
    const token = localStorage.getItem('token')
    const res = await axios.get(downloadGenCodeUrl(ids), {
      responseType: 'blob',
      headers: token ? { Authorization: token } : {},
      withCredentials: true,
    })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = 'code.zip'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('下载已开始')
  }

  const hasModifiedPreviewFiles = computed(() =>
    previewFiles.value.some((f) => f.includes('已修改')),
  )

  function codeLanguage(name: string) {
    if (name.endsWith('.java')) return 'java'
    if (name.endsWith('.ts')) return 'typescript'
    if (name.endsWith('.vue')) return 'html'
    return 'plaintext'
  }

  return {
    loading, total, tableData, selectedIds, queryParams,
    importVisible, importLoading, dbTables, selectedTableNames, importQuery, importTotal,
    editVisible, editForm, dictTypeOptions,
    previewVisible, previewTab, previewCodes,
    generateVisible, generateType, generateLoading, currentGenId,
    previewFilesVisible, previewFiles, previewAction, previewFilesLoading, hasModifiedPreviewFiles,
    resultVisible, resultType, resultFiles,
    JAVA_TYPES, QUERY_TYPES, HTML_TYPES,
    loadData, handleQuery, resetQuery, onSelectionChange,
    openImport, loadDbTables, handleImport,
    openEdit, saveEdit, handleDelete, batchDelete,
    handlePreview, openGenerate, batchGenerate, confirmGenerate, executeGenerateOrRemove,
    handleRemoveCode, handleSync, codeLanguage,
  }
}
