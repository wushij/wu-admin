<template>
  <div class="section-card">
    <div class="section-header">
      <div class="section-title-wrapper">
        <div class="section-icon">
          <el-icon :size="18"><Promotion /></el-icon>
        </div>
        <span class="section-title">最近登录</span>
      </div>
      <el-button link type="primary" @click="$router.push('/system/login-log')">查看全部</el-button>
    </div>
    <el-table :data="rows" size="small" stripe empty-text="暂无登录记录">
      <el-table-column prop="username" label="用户" width="120" />
      <el-table-column prop="ipaddr" label="IP" width="140" />
      <el-table-column prop="loginLocation" label="地点" min-width="120" show-overflow-tooltip />
      <el-table-column prop="browser" label="浏览器" width="100" show-overflow-tooltip />
      <el-table-column label="结果" width="88">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
            {{ row.status === 0 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="170">
        <template #default="{ row }">
          {{ formatLoginTime(row.loginTime) }}
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { Promotion } from '@element-plus/icons-vue'
import type { RecentLogin } from '@/api/dashboard'

defineProps<{
  rows: RecentLogin[]
}>()

function formatLoginTime(t: string | undefined) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}
</script>

<style scoped>
.section-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.section-title-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, var(--theme-primary, #111827) 0%, var(--theme-primary-hover, #374151) 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--theme-text-base, #1F2937);
}
</style>
