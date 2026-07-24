import { ref, reactive, computed } from 'vue'
import { getProfile, sendProfileEmailBindCode, bindProfileEmail } from '@/api/system/profile'
import { logger } from '@/utils/logger'
import { useUserStore } from '@/store/user'
import { safeNavigateBack } from '@/utils/navigate-back'

const PROFILE_URL = '/pages-sub/mine/profile'

export function maskBoundEmail(email?: string) {
  const e = (email || '').trim()
  if (!e) return '未绑定'
  const [name, domain] = e.split('@')
  if (!domain) return e
  if (name.length <= 2) return `${name.slice(0, 1)}****@${domain}`
  return `${name.slice(0, 2)}****@${domain}`
}

const form = reactive({
  bindEmail: '',
  bindEmailCode: '',
})

const currentEmail = ref('')
const emailCodeCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null
let initialized = false

export function useEmailBindForm() {
  const loading = ref(false)
  const bindingEmail = ref(false)
  const sendingEmailCode = ref(false)

  const hasBoundEmail = computed(() => /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(currentEmail.value))
  const pageTitle = computed(() => (hasBoundEmail.value ? '更换邮箱' : '绑定邮箱'))

  function startCountdown() {
    emailCodeCountdown.value = 60
    if (countdownTimer) clearInterval(countdownTimer)
    countdownTimer = setInterval(() => {
      emailCodeCountdown.value -= 1
      if (emailCodeCountdown.value <= 0) {
        if (countdownTimer) clearInterval(countdownTimer)
        countdownTimer = null
      }
    }, 1000)
  }

  async function load() {
    if (!initialized) {
      loading.value = true
    }
    try {
      const res = await getProfile()
      currentEmail.value = res.data?.email || ''
      initialized = true
    } catch (e) {
      logger.error(e)
      if (!initialized) {
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
    } finally {
      loading.value = false
    }
  }

  async function sendEmailBindCodeAction() {
    const email = form.bindEmail.trim()
    if (!email) {
      uni.showToast({ title: '请输入新邮箱地址', icon: 'none' })
      return
    }
    if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email)) {
      uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
      return
    }
    if (hasBoundEmail.value && email === currentEmail.value) {
      uni.showToast({ title: '新邮箱不能与原邮箱相同', icon: 'none' })
      return
    }

    sendingEmailCode.value = true
    try {
      await sendProfileEmailBindCode({ email })
      uni.showToast({ title: '验证码已发送至新邮箱', icon: 'success' })
      startCountdown()
    } catch (e) {
      logger.error(e)
    } finally {
      sendingEmailCode.value = false
    }
  }

  async function submit() {
    const email = form.bindEmail.trim()
    const code = form.bindEmailCode.trim()
    if (!email) {
      uni.showToast({ title: '请输入邮箱地址', icon: 'none' })
      return
    }
    if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email)) {
      uni.showToast({ title: '邮箱格式不正确', icon: 'none' })
      return
    }
    if (!code) {
      uni.showToast({ title: '请输入邮箱验证码', icon: 'none' })
      return
    }

    bindingEmail.value = true
    try {
      await bindProfileEmail({ email, code })
      uni.showToast({ title: hasBoundEmail.value ? '邮箱更换成功' : '邮箱绑定成功', icon: 'success' })
      form.bindEmail = ''
      form.bindEmailCode = ''
      emailCodeCountdown.value = 0
      if (countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
      await useUserStore().getUserInfo().catch((e) => logger.warn('同步用户信息失败', e))
      setTimeout(() => {
        safeNavigateBack(PROFILE_URL)
      }, 400)
    } catch (e) {
      logger.error(e)
    } finally {
      bindingEmail.value = false
    }
  }

  return {
    loading,
    bindingEmail,
    sendingEmailCode,
    emailCodeCountdown,
    currentEmail,
    form,
    hasBoundEmail,
    pageTitle,
    load,
    sendEmailBindCodeAction,
    submit,
  }
}
