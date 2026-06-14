<template>
  <view class="search-bar">
    <IconFont class="search-bar__icon" name="search" :size="28" color="#909399" />
    <input
      class="search-bar__input"
      type="text"
      :value="modelValue"
      :placeholder="placeholder"
      confirm-type="search"
      @input="onInput"
      @confirm="onConfirm"
    />
  </view>
</template>

<script setup lang="ts">
import IconFont from '@/components/common/IconFont/index.vue'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    placeholder?: string
  }>(),
  {
    modelValue: '',
    placeholder: '搜索',
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  search: [value: string]
}>()

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function onInput(e: any) {
  emit('update:modelValue', e.detail.value as string)
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function onConfirm(e: any) {
  const value = (e.detail?.value ?? props.modelValue) as string
  emit('update:modelValue', value)
  emit('search', value)
}
</script>

<style lang="scss" scoped>

.search-bar {
  display: flex;
  align-items: center;
  height: 72rpx;
  padding: 0 24rpx;
  border-radius: 36rpx;
  background: $color-bg-muted;
  margin-bottom: 24rpx;
}

.search-bar__icon {
  margin-right: 12rpx;
  flex-shrink: 0;
}

.search-bar__input {
  flex: 1;
  font-size: $font-size-base;
  color: $color-text-primary;
}
</style>
