<template>
  <view class="permission-block">
    <view class="permission-block__icon">
      <IconFont name="lock" :size="64" color="#c0c4cc" />
    </view>
    <text class="permission-block__title">暂无访问权限</text>
    <text class="permission-block__desc">请联系管理员分配菜单权限</text>
  </view>
</template>

<script setup lang="ts">
import IconFont from '@/components/common/IconFont/index.vue'
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.permission-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 48rpx;
}

.permission-block__icon {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 24rpx;
  border-radius: 50%;
  background: linear-gradient(180deg, $color-bg-muted 0%, $color-bg-page 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.permission-block__title {
  font-size: $font-size-base;
  color: $color-text-secondary;
}

.permission-block__desc {
  margin-top: 12rpx;
  font-size: $font-size-sm;
  color: $color-text-placeholder;
}
</style>
