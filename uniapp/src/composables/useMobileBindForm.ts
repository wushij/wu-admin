import { ref, reactive, computed, onUnmounted } from 'vue'
import {
  getProfile,
  sendProfileMobileBindSmsCode,
  bindProfileMobile,
} from '@/api/system/profile'
import { getConfig } from '@/api/system/auth'
import { logger } from '@/utils/logger'
import { useUserStore } from '@/store/user'
import { sliderVerifyToRequest } from '@/utils/slider-captcha'
import type { SliderVerifyPayload } from '@/utils/slider-captcha'
import { safeNavigateBack } from '@/utils/navigate-back'

const PROFILE_URL = '/pages-sub/mine/profile'

export function maskBoundMobile(mobile?: string) {
  const m = (mobile || '').trim()
  if (!/^1[3-9]\d{9}$/.test(m)) return m || '未绑定'
  return `${m.slice(0, 3)} **** ${m.slice(-4)}`
}

const form = reactive({
  bindMobile: '',
  bindSmsCode: '',
})

const currentMobile = ref('')
const bindSmsCountdown = ref(0)
let bindSmsTimer: ReturnType<typeof setInterval> | null = null
let initialized = false

export function useMobileBindForm() {
  const loading = ref(false)
  const bindingMobile = ref(false)
  const sendingBindSms = ref(false)
  const smsEnabled = ref(false)
  const showSlider = ref(false)

  const hasBoundMobile = computed(() => /^1[3-9]\d{9}$/.test(currentMobile.value.trim()))
  const pageTitle = computed(() => (hasBoundMobile.value ? '更换手机号' : '绑定手机号'))

  async function load() {
    if (!initialized) {
      loading.value = true
    }
    try {
      const [cfgRes, profileRes] = await Promise.all([getConfig(), getProfile()])
      smsEnabled.value = cfgRes.data?.login?.smsEnabled === true
      currentMobile.value = profileRes.data?.mobile || ''
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

  function startBindSmsCountdown() {
    bindSmsCountdown.value = 60
    if (bindSmsTimer) clearInterval(bindSmsTimer)
    bindSmsTimer = setInterval(() => {
      bindSmsCountdown.value -= 1
      if (bindSmsCountdown.value <= 0 && bindSmsTimer) {
        clearInterval(bindSmsTimer)
        bindSmsTimer = null
      }
    }, 1000)
  }

  async function doSendBindSmsCode(slider: { uuid: string; code: string }) {
    const mobile = form.bindMobile.trim()
    if (!/^1[3-9]\d{9}$/.test(mobile)) {
      uni.showToast({ title: '请输入正确手机号', icon: 'none' })
      return
    }
    sendingBindSms.value = true
    try {
      await sendProfileMobileBindSmsCode({ mobile, ...slider })
      uni.showToast({ title: '验证码已发送', icon: 'success' })
      startBindSmsCountdown()
    } catch (e) {
      logger.error(e)
    } finally {
      sendingBindSms.value = false
    }
  }

  function sendBindSmsCode() {
    const mobile = form.bindMobile.trim()
    if (!/^1[3-9]\d{9}$/.test(mobile)) {
      uni.showToast({ title: '请输入正确手机号', icon: 'none' })
      return
    }
    showSlider.value = true
  }

  async function onSliderSuccess(payload: SliderVerifyPayload) {
    await doSendBindSmsCode(sliderVerifyToRequest(payload))
  }

  async function submit() {
    if (!smsEnabled.value) {
      uni.showToast({ title: '短信功能未启用', icon: 'none' })
      return
    }
    const mobile = form.bindMobile.trim()
    const smsCode = form.bindSmsCode.trim()
    if (!mobile || !smsCode) {
      uni.showToast({ title: '请填写手机号和验证码', icon: 'none' })
      return
    }
    bindingMobile.value = true
    const isChange = hasBoundMobile.value
    try {
      await bindProfileMobile({ mobile, smsCode })
      form.bindMobile = ''
      form.bindSmsCode = ''
      bindSmsCountdown.value = 0
      if (bindSmsTimer) {
        clearInterval(bindSmsTimer)
        bindSmsTimer = null
      }
      await useUserStore().getUserInfo().catch((e) => logger.warn('同步用户信息失败', e))
      uni.showToast({
        title: isChange ? '手机号已更换' : '绑定成功',
        icon: 'success',
      })
      setTimeout(() => {
        safeNavigateBack(PROFILE_URL)
      }, 400)
    } catch (e) {
      logger.error(e)
    } finally {
      bindingMobile.value = false
    }
  }

  onUnmounted(() => {
    if (bindSmsTimer) clearInterval(bindSmsTimer)
  })

  return {
    loading,
    bindingMobile,
    sendingBindSms,
    bindSmsCountdown,
    smsEnabled,
    currentMobile,
    form,
    hasBoundMobile,
    pageTitle,
    showSlider,
    load,
    sendBindSmsCode,
    onSliderSuccess,
    submit,
  }
}
