import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { getConfig } from '@/api/system/auth'
import {
  getProfile, changePassword, sendProfilePasswordSmsCode, resetPasswordBySms,
  sendProfilePasswordEmailCode, resetPasswordByEmail,
} from '@/api/system/profile'
import { sliderVerifyToRequest } from '@/utils/slider-captcha'
import type { SliderVerifyPayload } from '@/utils/slider-captcha'
import { logger } from '@/utils/logger'

type SecurityMode = 'password' | 'sms' | 'email'

export function usePasswordForm() {
  const saving = ref(false)
  const sendingSms = ref(false)
  const sendingEmail = ref(false)
  const smsCountdown = ref(0)
  const emailCountdown = ref(0)
  const mode = ref<SecurityMode>('password')
  const smsEnabled = ref(false)
  const emailEnabled = ref(false)
  const hasMobile = ref(false)
  const hasEmail = ref(false)
  const maskedMobile = ref('')
  const maskedEmail = ref('')
  const minPwdLen = ref(6)
  const showSlider = ref(false)

  const form = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
  })

  const smsForm = reactive({
    smsCode: '',
    newPassword: '',
    confirmPassword: '',
  })

  const emailForm = reactive({
    emailCode: '',
    newPassword: '',
    confirmPassword: '',
  })

  let smsTimer: ReturnType<typeof setInterval> | null = null
  let emailTimer: ReturnType<typeof setInterval> | null = null

  const canUseSmsReset = computed(() => smsEnabled.value && hasMobile.value)
  const canUseEmailReset = computed(() => hasEmail.value)

  function maskMobile(mobile?: string) {
    if (!mobile || mobile.length < 7) return mobile || ''
    return `${mobile.slice(0, 3)}****${mobile.slice(-4)}`
  }

  function maskEmail(email?: string) {
    if (!email || !email.includes('@')) return email || ''
    const [name, domain] = email.split('@')
    if (name.length <= 2) return `${name.slice(0, 1)}****@${domain}`
    return `${name.slice(0, 2)}****@${domain}`
  }

  function openSmsModeFromQuery(queryMode?: string) {
    if (queryMode === 'sms') mode.value = 'sms'
    else if (queryMode === 'email') mode.value = 'email'
  }

  async function loadContext() {
    try {
      const [cfgRes, profileRes] = await Promise.all([getConfig(), getProfile()])
      smsEnabled.value = cfgRes.data?.login?.smsEnabled !== false
      const profile = profileRes.data
      hasMobile.value = !!profile.mobile?.trim()
      hasEmail.value = !!profile.email?.trim()
      maskedMobile.value = maskMobile(profile.mobile)
      maskedEmail.value = maskEmail(profile.email)
      minPwdLen.value = profile.minPasswordLength || 6
    } catch {
      /* 非关键 */
    }
  }

  function resetPasswordForm() {
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
  }

  function resetSmsForm() {
    smsForm.smsCode = ''
    smsForm.newPassword = ''
    smsForm.confirmPassword = ''
  }

  function resetEmailForm() {
    emailForm.emailCode = ''
    emailForm.newPassword = ''
    emailForm.confirmPassword = ''
  }

  function openSmsMode() {
    if (!canUseSmsReset.value) {
      uni.showToast({ title: '请先绑定手机号', icon: 'none' })
      return
    }
    mode.value = 'sms'
    resetPasswordForm()
  }

  function openEmailMode() {
    if (!canUseEmailReset.value) {
      uni.showToast({ title: '请先绑定邮箱', icon: 'none' })
      return
    }
    mode.value = 'email'
    resetPasswordForm()
  }

  function closeSmsMode() {
    mode.value = 'password'
    resetSmsForm()
  }

  function closeEmailMode() {
    mode.value = 'password'
    resetEmailForm()
  }

  function validatePwd(newPassword: string, confirmPassword: string) {
    if (!newPassword || !confirmPassword) {
      uni.showToast({ title: '请填写完整', icon: 'none' })
      return false
    }
    if (newPassword !== confirmPassword) {
      uni.showToast({ title: '两次密码不一致', icon: 'none' })
      return false
    }
    if (newPassword.length < minPwdLen.value) {
      uni.showToast({ title: `密码至少 ${minPwdLen.value} 位`, icon: 'none' })
      return false
    }
    return true
  }

  async function submit() {
    if (!form.oldPassword) {
      uni.showToast({ title: '请输入原密码', icon: 'none' })
      return
    }
    if (!validatePwd(form.newPassword, form.confirmPassword)) return
    saving.value = true
    try {
      await changePassword({ ...form })
      uni.showToast({ title: '修改成功', icon: 'success' })
      resetPasswordForm()
      setTimeout(() => uni.navigateBack(), 500)
    } catch (e) {
      logger.error(e)
    } finally {
      saving.value = false
    }
  }

  async function doSendSmsCode(slider: { uuid: string; code: string }) {
    if (sendingSms.value || smsCountdown.value > 0) return
    sendingSms.value = true
    try {
      await sendProfilePasswordSmsCode(slider)
      uni.showToast({ title: '验证码已发送', icon: 'success' })
      smsCountdown.value = 60
      smsTimer = setInterval(() => {
        smsCountdown.value -= 1
        if (smsCountdown.value <= 0 && smsTimer) {
          clearInterval(smsTimer)
          smsTimer = null
        }
      }, 1000)
    } catch (e) {
      logger.error(e)
    } finally {
      sendingSms.value = false
    }
  }

  function sendSmsCode() {
    showSlider.value = true
  }

  async function sendEmailCode() {
    if (!canUseEmailReset.value || sendingEmail.value || emailCountdown.value > 0) return
    sendingEmail.value = true
    try {
      await sendProfilePasswordEmailCode()
      uni.showToast({ title: '验证码已发送至邮箱', icon: 'success' })
      emailCountdown.value = 60
      emailTimer = setInterval(() => {
        emailCountdown.value -= 1
        if (emailCountdown.value <= 0 && emailTimer) {
          clearInterval(emailTimer)
          emailTimer = null
        }
      }, 1000)
    } catch (e) {
      logger.error(e)
    } finally {
      sendingEmail.value = false
    }
  }

  async function onSliderSuccess(payload: SliderVerifyPayload) {
    await doSendSmsCode(sliderVerifyToRequest(payload))
  }

  async function submitSmsReset() {
    if (!smsForm.smsCode.trim()) {
      uni.showToast({ title: '请输入验证码', icon: 'none' })
      return
    }
    if (!validatePwd(smsForm.newPassword, smsForm.confirmPassword)) return
    saving.value = true
    try {
      await resetPasswordBySms({ ...smsForm })
      uni.showToast({ title: '密码已重置', icon: 'success' })
      resetSmsForm()
      setTimeout(() => uni.navigateBack(), 500)
    } catch (e) {
      logger.error(e)
    } finally {
      saving.value = false
    }
  }

  async function submitEmailReset() {
    if (!emailForm.emailCode.trim()) {
      uni.showToast({ title: '请输入邮箱验证码', icon: 'none' })
      return
    }
    if (!validatePwd(emailForm.newPassword, emailForm.confirmPassword)) return
    saving.value = true
    try {
      await resetPasswordByEmail({ ...emailForm })
      uni.showToast({ title: '密码已重置', icon: 'success' })
      resetEmailForm()
      setTimeout(() => uni.navigateBack(), 500)
    } catch (e) {
      logger.error(e)
    } finally {
      saving.value = false
    }
  }

  onMounted(loadContext)

  onUnmounted(() => {
    if (smsTimer) clearInterval(smsTimer)
    if (emailTimer) clearInterval(emailTimer)
  })

  return {
    saving,
    sendingSms,
    sendingEmail,
    smsCountdown,
    emailCountdown,
    mode,
    form,
    smsForm,
    emailForm,
    canUseSmsReset,
    canUseEmailReset,
    maskedMobile,
    maskedEmail,
    minPwdLen,
    showSlider,
    submit,
    sendSmsCode,
    sendEmailCode,
    onSliderSuccess,
    submitSmsReset,
    submitEmailReset,
    openSmsMode,
    openEmailMode,
    openSmsModeFromQuery,
    closeSmsMode,
    closeEmailMode,
    resetPasswordForm,
    resetSmsForm,
    resetEmailForm,
  }
}
