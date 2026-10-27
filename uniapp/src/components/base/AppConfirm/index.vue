<template>
  <view v-if="visible" class="app-confirm" @touchmove.stop.prevent>
    <view class="app-confirm__mask" @tap.stop="onCancel" />
    <view class="app-confirm__panel" @tap.stop>
      <text v-if="title" class="app-confirm__title">{{ title }}</text>
      <text
        class="app-confirm__content"
        :class="{ 'app-confirm__content--left': contentAlign === 'left' }"
      >{{ content }}</text>
      <input
        v-if="editable"
        v-model="inputValue"
        class="app-confirm__input"
        :type="inputType === 'password' ? 'text' : 'text'"
        :password="inputType === 'password'"
        :placeholder="placeholderText || '请输入'"
        :maxlength="200"
      />
      <view class="app-confirm__actions" :class="{ 'app-confirm__actions--solo': showCancel === false }">
        <view v-if="showCancel !== false" class="app-confirm__btn app-confirm__btn--ghost" @tap.stop="onCancel">
          {{ cancelText }}
        </view>
        <view
          class="app-confirm__btn"
          :class="tone === 'danger' ? 'app-confirm__btn--danger' : 'app-confirm__btn--primary'"
          @tap.stop="onConfirm"
        >
          {{ confirmText }}
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ConfirmTone } from '@/store/dialog'

const props = defineProps<{
  visible: boolean
  title?: string
  content: string
  confirmText?: string
  cancelText?: string
  tone?: ConfirmTone
  editable?: boolean
  placeholderText?: string
  inputType?: 'text' | 'password'
  inputValue?: string
  showCancel?: boolean
  contentAlign?: 'left' | 'center'
}>()

const emit = defineEmits<{
  confirm: []
  cancel: []
  'update:inputValue': [value: string]
}>()

const inputValue = computed({
  get: () => props.inputValue || '',
  set: (value: string) => emit('update:inputValue', value),
})

function onConfirm() {
  emit('confirm')
}

function onCancel() {
  emit('cancel')
}
</script>

<style lang="scss" scoped>

.app-confirm {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48rpx;
  box-sizing: border-box;
}

.app-confirm__mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.46);
  backdrop-filter: blur(8px);
}

.app-confirm__panel {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 620rpx;
  padding: 44rpx 36rpx 36rpx;
  border-radius: 28rpx;
  background: $color-bg-card;
  box-shadow: 0 24rpx 64rpx rgba(15, 23, 42, 0.18);
  animation: confirm-in 0.22s ease;
}

.app-confirm__title {
  display: block;
  text-align: center;
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.app-confirm__content {
  display: block;
  margin-top: 20rpx;
  text-align: center;
  font-size: $font-size-base;
  color: $color-text-secondary;
  line-height: 1.65;
  white-space: pre-line;

  &--left {
    text-align: left;
    padding: 24rpx;
    border-radius: $radius-md;
    background: $color-bg-muted;
    color: $color-text-regular;
  }
}

.app-confirm__input {
  display: block;
  width: 100%;
  height: 84rpx;
  margin-top: 28rpx;
  padding: 0 24rpx;
  box-sizing: border-box;
  border-radius: $radius-lg;
  background: $color-bg-muted;
  font-size: $font-size-base;
  color: $color-text-primary;
}

.app-confirm__actions {
  display: flex;
  gap: 20rpx;
  margin-top: 36rpx;

  &--solo .app-confirm__btn {
    flex: 1;
  }
}

.app-confirm__btn {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  text-align: center;
  border-radius: $radius-lg;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;

  &:active {
    opacity: 0.88;
  }
}

.app-confirm__btn--ghost {
  color: $color-text-regular;
  background: $color-bg-muted;
}

.app-confirm__btn--primary {
  color: #fff;
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  box-shadow: 0 10rpx 24rpx rgba(79, 70, 229, 0.28);
}

.app-confirm__btn--danger {
  color: #fff;
  background: linear-gradient(135deg, #ef4444, #f87171);
  box-shadow: 0 10rpx 24rpx rgba(239, 68, 68, 0.24);
}

@keyframes confirm-in {
  from {
    opacity: 0;
    transform: scale(0.94) translateY(12rpx);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
</style>
