import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ElTree } from 'element-plus'
import { getUserPage, type UserVO, type UserPageQuery } from '@/api/system/user'
import type { DeptVO, DeptSaveDTO } from '@/api/system/dept'
import type { PostVO, PostSaveDTO } from '@/api/system/post'
import {
  getDeptTree,
  getDept,
  createDept,
  updateDept,
  deleteDept,
  moveDept,
} from '@/api/system/dept'
import {
  getPostTree,
  getPost,
  createPost,
  updatePost,
  deletePost,
  movePost,
} from '@/api/system/post'
import { unwrapOrgTreeNode } from '@/types/org'
import { displayOrgTree, resolveDeptRootParentId, collectExpandKeysByDepth } from '@/utils/org-tree'

export function useOrgPage() {
  const router = useRouter()
  const activeTab = ref('dept')
  const treeSearch = ref('')
  const deptTreeRef = ref<InstanceType<typeof ElTree> | null>(null)
  const postTreeRef = ref<InstanceType<typeof ElTree> | null>(null)
  const deptTreeRaw = ref<DeptVO[]>([])
  const deptDisplayTree = computed(() => displayOrgTree(deptTreeRaw.value))
  const postTree = ref<PostVO[]>([])
  const postDefaultExpandedKeys = computed(() => collectExpandKeysByDepth(postTree.value, 2))

  const selectedId = ref<number | null>(null)
  const selectedName = ref('')

  const userLoading = ref(false)
  const userList = ref<UserVO[]>([])
  const userTotal = ref(0)
  const userQuery = reactive<UserPageQuery>({ pageNo: 1, pageSize: 10 })

  const memberTitle = computed(() => {
    if (!selectedId.value) return '所有用户'
    const label = activeTab.value === 'dept' ? '部门成员' : '岗位成员'
    return `【${selectedName.value}】${label}`
  })

  watch(activeTab, () => {
    selectedId.value = null
    selectedName.value = ''
    treeSearch.value = ''
    userQuery.pageNo = 1
    loadTree()
    loadUsers()
  })

  watch(treeSearch, (val) => {
    if (activeTab.value === 'dept') {
      deptTreeRef.value?.filter(val)
    } else {
      postTreeRef.value?.filter(val)
    }
  })

  function filterTreeNode(value: string, data: unknown) {
    const node = data as DeptVO
    if (!value) return true
    return String(node.name ?? '').includes(value)
  }

  function filterPostTreeNode(value: string, data: unknown) {
    const node = data as PostVO
    if (!value) return true
    const label = String(node.postName ?? node.name ?? '')
    return label.includes(value)
  }

  async function loadTree() {
    if (activeTab.value === 'dept') {
      const res = await getDeptTree()
      deptTreeRaw.value = res.data || []
    } else {
      const res = await getPostTree()
      postTree.value = res.data || []
    }
  }

  async function loadUsers() {
    userLoading.value = true
    try {
      const params: UserPageQuery = {
        pageNo: userQuery.pageNo,
        pageSize: userQuery.pageSize,
      }
      if (selectedId.value) {
        if (activeTab.value === 'dept') params.deptId = selectedId.value
        else params.postId = selectedId.value
      }
      const res = await getUserPage(params)
      userList.value = res.data?.list || []
      userTotal.value = Number(res.data?.total) || 0
    } finally {
      userLoading.value = false
    }
  }

  function goUserManage() {
    const query: Record<string, string> = {}
    if (selectedId.value && activeTab.value === 'dept') {
      query.deptId = String(selectedId.value)
    }
    router.push({ path: '/system/user', query })
  }

  function onDeptNodeClick(data: DeptVO) {
    if (selectedId.value === data.id) {
      selectedId.value = null
      selectedName.value = ''
    } else {
      selectedId.value = data.id
      selectedName.value = data.name
    }
    userQuery.pageNo = 1
    loadUsers()
  }

  function onPostNodeClick(data: PostVO) {
    if (selectedId.value === data.id) {
      selectedId.value = null
      selectedName.value = ''
    } else {
      selectedId.value = data.id
      selectedName.value = String(data.postName ?? data.name ?? '')
    }
    userQuery.pageNo = 1
    loadUsers()
  }

  function allowDeptDrop(dragging: unknown, drop: unknown, type: string) {
    const dragData = unwrapOrgTreeNode<DeptVO>(dragging)
    const dropData = unwrapOrgTreeNode<DeptVO>(drop)
    if (type === 'inner' && dragData.id === dropData.id) return false
    return true
  }

  function allowPostDrop(dragging: unknown, drop: unknown, type: string) {
    const dragData = unwrapOrgTreeNode<PostVO>(dragging)
    const dropData = unwrapOrgTreeNode<PostVO>(drop)
    if (type === 'inner' && dragData.id === dropData.id) return false
    return true
  }

  async function onDeptDrop(
    dragging: unknown,
    drop: unknown,
    dropType: 'before' | 'after' | 'inner',
    _evt?: DragEvent
  ) {
    const dragData = unwrapOrgTreeNode<DeptVO>(dragging)
    const dropData = unwrapOrgTreeNode<DeptVO>(drop)
    const id = dragData.id
    const parentId = dropType === 'inner' ? dropData.id : (dropData.parentId || 0)
    await moveDept(id, parentId, 0)
    ElMessage.success('移动成功')
    loadTree()
  }

  async function onPostDrop(
    dragging: unknown,
    drop: unknown,
    dropType: 'before' | 'after' | 'inner',
    _evt?: DragEvent
  ) {
    const dragData = unwrapOrgTreeNode<PostVO>(dragging)
    const dropData = unwrapOrgTreeNode<PostVO>(drop)
    const id = dragData.id
    const parentId = dropType === 'inner' ? dropData.id : (dropData.parentId || 0)
    await movePost(id, parentId)
    ElMessage.success('移动成功')
    loadTree()
  }

  const deptDialogVisible = ref(false)
  const deptDialogTitle = ref('')
  const deptSubmitting = ref(false)
  const deptFormRef = ref()
  const deptForm = reactive<DeptSaveDTO>({
    id: undefined,
    parentId: 0,
    name: '',
    leaderName: '',
    phone: '',
    email: '',
    sort: 0,
    status: 1,
  })
  const deptRules = { name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }] }
  const deptTreeOptions = computed(() => [{ id: 0, name: '主目录', children: deptTreeRaw.value }])

  const postDialogVisible = ref(false)
  const postDialogTitle = ref('')
  const postSubmitting = ref(false)
  const postFormRef = ref()
  const postForm = reactive<PostSaveDTO>({
    id: undefined,
    parentId: 0,
    postCode: '',
    postName: '',
    sort: 0,
    status: 1,
    remark: '',
  })
  const postRules = {
    postCode: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
    postName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  }
  const postTreeOptions = computed(() => [
    { id: 0, postName: '顶级', name: '顶级', children: postTree.value },
  ])

  function handleAddRoot() {
    if (activeTab.value === 'dept') {
      openDeptForm(null, resolveDeptRootParentId(deptTreeRaw.value))
    } else {
      openPostForm(null, 0)
    }
  }

  function handleAddChild() {
    if (activeTab.value === 'dept') openDeptForm(null, selectedId.value)
    else openPostForm(null, selectedId.value)
  }

  async function handleEditNode() {
    if (selectedId.value == null) return
    const nodeId = selectedId.value
    if (activeTab.value === 'dept') {
      const res = await getDept(nodeId)
      openDeptForm(res.data, res.data?.parentId ?? 0)
    } else {
      const res = await getPost(nodeId)
      openPostForm(res.data, res.data?.parentId ?? 0)
    }
  }

  function openDeptForm(row: DeptVO | null, parentId?: number | null) {
    deptDialogTitle.value = row?.id ? '编辑部门' : '新增部门'
    Object.assign(deptForm, {
      id: row?.id,
      parentId: row?.parentId ?? parentId ?? 0,
      name: row?.name || '',
      leaderName: row?.leaderName || '',
      phone: row?.phone || '',
      email: row?.email || '',
      sort: row?.sort ?? 0,
      status: row?.status ?? 1,
    })
    if (deptForm.parentId == null) deptForm.parentId = 0
    deptDialogVisible.value = true
  }

  function openPostForm(row: PostVO | null, parentId?: number | null) {
    postDialogTitle.value = row?.id ? '编辑岗位' : '新增岗位'
    Object.assign(postForm, {
      id: row?.id,
      parentId: row?.parentId ?? parentId ?? 0,
      postCode: row?.postCode || '',
      postName: row?.postName || '',
      sort: row?.sort ?? 0,
      status: row?.status ?? 1,
      remark: row?.remark || '',
    })
    if (postForm.parentId == null) postForm.parentId = 0
    postDialogVisible.value = true
  }

  async function submitDept() {
    await deptFormRef.value?.validate()
    deptSubmitting.value = true
    try {
      const payload = { ...deptForm, parentId: deptForm.parentId || 0 }
      if (deptForm.id) {
        await updateDept(payload)
        ElMessage.success('更新成功')
      } else {
        await createDept(payload)
        ElMessage.success('创建成功')
      }
      deptDialogVisible.value = false
      loadTree()
    } finally {
      deptSubmitting.value = false
    }
  }

  async function submitPost() {
    await postFormRef.value?.validate()
    postSubmitting.value = true
    try {
      const payload = { ...postForm, parentId: postForm.parentId || 0 }
      if (postForm.id) {
        await updatePost(payload)
        ElMessage.success('更新成功')
      } else {
        await createPost(payload)
        ElMessage.success('创建成功')
      }
      postDialogVisible.value = false
      loadTree()
    } finally {
      postSubmitting.value = false
    }
  }

  async function handleDeleteNode() {
    if (selectedId.value == null) return
    const nodeId = selectedId.value
    const name = selectedName.value
    await ElMessageBox.confirm(`确定删除「${name}」？`, '提示', { type: 'warning' })
    if (activeTab.value === 'dept') {
      await deleteDept(nodeId)
    } else {
      await deletePost(nodeId)
    }
    ElMessage.success('删除成功')
    selectedId.value = null
    selectedName.value = ''
    loadTree()
    loadUsers()
  }

  onMounted(() => {
    loadTree()
    loadUsers()
  })

  return {
    activeTab,
    treeSearch,
    deptTreeRef,
    postTreeRef,
    deptDisplayTree,
    postTree,
    postDefaultExpandedKeys,
    selectedId,
    memberTitle,
    userLoading,
    userList,
    userTotal,
    userQuery,
    filterTreeNode,
    filterPostTreeNode,
    goUserManage,
    onDeptNodeClick,
    onPostNodeClick,
    allowDeptDrop,
    allowPostDrop,
    onDeptDrop,
    onPostDrop,
    handleAddRoot,
    handleAddChild,
    handleEditNode,
    handleDeleteNode,
    deptDialogVisible,
    deptDialogTitle,
    deptSubmitting,
    deptFormRef,
    deptForm,
    deptRules,
    deptTreeOptions,
    submitDept,
    postDialogVisible,
    postDialogTitle,
    postSubmitting,
    postFormRef,
    postForm,
    postRules,
    postTreeOptions,
    submitPost,
    loadUsers,
  }
}
