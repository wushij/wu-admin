<template>
  <view class="tab-bar">
    <view class="tab-bar__inner">
      <view
        v-for="(item, index) in TAB_BAR_ITEMS"
        :key="item.pagePath"
        class="tab-bar__item"
        :class="{ 'tab-bar__item--active': selected === index }"
        @click="switchTab(index, item.pagePath)"
      >
        <view class="tab-bar__icon-wrap" :class="{ 'tab-bar__icon-wrap--active': selected === index }">
          <IconFont
            :name="selected === index ? item.iconActive : item.icon"
            :size="40"
            :color="selected === index ? '#010710' : '#94a3b8'"
          />
          <view v-if="index === 2 && unread > 0" class="tab-bar__badge">
            <text class="tab-bar__badge-text">{{ unread > 99 ? '99+' : unread }}</text>
          </view>
        </view>
        <text class="tab-bar__text" :class="{ 'tab-bar__text--active': selected === index }">
          {{ item.text }}
        </text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import IconFont from '@/components/common/IconFont/index.vue'
import { TAB_BAR_ITEMS } from '@/constants/tabBar'
import { useTabBarStore } from '@/store/tabBar'
import { useMessageStore } from '@/store/message'

const tabBarStore = useTabBarStore()
const messageStore = useMessageStore()
const { selected } = storeToRefs(tabBarStore)
const { totalUnread: unread } = storeToRefs(messageStore)

function switchTab(index: number, url: string) {
  if (selected.value === index) return
  tabBarStore.setSelected(index)
  uni.switchTab({ url })
}
</script>

<style lang="scss" scoped>
.tab-bar {
  position: fixed;
  left: 20rpx;
  right: 20rpx;
  bottom: calc(20rpx + env(safe-area-inset-bottom));
  z-index: 999;
  pointer-events: none;
}

.tab-bar__inner {
  pointer-events: auto;
  display: flex;
  height: 112rpx;
  align-items: center;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(24rpx);
  -webkit-backdrop-filter: blur(24rpx);
  border-radius: 999rpx;
  border: 1rpx solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 8rpx 32rpx rgba(15, 23, 42, 0.08), 0 2rpx 8rpx rgba(15, 23, 42, 0.04);
  padding: 8rpx 6rpx;
  box-sizing: border-box;
}

.tab-bar__item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2rpx;
  padding: 6rpx 0 4rpx;
  position: relative;
}

.tab-bar__icon-wrap {
  position: relative;
  width: 50rpx;
  height: 50rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.tab-bar__icon-wrap--active {
  background: rgba(1, 7, 16, 0.06);
}

.tab-bar__badge {
  position: absolute;
  top: -2rpx;
  right: -4rpx;
  min-width: 20rpx;
  height: 20rpx;
  padding: 0 4rpx;
  border-radius: 999rpx;
  background: #ef4444;
  border: 1.5rpx solid #ffffff;
  box-shadow: 0 2rpx 6rpx rgba(239, 68, 68, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-bar__badge-text {
  font-size: 14rpx;
  font-weight: 700;
  color: #ffffff;
  line-height: 1;
}

.tab-bar__text {
  font-size: 18rpx;
  color: #94a3b8;
  font-weight: 500;
  transition: all 0.25s ease;
}

.tab-bar__text--active {
  color: #010710;
  font-weight: 700;
}
</style>

