import { ref, computed } from 'vue'
import { getMenuList } from '@/api/system/menu'
import { getRoleMenuIds, assignRoleMenu } from '@/api/system/role'
import { buildMenuTree, collectMenuNodeIds } from '@/utils/menu-tree'
import type { MenuVO } from '@/types/system'

export interface PermMenuRow {
  id: number
  name: string
  level: number
  type?: number
  hasChildren: boolean
}

function normalizeMenuTree(data: MenuVO[]) {
  if (!data.length) return []
  return data[0]?.children !== undefined ? data : buildMenuTree(data)
}

function buildNodeMaps(tree: MenuVO[]) {
  const idToNode = new Map<number, MenuVO>()
  const idToParent = new Map<number, number>()
  const walk = (nodes: MenuVO[], parentId?: number) => {
    for (const node of nodes) {
      idToNode.set(node.id, node)
      if (parentId != null) idToParent.set(node.id, parentId)
      if (node.children?.length) walk(node.children, node.id)
    }
  }
  walk(tree)
  return { idToNode, idToParent }
}

function flattenVisibleRows(tree: MenuVO[], expandedIds: Set<number>): PermMenuRow[] {
  const rows: PermMenuRow[] = []
  const walk = (nodes: MenuVO[], level: number) => {
    for (const node of nodes) {
      const hasChildren = !!(node.children?.length)
      rows.push({
        id: node.id,
        name: node.name,
        level,
        type: node.type,
        hasChildren,
      })
      if (hasChildren && expandedIds.has(node.id)) {
        walk(node.children!, level + 1)
      }
    }
  }
  walk(tree, 0)
  return rows
}

function collectParentIds(id: number, idToParent: Map<number, number>) {
  const ids: number[] = []
  let current = idToParent.get(id)
  while (current != null) {
    ids.push(current)
    current = idToParent.get(current)
  }
  return ids
}

export function useRoleMenuAssign() {
  const loading = ref(false)
  const saving = ref(false)
  const roleId = ref(0)
  const roleName = ref('')
  const menuTree = ref<MenuVO[]>([])
  const checkedIds = ref<Set<number>>(new Set())
  const expandedIds = ref<Set<number>>(new Set())
  const idToNode = ref(new Map<number, MenuVO>())
  const idToParent = ref(new Map<number, number>())
  const savedCheckedKey = ref('')

  const visibleRows = computed(() => flattenVisibleRows(menuTree.value, expandedIds.value))
  const checkedCount = computed(() => checkedIds.value.size)

  function serializeChecked(ids: Set<number>) {
    return [...ids].sort((a, b) => a - b).join(',')
  }

  const isDirty = computed(() => serializeChecked(checkedIds.value) !== savedCheckedKey.value)

  function syncSavedBaseline() {
    savedCheckedKey.value = serializeChecked(checkedIds.value)
  }

  function refreshMaps(tree: MenuVO[]) {
    const maps = buildNodeMaps(tree)
    idToNode.value = maps.idToNode
    idToParent.value = maps.idToParent
    expandedIds.value = new Set()
  }

  async function load(id: number, name?: string) {
    roleId.value = id
    roleName.value = name || ''
    loading.value = true
    try {
      const [menuRes, idsRes] = await Promise.all([getMenuList(), getRoleMenuIds(id)])
      menuTree.value = normalizeMenuTree(menuRes.data || [])
      refreshMaps(menuTree.value)
      checkedIds.value = new Set((idsRes.data || []).map(Number))
      syncSavedBaseline()
      uni.setNavigationBarTitle({ title: '分配权限' })
    } finally {
      loading.value = false
    }
  }

  function isExpanded(id: number) {
    return expandedIds.value.has(id)
  }

  function toggleExpand(id: number) {
    const next = new Set(expandedIds.value)
    if (next.has(id)) next.delete(id)
    else next.add(id)
    expandedIds.value = next
  }

  function isNodeFullyChecked(id: number): boolean {
    const node = idToNode.value.get(id)
    if (!node) return false
    if (!node.children?.length) return checkedIds.value.has(id)
    return node.children.every((child) => isNodeFullyChecked(child.id))
  }

  function isNodeIndeterminate(id: number): boolean {
    const node = idToNode.value.get(id)
    if (!node?.children?.length) return false
    if (isNodeFullyChecked(id)) return false
    return node.children.some(
      (child) => isNodeFullyChecked(child.id) || isNodeIndeterminate(child.id),
    )
  }

  function isChecked(id: number) {
    return isNodeFullyChecked(id)
  }

  function isIndeterminate(id: number) {
    return isNodeIndeterminate(id)
  }

  function setChecked(id: number, checked: boolean) {
    const node = idToNode.value.get(id)
    if (!node) return
    const next = new Set(checkedIds.value)
    const targetIds = collectMenuNodeIds(node)
    if (checked) {
      targetIds.forEach((tid) => next.add(tid))
      collectParentIds(id, idToParent.value).forEach((pid) => next.add(pid))
    } else {
      targetIds.forEach((tid) => next.delete(tid))
    }
    checkedIds.value = next
  }

  function toggleCheck(id: number) {
    if (isNodeFullyChecked(id)) setChecked(id, false)
    else setChecked(id, true)
  }

  async function save() {
    saving.value = true
    try {
      await assignRoleMenu({ roleId: roleId.value, menuIds: [...checkedIds.value] })
      syncSavedBaseline()
      uni.showToast({ title: '权限已保存', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 400)
    } finally {
      saving.value = false
    }
  }

  return {
    loading,
    saving,
    roleName,
    visibleRows,
    checkedCount,
    isDirty,
    load,
    isExpanded,
    toggleExpand,
    isChecked,
    isIndeterminate,
    toggleCheck,
    save,
  }
}
