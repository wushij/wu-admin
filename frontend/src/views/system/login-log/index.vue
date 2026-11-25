<template>
  <div class="app-container module-page">
    <PageHeader
      title="登录日志"
      description="记录用户登录行为，支持按账号、IP 与登录状态检索"
      :icon="MODULE_PAGE_ICON.loginLog"
      :stats="[{ label: '日志总数', value: total }]"
    />

    <el-card class="search-card module-search-card" shadow="never">
      <el-form :model="queryParams" inline class="module-search-form">
        <el-form-item label="用户名">
          <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input v-model="queryParams.ipaddr" placeholder="请输入IP地址" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 150px">
            <el-option label="成功" :value="0" />
            <el-option label="失败" :value="1" />
          </el-select>
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
          <span>登录日志列表</span>
          <div class="header-actions">
            <ListExportButton module="login-log" :query-params="queryParams" permission="system:loginLog:query" />
            <el-button
              type="danger"
              plain
              v-permission="'system:loginLog:clear'"
              @click="handleClear"
            >
              清空日志
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        v-if="logList.length || loading"
        :data="logList"
        v-loading="loading"
        border
        :header-cell-style="tableHeaderStyle"
        :cell-style="tableCellStyle"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" header-align="center" />
        <el-table-column prop="username" label="用户名" width="120" align="center" header-align="center" />
        <el-table-column prop="ipaddr" label="IP地址" width="140" align="center" header-align="center" />
        <el-table-column prop="loginLocation" label="登录地点" width="150" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" effect="plain">
              {{ row.status === 0 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="msg" label="消息" min-width="150" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column prop="loginTime" label="登录时间" width="180" align="center" header-align="center" />
        <el-table-column prop="browser" label="浏览器" width="120" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column prop="os" label="操作系统" width="120" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right" align="center" header-align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                type="danger"
                plain
                size="small"
                v-permission="'system:loginLog:delete'"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState v-else title="暂无登录日志" description="当前筛选条件下没有登录审计记录" />

      <el-pagination
        v-if="total > 0"
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="table-pagination"
        @size-change="getList"
        @current-change="getList"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import ListExportButton from '@/components/ListExportButton.vue'
import { openAlertDialog } from '@/composables/ui/useAlertDialog'
import {
  getLoginLogList,
  deleteLoginLog,
  clearLoginLog,
  type LoginLogVO,
  type LoginLogPageQuery,
} from '@/api/system/login-log'

const tableHeaderStyle = { textAlign: 'center' as const }
const tableCellStyle = { textAlign: 'center' as const }

const loading = ref(false)
const total = ref(0)
const logList = ref<LoginLogVO[]>([])

const queryParams = reactive<LoginLogPageQuery>({
  pageNo: 1,
  pageSize: 10,
  username: '',
  ipaddr: '',
  status: null,
})

const getList = async () => {
  loading.value = true
  try {
    const res = await getLoginLogList(queryParams)
    logList.value = res.data.list || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryParams.username = ''
  queryParams.ipaddr = ''
  queryParams.status = null
  handleQuery()
}

const handleDelete = async (row: LoginLogVO) => {
  await openAlertDialog({
    title: '永久删除登录日志',
    targetName: `${row.username} · ${row.ipaddr}`,
    message: '删除后该条登录审计日志将被永久移除，不可恢复。',
    severity: 'irreversible',
    confirmText: '永久删除',
    onConfirm: async () => {
      await deleteLoginLog(row.id)
      ElMessage.success('已永久删除')
      getList()
    },
  })
}

const handleClear = async () => {
  await openAlertDialog({
    title: '清空全部登录日志',
    targetName: `共 ${total.value} 条登录记录`,
    message: '此操作将永久清空全部登录审计日志且不可恢复，请确认已完成安全留档。',
    severity: 'irreversible',
    confirmText: '清空全部日志',
    requireConfirmInput: '清空日志',
    onConfirm: async () => {
      await clearLoginLog()
      ElMessage.success('已清空全部登录日志')
      getList()
    },
  })
}

onMounted(() => {
  getList()
})
</script>
