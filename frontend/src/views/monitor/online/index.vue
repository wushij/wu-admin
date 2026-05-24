<template>
  <div class="app-container module-page online-page">
    <el-card class="search-card module-hero-card" shadow="never">
      <div class="module-hero-row">
        <div class="module-hero-text">
          <div class="module-hero-title">
            <ModulePageIcon :icon="MODULE_PAGE_ICON.online" />
            <span>在线用户</span>
          </div>
          <p class="module-hero-desc">查看当前在线会话，支持强制下线异常或闲置连接</p>
        </div>
        <div class="module-hero-stats">
          <div class="stat-num">{{ total }}</div>
          <div class="stat-label">当前在线</div>
        </div>
      </div>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>在线用户列表</span>
          <div class="header-actions">
            <ListExportButton
              module="online-user"
              :query-params="queryParams"
              permission="monitor:online:list"
            />
            <el-button
              type="primary"
              :icon="Refresh"
              class="refresh-btn"
              :class="{ 'is-refreshing': refreshing }"
              :loading="refreshing"
              @click="refreshByUser"
            >
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="pagedData"
        v-loading="tableLoading"
        border
        stripe
        :header-cell-style="tableHeaderStyle"
        :cell-style="tableCellStyle"
      >
        <el-table-column type="index" label="序号" width="60" align="center" header-align="center" :index="indexMethod" />
        <el-table-column prop="loginName" label="用户名" width="120" align="center" header-align="center" />
        <el-table-column prop="deptName" label="部门/昵称" min-width="120" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column prop="ipaddr" label="主机" width="130" align="center" header-align="center" />
        <el-table-column prop="loginLocation" label="登录地点" width="120" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column prop="browser" label="浏览器" width="120" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column prop="os" label="操作系统" width="120" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column prop="status" label="会话状态" width="100" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="loginTime" label="登录时间" width="180" align="center" header-align="center" />
        <el-table-column prop="lastAccessTime" label="最后访问时间" width="180" align="center" header-align="center" />
        <el-table-column label="操作" width="100" fixed="right" align="center" header-align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                v-permission="'monitor:online:forceLogout'"
                type="danger"
                size="small"
                @click="handleForceLogout(row)"
              >
                强退
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
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import ListExportButton from '@/components/ListExportButton.vue'
import { getOnlineUserList, forceLogoutOnlineUser, type OnlineUser } from '@/api/monitor/online'
import { useUserStore } from '@/store/user'
import router from '@/router'

const tableHeaderStyle = { textAlign: 'center' as const }
const tableCellStyle = { textAlign: 'center' as const }

const userStore = useUserStore()
const allData = ref<OnlineUser[]>([])
const refreshing = ref(false)
const tableLoading = ref(false)

const queryParams = ref({
  pageNo: 1,
  pageSize: 10,
})

const total = computed(() => allData.value.length)

const pagedData = computed(() => {
  const { pageNo, pageSize } = queryParams.value
  const start = (pageNo - 1) * pageSize
  return allData.value.slice(start, start + pageSize)
})

function indexMethod(index: number) {
  return (queryParams.value.pageNo - 1) * queryParams.value.pageSize + index + 1
}

async function loadData(showTableLoading = false) {
  if (showTableLoading) {
    tableLoading.value = true
  }
  try {
    const res = await getOnlineUserList()
    allData.value = res.data || []
    const maxPage = Math.max(1, Math.ceil(allData.value.length / queryParams.value.pageSize))
    if (queryParams.value.pageNo > maxPage) {
      queryParams.value.pageNo = maxPage
    }
  } catch (e) {
    console.error(e)
  } finally {
    if (showTableLoading) {
      tableLoading.value = false
    }
  }
}

async function refreshByUser() {
  if (refreshing.value) return
  refreshing.value = true
  tableLoading.value = true
  try {
    await loadData(false)
  } finally {
    refreshing.value = false
    tableLoading.value = false
  }
}

function handleForceLogout(row: OnlineUser) {
  const isSelf = userStore.userInfo?.userId != null && row.userId === userStore.userInfo.userId

  ElMessageBox.confirm('确定要强制下线该用户吗？', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  })
    .then(async () => {
      await forceLogoutOnlineUser(row.userId)
      ElMessage.success('操作成功')
      if (isSelf) {
        await userStore.logoutAction()
        router.push('/login')
        return
      }
      await loadData(true)
    })
    .catch(() => {})
}

onMounted(() => loadData(true))
</script>

<style scoped lang="scss">
.online-page {
  .header-actions {
    :deep(.list-export-btn) {
      margin: 0;
    }

    .refresh-btn {
      min-width: 80px;
      margin: 0;
    }
  }
}
</style>
