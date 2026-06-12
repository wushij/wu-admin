import { ref, computed } from 'vue'
import { getDeptTree, getPostTree } from '@/api/system/dept'
import type { DeptVO, PostVO } from '@/types/system'
import { postDisplayName } from '@/utils/post-tree'
import {
  buildDeptNodeMaps,
  buildDeptPath,
  buildDeptLabels,
  flattenDeptRows,
  buildDeptExpandedIds,
  buildDeptExpandedIdsForMulti,
  pruneDeptSubtree,
} from '@/utils/dept-tree'
import {
  buildPostNodeMaps,
  flattenPostRows,
  buildPostExpandedIds,
  buildPostLabels,
  prunePostSubtree,
} from '@/utils/post-tree'

export type TreeSelectType = 'dept' | 'post'

const EMPTY_LABEL = '未分配'

export interface TreeSelectRow {
  id: number
  name: string
  level: number
  hasChildren: boolean
  extra?: string
}

export function useTreeSelectPage() {
  const loading = ref(false)
  const selectType = ref<TreeSelectType>('dept')
  const multiple = ref(false)
  const allowEmpty = ref(false)
  const selectedId = ref<number | null>(null)
  const initialSelectedId = ref<number | null>(null)
  const selectedIds = ref<number[]>([])
  const initialSelectedIds = ref<number[]>([])
  const deptTree = ref<DeptVO[]>([])
  const postTree = ref<PostVO[]>([])
  const expandedIds = ref<Set<number>>(new Set())
  const idToDept = ref(new Map<number, DeptVO>())
  const idToDeptParent = ref(new Map<number, number>())
  const idToPost = ref(new Map<number, PostVO>())
  const idToPostParent = ref(new Map<number, number>())
  const emptyLabelText = ref(EMPTY_LABEL)
  const emptyLabel = computed(() => emptyLabelText.value)

  const visibleRows = computed<TreeSelectRow[]>(() => {
    if (selectType.value === 'post') {
      return flattenPostRows(postTree.value, expandedIds.value)
    }
    return flattenDeptRows(deptTree.value, expandedIds.value).map((row) => ({
      id: row.id,
      name: row.name,
      level: row.level,
      hasChildren: row.hasChildren,
    }))
  })

  const isDirty = computed(() => {
    if (multiple.value) {
      const a = [...selectedIds.value].sort((x, y) => x - y)
      const b = [...initialSelectedIds.value].sort((x, y) => x - y)
      return JSON.stringify(a) !== JSON.stringify(b)
    }
    return selectedId.value !== initialSelectedId.value
  })

  const selectedLabel = computed(() => {
    if (multiple.value && selectType.value === 'dept') {
      if (!selectedIds.value.length) return EMPTY_LABEL
      const labels = buildDeptLabels(selectedIds.value, idToDept.value)
      return labels.length ? labels.join('、') : EMPTY_LABEL
    }
    if (multiple.value) {
      if (!selectedIds.value.length) return EMPTY_LABEL
      const labels = buildPostLabels(selectedIds.value, idToPost.value)
      return labels.length ? labels.join('、') : EMPTY_LABEL
    }
    if (selectedId.value == null) return emptyLabelText.value
    if (selectType.value === 'post') {
      const node = idToPost.value.get(selectedId.value)
      return node ? postDisplayName(node) : emptyLabelText.value
    }
    return buildDeptPath(selectedId.value, idToDept.value, idToDeptParent.value) || emptyLabelText.value
  })

  function commitSelection() {
    if (multiple.value) {
      initialSelectedIds.value = [...selectedIds.value]
    } else {
      initialSelectedId.value = selectedId.value
    }
  }

  async function loadDept(
    initialId: number | null,
    emptyAllowed: boolean,
    options?: { excludeId?: number; emptyLabel?: string },
  ) {
    selectType.value = 'dept'
    multiple.value = false
    allowEmpty.value = emptyAllowed
    emptyLabelText.value = options?.emptyLabel ?? EMPTY_LABEL
    selectedId.value = initialId
    initialSelectedId.value = initialId
    loading.value = true
    try {
      const res = await getDeptTree({ status: 1 })
      deptTree.value = pruneDeptSubtree(res.data || [], options?.excludeId)
      const maps = buildDeptNodeMaps(deptTree.value)
      idToDept.value = maps.idToNode
      idToDeptParent.value = maps.idToParent
      expandedIds.value = buildDeptExpandedIds(deptTree.value, maps.idToParent, initialId)
    } finally {
      loading.value = false
    }
  }

  async function loadPostParent(
    initialId: number | null,
    emptyAllowed: boolean,
    options?: { excludeId?: number; emptyLabel?: string },
  ) {
    selectType.value = 'post'
    multiple.value = false
    allowEmpty.value = emptyAllowed
    emptyLabelText.value = options?.emptyLabel ?? EMPTY_LABEL
    selectedId.value = initialId
    initialSelectedId.value = initialId
    loading.value = true
    try {
      const res = await getPostTree()
      postTree.value = prunePostSubtree(filterPostTree(res.data || []), options?.excludeId)
      const maps = buildPostNodeMaps(postTree.value)
      idToPost.value = maps.idToNode
      idToPostParent.value = maps.idToParent
      expandedIds.value = buildPostExpandedIds(postTree.value, maps.idToParent, initialId != null ? [initialId] : [])
    } finally {
      loading.value = false
    }
  }

  async function loadDeptMulti(initial: number[]) {
    selectType.value = 'dept'
    multiple.value = true
    allowEmpty.value = false
    selectedIds.value = [...initial]
    initialSelectedIds.value = [...initial]
    loading.value = true
    try {
      const res = await getDeptTree({ status: 1 })
      deptTree.value = res.data || []
      const maps = buildDeptNodeMaps(deptTree.value)
      idToDept.value = maps.idToNode
      idToDeptParent.value = maps.idToParent
      expandedIds.value = buildDeptExpandedIdsForMulti(deptTree.value, maps.idToParent, initial)
    } finally {
      loading.value = false
    }
  }

  async function loadPost(initial: number[]) {
    selectType.value = 'post'
    multiple.value = true
    allowEmpty.value = false
    selectedIds.value = [...initial]
    initialSelectedIds.value = [...initial]
    loading.value = true
    try {
      const res = await getPostTree()
      postTree.value = filterPostTree(res.data || [])
      const maps = buildPostNodeMaps(postTree.value)
      idToPost.value = maps.idToNode
      idToPostParent.value = maps.idToParent
      expandedIds.value = buildPostExpandedIds(postTree.value, maps.idToParent, initial)
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

  function selectId(id: number | null) {
    if (multiple.value) return
    if (id != null && selectedId.value === id) return
    selectedId.value = id
  }

  function toggleId(id: number) {
    if (!multiple.value) return
    const idx = selectedIds.value.indexOf(id)
    if (idx >= 0) {
      selectedIds.value = selectedIds.value.filter((v) => v !== id)
    } else {
      selectedIds.value = [...selectedIds.value, id]
    }
  }

  function isSelected(id: number | null) {
    if (multiple.value) {
      return id != null && selectedIds.value.includes(id)
    }
    return selectedId.value === id
  }

  function onRowTap(id: number) {
    if (multiple.value) toggleId(id)
    else selectId(id)
  }

  return {
    loading,
    multiple,
    allowEmpty,
    selectedId,
    selectedIds,
    selectedLabel,
    visibleRows,
    isDirty,
    loadDept,
    loadDeptMulti,
    loadPostParent,
    loadPost,
    commitSelection,
    isExpanded,
    toggleExpand,
    selectId,
    toggleId,
    onRowTap,
    isSelected,
    emptyLabel,
  }
}

function filterPostTree(nodes: PostVO[]): PostVO[] {
  return nodes
    .filter((node) => node.status == null || node.status === 1)
    .map((node) => ({
      ...node,
      children: node.children?.length ? filterPostTree(node.children) : node.children,
    }))
}
