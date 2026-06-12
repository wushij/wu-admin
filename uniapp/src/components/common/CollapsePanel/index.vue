<template>
  <view class="collapse-panel">
    <view class="collapse-panel__head" @click="toggle">
      <text class="collapse-panel__title">{{ title }}</text>
      <IconFont
        name="arrow"
        :size="28"
        color="#909399"
        class="collapse-panel__arrow"
        :class="{ 'collapse-panel__arrow--open': expanded }"
      />
    </view>
    <view v-show="expanded" class="collapse-panel__body">
      <slot />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import IconFont from '@/components/common/IconFont/index.vue'

const props = withDefaults(
  defineProps<{
    title: string
    name?: string
    modelValue?: string[]
    defaultOpen?: boolean
  }>(),
  { defaultOpen: false },
)

const emit = defineEmits<{ 'update:modelValue': [value: string[]] }>()

const expanded = ref(props.defaultOpen)

watch(
  () => props.modelValue,
  (val) => {
    if (val && props.name) expanded.value = val.includes(props.name)
  },
  { immediate: true },
)

function toggle() {
  expanded.value = !expanded.value
  if (props.name && props.modelValue) {
    const set = new Set(props.modelValue)
    if (expanded.value) set.add(props.name)
    else set.delete(props.name)
    emit('update:modelValue', [...set])
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.collapse-panel {
  margin-bottom: 16rpx;
  border-radius: $radius-lg;
  background: $color-bg-card;
  border: 1px solid $color-border-light;
  overflow: hidden;
}

.collapse-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx;
  background: $color-bg-muted;
}

.collapse-panel__title {
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.collapse-panel__arrow {
  flex-shrink: 0;
  transform: rotate(90deg);
  transition: transform 0.2s ease;
}

.collapse-panel__arrow--open {
  transform: rotate(-90deg);
}

.collapse-panel__body {
  border-top: 1px solid $color-border-light;
}
</style>
