<template>
  <view class="email-bind-page">
    <ListLoading v-if="loading" />

    <template v-else>
      <view class="bind-hero">
        <view class="bind-hero__pattern" />
        <view class="bind-hero__glow" />
        <view class="bind-hero__body">
          <view class="bind-hero__icon">
            <svg class="email-svg-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M3 6.5A2.5 2.5 0 0 1 5.5 4h13A2.5 2.5 0 0 1 21 6.5v11a2.5 2.5 0 0 1-2.5 2.5h-13A2.5 2.5 0 0 1 3 17.5v-11z" stroke="#ffffff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M3.8 6.8l7.5 5.5c.4.3 1 .3 1.4 0l7.5-5.5" stroke="#ffffff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </view>
          <view class="bind-hero__text">
            <text class="bind-hero__title">{{ hasBoundEmail ? '当前绑定邮箱' : '绑定邮箱' }}</text>
            <text v-if="hasBoundEmail" class="bind-hero__email">{{ maskBoundEmail(currentEmail) }}</text>
            <text class="bind-hero__sub">
              {{ hasBoundEmail ? '更换后，系统通知与安全验证将使用新邮箱' : '绑定后可接收安全验证码与系统通知' }}
            </text>
          </view>
        </view>
      </view>

      <view class="bind-form card--elevated">
        <view class="bind-field">
          <text class="bind-field__label">{{ hasBoundEmail ? '新邮箱地址' : '邮箱地址' }}</text>
          <input
            v-model="form.bindEmail"
            class="bind-field__input"
            type="text"
            :maxlength="64"
            placeholder="请输入新邮箱地址"
          />
        </view>

        <view class="bind-field bind-field--last">
          <text class="bind-field__label">邮箱验证码</text>
          <view class="bind-field__sms">
            <input
              v-model="form.bindEmailCode"
              class="bind-field__input bind-field__input--grow"
              type="number"
              :maxlength="6"
              placeholder="请输入 6 位验证码"
            />
            <button
              class="bind-field__sms-btn"
              :disabled="emailCodeCountdown > 0 || sendingEmailCode"
              :loading="sendingEmailCode"
              @click="sendEmailBindCodeAction"
            >
              {{ emailCodeCountdown > 0 ? `${emailCodeCountdown}s` : '获取验证码' }}
            </button>
          </view>
        </view>
      </view>
    </template>

    <PageFooter v-if="!loading">
      <button class="page-footer__btn" :loading="bindingEmail" @click="submit">
        {{ hasBoundEmail ? '确认更换' : '确认绑定' }}
      </button>
    </PageFooter>
  </view>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import { maskBoundEmail, useEmailBindForm } from '@/composables/useEmailBindForm'
import { useShallowStackBackFallback } from '@/composables/useShallowStackBackFallback'

const PROFILE_URL = '/pages-sub/mine/profile'

const {
  loading,
  bindingEmail,
  sendingEmailCode,
  emailCodeCountdown,
  currentEmail,
  form,
  hasBoundEmail,
  load,
  sendEmailBindCodeAction,
  submit,
} = useEmailBindForm()

useShallowStackBackFallback(PROFILE_URL)

onMounted(load)
</script>

<style lang="scss" scoped>
@use '@/styles/mine.scss' as *;

.email-bind-page {
  @include mine-page-bg;
  min-height: 100vh;
  padding: $page-padding-y $page-padding-x calc(160rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.bind-hero {
  position: relative;
  margin-bottom: $section-gap;
  border-radius: $radius-xl;
  overflow: hidden;
  background: linear-gradient(135deg, #010710 0%, #0f1a2e 55%, #1e293b 100%);
  box-shadow: 0 12rpx 40rpx rgba(0, 0, 0, 0.14);
}

.bind-hero__pattern {
  position: absolute;
  inset: 0;
  opacity: 0.07;
  background-image: radial-gradient(rgba(255, 255, 255, 0.8) 1px, transparent 1px);
  background-size: 32rpx 32rpx;
  pointer-events: none;
}

.bind-hero__glow {
  position: absolute;
  top: -30%;
  right: -8%;
  width: 260rpx;
  height: 260rpx;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.28) 0%, transparent 70%);
  pointer-events: none;
}

.bind-hero__body {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 40rpx 32rpx;
}

.bind-hero__icon {
  position: relative;
  width: 88rpx;
  height: 88rpx;
  border-radius: 26rpx;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 8rpx 24rpx rgba(99, 102, 241, 0.35);
  overflow: hidden;
}

.email-svg-icon {
  width: 44rpx;
  height: 44rpx;
}

.bind-hero__text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.bind-hero__title {
  font-size: $font-size-md;
  font-weight: $font-weight-bold;
  color: #ffffff;
  margin-bottom: 6rpx;
}

.bind-hero__email {
  font-size: $font-size-lg;
  font-weight: 700;
  color: #818cf8;
  letter-spacing: 0.02em;
  margin-bottom: 8rpx;
}

.bind-hero__sub {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.4;
}

.bind-form {
  padding: 16rpx 28rpx;
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(15, 23, 42, 0.05);
}

.bind-field {
  padding: 24rpx 0;
  border-bottom: 1px solid $color-border-light;

  &--last {
    border-bottom: none;
  }
}

.bind-field__label {
  display: block;
  margin-bottom: 14rpx;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  color: $color-text-secondary;
  letter-spacing: 0.02em;
}

.bind-field__input {
  width: 100%;
  min-height: 84rpx;
  padding: 0 24rpx;
  border: 1px solid #e2e8f0;
  border-radius: 16rpx;
  background: #f8fafc;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif !important;
  font-size: 28rpx;
  color: #0f172a;
  box-sizing: border-box;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:focus {
    border-color: #010710;
    background: #ffffff;
    box-shadow: 0 0 0 4rpx rgba(1, 7, 16, 0.06);
  }
}

.bind-field__sms {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.bind-field__input--grow {
  flex: 1;
  min-width: 0;
}

.bind-field__sms-btn {
  flex-shrink: 0;
  min-width: 180rpx;
  height: 84rpx;
  line-height: 84rpx;
  margin: 0;
  padding: 0 24rpx;
  border-radius: 16rpx;
  background: #6366f1;
  color: #ffffff;
  font-size: $font-size-sm;
  font-weight: 600;
  border: none;

  &::after {
    border: none;
  }

  &[disabled] {
    background: #f1f5f9;
    color: #334155;
    font-weight: 700;
    opacity: 1;
  }
}

.page-footer__btn {
  @include mine-primary-btn;
  width: 100%;
}
</style>
