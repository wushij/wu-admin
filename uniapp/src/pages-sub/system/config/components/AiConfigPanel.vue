<template>
<view class="form-panel">
  <FormCell label="AI 助手" hint="关闭后，PC端与移动端的悬浮小窗将不再显示" switch-cell>
    <switch :checked="aiDraft.assistantEnabled !== false" :disabled="!canEdit" @change="aiDraft.assistantEnabled = $event.detail.value" />
  </FormCell>
  <FormCell label="回答边界" last>
    <ConfigRadioGroup v-model="aiDraft.answerScope" :options="answerScopeOptions" :disabled="!canEdit" />
  </FormCell>

  <view class="ai-quota-card">
    <view class="ai-quota-card__header">
      <text class="ai-quota-card__title">Token 每日配额设置</text>
      <text class="ai-quota-card__sub">限制每位用户或不同角色每天可消耗的最大 AI Token 数量</text>
    </view>

    <!-- 默认每用户每日配额 -->
    <view class="ai-quota-section">
      <view class="ai-quota-section__head">
        <text class="ai-quota-section__title">每用户默认每日配额</text>
        <text class="ai-quota-section__desc">未配置单独角色规则的用户，统一套用此额度</text>
      </view>
      <FormCell label="默认配额" last>
        <NumberStepper v-model="aiDraft.tokensPerUserDaily" :min="0" :max="10000000" :step="10000" width="280rpx" :disabled="!canEdit" />
      </FormCell>
      <view class="ai-quota-default-tag">
        <text v-if="aiDraft.tokensPerUserDaily === 0" class="tag-text tag-text--success">默认生效：无限制</text>
        <text v-else class="tag-text tag-text--info">默认生效：{{ aiDraft.tokensPerUserDaily ? aiDraft.tokensPerUserDaily.toLocaleString() : 0 }} Tokens/天</text>
      </view>
    </view>

    <!-- 角色差异化配额 -->
    <view class="ai-quota-section ai-quota-section--border">
      <view class="ai-quota-section__head ai-quota-section__head--row">
        <view class="ai-quota-section__titles">
          <text class="ai-quota-section__title">角色差异化配额</text>
          <text class="ai-quota-section__desc">针对特定角色设置专属配额</text>
        </view>
        <view v-if="canEdit && aiDraft.roleTokenQuotas.length < 50" class="ai-quota-add-btn" @click="addQuotaRow">
          <text>+ 添加规则</text>
        </view>
      </view>

      <view v-if="aiDraft.roleTokenQuotas.length === 0" class="ai-quota-empty">
        <text class="ai-quota-empty__text">暂无单独配置的角色规则，全员套用默认配额</text>
      </view>

      <view v-for="(row, idx) in aiDraft.roleTokenQuotas" :key="idx" class="ai-quota-row">
        <view class="ai-quota-row__header">
          <text class="ai-quota-row__index">规则 #{{ idx + 1 }}</text>
          <text v-if="canEdit" class="ai-quota-row__del-btn" @click="removeQuotaRow(idx)">删除</text>
        </view>
        <FormCell label="目标角色" clickable arrow @click="pickQuotaRole(idx)">
          <text class="picker-value">{{ quotaRoleLabel(row.roleId) }}</text>
        </FormCell>
        <FormCell label="每日 Token 配额" last>
          <NumberStepper v-model="row.tokensDaily" :min="0" :max="10000000" :step="10000" width="280rpx" :disabled="!canEdit" />
        </FormCell>
        <view class="ai-quota-row__tag">
          <text v-if="row.tokensDaily === 0" class="tag-text tag-text--success">生效规则：无限制</text>
          <text v-else class="tag-text tag-text--info">生效规则：{{ row.tokensDaily ? row.tokensDaily.toLocaleString() : 0 }} Tokens/天</text>
        </view>
      </view>

      <view class="ai-quota-hint">
        <text class="ai-quota-hint__text">💡 多角色用户自动按最高配额生效；配额 0 表示该角色不设限制。</text>
      </view>
    </view>
  </view>
</view>
</template>

<script setup lang="ts">
import ConfigRadioGroup from '@/components/business/system/ConfigRadioGroup/index.vue'
import FormCell from '@/components/base/FormCell/index.vue'
import NumberStepper from '@/components/base/NumberStepper/index.vue'

import type { AiAdminConfig } from '@/types/config-types'

defineProps<{
  aiDraft: AiAdminConfig
  canEdit: boolean
  answerScopeOptions: { label: string; value: string }[]
  quotaRoleLabel: (roleId: number) => string
  pickQuotaRole: (idx: number) => void
  addQuotaRow: () => void
  removeQuotaRow: (idx: number) => void
}>()
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.ai-quota-card {
  margin: 24rpx;
  border-radius: $radius-lg;
  background: $color-bg-card;
  border: 1px solid $color-border-light;
  overflow: hidden;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
}

.ai-quota-card__header {
  padding: 24rpx 28rpx;
  background: $color-bg-muted;
  border-bottom: 1px solid $color-border-light;
}

.ai-quota-card__title {
  display: block;
  font-size: $font-size-base;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  margin-bottom: 4rpx;
}

.ai-quota-card__sub {
  display: block;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.ai-quota-section {
  padding: 20rpx 0;
}

.ai-quota-default-tag {
  margin: 8rpx 28rpx 0;
  padding: 10rpx 20rpx;
  background: $color-bg-muted;
  border-radius: $radius-sm;
}

.ai-quota-section--border {
  border-top: 1px solid $color-border-light;
}

.ai-quota-section__head {
  padding: 0 28rpx 16rpx;
}

.ai-quota-section__head--row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ai-quota-section__titles {
  flex: 1;
}

.ai-quota-section__title {
  display: block;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.ai-quota-section__desc {
  display: block;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  margin-top: 2rpx;
}

.ai-quota-add-btn {
  padding: 8rpx 20rpx;
  border-radius: $radius-sm;
  background: rgba(99, 102, 241, 0.1);
  color: $color-primary;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
}

.ai-quota-empty {
  margin: 12rpx 28rpx;
  padding: 32rpx 20rpx;
  text-align: center;
  background: $color-bg-muted;
  border-radius: $radius-md;
}

.ai-quota-empty__text {
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.ai-quota-row {
  margin: 16rpx 24rpx;
  border: 1px solid $color-border-light;
  border-radius: $radius-md;
  background: $color-bg-card;
  overflow: hidden;
}

.ai-quota-row__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14rpx 24rpx;
  background: $color-bg-muted;
  border-bottom: 1px solid $color-border-light;
}

.ai-quota-row__index {
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  color: $color-text-regular;
}

.ai-quota-row__del-btn {
  padding: 4rpx 16rpx;
  font-size: $font-size-xs;
  color: #ef4444;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 999rpx;
  line-height: 1.4;
}

.ai-quota-row__tag {
  padding: 10rpx 24rpx;
  background: $color-bg-muted;
  border-top: 1px solid $color-border-light;
}

.tag-text {
  font-size: $font-size-xs;
}

.tag-text--success {
  color: $color-success;
}

.tag-text--info {
  color: $color-text-secondary;
}

.ai-quota-hint {
  margin: 16rpx 24rpx 8rpx;
  padding: 16rpx 20rpx;
  background: $color-bg-muted;
  border-radius: $radius-md;
}

.ai-quota-hint__text {
  font-size: $font-size-xs;
  color: $color-text-secondary;
  line-height: 1.5;
}
</style>
