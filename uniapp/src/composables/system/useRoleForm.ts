import { ref, reactive, computed } from 'vue'
import { createRole, getRole, updateRole } from '@/api/system/role'
import { getDeptTree } from '@/api/system/dept'
import { buildDeptLabels, buildDeptNodeMaps } from '@/utils/tree/dept-tree'
import { leaveFormPageAfterSave } from '@/utils/nav/navigate-back'
import type { DeptVO } from '@/types/system'

/** 与 PC 端角色编辑保持一致的数据范围选项 */
export const dataScopeOptions = [
  { value: 1, label: '全部数据' },
  { value: 2, label: '自定义部门' },
  { value: 3, label: '本部门' },
  { value: 4, label: '本部门及以下' },
  { value: 5, label: '仅本人' },
]

function parseDeptIds(raw?: string): number[] {
  if (!raw) return []
  return [
    ...new Set(
      raw
        .split(',')
        .map((id) => Number(id.trim()))
        .filter((id) => Number.isFinite(id) && id > 0),
    ),
  ]
}

export function useRoleForm() {
  const loading = ref(false)
  const saving = ref(false)
  const isCreate = ref(false)

  const form = reactive({
    name: '',
    code: '',
    sort: 0,
    status: 1,
    remark: '',
    dataScope: 1,
    /** 自定义数据范围的部门 ID（仅 dataScope=2 时生效） */
    dataScopeDeptIds: [] as number[],
  })

  const statusOptions = [
    { label: '启用', value: 1 },
    { label: '停用', value: 0 },
  ]
  const statusIndex = ref(0)

  const deptTree = ref<DeptVO[]>([])
  const selectedDeptLabel = ref('未选择')

  const dataScopeLabel = computed(
    () => dataScopeOptions.find((item) => item.value === form.dataScope)?.label || '全部数据',
  )

  function syncDeptLabel() {
    if (!form.dataScopeDeptIds.length) {
      selectedDeptLabel.value = '未选择'
      return
    }
    const { idToNode } = buildDeptNodeMaps(deptTree.value)
    if (!idToNode.size) {
      selectedDeptLabel.value = `已选 ${form.dataScopeDeptIds.length} 个部门`
      return
    }
    selectedDeptLabel.value =
      buildDeptLabels(form.dataScopeDeptIds, idToNode).join('、') || '未选择'
  }

  async function loadDeptOptions() {
    if (deptTree.value.length) return
    try {
      const res = await getDeptTree({ status: 1 })
      deptTree.value = res.data || []
    } catch {
      // 部门树加载失败不阻断角色编辑，选择部门时仍由选择页自行加载
    }
  }

  async function initCreate() {
    isCreate.value = true
    form.name = ''
    form.code = ''
    form.sort = 0
    form.status = 1
    form.remark = ''
    form.dataScope = 1
    form.dataScopeDeptIds = []
    selectedDeptLabel.value = '未选择'
    statusIndex.value = 0
    uni.setNavigationBarTitle({ title: '新增角色' })
  }

  async function loadRole(id: number) {
    isCreate.value = false
    loading.value = true
    try {
      const res = await getRole(id)
      const role = res.data
      if (!role) return
      form.name = role.name
      form.code = role.code
      form.sort = role.sort ?? 0
      form.status = role.status ?? 1
      form.remark = role.remark || ''
      form.dataScope = role.dataScope ?? 1
      form.dataScopeDeptIds = parseDeptIds(role.dataScopeDeptIds)
      statusIndex.value = form.status === 0 ? 1 : 0

      await loadDeptOptions()
      syncDeptLabel()
    } finally {
      loading.value = false
    }
  }

  function onStatusChange(e: { detail: { value: number } }) {
    statusIndex.value = Number(e.detail.value)
    form.status = statusOptions[statusIndex.value]?.value ?? 1
  }

  function onDataScopeChange(e: { detail: { value: number } }) {
    const next = dataScopeOptions[Number(e.detail.value)]?.value ?? 1
    form.dataScope = next
    if (next !== 2) {
      form.dataScopeDeptIds = []
      selectedDeptLabel.value = '未选择'
    }
  }

  /** 接收部门多选页回传的结果 */
  function setDeptSelection(ids: number[], labelText?: string) {
    form.dataScopeDeptIds = [...ids]
    if (labelText && labelText !== '未分配') {
      selectedDeptLabel.value = labelText
      return
    }
    syncDeptLabel()
  }

  async function save(id: number) {
    if (!form.name.trim() || !form.code.trim()) {
      uni.showToast({ title: '请填写名称和编码', icon: 'none' })
      return
    }
    if (form.dataScope === 2 && form.dataScopeDeptIds.length === 0) {
      uni.showToast({ title: '请选择自定义数据范围的部门', icon: 'none' })
      return
    }
    saving.value = true
    try {
      const payload = {
        name: form.name.trim(),
        code: form.code.trim(),
        sort: form.sort,
        status: form.status,
        remark: form.remark.trim() || undefined,
        dataScope: form.dataScope,
        dataScopeDeptIds: form.dataScope === 2 ? form.dataScopeDeptIds.join(',') : '',
      }
      if (isCreate.value) {
        await createRole(payload)
      } else {
        await updateRole({ id, ...payload })
      }
      uni.showToast({ title: isCreate.value ? '创建成功' : '保存成功', icon: 'success' })
      leaveFormPageAfterSave('/pages-sub/system/role/index')
    } finally {
      saving.value = false
    }
  }

  return {
    loading,
    saving,
    isCreate,
    form,
    statusOptions,
    statusIndex,
    dataScopeOptions,
    dataScopeLabel,
    selectedDeptLabel,
    initCreate,
    loadRole,
    onStatusChange,
    onDataScopeChange,
    setDeptSelection,
    save,
  }
}
