<template>
  <el-card class="search-card module-hero-card" shadow="never">
    <div class="module-hero-row security-hero-row">
      <div class="module-hero-text">
        <div class="module-hero-title">
          <ModulePageIcon :icon="MODULE_PAGE_ICON.securityLog" />
          <span>安全风控</span>
        </div>
        <p class="module-hero-desc">
          全链路反爬虫嗅探、防重放攻击、HMAC-SM3 国密验签拦截与动态行为风控闭环
        </p>
      </div>
      <div class="security-stats-group">
        <div class="stat-card stat-sky motion-hover-lift">
          <div class="stat-indicator" />
          <div class="stat-body">
            <div class="stat-num"><CountUpNumber :value="stats.todayTotal" /></div>
            <div class="stat-meta">
              <span class="stat-dot" />
              <span class="stat-label">今日拦截</span>
            </div>
          </div>
        </div>

        <div class="stat-card stat-amber motion-hover-lift">
          <div class="stat-indicator" />
          <div class="stat-body">
            <div class="stat-num"><CountUpNumber :value="stats.todayUaBlock" /></div>
            <div class="stat-meta">
              <span class="stat-dot" />
              <span class="stat-label">爬虫嗅探</span>
            </div>
          </div>
        </div>

        <div class="stat-card stat-purple motion-hover-lift">
          <div class="stat-indicator" />
          <div class="stat-body">
            <div class="stat-num"><CountUpNumber :value="stats.todayReplayBlock" /></div>
            <div class="stat-meta">
              <span class="stat-dot" />
              <span class="stat-label">重放攻击</span>
            </div>
          </div>
        </div>

        <div class="stat-card stat-teal motion-hover-lift">
          <div class="stat-indicator" />
          <div class="stat-body">
            <div class="stat-num"><CountUpNumber :value="stats.todayHoneypot" /></div>
            <div class="stat-meta">
              <span class="stat-dot" />
              <span class="stat-label">蜜罐击中</span>
            </div>
          </div>
        </div>

        <div class="stat-card stat-rose motion-hover-lift">
          <div class="stat-indicator" />
          <div class="stat-body">
            <div class="stat-num"><CountUpNumber :value="stats.todayForbidden" /></div>
            <div class="stat-meta">
              <span class="stat-dot" />
              <span class="stat-label">自动封禁</span>
            </div>
          </div>
        </div>

        <div class="stat-card stat-banned motion-hover-lift" :class="{ 'has-banned': stats.activeBlockedCount > 0 }">
          <div class="stat-indicator" />
          <div class="stat-body">
            <div class="stat-num"><CountUpNumber :value="stats.activeBlockedCount" /></div>
            <div class="stat-meta">
              <span class="live-pulse-dot" :class="{ danger: stats.activeBlockedCount > 0 }" />
              <span class="stat-label">当前封禁</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import CountUpNumber from '@/components/ui/CountUpNumber.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import type { SecurityStatsVO } from '@/api/system/security'

defineProps<{
  stats: SecurityStatsVO
}>()
</script>

<style scoped lang="scss">
.security-stats-group {
  display: grid;
  grid-template-columns: repeat(6, minmax(80px, 94px));
  gap: var(--space-2);
  flex-shrink: 0;
}

.stat-card {
  --sec-accent: var(--el-color-primary);
  position: relative;
  min-width: 0;
  padding: 12px 8px 10px;
  border-radius: var(--radius-lg);
  background: var(--color-bg-subtle);
  background: color-mix(in srgb, var(--color-bg-subtle) 95%, var(--sec-accent) 5%);
  border: 1px solid var(--color-border);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  user-select: none;
  transition:
    border-color var(--duration-fast) var(--ease-out),
    background-color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);
}

@media (hover: hover) and (pointer: fine) {
  .stat-card:hover {
    border-color: color-mix(in srgb, var(--sec-accent) 38%, var(--color-border));

    .stat-indicator {
      opacity: 0.28;
    }
  }
}

/* 顶部柔和氛围漫射光晕：替代生硬截断的直横线，随圆角自然融进卡片底色 */
.stat-indicator {
  position: absolute;
  top: -16px;
  left: 50%;
  width: 78%;
  height: 28px;
  transform: translateX(-50%);
  border-radius: 50%;
  background: var(--sec-accent);
  opacity: 0.18;
  filter: blur(12px);
  pointer-events: none;
  transition: opacity var(--duration-fast) var(--ease-out);
}

.stat-body {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.stat-num {
  font-size: 21px;
  font-weight: 700;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.stat-meta {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 4px;
}

.stat-dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-circle);
  background: var(--sec-accent);
  box-shadow: 0 0 6px color-mix(in srgb, var(--sec-accent) 55%, transparent);
  flex-shrink: 0;
}

.stat-label {
  font-size: var(--text-caption);
  font-weight: 500;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.stat-sky {
  --sec-accent: #0ea5e9;
  .stat-num { color: var(--sec-sky-num, #0284c7); }
}
.stat-amber {
  --sec-accent: #f59e0b;
  .stat-num { color: var(--sec-amber-num, #d97706); }
}
.stat-purple {
  --sec-accent: #8b5cf6;
  .stat-num { color: var(--sec-purple-num, #7c3aed); }
}
.stat-teal {
  --sec-accent: #14b8a6;
  .stat-num { color: var(--sec-teal-num, #0d9488); }
}
.stat-rose {
  --sec-accent: #f43f5e;
  .stat-num { color: var(--sec-rose-num, #e11d48); }
}

.stat-banned {
  --sec-accent: var(--color-success);
  .stat-num { color: var(--color-success); }

  &.has-banned {
    --sec-accent: var(--color-danger);
    background: var(--color-danger-soft);
    border-color: var(--color-danger-border);
    .stat-num { color: var(--color-danger); }
  }
}

.live-pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-circle);
  background: var(--color-success);
  flex-shrink: 0;

  &.danger {
    background: var(--color-danger);
  }
}

@media (max-width: 1180px) {
  .security-hero-row {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-4);
  }
  .security-stats-group {
    width: 100%;
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }
}

@media (max-width: 840px) {
  .security-stats-group {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 480px) {
  .security-stats-group {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

<style lang="scss">
html.dark .security-stats-group {
  --sec-sky-num: #38bdf8;
  --sec-amber-num: #fbbf24;
  --sec-purple-num: #a78bfa;
  --sec-teal-num: #2dd4bf;
  --sec-rose-num: #fb7185;
}
</style>
