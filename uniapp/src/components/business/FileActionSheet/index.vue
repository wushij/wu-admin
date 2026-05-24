<template>
  <view v-if="visible" class="file-sheet" @touchmove.stop.prevent>
    <view class="file-sheet__mask" @click="close" />
    <view class="file-sheet__panel">
      <text v-if="fileName" class="file-sheet__title">{{ fileName }}</text>

      <view v-if="showDownload" class="file-sheet__action" @click="onDownload">
        <text class="file-sheet__action-text">下载</text>
      </view>
      <view v-if="showDelete" class="file-sheet__action file-sheet__action--danger" @click="onDelete">
        <text class="file-sheet__action-text">删除</text>
      </view>

      <view class="file-sheet__cancel" @click="close">取消</view>
    </view>
  </view>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean
  fileName?: string
  showDownload?: boolean
  showDelete?: boolean
}>()

const emit = defineEmits<{
  close: []
  download: []
  delete: []
}>()

function close() {
  emit('close')
}

function onDownload() {
  emit('download')
  emit('close')
}

function onDelete() {
  emit('delete')
  emit('close')
}
</script>

<style lang="scss" scoped>

.file-sheet {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.file-sheet__mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.42);
  backdrop-filter: blur(6px);
}

.file-sheet__panel {
  position: relative;
  z-index: 1;
  width: 100%;
  padding: 0 24rpx calc(24rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  animation: sheet-in 0.24s ease;
}

.file-sheet__title {
  display: block;
  margin-bottom: 16rpx;
  padding: 0 12rpx;
  text-align: center;
  font-size: $font-size-sm;
  color: rgba(255, 255, 255, 0.9);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-sheet__action {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 104rpx;
  margin-bottom: 16rpx;
  border-radius: $radius-xl;
  background: $color-bg-card;
  box-shadow: $shadow-elevated;

  &:active {
    background: $color-bg-muted;
  }

  &--danger .file-sheet__action-text {
    color: $color-danger;
  }
}

.file-sheet__action-text {
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.file-sheet__cancel {
  margin-top: 4rpx;
  height: 96rpx;
  line-height: 96rpx;
  text-align: center;
  border-radius: $radius-xl;
  background: $color-bg-card;
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-secondary;
  box-shadow: $shadow-card;

  &:active {
    background: $color-bg-muted;
  }
}

@keyframes sheet-in {
  from {
    opacity: 0;
    transform: translateY(100%);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
