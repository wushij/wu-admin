<template>
  <el-card class="side-card account-card" shadow="never">
    <template #header>
      <div class="side-card-header"><el-icon><Postcard /></el-icon><span>账号概览</span></div>
    </template>
    <ul class="meta-list">
      <li><span class="meta-label">账号 ID</span><span class="meta-value">{{ profile.userId ?? '—' }}</span></li>
      <li><span class="meta-label">注册时间</span><span class="meta-value">{{ formatTime(profile.createTime) }}</span></li>
      <li><span class="meta-label">资料更新</span><span class="meta-value">{{ formatTime(profile.updateTime) }}</span></li>
      <li><span class="meta-label">最近登录 IP</span><span class="meta-value">{{ profile.lastLoginIp || '—' }}</span></li>
      <li><span class="meta-label">登录地点</span><span class="meta-value">{{ profile.lastLoginLocation || '—' }}</span></li>
    </ul>
  </el-card>
  <el-card class="side-card tips-card" shadow="never">
    <template #header>
      <div class="side-card-header"><el-icon><InfoFilled /></el-icon><span>安全提示</span></div>
    </template>
    <ul class="tips-list">
      <li>请勿将账号密码告知他人或在公共设备勾选「记住我」。</li>
      <li>发现陌生登录记录时，请立即修改密码并联系管理员。</li>
      <li>部门、岗位、角色由管理员分配，如需调整请联系系统管理员。</li>
    </ul>
  </el-card>
</template>

<script setup lang="ts">
import { Postcard, InfoFilled } from '@element-plus/icons-vue'
import type { UserProfile } from '@/types/profile'

defineProps<{
  profile: UserProfile
  formatTime: (time?: string, short?: boolean) => string
}>()
</script>
