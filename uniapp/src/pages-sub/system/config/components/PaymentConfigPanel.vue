<template>
<view class="config-collapse-wrap">
  <CollapsePanel title="微信支付">
    <view class="form-panel form-panel--flat">
      <FormCell label="启用" switch-cell><switch :checked="paymentDraft.wechatPay.enabled" :disabled="!canEdit" @change="onPaySwitch('wechatPay', 'enabled', $event)" /></FormCell>
      <FormCell v-model="paymentDraft.wechatPay.mchId" label="商户号" editable boxed password :disabled="!canEdit" />
      <FormCell v-model="paymentDraft.wechatPay.appId" label="AppID" editable boxed password :disabled="!canEdit" />
      <FormCell v-model="paymentDraft.wechatPay.apiV3Key" label="APIv3 密钥" editable boxed password :disabled="!canEdit" />
      <view class="config-textarea">
        <text class="config-textarea__label">商户私钥</text>
        <textarea v-model="paymentDraft.wechatPay.privateKey" class="config-textarea__input config-textarea__mask" :disabled="!canEdit" />
      </view>
      <FormCell v-model="paymentDraft.wechatPay.certSerialNo" label="证书序列号" editable boxed password :disabled="!canEdit" />
      <FormCell v-model="paymentDraft.wechatPay.notifyUrl" label="回调地址" editable boxed :disabled="!canEdit" last />
      <text class="config-hint config-hint--inline">须公网 HTTPS，对应 POST /api/pay/notify/wechat</text>
      <button v-if="canEdit && paymentDraft.wechatPay.enabled" class="config-test-btn" :loading="paymentTesting" @click="sendTestPayment('wechat')">测试微信支付</button>
    </view>
  </CollapsePanel>
  <CollapsePanel title="支付宝支付">
    <view class="form-panel form-panel--flat">
      <FormCell label="启用" switch-cell><switch :checked="paymentDraft.alipay.enabled" :disabled="!canEdit" @change="onPaySwitch('alipay', 'enabled', $event)" /></FormCell>
      <FormCell v-model="paymentDraft.alipay.appId" label="AppID" editable boxed password :disabled="!canEdit" />
      <view class="config-textarea">
        <text class="config-textarea__label">应用私钥</text>
        <textarea v-model="paymentDraft.alipay.privateKey" class="config-textarea__input config-textarea__mask" :disabled="!canEdit" />
      </view>
      <view class="config-textarea">
        <text class="config-textarea__label">支付宝公钥</text>
        <textarea v-model="paymentDraft.alipay.publicKey" class="config-textarea__input config-textarea__mask" :disabled="!canEdit" />
      </view>
      <FormCell label="签名类型" clickable boxed arrow @click="pickAlipaySign">
        <text class="picker-value">{{ paymentDraft.alipay.signType }}</text>
      </FormCell>
      <FormCell label="网关环境" clickable boxed arrow @click="pickAlipayGateway">
        <text class="picker-value">{{ alipayGatewayLabel }}</text>
      </FormCell>
      <FormCell v-model="paymentDraft.alipay.notifyUrl" label="回调地址" editable boxed :disabled="!canEdit" />
      <FormCell v-model="paymentDraft.alipay.returnUrl" label="同步跳转" editable boxed :disabled="!canEdit" last />
      <text class="config-hint config-hint--inline">须公网 HTTPS，对应 POST /api/pay/notify/alipay</text>
      <button v-if="canEdit && paymentDraft.alipay.enabled" class="config-test-btn" :loading="paymentTesting" @click="sendTestPayment('alipay')">测试支付宝</button>
    </view>
  </CollapsePanel>
  <view class="config-info">
    <text>填写后务必点击页底「保存全部」。</text>
    <text>测试订单金额固定 0.01 元，保存配置后再生成订单。</text>
  </view>
</view>
</template>

<script setup lang="ts">
import CollapsePanel from '@/components/base/CollapsePanel/index.vue'
import FormCell from '@/components/base/FormCell/index.vue'

import type { PaymentConfig } from '@/types/config-types'

defineProps<{
  paymentDraft: PaymentConfig
  canEdit: boolean
  paymentTesting: boolean
  alipayGatewayLabel: string
  onPaySwitch: (provider: any, key: string, e: any) => void
  pickAlipaySign: () => void
  pickAlipayGateway: () => void
  sendTestPayment: (type: 'wechat' | 'alipay') => void
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

.config-test-btn {
  margin: 16rpx 32rpx;
  background: $color-primary;
  color: #fff;
  border-radius: $radius-md;
  font-size: $font-size-base;
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

.config-info {
  margin-top: 8rpx;
  padding: 20rpx 24rpx;
  border-radius: $radius-md;
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.18);
  font-size: $font-size-xs;
  color: $color-text-secondary;
  line-height: 1.65;

  text {
    display: block;
  }
}
</style>
