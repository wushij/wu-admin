<template>
  <view class="list-row" :class="{ 'list-row--unread': unread }" @click="emit('click')">
    <view v-if="$slots.lead" class="list-row__lead">
      <slot name="lead" />
    </view>
    <view class="list-row__body">
      <view class="list-row__top">
        <text class="list-row__title" :class="{ 'list-row__title--bold': unread }">{{ title }}</text>
        <text v-if="time" class="list-row__time">{{ time }}</text>
      </view>
      <view class="list-row__bottom">
        <text class="list-row__desc">{{ desc }}</text>
        <view v-if="badge > 0" class="list-row__badge" :class="{ 'list-row__badge--at': atMe }">
          {{ atMe ? '@' : (badge > 99 ? '99+' : badge) }}
        </view>
      </view>
    </view>
    <IconFont v-if="arrow" name="arrow" :size="28" color="#c0c4cc" />
  </view>
</template>

<script setup lang="ts">
import IconFont from '@/components/common/IconFont/index.vue'

withDefaults(
  defineProps<{
    title: string
    desc?: string
    time?: string
    unread?: boolean
    badge?: number
    atMe?: boolean
    arrow?: boolean
  }>(),
  {
    desc: '',
    badge: 0,
    atMe: false,
    arrow: true,
  },
)

const emit = defineEmits<{ click: [] }>()
</script>

<style lang="scss" scoped>
.list-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 28rpx 24rpx;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
  transition: background 0.15s ease;
}

.list-row:active {
  background: #f7f8fa;
}

.list-row__lead {
  flex-shrink: 0;
}

.list-row__body {
  flex: 1;
  min-width: 0;
}

.list-row__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.list-row__title {
  flex: 1;
  font-size: 30rpx;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-row__title--bold {
  font-weight: 600;
  color: #0f172a;
}

.list-row__time {
  flex-shrink: 0;
  font-size: 22rpx;
  color: #909399;
}

.list-row__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-top: 8rpx;
}

.list-row__desc {
  flex: 1;
  font-size: 26rpx;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-row__badge {
  min-width: 32rpx;
  height: 32rpx;
  padding: 0 10rpx;
  border-radius: 999rpx;
  background: #fa5151;
  color: #fff;
  font-size: 20rpx;
  line-height: 32rpx;
  text-align: center;
}

.list-row__badge--at {
  min-width: 36rpx;
  font-size: 22rpx;
  font-weight: 600;
}

.list-row--unread .list-row__title::before {
  content: '';
  display: inline-block;
  width: 12rpx;
  height: 12rpx;
  margin-right: 10rpx;
  border-radius: 50%;
  background: #6366f1;
  vertical-align: middle;
}
</style>
