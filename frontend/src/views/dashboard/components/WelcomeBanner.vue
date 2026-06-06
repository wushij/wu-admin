<template>
  <div class="welcome-banner">
    <div class="welcome-content">
      <div class="welcome-left">
        <div class="welcome-avatar">
          <el-icon :size="40"><Avatar /></el-icon>
        </div>
        <div class="welcome-text">
          <h1>欢迎回来，{{ nickname }}</h1>
          <p class="welcome-platform">{{ platformName }} · {{ platformSubtitle }}</p>
          <p class="welcome-time">{{ currentTime }}</p>
          <p class="welcome-greeting">{{ greetingMessage }}</p>
        </div>
      </div>
      <div class="welcome-right">
        <div class="welcome-stat-item">
          <div class="stat-icon-wrapper online">
            <el-icon :size="20"><UserFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.onlineCount ?? 0 }}</div>
            <div class="stat-label">在线用户</div>
          </div>
        </div>
        <div class="welcome-stat-divider" />
        <div class="welcome-stat-item">
          <div class="stat-icon-wrapper visit">
            <el-icon :size="20"><View /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.todayVisits ?? 0 }}</div>
            <div class="stat-label">今日访问</div>
          </div>
        </div>
        <div class="welcome-stat-divider" />
        <div class="welcome-stat-item">
          <div class="stat-icon-wrapper login-ok">
            <el-icon :size="20"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.todayLoginSuccess ?? 0 }}</div>
            <div class="stat-label">今日登录成功</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Avatar, UserFilled, View, CircleCheck } from '@element-plus/icons-vue'
import type { DashboardStats } from '@/api/dashboard'

defineProps<{
  nickname: string
  platformName: string
  platformSubtitle: string
  currentTime: string
  greetingMessage: string
  stats: DashboardStats
}>()
</script>

<style scoped>
.welcome-banner {
  background: linear-gradient(135deg, var(--theme-primary, #111827) 0%, var(--theme-primary-hover, #374151) 50%, var(--theme-primary-active, #4b5563) 100%);
  border-radius: 16px;
  padding: 32px;
  margin-bottom: 24px;
  color: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  position: relative;
  overflow: hidden;
}

.welcome-banner::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -10%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
  pointer-events: none;
}

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 32px;
  position: relative;
  z-index: 1;
}

.welcome-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.welcome-avatar {
  width: 64px;
  height: 64px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
  flex-shrink: 0;
}

.welcome-text h1 {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.welcome-time {
  margin: 0 0 4px 0;
  font-size: 14px;
  opacity: 0.9;
}

.welcome-platform {
  margin: 4px 0 0;
  font-size: 14px;
  opacity: 0.9;
}

.welcome-greeting {
  margin: 0;
  font-size: 13px;
  opacity: 0.75;
}

.stat-icon-wrapper.login-ok {
  background: rgba(34, 197, 94, 0.35);
}

.welcome-right {
  display: flex;
  align-items: center;
  gap: 24px;
  background: rgba(255, 255, 255, 0.1);
  padding: 16px 24px;
  border-radius: 12px;
  backdrop-filter: blur(10px);
}

.welcome-stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
}

.stat-info .stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-info .stat-label {
  font-size: 12px;
  opacity: 0.8;
}

.welcome-stat-divider {
  width: 1px;
  height: 40px;
  background: rgba(255, 255, 255, 0.3);
}

@media (max-width: 768px) {
  .welcome-banner { padding: 20px; }
  .welcome-content { flex-direction: column; align-items: flex-start; }
  .welcome-text h1 { font-size: 20px; }
  .welcome-right { width: 100%; justify-content: space-around; }
}
</style>
