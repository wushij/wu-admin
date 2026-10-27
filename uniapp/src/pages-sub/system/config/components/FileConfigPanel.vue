<template>
<view class="config-file-wrap">
  <ConfigSectionCard title="存储引擎">
    <view class="form-panel form-panel--flat">
      <view class="storage-engine-grid">
        <view
          v-for="engine in storageEngineOptions"
          :key="engine.type"
          class="storage-engine"
          :class="{ 'storage-engine--active': storageEngine === engine.type }"
          @click="selectStorageEngine(engine.type)"
        >
          <view class="storage-engine__head">
            <text class="storage-engine__name">{{ engine.name }}</text>
            <text v-if="storageActiveType === engine.type" class="storage-engine__badge">当前生效</text>
          </view>
          <text class="storage-engine__desc">{{ engine.desc }}</text>
        </view>
      </view>
      <view class="storage-status">
        <text class="storage-status__label">当前生效引擎</text>
        <text class="storage-status__value">{{ storageActiveLabel }}</text>
        <text
          class="storage-status__health"
          :class="storageHealthy ? 'storage-status__health--ok' : 'storage-status__health--bad'"
        >
          {{ storageHealthy ? '连接正常' : '连接异常' }}
        </text>
      </view>
    </view>
  </ConfigSectionCard>

  <ConfigSectionCard :title="`${storageSelectedLabel}参数`">
    <view class="form-panel form-panel--flat">
      <template v-if="storageEngine === 'local'">
        <FormCell v-model="storageForm.localPath" label="存储根目录" editable boxed :disabled="!canEdit" placeholder="./data/uploads" />
        <FormCell v-model="storageForm.localDomain" label="外网访问域名" editable boxed :disabled="!canEdit" placeholder="选填，如 https://files.example.com" last />
      </template>
      <template v-else-if="storageEngine === 'minio'">
        <FormCell v-model="storageForm.minioEndpoint" label="服务端点" editable boxed :disabled="!canEdit" placeholder="http://127.0.0.1:9000" />
        <FormCell v-model="storageForm.minioBucket" label="存储桶" editable boxed :disabled="!canEdit" placeholder="wu-admin" />
        <FormCell v-model="storageForm.minioAccessKey" label="AccessKey" editable boxed :disabled="!canEdit" />
        <FormCell
          v-model="storageForm.minioSecretKey"
          label="SecretKey"
          editable
          boxed
          password
          :disabled="!canEdit"
          :placeholder="storageForm.minioSecretKeyConfigured ? '已配置，留空保持不变' : '请输入 SecretKey'"
        />
        <FormCell v-model="storageForm.minioDomain" label="外网访问域名" editable boxed :disabled="!canEdit" placeholder="选填" last />
      </template>
      <template v-else-if="storageEngine === 'cos'">
        <FormCell v-model="storageForm.cosRegion" label="地域" editable boxed :disabled="!canEdit" placeholder="ap-guangzhou" />
        <FormCell v-model="storageForm.cosBucket" label="存储桶" editable boxed :disabled="!canEdit" />
        <FormCell v-model="storageForm.cosSecretId" label="SecretId" editable boxed :disabled="!canEdit" />
        <FormCell
          v-model="storageForm.cosSecretKey"
          label="SecretKey"
          editable
          boxed
          password
          :disabled="!canEdit"
          :placeholder="storageForm.cosSecretKeyConfigured ? '已配置，留空保持不变' : '请输入 SecretKey'"
        />
        <FormCell v-model="storageForm.cosDomain" label="CDN 加速域名" editable boxed :disabled="!canEdit" placeholder="选填" last />
      </template>
      <template v-else>
        <FormCell v-model="storageForm.ossEndpoint" label="地域节点" editable boxed :disabled="!canEdit" placeholder="oss-cn-hangzhou.aliyuncs.com" />
        <FormCell v-model="storageForm.ossBucket" label="存储空间" editable boxed :disabled="!canEdit" />
        <FormCell v-model="storageForm.ossAccessKeyId" label="AccessKey ID" editable boxed :disabled="!canEdit" />
        <FormCell
          v-model="storageForm.ossAccessKeySecret"
          label="AccessKey Secret"
          editable
          boxed
          password
          :disabled="!canEdit"
          :placeholder="storageForm.ossAccessKeySecretConfigured ? '已配置，留空保持不变' : '请输入 AccessKey Secret'"
        />
        <FormCell v-model="storageForm.ossDomain" label="自定义域名" editable boxed :disabled="!canEdit" placeholder="选填" last />
      </template>
    </view>
    <view v-if="canEdit" class="storage-actions">
      <button class="storage-action-btn" :loading="storageTesting" @click="testStorageConnection">测试连通性</button>
      <button class="storage-action-btn storage-action-btn--primary" :loading="storageSaving" @click="saveStorageConfig">保存并热重载</button>
    </view>
  </ConfigSectionCard>

  <ConfigSectionCard title="上传策略">
    <view class="form-panel form-panel--file form-panel--flat">
      <FormCell label="单文件上限">
        <NumberStepper v-model="fileDraft.maxSizeMb" :min="1" :max="platformMaxFileMb" :disabled="!canEdit" />
        <text class="config-unit">MB（最高 {{ platformMaxFileMb }}）</text>
      </FormCell>
      <FormCell label="允许扩展名" class="form-cell--stacked" last>
        <textarea
          v-model="fileDraft.allowedExtensions"
          class="config-boxed-textarea"
          :disabled="!canEdit"
          placeholder="逗号分隔，如 jpg,png,pdf"
        />
      </FormCell>
    </view>
  </ConfigSectionCard>
</view>
</template>

<script setup lang="ts">
import ConfigSectionCard from '@/components/business/system/ConfigSectionCard/index.vue'
import FormCell from '@/components/base/FormCell/index.vue'
import NumberStepper from '@/components/base/NumberStepper/index.vue'

import type { FileStorageConfig, StorageEngineType } from '@/types/config-types'

interface StorageEngineOption {
  type: StorageEngineType
  name: string
  desc: string
}

defineProps<{
  fileDraft: FileStorageConfig
  canEdit: boolean
  storageForm: any
  storageEngine: StorageEngineType
  storageActiveType: StorageEngineType
  storageHealthy: boolean
  storageSaving: boolean
  storageTesting: boolean
  storageEngineOptions: StorageEngineOption[]
  storageActiveLabel: string
  storageSelectedLabel: string
  platformMaxFileMb: number
  selectStorageEngine: (type: StorageEngineType) => void
  testStorageConnection: () => void
  saveStorageConfig: () => void
}>()
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.config-file-wrap {
  padding-bottom: 8rpx;
}

.storage-engine-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  padding: 24rpx;
}

.storage-engine {
  padding: 20rpx;
  border: 1px solid $color-border-light;
  border-radius: $radius-md;
  background: $color-bg-card;
  transition: all 0.2s ease;
}

.storage-engine--active {
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.06);
  box-shadow: 0 0 0 2rpx rgba(99, 102, 241, 0.12);
}

.storage-engine__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8rpx;
}

.storage-engine__name {
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.storage-engine__badge {
  flex-shrink: 0;
  padding: 2rpx 10rpx;
  border-radius: 999rpx;
  background: #6366f1;
  color: #fff;
  font-size: 18rpx;
  line-height: 1.6;
}

.storage-engine__desc {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  line-height: 1.5;
}

.storage-status {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 0 24rpx 24rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.storage-status__value {
  color: $color-text-primary;
  font-weight: $font-weight-semibold;
}

.storage-status__health {
  margin-left: auto;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
}

.storage-status__health--ok {
  color: #16a34a;
  background: rgba(22, 163, 74, 0.1);
}

.storage-status__health--bad {
  color: #dc2626;
  background: rgba(220, 38, 38, 0.1);
}

.storage-actions {
  display: flex;
  gap: 16rpx;
  padding: 0 24rpx 24rpx;
}

.storage-action-btn {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
  color: $color-text-regular;
  font-size: $font-size-sm;
}

.storage-action-btn::after {
  border: none;
}

.storage-action-btn--primary {
  background: $color-primary;
  color: #fff;
}

.config-boxed-textarea {
  width: 100%;
  min-height: 160rpx;
  padding: 16rpx 20rpx;
  border: 1px solid $color-border-light;
  border-radius: $radius-md;
  background: $color-bg-card;
  font-size: $font-size-sm;
  line-height: 1.55;
  box-sizing: border-box;
  color: $color-text-primary;
}

.config-boxed-textarea::placeholder {
  color: $color-text-placeholder;
}

.config-unit {
  flex-shrink: 0;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  white-space: nowrap;
}
</style>
