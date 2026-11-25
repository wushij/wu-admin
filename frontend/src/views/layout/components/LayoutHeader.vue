<template>
  <header class="header">
    <div class="header-left">
      <button
        type="button"
        class="collapse-btn"
        :aria-label="isCollapse ? '展开侧边栏' : '收起侧边栏'"
        @click="$emit('toggleCollapse')"
      >
        <el-icon :size="18">
          <component :is="isCollapse ? ElementPlusIconsVue.Expand : ElementPlusIconsVue.Fold" />
        </el-icon>
      </button>
      <el-breadcrumb separator="/" class="header-breadcrumb">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">工作台</el-breadcrumb-item>
        <el-breadcrumb-item v-if="currentPageTitle && currentPageTitle !== '工作台'">
          {{ currentPageTitle }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="header-right">
      <el-tooltip content="快捷跳转菜单与主题 (Ctrl + K)" placement="bottom" :show-after="1000">
        <button
          id="tour-command-btn"
          type="button"
          class="command-trigger"
          aria-label="快捷跳转菜单与主题 (Ctrl + K)"
          @click="$emit('openCommand')"
        >
          <el-icon :size="15"><ElementPlusIconsVue.Search /></el-icon>
          <span class="command-trigger__text">快速跳转…</span>
          <kbd class="command-trigger__kbd">Ctrl K</kbd>
        </button>
      </el-tooltip>

      <div
        class="header-action-group"
        :class="{ 'is-dark': isDark }"
        :style="{ '--dock-index': activeDockIndex }"
        role="group"
        aria-label="快捷工具分段栏"
      >
        <span class="dock-backdrop" aria-hidden="true">
          <span class="dock-stars">
            <span class="dock-star star-1" />
            <span class="dock-star star-2" />
            <span class="dock-star star-3" />
            <span class="dock-star star-4" />
          </span>
        </span>
        <span class="dock-thumb" aria-hidden="true" />

        <el-tooltip content="工作台功能引导" placement="bottom" :show-after="1000">
          <button
            type="button"
            class="dock-segment-btn"
            :class="{ 'is-active': activeDockIndex === 0 }"
            aria-label="开启功能引导"
            @click="handleTourClick"
          >
            <el-icon :size="15"><ElementPlusIconsVue.Compass /></el-icon>
          </button>
        </el-tooltip>

        <LayoutThemeToggle
          :is-dark="isDark"
          :is-active="activeDockIndex === 1"
          :active-preset-id="activePresetId"
          @toggle="handleThemeToggle"
        />

        <LayoutMessagePopover
          v-model:message-tab="messageTab"
          :inbox-list="inboxList"
          :announce-list="announceList"
          :is-active="activeDockIndex === 2"
          @activate="activeDockIndex = 2"
          @show="handleMessagesShow"
          @read-inbox="$emit('readInbox', $event)"
          @read-announce="$emit('readAnnounce', $event)"
          @read-all-inbox="$emit('readAllInbox')"
          @read-all-announce="$emit('readAllAnnounce')"
        />

        <LayoutThemePicker
          :current-color="currentColor"
          :color-mode="colorMode"
          :active-preset-id="activePresetId"
          :is-active="activeDockIndex === 3"
          @activate="activeDockIndex = 3"
          @preset-select="(id, origin) => $emit('presetSelect', id, origin)"
          @color-change="$emit('colorChange', $event)"
          @color-mode-change="(mode, origin) => $emit('colorModeChange', mode, origin)"
        />
      </div>

      <div class="header-divider" aria-hidden="true" />

      <el-dropdown @command="$emit('userCommand', $event)">
        <div class="user-info" role="button" tabindex="0">
          <el-avatar :size="28" :src="headerAvatarSrc" class="header-avatar" fit="cover">
            <el-icon v-if="!headerAvatarSrc"><component :is="ElementPlusIconsVue.UserFilled" /></el-icon>
          </el-avatar>
          <span class="username">{{ nickname }}</span>
          <el-icon class="user-arrow"><component :is="ElementPlusIconsVue.ArrowDown" /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import type { InboxNoticeItem } from '@/store/message'
import type { AnnounceMyVO } from '@/types/message'
import type { ColorMode, ThemeTransitionOrigin } from '@/utils/theme'
import LayoutMessagePopover from './LayoutMessagePopover.vue'
import LayoutThemePicker from './LayoutThemePicker.vue'
import LayoutThemeToggle from './LayoutThemeToggle.vue'

defineProps<{
  isCollapse: boolean
  currentPageTitle: string
  currentColor: string
  colorMode: ColorMode
  isDark: boolean
  activePresetId: string | undefined
  headerAvatarSrc: string | undefined
  nickname: string
  inboxList: InboxNoticeItem[]
  announceList: AnnounceMyVO[]
}>()

const messageTab = defineModel<string>('messageTab', { default: 'inbox' })

const emit = defineEmits<{
  toggleCollapse: []
  toggleDark: [origin?: ThemeTransitionOrigin]
  openCommand: []
  startTour: []
  loadMessages: []
  readInbox: [item: InboxNoticeItem]
  readAnnounce: [item: AnnounceMyVO]
  readAllInbox: []
  readAllAnnounce: []
  presetSelect: [presetId: string, origin?: ThemeTransitionOrigin]
  colorChange: [color: string | null]
  colorModeChange: [mode: ColorMode, origin?: ThemeTransitionOrigin]
  userCommand: [command: string]
}>()

// 胶囊分段滑块索引：默认定位至明暗模式分段（槽位 1），点击任意工位平滑滑入对应分段
const activeDockIndex = ref(1)

function handleTourClick() {
  activeDockIndex.value = 0
  emit('startTour')
}

function handleThemeToggle(origin?: ThemeTransitionOrigin) {
  activeDockIndex.value = 1
  emit('toggleDark', origin)
}

function handleMessagesShow() {
  activeDockIndex.value = 2
  emit('loadMessages')
}
</script>

<style scoped lang="scss">
.header {
  height: 60px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-4);
  padding: 0 var(--space-5);
  background: var(--color-header-bg);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--color-border-subtle);
  flex-shrink: 0;
  z-index: var(--z-header);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
}

.collapse-btn {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-circle);
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition:
    border-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out),
    background-color var(--duration-fast) var(--ease-out);

  &:hover {
    border-color: var(--el-color-primary);
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }
}

.header-breadcrumb {
  white-space: nowrap;
  overflow: hidden;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-shrink: 0;
}

.command-trigger {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  height: 34px;
  padding: 0 var(--space-3);
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition:
    border-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out),
    background-color var(--duration-fast) var(--ease-out);

  &:hover {
    border-color: var(--el-color-primary-light-5);
    color: var(--color-text-primary);
    background: var(--color-bg-surface);
  }
}

.command-trigger__kbd {
  padding: 1px 7px;
  border-radius: var(--radius-pill);
  font-size: 11px;
  font-family: var(--font-sans);
  font-weight: 600;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
}

/* ── 四工位胶囊分段切换坞（复刻深空星海 / 浅柔珠光轨道与弹簧圆盘） ── */
.header-action-group {
  --dock-pad: 3px;
  --dock-slot: 28px;
  --dock-gap: 4px;
  box-sizing: border-box;
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: var(--dock-gap);
  height: 36px;
  padding: var(--dock-pad);
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-border);
  background: linear-gradient(135deg, #f8fafc 0%, #eff6ff 100%);
  box-shadow:
    inset 0 1px 2px rgba(15, 23, 42, 0.05),
    var(--shadow-xs);
  flex-shrink: 0;
  transition:
    border-color var(--duration-fast) var(--ease-out),
    background 0.35s var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);

  &:hover {
    border-color: var(--el-color-primary-light-5);
  }

  &.is-dark {
    background: linear-gradient(135deg, #090f1f 0%, #172554 100%);
    border-color: rgba(148, 163, 184, 0.28);
    box-shadow:
      inset 0 1px 2px rgba(0, 0, 0, 0.45),
      0 2px 10px rgba(2, 6, 23, 0.45);

    &:hover {
      border-color: rgba(147, 197, 253, 0.46);
    }
  }
}

.dock-backdrop {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

.dock-stars {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.header-action-group.is-dark .dock-stars {
  opacity: 1;
}

.dock-star {
  position: absolute;
  border-radius: 50%;
  background: #e0f2fe;
  box-shadow: 0 0 4px rgba(125, 211, 252, 0.9);
  animation: dock-star-twinkle 3s ease-in-out infinite;
}

.star-1 {
  width: 2px;
  height: 2px;
  top: 7px;
  left: 16px;
}

.star-2 {
  width: 1.5px;
  height: 1.5px;
  top: 22px;
  left: 48px;
  animation-delay: 0.7s;
}

.star-3 {
  width: 1.5px;
  height: 1.5px;
  top: 9px;
  left: 82px;
  animation-delay: 1.5s;
}

.star-4 {
  width: 2px;
  height: 2px;
  top: 20px;
  left: 112px;
  animation-delay: 2.2s;
}

@keyframes dock-star-twinkle {
  0%,
  100% {
    opacity: 0.38;
    transform: scale(0.9);
  }
  50% {
    opacity: 0.95;
    transform: scale(1.22);
  }
}

.dock-thumb {
  box-sizing: border-box;
  position: absolute;
  top: 50%;
  left: var(--dock-pad);
  z-index: 1;
  width: var(--dock-slot);
  height: var(--dock-slot);
  border-radius: var(--radius-circle);
  pointer-events: none;
  background: #ffffff;
  box-shadow:
    0 2px 6px rgba(15, 23, 42, 0.14),
    0 0 0 1px rgba(15, 23, 42, 0.04);
  transform: translate3d(calc(var(--dock-index, 1) * (var(--dock-slot) + var(--dock-gap))), -50%, 0);
  transition:
    transform 0.38s var(--ease-spring),
    background-color 0.28s var(--ease-out),
    box-shadow 0.28s var(--ease-out);
}

.header-action-group.is-dark .dock-thumb {
  background: #172554;
  box-shadow:
    0 2px 8px rgba(2, 6, 23, 0.6),
    0 0 0 1px rgba(147, 197, 253, 0.36);
}

.header-action-group :deep(.dock-segment-btn) {
  box-sizing: border-box;
  position: relative;
  z-index: 2;
  width: var(--dock-slot);
  height: var(--dock-slot);
  margin: 0;
  padding: 0;
  border: none;
  border-radius: var(--radius-circle);
  background: transparent;
  color: #64748b;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition:
    color var(--duration-fast) var(--ease-out),
    transform var(--duration-fast) var(--ease-out);

  &:hover:not(.is-active) {
    color: var(--color-text-primary);
  }

  &.is-active:not(.is-sun) {
    color: var(--el-color-primary);
  }
}

.header-action-group.is-dark :deep(.dock-segment-btn) {
  color: rgba(226, 232, 240, 0.62);

  &:hover:not(.is-active) {
    color: #e0f2fe;
  }

  &.is-active {
    color: #93c5fd;
  }
}

@media (prefers-reduced-motion: reduce) {
  .dock-star {
    animation: none;
  }

  .dock-thumb,
  .header-action-group :deep(.dock-segment-btn) {
    transition: none;
  }
}

.header-divider {
  width: 1px;
  height: 18px;
  background: var(--color-border);
}

.user-info {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: 3px 12px 3px 4px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  cursor: pointer;
  user-select: none;
  transition:
    border-color var(--duration-fast) var(--ease-out),
    background-color var(--duration-fast) var(--ease-out),
    transform var(--duration-fast) var(--ease-out);

  &:hover {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);

    .user-arrow {
      color: var(--el-color-primary);
      transform: rotate(180deg);
    }
  }
}

.header-avatar {
  flex-shrink: 0;
}

.username {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
  max-width: 112px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-arrow {
  font-size: 12px;
  color: var(--color-text-muted);
  transition:
    transform var(--duration-normal) var(--ease-out),
    color var(--duration-fast) var(--ease-out);
}

@media (max-width: 900px) {
  .command-trigger__text,
  .command-trigger__kbd {
    display: none;
  }

  .command-trigger {
    width: 34px;
    padding: 0;
    justify-content: center;
  }
}

@media (max-width: 640px) {
  .header {
    padding: 0 var(--space-3);
  }

  .header-breadcrumb,
  .username,
  .header-divider {
    display: none;
  }

  .user-info {
    padding: 3px;
  }
}
</style>
