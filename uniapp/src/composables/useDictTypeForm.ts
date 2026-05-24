import { ref, reactive } from 'vue'
import { createDictType, getDictType, updateDictType } from '@/api/system/dict'
import { reloadDictTypes } from '@/composables/useDict'
import { leaveFormPageAfterSave } from '@/utils/navigate-back'

export function useDictTypeForm() {
  const loading = ref(false)
  const saving = ref(false)
  const isCreate = ref(false)

  const form = reactive({
    dictName: '',
    dictType: '',
    status: 1,
    remark: '',
  })

  const statusOptions = [
    { label: '启用', value: 1 },
    { label: '停用', value: 0 },
  ]
  const statusIndex = ref(0)

  function initCreate() {
    isCreate.value = true
    form.dictName = ''
    form.dictType = ''
    form.status = 1
    form.remark = ''
    statusIndex.value = 0
    uni.setNavigationBarTitle({ title: '新增字典类型' })
  }

  async function load(id: number) {
    isCreate.value = false
    loading.value = true
    try {
      const res = await getDictType(id)
      const row = res.data
      if (!row) return
      form.dictName = row.dictName
      form.dictType = row.dictType
      form.status = row.status ?? 1
      form.remark = row.remark || ''
      statusIndex.value = form.status === 0 ? 1 : 0
    } finally {
      loading.value = false
    }
  }

  function onStatusChange(e: { detail: { value: number } }) {
    statusIndex.value = Number(e.detail.value)
    form.status = statusOptions[statusIndex.value]?.value ?? 1
  }

  const DICT_TYPE_PATTERN = /^[a-z][a-z0-9_]*$/

  async function save(id: number) {
    if (!form.dictName.trim() || !form.dictType.trim()) {
      uni.showToast({ title: '请填写名称和类型', icon: 'none' })
      return
    }
    if (isCreate.value && !DICT_TYPE_PATTERN.test(form.dictType.trim())) {
      uni.showToast({ title: '类型仅小写字母、数字、下划线，且以字母开头', icon: 'none' })
      return
    }
    saving.value = true
    try {
      const payload = {
        dictName: form.dictName.trim(),
        dictType: form.dictType.trim(),
        status: form.status,
        remark: form.remark.trim() || undefined,
      }
      if (isCreate.value) await createDictType(payload)
      else await updateDictType({ id, ...payload })
      await reloadDictTypes([payload.dictType])
      uni.showToast({ title: '保存成功', icon: 'success' })
      leaveFormPageAfterSave('/pages-sub/system/dict/index')
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
    load,
    onStatusChange,
    save,
  }
}

