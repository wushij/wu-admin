import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getCaptcha, register, getConfig } from '@/api/system/auth'
import type { RegisterForm } from '@/types/api'
import { getErrorMessage } from '@/utils/axiosError'
import { sliderVerifyToRequest } from '@/types/slider-captcha'
import type { SliderVerifyPayload } from '@/types/slider-captcha'

type CaptchaMode = 'image' | 'slider'

interface RegisterFormModel {
  username: string
  password: string
  confirmPassword: string
  nickname: string
  code: string
}

export function useRegisterForm() {
  const router = useRouter()
  const showSliderModal = ref(false)

  const captchaEnabled = ref(true)
  const captchaType = ref<CaptchaMode>('image')
  const minPasswordLength = ref(6)
  const sitePlatformName = ref('Admin Platform')
  const sitePlatformSubtitle = ref('统一运维 · 高效管控')
  const siteRegisterTitle = ref('Sign Up')
  const agreeTerms = ref(false)
  const agreeRowAlert = ref(false)

  const captchaImg = ref('')
  const captchaUuid = ref('')

  const formRef = ref<FormInstance | null>(null)
  const loading = ref(false)
  const submitAttempted = ref(false)

  const formData = reactive<RegisterFormModel>({
    username: '',
    password: '',
    confirmPassword: '',
    nickname: '',
    code: '',
  })

  function parseCaptchaMode(value: string | undefined): CaptchaMode {
    return value === 'slider' ? 'slider' : 'image'
  }

  const validateConfirmPassword = (
    _rule: unknown,
    value: string,
    callback: (error?: Error) => void,
  ) => {
    if (value !== formData.password) {
      callback(new Error('两次输入的密码不一致'))
    } else {
      callback()
    }
  }

  const rules = computed<FormRules>(() => {
    const base: FormRules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        {
          pattern: /^[a-zA-Z0-9_]{4,12}$/,
          message: '用户名只能包含字母、数字、下划线，长度4-12位',
          trigger: 'blur',
        },
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        {
          min: minPasswordLength.value,
          max: 32,
          message: `密码长度至少 ${minPasswordLength.value} 位`,
          trigger: 'blur',
        },
      ],
      confirmPassword: [
        { required: true, message: '请确认密码', trigger: 'blur' },
        { validator: validateConfirmPassword, trigger: 'blur' },
      ],
    }
    if (captchaEnabled.value && captchaType.value === 'image') {
      base.code = [{ required: true, message: '请输入验证码', trigger: 'blur' }]
    }
    return base
  })

  async function loadConfig() {
    try {
      const res = await getConfig()
      const config = res.data
      if (!config) return

      if (config.register) {
        if (config.register.captchaEnabled !== undefined) {
          captchaEnabled.value = config.register.captchaEnabled !== false
        }
        if (config.register.captchaType) {
          captchaType.value = parseCaptchaMode(config.register.captchaType)
        }
        if (config.register.minPasswordLength) {
          minPasswordLength.value = Number(config.register.minPasswordLength) || 6
        }
        if (config.register.enabled === false) {
          ElMessage.warning('系统暂未开放注册')
          router.push('/login')
        }
      }
      if (config.site) {
        if (config.site.platformName) sitePlatformName.value = config.site.platformName
        if (config.site.platformSubtitle) sitePlatformSubtitle.value = config.site.platformSubtitle
        if (config.site.registerTitle) siteRegisterTitle.value = config.site.registerTitle
      }
    } catch (error) {
      console.error('加载配置失败', error)
    }
  }

  async function loadCaptcha() {
    try {
      const res = await getCaptcha('register')
      const img = res.data.img ?? res.data.image
      if (img) {
        captchaImg.value = `data:image/png;base64,${img}`
        captchaUuid.value = res.data.uuid
      }
    } catch (error) {
      console.error('获取验证码失败', error)
    }
  }

  function bumpAgreeRow() {
    agreeRowAlert.value = false
    nextTick(() => {
      agreeRowAlert.value = true
      window.setTimeout(() => {
        agreeRowAlert.value = false
      }, 540)
    })
  }

  async function handleRegister() {
    if (!agreeTerms.value) {
      bumpAgreeRow()
      return
    }
    if (!formRef.value) return

    submitAttempted.value = true
    formData.username = (formData.username || '').trim()

    try {
      await formRef.value.validate()
    } catch {
      return
    }

    if (captchaEnabled.value && captchaType.value === 'slider') {
      showSliderModal.value = true
      return
    }
    await doRegister()
  }

  async function doRegister(sliderCaptcha?: { uuid: string; code: string }) {
    loading.value = true
    try {
      const registerData: RegisterForm = {
        username: formData.username,
        password: formData.password,
        nickname: formData.nickname || undefined,
      }

      if (captchaEnabled.value) {
        if (captchaType.value === 'slider') {
          if (!sliderCaptcha) return
          registerData.uuid = sliderCaptcha.uuid
          registerData.code = sliderCaptcha.code
        } else {
          registerData.uuid = captchaUuid.value
          registerData.code = formData.code
        }
      }

      const res = await register(registerData)
      ElMessage.success(res.message || res.msg || '注册成功，请登录')
      router.push('/login')
    } catch (error) {
      console.error('注册失败', error)
      const errorMessage = getErrorMessage(error)
      if (errorMessage && !errorMessage.includes('status code')) {
        ElMessage.error(errorMessage)
      }
      if (captchaEnabled.value && captchaType.value === 'image') {
        loadCaptcha()
        formData.code = ''
      }
    } finally {
      loading.value = false
    }
  }

  function onSliderSuccess(payload: SliderVerifyPayload) {
    void doRegister(sliderVerifyToRequest(payload))
  }

  function goLogin() {
    router.push('/login')
  }

  onMounted(async () => {
    await loadConfig()
    if (captchaEnabled.value && captchaType.value === 'image') {
      loadCaptcha()
    }
    await nextTick()
    formRef.value?.clearValidate()
  })

  return {
    sitePlatformName,
    sitePlatformSubtitle,
    siteRegisterTitle,
    formRef,
    formData,
    rules,
    submitAttempted,
    captchaEnabled,
    captchaType,
    captchaImg,
    agreeTerms,
    agreeRowAlert,
    loading,
    showSliderModal,
    onSliderSuccess,
    loadCaptcha,
    handleRegister,
    doRegister,
    goLogin,
  }
}
