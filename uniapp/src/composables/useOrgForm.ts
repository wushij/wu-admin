import { ref, reactive } from 'vue'
import {
  getDept,
  getDeptTree,
  createDept,
  updateDept,
  getPost,
  getPostTree,
  createPost,
  updatePost,
} from '@/api/system/dept'
import { findDeptPathLabel } from '@/utils/dept-tree'
import { postDisplayName } from '@/utils/post-tree'
import type { DeptSaveDTO, DeptVO, PostSaveDTO, PostVO } from '@/types/system'

export type OrgEntity = 'dept' | 'post'

function findPostPathLabel(tree: PostVO[], id: number | null | undefined) {
  if (id == null || id <= 0) return '顶级岗位'
  const maps = new Map<number, PostVO>()
  const parents = new Map<number, number>()
  const walk = (nodes: PostVO[], parentId?: number) => {
    for (const node of nodes) {
      maps.set(node.id, node)
      if (parentId != null) parents.set(node.id, parentId)
      if (node.children?.length) walk(node.children, node.id)
    }
  }
  walk(tree)
  const names: string[] = []
  let current: number | undefined = id
  while (current != null) {
    const node = maps.get(current)
    if (!node) break
    names.unshift(postDisplayName(node))
    current = parents.get(current)
  }
  return names.length ? names.join(' / ') : '顶级岗位'
}

export function useOrgForm() {
  const loading = ref(false)
  const saving = ref(false)
  const isCreate = ref(false)
  const entity = ref<OrgEntity>('dept')
  const parentLabel = ref('顶级部门')
  const excludeParentId = ref<number | undefined>(undefined)
  const deptTreeCache = ref<DeptVO[]>([])
  const postTreeCache = ref<PostVO[]>([])
  const leaderLabel = ref('不设置')

  const deptForm = reactive<DeptSaveDTO>({
    parentId: 0,
    name: '',
    leaderUserId: null,
    phone: '',
    email: '',
    sort: 0,
    status: 1,
  })

  const postForm = reactive<PostSaveDTO>({
    parentId: 0,
    postCode: '',
    postName: '',
    sort: 0,
    status: 1,
    remark: '',
  })

  const statusOptions = [
    { label: '启用', value: 1 },
    { label: '停用', value: 0 },
  ]
  const statusIndex = ref(0)

  async function refreshParentLabel() {
    if (entity.value === 'dept') {
      if (!deptTreeCache.value.length) {
        const res = await getDeptTree()
        deptTreeCache.value = res.data || []
      }
      const pid = deptForm.parentId ?? 0
      parentLabel.value = pid > 0 ? findDeptPathLabel(deptTreeCache.value, pid) : '顶级部门'
      return
    }
    if (!postTreeCache.value.length) {
      const res = await getPostTree()
      postTreeCache.value = res.data || []
    }
    const pid = postForm.parentId ?? 0
    parentLabel.value = findPostPathLabel(postTreeCache.value, pid > 0 ? pid : null)
  }

  function syncStatusIndex(status?: number) {
    const idx = statusOptions.findIndex((s) => s.value === (status ?? 1))
    statusIndex.value = idx >= 0 ? idx : 0
  }

  function setLeaderSelection(id: number | null | undefined, label?: string) {
    deptForm.leaderUserId = id == null || id <= 0 ? null : id
    leaderLabel.value = label || (deptForm.leaderUserId ? '已选择' : '不设置')
  }

  function setParentSelection(id: number | null | undefined, label?: string) {
    const parentId = id == null || id <= 0 ? 0 : id
    if (entity.value === 'dept') {
      deptForm.parentId = parentId
      parentLabel.value = label || (parentId > 0 ? findDeptPathLabel(deptTreeCache.value, parentId) : '顶级部门')
      return
    }
    postForm.parentId = parentId
    parentLabel.value = label || findPostPathLabel(postTreeCache.value, parentId > 0 ? parentId : null)
  }

  async function initCreate(ent: OrgEntity, parentId?: number) {
    entity.value = ent
    isCreate.value = true
    excludeParentId.value = undefined
    if (ent === 'dept') {
      const res = await getDeptTree()
      deptTreeCache.value = res.data || []
      deptForm.parentId = parentId ?? 0
      deptForm.name = ''
      deptForm.leaderUserId = null
      leaderLabel.value = '不设置'
      deptForm.phone = ''
      deptForm.email = ''
      deptForm.sort = 0
      deptForm.status = 1
      setParentSelection(deptForm.parentId)
      syncStatusIndex(1)
      uni.setNavigationBarTitle({ title: '新增部门' })
      return
    }
    const res = await getPostTree()
    postTreeCache.value = res.data || []
    postForm.parentId = parentId ?? 0
    postForm.postCode = ''
    postForm.postName = ''
    postForm.sort = 0
    postForm.status = 1
    postForm.remark = ''
    setParentSelection(postForm.parentId)
    syncStatusIndex(1)
    uni.setNavigationBarTitle({ title: '新增岗位' })
  }

  async function loadEdit(ent: OrgEntity, id: number) {
    entity.value = ent
    isCreate.value = false
    excludeParentId.value = id
    loading.value = true
    try {
      if (ent === 'dept') {
        const [deptRes, treeRes] = await Promise.all([getDept(id), getDeptTree()])
        const data = deptRes.data
        deptTreeCache.value = treeRes.data || []
        if (!data) return
        deptForm.id = data.id
        deptForm.parentId = data.parentId ?? 0
        deptForm.name = data.name
        deptForm.leaderUserId = data.leaderUserId ?? null
        leaderLabel.value = data.leaderName || (deptForm.leaderUserId ? '已选择' : '不设置')
        deptForm.phone = data.phone || ''
        deptForm.email = data.email || ''
        deptForm.sort = data.sort ?? 0
        deptForm.status = data.status ?? 1
        setParentSelection(deptForm.parentId)
        syncStatusIndex(deptForm.status)
        uni.setNavigationBarTitle({ title: '编辑部门' })
        return
      }
      const [postRes, treeRes] = await Promise.all([getPost(id), getPostTree()])
      const data = postRes.data
      postTreeCache.value = treeRes.data || []
      if (!data) return
      postForm.id = data.id
      postForm.parentId = data.parentId ?? 0
      postForm.postCode = data.postCode || data.code || ''
      postForm.postName = data.postName || data.name || ''
      postForm.sort = data.sort ?? 0
      postForm.status = data.status ?? 1
      postForm.remark = data.remark || ''
      setParentSelection(postForm.parentId)
      syncStatusIndex(postForm.status)
      uni.setNavigationBarTitle({ title: '编辑岗位' })
    } finally {
      loading.value = false
    }
  }

  function onStatusChange(index: number) {
    statusIndex.value = index
    const status = statusOptions[index]?.value ?? 1
    if (entity.value === 'dept') deptForm.status = status
    else postForm.status = status
  }

  async function save(recordId?: number) {
    if (entity.value === 'dept') {
      if (!deptForm.name?.trim()) {
        uni.showToast({ title: '请输入部门名称', icon: 'none' })
        return false
      }
      saving.value = true
      try {
        const payload = {
          ...deptForm,
          name: deptForm.name.trim(),
          leaderUserId: deptForm.leaderUserId ?? null,
        }
        delete (payload as { leaderName?: string }).leaderName
        if (isCreate.value) await createDept(payload)
        else await updateDept({ ...payload, id: recordId || deptForm.id })
        uni.showToast({ title: isCreate.value ? '创建成功' : '保存成功', icon: 'success' })
        return true
      } finally {
        saving.value = false
      }
    }

    if (!postForm.postName?.trim()) {
      uni.showToast({ title: '请输入岗位名称', icon: 'none' })
      return false
    }
    if (isCreate.value && !postForm.postCode?.trim()) {
      uni.showToast({ title: '请输入岗位编码', icon: 'none' })
      return false
    }
    saving.value = true
    try {
      const payload = {
        ...postForm,
        postName: postForm.postName.trim(),
        postCode: postForm.postCode?.trim(),
      }
      if (isCreate.value) await createPost(payload)
      else await updatePost({ ...payload, id: recordId || postForm.id })
      uni.showToast({ title: isCreate.value ? '创建成功' : '保存成功', icon: 'success' })
      return true
    } finally {
      saving.value = false
    }
  }

  return {
    loading,
    saving,
    isCreate,
    entity,
    deptForm,
    postForm,
    parentLabel,
    excludeParentId,
    statusOptions,
    statusIndex,
    leaderLabel,
    initCreate,
    loadEdit,
    setParentSelection,
    setLeaderSelection,
    onStatusChange,
    save,
    refreshParentLabel,
  }
}
