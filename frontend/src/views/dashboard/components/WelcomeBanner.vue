<template>
  <section ref="bannerRef" class="welcome-banner motion-spotlight">
    <div class="welcome-content">
      <div class="welcome-left">
        <el-avatar :size="60" :src="avatarSrc" class="welcome-avatar-img">
          {{ avatarFallback }}
        </el-avatar>
        <div class="welcome-text">
          <div class="welcome-title-row">
            <h1 class="welcome-title">欢迎回来，{{ nickname }}</h1>
            <span v-for="tag in platformTags" :key="tag" class="welcome-tag">{{ tag }}</span>
          </div>
          <p class="welcome-greeting">{{ greetingMessage }}</p>
          <p class="welcome-time motion-tabular">
            <el-icon class="welcome-time-icon" :size="14"><Clock /></el-icon>
            <span>{{ currentTime }}</span>
          </p>
        </div>
      </div>

      <div class="welcome-right">
        <div class="welcome-stat-item">
          <div class="stat-icon-wrapper online" aria-hidden="true">
            <el-icon :size="18"><UserFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              <CountUpNumber :value="stats.onlineCount ?? 0" />
            </div>
            <div class="stat-label">在线用户</div>
          </div>
        </div>
        <div class="welcome-stat-divider" aria-hidden="true" />
        <div class="welcome-stat-item">
          <div class="stat-icon-wrapper visit" aria-hidden="true">
            <el-icon :size="18"><View /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              <CountUpNumber :value="stats.todayVisits ?? 0" />
            </div>
            <div class="stat-label">今日访问</div>
          </div>
        </div>
        <div class="welcome-stat-divider" aria-hidden="true" />
        <div class="welcome-stat-item">
          <div class="stat-icon-wrapper login-ok" aria-hidden="true">
            <el-icon :size="18"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              <CountUpNumber :value="stats.todayLoginSuccess ?? 0" />
            </div>
            <div class="stat-label">今日登录成功</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 平台核心能力横向无缝跑马灯（hover/focus 自动暂停） -->
    <div class="welcome-marquee-bar">
      <div class="motion-marquee" tabindex="0" aria-label="系统核心能力列表">
        <div class="motion-marquee-track" style="--marquee-duration: 32s;">
          <span v-for="cap in CAPABILITIES" :key="`a-${cap.label}`" class="cap-pill">
            <el-icon :size="13"><component :is="cap.icon" /></el-icon>
            <span>{{ cap.label }}</span>
          </span>
          <span
            v-for="cap in CAPABILITIES"
            :key="`b-${cap.label}`"
            class="cap-pill"
            aria-hidden="true"
          >
            <el-icon :size="13"><component :is="cap.icon" /></el-icon>
            <span>{{ cap.label }}</span>
          </span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  UserFilled,
  View,
  CircleCheck,
  Clock,
  Lock,
  Key,
  FolderOpened,
  DataLine,
  Connection,
} from '@element-plus/icons-vue'
import { AiModel } from '@/utils/menu-icon'
import type { DashboardStats } from '@/api/dashboard'
import { useSpotlight } from '@/composables/useMotion'
import CountUpNumber from '@/components/ui/CountUpNumber.vue'

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

const bannerRef = ref<HTMLElement | null>(null)
useSpotlight(bannerRef)

const CAPABILITIES = [
  { icon: Lock, label: 'SM4-GCM 全链路加密传输' },
  { icon: Key, label: 'SM2 / HMAC-SM3 防重放签名' },
  { icon: AiModel, label: 'AI 智能中枢 · RAG 知识检索' },
  { icon: DataLine, label: '实时审计与风控速率限制' },
  { icon: FolderOpened, label: '多引擎对象存储热切换' },
  { icon: Connection, label: 'RBAC 与部门数据范围隔离' },
]

const platformTags = computed(() => {
  const tags: string[] = []
  if (props.platformName?.trim()) tags.push(props.platformName.trim())
  if (props.platformSubtitle?.trim()) {
    props.platformSubtitle
      .split('·')
      .map((s) => s.trim())
      .filter(Boolean)
      .slice(0, 2)
      .forEach((part) => tags.push(part))
  }
  return tags
})
</script>

<style scoped lang="scss">
.welcome-banner {
  position: relative;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-5) var(--space-6) var(--space-4);
  margin-bottom: var(--space-5);
  color: var(--color-text-primary);
}

.welcome-content {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-6);
}

.welcome-left {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  min-width: 0;
}

.welcome-avatar-img {
  flex-shrink: 0;
  border: 2px solid var(--el-color-primary-light-7);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 22px;
  font-weight: 600;
}

.welcome-text {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.welcome-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.welcome-title {
  margin: 0;
  font-size: var(--text-h1);
  font-weight: 700;
  line-height: var(--leading-h1);
  letter-spacing: -0.02em;
  color: var(--color-text-primary);
}

.welcome-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  font-size: var(--text-caption);
  font-weight: 500;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: var(--radius-pill);
}

.welcome-greeting {
  margin: 0;
  font-size: var(--text-body);
  color: var(--color-text-secondary);
}

.welcome-time {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  font-size: var(--text-caption);
  color: var(--color-text-muted);
}

.welcome-time-icon {
  flex-shrink: 0;
}

.welcome-right {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex-shrink: 0;
  padding: var(--space-3) var(--space-5);
  border-radius: var(--radius-lg);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
}

.welcome-stat-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.stat-icon-wrapper {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.online {
    background: var(--color-info-soft);
    color: var(--color-info);
  }
  &.visit {
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
  }
  &.login-ok {
    background: var(--color-success-soft);
    color: var(--color-success);
  }
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.15;
  color: var(--color-text-primary);
}

.stat-label {
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.welcome-stat-divider {
  width: 1px;
  height: 30px;
  background: var(--color-border);
}

.welcome-marquee-bar {
  position: relative;
  z-index: 1;
  margin-top: var(--space-4);
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border-subtle);
}

.cap-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  color: var(--color-text-secondary);
  font-size: var(--text-caption);
  font-weight: 500;
  white-space: nowrap;
}

@media (max-width: 1024px) {
  .welcome-content {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-4);
  }

  .welcome-right {
    width: 100%;
    justify-content: space-around;
  }
}

@media (max-width: 640px) {
  .welcome-banner {
    padding: var(--space-4);
  }

  .welcome-title {
    font-size: 20px;
  }

  .welcome-right {
    flex-wrap: wrap;
    gap: var(--space-3);
  }

  .welcome-stat-divider {
    display: none;
  }
}
</style>
