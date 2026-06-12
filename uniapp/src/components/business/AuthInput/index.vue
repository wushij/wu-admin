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
      :password="password && !visible"
      :type="inputType"
      :placeholder="placeholder"
      :maxlength="maxlength"
      @input="onInput"
    />
    <view
      v-if="password && showToggle"
      class="auth-input-field__toggle"
      @click.stop="toggleVisible"
    >
      <IconFont :name="visible ? 'eye-o' : 'closed-eye'" :size="toggleSize" color="#909399" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import IconFont from '@/components/common/IconFont/index.vue'
import type { IconName } from '@/constants/iconfont'

export type AuthInputIcon = IconName | 'key'

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

const inputType = computed(() => {
  if (props.password) return 'text'
  return props.type
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

.auth-input-field__prefix + .auth-input-field__control {
  padding-left: 0;
}

.auth-input-field--password .auth-input-field__control {
  padding-right: 72rpx;
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
</style>
