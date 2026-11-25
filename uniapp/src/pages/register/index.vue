<template>
  <view class="register-page">
    <AuthParticleBackground />
    <AuthGlassToast />

    <view class="register-page__content">
    <AuthGlassForm body-class="register-glass-body">
      <view class="auth-form__head">
        <text class="auth-form__title">{{ appStore.registerTitle }}</text>
        <text class="auth-form__platform">{{ appStore.platformName }}</text>
      </view>

      <AuthInput v-model="form.username" icon="user" placeholder="用户名（4-12位字母数字下划线）" :maxlength="12" />
      <AuthPasswordInput v-model="form.password" placeholder="请输入密码（6-20位）" :maxlength="20" />
      <AuthInput v-model="form.nickname" icon="user-o" placeholder="昵称（可选）" :maxlength="20" />
      <AuthInput
        v-model="form.mobile"
        icon="phone-o"
        type="number"
        placeholder="手机号（可选）"
        :maxlength="11"
      />

      <view v-if="captchaEnabled && captchaType === 'image'" class="captcha-row">
        <AuthInput
          v-model="form.code"
          icon="key"
          custom-class="captcha-input"
          placeholder="请输入验证码"
          :maxlength="6"
        />
        <image v-if="captchaImg" class="captcha-img" :src="captchaImg" mode="aspectFit" @click="refreshCaptcha" />
      </view>

      <view class="agree-row" :class="{ 'agree-row--alert': agreeAlert }">
        <label class="agree-label">
          <checkbox :checked="agreeTerms" @click="agreeTerms = !agreeTerms" />
          <text>我已阅读并同意《用户协议》和《隐私政策》</text>
        </label>
      </view>

      <AuthSubmitButton
        variant="ice"
        label="注 册"
        loading-text="注册中…"
        :loading="loading"
        :success="registerSuccess"
        @click="handleSubmit"
      />

      <AuthFooterLink text="已有账号？" link-text="立即登录" @click="goLogin" />
    </AuthGlassForm>
    </view>

    <SliderCaptcha v-model:show="showSlider" scene="register" @success="onSliderSuccess" />
  </view>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, onUnmounted } from 'vue'
import { register, getCaptcha, getConfig } from '@/api/system/auth'
import { useAppStore } from '@/store/app'
import { sliderVerifyToRequest } from '@/utils/security/slider-captcha'
import type { SliderVerifyPayload } from '@/utils/security/slider-captcha'
import SliderCaptcha from '@/components/business/shared/SliderCaptcha/index.vue'
import AuthParticleBackground from '@/components/business/auth/AuthParticleBackground/index.vue'
import AuthGlassToast from '@/components/business/auth/AuthGlassToast/index.vue'
import AuthGlassForm from '@/components/business/auth/AuthGlassForm/index.vue'
import AuthInput from '@/components/business/auth/AuthInput/index.vue'
import AuthPasswordInput from '@/components/business/auth/AuthPasswordInput/index.vue'
import AuthSubmitButton from '@/components/business/auth/AuthSubmitButton/index.vue'
import AuthFooterLink from '@/components/business/auth/AuthFooterLink/index.vue'
import { showAuthToast } from '@/composables/auth/useAuthToast'
import { toCaptchaDataUrl } from '@/utils/security/captcha'
import { navigateToLogin } from '@/utils/nav/auth-route'
import { useH5PageHead } from '@/composables/base/useH5PageHead'
import { encryptSm2 } from '@/utils/security/crypto'

useH5PageHead('注册')

const appStore = useAppStore()
const loading = ref(false)
const registerSuccess = ref(false)
const captchaEnabled = ref(true)
const captchaType = ref<'image' | 'slider'>('image')
const captchaImg = ref('')
const captchaUuid = ref('')
const showSlider = ref(false)
const agreeTerms = ref(false)
const agreeAlert = ref(false)
let navigatingToLogin = false
let refreshingCaptcha = false
let redirectTimer: ReturnType<typeof setTimeout> | null = null

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  mobile: '',
  code: '',
})

async function loadConfig() {
  await appStore.loadPublicConfig()
  try {
    const res = await getConfig()
    captchaEnabled.value = res.data?.register?.captchaEnabled !== false
    captchaType.value = res.data?.register?.captchaType === 'slider' ? 'slider' : 'image'
  } catch {
    /* ignore */
  }
}

async function refreshCaptcha() {
  if (refreshingCaptcha || !captchaEnabled.value || captchaType.value === 'slider') return
  refreshingCaptcha = true
  try {
    const res = await getCaptcha('register')
    captchaUuid.value = res.data.uuid
    captchaImg.value = toCaptchaDataUrl(res.data.img || res.data.image)
  } catch (e) {
    console.error(e)
    captchaImg.value = ''
  } finally {
    refreshingCaptcha = false
  }
}

async function doRegister(sliderCaptcha?: { uuid: string; code: string }) {
  if (loading.value || registerSuccess.value) return
  if (!agreeTerms.value) {
    agreeAlert.value = true
    showAuthToast('请先同意用户协议', 'warning')
    return
  }
  agreeAlert.value = false
  const username = form.username.trim()
  if (!username || !form.password) {
    showAuthToast('请填写用户名和密码', 'warning')
    return
  }
  if (!/^[a-zA-Z0-9_]{4,12}$/.test(username)) {
    showAuthToast('用户名只能包含字母、数字、下划线，长度4-12位', 'warning')
    return
  }
  loading.value = true
  try {
    const payload: Parameters<typeof register>[0] = {
      username,
      password: encryptSm2(form.password),
      nickname: form.nickname.trim() || undefined,
      mobile: form.mobile.trim() || undefined,
    }
    if (captchaEnabled.value) {
      if (captchaType.value === 'slider') {
        if (!sliderCaptcha) return
        payload.uuid = sliderCaptcha.uuid
        payload.code = sliderCaptcha.code
      } else {
        payload.uuid = captchaUuid.value
        payload.code = form.code || undefined
      }
    }
    const res = await register(payload)
    const needAudit = res.data === true || (typeof res.message === 'string' && res.message.includes('审核'))
    if (needAudit) {
      uni.showModal({
        title: '注册成功（待审核）',
        content: '您的账号已成功提交注册，当前需要等待管理员审核通过后方可登录。请耐心等待！',
        showCancel: false,
        confirmText: '去登录',
        success: () => goLogin(),
      })
    } else {
      const msg = res.message || res.msg || '注册成功，请登录'
      registerSuccess.value = true
      showAuthToast(msg, 'success')
      redirectTimer = setTimeout(() => {
        redirectTimer = null
        goLogin()
      }, 780)
    }
  } catch (e) {
    console.error(e)
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

function handleSubmit() {
  if (loading.value || registerSuccess.value) return
  if (captchaEnabled.value && captchaType.value === 'slider') {
    showSlider.value = true
    return
  }
  doRegister()
}

function onSliderSuccess(payload: SliderVerifyPayload) {
  doRegister(sliderVerifyToRequest(payload))
}

function goLogin() {
  if (navigatingToLogin) return
  navigatingToLogin = true
  navigateToLogin()
  setTimeout(() => {
    navigatingToLogin = false
  }, 600)
}

onMounted(async () => {
  await loadConfig()
  await refreshCaptcha()
})

onUnmounted(() => {
  if (redirectTimer) clearTimeout(redirectTimer)
})
</script>

<style lang="scss" scoped>

.register-page {
  position: relative;
  min-height: 100vh;
  background: $color-auth-bg;
  overflow: hidden;
}

.register-page__content {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding: 80rpx 40rpx calc(48rpx + env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.auth-form__head {
  margin-bottom: 12rpx;
  text-align: center;
}

.auth-form__title {
  display: block;
  color: #f8fafc;
  font-size: 50rpx;
  font-weight: 700;
  letter-spacing: 0.02em;
  line-height: 1.25;
}

.auth-form__platform {
  display: block;
  margin-top: 14rpx;
  color: rgba(203, 213, 225, 0.72);
  font-size: 26rpx;
  font-weight: 400;
  letter-spacing: 0.04em;
}

:deep(.register-glass-body) {
  display: flex;
  flex-direction: column;
  gap: 28rpx;
}

.captcha-row {
  display: flex;
  gap: 18rpx;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  width: 220rpx;
  height: 92rpx;
  border-radius: 9999rpx;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(248, 250, 252, 0.9);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.35);
}

.agree-row {
  padding: 0 8rpx;
}

.agree-row--alert {
  animation: shake 0.4s ease;
}

.agree-label {
  display: flex;
  align-items: center;
  gap: 12rpx;
  color: rgba(226, 232, 240, 0.86);
  font-size: 25rpx;
  line-height: 1.5;
  @include auth-glass-checkbox;
}

@keyframes shake {
  0%,
  100% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(-8rpx);
  }
  75% {
    transform: translateX(8rpx);
  }
}
</style>

<style lang="scss">
page,
uni-page-body {
  background-color: $color-auth-bg;
}
</style>
