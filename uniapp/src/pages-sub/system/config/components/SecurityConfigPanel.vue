<template>
<view class="config-security-wrap">
  <ConfigSectionCard title="前端安全">
    <view class="form-panel form-panel--flat">
      <FormCell
        label="禁止前端调试"
        hint="开启后将限制打开开发者工具，需刷新页面生效"
        switch-cell
        last
      >
        <switch :checked="securityDraft.disableDevtool" :disabled="!canEdit" @change="onSecuritySwitch('disableDevtool', $event)" />
      </FormCell>
    </view>
  </ConfigSectionCard>
  <ConfigSectionCard title="会话安全">
    <view class="form-panel form-panel--flat">
      <FormCell
        label="禁止多端同时在线"
        hint="开启后，同一账号再次登录会先踢掉之前的会话"
        switch-cell
        last
      >
        <switch :checked="forbidConcurrentLogin" :disabled="!canEdit" @change="onForbidConcurrentChange" />
      </FormCell>
    </view>
  </ConfigSectionCard>
  <ConfigSectionCard title="API 安全防线与防重放">
    <view class="form-panel form-panel--flat">
      <FormCell
        label="SM2 密码加密"
        hint="是否启用登录/注册/重置密码非对称加密传输（国密 SM2 椭圆公钥加密，默认开启）"
        switch-cell
      >
        <switch :checked="securityDraft.sm2EncryptEnabled ?? true" :disabled="!canEdit" @change="onSecuritySwitch('sm2EncryptEnabled', $event)" />
      </FormCell>
      <FormCell
        label="SM4 数据加密"
        hint="是否启用接口请求/响应数据加密（国密 SM4-GCM 认证加密，IV[12] + 密文 + Tag[16]）"
        switch-cell
      >
        <switch :checked="securityDraft.sm4EncryptEnabled" :disabled="!canEdit" @change="onSecuritySwitch('sm4EncryptEnabled', $event)" />
      </FormCell>
      <FormCell
        label="数字签名验签"
        hint="是否启用接口签名验签（国密 HMAC-SM3 高性能签名防篡改）"
        switch-cell
      >
        <switch :checked="securityDraft.sm2SignEnabled" :disabled="!canEdit" @change="onSecuritySwitch('sm2SignEnabled', $event)" />
      </FormCell>
      <FormCell
        label="时间戳校验"
        hint="校验请求时间，防止过期请求（5分钟时间窗口）"
        switch-cell
      >
        <switch :checked="securityDraft.timestampEnabled ?? true" :disabled="!canEdit" @change="onSecuritySwitch('timestampEnabled', $event)" />
      </FormCell>
      <FormCell
        label="Nonce 校验"
        hint="校验随机数，防止高频重放攻击（Redis 查重）"
        switch-cell
        last
      >
        <switch :checked="securityDraft.nonceEnabled ?? true" :disabled="!canEdit" @change="onSecuritySwitch('nonceEnabled', $event)" />
      </FormCell>
    </view>
  </ConfigSectionCard>
  <ConfigSectionCard title="行为风控与分级处置">
    <view class="form-panel form-panel--flat">
      <FormCell
        label="启用风控"
        hint="总开关，关闭后不再累加风险分，封禁标记也失效"
        switch-cell
      >
        <switch
          :checked="securityDraft.riskEnabled ?? true"
          :disabled="!canEdit"
          @change="onSecuritySwitch('riskEnabled', $event)"
        />
      </FormCell>
      <FormCell label="滑动窗口" hint="分钟。只统计窗口内的事件，攻击停止后分数自动回落，默认 60">
        <NumberStepper
          :model-value="securityDraft.riskWindowMinutes ?? 60"
          :min="5"
          :max="1440"
          :step="10"
          :disabled="!canEdit"
          @update:model-value="securityDraft.riskWindowMinutes = $event"
        />
      </FormCell>
      <FormCell label="关注阈值" hint="达到后记录事件便于回溯，默认 30">
        <NumberStepper
          :model-value="securityDraft.riskNoticeScore ?? 30"
          :min="10"
          :max="2000"
          :step="10"
          :disabled="!canEdit"
          @update:model-value="securityDraft.riskNoticeScore = $event"
        />
      </FormCell>
      <FormCell label="警告阈值" hint="达到后按配置推送告警，默认 50">
        <NumberStepper
          :model-value="securityDraft.riskWarnScore ?? 50"
          :min="10"
          :max="3000"
          :step="10"
          :disabled="!canEdit"
          @update:model-value="securityDraft.riskWarnScore = $event"
        />
      </FormCell>
      <FormCell label="挑战阈值" hint="达到后进入挑战档，默认 70">
        <NumberStepper
          :model-value="securityDraft.riskChallengeScore ?? 70"
          :min="20"
          :max="4000"
          :step="10"
          :disabled="!canEdit"
          @update:model-value="securityDraft.riskChallengeScore = $event"
        />
      </FormCell>
      <FormCell label="临时封禁阈值" hint="风险分达到后临时封禁，默认 90">
        <NumberStepper
          :model-value="securityDraft.riskBlockScore ?? 90"
          :min="20"
          :max="5000"
          :step="10"
          :disabled="!canEdit"
          @update:model-value="securityDraft.riskBlockScore = $event"
        />
      </FormCell>
      <FormCell
        label="最高档阈值"
        hint="风险分达到后按最高档封禁，必须依次高于前四个阈值，默认 120"
      >
        <NumberStepper
          :model-value="securityDraft.riskBanScore ?? 120"
          :min="40"
          :max="10000"
          :step="10"
          :disabled="!canEdit"
          @update:model-value="securityDraft.riskBanScore = $event"
        />
      </FormCell>
      <FormCell label="临时封禁时长" hint="秒，默认 3600（1 小时）">
        <NumberStepper
          :model-value="securityDraft.riskBlockSeconds ?? 3600"
          :min="60"
          :max="604800"
          :step="600"
          :disabled="!canEdit"
          @update:model-value="securityDraft.riskBlockSeconds = $event"
        />
      </FormCell>
      <FormCell label="最高档封禁时长" hint="秒，默认 86400（24 小时）">
        <NumberStepper
          :model-value="securityDraft.riskBanSeconds ?? 86400"
          :min="60"
          :max="2592000"
          :step="3600"
          :disabled="!canEdit"
          @update:model-value="securityDraft.riskBanSeconds = $event"
        />
      </FormCell>
      <FormCell
        label="启用挑战"
        hint="开启后高风险主体发起写操作需先完成滑块验证（携带 X-Slider-Ticket 重放原请求），默认关闭"
        switch-cell
      >
        <switch
          :checked="securityDraft.riskChallengeEnabled ?? false"
          :disabled="!canEdit"
          @change="onSecuritySwitch('riskChallengeEnabled', $event)"
        />
      </FormCell>
      <FormCell label="行为识别" hint="识别翻页遍历与批量导出行为" switch-cell>
        <switch
          :checked="securityDraft.riskBehaviorEnabled ?? true"
          :disabled="!canEdit"
          @change="onSecuritySwitch('riskBehaviorEnabled', $event)"
        />
      </FormCell>
      <FormCell label="翻页遍历阈值" hint="60 秒内访问的不同页码数达到该值判定为遍历，默认 8">
        <NumberStepper
          :model-value="securityDraft.riskPageScanThreshold ?? 8"
          :min="3"
          :max="200"
          :step="1"
          :disabled="!canEdit"
          @update:model-value="securityDraft.riskPageScanThreshold = $event"
        />
      </FormCell>
      <FormCell label="批量导出阈值" hint="60 秒内导出/下载次数达到该值判定为批量拉取，默认 5">
        <NumberStepper
          :model-value="securityDraft.riskExportThreshold ?? 5"
          :min="2"
          :max="100"
          :step="1"
          :disabled="!canEdit"
          @update:model-value="securityDraft.riskExportThreshold = $event"
        />
      </FormCell>
      <FormCell
        v-model="securityDraft.riskAlertWebhook"
        label="告警 Webhook"
        editable
        boxed
        placeholder="https://..."
        hint="封禁、挑战、警告事件会 POST 到该地址；留空表示不推送"
        :disabled="!canEdit"
        last
      />
    </view>
  </ConfigSectionCard>
  <ConfigSectionCard title="蜜罐诱捕">
    <view class="form-panel form-panel--flat">
      <FormCell
        label="启用蜜罐"
        hint="开启后 /api/system/internal/ 下的诱饵路径对匿名请求开放，命中即追加风险分并封禁来源 IP"
        switch-cell
      >
        <switch
          :checked="securityDraft.honeypotEnabled ?? false"
          :disabled="!canEdit"
          @change="onSecuritySwitch('honeypotEnabled', $event)"
        />
      </FormCell>
      <FormCell label="命中风险分" hint="每次命中追加的风险分，默认 80">
        <NumberStepper
          :model-value="securityDraft.honeypotRiskDelta ?? 80"
          :min="20"
          :max="500"
          :disabled="!canEdit"
          @update:model-value="securityDraft.honeypotRiskDelta = $event"
        />
      </FormCell>
      <FormCell label="封禁时长" hint="命中后封禁来源 IP 的秒数，默认 86400（24 小时）">
        <NumberStepper
          :model-value="securityDraft.honeypotBlockSeconds ?? 86400"
          :min="60"
          :max="604800"
          :step="3600"
          :disabled="!canEdit"
          @update:model-value="securityDraft.honeypotBlockSeconds = $event"
        />
      </FormCell>
      <FormCell
        label="响应延迟"
        hint="命中后随机延迟的上限（毫秒），用于消耗扫描器并发，默认 300"
        last
      >
        <NumberStepper
          :model-value="securityDraft.honeypotTarpitMs ?? 300"
          :min="0"
          :max="3000"
          :step="100"
          :disabled="!canEdit"
          @update:model-value="securityDraft.honeypotTarpitMs = $event"
        />
      </FormCell>
    </view>
  </ConfigSectionCard>
  <view class="config-info">
    <text>保存全部后立即生效：接口安全开关即时作用于全部 REST 接口；时间戳与 Nonce 建议保持开启。</text>
  </view>
</view>
</template>

<script setup lang="ts">
import ConfigSectionCard from '@/components/business/system/ConfigSectionCard/index.vue'
import FormCell from '@/components/base/FormCell/index.vue'
import NumberStepper from '@/components/base/NumberStepper/index.vue'

import type { SecurityAdminConfig } from '@/types/config-types'

defineProps<{
  securityDraft: SecurityAdminConfig
  canEdit: boolean
  forbidConcurrentLogin: boolean
  onSecuritySwitch: (key: any, e: any) => void
  onForbidConcurrentChange: (e: any) => void
}>()
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
