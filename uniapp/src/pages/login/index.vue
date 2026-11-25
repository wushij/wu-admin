<template>
  <view class="login-page">
    <AuthParticleBackground />
    <AuthGlassToast />

    <view class="login-page__content" :class="{ 'login-page__content--centered': !showMobileEarth }">
      <!-- #ifdef H5 -->
      <view v-if="showMobileEarth" class="login-page__brand">
        <AuthEarth
          class="login-page__globe"
          :size="globeSize"
          :bar-count="220"
          :enable-orbit="true"
          :enable-zoom="false"
        />
      </view>
      <!-- #endif -->

      <AuthGlassForm>
        <view class="auth-form__head">
          <text class="auth-form__title">{{ appStore.loginWelcome }}</text>
          <text class="auth-form__platform">{{ appStore.platformName }}</text>
        </view>

        <view v-if="showLoginModeSwitch" class="login-mode-switch">
          <view
            class="login-mode-switch__item"
            :class="{ 'login-mode-switch__item--active': loginMode === 'account' }"
            @click="switchLoginMode('account')"
          >
            <svg viewBox="0 0 24 24" class="login-mode-switch__icon" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
            <text>账号登录</text>
          </view>
          <view
            v-if="smsLoginEnabled && smsEnabled"
            class="login-mode-switch__item"
            :class="{ 'login-mode-switch__item--active': loginMode === 'sms' }"
            @click="switchLoginMode('sms')"
          >
            <svg viewBox="0 0 24 24" class="login-mode-switch__icon" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="5" y="2" width="14" height="20" rx="2" ry="2" />
              <line x1="12" y1="18" x2="12.01" y2="18" />
            </svg>
            <text>短信登录</text>
          </view>
          <view
            v-if="emailLoginEnabled && emailEnabled"
            class="login-mode-switch__item"
            :class="{ 'login-mode-switch__item--active': loginMode === 'email' }"
            @click="switchLoginMode('email')"
          >
            <svg viewBox="0 0 24 24" class="login-mode-switch__icon" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
              <polyline points="22,6 12,13 2,6" />
            </svg>
            <text>邮箱登录</text>
          </view>
        </view>

        <view v-if="loginMode === 'account'" class="form-block">
          <AuthInput
            v-model="formData.username"
            icon="user"
            custom-class="auth-anim"
            placeholder="请输入用户名"
          />
          <AuthPasswordInput
            :key="passwordFieldKey"
            v-model="formData.password"
            custom-class="auth-anim auth-anim--2"
            placeholder="请输入密码"
          />
          <view v-if="captchaEnabled && captchaType === 'image'" class="captcha-row auth-anim auth-anim--3">
            <AuthInput
              v-model="formData.code"
              icon="lock"
              custom-class="captcha-input"
              placeholder="请输入验证码"
              :maxlength="6"
            />
            <image
              v-if="captchaImg"
              class="captcha-img"
              :src="captchaImg"
              mode="aspectFit"
              @click="refreshCaptcha"
            />
          </view>
          <view v-if="captchaEnabled && captchaType === 'slider'" class="slider-hint auth-anim auth-anim--3" @click="handleSubmit">
            <text>点击登录将弹出滑块验证</text>
          </view>
        </view>

        <view v-else-if="loginMode === 'sms'" class="form-block">
          <AuthInput
            v-model="formData.phone"
            icon="phone-o"
            type="number"
            custom-class="auth-anim"
            placeholder="请输入绑定的手机号"
            :maxlength="11"
          />
          <view class="sms-row auth-anim auth-anim--2">
            <AuthInput
              v-model="formData.code"
              icon="lock"
              custom-class="sms-input"
              placeholder="请输入验证码"
              :maxlength="6"
            />
            <button
              class="sms-btn"
              :disabled="!smsEnabled || sendingSms || smsCountdown > 0"
              :loading="sendingSms"
              @click="handleSendSms"
            >
              {{ smsCountdown > 0 ? `${smsCountdown}s` : '获取验证码' }}
            </button>
          </view>
        </view>

        <view v-else-if="loginMode === 'email'" class="form-block">
          <AuthInput
            v-model="formData.email"
            icon="envelop-o"
            custom-class="auth-anim"
            placeholder="请输入绑定的邮箱地址"
            :maxlength="60"
          />
          <view class="sms-row auth-anim auth-anim--2">
            <AuthInput
              v-model="formData.code"
              icon="lock"
              custom-class="sms-input"
              placeholder="请输入验证码"
              :maxlength="6"
            />
            <button
              class="sms-btn"
              :disabled="!emailEnabled || sendingEmail || emailCountdown > 0"
              :loading="sendingEmail"
              @click="handleSendEmail"
            >
              {{ emailCountdown > 0 ? `${emailCountdown}s` : '获取验证码' }}
            </button>
          </view>
        </view>

        <view class="remember-row auth-anim auth-anim--4">
          <label v-if="rememberMeEnabled" class="remember-label">
            <checkbox :checked="formData.rememberMe" @click="formData.rememberMe = !formData.rememberMe" />
            <text>记住我</text>
          </label>
          <text class="forgot-link" @click="goForgotPassword">忘记密码</text>
        </view>

        <AuthSubmitButton
          custom-class="auth-anim auth-anim--5"
          label="登 录"
          loading-text="登录中…"
          :loading="loading"
          :success="loginSuccess"
          @click="handleSubmit"
        />

        <AuthFooterLink
          v-if="registerEnabled"
          custom-class="auth-anim auth-anim--5"
          text="还没有账号？"
          link-text="立即注册"
          @click="goRegister"
        />
      </AuthGlassForm>

      <view v-if="appStore.copyright || (appStore.icpEnabled && appStore.icpNumber)" class="login-page__footer auth-anim auth-anim--5">
        <text v-if="appStore.copyright" class="footer-text">{{ appStore.copyright }}</text>
        <text v-if="appStore.copyright && appStore.icpEnabled && appStore.icpNumber" class="footer-divider">|</text>
        <!-- #ifdef H5 -->
        <a
          v-if="appStore.icpEnabled && appStore.icpNumber"
          class="footer-icp-link"
          :href="appStore.icpUrl || 'https://beian.miit.gov.cn/'"
          target="_blank"
          rel="noopener noreferrer"
        >
          {{ appStore.icpNumber }}
        </a>
        <!-- #endif -->
        <!-- #ifndef H5 -->
        <text v-if="appStore.icpEnabled && appStore.icpNumber" class="footer-text">{{ appStore.icpNumber }}</text>
        <!-- #endif -->
      </view>
    </view>

    <SliderCaptcha
      v-model:show="showSliderModal"
      :scene="sliderPurpose"
      @success="onSliderSuccess"
    />
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SliderCaptcha from '@/components/business/shared/SliderCaptcha/index.vue'
// #ifdef H5
import AuthEarth from '@/components/business/auth/AuthEarth/index.vue'
// #endif
import AuthParticleBackground from '@/components/business/auth/AuthParticleBackground/index.vue'
import AuthGlassToast from '@/components/business/auth/AuthGlassToast/index.vue'
import AuthGlassForm from '@/components/business/auth/AuthGlassForm/index.vue'
import AuthInput from '@/components/business/auth/AuthInput/index.vue'
import AuthPasswordInput from '@/components/business/auth/AuthPasswordInput/index.vue'
import AuthSubmitButton from '@/components/business/auth/AuthSubmitButton/index.vue'
import AuthFooterLink from '@/components/business/auth/AuthFooterLink/index.vue'
import { useLoginForm } from '@/composables/auth/useLoginForm'
import { useH5PageHead } from '@/composables/base/useH5PageHead'

const pageReady = ref(false)
/** 移动端登录页 3D 地球开关：默认关闭，改为 true 即可恢复顶部 3D 地球展示 */
const showMobileEarth = ref(false)

// #ifdef H5
const globeSize = ref(240)
// #endif

useH5PageHead('登录')

const {
  appStore,
  captchaEnabled,
  captchaType,
  smsLoginEnabled,
  emailLoginEnabled,
  loginMode,
  rememberMeEnabled,
  registerEnabled,
  showSliderModal,
  sliderPurpose,
  captchaImg,
  formData,
  passwordFieldKey,
  loading,
  loginSuccess,
  sendingSms,
  sendingEmail,
  smsCountdown,
  emailCountdown,
  smsEnabled,
  emailEnabled,
  showLoginModeSwitch,
  loadConfig,
  refreshCaptcha,
  restoreRemember,
  switchLoginMode,
  handleSendSms,
  handleSendEmail,
  handleSubmit,
  onSliderSuccess,
  goRegister,
  goForgotPassword,
} = useLoginForm()

onMounted(async () => {
  // #ifdef H5
  const sysInfo = uni.getSystemInfoSync()
  globeSize.value = Math.min(Math.round(sysInfo.windowWidth * 0.6), 280)
  // #endif

  try {
    await appStore.loadPublicConfig()
  } catch {
    /* 离线或后端未启动时使用默认文案 */
  }
  await loadConfig()
  restoreRemember()
  await refreshCaptcha()
  pageReady.value = true
})

onShow(() => {
  // 已登录用户重定向由全局守卫 global-page-guards 统一处理
  if (!pageReady.value) return
  refreshCaptcha()
})
</script>

<style lang="scss" scoped>

.login-page {
  position: relative;
  min-height: 100vh;
  background: $color-auth-bg;
  overflow: hidden;
}

.login-page__content {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding: 24rpx 40rpx calc(48rpx + env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  box-sizing: border-box;

  &--centered {
    justify-content: center;
    padding-top: calc(48rpx + env(safe-area-inset-top));
  }
}

/* #ifdef MP-WEIXIN */
.login-page__content {
  justify-content: center;
  padding: calc(32rpx + env(safe-area-inset-top)) 40rpx calc(48rpx + env(safe-area-inset-bottom));
}
/* #endif */

.login-page__brand {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 720rpx;
  text-align: center;
  margin-bottom: 8rpx;
  transform: translateY(-24rpx);
}

.login-page__globe {
  margin: 0 auto 4rpx;
}

.auth-form__head {
  margin-bottom: 40rpx;
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

.login-mode-switch {
  display: flex;
  gap: 8rpx;
  padding: 8rpx;
  margin-bottom: 36rpx;
  border-radius: 9999rpx;
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.07);
}

.login-mode-switch__item {
  flex: 1;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  border-radius: 9999rpx;
  border: 1px solid transparent;
  color: rgba(203, 213, 225, 0.75);
  font-size: 27rpx;
  font-weight: 500;
  transition: all 0.22s ease;
}

.login-mode-switch__icon {
  width: 30rpx;
  height: 30rpx;
  flex-shrink: 0;
  color: currentColor;
}

.login-mode-switch__item--active {
  @include auth-pearl-ice-btn;
  font-weight: 600;
}

.form-block {
  display: flex;
  flex-direction: column;
  gap: 28rpx;
}

.captcha-row,
.sms-row {
  display: flex;
  gap: 18rpx;
  align-items: center;
}

.captcha-input,
.sms-input {
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

.sms-btn {
  min-width: 224rpx;
  height: 92rpx;
  line-height: 92rpx;
  padding: 0 28rpx;
  font-size: 27rpx;
  font-weight: 600;
  @include auth-pearl-ice-btn;

  &[disabled] {
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(255, 255, 255, 0.14);
    color: rgba(203, 213, 225, 0.6);
    box-shadow: none;
  }
}

.slider-hint {
  padding: 24rpx;
  text-align: center;
  color: rgba(203, 213, 225, 0.78);
  font-size: 24rpx;
  border: 1px dashed rgba(255, 255, 255, 0.22);
  border-radius: 9999rpx;
  background: rgba(255, 255, 255, 0.04);
}

.remember-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 28rpx 8rpx 32rpx;
}

.remember-label {
  display: flex;
  align-items: center;
  gap: 12rpx;
  color: rgba(226, 232, 240, 0.86);
  font-size: 27rpx;
  @include auth-glass-checkbox;
}

.forgot-link {
  color: rgba(226, 232, 240, 0.88);
  font-size: 27rpx;
  font-weight: 500;
}

.auth-anim {
  animation: fadeInUp 0.6s ease both;
}

.auth-anim--2 {
  animation-delay: 0.1s;
}

.auth-anim--3 {
  animation-delay: 0.2s;
}

.auth-anim--4 {
  animation-delay: 0.3s;
}

.auth-anim--5 {
  animation-delay: 0.4s;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-page__footer {
  margin-top: 32rpx;
  padding: 16rpx 20rpx 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12rpx;
  text-align: center;
  font-size: 23rpx;
  color: rgba(203, 213, 225, 0.56);
}

.footer-divider {
  color: rgba(203, 213, 225, 0.35);
}

.footer-text {
  color: rgba(203, 213, 225, 0.56);
}

.footer-icp-link {
  color: rgba(203, 213, 225, 0.68);
  text-decoration: none !important;
}

.footer-icp-link:hover {
  color: rgba(248, 250, 252, 0.9);
  text-decoration: none !important;
}

</style>

<style lang="scss">
page,
uni-page-body {
  background-color: $color-auth-bg;
}
</style>
