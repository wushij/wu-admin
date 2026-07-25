<template>
  <view class="register-page">
    <AuthParticleBackground />

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

      <button class="auth-submit" :loading="loading" @click="handleSubmit">注 册</button>

      <AuthFooterLink text="已有账号？" link-text="立即登录" @click="goLogin" />
    </AuthGlassForm>
    </view>

    <SliderCaptcha v-model:show="showSlider" scene="register" @success="onSliderSuccess" />
  </view>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { register, getCaptcha, getConfig } from '@/api/system/auth'
import { useAppStore } from '@/store/app'
import { sliderVerifyToRequest } from '@/utils/slider-captcha'
import type { SliderVerifyPayload } from '@/utils/slider-captcha'
import SliderCaptcha from '@/components/business/SliderCaptcha/index.vue'
import AuthParticleBackground from '@/components/business/AuthParticleBackground/index.vue'
import AuthGlassForm from '@/components/business/AuthGlassForm/index.vue'
import AuthInput from '@/components/business/AuthInput/index.vue'
import AuthPasswordInput from '@/components/business/AuthPasswordInput/index.vue'
import AuthFooterLink from '@/components/business/AuthFooterLink/index.vue'
import { toCaptchaDataUrl } from '@/utils/captcha'
import { navigateToLogin } from '@/utils/auth-route'
import { useH5PageHead } from '@/composables/useH5PageHead'

useH5PageHead('注册')

const appStore = useAppStore()
const loading = ref(false)
const captchaEnabled = ref(true)
const captchaType = ref<'image' | 'slider'>('image')
const captchaImg = ref('')
const captchaUuid = ref('')
const showSlider = ref(false)
const agreeTerms = ref(false)
const agreeAlert = ref(false)
let navigatingToLogin = false
let refreshingCaptcha = false

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
  if (!agreeTerms.value) {
    agreeAlert.value = true
    uni.showToast({ title: '请先同意用户协议', icon: 'none' })
    return
  }
  agreeAlert.value = false
  const username = form.username.trim()
  if (!username || !form.password) {
    uni.showToast({ title: '请填写用户名和密码', icon: 'none' })
    return
  }
  if (!/^[a-zA-Z0-9_]{4,12}$/.test(username)) {
    uni.showToast({ title: '用户名只能包含字母、数字、下划线，长度4-12位', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const payload: Parameters<typeof register>[0] = {
      username,
      password: form.password,
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
    const msg = res.message || res.msg || '注册成功，请登录'
    uni.showToast({ title: msg, icon: 'success' })
    setTimeout(() => goLogin(), 500)
  } catch (e) {
    console.error(e)
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

function handleSubmit() {
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
  margin-bottom: 8rpx;
  text-align: center;
}

.auth-form__title {
  display: block;
  color: #fff;
  font-size: 48rpx;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-shadow: 0 4rpx 24rpx rgba(0, 0, 0, 0.35);
}

.auth-form__platform {
  display: block;
  margin-top: 12rpx;
  color: rgba(230, 228, 250, 0.92);
  font-size: 28rpx;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.35);
}

:deep(.register-glass-body) {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.captcha-row {
  display: flex;
  gap: 16rpx;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  width: 200rpx;
  height: 96rpx;
  border-radius: 16rpx;
  background: #fff;
}

.auth-submit {
  width: 100%;
  height: 104rpx;
  line-height: 104rpx;
  margin-top: 8rpx;
  border-radius: $radius-md;
  font-size: 32rpx;
  font-weight: 600;
  letter-spacing: 4rpx;
  border: 1px solid rgba(196, 181, 253, 0.42);
  box-shadow:
    0 20rpx 56rpx rgba(99, 102, 241, 0.38),
    inset 0 1px 0 rgba(255, 255, 255, 0.18);
  @include auth-submit-gradient;
}

.agree-row {
  padding: 4rpx 0;
}

.agree-row--alert {
  animation: shake 0.4s ease;
}

.agree-label {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  color: rgba(255, 255, 255, 0.85);
  font-size: 24rpx;
  line-height: 1.5;
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
  background-color: #000000;
}
</style>
