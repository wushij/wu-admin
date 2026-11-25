<template>
  <section class="section-card">
    <div class="section-header">
      <div class="section-title-wrapper">
        <div class="section-icon" aria-hidden="true">
          <el-icon :size="18"><Grid /></el-icon>
        </div>
        <h2 class="section-title">快捷入口</h2>
      </div>
      <el-tag type="info" size="small" effect="plain">常用功能</el-tag>
    </div>
    <div v-if="visibleEntries.length" class="quick-grid">
      <button
        v-for="(item, idx) in visibleEntries"
        :key="item.key"
        type="button"
        class="quick-item motion-hover-lift motion-stagger-item"
        :style="{ '--stagger-index': idx }"
        @click="goQuick(item)"
      >
        <div class="quick-icon-wrapper" :class="item.theme" aria-hidden="true">
          <el-icon :size="22"><component :is="item.icon" /></el-icon>
        </div>
        <div class="quick-info">
          <div class="quick-name">{{ item.name }}</div>
          <div class="quick-desc">{{ item.desc }}</div>
        </div>
        <el-icon class="quick-arrow" :size="14"><ArrowRight /></el-icon>
      </button>
    </div>
    <EmptyState v-else title="暂无可用快捷入口" description="当前账号暂无可访问的快捷模块" />
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Grid, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { hasMenuPerm } from '@/utils/hasMenuPerm'
import EmptyState from '@/components/ui/EmptyState.vue'
import { quickEntries, type QuickEntry } from '../constants/quickEntries'

const router = useRouter()
const userStore = useUserStore()

const visibleEntries = computed(() =>
  quickEntries.filter((item) => hasMenuPerm(userStore.menus, item.permission)),
)

function goQuick(item: QuickEntry) {
  if (item.query) {
    router.push({ path: item.path, query: item.query })
  } else {
    router.push(item.path)
  }
}
</script>

<style scoped lang="scss">
.section-card {
  background: var(--color-bg-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  overflow: hidden;
  height: 100%;
  display: flex;
  flex-direction: column;
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

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-3);
  padding: var(--space-5);
}

.quick-item {
  display: flex;
  align-items: center;
  padding: var(--space-4);
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  cursor: pointer;
  gap: var(--space-3);
  text-align: left;
  width: 100%;

  &:hover {
    background: var(--color-bg-surface);
    border-color: var(--el-color-primary-light-5);

    .quick-arrow {
      color: var(--el-color-primary);
    }
  }
}

.quick-icon-wrapper {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  flex-shrink: 0;
  transition: transform var(--duration-fast) var(--ease-out);
}

.quick-icon-wrapper.user { background: rgba(79, 70, 229, 0.1); color: #6366f1; border-color: rgba(99, 102, 241, 0.22); }
.quick-icon-wrapper.menu { background: rgba(37, 99, 235, 0.1); color: #3b82f6; border-color: rgba(59, 130, 246, 0.22); }
.quick-icon-wrapper.dict { background: rgba(124, 58, 237, 0.1); color: #8b5cf6; border-color: rgba(139, 92, 246, 0.22); }
.quick-icon-wrapper.config { background: rgba(100, 116, 139, 0.12); color: var(--color-text-secondary); border-color: var(--color-border); }
.quick-icon-wrapper.approval { background: rgba(147, 51, 234, 0.1); color: #a855f7; border-color: rgba(168, 85, 247, 0.22); }
.quick-icon-wrapper.chat { background: rgba(234, 88, 12, 0.1); color: #f97316; border-color: rgba(249, 115, 22, 0.22); }
.quick-icon-wrapper.notice { background: rgba(219, 39, 119, 0.1); color: #ec4899; border-color: rgba(236, 72, 153, 0.22); }
.quick-icon-wrapper.job { background: rgba(13, 148, 136, 0.1); color: #14b8a6; border-color: rgba(20, 184, 166, 0.22); }
.quick-icon-wrapper.monitor { background: rgba(2, 132, 199, 0.1); color: #38bdf8; border-color: rgba(56, 189, 248, 0.22); }
.quick-icon-wrapper.server { background: rgba(109, 40, 217, 0.1); color: #8b5cf6; border-color: rgba(139, 92, 246, 0.22); }
.quick-icon-wrapper.cache { background: rgba(217, 119, 6, 0.1); color: #f59e0b; border-color: rgba(245, 158, 11, 0.22); }
.quick-icon-wrapper.log { background: rgba(225, 29, 72, 0.1); color: #f43f5e; border-color: rgba(244, 63, 94, 0.22); }

.quick-info {
  flex: 1;
  min-width: 0;
}

.quick-name {
  font-size: var(--text-body);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 2px;
}

.quick-desc {
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-arrow {
  color: var(--color-text-muted);
  transition:
    transform var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out);
  flex-shrink: 0;
}

@media (max-width: 1200px) {
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    padding: var(--space-4);
  }
}

@media (max-width: 640px) {
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
