import { ref, reactive, computed } from 'vue'
import { createApproval, getApprovalApproverOptions } from '@/api/system/approval'
import {
  useDict,
  reloadDictTypes,
  getDictDefaultValue,
} from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { logger } from '@/utils/logger'
import { DICT_TYPE } from '@/constants/dict'
import type { DictOption } from '@/types/api'
import type { UserVO } from '@/types/user'

/** 字典未加载或为空时的兜底选项（不含注册审核） */
const FALLBACK_FORM_TYPES: DictOption[] = [
  { label: '通用', value: 'GENERAL' },
  { label: '请假', value: 'LEAVE' },
  { label: '采购', value: 'PURCHASE' },
  { label: '报销', value: 'REIMBURSE' },
  { label: '用印', value: 'SEAL' },
  { label: '合同', value: 'CONTRACT' },
]

/** 兜底：接口数据异常时仍排除当前登录用户 */
function excludeSelfApprovers(users: UserVO[], self: { userId?: number | null; username?: string }) {
  return users.filter((u) => {
    if (self.userId != null && Number(u.id) === Number(self.userId)) return false
    if (self.username && u.username === self.username) return false
    return true
  })
}

export function useApprovalCreateForm() {
  const saving = ref(false)
  const ready = ref(false)
  const approvers = ref<UserVO[]>([])
  const approverIndex = ref(0)
  const formTypeIndex = ref(0)
  const userStore = useUserStore()
  const { options: formTypeOptions, load: loadFormTypes } = useDict(
    DICT_TYPE.APPROVAL_FORM_TYPE,
  )

  const availableFormTypes = computed(() => {
    const fromDict = formTypeOptions.value.filter((o) => String(o.value) !== 'REGISTER')
    return fromDict.length ? fromDict : FALLBACK_FORM_TYPES
  })

  const form = reactive({
    formType: 'GENERAL',
    title: '',
    approverUserId: null as number | null,
    content: '',
  })

  function resetForm() {
    form.title = ''
    form.content = ''
    form.approverUserId = null
    form.formType = 'GENERAL'
    approverIndex.value = 0
    formTypeIndex.value = 0
  }

  function syncFormTypeIndex() {
    const idx = availableFormTypes.value.findIndex((o) => String(o.value) === form.formType)
    formTypeIndex.value = idx >= 0 ? idx : 0
  }

  function syncApproverIndex() {
    if (!approvers.value.length || form.approverUserId == null) {
      approverIndex.value = 0
      return
    }
    const idx = approvers.value.findIndex((u) => Number(u.id) === Number(form.approverUserId))
    approverIndex.value = idx >= 0 ? idx : 0
  }

  function applyDefaultFormType() {
    const types = availableFormTypes.value
    if (!types.length) return
    const defaultValue = getDictDefaultValue(DICT_TYPE.APPROVAL_FORM_TYPE) || 'GENERAL'
    const hit = types.find((o) => String(o.value) === String(defaultValue))
    form.formType = hit ? String(hit.value) : String(types[0].value)
    syncFormTypeIndex()
  }

  async function loadApprovers() {
    ready.value = false
    try {
      if (userStore.userInfo?.userId == null) {
        try {
          await userStore.getUserInfo()
        } catch {
          /* 忽略，后续仍尝试加载审批人 */
        }
      }

      await reloadDictTypes([DICT_TYPE.APPROVAL_FORM_TYPE])
      await loadFormTypes(true)
      applyDefaultFormType()

      const res = await getApprovalApproverOptions()
      const self = {
        userId: userStore.userInfo?.userId,
        username: userStore.userInfo?.username,
      }
      const list = (res.data || []).map((u) => ({
        id: u.id,
        username: u.username,
        nickname: u.nickname,
      })) as UserVO[]
      approvers.value = excludeSelfApprovers(list, self)
      syncApproverIndex()
    } catch (e) {
      logger.error(e)
      applyDefaultFormType()
      approvers.value = []
      form.approverUserId = null
    } finally {
      ready.value = true
    }
  }

  async function initPage() {
    resetForm()
    await loadApprovers()
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
    const selfId = userStore.userInfo?.userId
    if (selfId != null && Number(form.approverUserId) === Number(selfId)) {
      uni.showToast({ title: '不能选择自己作为审批人', icon: 'none' })
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

  return {
    saving,
    ready,
    form,
    approvers,
    approverIndex,
    formTypeIndex,
    availableFormTypes,
    initPage,
    onFormTypeChange,
    onApproverChange,
    approverLabel,
    submit,
  }
}
