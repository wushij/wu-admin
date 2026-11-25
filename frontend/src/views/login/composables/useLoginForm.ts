import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useSiteStore } from '@/store/site'
import { getCaptcha, getConfig, sendSmsCode, sendEmailCode } from '@/api/system/auth'
import type { LoginForm } from '@/types/api'
import { getErrorMessage, isErrorToastShown } from '@/utils/axiosError'
import { isApiSuccessCode } from '@/core/http/api-response'
import { sliderVerifyToRequest } from '@/types/slider-captcha'
import type { SliderVerifyPayload } from '@/types/slider-captcha'
import {
  clearLoginRemember,
  loadLoginRemember,
  saveLoginRemember,
} from '@/utils/loginRemember'
import { getSecurityConfig } from '@/utils/security-config'
import { encryptSm2 } from '@/utils/crypto'

type CaptchaMode = 'image' | 'slider'
type LoginMode = 'account' | 'sms' | 'email'
type SliderPurpose = 'login' | 'sms' | 'email'

interface LoginFormModel {
  username: string
  password: string
  phone: string
  email: string
  code: string
  rememberMe: boolean
}

export function useLoginForm() {
  const router = useRouter()
  const userStore = useUserStore()

  const captchaEnabled = ref(true)
  const captchaType = ref<CaptchaMode>('image')
  const smsLoginEnabled = ref(false)
  const smsLoginSliderCaptchaEnabled = ref(false)
  const emailLoginEnabled = ref(false)
  const emailLoginSliderCaptchaEnabled = ref(false)
  const loginMode = ref<LoginMode>('account')
  const smsEnabled = ref(true)
  const emailEnabled = ref(true)
  const rememberMeEnabled = ref(true)
  const registerEnabled = ref(true)
  const showSliderModal = ref(false)
  const sliderPurpose = ref<SliderPurpose>('login')
  const sitePlatformName = ref('Admin Platform')
  const sitePlatformSubtitle = ref('统一运维 · 高效管控')
  const siteLoginWelcome = ref('Welcome')

  const captchaImg = ref('')
  const captchaUuid = ref('')

  const PHONE_REGEX = /^1[3-9]\d{9}$/
  const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/

  const formRef = ref<FormInstance | null>(null)
  const loading = ref(false)
  const loginSuccess = ref(false)
  const sendingSms = ref(false)
  const sendingEmail = ref(false)
  const smsCountdown = ref(0)
  const emailCountdown = ref(0)
  let smsTimer: ReturnType<typeof setInterval> | null = null
  let emailTimer: ReturnType<typeof setInterval> | null = null
  let redirectTimer: ReturnType<typeof setTimeout> | null = null

  const formData = reactive<LoginFormModel>({
    username: '',
    password: '',
    phone: '',
    email: '',
    code: '',
    rememberMe: false,
  })

  const showLoginModeSwitch = computed(
    () => (smsLoginEnabled.value && smsEnabled.value) || (emailLoginEnabled.value && emailEnabled.value),
  )

  function parseCaptchaMode(value: string | undefined): CaptchaMode {
    return value === 'slider' ? 'slider' : 'image'
  }

  function switchLoginMode(mode: LoginMode) {
    if (loginMode.value === mode) return
    loginMode.value = mode
    formData.phone = ''
    formData.email = ''
    formData.code = ''
  }

  function startSmsCountdown(seconds = 60) {
    if (smsTimer) {
      clearInterval(smsTimer)
      smsTimer = null
    }
    smsCountdown.value = seconds
    smsTimer = setInterval(() => {
      if (smsCountdown.value <= 1) {
        smsCountdown.value = 0
        if (smsTimer) {
          clearInterval(smsTimer)
          smsTimer = null
        }
      } else {
        smsCountdown.value -= 1
      }
    }, 1000)
  }

  function startEmailCountdown(seconds = 60) {
    if (emailTimer) {
      clearInterval(emailTimer)
      emailTimer = null
    }
    emailCountdown.value = seconds
    emailTimer = setInterval(() => {
      if (emailCountdown.value <= 1) {
        emailCountdown.value = 0
        if (emailTimer) {
          clearInterval(emailTimer)
          emailTimer = null
        }
      } else {
        emailCountdown.value -= 1
      }
    }, 1000)
  }

  function restoreLoginRemember() {
    if (!rememberMeEnabled.value) return
    const saved = loadLoginRemember()
    if (!saved) return

    // 仅恢复账号登录；历史误存的短信记录清掉
    if (saved.mode !== 'account') {
      clearLoginRemember()
      return
    }

    formData.rememberMe = true
    loginMode.value = 'account'
    if (saved.username) formData.username = saved.username
    if (saved.password) formData.password = saved.password
  }

  function persistLoginRemember() {
    if (
      !rememberMeEnabled.value
      || !formData.rememberMe
      || loginMode.value !== 'account'
    ) {
      clearLoginRemember()
      return
    }
    saveLoginRemember({
      mode: 'account',
      rememberMe: true,
      username: formData.username.trim(),
      password: formData.password,
    })
  }

  async function loadConfig() {
    try {
      const res = await getConfig()
      const config = res.data
      if (!config) return

      if (config.login) {
        captchaEnabled.value = config.login.captchaEnabled !== false
        captchaType.value = parseCaptchaMode(config.login.captchaType)
        smsLoginEnabled.value = config.login.smsLoginEnabled === true
        smsLoginSliderCaptchaEnabled.value = config.login.smsLoginSliderCaptchaEnabled === true
        emailLoginEnabled.value = config.login.emailLoginEnabled === true && config.login.emailEnabled !== false
        emailLoginSliderCaptchaEnabled.value = config.login.emailLoginSliderCaptchaEnabled === true
        rememberMeEnabled.value = config.login.rememberMe !== false
        smsEnabled.value = config.login.smsEnabled !== false
        emailEnabled.value = config.login.emailEnabled !== false
      }
      if (config.register) {
        registerEnabled.value = config.register.enabled !== false
      }
      if (config.site) {
        if (config.site.platformName) sitePlatformName.value = config.site.platformName
        if (config.site.platformSubtitle) sitePlatformSubtitle.value = config.site.platformSubtitle
        if (config.site.loginWelcome) siteLoginWelcome.value = config.site.loginWelcome
        const siteStore = useSiteStore()
        siteStore.siteConfig = {
          ...siteStore.siteConfig,
          ...config.site,
        }
      }
    } catch (error) {
      console.error('加载配置失败', error)
    }
  }

  async function loadCaptcha() {
    try {
      const res = await getCaptcha()
      const img = res.data.img ?? res.data.image
      if (img) {
        captchaImg.value = `data:image/png;base64,${img}`
        captchaUuid.value = res.data.uuid
      }
    } catch (error) {
      console.error('获取验证码失败', error)
    }
  }

  function syncAutofillFromDom() {
    const root = formRef.value?.$el as HTMLElement | undefined
    if (!root) return
    if (loginMode.value === 'account') {
      const inputs = Array.from(root.querySelectorAll<HTMLInputElement>('input.el-input__inner'))
      if (inputs[0]?.value) formData.username = inputs[0].value.trim()
      if (inputs[1]?.value) formData.password = inputs[1].value
      if (inputs[2]?.value && captchaEnabled.value && captchaType.value === 'image') {
        formData.code = inputs[2].value.trim()
      }
    } else if (loginMode.value === 'sms') {
      const phoneInput = root.querySelector<HTMLInputElement>('input[placeholder="请输入绑定的手机号"]')
      const codeInput = root.querySelector<HTMLInputElement>('input[placeholder="请输入验证码"]')
      if (phoneInput?.value) formData.phone = phoneInput.value.trim()
      if (codeInput?.value) formData.code = codeInput.value.trim()
    } else if (loginMode.value === 'email') {
      const emailInput = root.querySelector<HTMLInputElement>('input[placeholder="请输入绑定的邮箱地址"]')
      const codeInput = root.querySelector<HTMLInputElement>('input[placeholder="请输入验证码"]')
      if (emailInput?.value) formData.email = emailInput.value.trim()
      if (codeInput?.value) formData.code = codeInput.value.trim()
    }
  }

  async function handleSendSmsCode() {
    if (sendingSms.value || smsCountdown.value > 0) return
    formData.phone = (formData.phone || '').trim()
    if (!formData.phone) {
      ElMessage.warning('请输入手机号')
      return
    }
    if (!PHONE_REGEX.test(formData.phone)) {
      ElMessage.warning('请输入正确的手机号')
      return
    }
    if (!smsEnabled.value) {
      ElMessage.warning('短信功能未启用')
      return
    }
    if (smsLoginSliderCaptchaEnabled.value) {
      sliderPurpose.value = 'sms'
      showSliderModal.value = true
      return
    }
    await doSendSmsCode()
  }

  async function doSendSmsCode(slider?: { uuid: string; code: string }) {
    if (sendingSms.value || smsCountdown.value > 0) return
    sendingSms.value = true
    try {
      await sendSmsCode(formData.phone, slider)
      ElMessage.success('验证码已发送至绑定手机号')
      startSmsCountdown()
    } catch (error) {
      ElMessage.error(getErrorMessage(error) || '发送失败')
    } finally {
      sendingSms.value = false
    }
  }

  async function handleSendEmailCode() {
    if (sendingEmail.value || emailCountdown.value > 0) return
    formData.email = (formData.email || '').trim()
    if (!formData.email) {
      ElMessage.warning('请输入邮箱地址')
      return
    }
    if (!EMAIL_REGEX.test(formData.email)) {
      ElMessage.warning('请输入正确的邮箱地址')
      return
    }
    if (!emailEnabled.value) {
      ElMessage.warning('邮件功能未启用')
      return
    }
    if (emailLoginSliderCaptchaEnabled.value) {
      sliderPurpose.value = 'email'
      showSliderModal.value = true
      return
    }
    await doSendEmailCode()
  }

  async function doSendEmailCode(slider?: { uuid: string; code: string }) {
    if (sendingEmail.value || emailCountdown.value > 0) return
    sendingEmail.value = true
    try {
      await sendEmailCode(formData.email, slider)
      ElMessage.success('验证码已发送至绑定邮箱')
      startEmailCountdown()
    } catch (error) {
      ElMessage.error(getErrorMessage(error) || '发送失败')
    } finally {
      sendingEmail.value = false
    }
  }

  function onSliderSuccess(payload: SliderVerifyPayload) {
    const slider = sliderVerifyToRequest(payload)
    if (sliderPurpose.value === 'sms') {
      void doSendSmsCode(slider)
    } else if (sliderPurpose.value === 'email') {
      void doSendEmailCode(slider)
    } else {
      void doLogin(slider)
    }
  }

  function validateLoginForm(): boolean {
    if (loginMode.value === 'sms') {
      const phone = (formData.phone || '').trim()
      formData.phone = phone
      if (!phone) {
        ElMessage.warning('请输入手机号')
        return false
      }
      if (!PHONE_REGEX.test(phone)) {
        ElMessage.warning('请输入正确的手机号')
        return false
      }
      if (!(formData.code || '').trim()) {
        ElMessage.warning('请输入短信验证码')
        return false
      }
      return true
    }

    if (loginMode.value === 'email') {
      const email = (formData.email || '').trim()
      formData.email = email
      if (!email) {
        ElMessage.warning('请输入邮箱地址')
        return false
      }
      if (!EMAIL_REGEX.test(email)) {
        ElMessage.warning('请输入正确的邮箱地址')
        return false
      }
      if (!(formData.code || '').trim()) {
        ElMessage.warning('请输入邮箱验证码')
        return false
      }
      return true
    }

    if (!formData.username) {
      ElMessage.warning('请输入用户名')
      return false
    }
    if (!formData.password) {
      ElMessage.warning('请输入密码')
      return false
    }
    if (captchaEnabled.value && captchaType.value === 'image' && !(formData.code || '').trim()) {
      ElMessage.warning('请完成图形验证码')
      return false
    }
    return true
  }

  async function handleLogin() {
    if (loading.value || loginSuccess.value) return
    syncAutofillFromDom()
    formData.username = (formData.username || '').trim()

    if (!validateLoginForm()) return

    if (loginMode.value === 'account' && captchaEnabled.value && captchaType.value === 'slider') {
      sliderPurpose.value = 'login'
      showSliderModal.value = true
      return
    }
    await doLogin()
  }

  async function doLogin(sliderCaptcha?: { uuid: string; code: string }) {
    if (loading.value || loginSuccess.value) return
    loginSuccess.value = false
    loading.value = true
    try {
      let loginData: LoginForm
      if (loginMode.value === 'sms') {
        loginData = {
          loginType: 'sms',
          phone: formData.phone.trim(),
          code: formData.code.trim(),
          rememberMe: formData.rememberMe,
        }
      } else if (loginMode.value === 'email') {
        loginData = {
          loginType: 'email',
          email: formData.email.trim(),
          emailCode: formData.code.trim(),
          rememberMe: formData.rememberMe,
        }
      } else {
        const secConfig = getSecurityConfig()
        const encryptedPassword = secConfig.sm2EncryptEnabled && secConfig.sm2PublicKey
          ? encryptSm2(formData.password, secConfig.sm2PublicKey)
          : formData.password
        loginData = {
          loginType: 'account',
          username: formData.username,
          password: encryptedPassword,
          rememberMe: formData.rememberMe,
        }
        if (captchaEnabled.value && captchaType.value === 'slider') {
          if (!sliderCaptcha) return
          loginData.uuid = sliderCaptcha.uuid
          loginData.code = sliderCaptcha.code
        } else if (captchaEnabled.value && captchaType.value === 'image') {
          loginData.uuid = captchaUuid.value
          loginData.code = formData.code
        }
      }
      const result = await userStore.loginAction(loginData)
      if (isApiSuccessCode(result.code)) {
        // 必须等待用户信息与菜单树拉取完成，再进入成功打勾态，避免跳转守卫失败退回登录页却卡死在“已打勾”
        try {
          await userStore.getUserInfo()
        } catch (infoError) {
          userStore.logout()
          throw infoError
        }
        persistLoginRemember()
        loginSuccess.value = true
        ElMessage.success({
          message: '登录成功',
          customClass: 'auth-glass-message',
        })
        if (redirectTimer) clearTimeout(redirectTimer)
        // 预留 680ms 让胶囊向心收拢与 SVG 一笔勾勒动效完整呈现后再跳转
        redirectTimer = setTimeout(async () => {
          try {
            await router.push('/')
          } finally {
            if (router.currentRoute?.value?.path === '/login') {
              loginSuccess.value = false
            }
          }
        }, 680)
      } else {
        loginSuccess.value = false
        ElMessage.error(result.msg || result.message || '登录失败')
        refreshCaptchaAfterFail()
      }
    } catch (error) {
      console.error('登录失败', error)
      loginSuccess.value = false
      if (!isErrorToastShown(error)) {
        const errorMessage = getErrorMessage(error)
        ElMessage.error(
          !errorMessage || errorMessage.includes('status code')
            ? '登录失败，请检查服务或网络连接'
            : errorMessage,
        )
      }
      refreshCaptchaAfterFail()
    } finally {
      loading.value = false
    }
  }

  function refreshCaptchaAfterFail() {
    if (loginMode.value === 'account' && captchaEnabled.value && captchaType.value === 'image') {
      loadCaptcha()
      formData.code = ''
    }
    if (loginMode.value === 'sms' || loginMode.value === 'email') {
      formData.code = ''
    }
  }

  function goRegister() {
    router.push('/register')
  }

  onMounted(async () => {
    await loadConfig()
    restoreLoginRemember()
    if (loginMode.value === 'account' && captchaEnabled.value && captchaType.value === 'image') {
      loadCaptcha()
    }
    await nextTick()
    syncAutofillFromDom()
  })

  onUnmounted(() => {
    if (smsTimer) {
      clearInterval(smsTimer)
      smsTimer = null
    }
    if (emailTimer) {
      clearInterval(emailTimer)
      emailTimer = null
    }
    if (redirectTimer) {
      clearTimeout(redirectTimer)
      redirectTimer = null
    }
  })

  return {
    sitePlatformName,
    sitePlatformSubtitle,
    siteLoginWelcome,
    showLoginModeSwitch,
    loginMode,
    switchLoginMode,
    formRef,
    formData,
    captchaEnabled,
    captchaType,
    captchaImg,
    smsEnabled,
    emailEnabled,
    smsLoginEnabled,
    emailLoginEnabled,
    rememberMeEnabled,
    registerEnabled,
    sendingSms,
    sendingEmail,
    smsCountdown,
    emailCountdown,
    loading,
    loginSuccess,
    showSliderModal,
    sliderPurpose,
    loadCaptcha,
    handleSendSmsCode,
    handleSendEmailCode,
    handleLogin,
    onSliderSuccess,
    goRegister,
  }
}
