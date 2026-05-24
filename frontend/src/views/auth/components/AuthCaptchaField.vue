<template>
  <div class="captcha-row">
    <el-input
      :model-value="modelValue"
      placeholder="请输入验证码"
      maxlength="6"
      class="form-input captcha-input"
      @update:model-value="$emit('update:modelValue', $event)"
      @keyup.enter="$emit('enter')"
    >
      <template #prefix>
        <el-icon class="input-icon"><Key /></el-icon>
      </template>
    </el-input>
    <img
      v-if="captchaImg"
      :src="captchaImg"
      class="captcha-img"
      title="点击刷新"
      @click="$emit('refresh')"
    />
    <el-skeleton v-else :rows="1" animated style="width: 120px; height: 48px" />
  </div>
</template>

<script setup lang="ts">
import { Key } from '@element-plus/icons-vue'

defineProps<{
  modelValue: string
  captchaImg: string
}>()

defineEmits<{
  'update:modelValue': [value: string]
  refresh: []
  enter: []
}>()
</script>

<style scoped>
.captcha-row {
  display: flex;
  gap: 16px;
  align-items: center;
}

.captcha-input { flex: 1; }

.captcha-img {
  height: 48px;
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.captcha-img:hover {
  opacity: 0.9;
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.form-input { transition: all 0.3s ease; }

.form-input :deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.95) !important;
  border-radius: 12px !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
}

.form-input :deep(.el-input__inner) {
  color: #303133 !important;
  height: 48px !important;
  font-size: 16px !important;
  padding: 0 20px !important;
}

.input-icon {
  color: #409eff !important;
  font-size: 18px !important;
}
</style>
