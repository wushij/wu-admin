<template>
  <!-- 顶部概览 Hero 卡片 -->
  <el-card class="search-card module-hero-card" shadow="never">
    <div class="module-hero-row">
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
        <!-- 1. 今日拦截总览 -->
        <div class="stat-card stat-sky">
          <div class="stat-indicator"></div>
          <div class="stat-body">
            <div class="stat-num">{{ stats.todayTotal }}</div>
            <div class="stat-meta">
              <span class="stat-dot"></span>
              <span class="stat-label">今日拦截</span>
            </div>
          </div>
        </div>

        <!-- 2. 爬虫拦截 -->
        <div class="stat-card stat-amber">
          <div class="stat-indicator"></div>
          <div class="stat-body">
            <div class="stat-num">{{ stats.todayUaBlock }}</div>
            <div class="stat-meta">
              <span class="stat-dot"></span>
              <span class="stat-label">爬虫嗅探</span>
            </div>
          </div>
        </div>

        <!-- 3. 重放攻击 / 国密验签 -->
        <div class="stat-card stat-purple">
          <div class="stat-indicator"></div>
          <div class="stat-body">
            <div class="stat-num">{{ stats.todayReplayBlock }}</div>
            <div class="stat-meta">
              <span class="stat-dot"></span>
              <span class="stat-label">重放攻击</span>
            </div>
          </div>
        </div>

        <!-- 4. 蜜罐诱捕击中 -->
        <div class="stat-card stat-teal">
          <div class="stat-indicator"></div>
          <div class="stat-body">
            <div class="stat-num">{{ stats.todayHoneypot }}</div>
            <div class="stat-meta">
              <span class="stat-dot"></span>
              <span class="stat-label">蜜罐击中</span>
            </div>
          </div>
        </div>

        <!-- 5. 自动封禁处置 -->
        <div class="stat-card stat-rose">
          <div class="stat-indicator"></div>
          <div class="stat-body">
            <div class="stat-num">{{ stats.todayForbidden }}</div>
            <div class="stat-meta">
              <span class="stat-dot"></span>
              <span class="stat-label">自动封禁</span>
            </div>
          </div>
        </div>

        <!-- 6. 当前受控封禁主体 -->
        <div class="stat-card stat-banned" :class="{ 'has-banned': stats.activeBlockedCount > 0 }">
          <div class="stat-indicator"></div>
          <div class="stat-body">
            <div class="stat-num">{{ stats.activeBlockedCount }}</div>
            <div class="stat-meta">
              <span class="live-pulse-dot" :class="{ danger: stats.activeBlockedCount > 0 }"></span>
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
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import type { SecurityStatsVO } from '@/api/system/security'

defineProps<{
  stats: SecurityStatsVO
}>()
</script>

<style scoped>
.security-stats-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
}

/* 核心微光指标卡片 */
.stat-card {
  position: relative;
  min-width: 98px;
  padding: 9px 12px 9px;
  border-radius: 10px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.02) 100%);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.18), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  transition: all 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  user-select: none;
}

.stat-card:hover {
  transform: translateY(-2px);
  border-color: rgba(255, 255, 255, 0.22);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.28), inset 0 1px 0 rgba(255, 255, 255, 0.16);
}

/* 顶部细致微弧光条 */
.stat-indicator {
  position: absolute;
  top: 0;
  left: 10px;
  right: 10px;
  height: 2px;
  border-radius: 0 0 2px 2px;
  opacity: 0.9;
  transition: opacity 0.25s ease;
}

.stat-card:hover .stat-indicator {
  opacity: 1;
  filter: drop-shadow(0 0 4px currentColor);
}

.stat-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.stat-num {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.5px;
  font-feature-settings: 'tnum';
  transition: transform 0.25s ease;
}

.stat-card:hover .stat-num {
  transform: scale(1.05);
}

.stat-meta {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 4px;
}

.stat-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  opacity: 0.85;
}

.stat-label {
  font-size: 12px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.75);
  letter-spacing: 0.3px;
  white-space: nowrap;
}

/* 1. 天蓝 - 今日拦截 */
.stat-sky .stat-indicator {
  background: linear-gradient(90deg, transparent, #38bdf8, transparent);
  color: #38bdf8;
}
.stat-sky .stat-num {
  color: #38bdf8;
  text-shadow: 0 0 16px rgba(56, 189, 248, 0.35);
}
.stat-sky .stat-dot {
  background: #38bdf8;
  box-shadow: 0 0 6px #38bdf8;
}
.stat-sky:hover {
  background: linear-gradient(180deg, rgba(56, 189, 248, 0.12) 0%, rgba(255, 255, 255, 0.03) 100%);
  border-color: rgba(56, 189, 248, 0.35);
}

/* 2. 金黄 - 爬虫嗅探 */
.stat-amber .stat-indicator {
  background: linear-gradient(90deg, transparent, #fbbf24, transparent);
  color: #fbbf24;
}
.stat-amber .stat-num {
  color: #fbbf24;
  text-shadow: 0 0 16px rgba(251, 191, 36, 0.35);
}
.stat-amber .stat-dot {
  background: #fbbf24;
  box-shadow: 0 0 6px #fbbf24;
}
.stat-amber:hover {
  background: linear-gradient(180deg, rgba(251, 191, 36, 0.12) 0%, rgba(255, 255, 255, 0.03) 100%);
  border-color: rgba(251, 191, 36, 0.35);
}

/* 3. 紫罗兰 - 重放反篡改 */
.stat-purple .stat-indicator {
  background: linear-gradient(90deg, transparent, #c084fc, transparent);
  color: #c084fc;
}
.stat-purple .stat-num {
  color: #c084fc;
  text-shadow: 0 0 16px rgba(192, 132, 252, 0.35);
}
.stat-purple .stat-dot {
  background: #c084fc;
  box-shadow: 0 0 6px #c084fc;
}
.stat-purple:hover {
  background: linear-gradient(180deg, rgba(192, 132, 252, 0.12) 0%, rgba(255, 255, 255, 0.03) 100%);
  border-color: rgba(192, 132, 252, 0.35);
}

/* 4. 极光翠青 - 蜜罐诱捕 */
.stat-teal .stat-indicator {
  background: linear-gradient(90deg, transparent, #2dd4bf, transparent);
  color: #2dd4bf;
}
.stat-teal .stat-num {
  color: #2dd4bf;
  text-shadow: 0 0 16px rgba(45, 212, 191, 0.35);
}
.stat-teal .stat-dot {
  background: #2dd4bf;
  box-shadow: 0 0 6px #2dd4bf;
}
.stat-teal:hover {
  background: linear-gradient(180deg, rgba(45, 212, 191, 0.12) 0%, rgba(255, 255, 255, 0.03) 100%);
  border-color: rgba(45, 212, 191, 0.35);
}

/* 5. 珊瑚红 - 自动封禁 */
.stat-rose .stat-indicator {
  background: linear-gradient(90deg, transparent, #fb7185, transparent);
  color: #fb7185;
}
.stat-rose .stat-num {
  color: #fb7185;
  text-shadow: 0 0 16px rgba(251, 113, 133, 0.35);
}
.stat-rose .stat-dot {
  background: #fb7185;
  box-shadow: 0 0 6px #fb7185;
}
.stat-rose:hover {
  background: linear-gradient(180deg, rgba(251, 113, 133, 0.12) 0%, rgba(255, 255, 255, 0.03) 100%);
  border-color: rgba(251, 113, 133, 0.35);
}

/* 6. 动态在封黑名单态 (带呼吸状态点) */
.stat-banned .stat-indicator {
  background: linear-gradient(90deg, transparent, #34d399, transparent);
  color: #34d399;
}
.stat-banned .stat-num {
  color: #34d399;
  text-shadow: 0 0 16px rgba(52, 211, 153, 0.35);
}
.stat-banned:hover {
  background: linear-gradient(180deg, rgba(52, 211, 153, 0.12) 0%, rgba(255, 255, 255, 0.03) 100%);
  border-color: rgba(52, 211, 153, 0.35);
}

/* 当有主体在封时，激活高危警报态 */
.stat-banned.has-banned {
  background: linear-gradient(180deg, rgba(239, 68, 68, 0.15) 0%, rgba(239, 68, 68, 0.04) 100%);
  border-color: rgba(239, 68, 68, 0.3);
}
.stat-banned.has-banned .stat-indicator {
  background: linear-gradient(90deg, transparent, #ef4444, transparent);
  color: #ef4444;
}
.stat-banned.has-banned .stat-num {
  color: #f87171;
  text-shadow: 0 0 16px rgba(239, 68, 68, 0.45);
}
.stat-banned.has-banned:hover {
  background: linear-gradient(180deg, rgba(239, 68, 68, 0.22) 0%, rgba(255, 255, 255, 0.04) 100%);
  border-color: rgba(239, 68, 68, 0.5);
  box-shadow: 0 8px 24px rgba(239, 68, 68, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.16);
}

/* 呼吸指示灯 */
.live-pulse-dot {
  position: relative;
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #34d399;
  box-shadow: 0 0 6px #34d399;
}

.live-pulse-dot.danger {
  background-color: #ef4444;
  box-shadow: 0 0 6px #ef4444;
}

.live-pulse-dot::after {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: inherit;
  opacity: 0.6;
  animation: pulse-ring 2s cubic-bezier(0.24, 0, 0.38, 1) infinite;
}

@keyframes pulse-ring {
  0% {
    transform: scale(0.6);
    opacity: 0.8;
  }
  100% {
    transform: scale(2.4);
    opacity: 0;
  }
}

/* 响应式适配 */
@media (max-width: 1360px) {
  .module-hero-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  .security-stats-group {
    width: 100%;
    justify-content: space-between;
  }
  .stat-card {
    flex: 1;
    min-width: 0;
    padding: 10px 8px;
  }
}

@media (max-width: 768px) {
  .security-stats-group {
    flex-wrap: wrap;
    gap: 10px;
  }
  .stat-card {
    flex: calc(50% - 10px);
    min-width: 120px;
  }
}
</style>

