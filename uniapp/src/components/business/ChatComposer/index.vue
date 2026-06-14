<template>
  <view
    class="chat-composer"
    :class="{ 'chat-composer--panel-open': emojiActive }"
  >
    <view
      class="chat-composer__tool"
      @click.stop="emit('toggle-emoji')"
    >
      <text class="chat-composer__emoji">☺</text>
    </view>
    <view class="chat-composer__tool" @click.stop="emit('pick-attach')">
      <text class="chat-composer__plus">+</text>
    </view>
    <input
      :id="inputId"
      :value="modelValue"
      :focus="inputFocus"
      class="chat-composer__input"
      :placeholder="placeholder || '输入消息'"
      confirm-type="send"
      :adjust-position="false"
      :hold-keyboard="true"
      @input="onInput"
      @focus="onInputFocus"
      @confirm="emit('send')"
    />
    <view
      class="chat-composer__send"
      :class="{ 'chat-composer__send--loading': loading }"
      @mousedown.prevent
      @click.stop="emit('send')"
    >
      <text v-if="loading" class="chat-composer__send-text">发送中</text>
      <text v-else class="chat-composer__send-text">发送</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, nextTick, ref } from 'vue'

defineProps<{
  modelValue: string
  loading?: boolean
  placeholder?: string
  emojiActive?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  send: []
  'pick-attach': []
  'toggle-emoji': []
  'close-panels': []
}>()

const instance = getCurrentInstance()
const inputId = `chat-input-${instance?.uid ?? 0}`
const mpFocusOnce = ref(false)

const inputFocus = computed(() => {
  // #ifdef H5
  return undefined
  // #endif
  // #ifndef H5
  return mpFocusOnce.value
  // #endif
})

function onInput(e: { detail: { value: string } }) {
  emit('update:modelValue', e.detail.value)
}

function onInputFocus() {
  mpFocusOnce.value = false
  emit('close-panels')
}

async function focusInput() {
  await nextTick()
  setTimeout(() => {
    // #ifdef H5
    const host = document.getElementById(inputId)
    const el = host?.querySelector('input') ?? host
    if (el instanceof HTMLElement) el.focus({ preventScroll: true })
    // #endif
    // #ifndef H5
    mpFocusOnce.value = true
    // #endif
  }, 50)
}

defineExpose({ focusInput })
</script>

<style lang="scss" scoped>
.chat-composer {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-shrink: 0;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  background: #f7f7f7;

  &--panel-open {
    padding-bottom: 16rpx;
    border-bottom: 1px solid #e5e5e5;
  }
}

.chat-composer__tool {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64rpx;
  height: 64rpx;
  border-radius: 8rpx;
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
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 120rpx;
  height: 72rpx;
  padding: 0 24rpx;
  background: #07c160;
  border-radius: 8rpx;

  &--loading {
    opacity: 0.72;
  }

  &:active {
    opacity: 0.85;
  }
}

.chat-composer__send-text {
  color: #fff;
  font-size: 28rpx;
  line-height: 1;
}
</style>
