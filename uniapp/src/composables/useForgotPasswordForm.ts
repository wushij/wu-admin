import { ref, reactive, onUnmounted } from 'vue'
import { getConfig, checkForgotPassword, sendForgotPasswordSmsCode, resetForgotPassword } from '@/api/system/auth'
import { sliderVerifyToRequest } from '@/utils/slider-captcha'
import type { SliderVerifyPayload } from '@/utils/slider-captcha'
import { navigateToLogin } from '@/utils/auth-route'
import { logger } from '@/utils/logger'

type ForgotStep = 1 | 2 | 3

export function useForgotPasswordForm() {
  const step = ref<ForgotStep>(1)
  const loading = ref(false)
  const sendingSms = ref(false)
  const smsCountdown = ref(0)
  const showSlider = ref(false)
  const maskedMobile = ref('')
  const minPwdLen = ref(6)

  const form = reactive({
    username: '',
    smsCode: '',
    newPassword: '',
    confirmPassword: '',
  })

  let smsTimer: ReturnType<typeof setInterval> | null = null

  async function loadConfig() {
    try {
      const res = await getConfig()
      minPwdLen.value = res.data?.register?.minPasswordLength || 6
    } catch {
      /* 使用默认值 */
    }
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

  async function submitUsername() {
    const username = form.username.trim()
    if (!username) {
      uni.showToast({ title: '请输入用户名', icon: 'none' })
      return
    }
    loading.value = true
    try {
      const res = await checkForgotPassword(username)
      maskedMobile.value = res.data?.maskedMobile || ''
      minPwdLen.value = res.data?.minPasswordLength || minPwdLen.value
      step.value = 2
    } catch (e) {
      logger.error(e)
    } finally {
      loading.value = false
    }
  }

  function startSmsCountdown() {
    smsCountdown.value = 60
    smsTimer = setInterval(() => {
      smsCountdown.value -= 1
      if (smsCountdown.value <= 0 && smsTimer) {
        clearInterval(smsTimer)
        smsTimer = null
      }
    }, 1000)
  }

  async function doSendSms(slider: { uuid: string; code: string }) {
    if (sendingSms.value || smsCountdown.value > 0) return
    sendingSms.value = true
    try {
      await sendForgotPasswordSmsCode(form.username.trim(), slider)
      uni.showToast({ title: '验证码已发送', icon: 'success' })
      startSmsCountdown()
    } catch (e) {
      logger.error(e)
    } finally {
      sendingSms.value = false
    }
  }

  function handleSendSms() {
    showSlider.value = true
  }

  async function onSliderSuccess(payload: SliderVerifyPayload) {
    await doSendSms(sliderVerifyToRequest(payload))
  }

  function submitSmsStep() {
    if (!form.smsCode.trim()) {
      uni.showToast({ title: '请输入验证码', icon: 'none' })
      return
    }
    step.value = 3
  }

  async function submitReset() {
    if (!validatePwd(form.newPassword, form.confirmPassword)) return
    loading.value = true
    try {
      await resetForgotPassword({
        username: form.username.trim(),
        smsCode: form.smsCode.trim(),
        newPassword: form.newPassword,
        confirmPassword: form.confirmPassword,
      })
      uni.showToast({ title: '密码已重置', icon: 'success' })
      setTimeout(() => navigateToLogin(), 600)
    } catch (e) {
      logger.error(e)
    } finally {
      loading.value = false
    }
  }

  function goBack() {
    if (step.value === 1) {
      uni.navigateBack({
        fail: () => navigateToLogin(),
      })
      return
    }
    if (step.value === 3) {
      step.value = 2
      form.newPassword = ''
      form.confirmPassword = ''
      return
    }
    step.value = 1
    form.smsCode = ''
    maskedMobile.value = ''
  }

  onUnmounted(() => {
    if (smsTimer) clearInterval(smsTimer)
  })

  return {
    step,
    loading,
    sendingSms,
    smsCountdown,
    showSlider,
    maskedMobile,
    minPwdLen,
    form,
    loadConfig,
    submitUsername,
    handleSendSms,
    onSliderSuccess,
    submitSmsStep,
    submitReset,
    goBack,
  }
}
