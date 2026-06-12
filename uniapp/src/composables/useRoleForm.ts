import { ref, reactive } from 'vue'
import { createRole, getRole, updateRole } from '@/api/system/role'

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
  })

  const statusOptions = [
    { label: '启用', value: 1 },
    { label: '停用', value: 0 },
  ]
  const statusIndex = ref(0)

  async function initCreate() {
    isCreate.value = true
    form.name = ''
    form.code = ''
    form.sort = 0
    form.status = 1
    form.remark = ''
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
      statusIndex.value = form.status === 0 ? 1 : 0
    } finally {
      loading.value = false
    }
  }

  function onStatusChange(e: { detail: { value: number } }) {
    statusIndex.value = Number(e.detail.value)
    form.status = statusOptions[statusIndex.value]?.value ?? 1
  }

  async function save(id: number) {
    if (!form.name.trim() || !form.code.trim()) {
      uni.showToast({ title: '请填写名称和编码', icon: 'none' })
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
      }
      if (isCreate.value) {
        await createRole(payload)
      } else {
        await updateRole({ id, ...payload })
      }
      uni.showToast({ title: isCreate.value ? '创建成功' : '保存成功', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 400)
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
    initCreate,
    loadRole,
    onStatusChange,
    save,
  }
}

