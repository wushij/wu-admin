import { ref, reactive, computed, onUnmounted } from 'vue'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { getCaptcha, getConfig, sendSmsCode } from '@/api/system/auth'
import type { LoginForm } from '@/types/api'
import { sliderVerifyToRequest } from '@/utils/slider-captcha'
import type { SliderVerifyPayload } from '@/utils/slider-captcha'
import {
  clearLoginRemember,
  loadLoginRemember,
  saveLoginRemember,
} from '@/utils/loginRemember'
import { startSessionServices } from '@/composables/useSessionServices'
import { toCaptchaDataUrl } from '@/utils/captcha'
import { navigateToRegister } from '@/utils/auth-route'
import { logger } from '@/utils/logger'

type CaptchaMode = 'image' | 'slider'
type LoginMode = 'account' | 'sms'
type SliderPurpose = 'login' | 'sms'

export function useLoginForm() {
  const userStore = useUserStore()
  const appStore = useAppStore()

  const captchaEnabled = ref(true)
  const captchaType = ref<CaptchaMode>('image')
  const smsLoginEnabled = ref(false)
  const smsLoginSliderCaptchaEnabled = ref(false)
  const loginMode = ref<LoginMode>('account')
  const smsEnabled = ref(true)
  const rememberMeEnabled = ref(true)
  const registerEnabled = ref(true)
  const showSliderModal = ref(false)
  const sliderPurpose = ref<SliderPurpose>('login')

  const captchaImg = ref('')
  const captchaUuid = ref('')

  const loading = ref(false)
  const sendingSms = ref(false)
  const smsCountdown = ref(0)
  let smsTimer: ReturnType<typeof setInterval> | null = null

  const formData = reactive({
    username: '',
    password: '',
    phone: '',
    code: '',
    rememberMe: false,
  })

  const showLoginModeSwitch = computed(() => smsLoginEnabled.value && smsEnabled.value)

  function parseCaptchaMode(value: string | undefined): CaptchaMode {
    return value === 'slider' ? 'slider' : 'image'
  }

  function switchLoginMode(mode: LoginMode) {
    loginMode.value = mode
    if (mode === 'account') {
      formData.phone = ''
      formData.code = ''
    } else {
      formData.code = ''
    }
  }

  async function loadConfig() {
    try {
      const res = await getConfig()
      const login = res.data?.login
      const register = res.data?.register
      captchaEnabled.value = login?.captchaEnabled !== false
      captchaType.value = parseCaptchaMode(login?.captchaType)
      smsLoginEnabled.value = !!login?.smsLoginEnabled
      smsLoginSliderCaptchaEnabled.value = !!login?.smsLoginSliderCaptchaEnabled
      smsEnabled.value = login?.smsEnabled !== false
      rememberMeEnabled.value = login?.rememberMe !== false
      registerEnabled.value = register?.enabled !== false
    } catch {
      /* 使用默认值 */
    }
  }

  async function refreshCaptcha() {
    if (!captchaEnabled.value || captchaType.value === 'slider') return
    try {
      const res = await getCaptcha('login')
      captchaUuid.value = res.data.uuid
      captchaImg.value = toCaptchaDataUrl(res.data.img || res.data.image)
    } catch {
      captchaImg.value = ''
    }
  }

  function restoreRemember() {
    const saved = loadLoginRemember()
    if (!saved) return
    loginMode.value = saved.mode
    formData.rememberMe = true
    if (saved.username) formData.username = saved.username
    if (saved.password) formData.password = saved.password
    if (saved.phone) formData.phone = saved.phone
  }

  function persistRemember() {
    if (!formData.rememberMe) {
      clearLoginRemember()
      return
    }
    if (loginMode.value === 'account') {
      saveLoginRemember({
        mode: 'account',
        rememberMe: true,
        username: formData.username,
        password: formData.password,
      })
    } else {
      saveLoginRemember({
        mode: 'sms',
        rememberMe: true,
        phone: formData.phone,
      })
    }
  }

  function openSlider(purpose: SliderPurpose) {
    sliderPurpose.value = purpose
    showSliderModal.value = true
  }

  async function onSliderSuccess(payload: SliderVerifyPayload) {
    const slider = sliderVerifyToRequest(payload)
    if (sliderPurpose.value === 'sms') {
      await doSendSms(slider)
      return
    }
    await submitLogin(slider)
  }

  async function doSendSms(slider?: { uuid: string; code: string }) {
    if (sendingSms.value || smsCountdown.value > 0) return
    const phone = formData.phone.trim()
    if (!phone) {
      uni.showToast({ title: '请输入手机号', icon: 'none' })
      return
    }
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    if (!smsEnabled.value) {
      uni.showToast({ title: '短信功能未启用', icon: 'none' })
      return
    }
    sendingSms.value = true
    try {
      await sendSmsCode(formData.phone, slider)
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

  async function handleSendSms() {
    const phone = formData.phone.trim()
    if (!phone) {
      uni.showToast({ title: '请输入手机号', icon: 'none' })
      return
    }
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    if (!smsEnabled.value) {
      uni.showToast({ title: '短信功能未启用', icon: 'none' })
      return
    }
    if (smsLoginSliderCaptchaEnabled.value) {
      openSlider('sms')
      return
    }
    await doSendSms()
  }

  function validateForm(): boolean {
    if (loginMode.value === 'account') {
      if (!formData.username.trim()) {
        uni.showToast({ title: '请输入用户名', icon: 'none' })
        return false
      }
      if (!formData.password) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return false
      }
    } else {
      if (!formData.phone.trim()) {
        uni.showToast({ title: '请输入手机号', icon: 'none' })
        return false
      }
      if (!/^1[3-9]\d{9}$/.test(formData.phone.trim())) {
        uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
        return false
      }
      if (!formData.code.trim()) {
        uni.showToast({ title: '请输入短信验证码', icon: 'none' })
        return false
      }
    }
    return true
  }

  async function submitLogin(sliderCaptcha?: { uuid: string; code: string }) {
    if (!validateForm()) return
    loading.value = true
    try {
      const loginData: LoginForm = {
        rememberMe: formData.rememberMe,
      }
      if (loginMode.value === 'account') {
        loginData.loginType = 'account'
        loginData.username = formData.username.trim()
        loginData.password = formData.password
        if (captchaEnabled.value) {
          if (captchaType.value === 'slider') {
            if (!sliderCaptcha) return
            loginData.uuid = sliderCaptcha.uuid
            loginData.code = sliderCaptcha.code
          } else {
            loginData.uuid = captchaUuid.value
            loginData.code = formData.code
          }
        }
      } else {
        loginData.loginType = 'sms'
        loginData.phone = formData.phone
        loginData.code = formData.code
      }

      await userStore.loginAction(loginData)
      await userStore.getUserInfo()
      startSessionServices()
      persistRemember()
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        uni.switchTab({ url: '/pages/index/index' })
      }, 300)
    } catch (e) {
      logger.error(e)
      if (captchaEnabled.value && captchaType.value === 'image') {
        await refreshCaptcha()
      }
    } finally {
      loading.value = false
    }
  }

  async function handleSubmit() {
    if (loginMode.value === 'account' && captchaEnabled.value && captchaType.value === 'slider') {
      openSlider('login')
      return
    }
    await submitLogin()
  }

  function goRegister() {
    navigateToRegister()
  }

  function goForgotPassword() {
    uni.navigateTo({ url: '/pages/login/forgot-password' })
  }

  onUnmounted(() => {
    if (smsTimer) clearInterval(smsTimer)
  })

  return {
    appStore,
    captchaEnabled,
    captchaType,
    smsLoginEnabled,
    loginMode,
    rememberMeEnabled,
    registerEnabled,
    showSliderModal,
    sliderPurpose,
    captchaImg,
    formData,
    loading,
    sendingSms,
    smsCountdown,
    smsEnabled,
    showLoginModeSwitch,
    loadConfig,
    refreshCaptcha,
    restoreRemember,
    switchLoginMode,
    handleSendSms,
    handleSubmit,
    onSliderSuccess,
    goRegister,
    goForgotPassword,
  }
}
