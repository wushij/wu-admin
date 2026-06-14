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
  </view>
</template>

<script setup lang="ts">
import { watch, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
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
  load,
  sendBindSmsCode,
  submit,
} = useMobileBindForm()

useShallowStackBackFallback(PROFILE_URL)

watch(pageTitle, (title) => {
  uni.setNavigationBarTitle({ title })
}, { immediate: true })

onMounted(load)
onShow(load)
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
  padding: 32rpx;
}

.bind-disabled__text {
  font-size: $font-size-sm;
  color: $color-text-secondary;
  line-height: 1.6;
}

.bind-form {
  padding: 12rpx 0 8rpx;
}

.bind-field {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 28rpx 32rpx;
  border-bottom: 1px solid $color-border-light;

  &--last {
    border-bottom: none;
  }
}

.bind-field__label {
  flex-shrink: 0;
  width: 140rpx;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-secondary;
}

.bind-field__input {
  flex: 1;
  min-width: 0;
  height: 80rpx;
  padding: 0 24rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
  font-size: $font-size-base;
  color: $color-text-primary;
  text-align: left;
  box-sizing: border-box;
}

.bind-field__input::placeholder {
  font-size: $font-size-sm;
  color: $color-text-placeholder;
}

.bind-field__sms {
  flex: 1;
  min-width: 0;
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
  background: $color-primary-muted;
  color: $color-primary;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;

  &::after {
    border: none;
  }

  &[disabled] {
    opacity: 0.55;
  }
}
</style>
