import { ref, reactive } from 'vue'
import {
  createChatGroup,
  updateChatGroup,
  getChatUsers,
  getGroupDetail,
  addGroupMembers,
} from '@/api/message'
import type { ChatUser } from '@/types/message'
import { logger } from '@/utils/logger'

export function useGroupForm() {
  const loading = ref(false)
  const saving = ref(false)
  const mode = ref<'create' | 'edit'>('create')
  const groupId = ref(0)
  const users = ref<ChatUser[]>([])
  const selectedIds = ref<number[]>([])

  const form = reactive({
    name: '',
    announcement: '',
  })

  async function loadUsers() {
    const res = await getChatUsers()
    users.value = res.data || []
  }

  async function loadGroup(id: number) {
    loading.value = true
    try {
      await loadUsers()
      const res = await getGroupDetail(id)
      const group = res.data
      if (!group) return
      form.name = group.name || ''
      form.announcement = group.announcement || ''
    } finally {
      loading.value = false
    }
  }

  function toggleMember(id: number) {
    const idx = selectedIds.value.indexOf(id)
    if (idx >= 0) selectedIds.value.splice(idx, 1)
    else selectedIds.value.push(id)
  }

  function isSelected(id: number) {
    return selectedIds.value.includes(id)
  }

  async function submitCreate() {
    if (!form.name.trim()) {
      uni.showToast({ title: '请输入群名称', icon: 'none' })
      return
    }
    if (!selectedIds.value.length) {
      uni.showToast({ title: '请选择成员', icon: 'none' })
      return
    }
    saving.value = true
    try {
      await createChatGroup({ name: form.name.trim(), memberIds: selectedIds.value })
      uni.showToast({ title: '群聊已创建', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 400)
    } catch (e) {
      logger.error(e)
    } finally {
      saving.value = false
    }
  }

  async function submitEdit() {
    if (!form.name.trim()) {
      uni.showToast({ title: '请输入群名称', icon: 'none' })
      return
    }
    saving.value = true
    try {
      await updateChatGroup({
        id: groupId.value,
        name: form.name.trim(),
        announcement: form.announcement.trim() || undefined,
      })
      if (selectedIds.value.length) {
        await addGroupMembers(groupId.value, selectedIds.value)
      }
      uni.showToast({ title: '已保存', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 400)
    } catch (e) {
      logger.error(e)
    } finally {
      saving.value = false
    }
  }

  async function init(options: { mode?: string; id?: string }) {
    mode.value = options.mode === 'edit' ? 'edit' : 'create'
    if (mode.value === 'edit') {
      groupId.value = Number(options.id)
      if (groupId.value) await loadGroup(groupId.value)
    } else {
      await loadUsers()
    }
  }

  return {
    loading,
    saving,
    mode,
    form,
    users,
    selectedIds,
    toggleMember,
    isSelected,
    init,
    submitCreate,
    submitEdit,
  }
}
