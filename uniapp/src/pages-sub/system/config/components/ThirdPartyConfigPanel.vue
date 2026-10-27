<template>
<view class="config-collapse-wrap">
  <CollapsePanel title="微信登录">
    <view class="form-panel form-panel--flat">
      <FormCell label="启用" switch-cell><switch :checked="thirdDraft.wechat.enabled" :disabled="!canEdit" @change="onThirdSwitch('wechat', 'enabled', $event)" /></FormCell>
      <FormCell v-model="thirdDraft.wechat.appId" label="AppID" editable boxed :disabled="!canEdit" />
      <FormCell v-model="thirdDraft.wechat.appSecret" label="AppSecret" editable boxed password :disabled="!canEdit" last />
    </view>
  </CollapsePanel>
  <CollapsePanel title="支付宝登录">
    <view class="form-panel form-panel--flat">
      <FormCell label="启用" switch-cell><switch :checked="thirdDraft.alipay.enabled" :disabled="!canEdit" @change="onThirdSwitch('alipay', 'enabled', $event)" /></FormCell>
      <FormCell v-model="thirdDraft.alipay.appId" label="AppID" editable boxed :disabled="!canEdit" />
      <view class="config-textarea">
        <text class="config-textarea__label">应用私钥</text>
        <textarea v-model="thirdDraft.alipay.privateKey" class="config-textarea__input" :disabled="!canEdit" />
      </view>
      <view class="config-textarea">
        <text class="config-textarea__label">支付宝公钥</text>
        <textarea v-model="thirdDraft.alipay.publicKey" class="config-textarea__input" :disabled="!canEdit" />
      </view>
    </view>
  </CollapsePanel>
  <CollapsePanel title="GitHub 登录">
    <view class="form-panel form-panel--flat">
      <FormCell label="启用" switch-cell><switch :checked="thirdDraft.github.enabled" :disabled="!canEdit" @change="onThirdSwitch('github', 'enabled', $event)" /></FormCell>
      <FormCell v-model="thirdDraft.github.clientId" label="Client ID" editable boxed :disabled="!canEdit" />
      <FormCell v-model="thirdDraft.github.clientSecret" label="Client Secret" editable boxed password :disabled="!canEdit" last />
    </view>
  </CollapsePanel>
  <CollapsePanel title="Google 登录">
    <view class="form-panel form-panel--flat">
      <FormCell label="启用" switch-cell><switch :checked="thirdDraft.google.enabled" :disabled="!canEdit" @change="onThirdSwitch('google', 'enabled', $event)" /></FormCell>
      <FormCell v-model="thirdDraft.google.clientId" label="Client ID" editable boxed :disabled="!canEdit" />
      <FormCell v-model="thirdDraft.google.clientSecret" label="Client Secret" editable boxed password :disabled="!canEdit" />
      <FormCell v-model="thirdDraft.google.redirectUri" label="重定向 URI" editable boxed :disabled="!canEdit" last />
      <text class="config-hint config-hint--inline">须在 Google Cloud 控制台配置相同重定向 URI</text>
    </view>
  </CollapsePanel>
</view>
</template>

<script setup lang="ts">
import CollapsePanel from '@/components/base/CollapsePanel/index.vue'
import FormCell from '@/components/base/FormCell/index.vue'

import type { ThirdPartyConfig } from '@/types/config-types'

defineProps<{
  thirdDraft: ThirdPartyConfig
  canEdit: boolean
  onThirdSwitch: (provider: any, key: string, e: any) => void
}>()
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.config-textarea {
  padding: 20rpx 32rpx 24rpx;
  border-bottom: 1px solid $color-border-light;
  background: $color-bg-card;
}

.config-textarea__label {
  display: block;
  margin-bottom: 12rpx;
  font-size: $font-size-md;
  color: $color-text-primary;
  font-weight: $font-weight-semibold;
}

.config-textarea__input {
  width: 100%;
  min-height: 180rpx;
  padding: 20rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
  border: 1px solid $color-border-light;
  font-size: $font-size-sm;
  line-height: 1.55;
  box-sizing: border-box;
}

.config-textarea__mask {
  -webkit-text-security: disc;
}

.config-hint {
  display: block;
  padding: 0 32rpx 20rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  line-height: 1.55;
  background: $color-bg-muted;
}

.config-hint--inline {
  display: block;
  padding: 0 32rpx 20rpx;
  background: $color-bg-card;
}
</style>
