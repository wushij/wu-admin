<template>
  <view class="mobile-bind-page">
    <ListLoading v-if="loading" />

    <template v-else>
      <view v-if="hasBoundMobile" class="bind-hero card--elevated">
        <view class="bind-hero__icon">
          <IconFont name="phone-o" :size="40" color="#4f46e5" />
        </view>
        <text class="bind-hero__title">当前绑定手机号</text>
        <text class="bind-hero__mobile">{{ maskBoundMobile(currentMobile) }}</text>
        <text class="bind-hero__sub">更换后，登录与短信验证将使用新号码</text>
      </view>

      <view v-else class="bind-hero bind-hero--empty card--elevated">
        <view class="bind-hero__icon">
          <IconFont name="phone-o" :size="40" color="#4f46e5" />
        </view>
        <text class="bind-hero__title">绑定手机号</text>
        <text class="bind-hero__sub">绑定后可使用短信验证找回密码</text>
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
            placeholder="请输入 11 位手机号"
          />
        </view>
        <view class="bind-field bind-field--last">
          <text class="bind-field__label">验证码</text>
          <view class="bind-field__sms">
            <input
              v-model="form.bindSmsCode"
              class="bind-field__input bind-field__input--grow"
              :maxlength="6"
              placeholder="6 位验证码"
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
import IconFont from '@/components/common/IconFont/index.vue'
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
@import '@/styles/variables.scss';
@import '@/styles/mine.scss';
@import '@/styles/common.scss';

.mobile-bind-page {
  @include mine-page-bg;
  min-height: 100vh;
  padding: 24rpx 24rpx calc(140rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.bind-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 40rpx 40rpx;
  margin-bottom: 24rpx;
  text-align: center;
}

.bind-hero__icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.12), rgba(129, 140, 248, 0.18));
  margin-bottom: 24rpx;
}

.bind-hero__title {
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.bind-hero__mobile {
  margin-top: 16rpx;
  font-size: 44rpx;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  letter-spacing: 2rpx;
}

.bind-hero__sub {
  margin-top: 16rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  line-height: 1.55;
  max-width: 520rpx;
}

.bind-disabled {
  padding: 32rpx;
  text-align: center;
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
  padding: 28rpx 32rpx;
  border-bottom: 1px solid $color-border-light;

  &--last {
    border-bottom: none;
  }
}

.bind-field__label {
  display: block;
  margin-bottom: 16rpx;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-secondary;
  text-align: center;
}

.bind-field__input {
  width: 100%;
  height: 88rpx;
  padding: 0 28rpx;
  border-radius: $radius-lg;
  background: $color-bg-muted;
  font-size: $font-size-md;
  color: $color-text-primary;
  text-align: center;
  box-sizing: border-box;
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
  min-width: 188rpx;
  height: 88rpx;
  line-height: 88rpx;
  margin: 0;
  padding: 0 20rpx;
  border-radius: $radius-lg;
  background: $color-primary-muted;
  color: $color-primary;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;

  &::after {
    border: none;
  }
}
</style>
