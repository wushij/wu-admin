<template>
  <section class="section-card">
    <div class="section-header">
      <div class="section-title-wrapper">
        <div class="section-icon" aria-hidden="true">
          <el-icon :size="18"><Promotion /></el-icon>
        </div>
        <h2 class="section-title">最近登录</h2>
      </div>
      <el-button link type="primary" @click="$router.push('/system/login-log')">查看全部</el-button>
    </div>
    <div class="table-wrap">
      <el-table v-if="rows.length" :data="rows" size="small" stripe>
        <el-table-column prop="username" label="用户" width="130" />
        <el-table-column prop="ipaddr" label="IP" width="150">
          <template #default="{ row }">
            <span class="motion-tabular">{{ row.ipaddr || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="loginLocation" label="地点" min-width="140" show-overflow-tooltip />
        <el-table-column prop="browser" label="浏览器" width="120" show-overflow-tooltip />
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small" effect="plain">
              {{ row.status === 0 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="175">
          <template #default="{ row }">
            <span class="motion-tabular">{{ formatLoginTime(row.loginTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <EmptyState v-else title="暂无登录记录" description="最近的账号登录审计记录将会展示在此" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { Promotion } from '@element-plus/icons-vue'
import type { RecentLogin } from '@/api/dashboard'
import EmptyState from '@/components/ui/EmptyState.vue'

defineProps<{
  rows: RecentLogin[]
}>()

function formatLoginTime(t: string | undefined) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}
</script>

<style scoped lang="scss">
.section-card {
  background: var(--color-bg-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--color-border-subtle);
}

.section-title-wrapper {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.section-icon {
  width: 32px;
  height: 32px;
  background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-color-primary);
}

.section-title {
  margin: 0;
  font-size: var(--text-h3);
  font-weight: 600;
  color: var(--color-text-primary);
}

.table-wrap {
  padding: var(--space-3) var(--space-4) var(--space-4);
}
</style>
