import { ref, reactive } from 'vue'
import { createMenu, getMenu, getMenuList, updateMenu } from '@/api/system/menu'
import { leaveFormPageAfterSave } from '@/utils/navigate-back'
import type { MenuSaveDTO, MenuVO } from '@/types/system'

function flattenParentOptions(nodes: MenuVO[], prefix = ''): { id: number; label: string }[] {
  const rows: { id: number; label: string }[] = []
  for (const node of nodes) {
    if (node.type === 3) continue
    const label = prefix ? `${prefix} / ${node.name}` : node.name
    rows.push({ id: node.id, label })
    if (node.children?.length) {
      rows.push(...flattenParentOptions(node.children, label))
    }
  }
  return rows
}

export function useMenuForm() {
  const loading = ref(false)
  const saving = ref(false)
  const isCreate = ref(false)
  const parentOptions = ref<{ id: number; label: string }[]>([{ id: 0, label: '顶级菜单' }])
  const parentIndex = ref(0)

  const form = reactive<MenuSaveDTO>({
    parentId: 0,
    name: '',
    type: 1,
    path: '',
    component: '',
    permission: '',
    sort: 0,
    icon: '',
    status: 1,
    isFrame: 0,
  })

  const typeOptions = [
    { label: '目录', value: 1 },
    { label: '菜单', value: 2 },
    { label: '按钮', value: 3 },
  ]
  const statusOptions = [
    { label: '启用', value: 1 },
    { label: '停用', value: 0 },
  ]
  const typeIndex = ref(0)
  const statusIndex = ref(0)

  async function loadParentOptions() {
    const res = await getMenuList()
    parentOptions.value = [{ id: 0, label: '顶级菜单' }, ...flattenParentOptions(res.data || [])]
  }

  function syncParentIndex() {
    const idx = parentOptions.value.findIndex((p) => p.id === (form.parentId ?? 0))
    parentIndex.value = idx >= 0 ? idx : 0
  }

  async function initCreate(parentId?: number) {
    isCreate.value = true
    form.parentId = parentId ?? 0
    form.name = ''
    form.type = 1
    form.path = ''
    form.component = ''
    form.permission = ''
    form.sort = 0
    form.icon = ''
    form.status = 1
    form.isFrame = 0
    typeIndex.value = 0
    statusIndex.value = 0
    uni.setNavigationBarTitle({ title: '新增菜单' })
    await loadParentOptions()
    syncParentIndex()
  }

  async function loadMenu(id: number) {
    isCreate.value = false
    loading.value = true
    try {
      const [menuRes] = await Promise.all([getMenu(id), loadParentOptions()])
      const row = menuRes.data
      if (!row) return
      form.parentId = row.parentId ?? 0
      form.name = row.name
      form.type = row.type ?? 1
      form.path = row.path || ''
      form.component = row.component || ''
      form.permission = row.permission || ''
      form.sort = row.sort ?? 0
      form.icon = row.icon || ''
      form.status = row.status ?? 1
      form.isFrame = row.isFrame ?? 0
      typeIndex.value = typeOptions.findIndex((t) => t.value === form.type)
      if (typeIndex.value < 0) typeIndex.value = 0
      statusIndex.value = form.status === 0 ? 1 : 0
      syncParentIndex()
    } finally {
      loading.value = false
    }
  }

  function onTypeChange(e: { detail: { value: number } }) {
    typeIndex.value = Number(e.detail.value)
    form.type = typeOptions[typeIndex.value]?.value ?? 1
    if (form.type === 3) {
      form.icon = ''
      form.path = ''
      form.component = ''
      form.isFrame = 0
    }
  }

  function onStatusChange(e: { detail: { value: number } }) {
    statusIndex.value = Number(e.detail.value)
    form.status = statusOptions[statusIndex.value]?.value ?? 1
  }

  function onParentChange(e: { detail: { value: number } }) {
    parentIndex.value = Number(e.detail.value)
    form.parentId = parentOptions.value[parentIndex.value]?.id ?? 0
  }

  function buildPayload(id?: number): MenuSaveDTO {
    const payload: MenuSaveDTO = {
      id: id ?? undefined,
      parentId: form.parentId === 0 ? 0 : form.parentId,
      name: form.name.trim(),
      type: form.type,
      sort: form.sort,
      status: form.status,
      permission: form.permission?.trim() || '',
      isFrame: form.isFrame,
    }
    if (form.type === 3) {
      payload.path = ''
      payload.component = ''
      payload.icon = ''
    } else {
      payload.icon = form.icon?.trim() || ''
      payload.path = form.path?.trim() || ''
      payload.component = form.component?.trim() || ''
      if (form.type === 1) payload.component = ''
      if (form.isFrame) payload.path = payload.path || payload.component
    }
    return payload
  }

  async function save(id: number) {
    if (!form.name.trim()) {
      uni.showToast({ title: '请填写菜单名称', icon: 'none' })
      return
    }
    saving.value = true
    try {
      const payload = buildPayload(isCreate.value ? undefined : id)
      if (isCreate.value) {
        await createMenu(payload)
      } else {
        await updateMenu({ ...payload, id })
      }
      uni.showToast({ title: isCreate.value ? '创建成功' : '保存成功', icon: 'success' })
      leaveFormPageAfterSave('/pages-sub/system/menu/index')
    } finally {
      saving.value = false
    }
  }

  return {
    loading,
    saving,
    isCreate,
    form,
    typeOptions,
    statusOptions,
    typeIndex,
    statusIndex,
    parentOptions,
    parentIndex,
    initCreate,
    loadMenu,
    onTypeChange,
    onStatusChange,
    onParentChange,
    save,
  }
}

