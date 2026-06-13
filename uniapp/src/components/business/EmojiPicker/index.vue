<template>
  <view class="emoji-picker">
    <scroll-view scroll-x class="emoji-picker__tabs" :show-scrollbar="false">
      <view
        v-for="cat in tabs"
        :key="cat.key"
        class="emoji-picker__tab"
        :class="{ 'emoji-picker__tab--active': activeTab === cat.key }"
        @click="activeTab = cat.key"
      >
        {{ cat.label }}
      </view>
    </scroll-view>
    <scroll-view scroll-y class="emoji-picker__grid-wrap">
      <view v-if="activeTab === RECENT_KEY && !currentEmojis.length" class="emoji-picker__empty">
        <text class="emoji-picker__empty-text">暂无最近表情</text>
      </view>
      <view v-else class="emoji-picker__grid">
        <text
          v-for="e in currentEmojis"
          :key="`${activeTab}-${e}`"
          class="emoji-picker__item"
          @click="onPick(e)"
        >{{ e }}</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { CHAT_EMOJI_CATEGORIES } from '@/constants/chat-emojis'
import { getRecentEmojis, recordRecentEmoji } from '@/utils/recent-emojis'

const RECENT_KEY = 'recent'

const emit = defineEmits<{
  pick: [emoji: string]
}>()

const props = defineProps<{
  visible?: boolean
}>()

const tabs = [{ key: RECENT_KEY, label: '最近' }, ...CHAT_EMOJI_CATEGORIES]

const activeTab = ref(RECENT_KEY)
const recentEmojis = ref<string[]>(getRecentEmojis())

const currentEmojis = computed(() => {
  if (activeTab.value === RECENT_KEY) return recentEmojis.value
  return CHAT_EMOJI_CATEGORIES.find((c) => c.key === activeTab.value)?.emojis ?? []
})

function refreshRecent() {
  recentEmojis.value = getRecentEmojis()
}

function onPick(emoji: string) {
  recentEmojis.value = recordRecentEmoji(emoji) ?? recentEmojis.value
  emit('pick', emoji)
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) refreshRecent()
  },
  { immediate: true },
)
</script>

<style lang="scss" scoped>
.emoji-picker {
  flex-shrink: 0;
  background: #ededed;
  padding-bottom: env(safe-area-inset-bottom);
}

.emoji-picker__tabs {
  display: flex;
  white-space: nowrap;
  padding: 12rpx 16rpx;
  background: #f7f7f7;
  border-bottom: 1px solid #e5e5e5;
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
  height: 480rpx;
  background: #ededed;
}

.emoji-picker__grid {
  display: flex;
  flex-wrap: wrap;
  padding: 12rpx 8rpx;
}

.emoji-picker__item {
  width: 80rpx;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  font-size: 44rpx;

  &:active {
    background: rgba(0, 0, 0, 0.06);
    border-radius: 8rpx;
  }
}

.emoji-picker__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.emoji-picker__empty-text {
  font-size: 26rpx;
  color: #909399;
}
</style>
