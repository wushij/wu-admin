import { ref, reactive } from 'vue'
import { createTicket, getTicketAssigneeOptions } from '@/api/system/ticket'
import { reloadDictTypes, useDict } from '@/composables/useDict'
import { DICT_TYPE } from '@/constants/dict'
import type { AssigneeOptionVO } from '@/types/system'
import { logger } from '@/utils/logger'

export function useTicketCreateForm() {
  const saving = ref(false)
  const ready = ref(false)
  const assignees = ref<AssigneeOptionVO[]>([])
  const assigneeIndex = ref(0)
  const priorityIndex = ref(0)
  const { options: priorityOptions, load: loadPriority } = useDict(DICT_TYPE.TICKET_PRIORITY)

  const form = reactive({
    title: '',
    description: '',
    priority: '',
    assigneeUserId: null as number | null,
    deadline: '',
  })

  function resetForm() {
    form.title = ''
    form.description = ''
    form.priority = ''
    form.assigneeUserId = null
    form.deadline = ''
    assigneeIndex.value = 0
    priorityIndex.value = 0
  }

  async function loadAssignees() {
    ready.value = false
    try {
      await reloadDictTypes([DICT_TYPE.TICKET_PRIORITY])
      await loadPriority(true)
      if (priorityOptions.value.length) {
        form.priority = String(priorityOptions.value[0].value)
        priorityIndex.value = 0
      }
      const res = await getTicketAssigneeOptions()
      assignees.value = [{ id: 0, nickname: '全员通知', username: 'all' }, ...(res.data || [])]
      if (assignees.value.length) {
        assigneeIndex.value = 0
        form.assigneeUserId = assignees.value[0].id
      }
    } catch (e) {
      logger.error(e)
    } finally {
      ready.value = true
    }
  }

  async function initPage() {
    resetForm()
    await loadAssignees()
  }

  function onAssigneeChange(e: { detail: { value: number } }) {
    assigneeIndex.value = Number(e.detail.value)
    const item = assignees.value[assigneeIndex.value]
    form.assigneeUserId = item ? item.id : null
  }

  function onPriorityChange(e: { detail: { value: number } }) {
    priorityIndex.value = Number(e.detail.value)
    const item = priorityOptions.value[priorityIndex.value]
    form.priority = item ? String(item.value) : ''
  }

  function onDeadlineChange(e: { detail: { value: string } }) {
    form.deadline = e.detail.value ? `${e.detail.value}T23:59:59` : ''
  }

  async function submit() {
    if (!form.title.trim()) {
      uni.showToast({ title: '请输入标题', icon: 'none' })
      return false
    }
    saving.value = true
    try {
      await createTicket({
        title: form.title.trim(),
        description: form.description.trim() || undefined,
        priority: form.priority || undefined,
        assigneeUserId: form.assigneeUserId,
        deadline: form.deadline || null,
      })
      uni.showToast({ title: '创建成功', icon: 'success' })
      return true
    } catch (e) {
      logger.error(e)
      return false
    } finally {
      saving.value = false
    }
  }

  return {
    saving,
    ready,
    form,
    assignees,
    assigneeIndex,
    priorityOptions,
    priorityIndex,
    initPage,
    onAssigneeChange,
    onPriorityChange,
    onDeadlineChange,
    submit,
  }
}
