import { ref, reactive } from 'vue'
import { createUser, getUser, updateUser, getUserRoleIds } from '@/api/system/user'
import { getDeptTree, getPostTree } from '@/api/system/dept'
import { getRoleList } from '@/api/system/role'
import { findDeptPathLabel } from '@/utils/dept-tree'
import { flattenPostTree } from '@/utils/org-tree'
import type { DeptVO, PostVO, RoleVO } from '@/types/system'
import type { UserVO } from '@/types/user'
import { logger } from '@/utils/logger'
import { getShallowBackFallback, safeNavigateBack } from '@/utils/navigate-back'

function buildPostLabel(ids: number[], postMap: Map<number, PostVO>) {
  if (!ids.length) return '未分配'
  const names = ids
    .map((id) => postMap.get(id))
    .filter(Boolean)
    .map((p) => p!.postName || p!.name || p!.postCode || '')
    .filter(Boolean)
  return names.length ? names.join('、') : '未分配'
}

/** 新增用户默认角色：普通用户（code=user） */
function resolveDefaultCreateRoleId(roles: RoleVO[]): number | null {
  const regular = roles.find((r) => r.code === 'user')
  if (regular) return regular.id
  const byName = roles.find((r) => r.name === '普通用户')
  if (byName) return byName.id
  const nonAdmin = roles.find((r) => r.code !== 'super_admin' && r.code !== 'admin')
  return nonAdmin?.id ?? roles[0]?.id ?? null
}

export function useUserEditForm(options?: { backFallback?: string | (() => string) }) {
  const loading = ref(false)
  const saving = ref(false)
  const isCreate = ref(false)
  const user = ref<UserVO | null>(null)
  const deptTree = ref<DeptVO[]>([])
  const postMap = ref(new Map<number, PostVO>())
  const deptLabel = ref('未分配')
  const postLabel = ref('未分配')
  const roleOptions = ref<RoleVO[]>([])
  const roleIndex = ref(0)
  const statusIndex = ref(0)

  const form = reactive({
    username: '',
    password: '',
    nickname: '',
    mobile: '',
    email: '',
    remark: '',
    deptId: null as number | null,
    postIds: [] as number[],
    roleId: null as number | null,
    status: 1,
  })

  const statusOptions = [
    { label: '启用', value: 1 },
    { label: '停用', value: 0 },
  ]

  async function loadOptions() {
    const [deptRes, roleRes, postRes] = await Promise.all([
      getDeptTree({ status: 1 }),
      getRoleList({ status: 1 }),
      getPostTree(),
    ])
    deptTree.value = deptRes.data || []
    roleOptions.value = roleRes.data || []
    const flat = flattenPostTree(postRes.data || [])
    const map = new Map<number, PostVO>()
    flat.forEach((node) => {
      map.set(node.id, {
        id: node.id,
        postName: node.label,
        postCode: node.extra,
        status: node.status,
      })
    })
    postMap.value = map
  }

  function syncPostLabel() {
    postLabel.value = buildPostLabel(form.postIds, postMap.value)
  }

  function syncDeptLabel() {
    deptLabel.value = findDeptPathLabel(deptTree.value, form.deptId)
  }

  function setDeptSelection(id: number | null, label: string) {
    form.deptId = id
    deptLabel.value = label
  }

  function setPostSelection(ids: number[], label: string) {
    form.postIds = [...ids]
    postLabel.value = label || buildPostLabel(ids, postMap.value)
  }

  async function initCreate() {
    isCreate.value = true
    loading.value = true
    try {
      await loadOptions()
      user.value = { id: 0, username: '', nickname: '', status: 1 }
      form.username = ''
      form.password = ''
      form.nickname = ''
      form.mobile = ''
      form.email = ''
      form.remark = ''
      form.deptId = null
      form.postIds = []
      form.roleId = resolveDefaultCreateRoleId(roleOptions.value)
      form.status = 1
      deptLabel.value = '未分配'
      postLabel.value = '未分配'
      roleIndex.value = Math.max(0, roleOptions.value.findIndex((r) => r.id === form.roleId))
      statusIndex.value = 0
      uni.setNavigationBarTitle({ title: '新增用户' })
    } finally {
      loading.value = false
    }
  }

  async function loadUser(id: number) {
    isCreate.value = false
    loading.value = true
    try {
      await loadOptions()
      const [userRes, roleIdsRes] = await Promise.all([getUser(id), getUserRoleIds(id)])
      user.value = userRes.data || null
      if (!user.value) return

      form.nickname = user.value.nickname || ''
      form.mobile = user.value.mobile || ''
      form.email = user.value.email || ''
      form.remark = user.value.remark || ''
      form.deptId = user.value.deptId ?? null
      form.postIds = user.value.postIds ? [...user.value.postIds] : []
      form.status = user.value.status ?? 1

      const roleIds = roleIdsRes.data || user.value.roleIds || []
      form.roleId = roleIds[0] ?? null
      syncDeptLabel()
      syncPostLabel()
      if (!form.postIds.length && user.value.postNames) {
        postLabel.value = user.value.postNames
      }

      roleIndex.value = Math.max(0, roleOptions.value.findIndex((r) => r.id === form.roleId))
      statusIndex.value = form.status === 0 ? 1 : 0
    } finally {
      loading.value = false
    }
  }

  function onRoleChange(e: { detail: { value: number } }) {
    roleIndex.value = Number(e.detail.value)
    form.roleId = roleOptions.value[roleIndex.value]?.id ?? null
  }

  function onStatusChange(e: { detail: { value: number } }) {
    statusIndex.value = Number(e.detail.value)
    form.status = statusOptions[statusIndex.value]?.value ?? 1
  }

  async function save(id: number, onSaved?: () => void) {
    if (isCreate.value) {
      if (!form.username.trim()) {
        uni.showToast({ title: '请输入用户名', icon: 'none' })
        return
      }
      if (!form.password.trim()) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return
      }
    }
    if (!form.nickname.trim()) {
      uni.showToast({ title: '请输入昵称', icon: 'none' })
      return
    }

    const mobile = String(form.mobile ?? '').trim()
    if (mobile && !/^1[3-9]\d{9}$/.test(mobile)) {
      uni.showToast({ title: '手机号格式不正确', icon: 'none' })
      return
    }

    saving.value = true
    try {
      const payload = {
        nickname: form.nickname.trim(),
        mobile,
        email: form.email.trim() || undefined,
        deptId: form.deptId,
        postIds: form.postIds.length ? [...form.postIds] : undefined,
        roleId: form.roleId ?? undefined,
        status: form.status,
        remark: form.remark.trim() || undefined,
      }

      if (isCreate.value) {
        await createUser({
          ...payload,
          username: form.username.trim(),
          password: form.password.trim(),
        })
      } else {
        await updateUser({ id, ...payload, username: user.value!.username })
      }

      uni.showToast({ title: isCreate.value ? '创建成功' : '保存成功', icon: 'success' })
      onSaved?.()
      const fallback =
        typeof options?.backFallback === 'function'
          ? options.backFallback()
          : options?.backFallback || getShallowBackFallback()
      setTimeout(() => safeNavigateBack(fallback), 400)
    } catch (e) {
      logger.error(e)
    } finally {
      saving.value = false
    }
  }

  return {
    loading,
    saving,
    isCreate,
    user,
    form,
    deptLabel,
    postLabel,
    roleOptions,
    statusOptions,
    roleIndex,
    statusIndex,
    initCreate,
    loadUser,
    setDeptSelection,
    setPostSelection,
    onRoleChange,
    onStatusChange,
    save,
  }
}
