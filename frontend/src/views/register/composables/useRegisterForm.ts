import { ref, reactive, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { useSiteStore } from '@/store/site'
import { getCaptcha, register, getConfig } from '@/api/system/auth'
import type { RegisterForm } from '@/types/api'
import { getErrorMessage } from '@/utils/axiosError'
import { sliderVerifyToRequest } from '@/types/slider-captcha'
import type { SliderVerifyPayload } from '@/types/slider-captcha'
import { getSecurityConfig } from '@/utils/security-config'
import { encryptSm2 } from '@/utils/crypto'

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

  function showAgreeAlert() {
    agreeRowAlert.value = true
    ElMessage.warning('请先阅读并同意《用户协议》和《隐私政策》')
  }

  watch(agreeTerms, (checked) => {
    if (checked) {
      agreeRowAlert.value = false
    }
  })

  function validateRegisterForm(): boolean {
    const username = (formData.username || '').trim()
    formData.username = username

    if (!username) {
      ElMessage.warning('请输入用户名')
      return false
    }
    if (!/^[a-zA-Z0-9_]{4,12}$/.test(username)) {
      ElMessage.warning('用户名只能包含字母、数字、下划线，长度4-12位')
      return false
    }
    if (!formData.password) {
      ElMessage.warning('请输入密码')
      return false
    }
    if (formData.password.length < minPasswordLength.value || formData.password.length > 32) {
      ElMessage.warning(`密码长度应为 ${minPasswordLength.value}-32 位`)
      return false
    }
    if (!formData.confirmPassword) {
      ElMessage.warning('请再次输入密码确认')
      return false
    }
    if (formData.confirmPassword !== formData.password) {
      ElMessage.warning('两次输入的密码不一致')
      return false
    }
    if (captchaEnabled.value && captchaType.value === 'image' && !(formData.code || '').trim()) {
      ElMessage.warning('请完成图形验证码')
      return false
    }
    return true
  }

  async function handleRegister() {
    if (!agreeTerms.value) {
      showAgreeAlert()
      return
    }
    if (loading.value) return
    if (!validateRegisterForm()) return

    if (captchaEnabled.value && captchaType.value === 'slider') {
      showSliderModal.value = true
      return
    }
    await doRegister()
  }

  async function doRegister(sliderCaptcha?: { uuid: string; code: string }) {
    loading.value = true
    try {
      const secConfig = getSecurityConfig()
      const encryptedPassword = secConfig.sm2EncryptEnabled && secConfig.sm2PublicKey
        ? encryptSm2(formData.password, secConfig.sm2PublicKey)
        : formData.password
      const registerData: RegisterForm = {
        username: formData.username,
        password: encryptedPassword,
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
      const needAudit = res.data === true || (typeof res.message === 'string' && res.message.includes('审核'))
      if (needAudit) {
        try {
          await ElMessageBox.alert(
            '您的账号已成功提交注册，当前需要等待管理员审核通过后方可登录。请耐心等待！',
            '注册成功（待审核）',
            {
              confirmButtonText: '我知道了，去登录',
              type: 'warning',
              customClass: 'register-audit-alert-box',
              showClose: false,
              closeOnClickModal: false,
              closeOnPressEscape: false,
            }
          )
        } catch {
          // 用户通过任意非确定方式关闭弹窗，均按知晓处理并正常前往登录页
        }
        router.push('/login')
      } else {
        ElMessage.success(res.message || res.msg || '注册成功，请登录')
        router.push('/login')
      }
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
  })

  return {
    sitePlatformName,
    sitePlatformSubtitle,
    siteRegisterTitle,
    formRef,
    formData,
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
