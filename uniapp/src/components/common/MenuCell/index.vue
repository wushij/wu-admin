<template>
  <view class="menu-cell" @click="emit('click')">
    <view class="menu-cell__left">
      <ModuleIcon :icon="icon" :theme="resolvedTheme" size="ml" />
      <view class="menu-cell__texts">
        <text class="menu-cell__label">{{ label }}</text>
        <text v-if="desc" class="menu-cell__desc">{{ desc }}</text>
      </view>
    </view>
    <view class="menu-cell__right">
      <text v-if="badgeText" class="menu-cell__badge">{{ badgeText }}</text>
      <text v-if="value" class="menu-cell__value">{{ value }}</text>
      <IconFont v-if="arrow" name="arrow" :size="28" color="#c0c4cc" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import IconFont from '@/components/common/IconFont/index.vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import type { IconName } from '@/constants/iconfont'

const props = withDefaults(
  defineProps<{
    icon: IconName
    label: string
    desc?: string
    value?: string
    arrow?: boolean
    theme?: 'indigo' | 'cyan' | 'violet' | 'slate' | 'amber' | 'emerald' | 'rose' | 'notice' | 'default'
    badge?: number
  }>(),
  { arrow: true, theme: 'default' },
)

const emit = defineEmits<{ click: [] }>()

const resolvedTheme = computed(() => {
  if (props.theme === 'notice') return 'inbox'
  return props.theme
})

const badgeText = computed(() => {
  const n = props.badge ?? 0
  if (n <= 0) return ''
  return n > 99 ? '99+' : String(n)
})
</script>

<style lang="scss" scoped>

.menu-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 112rpx;
  padding: 20rpx 32rpx;
  border-bottom: 1px solid $color-border-light;
  transition: background 0.15s ease;
}

.menu-cell:active {
  background: $color-bg-muted;
}

.menu-cell:last-child {
  border-bottom: none;
}

.menu-cell__left {
  display: flex;
  align-items: center;
  gap: 20rpx;
  flex: 1;
  min-width: 0;
}

.menu-cell__texts {
  flex: 1;
  min-width: 0;
}

.menu-cell__label {
  display: block;
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.menu-cell__desc {
  display: block;
  margin-top: 6rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  line-height: 1.4;
}

.menu-cell__right {
  display: flex;
  align-items: center;
  gap: 8rpx;
  flex-shrink: 0;
}

.menu-cell__value {
  font-size: 26rpx;
  color: $color-text-secondary;
}

.menu-cell__badge {
  min-width: 36rpx;
  height: 36rpx;
  padding: 0 10rpx;
  border-radius: 999rpx;
  background: $color-danger;
  color: #fff;
  font-size: 20rpx;
  font-weight: $font-weight-bold;
  line-height: 36rpx;
  text-align: center;
}
</style>
