import { ref, reactive, computed } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  getProfile, updateProfile, uploadAvatar,
} from '@/api/system/profile'
import type { UserProfile } from '@/types/profile'

export function useProfileInfo() {
  const userStore = useUserStore()

  const profile = ref<UserProfile>({} as UserProfile)
  const pageLoading = ref(false)
  const savingInfo = ref(false)
  const avatarInputRef = ref<HTMLInputElement>()
  const basicInfoFormRef = ref<{ formRef?: FormInstance }>()
  const mobileBindEditing = ref(false)
  const emailBindEditing = ref(false)

  function getInfoFormRef() {
    return basicInfoFormRef.value?.formRef
  }

  const infoForm = reactive({
    nickname: '',
    email: '',
    bindMobile: '',
    bindSmsCode: '',
    bindEmail: '',
    bindEmailCode: '',
  })

  const avatarSrc = computed(() => {
    const url = profile.value.avatar || userStore.userInfo.avatar
    if (!url) return undefined
    return url.startsWith('http') ? url : url
  })

  const avatarFallback = computed(() => {
    const name = profile.value.nickname || profile.value.username || 'U'
    return name.charAt(0).toUpperCase()
  })

  const statusLabel = computed(() => {
    const map: Record<number, string> = { 0: '已停用', 1: '正常', 2: '待审核', 3: '审核驳回' }
    return map[profile.value.status ?? 1] ?? '未知'
  })

  const statusTagType = computed(() => {
    const map: Record<number, 'success' | 'warning' | 'danger' | 'info'> = { 0: 'danger', 1: 'success', 2: 'warning', 3: 'danger' }
    return map[profile.value.status ?? 1] ?? 'info'
  })

  const postDisplay = computed(() => {
    const names = profile.value.postNames
    if (!names?.length) return '未分配'
    return names.join('、')
  })

  const lastLoginDisplay = computed(() => {
    if (!profile.value.lastLoginTime) return '暂无记录'
    return formatTime(profile.value.lastLoginTime, true)
  })

  const maskedMobile = computed(() => {
    const m = (profile.value.mobile || '').trim()
    if (!/^1[3-9]\d{9}$/.test(m)) return '未绑定手机号'
    return `${m.slice(0, 3)} **** ${m.slice(-4)}`
  })

  const hasBoundMobile = computed(() => /^1[3-9]\d{9}$/.test((profile.value.mobile || '').trim()))

  const maskedEmail = computed(() => {
    const e = (profile.value.email || '').trim()
    if (!e) return '未绑定邮箱'
    const [name, domain] = e.split('@')
    if (!domain) return e
    if (name.length <= 2) return `${name.slice(0, 1)}****@${domain}`
    return `${name.slice(0, 2)}****@${domain}`
  })

  const hasBoundEmail = computed(() => /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test((profile.value.email || '').trim()))

  function formatTime(time?: string, short = false) {
    if (!time) return '—'
    if (short) return time.replace('T', ' ').slice(0, 16)
    return time.replace('T', ' ').slice(0, 19)
  }

  function syncInfoForm() {
    infoForm.nickname = profile.value.nickname || ''
    infoForm.email = profile.value.email || ''
    infoForm.bindMobile = ''
    infoForm.bindSmsCode = ''
    infoForm.bindEmail = ''
    infoForm.bindEmailCode = ''
    mobileBindEditing.value = !hasBoundMobile.value
    emailBindEditing.value = false
  }

  async function loadProfile() {
    pageLoading.value = true
    try {
      const res = await getProfile()
      profile.value = res.data
      syncInfoForm()
    } finally {
      pageLoading.value = false
    }
  }

  function resetInfoForm() {
    syncInfoForm()
    getInfoFormRef()?.clearValidate()
  }

  async function handleSaveInfo() {
    try {
      await getInfoFormRef()?.validateField('nickname')
      await getInfoFormRef()?.validateField('email')
    } catch { return }
    savingInfo.value = true
    try {
      await updateProfile({ nickname: infoForm.nickname.trim(), email: infoForm.email.trim() })
      ElMessage.success('资料已保存')
      await loadProfile()
      userStore.patchUserInfo({ nickname: infoForm.nickname.trim() })
    } finally { savingInfo.value = false }
  }

  function triggerAvatarUpload() { avatarInputRef.value?.click() }

  async function handleAvatarChange(e: Event) {
    const input = e.target as HTMLInputElement
    const file = input.files?.[0]
    input.value = ''
    if (!file) return
    if (!file.type.startsWith('image/')) { ElMessage.warning('请选择图片文件'); return }
    if (file.size > 2 * 1024 * 1024) { ElMessage.warning('头像大小不能超过 2MB'); return }
    try {
      const res = await uploadAvatar(file)
      const url = res.data?.url
      if (url) {
        profile.value.avatar = url
        userStore.patchUserInfo({ avatar: url })
        ElMessage.success('头像已更新')
      }
    } catch { /* request 拦截器已提示 */ }
  }

  function openMobileBindEditing() {
    mobileBindEditing.value = true
    infoForm.bindMobile = ''
    infoForm.bindSmsCode = ''
    getInfoFormRef()?.clearValidate(['bindMobile', 'bindSmsCode'])
  }

  function cancelMobileBindEditing() {
    mobileBindEditing.value = false
    infoForm.bindMobile = ''
    infoForm.bindSmsCode = ''
    getInfoFormRef()?.clearValidate(['bindMobile', 'bindSmsCode'])
  }

  function openEmailBindEditing() {
    emailBindEditing.value = true
    infoForm.bindEmail = ''
    infoForm.bindEmailCode = ''
    getInfoFormRef()?.clearValidate(['bindEmail', 'bindEmailCode'])
  }

  function cancelEmailBindEditing() {
    emailBindEditing.value = false
    infoForm.bindEmail = ''
    infoForm.bindEmailCode = ''
    getInfoFormRef()?.clearValidate(['bindEmail', 'bindEmailCode'])
  }

  return {
    profile, pageLoading, savingInfo, avatarInputRef, basicInfoFormRef, getInfoFormRef,
    mobileBindEditing, emailBindEditing, infoForm,
    avatarSrc, avatarFallback, statusLabel, statusTagType,
    postDisplay, lastLoginDisplay, maskedMobile, hasBoundMobile,
    maskedEmail, hasBoundEmail,
    formatTime, loadProfile, resetInfoForm, handleSaveInfo,
    triggerAvatarUpload, handleAvatarChange,
    openMobileBindEditing, cancelMobileBindEditing,
    openEmailBindEditing, cancelEmailBindEditing,
  }
}
