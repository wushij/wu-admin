<template>
  <div class="app-container dict-page">
    <el-card class="search-card" shadow="never">
      <el-form :model="queryParams" inline>
        <el-form-item label="字典名称">
          <el-input v-model="queryParams.dictName" placeholder="请输入字典名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="字典类型">
          <el-input v-model="queryParams.dictType" placeholder="编码，如 sys_xxx" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect
            v-model="queryParams.status"
            :dict-type="DICT_TYPE.NORMAL_DISABLE"
            value-type="number"
            placeholder="全部状态"
            width="120px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <p class="dict-hint">
        左侧选择字典类型，右侧维护选项；业务表单通过
        <code>DictSelect</code> / <code>DictTag</code> 引用类型编码（如 <code>sys_normal_disable</code>），修改后请点击「刷新缓存」。
      </p>
    </el-card>

    <div class="dict-layout">
      <!-- 左侧：字典类型 -->
      <el-card class="dict-type-card" shadow="never" v-loading="loading">
        <template #header>
          <div class="card-header">
            <span>字典类型</span>
            <el-button v-permission="'system:dict:create'" type="primary" size="small" @click="handleAddType">
              新增
            </el-button>
          </div>
        </template>
        <el-input v-model="typeFilter" placeholder="筛选名称/编码" clearable class="type-filter" />
        <div class="type-list">
          <div
            v-for="row in filteredTypes"
            :key="row.id"
            class="type-item"
            :class="{ active: selectedType?.id === row.id }"
            @click="selectType(row)"
          >
            <div class="type-item-main">
              <span class="type-name">{{ row.dictName }}</span>
              <el-tag size="small" type="info">{{ row.dataCount ?? 0 }} 项</el-tag>
            </div>
            <div class="type-code">{{ row.dictType }}</div>
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" class="type-status">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </div>
          <el-empty v-if="!filteredTypes.length" description="暂无字典类型" :image-size="64" />
        </div>
        <el-pagination
          v-model:current-page="queryParams.pageNo"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          small
          layout="total, prev, pager, next"
          class="type-pager"
          @current-change="loadTypes"
          @size-change="loadTypes"
        />
      </el-card>

      <!-- 右侧：字典数据 -->
      <el-card class="dict-data-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span v-if="selectedType">
              {{ selectedType.dictName }}
              <el-text type="info" size="small" class="header-code">（{{ selectedType.dictType }}）</el-text>
            </span>
            <span v-else class="text-muted">请从左侧选择字典类型</span>
            <div v-if="selectedType" class="header-actions">
              <el-button size="small" @click="handleRefreshCache">刷新缓存</el-button>
              <el-button
                v-permission="'system:dict:copy'"
                size="small"
                @click="handleCopyType"
              >
                复制类型
              </el-button>
              <el-button
                v-permission="'system:dict:update'"
                size="small"
                @click="handleEditType(selectedType)"
              >
                编辑类型
              </el-button>
              <el-button
                v-permission="'system:dict:delete'"
                type="danger"
                size="small"
                @click="handleDeleteType(selectedType)"
              >
                删除类型
              </el-button>
              <el-button
                v-permission="'system:dict:create'"
                type="primary"
                size="small"
                @click="handleAddData"
              >
                新增数据
              </el-button>
            </div>
          </div>
        </template>

        <template v-if="selectedType">
          <el-input
            v-model="dataFilter"
            placeholder="筛选标签或键值"
            clearable
            class="data-filter"
          />
          <el-table
            :data="filteredDataList"
            v-loading="dataLoading"
            border
            stripe
            size="small"
            :header-cell-style="{ textAlign: 'center' }"
            :cell-style="{ textAlign: 'center' }"
          >
            <el-table-column prop="sort" label="排序" width="70" />
            <el-table-column prop="dictLabel" label="字典标签" min-width="110" />
            <el-table-column prop="dictValue" label="字典键值" min-width="100" />
            <el-table-column label="回显" width="100">
              <template #default="{ row }">
                <el-tag :type="listClassToTagType(row.listClass)" size="small">{{ row.dictLabel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="默认" width="70">
              <template #default="{ row }">
                <el-tag v-if="row.isDefault === 1" type="warning" size="small">默认</el-tag>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <DictTag :value="row.status" :dict-type="DICT_TYPE.NORMAL_DISABLE" />
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
            <el-table-column label="操作" width="150" fixed="right" align="center">
              <template #default="{ row }">
                <div class="action-buttons">
                  <el-button
                    v-permission="'system:dict:update'"
                    type="primary"
                    size="small"
                    @click="handleEditData(row)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-permission="'system:dict:delete'"
                    type="danger"
                    size="small"
                    @click="handleDeleteData(row)"
                  >
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </template>
        <el-empty v-else description="选择左侧字典类型后在此维护选项" />
      </el-card>
    </div>

    <!-- 字典类型表单 -->
    <el-dialog
      v-model="typeDialogVisible"
      :title="typeDialogTitle"
      width="520px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="96px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="如：系统状态" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input
            v-model="typeForm.dictType"
            placeholder="小写+下划线，如 sys_order_status"
            :disabled="!!typeForm.id"
          />
          <div v-if="!typeForm.id" class="form-tip">创建后不可修改；业务代码用此编码引用字典</div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <DictSelect
            v-model="typeForm.status"
            :dict-type="DICT_TYPE.NORMAL_DISABLE"
            value-type="number"
            :clearable="false"
            width="160px"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="typeSubmitting" @click="submitType">确定</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据表单 -->
    <el-dialog
      v-model="dataFormVisible"
      :title="dataFormTitle"
      width="520px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="96px">
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" placeholder="界面展示文字" />
        </el-form-item>
        <el-form-item label="字典键值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" placeholder="存入数据库的值" />
        </el-form-item>
        <el-form-item label="显示排序" prop="sort">
          <el-input-number v-model="dataForm.sort" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="回显样式">
          <el-select v-model="dataForm.listClass" placeholder="列表/标签颜色" style="width: 100%">
            <el-option label="默认" value="default" />
            <el-option label="成功" value="success" />
            <el-option label="警告" value="warning" />
            <el-option label="错误" value="danger" />
            <el-option label="信息" value="info" />
            <el-option label="主色" value="primary" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否默认">
          <el-radio-group v-model="dataForm.isDefault">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect
            v-model="dataForm.status"
            :dict-type="DICT_TYPE.NORMAL_DISABLE"
            value-type="number"
            :clearable="false"
            width="160px"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="dataSubmitting" @click="submitData">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageDictType,
  createDictType,
  updateDictType,
  deleteDictType,
  copyDictType,
  listDictDataForManage,
  createDictData,
  updateDictData,
  deleteDictData
} from '@/api/system/dict'
import { clearDictCache, preloadDicts, listClassToTagType } from '@/composables/useDict'
import { DICT_TYPE, COMMON_DICT_TYPES } from '@/constants/dict'

const loading = ref(false)
const total = ref(0)
const tableData = ref([])
const typeFilter = ref('')
const selectedType = ref(null)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  dictName: '',
  dictType: '',
  status: null
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
const typeForm = reactive({
  id: undefined,
  dictName: '',
  dictType: '',
  status: 1,
  remark: ''
})
const typeRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [
    { required: true, message: '请输入字典类型', trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9_]*$/,
      message: '仅小写字母、数字、下划线，且以字母开头',
      trigger: 'blur'
    }
  ]
}

const dictDataList = ref([])
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
const dataForm = reactive({
  id: undefined,
  sort: 0,
  dictLabel: '',
  dictValue: '',
  dictType: '',
  listClass: 'default',
  isDefault: 0,
  status: 1,
  remark: ''
})
const dataRules = {
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典键值', trigger: 'blur' }]
}

async function loadTypes() {
  loading.value = true
  try {
    const res = await pageDictType({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      dictName: queryParams.dictName || undefined,
      dictType: queryParams.dictType || undefined,
      status: queryParams.status
    })
    tableData.value = res.data?.list || []
    total.value = Number(res.data?.total) || 0
    if (selectedType.value) {
      const hit = tableData.value.find((r) => r.id === selectedType.value.id)
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

function selectType(row) {
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

function handleEditType(row) {
  typeDialogTitle.value = '编辑字典类型'
  Object.assign(typeForm, {
    id: row.id,
    dictName: row.dictName,
    dictType: row.dictType,
    status: row.status,
    remark: row.remark || ''
  })
  typeDialogVisible.value = true
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
    COMMON_DICT_TYPES.forEach((t) => clearDictCache(t))
    typeDialogVisible.value = false
    await loadTypes()
  } finally {
    typeSubmitting.value = false
  }
}

async function handleDeleteType(row) {
  const n = row.dataCount ?? 0
  await ElMessageBox.confirm(
    `确定删除字典类型「${row.dictName}」吗？将同时删除其下 ${n} 条字典数据，且业务表单将无法再加载该字典。`,
    '提示',
    { type: 'warning' }
  )
  await deleteDictType(row.id)
  clearDictCache(row.dictType)
  if (selectedType.value?.id === row.id) selectedType.value = null
  ElMessage.success('删除成功')
  loadTypes()
}

async function handleCopyType() {
  if (!selectedType.value) return
  await ElMessageBox.confirm(`复制「${selectedType.value.dictName}」及其全部数据？`, '提示', {
    type: 'info'
  })
  await copyDictType(selectedType.value.id)
  ElMessage.success('复制成功')
  clearDictCache()
  await loadTypes()
}

async function handleRefreshCache() {
  clearDictCache()
  await preloadDicts(COMMON_DICT_TYPES)
  if (selectedType.value) clearDictCache(selectedType.value.dictType)
  ElMessage.success('字典缓存已刷新')
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
    remark: ''
  })
  dataFormVisible.value = true
}

function handleEditData(row) {
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
    remark: row.remark || ''
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
    clearDictCache(dataForm.dictType)
    dataFormVisible.value = false
    await loadDictData()
    await loadTypes()
  } finally {
    dataSubmitting.value = false
  }
}

async function handleDeleteData(row) {
  await ElMessageBox.confirm(`确定要删除字典数据「${row.dictLabel}」吗？`, '提示', { type: 'warning' })
  await deleteDictData(row.id)
  clearDictCache(row.dictType)
  ElMessage.success('删除成功')
  await loadDictData()
  await loadTypes()
}

onMounted(async () => {
  await preloadDicts(COMMON_DICT_TYPES)
  loadTypes()
})
</script>

<style scoped lang="scss">
.search-card {
  margin-bottom: 12px;
}
.dict-hint {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  code {
    padding: 0 4px;
    background: var(--el-fill-color-light);
    border-radius: 4px;
  }
}
.dict-layout {
  display: flex;
  gap: 12px;
  min-height: 520px;
}
.dict-type-card {
  flex: 0 0 320px;
  display: flex;
  flex-direction: column;
}
.dict-data-card {
  flex: 1;
  min-width: 0;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.header-code {
  margin-left: 4px;
}
.type-filter,
.data-filter {
  margin-bottom: 12px;
}
.type-list {
  flex: 1;
  overflow-y: auto;
  max-height: 420px;
}
.type-item {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid transparent;
  cursor: pointer;
  margin-bottom: 6px;
  transition: background 0.15s;
  &:hover {
    background: var(--el-fill-color-light);
  }
  &.active {
    background: var(--el-color-primary-light-9);
    border-color: var(--el-color-primary-light-5);
  }
}
.type-item-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.type-name {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.type-code {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  word-break: break-all;
}
.type-status {
  margin-top: 6px;
}
.type-pager {
  margin-top: 12px;
  justify-content: center;
}
.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
  margin-top: 4px;
}
.text-muted {
  color: var(--el-text-color-secondary);
}
</style>
