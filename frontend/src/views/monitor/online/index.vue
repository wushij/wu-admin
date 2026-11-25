<template>
  <div class="app-container module-page online-page">
    <PageHeader
      title="在线用户"
      description="查看当前在线会话，支持强制下线异常或闲置连接"
      :icon="MODULE_PAGE_ICON.online"
      :stats="[{ label: '当前在线', value: total }]"
    />

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
              :loading="refreshing"
              @click="refreshByUser"
            >
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        v-if="pagedData.length || tableLoading"
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
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="plain">
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
                plain
                size="small"
                @click="handleForceLogout(row)"
              >
                强退
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState v-else title="暂无在线用户" description="当前没有活跃在线会话" />

      <el-pagination
        v-if="total > 0"
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
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import ListExportButton from '@/components/ListExportButton.vue'
import { getOnlineUserList, forceLogoutOnlineUser, type OnlineUser } from '@/api/monitor/online'
import { useUserStore } from '@/store/user'
import { openAlertDialog } from '@/composables/ui/useAlertDialog'
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
  if (showTableLoading) tableLoading.value = true
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
    if (showTableLoading) tableLoading.value = false
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

async function handleForceLogout(row: OnlineUser) {
  const isSelf = userStore.userInfo?.userId != null && row.userId === userStore.userInfo.userId
  await openAlertDialog({
    title: '强制用户下线',
    targetName: `${row.loginName} (${row.ipaddr || '未知 IP'})`,
    message: isSelf
      ? '强制下线当前登录账号将立即退出控制台并跳转至登录页。'
      : '强制下线后该会话令牌将立即失效，目标用户正在进行的未保存操作可能丢失。',
    severity: 'irreversible',
    confirmText: '强制下线',
    onConfirm: async () => {
      await forceLogoutOnlineUser(row.userId)
      ElMessage.success('已强制下线')
      if (isSelf) {
        await userStore.logoutAction()
        router.push('/login')
        return
      }
      await loadData(true)
    },
  })
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
