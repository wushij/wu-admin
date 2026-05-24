<template>
  <div class="session-config-tab">
    <el-form :model="draft" label-width="130px" class="config-form session-form">
      <el-form-item label="Token 有效期">
        <el-input-number v-model="draft.tokenExpireHours" :min="1" :max="720" :disabled="!canEdit" />
        <span class="unit">小时（保存全部后生效）</span>
      </el-form-item>

      <el-form-item label="签名密钥有效期">
        <el-input-number v-model="draft.sessionSignExpireHours" :min="1" :max="120" :disabled="!canEdit" />
        <span class="unit">小时（会话签名/加密临时密钥，保存后下次协商生效）</span>
      </el-form-item>
    </el-form>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="建议：将「签名密钥有效期」与「Token 有效期」设为相同的时长（例如均为 24 小时），确保会话生命周期与签名加密凭证同步存活与续约。"
      class="config-tip-alert"
    />
  </div>
</template>

<script setup lang="ts">
import type { ConfigGroupMap } from '@/types/config'

defineProps<{
  draft: ConfigGroupMap['session']
  canEdit: boolean
}>()
</script>

<style scoped>
.session-form {
  max-width: 960px;
}

.session-form :deep(.el-form-item__content) {
  flex-wrap: nowrap;
}

.unit {
  margin-left: 8px;
  color: var(--el-text-color-regular, #606266);
  font-size: 13px;
  white-space: nowrap;
}

.config-tip-alert {
  margin-top: 16px;
  max-width: 960px;
  border-radius: 6px;
}
</style>
