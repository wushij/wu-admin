<template>
<view class="config-sms-wrap">
  <ConfigSectionCard title="基础配置">
    <view class="form-panel form-panel--flat">
      <FormCell label="启用短信" switch-cell>
        <switch :checked="smsDraft.enabled" :disabled="!canEdit" @change="onSmsSwitch('enabled', $event)" />
      </FormCell>
      <FormCell label="短信服务商" clickable boxed arrow @click="pickProvider">
        <text class="picker-value">{{ providerLabel }}</text>
      </FormCell>
      <FormCell v-model="smsDraft.accessKeyId" label="AccessKeyId" editable boxed password :disabled="!canEdit" />
      <FormCell v-model="smsDraft.accessKeySecret" label="AccessKeySecret" editable boxed password :disabled="!canEdit" />
      <FormCell v-model="smsDraft.signName" label="签名" editable boxed :disabled="!canEdit" />
      <FormCell v-if="smsDraft.provider === 'tencent'" v-model="smsDraft.tencentAppId" label="腾讯云 AppId" editable boxed password :disabled="!canEdit" />
      <FormCell v-if="smsDraft.provider === 'aliyunAuth'" label="验证码有效期(分钟)">
        <NumberStepper v-model="smsDraft.codeExpireMinutes" :min="1" :max="30" :disabled="!canEdit" />
      </FormCell>
    </view>
  </ConfigSectionCard>

  <ConfigSectionCard title="模板配置">
    <view class="form-panel form-panel--flat">
      <template v-if="smsDraft.provider === 'aliyunAuth'">
        <FormCell v-model="smsDraft.templateVerifyCode" label="登录/注册" editable boxed :disabled="!canEdit" />
        <FormCell v-model="smsDraft.templateModifyPhone" label="修改绑定手机" editable boxed :disabled="!canEdit" />
        <FormCell v-model="smsDraft.templateResetPassword" label="重置密码" editable boxed :disabled="!canEdit" />
        <FormCell v-model="smsDraft.templateBindPhone" label="绑定新手机" editable boxed :disabled="!canEdit" />
        <FormCell v-model="smsDraft.templateVerifyBindPhone" label="验证绑定手机" editable boxed :disabled="!canEdit" last />
      </template>
      <template v-else>
        <FormCell v-model="smsDraft.templateVerifyCode" label="验证码模板 ID" editable boxed :disabled="!canEdit" />
        <FormCell v-model="smsDraft.templateResetPassword" label="重置密码模板 ID" editable boxed :disabled="!canEdit" last />
      </template>
    </view>
  </ConfigSectionCard>

  <ConfigSectionCard v-if="canEdit" title="测试发送">
    <view class="form-panel form-panel--flat">
      <FormCell v-if="smsDraft.provider === 'aliyunAuth'" label="模板" clickable boxed arrow @click="pickTestTemplate">
        <text class="picker-value">{{ testTemplateLabel }}</text>
      </FormCell>
      <view class="sms-test__row">
        <input v-model="phone" class="sms-test__input" type="number" :maxlength="11" placeholder="11 位手机号" />
        <button class="sms-test__btn" :loading="smsTesting" @click="sendTestSms">发送</button>
      </view>
      <text class="config-hint config-hint--inline">将发送随机 6 位验证码，用于测试配置是否正确</text>
    </view>
  </ConfigSectionCard>

  <ConfigSectionCard title="发送记录">
    <template #extra>
      <text class="sms-logs__more" @click="openSmsLogs">查看全部</text>
    </template>
    <view class="sms-logs-list">
      <ListCard v-for="log in recentSmsLogs" :key="log.id || `${log.phone}-${log.createTime}`">
        <view class="list-card__top">
          <text class="list-card__title">{{ log.phone }}</text>
          <DictTag :label="smsStatusText(log.status)" :effect="log.status === 1 ? 'success' : log.status === 2 ? 'danger' : 'warning'" />
        </view>
        <text class="list-card__sub">验证码 {{ log.content || '—' }} · {{ log.createTime || '—' }}</text>
      </ListCard>
      <EmptyState v-if="!recentSmsLogs.length" title="暂无发送记录" icon="contact-o" />
    </view>
  </ConfigSectionCard>

  <view class="config-info">
    <text>填写密钥与模板后请点击页底「保存全部」，保存后再测试发送。</text>
  </view>
</view>
</template>

<script setup lang="ts">
import ConfigSectionCard from '@/components/business/system/ConfigSectionCard/index.vue'
import DictTag from '@/components/base/DictTag/index.vue'
import EmptyState from '@/components/base/EmptyState/index.vue'
import FormCell from '@/components/base/FormCell/index.vue'
import ListCard from '@/components/base/ListCard/index.vue'
import NumberStepper from '@/components/base/NumberStepper/index.vue'

import { computed } from 'vue'
import type { SmsAdminConfig, SmsLogRecord } from '@/types/config-types'

const props = defineProps<{
  smsDraft: SmsAdminConfig
  canEdit: boolean
  smsTesting: boolean
  providerLabel: string
  testTemplateLabel: string
  testSmsPhone: string
  recentSmsLogs: SmsLogRecord[]
  onSmsSwitch: (key: any, e: any) => void
  pickProvider: () => void
  pickTestTemplate: () => void
  sendTestSms: () => void
  openSmsLogs: () => void
  smsStatusText: (status: number | undefined) => string
}>()

const emit = defineEmits<{ 'update:testSmsPhone': [value: string] }>()

const phone = computed({
  get: () => props.testSmsPhone,
  set: (value: string) => emit('update:testSmsPhone', value),
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.config-sms-wrap {
  padding-bottom: 8rpx;
}

.sms-test__row {
  display: flex;
  gap: 16rpx;
  padding: 8rpx 32rpx 16rpx;
}

.sms-test__input {
  flex: 1;
  height: 72rpx;
  padding: 0 20rpx;
  border-radius: $radius-sm;
  background: $color-bg-muted;
  font-size: $font-size-base;
}

.sms-test__btn {
  flex-shrink: 0;
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 28rpx;
  border-radius: $radius-sm;
  background: $color-primary;
  color: #fff;
  font-size: $font-size-base;
}

.sms-logs-list {
  padding: 8rpx 0 16rpx;
}

.sms-logs__more {
  font-size: $font-size-sm;
  color: $color-primary;
  padding-right: 8rpx;
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
