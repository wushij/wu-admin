<template>
  <view class="login-page">
    <AuthParticleBackground />

    <view class="login-page__content">
      <view class="login-page__brand">
        <AuthEarth
          class="login-page__globe"
          :size="globeSize"
          :bar-count="220"
          :enable-orbit="true"
          :enable-zoom="false"
        />
      </view>

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
            账号登录
          </view>
          <view
            class="login-mode-switch__item"
            :class="{ 'login-mode-switch__item--active': loginMode === 'sms' }"
            @click="switchLoginMode('sms')"
          >
            短信登录
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
            v-model="formData.password"
            custom-class="auth-anim auth-anim--2"
            placeholder="请输入密码"
          />
          <view v-if="captchaEnabled && captchaType === 'image'" class="captcha-row auth-anim auth-anim--3">
            <AuthInput
              v-model="formData.code"
              icon="key"
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

        <view v-else class="form-block">
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
              icon="key"
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

        <view class="remember-row auth-anim auth-anim--4">
          <label v-if="rememberMeEnabled" class="remember-label">
            <checkbox :checked="formData.rememberMe" @click="formData.rememberMe = !formData.rememberMe" />
            <text>记住我</text>
          </label>
          <text class="forgot-link" @click="goForgotPassword">忘记密码</text>
        </view>

        <button class="auth-submit auth-anim auth-anim--5" :loading="loading" @click="handleSubmit">登 录</button>

        <AuthFooterLink
          v-if="registerEnabled"
          custom-class="auth-anim auth-anim--5"
          text="还没有账号？"
          link-text="立即注册"
          @click="goRegister"
        />
      </AuthGlassForm>
    </view>

    <SliderCaptcha v-model:show="showSliderModal" @success="onSliderSuccess" />
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SliderCaptcha from '@/components/business/SliderCaptcha/index.vue'
import AuthEarth from '@/components/business/AuthEarth/index.vue'
import AuthParticleBackground from '@/components/business/AuthParticleBackground/index.vue'
import AuthGlassForm from '@/components/business/AuthGlassForm/index.vue'
import AuthInput from '@/components/business/AuthInput/index.vue'
import AuthPasswordInput from '@/components/business/AuthPasswordInput/index.vue'
import AuthFooterLink from '@/components/business/AuthFooterLink/index.vue'
import { useLoginForm } from '@/composables/useLoginForm'
import { useH5PageHead } from '@/composables/useH5PageHead'

const globeSize = ref(240)
const pageReady = ref(false)

useH5PageHead('登录')

const {
  appStore,
  captchaEnabled,
  captchaType,
  loginMode,
  rememberMeEnabled,
  registerEnabled,
  showSliderModal,
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
} = useLoginForm()

onMounted(async () => {
  const sysInfo = uni.getSystemInfoSync()
  globeSize.value = Math.min(Math.round(sysInfo.windowWidth * 0.6), 280)

  await appStore.loadPublicConfig()
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
  background: #000;
  overflow: hidden;
}

.login-page__content {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding: 12rpx 40rpx calc(48rpx + env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  box-sizing: border-box;
}

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
  margin-bottom: 32rpx;
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

.login-mode-switch {
  display: flex;
  gap: 8rpx;
  padding: 8rpx;
  margin-bottom: 32rpx;
  border-radius: 24rpx;
  background: rgba(0, 0, 0, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.login-mode-switch__item {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border-radius: 18rpx;
  color: rgba(255, 255, 255, 0.65);
  font-size: 28rpx;
  transition: all 0.25s ease;
}

.login-mode-switch__item--active {
  background: rgba(255, 255, 255, 0.95);
  color: #303133;
  box-shadow: 0 4rpx 24rpx rgba(0, 0, 0, 0.15);
}

.form-block {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.captcha-row,
.sms-row {
  display: flex;
  gap: 16rpx;
  align-items: center;
}

.captcha-input,
.sms-input {
  flex: 1;
}

.captcha-img {
  width: 200rpx;
  height: 96rpx;
  border-radius: 16rpx;
  background: #fff;
}

.sms-btn {
  min-width: 200rpx;
  height: 96rpx;
  line-height: 96rpx;
  padding: 0 16rpx;
  font-size: 26rpx;
  color: #303133;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 16rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.2);

  &[disabled] {
    opacity: 0.45;
    color: #909399;
  }
}

.slider-hint {
  padding: 24rpx;
  text-align: center;
  color: rgba(255, 255, 255, 0.8);
  font-size: 24rpx;
  border: 1px dashed rgba(255, 255, 255, 0.3);
  border-radius: 16rpx;
}

.remember-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 24rpx 0;
}

.remember-label {
  display: flex;
  align-items: center;
  gap: 12rpx;
  color: rgba(255, 255, 255, 0.92);
  font-size: 26rpx;
  text-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.5);
}

.forgot-link {
  color: rgba(255, 255, 255, 0.92);
  font-size: 26rpx;
  text-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.5);
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

</style>

<style lang="scss">
page,
uni-page-body {
  background-color: #000000;
}
</style>
