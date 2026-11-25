<template>
  <view
    class="auth-input-field"
    :class="[
      customClass,
      {
        'auth-input-field--password': password && showToggle,
        'is-focused': focused,
      },
    ]"
  >
    <view v-if="icon" class="auth-input-field__prefix">
      <IconFont v-if="icon !== 'key'" :name="icon" :size="iconSize" :color="resolvedIconColor" />
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
      placeholder-style="color: rgba(148, 163, 184, 0.74)"
      :maxlength="maxlength"
      autocomplete="new-password"
      @input="onInput"
      @focus="onFocus"
      @blur="onBlur"
    />
    <view
      v-if="password && showToggle"
      class="auth-input-field__toggle"
      @tap.stop="toggleVisible"
    >
      <PasswordEyeIcon :slashed="visible" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import IconFont from '@/components/base/IconFont/index.vue'
import PasswordEyeIcon from '@/components/base/PasswordEyeIcon/index.vue'
import type { IconName } from '@/constants/iconfont'

export type AuthInputIcon = IconName | 'key'

const isH5 = import.meta.env.UNI_PLATFORM === 'h5'

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
    iconSize: 34,
    toggleSize: 36,
    iconColor: '',
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const visible = ref(false)
const focused = ref(false)

const resolvedIconColor = computed(() => {
  if (props.iconColor) return props.iconColor
  return focused.value ? '#f8fafc' : 'rgba(226, 232, 240, 0.85)'
})

const useMask = computed(() => props.password && isH5 && !visible.value && !!props.modelValue)
const useNativePassword = computed(
  () => props.password && !isH5 && !!props.modelValue && !visible.value,
)

const resolvedInputType = computed(() => {
  if (!props.password) return props.type
  if (isH5) return 'text'
  // 小程序/App：始终 type=text，仅用 password 属性掩码，避免预填/输入不同步
  return 'text'
})

function toggleVisible() {
  visible.value = !visible.value
}

function readInputValue(e: { detail?: { value?: string } }): string {
  return e.detail?.value ?? ''
}

function onInput(e: { detail: { value: string } }) {
  emit('update:modelValue', readInputValue(e))
}

function onFocus() {
  focused.value = true
}

function onBlur(e: { detail: { value: string } }) {
  focused.value = false
  if (isH5 || !props.password) return
  const value = readInputValue(e)
  // 预填密码时 native 可能返回空，勿覆盖已有 modelValue
  if (!value && props.modelValue) return
  if (value !== props.modelValue) {
    emit('update:modelValue', value)
  }
}
</script>

<style lang="scss" scoped>
.auth-input-field {
  position: relative;
  display: flex;
  align-items: center;
  height: 92rpx;
  border-radius: 9999rpx;
  background-color: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.16);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;

  &.is-focused {
    background-color: rgba(255, 255, 255, 0.16);
    border-color: rgba(186, 230, 253, 0.52);
    box-shadow:
      0 0 0 6rpx rgba(148, 163, 184, 0.16),
      inset 0 1px 0 rgba(255, 255, 255, 0.2);
  }
}

.auth-input-field__prefix {
  flex-shrink: 0;
  width: 78rpx;
  padding-left: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-input-field__key-icon {
  width: 34rpx;
  height: 34rpx;
  opacity: 0.9;
}

.auth-input-field__control {
  flex: 1;
  min-width: 0;
  height: 100%;
  padding: 0 32rpx 0 0;
  font-size: 29rpx;
  color: #f8fafc;
  caret-color: #f8fafc;
  background: transparent;
}

/* H5 端 UniApp <input> 会包裹在 .uni-input-wrapper / .uni-input-input 内，必须叠加父类高权重压住 reset.scss 全局黑字设置 */
.auth-input-field .auth-input-field__control :deep(.uni-input-wrapper) {
  background: transparent !important;
}

.auth-input-field .auth-input-field__control :deep(.uni-input-input),
.auth-input-field .auth-input-field__control :deep(input) {
  color: #f8fafc !important;
  -webkit-text-fill-color: #f8fafc !important;
  caret-color: #f8fafc !important;
  font-size: 29rpx !important;
  background: transparent !important;
}

.auth-input-field .auth-input-field__control :deep(.uni-input-placeholder),
.auth-input-field .auth-input-field__control::placeholder {
  color: rgba(203, 213, 225, 0.72) !important;
  -webkit-text-fill-color: rgba(203, 213, 225, 0.72) !important;
  font-size: 27rpx !important;
}

.auth-input-field__prefix + .auth-input-field__control {
  padding-left: 0;
}

.auth-input-field__control--masked,
.auth-input-field__control--masked :deep(.uni-input-input) {
  -webkit-text-security: disc;
  text-security: disc;
}

.auth-input-field__toggle {
  flex-shrink: 0;
  width: 76rpx;
  padding-right: 6rpx;
  height: 100%;
  color: rgba(226, 232, 240, 0.78);
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-input-field__toggle :deep(.password-eye-icon) {
  width: 34rpx;
  height: 34rpx;
}

/* #ifdef H5 */
.auth-input-field__control :deep(input:-webkit-autofill),
.auth-input-field__control :deep(input:-webkit-autofill:hover),
.auth-input-field__control :deep(input:-webkit-autofill:focus),
.auth-input-field__control :deep(.uni-input-input:-webkit-autofill),
.auth-input-field__control :deep(.uni-input-input:-webkit-autofill:hover),
.auth-input-field__control :deep(.uni-input-input:-webkit-autofill:focus),
.auth-input-field__control:-webkit-autofill,
.auth-input-field__control:-webkit-autofill:hover,
.auth-input-field__control:-webkit-autofill:focus {
  -webkit-text-fill-color: #f8fafc !important;
  caret-color: #f8fafc !important;
  -webkit-box-shadow: 0 0 0 1000px transparent inset !important;
  box-shadow: 0 0 0 1000px transparent inset !important;
  background-color: transparent !important;
  transition:
    background-color 99999s ease-in-out 0s,
    color 99999s ease-in-out 0s !important;
}

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
