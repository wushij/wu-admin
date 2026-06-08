<template>
  <div class="welcome-banner">
    <div class="welcome-bg-pattern" />
    <div class="welcome-content">
      <div class="welcome-left">
        <div class="welcome-avatar">
          <el-avatar :size="72" :src="avatarSrc" class="welcome-avatar-img">
            {{ avatarFallback }}
          </el-avatar>
        </div>
        <div class="welcome-text">
          <h1 class="welcome-title">欢迎回来，{{ nickname }}</h1>
          <p class="welcome-greeting">{{ greetingMessage }}</p>
          <div v-if="platformTags.length" class="welcome-tags">
            <span v-for="tag in platformTags" :key="tag" class="welcome-tag">{{ tag }}</span>
          </div>
          <p class="welcome-time">
            <el-icon class="welcome-time-icon" :size="14"><Clock /></el-icon>
            {{ currentTime }}
          </p>
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
import { computed } from 'vue'
import { UserFilled, View, CircleCheck, Clock } from '@element-plus/icons-vue'
import type { DashboardStats } from '@/api/dashboard'

const props = defineProps<{
  nickname: string
  avatarSrc?: string
  avatarFallback: string
  platformName: string
  platformSubtitle: string
  currentTime: string
  greetingMessage: string
  stats: DashboardStats
}>()

const platformTags = computed(() => {
  const tags: string[] = []
  if (props.platformName?.trim()) tags.push(props.platformName.trim())
  if (props.platformSubtitle?.trim()) {
    props.platformSubtitle
      .split('·')
      .map((s) => s.trim())
      .filter(Boolean)
      .forEach((part) => tags.push(part))
  }
  return tags
})
</script>

<style scoped>
.welcome-banner {
  position: relative;
  background: linear-gradient(
    135deg,
    var(--theme-primary, #111827) 0%,
    var(--theme-primary-hover, #374151) 55%,
    var(--theme-primary-active, #4b5563) 100%
  );
  border-radius: 16px;
  padding: 28px 32px;
  margin-bottom: 24px;
  color: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

.welcome-bg-pattern {
  position: absolute;
  inset: 0;
  opacity: 0.07;
  background-image:
    radial-gradient(circle at 18% 42%, #fff 1px, transparent 1px),
    radial-gradient(circle at 82% 18%, #fff 1px, transparent 1px);
  background-size: 36px 36px;
  pointer-events: none;
}

.welcome-banner::after {
  content: '';
  position: absolute;
  top: -40%;
  right: -8%;
  width: 360px;
  height: 360px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.12) 0%, transparent 68%);
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
  gap: 22px;
  min-width: 0;
}

.welcome-avatar {
  flex-shrink: 0;
}

.welcome-avatar-img {
  border: 3px solid rgba(255, 255, 255, 0.38);
  background: rgba(255, 255, 255, 0.12);
  font-size: 26px;
  font-weight: 600;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.18);
}

.welcome-text {
  display: flex;
  flex-direction: column;
  gap: 0;
  min-width: 0;
}

.welcome-title {
  margin: 0 0 10px;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -0.02em;
  color: #fff;
}

.welcome-greeting {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 400;
  line-height: 1.55;
  color: rgba(255, 255, 255, 0.88);
  max-width: 420px;
}

.welcome-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.welcome-tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.4;
  color: rgba(255, 255, 255, 0.92);
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 999px;
  backdrop-filter: blur(6px);
}

.welcome-time {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 12px;
  font-weight: 400;
  line-height: 1.4;
  color: rgba(255, 255, 255, 0.58);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.02em;
}

.welcome-time-icon {
  flex-shrink: 0;
  color: rgba(255, 255, 255, 0.72);
}

.welcome-time-icon :deep(svg) {
  stroke-width: 2;
}

.stat-icon-wrapper.login-ok {
  background: rgba(34, 197, 94, 0.35);
}

.welcome-right {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.1);
  padding: 16px 24px;
  border-radius: 12px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.08);
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
  font-variant-numeric: tabular-nums;
}

.stat-info .stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.welcome-stat-divider {
  width: 1px;
  height: 40px;
  background: rgba(255, 255, 255, 0.22);
}

@media (max-width: 768px) {
  .welcome-banner {
    padding: 20px;
  }

  .welcome-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .welcome-title {
    font-size: 22px;
  }

  .welcome-greeting {
    font-size: 14px;
    max-width: none;
  }

  .welcome-right {
    width: 100%;
    justify-content: space-around;
    padding: 14px 16px;
  }
}
</style>
