import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getCaptcha, getConfig, sendSmsCode } from '@/api/system/auth'
import type { LoginForm } from '@/types/api'
import { getErrorMessage } from '@/utils/axiosError'
import {
  clearLoginRemember,
  loadLoginRemember,
  saveLoginRemember,
} from '@/utils/loginRemember'

type CaptchaMode = 'image' | 'slider'
type LoginMode = 'account' | 'sms'
type SliderPurpose = 'login' | 'sms'

interface LoginFormModel {
  username: string
  password: string
  phone: string
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
  const loginMode = ref<LoginMode>('account')
  const smsEnabled = ref(true)
  const rememberMeEnabled = ref(true)
  const registerEnabled = ref(true)
  const showSliderModal = ref(false)
  const sliderPurpose = ref<SliderPurpose>('login')
  const sitePlatformName = ref('Admin Platform')
  const sitePlatformSubtitle = ref('统一运维 · 高效管控')
  const siteLoginWelcome = ref('Welcome')

  const captchaImg = ref('')
  const captchaUuid = ref('')

  const formRef = ref<FormInstance | null>(null)
  const loading = ref(false)
  const submitAttempted = ref(false)
  const sendingSms = ref(false)
  const smsCountdown = ref(0)
  let smsTimer: ReturnType<typeof setInterval> | null = null

  const formData = reactive<LoginFormModel>({
    username: '',
    password: '',
    phone: '',
    code: '',
    rememberMe: false,
  })

  const formRules = ref<FormRules>({})

  const showLoginModeSwitch = computed(() => smsLoginEnabled.value && smsEnabled.value)

  function parseCaptchaMode(value: string | undefined): CaptchaMode {
    return value === 'slider' ? 'slider' : 'image'
  }

  function switchLoginMode(mode: LoginMode) {
    if (loginMode.value === mode) return
    loginMode.value = mode
    submitAttempted.value = false
    if (mode === 'account') {
      formData.phone = ''
      formData.code = ''
    } else {
      formData.code = ''
    }
    rebuildFormRules()
    nextTick(() => formRef.value?.clearValidate())
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
        rememberMeEnabled.value = config.login.rememberMe !== false
        smsEnabled.value = config.login.smsEnabled !== false
      }
      if (config.register) {
        registerEnabled.value = config.register.enabled !== false
      }
      if (config.site) {
        if (config.site.platformName) sitePlatformName.value = config.site.platformName
        if (config.site.platformSubtitle) sitePlatformSubtitle.value = config.site.platformSubtitle
        if (config.site.loginWelcome) siteLoginWelcome.value = config.site.loginWelcome
      }
      rebuildFormRules()
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

  function rebuildFormRules() {
    const next: FormRules = {}
    if (loginMode.value === 'account') {
      next.username = [{ required: true, message: '请输入用户名', trigger: 'blur' }]
      next.password = [{ required: true, message: '请输入密码', trigger: 'blur' }]
      if (captchaEnabled.value && captchaType.value === 'image') {
        next.code = [{ required: true, message: '请输入验证码', trigger: 'blur' }]
      }
    } else if (loginMode.value === 'sms') {
      next.phone = [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
      ]
      next.code = [{ required: true, message: '请输入短信验证码', trigger: 'blur' }]
    }
    formRules.value = next
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
    }
  }

  async function handleSendSmsCode() {
    if (!formRef.value || sendingSms.value || smsCountdown.value > 0) return
    formData.phone = (formData.phone || '').trim()
    if (!formData.phone) {
      ElMessage.warning('请输入手机号')
      return
    }
    try {
      await formRef.value.validateField('phone')
    } catch {
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

  async function doSendSmsCode() {
    if (sendingSms.value || smsCountdown.value > 0) return
    sendingSms.value = true
    try {
      const sliderCode = smsLoginSliderCaptchaEnabled.value ? 'slider_verified' : undefined
      await sendSmsCode(formData.phone, sliderCode)
      ElMessage.success('验证码已发送至绑定手机号')
      startSmsCountdown()
    } catch (error) {
      ElMessage.error(getErrorMessage(error) || '发送失败')
    } finally {
      sendingSms.value = false
    }
  }

  function onSliderSuccess() {
    if (sliderPurpose.value === 'sms') {
      void doSendSmsCode()
    } else {
      void doLogin()
    }
  }

  async function handleLogin() {
    if (!formRef.value) return
    submitAttempted.value = true
    syncAutofillFromDom()
    formData.username = (formData.username || '').trim()

    try {
      await formRef.value.validate()
    } catch {
      return
    }

    if (loginMode.value === 'account' && captchaEnabled.value && captchaType.value === 'slider') {
      sliderPurpose.value = 'login'
      showSliderModal.value = true
      return
    }
    await doLogin()
  }

  async function doLogin() {
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
      } else {
        loginData = {
          loginType: 'account',
          username: formData.username,
          password: formData.password,
          rememberMe: formData.rememberMe,
        }
        if (captchaEnabled.value && captchaType.value === 'slider') {
          loginData.code = 'slider_verified'
        } else if (captchaEnabled.value && captchaType.value === 'image') {
          loginData.uuid = captchaUuid.value
          loginData.code = formData.code
        }
      }
      const result = await userStore.loginAction(loginData)
      if (result.code === 200) {
        persistLoginRemember()
        ElMessage.success('登录成功')
        setTimeout(() => router.push('/'), 500)
      } else {
        ElMessage.error(result.msg || result.message || '登录失败')
        refreshCaptchaAfterFail()
      }
    } catch (error) {
      console.error('登录失败', error)
      const errorMessage = getErrorMessage(error)
      if (!errorMessage || errorMessage.includes('status code')) {
        ElMessage.error('登录失败，请检查网络连接')
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
    if (loginMode.value === 'sms') {
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
    rebuildFormRules()
    await nextTick()
    syncAutofillFromDom()
    formRef.value?.clearValidate()
  })

  onUnmounted(() => {
    if (smsTimer) {
      clearInterval(smsTimer)
      smsTimer = null
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
    formRules,
    submitAttempted,
    captchaEnabled,
    captchaType,
    captchaImg,
    smsEnabled,
    rememberMeEnabled,
    registerEnabled,
    sendingSms,
    smsCountdown,
    loading,
    showSliderModal,
    loadCaptcha,
    handleSendSmsCode,
    handleLogin,
    onSliderSuccess,
    goRegister,
  }
}
