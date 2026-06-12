import { ref, reactive, computed, onMounted } from 'vue'
import { createApproval } from '@/api/system/approval'
import { getUserList } from '@/api/system/user'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { logger } from '@/utils/logger'
import { DICT_TYPE } from '@/constants/dict'
import type { UserVO } from '@/types/user'

export function useApprovalCreateForm() {
  const saving = ref(false)
  const approvers = ref<UserVO[]>([])
  const approverIndex = ref(0)
  const formTypeIndex = ref(0)
  const userStore = useUserStore()
  const { options: formTypeOptions, loading: formTypesLoading, load: loadFormTypes } = useDict(DICT_TYPE.APPROVAL_FORM_TYPE)

  const availableFormTypes = computed(() =>
    formTypeOptions.value.filter((o) => String(o.value) !== 'REGISTER'),
  )

  const form = reactive({
    formType: 'GENERAL',
    title: '',
    approverUserId: null as number | null,
    content: '',
  })

  async function loadApprovers() {
    await loadFormTypes()
    if (availableFormTypes.value.length) {
      form.formType = String(availableFormTypes.value[0].value)
    }
    const res = await getUserList()
    const selfId = userStore.userInfo?.userId
    approvers.value = (res.data || []).filter((u) => u.id !== selfId)
    if (approvers.value.length) {
      form.approverUserId = approvers.value[0].id
    }
  }

  function onFormTypeChange(e: { detail: { value: number } }) {
    formTypeIndex.value = Number(e.detail.value)
    const item = availableFormTypes.value[formTypeIndex.value]
    form.formType = item ? String(item.value) : 'GENERAL'
  }

  function onApproverChange(e: { detail: { value: number } }) {
    approverIndex.value = Number(e.detail.value)
    form.approverUserId = approvers.value[approverIndex.value]?.id ?? null
  }

  function approverLabel(u: UserVO) {
    return `${u.username}(${u.nickname || u.username})`
  }

  async function submit() {
    if (!form.title.trim()) {
      uni.showToast({ title: '请输入标题', icon: 'none' })
      return false
    }
    if (!form.approverUserId) {
      uni.showToast({ title: '请选择审批人', icon: 'none' })
      return false
    }
    saving.value = true
    try {
      await createApproval({
        formType: form.formType,
        title: form.title.trim(),
        approverUserId: form.approverUserId,
        content: form.content.trim(),
      })
      uni.showToast({ title: '提交成功', icon: 'success' })
      return true
    } catch (e) {
      logger.error(e)
      return false
    } finally {
      saving.value = false
    }
  }

  onMounted(loadApprovers)

  return {
    saving,
    form,
    approvers,
    approverIndex,
    formTypeIndex,
    availableFormTypes,
    formTypesLoading,
    onFormTypeChange,
    onApproverChange,
    approverLabel,
    submit,
  }
}

