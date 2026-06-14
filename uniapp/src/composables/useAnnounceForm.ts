import { ref, reactive } from 'vue'
import { createAnnounce, getAnnounceAdminDetail, updateAnnounce } from '@/api/message'
import { getUserList } from '@/api/system/user'
import { getDeptTree } from '@/api/system/dept'
import { leaveFormPageAfterSave } from '@/utils/navigate-back'
import { buildDeptLabels, buildDeptNodeMaps } from '@/utils/dept-tree'
import type { AnnounceSaveDTO } from '@/types/message'

export function useAnnounceForm() {
  const loading = ref(false)
  const saving = ref(false)
  const isCreate = ref(false)
  const targetIds = ref<number[]>([])
  const deptLabel = ref('请选择部门')
  const userRows = ref<{ id: number; label: string }[]>([])

  const form = reactive<AnnounceSaveDTO>({
    title: '',
    content: '',
    noticeType: 1,
    targetType: 3,
    channels: ['station'],
    status: 0,
  })

  const typeOptions = [
    { label: '通知', value: 1 },
    { label: '公告', value: 2 },
  ]
  const targetOptions = [
    { label: '全体人员', value: 3 },
    { label: '指定用户', value: 1 },
    { label: '指定部门', value: 2 },
  ]
  const typeIndex = ref(0)
  const targetIndex = ref(0)

  async function loadUserOptions() {
    const userRes = await getUserList()
    userRows.value = (userRes.data || []).map((u) => ({
      id: u.id,
      label: u.nickname || u.username,
    }))
  }

  async function resolveDeptLabel(ids: number[]) {
    if (!ids.length) return '请选择部门'
    const res = await getDeptTree({ status: 1 })
    const maps = buildDeptNodeMaps(res.data || [])
    const labels = buildDeptLabels(ids, maps.idToNode)
    return labels.length ? labels.join('、') : '请选择部门'
  }

  async function resolveUserLabel(ids: number[]) {
    if (!ids.length) return ''
    await loadUserOptions()
    const labels = ids
      .map((id) => userRows.value.find((u) => u.id === id)?.label)
      .filter(Boolean)
    return labels.join('、')
  }

  function initCreate() {
    isCreate.value = true
    form.title = ''
    form.content = ''
    form.noticeType = 1
    form.targetType = 3
    form.channels = ['station']
    form.status = 0
    targetIds.value = []
    deptLabel.value = '请选择部门'
    typeIndex.value = 0
    targetIndex.value = 0
    uni.setNavigationBarTitle({ title: '新增通知' })
  }

  async function load(id: number) {
    isCreate.value = false
    loading.value = true
    try {
      const res = await getAnnounceAdminDetail(id)
      const row = res.data
      if (!row) return
      form.title = row.title
      form.content = row.content
      form.noticeType = row.noticeType ?? 1
      form.targetType = row.targetType ?? 3
      form.channels = row.channels || ['station']
      form.status = row.status ?? 0
      targetIds.value = [...(row.targetIds || [])]
      typeIndex.value = form.noticeType === 2 ? 1 : 0
      targetIndex.value = targetOptions.findIndex((t) => t.value === form.targetType)
      if (targetIndex.value < 0) targetIndex.value = 0
      if (form.targetType === 2) {
        deptLabel.value = await resolveDeptLabel(targetIds.value)
      }
      await loadUserOptions()
      uni.setNavigationBarTitle({ title: '编辑通知' })
    } finally {
      loading.value = false
    }
  }

  function onTypeChange(e: { detail: { value: number } }) {
    typeIndex.value = Number(e.detail.value)
    form.noticeType = typeOptions[typeIndex.value]?.value ?? 1
  }

  function onTargetChange(e: { detail: { value: number } }) {
    targetIndex.value = Number(e.detail.value)
    form.targetType = targetOptions[targetIndex.value]?.value ?? 3
    targetIds.value = []
    deptLabel.value = '请选择部门'
  }

  function isTargetSelected(id: number) {
    return targetIds.value.includes(id)
  }

  function toggleTarget(id: number, checked: boolean) {
    const next = new Set(targetIds.value)
    if (checked) next.add(id)
    else next.delete(id)
    targetIds.value = [...next]
  }

  function setDeptSelection(ids: number[], label: string) {
    targetIds.value = [...ids]
    deptLabel.value = label || '请选择部门'
  }

  async function save(id: number, publish = false) {
    if (!form.title.trim() || !form.content.trim()) {
      uni.showToast({ title: '请填写标题和内容', icon: 'none' })
      return
    }
    if (form.targetType === 1 && !targetIds.value.length) {
      uni.showToast({ title: '请选择目标用户', icon: 'none' })
      return
    }
    if (form.targetType === 2 && !targetIds.value.length) {
      uni.showToast({ title: '请选择目标部门', icon: 'none' })
      return
    }
    saving.value = true
    try {
      const payload: AnnounceSaveDTO = {
        title: form.title.trim(),
        content: form.content.trim(),
        noticeType: form.noticeType,
        targetType: form.targetType,
        channels: form.channels,
        status: publish ? 1 : 0,
      }
      if (form.targetType === 1 || form.targetType === 2) {
        payload.targetIds = [...targetIds.value]
      }
      if (isCreate.value) {
        await createAnnounce(payload)
      } else {
        await updateAnnounce({ ...payload, id })
        if (publish) {
          const { publishAnnounce } = await import('@/api/message')
          await publishAnnounce(id)
        }
      }
      uni.showToast({ title: publish ? '已发布' : '保存成功', icon: 'success' })
      leaveFormPageAfterSave('/pages-sub/system/announce/index')
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
    targetOptions,
    typeIndex,
    targetIndex,
    targetIds,
    deptLabel,
    userRows,
    initCreate,
    load,
    loadUserOptions,
    resolveDeptLabel,
    resolveUserLabel,
    onTypeChange,
    onTargetChange,
    isTargetSelected,
    toggleTarget,
    setDeptSelection,
    save,
  }
}
