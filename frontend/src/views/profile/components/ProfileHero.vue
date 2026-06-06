<template>
  <div class="profile-hero">
    <div class="hero-bg-pattern" />
    <div class="hero-content">
      <div class="hero-left">
        <div class="avatar-uploader" @click="$emit('triggerAvatar')">
          <el-avatar :size="88" :src="avatarSrc" class="hero-avatar">{{ avatarFallback }}</el-avatar>
          <div class="avatar-mask">
            <el-icon :size="22"><Camera /></el-icon>
            <span>更换头像</span>
          </div>
        </div>
        <div class="hero-info">
          <h1 class="hero-name">{{ profile.nickname || profile.username || '用户' }}</h1>
          <p class="hero-username">@{{ profile.username }}</p>
          <div class="hero-tags">
            <el-tag v-for="role in profile.roleNames || []" :key="role" effect="dark" round class="role-tag">{{ role }}</el-tag>
            <el-tag :type="statusTagType" effect="plain" round>{{ statusLabel }}</el-tag>
          </div>
        </div>
      </div>
      <div class="hero-stats">
        <div class="stat-item"><div class="stat-value">{{ profile.deptName || '未分配' }}</div><div class="stat-label">所属部门</div></div>
        <div class="stat-divider" />
        <div class="stat-item"><div class="stat-value">{{ postDisplay }}</div><div class="stat-label">岗位</div></div>
        <div class="stat-divider" />
        <div class="stat-item"><div class="stat-value">{{ lastLoginDisplay }}</div><div class="stat-label">最近登录</div></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Camera } from '@element-plus/icons-vue'
import type { UserProfile } from '@/types/profile'

defineProps<{
  profile: UserProfile
  avatarSrc: string | undefined
  avatarFallback: string
  statusLabel: string
  statusTagType: 'success' | 'warning' | 'danger' | 'info'
  postDisplay: string
  lastLoginDisplay: string
}>()

defineEmits<{ triggerAvatar: [] }>()
</script>
