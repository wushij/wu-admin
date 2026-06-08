import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteLocationNormalized } from 'vue-router'

export interface TagView {
  path: string
  fullPath: string
  title: string
  name?: string
  affix?: boolean
}

const STORAGE_KEY = 'tags-view:visited'
/** 除固定标签外，最多保留最近访问的页签数量 */
const MAX_HISTORY = 8

const AFFIX_TAGS: TagView[] = [
  {
    path: '/dashboard',
    fullPath: '/dashboard',
    title: '工作台',
    name: 'Dashboard',
    affix: true,
  },
]

function loadCached(): TagView[] {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw) as TagView[]
    return Array.isArray(parsed) ? parsed.filter((t) => t?.path && t?.title) : []
  } catch {
    return []
  }
}

function saveCached(views: TagView[]) {
  const persistable = views.filter((v) => !v.affix)
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(persistable))
}

/** 固定标签始终保留，普通标签只保留最近 MAX_HISTORY 个 */
function trimToMax(views: TagView[]): TagView[] {
  const affix = views.filter((v) => v.affix)
  const normal = views.filter((v) => !v.affix)
  if (normal.length <= MAX_HISTORY) {
    return [...affix, ...normal]
  }
  return [...affix, ...normal.slice(normal.length - MAX_HISTORY)]
}

export const useTagsViewStore = defineStore('tagsView', () => {
  const visitedViews = ref<TagView[]>([])

  function initTags() {
    const cached = loadCached()
    const recent = cached.length > MAX_HISTORY ? cached.slice(-MAX_HISTORY) : cached
    const merged: TagView[] = [...AFFIX_TAGS]
    for (const tag of recent) {
      if (!merged.some((item) => item.path === tag.path)) {
        merged.push(tag)
      }
    }
    visitedViews.value = trimToMax(merged)
    saveCached(visitedViews.value)
  }

  function addView(route: RouteLocationNormalized) {
    const title = route.meta?.title
    if (typeof title !== 'string' || !title) return
    if (route.path === '/login' || route.path === '/register') return

    const existing = visitedViews.value.find((v) => v.path === route.path)
    if (existing) {
      // 已打开的页签：仅同步地址（如 query），不改变顺序
      existing.fullPath = route.fullPath
      existing.title = title
      saveCached(visitedViews.value)
      return
    }

    visitedViews.value.push({
      path: route.path,
      fullPath: route.fullPath,
      title,
      name: typeof route.name === 'string' ? route.name : undefined,
      affix: route.path === '/dashboard',
    })
    visitedViews.value = trimToMax(visitedViews.value)
    saveCached(visitedViews.value)
  }

  function delView(tag: TagView, currentPath: string): string {
    if (tag.affix) return currentPath === tag.path ? '/dashboard' : ''

    const index = visitedViews.value.findIndex((v) => v.path === tag.path)
    if (index < 0) return ''

    visitedViews.value.splice(index, 1)
    saveCached(visitedViews.value)

    if (currentPath !== tag.path) return ''

    const next = visitedViews.value[index] ?? visitedViews.value[index - 1]
    return next?.fullPath ?? '/dashboard'
  }

  function resetTags() {
    visitedViews.value = [...AFFIX_TAGS]
    sessionStorage.removeItem(STORAGE_KEY)
  }

  return {
    visitedViews,
    initTags,
    addView,
    delView,
    resetTags,
  }
})
