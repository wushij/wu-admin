<template>
<view class="form-panel form-panel--rate">
  <FormCell label="验证码(次/分/IP)">
    <NumberStepper v-model="rateDraft.captchaPerIpMinute" :min="0" :max="200" :disabled="!canEdit" />
  </FormCell>
  <FormCell label="登录(次/分/IP)">
    <NumberStepper v-model="rateDraft.loginPerIpMinute" :min="0" :max="200" :disabled="!canEdit" />
  </FormCell>
  <FormCell label="注册(次/分/IP)">
    <NumberStepper v-model="rateDraft.registerPerIpMinute" :min="0" :max="200" :disabled="!canEdit" />
  </FormCell>
  <text class="config-section">应用层滑动窗口（兜底限流）</text>
  <FormCell label="业务 API(次/分/IP)">
    <NumberStepper
      :model-value="rateDraft.apiPerIpMinute ?? 240"
      :min="0"
      :max="2000"
      :disabled="!canEdit"
      @update:model-value="rateDraft.apiPerIpMinute = $event"
    />
  </FormCell>
  <FormCell label="认证接口(次/分/IP)">
    <NumberStepper
      :model-value="rateDraft.authPerIpMinute ?? 60"
      :min="0"
      :max="600"
      :disabled="!canEdit"
      @update:model-value="rateDraft.authPerIpMinute = $event"
    />
  </FormCell>
  <FormCell label="文件访问(次/分/IP)">
    <NumberStepper
      :model-value="rateDraft.filesPerIpMinute ?? 600"
      :min="0"
      :max="5000"
      :disabled="!canEdit"
      @update:model-value="rateDraft.filesPerIpMinute = $event"
    />
  </FormCell>
  <text class="config-section">短信发送防刷</text>
  <FormCell label="短信(次/分/IP)">
    <NumberStepper v-model="rateDraft.smsPerIpMinute" :min="0" :max="200" :disabled="!canEdit" />
  </FormCell>
  <FormCell label="同号发送间隔(秒)">
    <NumberStepper v-model="rateDraft.smsSendIntervalSeconds" :min="30" :max="300" :disabled="!canEdit" />
  </FormCell>
  <FormCell label="同号每日上限(次)">
    <NumberStepper v-model="rateDraft.smsPerPhoneDaily" :min="0" :max="500" :disabled="!canEdit" />
  </FormCell>
  <FormCell label="同IP每日上限(次)">
    <NumberStepper v-model="rateDraft.smsPerIpDaily" :min="0" :max="500" :disabled="!canEdit" />
  </FormCell>
  <text class="config-section">AI 对话限流</text>
  <FormCell label="AI对话(次/分/用户)" last>
    <NumberStepper v-model="rateDraft.aiChatPerUserMinute" :min="0" :max="200" :disabled="!canEdit" />
  </FormCell>
</view>
</template>

<script setup lang="ts">
import FormCell from '@/components/base/FormCell/index.vue'
import NumberStepper from '@/components/base/NumberStepper/index.vue'

import type { RateLimitConfig } from '@/types/config-types'

defineProps<{
  rateDraft: RateLimitConfig
  canEdit: boolean
}>()
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.config-section {
  display: block;
  padding: 24rpx 24rpx 12rpx;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  background: $color-bg-muted;
  border-top: 1px solid $color-border-light;
}

.config-section:first-child {
  border-top: none;
}
</style>
