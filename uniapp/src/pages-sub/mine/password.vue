<template>
  <view class="password-page">
    <view v-if="canUseSmsReset" class="mode-tabs">
      <view
        class="mode-tabs__item"
        :class="{ 'mode-tabs__item--active': mode === 'password' }"
        @click="closeSmsMode"
      >
        原密码修改
      </view>
      <view
        class="mode-tabs__item"
        :class="{ 'mode-tabs__item--active': mode === 'sms' }"
        @click="openSmsMode"
      >
        短信重置
      </view>
    </view>

    <view v-if="canUseSmsReset && mode === 'sms'" class="sms-tip">
      <IconFont name="phone-o" :size="32" color="#4f46e5" />
      <view class="sms-tip__text">
        <text class="sms-tip__title">已绑定 {{ maskedMobile }}</text>
        <text class="sms-tip__sub">验证码将发送至该手机号</text>
      </view>
    </view>

    <view class="form-card">
      <template v-if="mode === 'password'">
        <view class="field">
          <text class="field__label">原密码</text>
          <input v-model="form.oldPassword" class="field__input" password placeholder="请输入原密码" />
        </view>
        <view class="field">
          <text class="field__label">新密码</text>
          <input v-model="form.newPassword" class="field__input" password :placeholder="`至少 ${minPwdLen} 位`" />
        </view>
        <view class="field field--last">
          <text class="field__label">确认密码</text>
          <input v-model="form.confirmPassword" class="field__input" password placeholder="再次输入新密码" />
        </view>
      </template>

      <template v-else>
        <view class="field">
          <text class="field__label">验证码</text>
          <view class="field__row">
            <input v-model="smsForm.smsCode" class="field__input field__input--grow" :maxlength="6" placeholder="6 位验证码" />
            <button
              class="field__sms-btn"
              :disabled="smsCountdown > 0 || sendingSms"
              :loading="sendingSms"
              @click="sendSmsCode"
            >
              {{ smsCountdown > 0 ? `${smsCountdown}s` : '获取' }}
            </button>
          </view>
        </view>
        <view class="field">
          <text class="field__label">新密码</text>
          <input v-model="smsForm.newPassword" class="field__input" password :placeholder="`至少 ${minPwdLen} 位`" />
        </view>
        <view class="field field--last">
          <text class="field__label">确认密码</text>
          <input v-model="smsForm.confirmPassword" class="field__input" password placeholder="再次输入新密码" />
        </view>
      </template>
    </view>

    <view class="password-page__footer">
      <button class="password-page__submit" :loading="saving" @click="mode === 'password' ? submit() : submitSmsReset()">
        {{ mode === 'password' ? '确认修改' : '确认重置' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import IconFont from '@/components/common/IconFont/index.vue'
import { usePasswordForm } from '@/composables/usePasswordForm'

const {
  saving,
  sendingSms,
  smsCountdown,
  mode,
  form,
  smsForm,
  canUseSmsReset,
  maskedMobile,
  minPwdLen,
  submit,
  sendSmsCode,
  submitSmsReset,
  openSmsMode,
  openSmsModeFromQuery,
  closeSmsMode,
} = usePasswordForm()

onLoad((options) => {
  openSmsModeFromQuery(options?.mode)
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/mine.scss';

.password-page {
  @include mine-page-bg;
  min-height: 100vh;
  padding: 24rpx 24rpx calc(140rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.mode-tabs {
  display: flex;
  gap: 12rpx;
  margin-bottom: 24rpx;
  padding: 8rpx;
  @include mine-card;
}

.mode-tabs__item {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  border-radius: $radius-md;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-secondary;
  transition: all 0.2s ease;

  &--active {
    color: #fff;
    background: linear-gradient(135deg, #4f46e5, #6366f1);
    box-shadow: 0 8rpx 20rpx rgba(79, 70, 229, 0.25);
  }
}

.sms-tip {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 24rpx;
  padding: 24rpx 28rpx;
  @include mine-card;
}

.sms-tip__title {
  display: block;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.sms-tip__sub {
  display: block;
  margin-top: 4rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.form-card {
  padding: 8rpx 0;
  @include mine-card;
}

.field {
  padding: 28rpx 32rpx;
  border-bottom: 1px solid $color-border-light;

  &--last {
    border-bottom: none;
  }
}

.field__label {
  display: block;
  margin-bottom: 16rpx;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-secondary;
}

.field__input {
  width: 100%;
  height: 80rpx;
  padding: 0 24rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
  font-size: $font-size-md;
  color: $color-text-primary;
  box-sizing: border-box;
}

.field__input--grow {
  flex: 1;
}

.field__row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.field__sms-btn {
  flex-shrink: 0;
  min-width: 140rpx;
  height: 80rpx;
  line-height: 80rpx;
  margin: 0;
  padding: 0 20rpx;
  border-radius: $radius-md;
  background: $color-primary-muted;
  color: $color-primary;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
}

.password-page__footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
}

.password-page__submit {
  width: 100%;
  @include mine-primary-btn;
}
</style>
