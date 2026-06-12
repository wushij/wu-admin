<template>
  <view class="chat-composer">
    <view class="chat-composer__tool" @click="emit('toggle-emoji')">
      <text class="chat-composer__emoji">☺</text>
    </view>
    <view class="chat-composer__tool" @click="emit('pick-attach')">
      <text class="chat-composer__plus">+</text>
    </view>
    <input
      :value="modelValue"
      class="chat-composer__input"
      :placeholder="placeholder || '输入消息'"
      confirm-type="send"
      @input="onInput"
      @confirm="emit('send')"
    />
    <button class="chat-composer__send" :loading="loading" @click="emit('send')">发送</button>
  </view>
</template>

<script setup lang="ts">
defineProps<{
  modelValue: string
  loading?: boolean
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  send: []
  'pick-attach': []
  'toggle-emoji': []
}>()

function onInput(e: { detail: { value: string } }) {
  emit('update:modelValue', e.detail.value)
}
</script>

<style lang="scss" scoped>
.chat-composer {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-shrink: 0;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  background: #f7f7f7;
}

.chat-composer__tool {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64rpx;
  height: 64rpx;
}

.chat-composer__emoji {
  font-size: 40rpx;
  line-height: 1;
  color: #576b95;
}

.chat-composer__plus {
  font-size: 48rpx;
  line-height: 1;
  color: #576b95;
}

.chat-composer__input {
  flex: 1;
  height: 72rpx;
  padding: 0 24rpx;
  border-radius: 8rpx;
  background: #fff;
  font-size: 28rpx;
}

.chat-composer__send {
  min-width: 120rpx;
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 24rpx;
  background: #07c160;
  color: #fff;
  font-size: 28rpx;
  border-radius: 8rpx;
}
</style>
