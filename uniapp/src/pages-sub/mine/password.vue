<template>
  <view class="password-page">
    <view class="password-hero">
      <view class="password-hero__pattern" />
      <view class="password-hero__glow" />
      <view class="password-hero__body">
        <ModuleIcon icon="shield-o" theme="violet" size="lg" />
        <view class="password-hero__text">
          <text class="password-hero__title">账号安全</text>
          <text class="password-hero__subtitle">定期更换密码，保护账号安全</text>
        </view>
      </view>
    </view>

    <view v-if="canUseSmsReset || canUseEmailReset" class="mode-tabs">
      <view
        class="mode-tabs__item"
        :class="{ 'mode-tabs__item--active': mode === 'password' }"
        @click="closeSmsMode"
      >
        <IconFont name="lock" :size="28" :color="mode === 'password' ? '#ffffff' : '#64748b'" />
        <text>原密码修改</text>
      </view>
      <view
        v-if="canUseSmsReset"
        class="mode-tabs__item"
        :class="{ 'mode-tabs__item--active': mode === 'sms' }"
        @click="openSmsMode"
      >
        <IconFont name="phone-o" :size="28" :color="mode === 'sms' ? '#ffffff' : '#64748b'" />
        <text>短信重置</text>
      </view>
      <view
        v-if="canUseEmailReset"
        class="mode-tabs__item"
        :class="{ 'mode-tabs__item--active': mode === 'email' }"
        @click="openEmailMode"
      >
        <IconFont name="envelop-o" :size="28" :color="mode === 'email' ? '#ffffff' : '#64748b'" />
        <text>邮箱重置</text>
      </view>
    </view>

    <view v-if="canUseSmsReset && mode === 'sms'" class="sms-banner card--elevated">
      <ModuleIcon icon="phone-o" theme="cyan" size="sm" />
      <view class="sms-banner__text">
        <text class="sms-banner__title">已绑定 {{ maskedMobile }}</text>
        <text class="sms-banner__sub">验证码将发送至该手机号</text>
      </view>
    </view>

    <view v-if="canUseEmailReset && mode === 'email'" class="sms-banner card--elevated">
      <ModuleIcon icon="envelop-o" theme="violet" size="sm" />
      <view class="sms-banner__text">
        <text class="sms-banner__title">已绑定 {{ maskedEmail }}</text>
        <text class="sms-banner__sub">验证码将发送至该邮箱</text>
      </view>
    </view>

    <view class="password-section card--elevated">
      <view class="password-section__head">
        <ModuleIcon :icon="mode === 'password' ? 'lock' : (mode === 'sms' ? 'chat-o' : 'envelop-o')" :theme="mode === 'password' ? 'violet' : (mode === 'sms' ? 'cyan' : 'violet')" size="sm" />
        <text class="password-section__title">{{ mode === 'password' ? '修改密码' : (mode === 'sms' ? '短信验证重置' : '邮箱验证重置') }}</text>
      </view>

      <view class="password-fields">
        <template v-if="mode === 'password'">
          <view class="password-field">
            <text class="password-field__label">原密码</text>
            <view class="password-field__input-wrap">
              <input
                v-model="form.oldPassword"
                class="password-field__input"
                :password="!showOldPassword"
                placeholder="请输入原密码"
              />
              <view class="password-field__toggle" @tap.stop="showOldPassword = !showOldPassword">
                <PasswordEyeIcon :slashed="showOldPassword" />
              </view>
            </view>
          </view>

          <view class="password-field">
            <text class="password-field__label">新密码</text>
            <view class="password-field__input-wrap">
              <input
                v-model="form.newPassword"
                class="password-field__input"
                :password="!showNewPassword"
                :placeholder="`至少 ${minPwdLen} 位`"
              />
              <view class="password-field__toggle" @tap.stop="showNewPassword = !showNewPassword">
                <PasswordEyeIcon :slashed="showNewPassword" />
              </view>
            </view>
          </view>

          <view class="password-field password-field--last">
            <text class="password-field__label">确认密码</text>
            <view class="password-field__input-wrap">
              <input
                v-model="form.confirmPassword"
                class="password-field__input"
                :password="!showConfirmPassword"
                placeholder="再次输入新密码"
              />
              <view class="password-field__toggle" @tap.stop="showConfirmPassword = !showConfirmPassword">
                <PasswordEyeIcon :slashed="showConfirmPassword" />
              </view>
            </view>
          </view>
        </template>

        <template v-else-if="mode === 'sms'">
          <view class="password-field">
            <text class="password-field__label">验证码</text>
            <view class="password-field__sms-row">
              <view class="password-field__input-wrap password-field__input-wrap--sms">
                <input
                  v-model="smsForm.smsCode"
                  class="password-field__input"
                  :maxlength="6"
                  placeholder="请输入验证码"
                />
              </view>
              <button
                class="password-field__sms-btn"
                :disabled="smsCountdown > 0 || sendingSms"
                :loading="sendingSms"
                @click="sendSmsCode"
              >
                {{ smsCountdown > 0 ? `${smsCountdown}s` : '获取验证码' }}
              </button>
            </view>
          </view>

          <view class="password-field">
            <text class="password-field__label">新密码</text>
            <view class="password-field__input-wrap">
              <input
                v-model="smsForm.newPassword"
                class="password-field__input"
                :password="!showSmsNewPassword"
                :placeholder="`至少 ${minPwdLen} 位`"
              />
              <view class="password-field__toggle" @tap.stop="showSmsNewPassword = !showSmsNewPassword">
                <PasswordEyeIcon :slashed="showSmsNewPassword" />
              </view>
            </view>
          </view>

          <view class="password-field password-field--last">
            <text class="password-field__label">确认密码</text>
            <view class="password-field__input-wrap">
              <input
                v-model="smsForm.confirmPassword"
                class="password-field__input"
                :password="!showSmsConfirmPassword"
                placeholder="再次输入新密码"
              />
              <view class="password-field__toggle" @tap.stop="showSmsConfirmPassword = !showSmsConfirmPassword">
                <PasswordEyeIcon :slashed="showSmsConfirmPassword" />
              </view>
            </view>
          </view>
        </template>

        <template v-else-if="mode === 'email'">
          <view class="password-field">
            <text class="password-field__label">邮箱验证码</text>
            <view class="password-field__sms-row">
              <view class="password-field__input-wrap password-field__input-wrap--sms">
                <input
                  v-model="emailForm.emailCode"
                  class="password-field__input"
                  :maxlength="6"
                  placeholder="请输入 6 位验证码"
                />
              </view>
              <button
                class="password-field__sms-btn"
                :disabled="emailCountdown > 0 || sendingEmail"
                :loading="sendingEmail"
                @click="sendEmailCode"
              >
                {{ emailCountdown > 0 ? `${emailCountdown}s` : '获取验证码' }}
              </button>
            </view>
          </view>

          <view class="password-field">
            <text class="password-field__label">新密码</text>
            <view class="password-field__input-wrap">
              <input
                v-model="emailForm.newPassword"
                class="password-field__input"
                :password="!showEmailNewPassword"
                :placeholder="`至少 ${minPwdLen} 位`"
              />
              <view class="password-field__toggle" @tap.stop="showEmailNewPassword = !showEmailNewPassword">
                <PasswordEyeIcon :slashed="showEmailNewPassword" />
              </view>
            </view>
          </view>

          <view class="password-field password-field--last">
            <text class="password-field__label">确认密码</text>
            <view class="password-field__input-wrap">
              <input
                v-model="emailForm.confirmPassword"
                class="password-field__input"
                :password="!showEmailConfirmPassword"
                placeholder="再次输入新密码"
              />
              <view class="password-field__toggle" @tap.stop="showEmailConfirmPassword = !showEmailConfirmPassword">
                <PasswordEyeIcon :slashed="showEmailConfirmPassword" />
              </view>
            </view>
          </view>
        </template>
      </view>
    </view>

    <view class="password-tips card--elevated">
      <text class="password-tips__title">安全建议</text>
      <view class="password-tips__item">
        <text class="password-tips__dot">·</text>
        <text class="password-tips__text">密码长度不少于 {{ minPwdLen }} 位，建议包含字母与数字。</text>
      </view>
      <view class="password-tips__item">
        <text class="password-tips__dot">·</text>
        <text class="password-tips__text">请勿使用与其他平台相同的密码。</text>
      </view>
    </view>

    <PageFooter>
      <button class="password-save-btn" :loading="saving" @click="onSubmit">
        {{ mode === 'password' ? '确认修改' : '确认重置' }}
      </button>
    </PageFooter>

    <SliderCaptcha v-model:show="showSlider" scene="profile" @success="onSliderSuccess" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import IconFont from '@/components/common/IconFont/index.vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import PasswordEyeIcon from '@/components/common/PasswordEyeIcon/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import SliderCaptcha from '@/components/business/SliderCaptcha/index.vue'
import { usePasswordForm } from '@/composables/usePasswordForm'

const showOldPassword = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)
const showSmsNewPassword = ref(false)
const showSmsConfirmPassword = ref(false)
const showEmailNewPassword = ref(false)
const showEmailConfirmPassword = ref(false)

const {
  saving,
  sendingSms,
  sendingEmail,
  smsCountdown,
  emailCountdown,
  mode,
  form,
  smsForm,
  emailForm,
  canUseSmsReset,
  canUseEmailReset,
  maskedMobile,
  maskedEmail,
  minPwdLen,
  showSlider,
  submit,
  sendSmsCode,
  sendEmailCode,
  onSliderSuccess,
  submitSmsReset,
  submitEmailReset,
  openSmsMode,
  openEmailMode,
  openSmsModeFromQuery,
  closeSmsMode,
  closeEmailMode,
} = usePasswordForm()

function onSubmit() {
  if (mode.value === 'password') {
    submit()
    return
  }
  if (mode.value === 'sms') {
    submitSmsReset()
    return
  }
  submitEmailReset()
}

onLoad((options) => {
  openSmsModeFromQuery(options?.mode)
})
</script>

<style lang="scss" scoped>
@use '@/styles/mine.scss' as *;

.password-page {
  @include mine-page-bg;
  min-height: 100vh;
  padding: $page-padding-y $page-padding-x calc(140rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.password-hero {
  @include mine-dark-hero-shell;
}

.password-hero__pattern {
  @include mine-dark-hero-pattern;
}

.password-hero__glow {
  @include mine-dark-hero-glow(rgba(124, 58, 237, 0.28));
}

.password-hero__body {
  @include mine-dark-hero-body;
}

.password-hero__text {
  @include mine-dark-hero-text;
}

.password-hero__title {
  @include mine-dark-hero-title;
}

.password-hero__subtitle {
  @include mine-dark-hero-sub;
}

.mode-tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: $card-gap;
}

.mode-tabs__item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  height: 80rpx;
  border-radius: $radius-lg;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-secondary;
  background: $color-bg-card;
  border: 1px solid $color-border-light;
  box-shadow: $shadow-card;
  transition: all 0.2s ease;

  &--active {
    color: #fff;
    background: linear-gradient(135deg, #4f46e5, #6366f1);
    border-color: transparent;
    box-shadow: 0 8rpx 24rpx rgba(79, 70, 229, 0.28);
  }
}

.sms-banner {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 28rpx;
  margin-bottom: $card-gap;
}

.sms-banner__title {
  display: block;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.sms-banner__sub {
  display: block;
  margin-top: 4rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.password-section {
  padding: 28rpx 28rpx 8rpx;
  margin-bottom: $card-gap;
}

.password-section__head {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 8rpx;
}

.password-section__title {
  font-size: $font-size-md;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.password-fields {
  display: flex;
  flex-direction: column;
}

.password-field {
  padding: 24rpx 0;
  border-bottom: 1px solid $color-border-light;

  &--last {
    border-bottom: none;
    padding-bottom: 16rpx;
  }
}

.password-field__label {
  display: block;
  margin-bottom: 12rpx;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  color: $color-text-secondary;
  letter-spacing: 0.02em;
}

.password-field__input-wrap {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 84rpx;
  padding: 0 12rpx 0 24rpx;
  border: 1px solid #e2e8f0;
  border-radius: 16rpx;
  background: #f8fafc;
  box-sizing: border-box;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:focus-within {
    border-color: #010710;
    background: #ffffff;
    box-shadow: 0 0 0 4rpx rgba(1, 7, 16, 0.06);
  }

  &--sms {
    flex: 1;
    min-width: 0;
    padding: 0 24rpx;
  }
}

.password-field__input {
  flex: 1;
  min-width: 0;
  height: 84rpx;
  line-height: 84rpx;
  padding: 0;
  border: none;
  background: transparent;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif !important;
  font-size: 28rpx;
  color: #0f172a;
  box-sizing: border-box;
}

.password-field__input--grow {
  flex: 1;
  min-width: 0;
}

.password-field__input::placeholder {
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif !important;
  color: #94a3b8 !important;
  font-size: 27rpx !important;
  font-weight: 400 !important;
}

.password-field__toggle {
  flex-shrink: 0;
  width: 52rpx;
  height: 52rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}

.password-field__toggle :deep(.password-eye-icon) {
  width: 36rpx;
  height: 36rpx;
}

.password-field__sms-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.password-field__sms-btn {
  flex-shrink: 0;
  min-width: 168rpx;
  height: 80rpx;
  line-height: 80rpx;
  margin: 0;
  padding: 0 20rpx;
  border-radius: $radius-md;
  background: $color-primary-muted;
  color: $color-primary;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  border: none;

  &::after {
    border: none;
  }

  &[disabled] {
    opacity: 0.55;
  }
}

.password-tips {
  padding: 28rpx;
}

.password-tips__title {
  display: block;
  margin-bottom: 16rpx;
  font-size: $font-size-sm;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.password-tips__item {
  display: flex;
  align-items: flex-start;
  gap: 8rpx;

  & + & {
    margin-top: 12rpx;
  }
}

.password-tips__dot {
  flex-shrink: 0;
  font-size: $font-size-lg;
  line-height: 1.2;
  color: $color-primary;
  font-weight: $font-weight-bold;
}

.password-tips__text {
  flex: 1;
  font-size: $font-size-sm;
  color: $color-text-regular;
  line-height: 1.55;
}

.password-save-btn {
  @include mine-primary-btn;
  width: 100%;
}
</style>
