import { ref, reactive, computed, onUnmounted } from 'vue'
import {
  getProfile,
  updateProfile,
  uploadAvatar,
  sendProfileMobileBindSmsCode,
  bindProfileMobile,
} from '@/api/system/profile'
import { getConfig } from '@/api/system/auth'
import { fileDisplayUrl } from '@/api/system/file/index'
import { logger } from '@/utils/logger'
import { useUserStore } from '@/store/user'
import { SLIDER_VERIFIED_CODE } from '@/constants'

export function useProfileForm() {
  const loading = ref(false)
  const saving = ref(false)
  const uploading = ref(false)
  const sendingBindSms = ref(false)
  const bindSmsCountdown = ref(0)
  const smsEnabled = ref(false)
  const avatarUrl = ref('')
  const form = reactive({
    userId: undefined as number | undefined,
    nickname: '',
    email: '',
    username: '',
    deptName: '',
    mobile: '',
    bindMobile: '',
    bindSmsCode: '',
    updateTime: '',
    roleNames: [] as string[],
    postNames: [] as string[],
  })

  let bindSmsTimer: ReturnType<typeof setInterval> | null = null

  const hasMobile = computed(() => !!form.mobile?.trim())
  const canBindMobile = computed(() => smsEnabled.value && !hasMobile.value)

  function maskMobile(mobile?: string) {
    if (!mobile || mobile.length < 7) return mobile || ''
    return `${mobile.slice(0, 3)}****${mobile.slice(-4)}`
  }

  async function load() {
    loading.value = true
    try {
      const [cfgRes, profileRes] = await Promise.all([getConfig(), getProfile()])
      smsEnabled.value = cfgRes.data?.login?.smsEnabled !== false
      const data = profileRes.data
      form.userId = data.userId
      form.nickname = data.nickname || ''
      form.email = data.email || ''
      form.username = data.username || ''
      form.deptName = data.deptName || ''
      form.mobile = data.mobile || ''
      form.updateTime = data.updateTime || ''
      form.roleNames = data.roleNames || []
      form.postNames = data.postNames || []
      avatarUrl.value = data.avatar ? fileDisplayUrl(data.avatar) : ''
    } catch (e) {
      logger.error(e)
      uni.showToast({ title: '加载资料失败', icon: 'none' })
    } finally {
      loading.value = false
    }
  }

  async function syncUserStore() {
    try {
      await useUserStore().getUserInfo()
    } catch (e) {
      logger.warn('同步用户信息失败', e)
    }
  }

  async function pickAvatar() {
    try {
      const choose = await uni.chooseImage({ count: 1, sizeType: ['compressed'] })
      const filePath = choose.tempFilePaths?.[0]
      if (!filePath) return
      uploading.value = true
      const res = await uploadAvatar(filePath)
      const url = res.data?.url
      if (!url) {
        uni.showToast({ title: '上传失败', icon: 'none' })
        return
      }
      await updateProfile({ avatar: url })
      avatarUrl.value = fileDisplayUrl(url)
      useUserStore().patchUserInfo({ avatar: url })
      await syncUserStore()
      uni.showToast({ title: '头像已更新', icon: 'success' })
    } catch (e) {
      logger.error(e)
      uni.showToast({ title: '头像上传失败', icon: 'none' })
    } finally {
      uploading.value = false
    }
  }

  function startBindSmsCountdown() {
    bindSmsCountdown.value = 60
    if (bindSmsTimer) clearInterval(bindSmsTimer)
    bindSmsTimer = setInterval(() => {
      bindSmsCountdown.value -= 1
      if (bindSmsCountdown.value <= 0 && bindSmsTimer) {
        clearInterval(bindSmsTimer)
        bindSmsTimer = null
      }
    }, 1000)
  }

  async function sendBindSmsCode() {
    const mobile = form.bindMobile.trim()
    if (!/^1[3-9]\d{9}$/.test(mobile)) {
      uni.showToast({ title: '请输入正确手机号', icon: 'none' })
      return
    }
    sendingBindSms.value = true
    try {
      await sendProfileMobileBindSmsCode({ mobile, code: SLIDER_VERIFIED_CODE })
      uni.showToast({ title: '验证码已发送', icon: 'success' })
      startBindSmsCountdown()
    } catch (e) {
      logger.error(e)
    } finally {
      sendingBindSms.value = false
    }
  }

  async function bindMobile() {
    const mobile = form.bindMobile.trim()
    const smsCode = form.bindSmsCode.trim()
    if (!mobile || !smsCode) {
      uni.showToast({ title: '请填写手机号和验证码', icon: 'none' })
      return
    }
    saving.value = true
    try {
      await bindProfileMobile({ mobile, smsCode })
      form.mobile = mobile
      form.bindMobile = ''
      form.bindSmsCode = ''
      await syncUserStore()
      uni.showToast({ title: '绑定成功', icon: 'success' })
    } catch (e) {
      logger.error(e)
    } finally {
      saving.value = false
    }
  }

  async function save() {
    if (!form.nickname.trim()) {
      uni.showToast({ title: '请输入昵称', icon: 'none' })
      return
    }
    saving.value = true
    try {
      await updateProfile({
        nickname: form.nickname.trim(),
        email: form.email.trim() || undefined,
      })
      useUserStore().patchUserInfo({ nickname: form.nickname.trim() })
      await syncUserStore()
      uni.showToast({ title: '保存成功', icon: 'success' })
    } catch (e) {
      logger.error(e)
    } finally {
      saving.value = false
    }
  }

  onUnmounted(() => {
    if (bindSmsTimer) clearInterval(bindSmsTimer)
  })

  return {
    loading,
    saving,
    uploading,
    sendingBindSms,
    bindSmsCountdown,
    avatarUrl,
    form,
    hasMobile,
    canBindMobile,
    maskMobile,
    load,
    pickAvatar,
    sendBindSmsCode,
    bindMobile,
    save,
  }
}
