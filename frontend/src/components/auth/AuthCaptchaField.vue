<template>
  <div class="captcha-row">
    <el-input
      :model-value="modelValue"
      placeholder="请输入验证码"
      maxlength="6"
      autocomplete="off"
      class="form-input captcha-input"
      @update:model-value="$emit('update:modelValue', $event)"
      @keyup.enter="$emit('enter')"
    >
      <template #prefix>
        <el-icon class="input-icon"><Key /></el-icon>
      </template>
    </el-input>
    <button
      v-if="captchaImg"
      type="button"
      class="captcha-visual-btn"
      aria-label="刷新图形验证码"
      @click="$emit('refresh')"
    >
      <img
        :src="captchaImg"
        class="captcha-img"
        alt="图形验证码"
      />
    </button>
    <button
      v-else
      type="button"
      class="captcha-placeholder motion-shimmer"
      aria-label="点击重新获取验证码"
      @click="$emit('refresh')"
    >
      <el-icon :size="15"><RefreshRight /></el-icon>
      <span>刷新验证码</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { Key, RefreshRight } from '@element-plus/icons-vue'

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

<style scoped src="./auth-glass-form.css"></style>
<style scoped>
.captcha-row {
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
}

.captcha-input {
  flex: 1;
  min-width: 0;
}

.captcha-visual-btn {
  height: 46px;
  width: 120px;
  padding: 0;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: var(--radius-pill);
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(248, 250, 252, 0.9);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.35);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition:
    border-color var(--duration-fast) var(--ease-out),
    transform var(--duration-fast) var(--ease-out);
}

@media (hover: hover) and (pointer: fine) {
  .captcha-visual-btn:hover {
    border-color: rgba(125, 211, 252, 0.5);
  }
}

.captcha-visual-btn:focus-visible {
  outline: 2px solid #7dd3fc;
  outline-offset: 2px;
}

.captcha-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.captcha-placeholder {
  width: 120px;
  height: 46px;
  flex-shrink: 0;
  border-radius: var(--radius-pill);
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.07);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  color: rgba(248, 250, 252, 0.88);
  font-size: 12.5px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  cursor: pointer;
  transition:
    background-color var(--duration-fast) var(--ease-out),
    border-color var(--duration-fast) var(--ease-out);
}

@media (hover: hover) and (pointer: fine) {
  .captcha-placeholder:hover {
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(255, 255, 255, 0.2);
  }
}
</style>
