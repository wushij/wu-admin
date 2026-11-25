import { ref, reactive, onUnmounted } from 'vue'
import { getConfig, checkForgotPassword, sendForgotPasswordSmsCode, resetForgotPassword } from '@/api/system/auth'
import { sliderVerifyToRequest } from '@/utils/security/slider-captcha'
import type { SliderVerifyPayload } from '@/utils/security/slider-captcha'
import { navigateToLogin } from '@/utils/nav/auth-route'
import { logger } from '@/core/logger'
import { encryptSm2 } from '@/utils/security/crypto'
import { showAuthToast } from '@/composables/auth/useAuthToast'

type ForgotStep = 1 | 2 | 3

export function useForgotPasswordForm() {
  const step = ref<ForgotStep>(1)
  const loading = ref(false)
  const resetSuccess = ref(false)
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
  let redirectTimer: ReturnType<typeof setTimeout> | null = null

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
      showAuthToast('请填写完整', 'warning')
      return false
    }
    if (newPassword !== confirmPassword) {
      showAuthToast('两次密码不一致', 'warning')
      return false
    }
    if (newPassword.length < minPwdLen.value) {
      showAuthToast(`密码至少 ${minPwdLen.value} 位`, 'warning')
      return false
    }
    return true
  }

  async function submitUsername() {
    const username = form.username.trim()
    if (!username) {
      showAuthToast('请输入用户名', 'warning')
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
      showAuthToast('验证码已发送', 'success')
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
      showAuthToast('请输入验证码', 'warning')
      return
    }
    step.value = 3
  }

  async function submitReset() {
    if (loading.value || resetSuccess.value) return
    if (!validatePwd(form.newPassword, form.confirmPassword)) return
    loading.value = true
    try {
      await resetForgotPassword({
        username: form.username.trim(),
        smsCode: form.smsCode.trim(),
        // 找回密码属于未登录敏感入口，口令同样走 SM2 传输加密
        newPassword: encryptSm2(form.newPassword),
        confirmPassword: encryptSm2(form.confirmPassword),
      })
      resetSuccess.value = true
      showAuthToast('密码已重置', 'success')
      redirectTimer = setTimeout(() => {
        redirectTimer = null
        navigateToLogin()
      }, 780)
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
    if (redirectTimer) clearTimeout(redirectTimer)
  })

  return {
    step,
    loading,
    resetSuccess,
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
