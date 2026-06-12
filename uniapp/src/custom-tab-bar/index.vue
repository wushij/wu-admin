<template>
  <view class="tab-bar">
    <view class="tab-bar__inner">
      <view
        v-for="(item, index) in TAB_BAR_ITEMS"
        :key="item.pagePath"
        class="tab-bar__item"
        @click="switchTab(index, item.pagePath)"
      >
        <view class="tab-bar__icon-wrap">
          <IconFont
            :name="selected === index ? item.iconActive : item.icon"
            :size="44"
            :color="selected === index ? '#010710' : '#999999'"
          />
          <view v-if="index === 2 && unread > 0" class="tab-bar__badge">
            {{ unread > 99 ? '99+' : unread }}
          </view>
        </view>
        <text class="tab-bar__text" :class="{ 'tab-bar__text--active': selected === index }">
          {{ item.text }}
        </text>
      </view>
    </view>
    <view class="tab-bar__safe" />
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
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.tab-bar__inner {
  display: flex;
  height: 100rpx;
  align-items: center;
}

.tab-bar__item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
}

.tab-bar__icon-wrap {
  position: relative;
}

.tab-bar__badge {
  position: absolute;
  top: -8rpx;
  right: -16rpx;
  min-width: 28rpx;
  height: 28rpx;
  padding: 0 6rpx;
  border-radius: 999rpx;
  background: #fa5151;
  color: #fff;
  font-size: 18rpx;
  line-height: 28rpx;
  text-align: center;
}

.tab-bar__text {
  font-size: 22rpx;
  color: #999999;
  line-height: 1.2;
}

.tab-bar__text--active {
  color: #010710;
  font-weight: 500;
}

.tab-bar__safe {
  height: constant(safe-area-inset-bottom);
  height: env(safe-area-inset-bottom);
}
</style>
