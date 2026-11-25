<template>
  <div class="app-container module-page">
    <PageHeader
      title="部门管理"
      description="维护组织部门树结构，支持负责人与联系方式配置"
      :icon="MODULE_PAGE_ICON.org"
      :stats="[{ label: '部门总数', value: deptCount }]"
    />

    <el-card class="search-card module-search-card" shadow="never">
      <el-form :model="queryParams" inline class="module-search-form">
        <el-form-item label="部门名称">
          <el-input v-model="queryParams.name" placeholder="请输入部门名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect
            v-model="queryParams.status"
            dict-type="sys_normal_disable"
            value-type="number"
            placeholder="请选择状态"
            width="150px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>部门列表</span>
          <div class="header-actions">
            <el-button v-permission="'system:dept:delete'" @click="openRecycleDialog">回收站</el-button>
            <el-button type="primary" v-permission="'system:dept:create'" @click="handleAdd()">新增部门</el-button>
          </div>
        </div>
      </template>

      <el-table
        v-if="deptList.length || loading"
        :data="deptList"
        v-loading="loading"
        row-key="id"
        border
        :tree-props="{ children: 'children' }"
        :header-cell-style="tableHeaderStyle"
        :cell-style="tableCellStyle"
      >
        <el-table-column prop="name" label="部门名称" width="200" align="center" header-align="center" />
        <el-table-column prop="leaderName" label="负责人" width="120" align="center" header-align="center" />
        <el-table-column prop="phone" label="联系电话" width="150" align="center" header-align="center" />
        <el-table-column prop="email" label="邮箱" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="80" align="center" header-align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center" header-align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              v-permission="'system:dept:update'"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center" header-align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" v-permission="'system:dept:create'" @click="handleAdd(row)">
                新增
              </el-button>
              <el-button type="primary" size="small" v-permission="'system:dept:update'" @click="handleEdit(row)">
                编辑
              </el-button>
              <el-button
                type="danger"
                plain
                size="small"
                v-permission="'system:dept:delete'"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState v-else title="暂无部门数据" description="点击右上角新增部门构建组织结构" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" :lock-scroll="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级部门" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="deptOptions"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            placeholder="请选择上级部门"
            check-strictly
            clearable
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="负责人" prop="leaderUserId">
          <DeptLeaderSelect v-model="form.leaderUserId" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recycleVisible" title="部门回收站" width="940px" :lock-scroll="false">
      <el-table
        v-if="recycleList.length || recycleLoading"
        :data="recycleList"
        v-loading="recycleLoading"
        border
        stripe
        :header-cell-style="{ textAlign: 'center' }"
        :cell-style="{ textAlign: 'center' }"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="部门名称" width="180" />
        <el-table-column prop="leaderName" label="负责人" width="120" />
        <el-table-column prop="phone" label="联系电话" width="150" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="删除时间" width="180" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" type="primary" plain @click="handleRestore(row)">恢复</el-button>
              <el-button size="small" type="danger" plain @click="handlePermanentDelete(row)">彻底清除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState v-else title="部门回收站为空" />
      <el-pagination
        v-if="recycleTotal > 0"
        v-model:current-page="recycleQuery.pageNo"
        v-model:page-size="recycleQuery.pageSize"
        :total="recycleTotal"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="table-pagination"
        @size-change="getRecycleList"
        @current-change="getRecycleList"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance } from 'element-plus'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import type { DeptVO, DeptSaveDTO, DeptRecycleQuery, DeptTreeQuery } from '@/api/system/dept'
import {
  getDeptList,
  createDept,
  updateDept,
  deleteDept,
  updateDeptStatus,
  getRecycleDeptPage,
  restoreDept,
  deleteDeptPermanent,
} from '@/api/system/dept'
import DeptLeaderSelect from '@/components/DeptLeaderSelect.vue'
import { openAlertDialog } from '@/composables/ui/useAlertDialog'
import { showSnackbar } from '@/composables/ui/useSnackbar'

const tableHeaderStyle = { textAlign: 'center' as const }
const tableCellStyle = { textAlign: 'center' as const }

const loading = ref(false)
const deptList = ref<DeptVO[]>([])
const deptOptions = ref<DeptVO[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance | null>(null)
const recycleVisible = ref(false)
const recycleLoading = ref(false)
const recycleList = ref<DeptVO[]>([])
const recycleTotal = ref(0)

const queryParams = reactive<DeptTreeQuery>({
  name: '',
  status: null,
})
const recycleQuery = reactive<DeptRecycleQuery>({
  pageNo: 1,
  pageSize: 10,
  name: '',
  status: null,
})

const form = reactive<DeptSaveDTO>({
  id: null,
  parentId: null,
  name: '',
  leaderUserId: null as number | null,
  phone: '',
  email: '',
  sort: 0,
  status: 1,
})

const rules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
}

const countDeptNodes = (list: DeptVO[]): number =>
  list.reduce((sum, item) => sum + 1 + countDeptNodes(item.children || []), 0)

const deptCount = computed(() => countDeptNodes(deptList.value))

const getList = async () => {
  loading.value = true
  try {
    const res = await getDeptList()
    const allDepts = res.data || []
    deptList.value = allDepts
    deptOptions.value = [{ id: 0, name: '根部门', children: allDepts }]
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => getList()

const resetQuery = () => {
  queryParams.name = ''
  queryParams.status = null
  handleQuery()
}

const handleAdd = (row?: DeptVO) => {
  resetForm()
  if (row) form.parentId = row.id
  dialogTitle.value = '新增部门'
  dialogVisible.value = true
}

const handleEdit = (row: DeptVO) => {
  resetForm()
  dialogTitle.value = '编辑部门'
  Object.assign(form, row)
  dialogVisible.value = true
}

/** 部门软删除进回收站：直接执行 + Snackbar 一键撤销 */
const handleDelete = async (row: DeptVO) => {
  await deleteDept(row.id)
  getList()
  showSnackbar({
    message: `已将部门「${row.name}」移至回收站`,
    variant: 'info',
    actionLabel: '撤销',
    onAction: async () => {
      await restoreDept(row.id)
      ElMessage.success(`已恢复部门「${row.name}」`)
      getList()
    },
  })
}

const getRecycleList = async () => {
  recycleLoading.value = true
  try {
    const res = await getRecycleDeptPage(recycleQuery)
    recycleList.value = res.data?.list || []
    recycleTotal.value = res.data?.total || 0
  } finally {
    recycleLoading.value = false
  }
}

const openRecycleDialog = async () => {
  recycleQuery.pageNo = 1
  recycleVisible.value = true
  await getRecycleList()
}

const handleRestore = async (row: DeptVO) => {
  await restoreDept(row.id)
  ElMessage.success('恢复成功')
  await getRecycleList()
  await getList()
}

const handlePermanentDelete = async (row: DeptVO) => {
  await openAlertDialog({
    title: '彻底清除部门',
    targetName: row.name,
    message: '彻底清除后该部门数据将从数据库物理删除，不可恢复。',
    severity: 'irreversible',
    confirmText: '彻底清除',
    onConfirm: async () => {
      await deleteDeptPermanent(row.id)
      ElMessage.success('已彻底清除')
      await getRecycleList()
    },
  })
}

const handleStatusChange = async (row: DeptVO) => {
  const nextStatus = row.status
  const prevStatus = nextStatus === 1 ? 0 : 1
  if (nextStatus === 0) {
    const ok = await openAlertDialog({
      title: '禁用部门',
      targetName: row.name,
      message: '禁用后该部门在新建用户或组织筛选中将不可选用。',
      severity: 'warning',
      confirmText: '确认禁用',
      onConfirm: async () => {
        if (row.id == null || nextStatus == null) return
        await updateDeptStatus(row.id, nextStatus)
      },
    })
    if (!ok) row.status = prevStatus
    else ElMessage.success('禁用成功')
    return
  }
  try {
    if (row.id == null || nextStatus == null) return
    await updateDeptStatus(row.id, nextStatus)
    showSnackbar({
      message: `已启用部门「${row.name}」`,
      variant: 'success',
      actionLabel: '撤销',
      onAction: async () => {
        await updateDeptStatus(row.id, prevStatus)
        row.status = prevStatus
      },
    })
  } catch {
    row.status = prevStatus
  }
}

const resetForm = () => {
  form.id = null
  form.parentId = null
  form.name = ''
  form.leaderUserId = null
  form.phone = ''
  form.email = ''
  form.sort = 0
  form.status = 1
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      const payload = {
        ...form,
        leaderUserId: form.leaderUserId ?? null,
      }
      delete (payload as { leaderName?: string }).leaderName
      if (form.id) {
        await updateDept(payload)
        ElMessage.success('修改成功')
      } else {
        await createDept(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      getList()
    }
  })
}

onMounted(() => getList())
</script>
