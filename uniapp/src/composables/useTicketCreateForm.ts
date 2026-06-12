import { ref, reactive, onMounted } from 'vue'
import { createTicket, getTicketAssigneeOptions } from '@/api/system/ticket'
import { useDict } from '@/composables/useDict'
import { DICT_TYPE } from '@/constants/dict'
import type { AssigneeOptionVO } from '@/types/system'
import { logger } from '@/utils/logger'

export function useTicketCreateForm() {
  const saving = ref(false)
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

  async function loadAssignees() {
    await loadPriority()
    if (priorityOptions.value.length) {
      form.priority = String(priorityOptions.value[0].value)
    }
    const res = await getTicketAssigneeOptions()
    assignees.value = [{ id: 0, nickname: '全员通知', username: 'all' }, ...(res.data || [])]
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
      return
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
      setTimeout(() => uni.navigateBack(), 400)
    } catch (e) {
      logger.error(e)
    } finally {
      saving.value = false
    }
  }

  onMounted(loadAssignees)

  return {
    saving,
    form,
    assignees,
    assigneeIndex,
    priorityOptions,
    priorityIndex,
    onAssigneeChange,
    onPriorityChange,
    onDeadlineChange,
    submit,
  }
}
