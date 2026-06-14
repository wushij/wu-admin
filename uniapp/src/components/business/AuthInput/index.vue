<template>
  <view
    class="auth-input-field"
    :class="[customClass, { 'auth-input-field--password': password && showToggle }]"
  >
    <view v-if="icon" class="auth-input-field__prefix">
      <IconFont v-if="icon !== 'key'" :name="icon" :size="iconSize" :color="iconColor" />
      <image
        v-else
        class="auth-input-field__key-icon"
        src="/static/icons/auth-key.svg"
        mode="aspectFit"
      />
    </view>
    <input
      :value="modelValue"
      class="auth-input-field__control"
      :class="{ 'auth-input-field__control--masked': useMask && !visible && !!modelValue }"
      :password="useNativePassword && !visible"
      :type="resolvedInputType"
      :placeholder="placeholder"
      :maxlength="maxlength"
      autocomplete="new-password"
      @input="onInput"
    />
    <view
      v-if="password && showToggle"
      class="auth-input-field__toggle"
      @click.stop="toggleVisible"
    >
      <PasswordEyeIcon :slashed="visible" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import IconFont from '@/components/common/IconFont/index.vue'
import PasswordEyeIcon from '@/components/common/PasswordEyeIcon/index.vue'
import type { IconName } from '@/constants/iconfont'

export type AuthInputIcon = IconName | 'key'

const isH5 = process.env.UNI_PLATFORM === 'h5'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    placeholder?: string
    icon?: AuthInputIcon
    password?: boolean
    showToggle?: boolean
    type?: 'text' | 'number' | 'digit'
    maxlength?: number
    customClass?: string
    iconSize?: number
    toggleSize?: number
    iconColor?: string
  }>(),
  {
    modelValue: '',
    placeholder: '',
    password: false,
    showToggle: true,
    type: 'text',
    maxlength: 50,
    customClass: '',
    iconSize: 36,
    toggleSize: 36,
    iconColor: '#409eff',
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const visible = ref(false)

const useMask = computed(() => props.password && isH5)
const useNativePassword = computed(() => props.password && !isH5)

const resolvedInputType = computed(() => {
  if (!props.password) return props.type
  if (isH5) return 'text'
  return visible.value ? 'text' : 'password'
})

function toggleVisible() {
  visible.value = !visible.value
}

function onInput(e: { detail: { value: string } }) {
  emit('update:modelValue', e.detail.value)
}
</script>

<style lang="scss" scoped>
.auth-input-field {
  position: relative;
  display: flex;
  align-items: center;
  height: 96rpx;
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.2);
}

.auth-input-field__prefix {
  flex-shrink: 0;
  width: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-input-field__key-icon {
  width: 36rpx;
  height: 36rpx;
}

.auth-input-field__control {
  flex: 1;
  min-width: 0;
  height: 100%;
  padding: 0 28rpx 0 0;
  font-size: 28rpx;
  color: #303133;
}

.auth-input-field__control::placeholder {
  color: #909399;
}

.auth-input-field__prefix + .auth-input-field__control {
  padding-left: 0;
}

.auth-input-field--password .auth-input-field__control {
  padding-right: 72rpx;
}

.auth-input-field__control--masked {
  -webkit-text-security: disc;
  text-security: disc;
}

.auth-input-field__toggle {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-input-field__toggle :deep(.password-eye-icon) {
  width: 36rpx;
  height: 36rpx;
}

/* #ifdef H5 */
.auth-input-field--password .auth-input-field__control::-ms-reveal,
.auth-input-field--password .auth-input-field__control::-ms-clear {
  display: none !important;
  width: 0 !important;
  height: 0 !important;
}

.auth-input-field--password .auth-input-field__control::-webkit-credentials-auto-fill-button,
.auth-input-field--password .auth-input-field__control::-webkit-contacts-auto-fill-button,
.auth-input-field--password .auth-input-field__control::-webkit-textfield-decoration-container {
  display: none !important;
  visibility: hidden !important;
  pointer-events: none !important;
  width: 0 !important;
  height: 0 !important;
}
/* #endif */
</style>
