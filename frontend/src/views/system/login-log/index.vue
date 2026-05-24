<template>
  <div class="app-container module-page">
    <el-card class="search-card module-hero-card" shadow="never">
      <div class="module-hero-row">
        <div class="module-hero-text">
          <div class="module-hero-title">
            <ModulePageIcon :icon="MODULE_PAGE_ICON.loginLog" />
            <span>登录日志</span>
          </div>
          <p class="module-hero-desc">记录用户登录行为，支持按账号、IP 与登录状态检索</p>
        </div>
        <div class="module-hero-stats">
          <div class="stat-num">{{ total }}</div>
          <div class="stat-label">日志总数</div>
        </div>
      </div>
    </el-card>

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
              v-permission="'system:loginLog:clear'"
              @click="handleClear"
            >
              清空日志
            </el-button>
          </div>
        </div>
      </template>
      <el-table
        :data="logList"
        v-loading="loading"
        border
        stripe
        :header-cell-style="tableHeaderStyle"
        :cell-style="tableCellStyle"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" header-align="center" />
        <el-table-column prop="username" label="用户名" width="120" align="center" header-align="center" />
        <el-table-column prop="ipaddr" label="IP地址" width="140" align="center" header-align="center" />
        <el-table-column prop="loginLocation" label="登录地点" width="150" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
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
      <el-pagination
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
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import { ElMessage, ElMessageBox } from 'element-plus'
import ListExportButton from '@/components/ListExportButton.vue'
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
  status: null
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
  await ElMessageBox.confirm('确定要删除该日志吗？', '提示', { type: 'warning' })
  await deleteLoginLog(row.id)
  ElMessage.success('删除成功')
  getList()
}

const handleClear = async () => {
  await ElMessageBox.confirm('确定要清空所有日志吗？此操作不可恢复！', '警告', { type: 'warning' })
  await clearLoginLog()
  ElMessage.success('清空成功')
  getList()
}

onMounted(() => {
  getList()
})
</script>

