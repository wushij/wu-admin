<template>
  <view class="emoji-picker">
    <scroll-view scroll-x class="emoji-picker__tabs" :show-scrollbar="false">
      <view
        v-for="cat in CHAT_EMOJI_CATEGORIES"
        :key="cat.key"
        class="emoji-picker__tab"
        :class="{ 'emoji-picker__tab--active': activeTab === cat.key }"
        @click="activeTab = cat.key"
      >
        {{ cat.label }}
      </view>
    </scroll-view>
    <scroll-view scroll-y class="emoji-picker__grid-wrap">
      <view class="emoji-picker__grid">
        <text
          v-for="e in currentEmojis"
          :key="`${activeTab}-${e}`"
          class="emoji-picker__item"
          @click="emit('pick', e)"
        >{{ e }}</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { CHAT_EMOJI_CATEGORIES } from '@/constants/chat-emojis'

const emit = defineEmits<{
  pick: [emoji: string]
}>()

const activeTab = ref(CHAT_EMOJI_CATEGORIES[0].key)

const currentEmojis = computed(
  () => CHAT_EMOJI_CATEGORIES.find((c) => c.key === activeTab.value)?.emojis ?? [],
)
</script>

<style lang="scss" scoped>
.emoji-picker {
  background: #f7f7f7;
  border-top: 1px solid #e5e5e5;
}

.emoji-picker__tabs {
  display: flex;
  white-space: nowrap;
  padding: 8rpx 16rpx;
  border-bottom: 1px solid #ebebeb;
}

.emoji-picker__tab {
  display: inline-block;
  padding: 8rpx 20rpx;
  margin-right: 8rpx;
  font-size: 24rpx;
  color: #606266;
  border-radius: 8rpx;
}

.emoji-picker__tab--active {
  color: #07c160;
  background: #e8f8ef;
}

.emoji-picker__grid-wrap {
  height: 360rpx;
}

.emoji-picker__grid {
  display: flex;
  flex-wrap: wrap;
  padding: 12rpx 8rpx;
}

.emoji-picker__item {
  width: 72rpx;
  height: 72rpx;
  line-height: 72rpx;
  text-align: center;
  font-size: 40rpx;
}
</style>
