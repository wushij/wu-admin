import { ref, reactive } from 'vue'
import { createDictData, updateDictData } from '@/api/system/dict'
import type { DictDataItem } from '@/types/api'

export function useDictDataForm() {
  const loading = ref(false)
  const saving = ref(false)
  const isCreate = ref(false)
  const dictType = ref('')

  const form = reactive({
    dictLabel: '',
    dictValue: '',
    sort: 0,
    status: 1,
    remark: '',
  })

  const statusOptions = [
    { label: '启用', value: 1 },
    { label: '停用', value: 0 },
  ]
  const statusIndex = ref(0)

  function initCreate(type: string) {
    isCreate.value = true
    dictType.value = type
    form.dictLabel = ''
    form.dictValue = ''
    form.sort = 0
    form.status = 1
    form.remark = ''
    statusIndex.value = 0
    uni.setNavigationBarTitle({ title: '新增字典项' })
  }

  function loadFromItem(type: string, item: DictDataItem) {
    isCreate.value = false
    dictType.value = type
    form.dictLabel = item.dictLabel
    form.dictValue = item.dictValue
    form.sort = item.sort ?? 0
    form.status = item.status ?? 1
    form.remark = item.remark || ''
    statusIndex.value = form.status === 0 ? 1 : 0
    uni.setNavigationBarTitle({ title: '编辑字典项' })
  }

  function onStatusChange(e: { detail: { value: number } }) {
    statusIndex.value = Number(e.detail.value)
    form.status = statusOptions[statusIndex.value]?.value ?? 1
  }

  async function save(dataId: number) {
    if (!form.dictLabel.trim() || !form.dictValue.trim()) {
      uni.showToast({ title: '请填写标签和键值', icon: 'none' })
      return
    }
    saving.value = true
    try {
      const payload: DictDataItem = {
        dictType: dictType.value,
        dictLabel: form.dictLabel.trim(),
        dictValue: form.dictValue.trim(),
        sort: form.sort,
        status: form.status,
        remark: form.remark.trim() || undefined,
      }
      if (isCreate.value) await createDictData(payload)
      else await updateDictData({ ...payload, id: dataId })
      uni.showToast({ title: '保存成功', icon: 'success' })
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
    loadFromItem,
    onStatusChange,
    save,
  }
}

