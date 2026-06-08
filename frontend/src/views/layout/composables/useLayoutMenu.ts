import { ref, computed, onUnmounted } from 'vue'
import { useUserStore } from '@/store/user'
import { resolveMenuIcon } from '@/utils/menu-icon'
import { buildSidebarMenuTree, type MenuNode } from '@/utils/menu-tree'
import type { Component } from 'vue'

const SYSTEM_MENU_ID = '1'
const MENU_ICON_SPIN_MS = 520

export function useLayoutMenu() {
  const userStore = useUserStore()
  const menuIconSpinKey = ref('')
  let menuIconSpinTimer: number | null = null

  const getIconComponent = (iconName?: string): Component => resolveMenuIcon(iconName) as Component

  const isSystemMenu = (menu: MenuNode) =>
    String(menu?.id) === SYSTEM_MENU_ID || menu?.name === '系统管理'

  const userMenus = computed<MenuNode[]>(() => {
    const menus = userStore.menus || []
    const filteredMenus = menus.filter((menu) => menu.type !== 3) as MenuNode[]
    const tree = buildSidebarMenuTree(filteredMenus)

    const sortMenus = (items: MenuNode[]) => {
      items.sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0) || a.id - b.id)
      items.forEach((menu) => {
        if (menu.children?.length) sortMenus(menu.children as MenuNode[])
      })
    }
    sortMenus(tree)
    return tree
  })

  function resolveMenuIndex(menu: MenuNode) {
    const comp = menu?.component?.trim()
    if (comp && /^https?:\/\//i.test(comp)) {
      return `external:${comp}`
    }
    return menu.path || String(menu.id)
  }

  function findTopMenuSpinKey(index: string) {
    const key = String(index)
    if (key === '/dashboard') return '/dashboard'
    for (const menu of userMenus.value) {
      if (String(menu.id) === key) return String(menu.id)
      if (resolveMenuIndex(menu) === key) return String(menu.id)
      if (menu.children?.length) {
        for (const child of menu.children) {
          if (resolveMenuIndex(child) === key) return String(menu.id)
        }
      }
    }
    return key
  }

  function triggerSystemMenuIconSpin() {
    if (menuIconSpinTimer) {
      window.clearTimeout(menuIconSpinTimer)
      menuIconSpinTimer = null
    }
    menuIconSpinKey.value = ''
    requestAnimationFrame(() => {
      menuIconSpinKey.value = SYSTEM_MENU_ID
      menuIconSpinTimer = window.setTimeout(() => {
        if (menuIconSpinKey.value === SYSTEM_MENU_ID) {
          menuIconSpinKey.value = ''
        }
        menuIconSpinTimer = null
      }, MENU_ICON_SPIN_MS)
    })
  }

  function handleSubMenuOpen(menu: MenuNode) {
    if (isSystemMenu(menu)) triggerSystemMenuIconSpin()
  }

  function handleSubMenuClose(menu: MenuNode) {
    if (isSystemMenu(menu)) triggerSystemMenuIconSpin()
  }

  function handleMenuSelect(index: string) {
    if (findTopMenuSpinKey(index) === SYSTEM_MENU_ID) {
      triggerSystemMenuIconSpin()
    }
    const key = String(index)
    if (key.startsWith('external:')) {
      window.open(key.slice('external:'.length), '_blank')
    }
  }

  onUnmounted(() => {
    if (menuIconSpinTimer) {
      window.clearTimeout(menuIconSpinTimer)
      menuIconSpinTimer = null
    }
  })

  return {
    SYSTEM_MENU_ID,
    menuIconSpinKey,
    userMenus,
    getIconComponent,
    isSystemMenu,
    resolveMenuIndex,
    handleSubMenuOpen,
    handleSubMenuClose,
    handleMenuSelect,
  }
}
