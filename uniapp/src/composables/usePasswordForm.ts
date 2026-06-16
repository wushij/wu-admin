import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { getConfig } from '@/api/system/auth'
import { getProfile, changePassword, sendProfilePasswordSmsCode, resetPasswordBySms } from '@/api/system/profile'
import { sliderVerifyToRequest } from '@/utils/slider-captcha'
import type { SliderVerifyPayload } from '@/utils/slider-captcha'
import { logger } from '@/utils/logger'

type SecurityMode = 'password' | 'sms'

export function usePasswordForm() {
  const saving = ref(false)
  const sendingSms = ref(false)
  const smsCountdown = ref(0)
  const mode = ref<SecurityMode>('password')
  const smsEnabled = ref(false)
  const hasMobile = ref(false)
  const maskedMobile = ref('')
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

  let smsTimer: ReturnType<typeof setInterval> | null = null

  const canUseSmsReset = computed(() => smsEnabled.value && hasMobile.value)

  function maskMobile(mobile?: string) {
    if (!mobile || mobile.length < 7) return mobile || ''
    return `${mobile.slice(0, 3)}****${mobile.slice(-4)}`
  }

  function openSmsModeFromQuery(queryMode?: string) {
    if (queryMode === 'sms') mode.value = 'sms'
  }

  async function loadContext() {
    try {
      const [cfgRes, profileRes] = await Promise.all([getConfig(), getProfile()])
      smsEnabled.value = cfgRes.data?.login?.smsEnabled !== false
      const profile = profileRes.data
      hasMobile.value = !!profile.mobile?.trim()
      maskedMobile.value = maskMobile(profile.mobile)
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

  function openSmsMode() {
    if (!canUseSmsReset.value) {
      uni.showToast({ title: '请先绑定手机号', icon: 'none' })
      return
    }
    mode.value = 'sms'
    resetPasswordForm()
  }

  function closeSmsMode() {
    mode.value = 'password'
    resetSmsForm()
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

  onMounted(loadContext)

  onUnmounted(() => {
    if (smsTimer) clearInterval(smsTimer)
  })

  return {
    saving,
    sendingSms,
    smsCountdown,
    mode,
    form,
    smsForm,
    canUseSmsReset,
    maskedMobile,
    minPwdLen,
    showSlider,
    submit,
    sendSmsCode,
    onSliderSuccess,
    submitSmsReset,
    openSmsMode,
    openSmsModeFromQuery,
    closeSmsMode,
    resetPasswordForm,
    resetSmsForm,
  }
}
