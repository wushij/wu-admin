<template>
<view class="form-panel">
  <FormCell label="登录人机校检" switch-cell>
    <switch :checked="loginDraft.captchaEnabled" :disabled="!canEdit" @change="onLoginSwitch('captchaEnabled', $event)" />
  </FormCell>
  <FormCell v-if="loginDraft.captchaEnabled" label="验证码类型">
    <ConfigRadioGroup
      v-model="loginDraft.captchaType"
      :options="captchaTypeOptions"
      :disabled="!canEdit"
    />
  </FormCell>
  <FormCell label="短信验证码登录" switch-cell>
    <switch :checked="loginDraft.smsLoginEnabled" :disabled="!canEdit" @change="onLoginSwitch('smsLoginEnabled', $event)" />
  </FormCell>
  <FormCell v-if="loginDraft.smsLoginEnabled" label="发送前滑块验证" switch-cell>
    <switch :checked="loginDraft.smsLoginSliderCaptchaEnabled" :disabled="!canEdit" @change="onLoginSwitch('smsLoginSliderCaptchaEnabled', $event)" />
  </FormCell>
  <view v-if="loginDraft.smsLoginEnabled && !smsDraft.enabled" class="config-warn">
    <text>请先在「短信」中开启短信功能，否则无法保存</text>
  </view>
  <FormCell label="邮箱验证码登录" switch-cell>
    <switch :checked="loginDraft.emailLoginEnabled" :disabled="!canEdit" @change="onLoginSwitch('emailLoginEnabled', $event)" />
  </FormCell>
  <FormCell v-if="loginDraft.emailLoginEnabled" label="邮箱发送前滑块" switch-cell>
    <switch :checked="loginDraft.emailLoginSliderCaptchaEnabled" :disabled="!canEdit" @change="onLoginSwitch('emailLoginSliderCaptchaEnabled', $event)" />
  </FormCell>
  <view v-if="loginDraft.emailLoginEnabled && !emailDraft.enabled" class="config-warn">
    <text>请先在「邮件」中开启邮件功能，否则无法保存</text>
  </view>
  <FormCell label="记住我" switch-cell>
    <switch :checked="loginDraft.rememberMe" :disabled="!canEdit" @change="onLoginSwitch('rememberMe', $event)" />
  </FormCell>
  <FormCell label="账号最大重试">
    <NumberStepper v-model="loginDraft.maxRetryCount" :min="1" :max="20" :disabled="!canEdit" />
  </FormCell>
  <FormCell label="IP 最大重试">
    <NumberStepper v-model="loginDraft.maxRetryCountIp" :min="1" :max="50" :disabled="!canEdit" />
  </FormCell>
  <FormCell label="锁定时长" last>
    <view class="config-control-row">
      <NumberStepper v-model="loginDraft.lockTime" :min="1" :max="120" :disabled="!canEdit" />
      <text class="config-unit">分钟</text>
    </view>
  </FormCell>
</view>
</template>

<script setup lang="ts">
import ConfigRadioGroup from '@/components/business/system/ConfigRadioGroup/index.vue'
import FormCell from '@/components/base/FormCell/index.vue'
import NumberStepper from '@/components/base/NumberStepper/index.vue'

import type { EmailAdminConfig, LoginAdminConfig, SmsAdminConfig } from '@/types/config-types'

defineProps<{
  loginDraft: LoginAdminConfig
  smsDraft: SmsAdminConfig
  emailDraft: EmailAdminConfig
  canEdit: boolean
  captchaTypeOptions: { label: string; value: string }[]
  onLoginSwitch: (key: any, e: any) => void
}>()
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.config-warn {
  margin: 0 24rpx 16rpx;
  padding: 16rpx 20rpx;
  border-radius: $radius-md;
  background: rgba(230, 162, 60, 0.12);
  border: 1px solid rgba(230, 162, 60, 0.22);
  font-size: $font-size-xs;
  color: $color-warning;
  line-height: 1.55;
}

.config-control-row {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12rpx;
  width: 100%;
  flex-wrap: nowrap;
}

.config-control-row--end {
  justify-content: flex-end;
}

.config-unit {
  flex-shrink: 0;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  white-space: nowrap;
}
</style>
