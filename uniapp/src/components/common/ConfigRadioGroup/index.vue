<template>
  <view class="config-radio-group">
    <view
      v-for="opt in options"
      :key="opt.value"
      class="config-radio"
      :class="{
        'config-radio--active': modelValue === opt.value,
        'config-radio--disabled': disabled,
      }"
      @click="select(opt.value)"
    >
      <view class="config-radio__icon">
        <view v-if="modelValue === opt.value" class="config-radio__dot" />
      </view>
      <text class="config-radio__label">{{ opt.label }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    modelValue: string
    options: Array<{ label: string; value: string }>
    disabled?: boolean
  }>(),
  { disabled: false },
)

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

function select(value: string) {
  if (props.disabled || props.modelValue === value) return
  emit('update:modelValue', value)
}
</script>

<style lang="scss" scoped>

.config-radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 24rpx 32rpx;
}

.config-radio {
  display: inline-flex;
  align-items: center;
  gap: 12rpx;
}

.config-radio__icon {
  width: 32rpx;
  height: 32rpx;
  border-radius: 50%;
  border: 2rpx solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  background: #fff;
}

.config-radio--active .config-radio__icon {
  border-color: $color-primary;
}

.config-radio__dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: $color-primary;
}

.config-radio__label {
  font-size: $font-size-sm;
  color: $color-text-primary;
}

.config-radio--disabled {
  opacity: 0.55;
}
</style>
