<template>
  <view class="mobile-bind-page">
    <ListLoading v-if="loading" />

    <template v-else>
      <view class="bind-hero">
        <view class="bind-hero__pattern" />
        <view class="bind-hero__glow" />
        <view class="bind-hero__body">
          <ModuleIcon icon="phone-o" theme="cyan" size="lg" />
          <view class="bind-hero__text">
            <text class="bind-hero__title">{{ hasBoundMobile ? '当前绑定手机号' : '绑定手机号' }}</text>
            <text v-if="hasBoundMobile" class="bind-hero__mobile">{{ maskBoundMobile(currentMobile) }}</text>
            <text class="bind-hero__sub">
              {{ hasBoundMobile ? '更换后，登录与短信验证将使用新号码' : '绑定后可使用短信验证找回密码' }}
            </text>
          </view>
        </view>
      </view>

      <view v-if="!smsEnabled" class="bind-disabled card--elevated">
        <text class="bind-disabled__text">短信功能未启用，无法绑定或更换手机号，请联系管理员</text>
      </view>

      <view v-else class="bind-form card--elevated">
        <view class="bind-field">
          <text class="bind-field__label">{{ hasBoundMobile ? '新手机号' : '手机号' }}</text>
          <input
            v-model="form.bindMobile"
            class="bind-field__input"
            type="number"
            :maxlength="11"
            placeholder="请输入手机号"
          />
        </view>

        <view class="bind-field bind-field--last">
          <text class="bind-field__label">验证码</text>
          <view class="bind-field__sms">
            <input
              v-model="form.bindSmsCode"
              class="bind-field__input bind-field__input--grow"
              type="number"
              :maxlength="6"
              placeholder="请输入验证码"
            />
            <button
              class="bind-field__sms-btn"
              :disabled="bindSmsCountdown > 0 || sendingBindSms"
              :loading="sendingBindSms"
              @click="sendBindSmsCode"
            >
              {{ bindSmsCountdown > 0 ? `${bindSmsCountdown}s` : '获取验证码' }}
            </button>
          </view>
        </view>
      </view>
    </template>

    <PageFooter v-if="smsEnabled && !loading">
      <button class="page-footer__btn" :loading="bindingMobile" @click="submit">
        {{ hasBoundMobile ? '确认更换' : '确认绑定' }}
      </button>
    </PageFooter>

    <SliderCaptcha v-model:show="showSlider" scene="profile" @success="onSliderSuccess" />
  </view>
</template>

<script setup lang="ts">
import { watch, onMounted } from 'vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import SliderCaptcha from '@/components/business/SliderCaptcha/index.vue'
import { maskBoundMobile, useMobileBindForm } from '@/composables/useMobileBindForm'
import { useShallowStackBackFallback } from '@/composables/useShallowStackBackFallback'

const PROFILE_URL = '/pages-sub/mine/profile'

const {
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
} = useMobileBindForm()

useShallowStackBackFallback(PROFILE_URL)

watch(pageTitle, (title) => {
  uni.setNavigationBarTitle({ title })
}, { immediate: true })

onMounted(load)
</script>

<style lang="scss" scoped>
@use '@/styles/mine.scss' as *;

.mobile-bind-page {
  @include mine-page-bg;
  min-height: 100vh;
  padding: 24rpx 24rpx calc(140rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.bind-hero {
  @include mine-dark-hero-shell;
}

.bind-hero__pattern {
  @include mine-dark-hero-pattern;
}

.bind-hero__glow {
  @include mine-dark-hero-glow(rgba(8, 145, 178, 0.28));
}

.bind-hero__body {
  @include mine-dark-hero-body;
}

.bind-hero__text {
  @include mine-dark-hero-text;
}

.bind-hero__title {
  @include mine-dark-hero-title;
}

.bind-hero__mobile {
  display: block;
  margin-top: 10rpx;
  font-size: $font-size-2xl;
  font-weight: $font-weight-bold;
  color: #fff;
  letter-spacing: 2rpx;
}

.bind-hero__sub {
  @include mine-dark-hero-sub;
}

.bind-disabled {
  padding: 32rpx 28rpx;
  background: #ffffff;
  border-radius: 24rpx;
  text-align: center;
}

.bind-disabled__text {
  font-size: $font-size-sm;
  color: $color-text-secondary;
  line-height: 1.5;
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
  min-width: 168rpx;
  height: 80rpx;
  line-height: 80rpx;
  margin: 0;
  padding: 0 16rpx;
  border-radius: $radius-lg;
  background: #6366f1;
  color: #ffffff;
  font-size: $font-size-sm;
  font-weight: 600;

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
</style>
