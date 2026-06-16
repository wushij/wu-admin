<template>
  <view class="forgot-page">
    <AuthParticleBackground />

    <AuthBackNav @click="goBack" />

    <view class="forgot-page__content">
      <AuthGlassForm body-class="forgot-glass-body">
        <view class="auth-form__head">
          <text class="auth-form__title">忘记密码</text>
          <text class="auth-form__platform">{{ stepTitle }}</text>
        </view>

        <view class="step-bar">
          <view
            v-for="item in steps"
            :key="item.no"
            class="step-bar__item"
            :class="{
              'step-bar__item--active': step === item.no,
              'step-bar__item--done': step > item.no,
            }"
          >
            <view class="step-bar__dot">
              <text v-if="step <= item.no" class="step-bar__num">{{ item.no }}</text>
              <text v-else class="step-bar__check">✓</text>
            </view>
            <text class="step-bar__label">{{ item.label }}</text>
          </view>
        </view>

        <view v-if="step === 1" class="form-block">
          <AuthInput
            v-model="form.username"
            icon="user"
            custom-class="auth-anim"
            placeholder="请输入用户名"
          />
          <button class="auth-submit auth-anim auth-anim--2" :loading="loading" @click="submitUsername">
            下一步
          </button>
        </view>

        <view v-else-if="step === 2" class="form-block">
          <view class="sms-tip auth-anim">
            <text class="sms-tip__title">已绑定 {{ maskedMobile }}</text>
            <text class="sms-tip__sub">验证码将发送至该手机号</text>
          </view>
          <view class="sms-row auth-anim auth-anim--2">
            <AuthInput
              v-model="form.smsCode"
              icon="key"
              type="number"
              custom-class="sms-input"
              placeholder="请输入验证码"
              :maxlength="6"
            />
            <button
              class="sms-btn"
              :disabled="sendingSms || smsCountdown > 0"
              :loading="sendingSms"
              @click="handleSendSms"
            >
              {{ smsCountdown > 0 ? `${smsCountdown}s` : '获取验证码' }}
            </button>
          </view>
          <button class="auth-submit auth-anim auth-anim--3" @click="submitSmsStep">下一步</button>
        </view>

        <view v-else class="form-block">
          <AuthPasswordInput
            v-model="form.newPassword"
            custom-class="auth-anim"
            :placeholder="`新密码（至少 ${minPwdLen} 位）`"
          />
          <AuthPasswordInput
            v-model="form.confirmPassword"
            custom-class="auth-anim auth-anim--2"
            placeholder="确认新密码"
          />
          <button class="auth-submit auth-anim auth-anim--3" :loading="loading" @click="submitReset">
            完成重置
          </button>
        </view>

        <AuthFooterLink
          custom-class="auth-anim auth-anim--4"
          text="想起密码了？"
          link-text="返回登录"
          @click="navigateToLogin"
        />
      </AuthGlassForm>
    </view>

    <SliderCaptcha v-model:show="showSlider" scene="forgot" @success="onSliderSuccess" />
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import SliderCaptcha from '@/components/business/SliderCaptcha/index.vue'
import AuthParticleBackground from '@/components/business/AuthParticleBackground/index.vue'
import AuthGlassForm from '@/components/business/AuthGlassForm/index.vue'
import AuthInput from '@/components/business/AuthInput/index.vue'
import AuthPasswordInput from '@/components/business/AuthPasswordInput/index.vue'
import AuthFooterLink from '@/components/business/AuthFooterLink/index.vue'
import AuthBackNav from '@/components/business/AuthBackNav/index.vue'
import { useForgotPasswordForm } from '@/composables/useForgotPasswordForm'
import { useH5PageHead } from '@/composables/useH5PageHead'
import { navigateToLogin } from '@/utils/auth-route'

useH5PageHead('忘记密码')

const steps = [
  { no: 1, label: '用户名' },
  { no: 2, label: '验证' },
  { no: 3, label: '新密码' },
] as const

const {
  step,
  loading,
  sendingSms,
  smsCountdown,
  showSlider,
  maskedMobile,
  minPwdLen,
  form,
  loadConfig,
  submitUsername,
  handleSendSms,
  onSliderSuccess,
  submitSmsStep,
  submitReset,
  goBack,
} = useForgotPasswordForm()

const stepTitle = computed(() => {
  if (step.value === 1) return '输入用户名以找回密码'
  if (step.value === 2) return '验证绑定手机号'
  return '设置新的登录密码'
})

onMounted(loadConfig)
</script>

<style lang="scss" scoped>

.forgot-page {
  position: relative;
  min-height: 100vh;
  background: #000;
  overflow: hidden;
}

.forgot-page__content {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding:
    calc(112rpx + env(safe-area-inset-top))
    40rpx
    calc(48rpx + env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

:deep(.forgot-glass-body) {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.auth-form__head {
  margin-bottom: 28rpx;
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
  color: rgba(230, 228, 250, 0.88);
  font-size: 26rpx;
  font-weight: 500;
  letter-spacing: 0.04em;
  line-height: 1.5;
}

.step-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 36rpx;
  padding: 0 8rpx;
}

.step-bar__item {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;

  &:not(:last-child)::after {
    content: '';
    position: absolute;
    top: 22rpx;
    left: calc(50% + 28rpx);
    width: calc(100% - 56rpx);
    height: 2rpx;
    background: rgba(255, 255, 255, 0.18);
  }

  &--done:not(:last-child)::after {
    background: rgba(167, 139, 250, 0.65);
  }
}

.step-bar__dot {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.12);
  border: 2rpx solid rgba(255, 255, 255, 0.22);
  transition: all 0.25s ease;
}

.step-bar__item--active .step-bar__dot {
  background: rgba(99, 102, 241, 0.35);
  border-color: rgba(196, 181, 253, 0.85);
  box-shadow: 0 0 20rpx rgba(99, 102, 241, 0.45);
}

.step-bar__item--done .step-bar__dot {
  background: rgba(99, 102, 241, 0.55);
  border-color: rgba(196, 181, 253, 0.7);
}

.step-bar__num,
.step-bar__check {
  color: rgba(255, 255, 255, 0.88);
  font-size: 22rpx;
  font-weight: 600;
  line-height: 1;
}

.step-bar__label {
  color: rgba(255, 255, 255, 0.45);
  font-size: 22rpx;
  white-space: nowrap;
}

.step-bar__item--active .step-bar__label,
.step-bar__item--done .step-bar__label {
  color: rgba(255, 255, 255, 0.82);
}

.form-block {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.sms-row {
  display: flex;
  gap: 16rpx;
  align-items: center;
}

.sms-input {
  flex: 1;
}

.sms-tip {
  padding: 24rpx 28rpx;
  border-radius: 16rpx;
  background: rgba(79, 70, 229, 0.18);
  border: 1px solid rgba(196, 181, 253, 0.35);
}

.sms-tip__title {
  display: block;
  color: rgba(255, 255, 255, 0.95);
  font-size: 28rpx;
  font-weight: 600;
}

.sms-tip__sub {
  display: block;
  margin-top: 8rpx;
  color: rgba(255, 255, 255, 0.72);
  font-size: 24rpx;
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
