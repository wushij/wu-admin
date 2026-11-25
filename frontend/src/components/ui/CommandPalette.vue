<template>
  <Teleport to="body">
    <Transition name="cmd-fade">
      <div
        v-if="visible"
        class="ui-cmd-backdrop"
        role="dialog"
        aria-modal="true"
        aria-label="全局快捷命令面板"
        @click.self="close"
      >
        <div class="ui-cmd-panel" @keydown="onPanelKeyDown">
          <div class="ui-cmd-search">
            <el-icon class="ui-cmd-search-icon" :size="18"><Search /></el-icon>
            <input
              ref="inputRef"
              v-model="keyword"
              type="text"
              class="ui-cmd-input"
              placeholder="搜索菜单页面、切换主题或发起引导… (Esc 关闭)"
            />
            <span class="ui-cmd-kbd">ESC</span>
          </div>

          <div class="ui-cmd-list">
            <div v-if="filteredItems.length === 0" class="ui-cmd-empty">
              未找到与「{{ keyword }}」匹配的菜单或操作
            </div>
            <button
              v-for="(item, idx) in filteredItems"
              :key="item.id"
              type="button"
              class="ui-cmd-item"
              :class="{ active: idx === activeIdx }"
              @mouseenter="activeIdx = idx"
              @click="execute(item)"
            >
              <span class="ui-cmd-item-icon" aria-hidden="true">
                <el-icon :size="16"><component :is="item.icon" /></el-icon>
              </span>
              <span class="ui-cmd-item-body">
                <span class="ui-cmd-item-title">{{ item.title }}</span>
                <span class="ui-cmd-item-sub">{{ item.subtitle }}</span>
              </span>
              <span class="ui-cmd-item-tag">{{ item.category }}</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch, onMounted, onUnmounted, type Component } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Moon, Sunny, Compass, DataAnalysis } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { resolveMenuIcon } from '@/utils/menu-icon'
import { buildSidebarMenuTree } from '@/utils/menu-tree'
import type { MenuNode } from '@/utils/menu-tree'

interface CommandEntry {
  id: string
  title: string
  subtitle: string
  category: string
  icon: Component
  run: () => void
}

const visible = defineModel<boolean>('visible', { default: false })
const props = defineProps<{
  isDark: boolean
}>()
const emit = defineEmits<{
  toggleDark: []
  startTour: []
}>()

const router = useRouter()
const userStore = useUserStore()
const keyword = ref('')
const activeIdx = ref(0)
const inputRef = ref<HTMLInputElement | null>(null)

/** 前端是否注册了该路径的正向路由（兜底 404 路由没有 name） */
function hasRegisteredRoute(path: string) {
  return router.resolve(path).matched.some((record) => Boolean(record.name))
}

function flattenMenus(nodes: MenuNode[], parentName = ''): CommandEntry[] {
  const result: CommandEntry[] = []
  for (const node of nodes) {
    // 按钮权限不承载页面，直接跳过
    if (node.type === 3) continue
    if (node.children?.length) {
      result.push(...flattenMenus(node.children as MenuNode[], node.name))
      continue
    }
    // 目录节点（如「系统管理」/system）本身不是页面，避免跳转到未注册路由
    if (node.type === 1 || !node.path) continue
    const targetPath = node.path.startsWith('/') ? node.path : `/${node.path}`
    if (!hasRegisteredRoute(targetPath)) continue
    result.push({
      id: `menu-${node.id}`,
      title: node.name,
      subtitle: parentName ? `${parentName} · ${targetPath}` : targetPath,
      category: '页面',
      icon: resolveMenuIcon(node.icon),
      run: () => {
        router.push(targetPath)
      },
    })
  }
  return result
}

const allEntries = computed<CommandEntry[]>(() => {
  // menus 为后端返回的扁平列表，先与侧栏一致地重建层级，再摊平成可跳转页面
  const menuTree = buildSidebarMenuTree(
    (userStore.menus || []).filter((menu) => menu.type !== 3) as MenuNode[],
  )
  const menuItems = flattenMenus(menuTree)
  return [
    {
      id: 'nav-dashboard',
      title: '工作台总览',
      subtitle: '返回系统首页与核心指标看板',
      category: '页面',
      icon: DataAnalysis,
      run: () => router.push('/dashboard'),
    },
    ...menuItems,
    {
      id: 'act-toggle-dark',
      title: props.isDark ? '切换为亮色模式' : '切换为暗黑模式',
      subtitle: '实时切换全局亮/暗黑双主题色彩系统',
      category: '外观',
      icon: props.isDark ? Sunny : Moon,
      run: () => emit('toggleDark'),
    },
    {
      id: 'act-tour',
      title: '开启工作台功能引导（Tour）',
      subtitle: '高亮介绍侧栏导航、命令面板、主题切换与 AI 助手',
      category: '帮助',
      icon: Compass,
      run: () => emit('startTour'),
    },
  ]
})

const filteredItems = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q) return allEntries.value.slice(0, 12)
  return allEntries.value
    .filter(
      (entry) =>
        entry.title.toLowerCase().includes(q) ||
        entry.subtitle.toLowerCase().includes(q) ||
        entry.category.toLowerCase().includes(q),
    )
    .slice(0, 14)
})

watch(filteredItems, () => {
  activeIdx.value = 0
})

watch(visible, async (val) => {
  if (val) {
    keyword.value = ''
    activeIdx.value = 0
    await nextTick()
    inputRef.value?.focus()
  }
})

function close() {
  visible.value = false
}

function execute(item: CommandEntry) {
  close()
  item.run()
}

function onPanelKeyDown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    e.preventDefault()
    close()
    return
  }
  if (!filteredItems.value.length) return
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    activeIdx.value = (activeIdx.value + 1) % filteredItems.value.length
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    activeIdx.value = (activeIdx.value - 1 + filteredItems.value.length) % filteredItems.value.length
  } else if (e.key === 'Enter') {
    e.preventDefault()
    const target = filteredItems.value[activeIdx.value]
    if (target) execute(target)
  }
}

function onGlobalKeyDown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    visible.value = !visible.value
  }
}

onMounted(() => window.addEventListener('keydown', onGlobalKeyDown))
onUnmounted(() => window.removeEventListener('keydown', onGlobalKeyDown))
</script>

<style scoped lang="scss">
.ui-cmd-backdrop {
  position: fixed;
  inset: 0;
  z-index: var(--z-modal, 2100);
  background: var(--color-backdrop);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 12vh var(--space-4) var(--space-4);
}

.ui-cmd-panel {
  width: 100%;
  max-width: 560px;
  border-radius: var(--radius-lg);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-float);
  overflow: hidden;
}

.ui-cmd-search {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--color-border-subtle);
}

.ui-cmd-search-icon {
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.ui-cmd-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  color: var(--color-text-primary);
  font-size: var(--text-body);

  &::placeholder {
    color: var(--color-text-muted);
  }
}

.ui-cmd-kbd {
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
}

.ui-cmd-list {
  max-height: 360px;
  overflow-y: auto;
  padding: var(--space-2);
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ui-cmd-empty {
  padding: var(--space-6) var(--space-4);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-muted);
}

.ui-cmd-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  padding: var(--space-2) var(--space-3);
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-primary);
  text-align: left;
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-out);

  &.active {
    background: var(--el-color-primary-light-9);
  }
}

.ui-cmd-item-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: var(--color-bg-subtle);
  color: var(--el-color-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ui-cmd-item-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.ui-cmd-item-title {
  font-size: var(--text-body);
  font-weight: 500;
  color: var(--color-text-primary);
}

.ui-cmd-item-sub {
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ui-cmd-item-tag {
  padding: 2px 10px;
  border-radius: var(--radius-pill);
  font-size: 11px;
  color: var(--color-text-secondary);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  flex-shrink: 0;
}

.cmd-fade-enter-active,
.cmd-fade-leave-active {
  transition: opacity var(--duration-fast) var(--ease-out);
}

.cmd-fade-enter-from,
.cmd-fade-leave-to {
  opacity: 0;
}
</style>
