<template>
  <div class="app-container online-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>在线用户</span>
          <el-button type="primary" @click="loadData">刷新</el-button>
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
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="loginName" label="用户名" width="120" />
        <el-table-column prop="deptName" label="部门/昵称" width="120" show-overflow-tooltip />
        <el-table-column prop="ipaddr" label="主机" width="130" />
        <el-table-column prop="loginLocation" label="登录地点" width="120" />
        <el-table-column prop="browser" label="浏览器" width="120" show-overflow-tooltip />
        <el-table-column prop="os" label="操作系统" width="120" show-overflow-tooltip />
        <el-table-column prop="status" label="会话状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="loginTime" label="登录时间" width="180" />
        <el-table-column prop="lastAccessTime" label="最后访问时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right" align="center">
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
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOnlineUserList, forceLogoutOnlineUser } from '@/api/monitor/online'
import { useUserStore } from '@/store/user'
import router from '@/router'

const userStore = useUserStore()
const loading = ref(false)
const tableData = ref([])

async function loadData() {
  loading.value = true
  try {
    const res = await getOnlineUserList()
    tableData.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function handleForceLogout(row) {
  const isSelf = userStore.userInfo?.userId != null && row.userId === userStore.userInfo.userId

  ElMessageBox.confirm('确定要强制下线该用户吗？', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    await forceLogoutOnlineUser(row.userId)
    ElMessage.success('操作成功')
    if (isSelf) {
      await userStore.logoutAction()
      router.push('/login')
      return
    }
    loadData()
  }).catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.online-page {
  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}
</style>
