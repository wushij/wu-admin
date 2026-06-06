import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { DictDataItem } from '@/types/api'
import type { DictTypeVO, DictTypeSaveDTO, DictDataSaveDTO, DictTypePageQuery } from '@/api/system/dict'
import {
  pageDictType,
  createDictType,
  updateDictType,
  deleteDictType,
  copyDictType,
  listDictDataForManage,
  createDictData,
  updateDictData,
  deleteDictData,
  refreshDictCache,
} from '@/api/system/dict'
import { clearDictCache, preloadDicts, reloadDictTypes } from '@/composables/useDict'
import { COMMON_DICT_TYPES } from '@/constants/dict'

export function useDictPage() {
  const loading = ref(false)
  const total = ref(0)
  const tableData = ref<DictTypeVO[]>([])
  const typeFilter = ref('')
  const selectedType = ref<DictTypeVO | null>(null)

  const queryParams = reactive<DictTypePageQuery>({
    pageNo: 1,
    pageSize: 10,
    dictName: '',
    dictType: '',
    status: null,
  })

  const filteredTypes = computed(() => {
    const kw = typeFilter.value.trim().toLowerCase()
    if (!kw) return tableData.value
    return tableData.value.filter(
      (r) =>
        (r.dictName && r.dictName.toLowerCase().includes(kw)) ||
        (r.dictType && r.dictType.toLowerCase().includes(kw))
    )
  })

  const typeDialogVisible = ref(false)
  const typeDialogTitle = ref('新增字典类型')
  const typeSubmitting = ref(false)
  const typeFormRef = ref()
  const typeForm = reactive<DictTypeSaveDTO>({
    id: undefined,
    dictName: '',
    dictType: '',
    status: 1,
    remark: '',
  })
  const typeRules = {
    dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
    dictType: [
      { required: true, message: '请输入字典类型', trigger: 'blur' },
      {
        pattern: /^[a-z][a-z0-9_]*$/,
        message: '仅小写字母、数字、下划线，且以字母开头',
        trigger: 'blur',
      },
    ],
  }

  const dictDataList = ref<DictDataItem[]>([])
  const dataLoading = ref(false)
  const dataFilter = ref('')

  const filteredDataList = computed(() => {
    const kw = dataFilter.value.trim().toLowerCase()
    if (!kw) return dictDataList.value
    return dictDataList.value.filter(
      (r) =>
        (r.dictLabel && r.dictLabel.toLowerCase().includes(kw)) ||
        (r.dictValue && String(r.dictValue).toLowerCase().includes(kw))
    )
  })

  const dataFormVisible = ref(false)
  const dataFormTitle = ref('新增字典数据')
  const dataSubmitting = ref(false)
  const dataFormRef = ref()
  const dataForm = reactive<DictDataSaveDTO>({
    id: undefined,
    sort: 0,
    dictLabel: '',
    dictValue: '',
    dictType: '',
    listClass: 'default',
    isDefault: 0,
    status: 1,
    remark: '',
  })
  const dataRules = {
    dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
    dictValue: [{ required: true, message: '请输入字典键值', trigger: 'blur' }],
  }

  async function loadTypes() {
    loading.value = true
    try {
      const res = await pageDictType({
        pageNo: queryParams.pageNo,
        pageSize: queryParams.pageSize,
        dictName: queryParams.dictName || undefined,
        dictType: queryParams.dictType || undefined,
        status: queryParams.status,
      })
      tableData.value = res.data?.list || []
      total.value = Number(res.data?.total) || 0
      if (selectedType.value) {
        const currentId = selectedType.value.id
        const hit = tableData.value.find((r) => r.id === currentId)
        if (hit) {
          selectedType.value = hit
          await loadDictData()
        } else {
          selectedType.value = tableData.value[0] || null
          if (selectedType.value) await loadDictData()
        }
      } else if (tableData.value.length) {
        selectType(tableData.value[0])
      }
    } finally {
      loading.value = false
    }
  }

  function selectType(row: DictTypeVO) {
    selectedType.value = row
    dataFilter.value = ''
    loadDictData()
  }

  function handleQuery() {
    queryParams.pageNo = 1
    selectedType.value = null
    loadTypes()
  }

  function resetQuery() {
    queryParams.dictName = ''
    queryParams.dictType = ''
    queryParams.status = null
    handleQuery()
  }

  function handleAddType() {
    typeDialogTitle.value = '新增字典类型'
    Object.assign(typeForm, { id: undefined, dictName: '', dictType: '', status: 1, remark: '' })
    typeDialogVisible.value = true
  }

  function handleEditType(row: DictTypeVO) {
    typeDialogTitle.value = '编辑字典类型'
    Object.assign(typeForm, {
      id: row.id,
      dictName: row.dictName,
      dictType: row.dictType,
      status: row.status,
      remark: row.remark || '',
    })
    typeDialogVisible.value = true
  }

  async function afterDictChanged(...types: (string | undefined)[]) {
    const merged = [...new Set([...types.filter((t): t is string => Boolean(t)), ...COMMON_DICT_TYPES])]
    await reloadDictTypes(merged)
  }

  async function submitType() {
    await typeFormRef.value?.validate()
    typeSubmitting.value = true
    try {
      if (typeForm.id) {
        await updateDictType({ ...typeForm })
        ElMessage.success('更新成功')
      } else {
        await createDictType({ ...typeForm })
        ElMessage.success('创建成功')
      }
      clearDictCache(typeForm.dictType)
      typeDialogVisible.value = false
      await loadTypes()
      await afterDictChanged(typeForm.dictType)
    } finally {
      typeSubmitting.value = false
    }
  }

  async function handleDeleteType(row: DictTypeVO) {
    const n = row.dataCount ?? 0
    await ElMessageBox.confirm(
      `确定删除字典类型「${row.dictName}」吗？将同时删除其下 ${n} 条字典数据，且业务表单将无法再加载该字典。`,
      '提示',
      { type: 'warning' }
    )
    await deleteDictType(row.id)
    if (selectedType.value?.id === row.id) selectedType.value = null
    ElMessage.success('删除成功')
    await loadTypes()
    await afterDictChanged(row.dictType)
  }

  async function handleCopyType() {
    if (!selectedType.value) return
    await ElMessageBox.confirm(`复制「${selectedType.value.dictName}」及其全部数据？`, '提示', {
      type: 'info',
    })
    await copyDictType(selectedType.value.id)
    ElMessage.success('复制成功')
    await loadTypes()
    await afterDictChanged()
  }

  async function handleRefreshCache() {
    await refreshDictCache()
    clearDictCache()
    await preloadDicts(COMMON_DICT_TYPES)
    if (selectedType.value) await reloadDictTypes([selectedType.value.dictType])
    ElMessage.success('服务端与本地字典缓存已刷新')
  }

  async function loadDictData() {
    if (!selectedType.value) return
    dataLoading.value = true
    try {
      const res = await listDictDataForManage(selectedType.value.dictType)
      dictDataList.value = res.data || []
    } finally {
      dataLoading.value = false
    }
  }

  function handleAddData() {
    if (!selectedType.value) return
    dataFormTitle.value = '新增字典数据'
    Object.assign(dataForm, {
      id: undefined,
      sort: dictDataList.value.length,
      dictLabel: '',
      dictValue: '',
      dictType: selectedType.value.dictType,
      listClass: 'default',
      isDefault: 0,
      status: 1,
      remark: '',
    })
    dataFormVisible.value = true
  }

  function handleEditData(row: DictDataItem) {
    dataFormTitle.value = '编辑字典数据'
    Object.assign(dataForm, {
      id: row.id,
      sort: row.sort ?? 0,
      dictLabel: row.dictLabel,
      dictValue: row.dictValue,
      dictType: row.dictType,
      listClass: row.listClass || 'default',
      isDefault: row.isDefault ?? 0,
      status: row.status ?? 1,
      remark: row.remark || '',
    })
    dataFormVisible.value = true
  }

  async function submitData() {
    await dataFormRef.value?.validate()
    dataSubmitting.value = true
    try {
      if (dataForm.id) {
        await updateDictData({ ...dataForm })
        ElMessage.success('更新成功')
      } else {
        await createDictData({ ...dataForm })
        ElMessage.success('创建成功')
      }
      dataFormVisible.value = false
      await loadDictData()
      await loadTypes()
      await afterDictChanged(dataForm.dictType)
    } finally {
      dataSubmitting.value = false
    }
  }

  async function handleDeleteData(row: DictDataItem) {
    if (row.id == null) return
    await ElMessageBox.confirm(`确定要删除字典数据「${row.dictLabel}」吗？`, '提示', { type: 'warning' })
    await deleteDictData(row.id)
    ElMessage.success('删除成功')
    await loadDictData()
    await loadTypes()
    await afterDictChanged(row.dictType)
  }

  onMounted(async () => {
    await preloadDicts(COMMON_DICT_TYPES)
    loadTypes()
  })

  return {
    loading,
    total,
    typeFilter,
    selectedType,
    queryParams,
    filteredTypes,
    typeDialogVisible,
    typeDialogTitle,
    typeSubmitting,
    typeFormRef,
    typeForm,
    typeRules,
    dictDataList,
    dataLoading,
    dataFilter,
    filteredDataList,
    dataFormVisible,
    dataFormTitle,
    dataSubmitting,
    dataFormRef,
    dataForm,
    dataRules,
    loadTypes,
    selectType,
    handleQuery,
    resetQuery,
    handleAddType,
    handleEditType,
    submitType,
    handleDeleteType,
    handleCopyType,
    handleRefreshCache,
    handleAddData,
    handleEditData,
    submitData,
    handleDeleteData,
  }
}
