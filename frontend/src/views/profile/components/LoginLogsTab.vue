<template>
  <div>
    <div class="tab-intro"><el-icon><Clock /></el-icon><span>查看您账号的最近登录活动，发现异常请及时修改密码。</span></div>
    <el-table :data="loginLogs" v-loading="loading" stripe class="logs-table"
      :header-cell-style="{ textAlign: 'center', background: '#f9fafb' }" :cell-style="{ textAlign: 'center' }">
      <el-table-column prop="loginTime" label="登录时间" width="180" />
      <el-table-column prop="ipaddr" label="IP 地址" width="140" />
      <el-table-column prop="loginLocation" label="登录地点" min-width="120" show-overflow-tooltip />
      <el-table-column prop="browser" label="浏览器" width="120" show-overflow-tooltip />
      <el-table-column prop="os" label="操作系统" width="120" show-overflow-tooltip />
      <el-table-column prop="status" label="结果" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small" effect="plain">
            {{ row.status === 0 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-if="total > 0" v-model:current-page="query.pageNo" v-model:page-size="query.pageSize"
      :total="total" :page-sizes="[5, 10, 20]" layout="total, prev, pager, next"
      class="logs-pagination" @current-change="$emit('loadLogs')" @size-change="$emit('loadLogs')" />
  </div>
</template>

<script setup lang="ts">
import { Clock } from '@element-plus/icons-vue'
import type { LoginLogVO } from '@/api/system/login-log'

defineProps<{
  loginLogs: LoginLogVO[]
  loading: boolean
  total: number
  query: { pageNo: number; pageSize: number }
}>()

defineEmits<{ loadLogs: [] }>()
</script>
