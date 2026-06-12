import { ref, computed, reactive, watch } from 'vue'
import { showConfirm, showActionSheet } from '@/utils/app-dialog'
import {
  getGroupDetail,
  getGroupMembers,
  getGroupLogs,
  getChatUsers,
  updateChatGroup,
  addGroupMembers,
  removeGroupMember,
  setGroupAdmin,
  setGroupMuted,
  transferGroupOwner,
  setGroupNotifyMuted,
  quitGroup,
  dissolveGroup,
} from '@/api/message'
import { useMessageStore } from '@/store/message'
import { useUserStore } from '@/store/user'
import { groupMemberDisplayName } from '@/utils/chat-message'
import type { ChatGroup, ChatGroupLogItem, ChatUser, GroupMember } from '@/types/message'

export type GroupDetailTab = 'info' | 'members' | 'logs'

export function useGroupDetail() {
  const userStore = useUserStore()
  const messageStore = useMessageStore()
  const loading = ref(false)
  const saving = ref(false)
  const membersLoading = ref(false)
  const logsLoading = ref(false)
  const tab = ref<GroupDetailTab>('info')
  const groupId = ref(0)
  const group = ref<ChatGroup | null>(null)
  const members = ref<GroupMember[]>([])
  const logs = ref<ChatGroupLogItem[]>([])
  const allUsers = ref<ChatUser[]>([])
  const addMemberIds = ref<number[]>([])

  const form = reactive({
    name: '',
    announcement: '',
    notifyMuted: false,
  })

  const selfId = computed(() => userStore.userInfo.userId)

  const myMember = computed(() => members.value.find((m) => m.userId === selfId.value))

  const isGroupOwner = computed(() => myMember.value?.role === 2)

  const canEditGroup = computed(() => {
    const role = myMember.value?.role
    return role === 2 || role === 1
  })

  const availableAddUsers = computed(() => {
    const memberIds = new Set(members.value.map((m) => m.userId))
    return allUsers.value.filter((u) => u.id !== selfId.value && !memberIds.has(u.id))
  })

  function memberName(m: GroupMember) {
    return groupMemberDisplayName(m)
  }

  function roleLabel(role?: number) {
    if (role === 2) return '群主'
    if (role === 1) return '管理员'
    return ''
  }

  function canManageMember(m: GroupMember) {
    if (m.userId === selfId.value) return false
    const me = myMember.value
    if (!me) return false
    if (me.role === 2) return m.role !== 2
    if (me.role === 1) return m.role === 0
    return false
  }

  async function loadGroup() {
    const res = await getGroupDetail(groupId.value)
    group.value = res.data || null
    if (!group.value) return
    form.name = group.value.name || ''
    form.announcement = group.value.announcement || ''
    form.notifyMuted = !!group.value.notifyMuted
    uni.setNavigationBarTitle({ title: group.value.name || '群组详情' })
  }

  async function loadMembers() {
    membersLoading.value = true
    try {
      const res = await getGroupMembers(groupId.value)
      members.value = res.data || []
    } finally {
      membersLoading.value = false
    }
  }

  async function loadLogs() {
    logsLoading.value = true
    try {
      const res = await getGroupLogs(groupId.value)
      logs.value = res.data || []
    } finally {
      logsLoading.value = false
    }
  }

  async function loadUsers() {
    const res = await getChatUsers()
    allUsers.value = res.data || []
  }

  async function init(id: number) {
    groupId.value = id
    loading.value = true
    try {
      await Promise.all([loadGroup(), loadMembers(), loadUsers()])
    } finally {
      loading.value = false
    }
  }

  async function saveInfo() {
    if (!canEditGroup.value || !form.name.trim()) {
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
      await loadGroup()
      uni.showToast({ title: '已保存', icon: 'success' })
    } finally {
      saving.value = false
    }
  }

  async function toggleNotifyMuted() {
    const next = !form.notifyMuted
    const prev = form.notifyMuted
    form.notifyMuted = next
    try {
      await setGroupNotifyMuted(groupId.value, next)
      messageStore.setGroupNotifyMutedLocal(groupId.value, next)
      if (group.value) group.value.notifyMuted = next
      uni.showToast({ title: next ? '已开启免打扰' : '已关闭免打扰', icon: 'none' })
    } catch {
      form.notifyMuted = prev
      uni.showToast({ title: '设置失败', icon: 'none' })
    }
  }

  function toggleAddMember(id: number) {
    const idx = addMemberIds.value.indexOf(id)
    if (idx >= 0) addMemberIds.value.splice(idx, 1)
    else addMemberIds.value.push(id)
  }

  async function submitAddMembers() {
    if (!addMemberIds.value.length) return
    await addGroupMembers(groupId.value, addMemberIds.value)
    addMemberIds.value = []
    await loadMembers()
    uni.showToast({ title: '成员已添加', icon: 'success' })
  }

  async function openMemberActions(m: GroupMember) {
    if (!canManageMember(m)) return
    const actions: string[] = []
    const cmds: string[] = []
    if (isGroupOwner.value && m.role === 0) {
      actions.push('设为管理员')
      cmds.push('setAdmin')
    }
    if (isGroupOwner.value && m.role === 1) {
      actions.push('取消管理员')
      cmds.push('removeAdmin')
    }
    if (!m.muted) {
      actions.push('禁言')
      cmds.push('mute')
    } else {
      actions.push('解除禁言')
      cmds.push('unmute')
    }
    if (isGroupOwner.value && m.role !== 2) {
      actions.push('转让群主')
      cmds.push('transfer')
    }
    actions.push('移除成员')
    cmds.push('remove')

    const items = actions.map((label, index) => ({
      label,
      danger: cmds[index] === 'remove',
    }))
    try {
      const tapIndex = await showActionSheet({ items })
      await runMemberAction(cmds[tapIndex], m)
    } catch {
      /* cancelled */
    }
  }

  async function runMemberAction(cmd: string, m: GroupMember) {
    const gid = groupId.value
    if (cmd === 'transfer') {
      const ok = await confirm(`确定将群主转让给 ${memberName(m)} 吗？`)
      if (!ok) return
      await transferGroupOwner(gid, m.userId)
    } else if (cmd === 'remove') {
      const ok = await confirm('确定移除该成员吗？')
      if (!ok) return
      await removeGroupMember(gid, m.userId)
    } else if (cmd === 'setAdmin') await setGroupAdmin(gid, m.userId, true)
    else if (cmd === 'removeAdmin') await setGroupAdmin(gid, m.userId, false)
    else if (cmd === 'mute') await setGroupMuted(gid, m.userId, true)
    else if (cmd === 'unmute') await setGroupMuted(gid, m.userId, false)

    uni.showToast({ title: '操作成功', icon: 'success' })
    await loadMembers()
    if (tab.value === 'logs') await loadLogs()
  }

  async function confirm(content: string, tone: 'default' | 'danger' = 'default') {
    const result = await showConfirm({ content, tone })
    return result.confirmed
  }

  async function handleQuit() {
    const ok = await confirm('确定要退出该群组吗？')
    if (!ok) return
    await quitGroup(groupId.value)
    uni.showToast({ title: '已退出群组', icon: 'success' })
    setTimeout(() => uni.navigateBack({ delta: 2 }), 400)
  }

  async function handleDissolve() {
    const ok = await confirm('确定要解散该群组吗？此操作不可撤销！', 'danger')
    if (!ok) return
    await dissolveGroup(groupId.value)
    uni.showToast({ title: '群组已解散', icon: 'success' })
    setTimeout(() => uni.navigateBack({ delta: 2 }), 400)
  }

  watch(tab, (key) => {
    if (key === 'logs' && !logs.value.length) loadLogs()
    if (key === 'members') loadMembers()
  })

  return {
    loading,
    saving,
    membersLoading,
    logsLoading,
    tab,
    group,
    members,
    logs,
    form,
    addMemberIds,
    availableAddUsers,
    isGroupOwner,
    canEditGroup,
    memberName,
    roleLabel,
    canManageMember,
    init,
    saveInfo,
    toggleNotifyMuted,
    toggleAddMember,
    submitAddMembers,
    openMemberActions,
    handleQuit,
    handleDissolve,
  }
}
