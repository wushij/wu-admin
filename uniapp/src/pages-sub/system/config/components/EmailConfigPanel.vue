<template>
<view class="config-security-wrap">
  <ConfigSectionCard title="SMTP 基础配置">
    <view class="form-panel form-panel--flat">
      <FormCell label="启用邮件服务" switch-cell>
        <switch :checked="emailDraft.enabled" :disabled="!canEdit" @change="emailDraft.enabled = $event.detail.value" />
      </FormCell>
      <FormCell label="邮件服务商" clickable boxed arrow @click="pickEmailProvider">
        <text class="picker-value">{{ emailProviderLabel }}</text>
      </FormCell>
      <FormCell v-model="emailDraft.username" label="发件邮箱账号" editable boxed password placeholder="如 wu@gmail.com" :disabled="!canEdit" />
      <FormCell v-model="emailDraft.password" label="SMTP 授权码" editable boxed placeholder="秘钥/授权码" password :disabled="!canEdit" />
      <FormCell v-model="emailDraft.host" label="SMTP 服务器" editable boxed placeholder="如 smtp.qq.com" :disabled="!canEdit" />
      <FormCell label="SMTP 端口">
        <NumberStepper v-model="emailDraft.port" :min="1" :max="65535" :disabled="!canEdit" />
      </FormCell>
      <FormCell label="加密传输方式" clickable boxed arrow @click="pickEmailSecurityType">
        <text class="picker-value">{{ emailSecurityLabel }}</text>
      </FormCell>
      <FormCell label="SMTP 认证" switch-cell>
        <switch :checked="emailDraft.authEnabled" :disabled="!canEdit" @change="emailDraft.authEnabled = $event.detail.value" />
      </FormCell>
      <FormCell v-model="emailDraft.fromName" label="发件人显示名称" editable boxed placeholder="如 wu-admin 系统团队" :disabled="!canEdit" last />
    </view>
  </ConfigSectionCard>

  <ConfigSectionCard title="高级网络与超时配置">
    <view class="form-panel form-panel--flat">
      <FormCell label="连接超时(ms)">
        <NumberStepper v-model="emailDraft.connectionTimeoutMs" :min="1000" :max="30000" :step="1000" :disabled="!canEdit" />
      </FormCell>
      <FormCell label="读取超时(ms)">
        <NumberStepper v-model="emailDraft.timeoutMs" :min="1000" :max="30000" :step="1000" :disabled="!canEdit" />
      </FormCell>
      <FormCell label="写入超时(ms)">
        <NumberStepper v-model="emailDraft.writeTimeoutMs" :min="1000" :max="30000" :step="1000" :disabled="!canEdit" />
      </FormCell>
      <FormCell label="默认字符编码" clickable boxed arrow @click="pickEmailEncoding">
        <text class="picker-value">{{ emailEncodingLabel }}</text>
      </FormCell>
      <FormCell label="Debug 日志" switch-cell last>
        <switch :checked="emailDraft.debug" :disabled="!canEdit" @change="emailDraft.debug = $event.detail.value" />
      </FormCell>
    </view>
  </ConfigSectionCard>

  <ConfigSectionCard title="验证码与防刷规则">
    <view class="form-panel form-panel--flat">
      <FormCell label="验证码有效期">
        <NumberStepper v-model="emailDraft.codeExpireMinutes" :min="1" :max="30" :disabled="!canEdit" />
        <text class="config-unit">分钟</text>
      </FormCell>
      <FormCell label="验证码长度">
        <NumberStepper v-model="emailDraft.codeLength" :min="4" :max="8" :disabled="!canEdit" />
        <text class="config-unit">位</text>
      </FormCell>
      <FormCell label="防刷间隔">
        <NumberStepper v-model="emailDraft.sendIntervalSeconds" :min="10" :max="300" :disabled="!canEdit" />
        <text class="config-unit">秒</text>
      </FormCell>
      <FormCell label="单箱每日上限" last>
        <NumberStepper v-model="emailDraft.dailyLimitPerEmail" :min="1" :max="100" :disabled="!canEdit" />
        <text class="config-unit">次</text>
      </FormCell>
    </view>
  </ConfigSectionCard>

  <ConfigSectionCard title="测试发送邮件">
    <view class="form-panel form-panel--flat">
      <view class="sms-test__row">
        <input v-model="testEmail" class="sms-test__input" placeholder="请输入接收测试邮箱" />
        <button class="sms-test__btn" :disabled="!canEdit || emailTesting" @click="sendTestEmailAction">
          {{ emailTesting ? '发送中...' : '测试发送' }}
        </button>
      </view>
      <text class="config-hint config-hint--inline">发送测试报文到该邮箱，验证 SMTP 配置与安全端口连通性</text>
    </view>
  </ConfigSectionCard>

  <ConfigSectionCard title="发送记录">
    <template #extra>
      <text class="sms-logs__more" @click="openEmailLogs">查看全部</text>
    </template>
    <view class="sms-logs-list">
      <ListCard v-for="log in recentEmailLogs" :key="log.id || `${log.email}-${log.createTime}`">
        <view class="list-card__top">
          <text class="list-card__title">{{ log.email }}</text>
          <DictTag :label="emailStatusText(log.status)" :effect="log.status === 1 ? 'success' : log.status === 2 ? 'danger' : 'warning'" />
        </view>
        <text class="list-card__sub">{{ log.subject || '系统邮件' }} · {{ log.createTime || '—' }}</text>
      </ListCard>
      <EmptyState v-if="!recentEmailLogs.length" title="暂无发送记录" icon="contact-o" />
    </view>
  </ConfigSectionCard>
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
import type { EmailAdminConfig, EmailLogRecord } from '@/types/config-types'

const props = defineProps<{
  emailDraft: EmailAdminConfig
  canEdit: boolean
  emailTesting: boolean
  testEmailTo: string
  emailProviderLabel: string
  emailSecurityLabel: string
  emailEncodingLabel: string
  recentEmailLogs: EmailLogRecord[]
  pickEmailProvider: () => void
  pickEmailSecurityType: () => void
  pickEmailEncoding: () => void
  sendTestEmailAction: () => void
  openEmailLogs: () => void
  emailStatusText: (status: number | undefined) => string
}>()

const emit = defineEmits<{ 'update:testEmailTo': [value: string] }>()

const testEmail = computed({
  get: () => props.testEmailTo,
  set: (value: string) => emit('update:testEmailTo', value),
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

$config-label-width: 240rpx;
$config-label-width-rate: 280rpx;
$config-cell-gap: 20rpx;
$config-cell-padding-x: 24rpx;

.config-security-wrap {
  padding-bottom: 8rpx;
}

.config-security-wrap :deep(.form-cell__label-wrap) {
  width: $config-label-width-rate;
  flex: 1;
  min-width: 0;
}

.config-security-wrap :deep(.form-cell--switch) {
  align-items: flex-start;
}

.config-security-wrap :deep(.form-cell--switch .form-cell__body) {
  flex: none;
  padding-top: 6rpx;
}

.config-security-wrap :deep(.form-cell__hint) {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  line-height: 1.5;
  white-space: normal;
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

.config-unit {
  flex-shrink: 0;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  white-space: nowrap;
}
</style>
