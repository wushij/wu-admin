<template>
  <div class="app-container">
    <el-card class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="字典名称">
          <el-input v-model="queryParams.dictName" placeholder="请输入字典名称" clearable />
        </el-form-item>
        <el-form-item label="字典类型">
          <el-input v-model="queryParams.dictType" placeholder="请输入字典类型" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>字典类型列表</span>
          <el-button
            v-permission="'system:dict:create'"
            type="primary"
            @click="handleAdd"
          >
            新增字典
          </el-button>
        </div>
      </template>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        :header-cell-style="{ textAlign: 'center' }"
        :cell-style="{ textAlign: 'center' }"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="dictName" label="字典名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="dictType" label="字典类型" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="handleViewData(row)">字典数据</el-button>
              <el-button
                v-permission="'system:dict:update'"
                type="primary"
                size="small"
                @click="handleEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-permission="'system:dict:delete'"
                type="danger"
                size="small"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 字典类型 -->
    <el-dialog
      v-model="typeDialogVisible"
      :title="typeDialogTitle"
      width="500px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="90px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input
            v-model="typeForm.dictType"
            placeholder="请输入字典类型"
            :disabled="!!typeForm.id"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="typeForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" placeholder="请输入备注" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="typeSubmitting" @click="submitType">确定</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据列表 -->
    <el-dialog
      v-model="dataListVisible"
      title="字典数据"
      width="900px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <div class="data-toolbar">
        <el-button
          v-permission="'system:dict:create'"
          type="primary"
          size="small"
          @click="handleAddData"
        >
          新增数据
        </el-button>
      </div>
      <el-table
        :data="dictDataList"
        v-loading="dataLoading"
        border
        stripe
        size="small"
        :header-cell-style="{ textAlign: 'center' }"
        :cell-style="{ textAlign: 'center' }"
      >
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column prop="dictLabel" label="字典标签" min-width="110" />
        <el-table-column prop="dictValue" label="字典键值" min-width="110" />
        <el-table-column label="回显样式" width="110">
          <template #default="{ row }">
            <el-tag :type="listClassTagType(row.listClass)" size="small">
              {{ row.dictLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
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
    </el-dialog>

    <!-- 字典数据表单 -->
    <el-dialog
      v-model="dataFormVisible"
      :title="dataFormTitle"
      width="500px"
      destroy-on-close
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="90px">
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" placeholder="请输入字典标签" />
        </el-form-item>
        <el-form-item label="字典键值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" placeholder="请输入字典键值" />
        </el-form-item>
        <el-form-item label="显示排序" prop="sort">
          <el-input-number v-model="dataForm.sort" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="回显样式">
          <el-select v-model="dataForm.listClass" placeholder="请选择回显样式" style="width: 100%">
            <el-option label="默认" value="default" />
            <el-option label="成功" value="success" />
            <el-option label="警告" value="warning" />
            <el-option label="错误" value="danger" />
            <el-option label="信息" value="info" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="dataForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" type="textarea" placeholder="请输入备注" :rows="3" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageDictType,
  createDictType,
  updateDictType,
  deleteDictType,
  listDictDataForManage,
  createDictData,
  updateDictData,
  deleteDictData
} from '@/api/system/dict'
import { clearDictCache } from '@/composables/useDict'

const loading = ref(false)
const total = ref(0)
const tableData = ref([])

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  dictName: '',
  dictType: '',
  status: null
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
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }]
}

const dataListVisible = ref(false)
const dictDataList = ref([])
const dataLoading = ref(false)
const currentDictType = ref('')

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

function listClassTagType(listClass) {
  const map = {
    default: 'info',
    success: 'success',
    warning: 'warning',
    danger: 'danger',
    error: 'danger',
    info: 'info',
    primary: 'primary'
  }
  return map[listClass || 'default'] || 'info'
}

async function loadData() {
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
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNo = 1
  loadData()
}

function resetQuery() {
  queryParams.dictName = ''
  queryParams.dictType = ''
  queryParams.status = null
  handleQuery()
}

function handleAdd() {
  typeDialogTitle.value = '新增字典类型'
  Object.assign(typeForm, { id: undefined, dictName: '', dictType: '', status: 1, remark: '' })
  typeDialogVisible.value = true
}

function handleEdit(row) {
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
    typeDialogVisible.value = false
    loadData()
  } finally {
    typeSubmitting.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定要删除字典类型「${row.dictName}」吗？`, '提示', { type: 'warning' })
  await deleteDictType(row.id)
  clearDictCache(row.dictType)
  ElMessage.success('删除成功')
  loadData()
}

async function handleViewData(row) {
  currentDictType.value = row.dictType
  dataListVisible.value = true
  await loadDictData()
}

async function loadDictData() {
  dataLoading.value = true
  try {
    const res = await listDictDataForManage(currentDictType.value)
    dictDataList.value = res.data || []
  } finally {
    dataLoading.value = false
  }
}

function handleAddData() {
  dataFormTitle.value = '新增字典数据'
  Object.assign(dataForm, {
    id: undefined,
    sort: 0,
    dictLabel: '',
    dictValue: '',
    dictType: currentDictType.value,
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
    loadDictData()
  } finally {
    dataSubmitting.value = false
  }
}

async function handleDeleteData(row) {
  await ElMessageBox.confirm(`确定要删除字典数据「${row.dictLabel}」吗？`, '提示', { type: 'warning' })
  await deleteDictData(row.id)
  clearDictCache(row.dictType)
  ElMessage.success('删除成功')
  loadDictData()
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.search-card {
  margin-bottom: 12px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.data-toolbar {
  margin-bottom: 12px;
}
.el-pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
