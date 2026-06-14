<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded recycle-page">
    <ModuleDarkHero
      title="回收中心"
      :subtitle="`${visibleModules.length} 类业务数据 · 可恢复或彻底清除`"
      icon="notes-o"
      theme="log"
      :count="visiblePendingTotal"
      count-label="待处理"
    />

    <view class="recycle-stats">
      <view class="recycle-stats__item">
        <text class="recycle-stats__label">待处理</text>
        <text class="recycle-stats__num">{{ visiblePendingTotal }}</text>
      </view>
      <view class="recycle-stats__item">
        <text class="recycle-stats__label">当前分类</text>
        <text class="recycle-stats__num">{{ activePendingCount }}</text>
      </view>
    </view>

    <scroll-view scroll-x class="recycle-types-scroll" :show-scrollbar="false">
      <view class="recycle-types">
        <view
          v-for="mod in visibleModules"
          :key="mod.key"
          class="recycle-type-card"
          :class="{
            'recycle-type-card--active': activeType === mod.key,
            'recycle-type-card--pending': countFor(mod.key) > 0,
          }"
          @click="switchType(mod.key)"
        >
          <ModuleIcon :icon="mod.icon" :theme="mod.theme" size="sm" />
          <view class="recycle-type-card__body">
            <text class="recycle-type-card__label">{{ mod.label }}</text>
            <text class="recycle-type-card__count">{{ countFor(mod.key) }} 条</text>
          </view>
          <view v-if="countFor(mod.key) > 0" class="recycle-type-card__dot" />
        </view>
      </view>
    </scroll-view>

    <view class="recycle-panel form-panel">
      <view class="recycle-panel__head">
        <view class="recycle-panel__title-wrap">
          <ModuleIcon
            v-if="currentModule"
            :icon="currentModule.icon"
            :theme="currentModule.theme"
            size="xs"
          />
          <text class="recycle-panel__title">{{ currentModule?.label || '' }}回收列表</text>
        </view>
        <button
          class="recycle-refresh-btn outline-btn outline-btn--primary"
          :class="{ 'recycle-refresh-btn--spinning': refreshing }"
          @click="handleRefresh"
        >
          <text class="recycle-refresh-btn__icon">↻</text>
          <text>刷新</text>
        </button>
      </view>

      <view v-if="currentModule?.hint" class="recycle-hint">
        <text>{{ currentModule.hint }}</text>
      </view>

      <view v-if="searchFields.length" class="recycle-search">
        <SearchBar
          v-model="searchKeyword"
          :placeholder="searchFields[0]?.placeholder || '搜索'"
          @search="handleSearch"
        />
        <button class="recycle-search__reset" @click="resetSearch">重置</button>
      </view>

      <scroll-view
        scroll-y
        class="recycle-panel__scroll"
        @scrolltolower="loadMore"
      >
        <ListCard v-for="item in list" :key="String(item.id)" @click="goDetail(item)">
          <view class="recycle-row">
            <UserAvatar
              v-if="rowThumb(item)?.type === 'avatar'"
              :src="String(item.avatar || '')"
              :name="rowTitle(item)"
              size="sm"
              class="recycle-row__avatar"
            />
            <FileThumb
              v-else-if="rowThumb(item)?.type === 'file' && rowThumb(item)?.file"
              :file="rowThumb(item)!.file!"
              class="recycle-row__file-thumb"
            />
            <image
              v-else-if="rowThumb(item)?.type === 'image'"
              class="recycle-row__thumb"
              :src="rowThumb(item)!.src"
              mode="aspectFill"
            />
            <view class="recycle-row__main">
              <view class="list-card__top">
                <text class="list-card__title">{{ rowTitle(item) }}</text>
                <IconFont name="arrow" :size="28" color="#c0c4cc" />
              </view>
              <view v-if="rowPreviewLines(item).length" class="recycle-row__lines">
                <text
                  v-for="(line, idx) in rowPreviewLines(item)"
                  :key="idx"
                  class="recycle-row__line"
                >
                  {{ line.label }}：{{ line.value }}
                </text>
              </view>
              <text v-if="rowMeta(item)" class="recycle-row__time">{{ rowMeta(item) }}</text>
            </view>
            <view v-if="canDelete" class="recycle-row__actions">
              <button class="outline-btn outline-btn--primary" @click.stop="onRestore(item)">恢复</button>
              <button class="outline-btn outline-btn--danger" @click.stop="onDelete(item)">清除</button>
            </view>
          </view>
        </ListCard>
        <EmptyState v-if="empty && !loading" title="暂无已删除数据" icon="notes-o" />
        <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
      </scroll-view>
    </view>

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import IconFont from '@/components/common/IconFont/index.vue'
import ListCard from '@/components/common/ListCard/index.vue'
import UserAvatar from '@/components/business/UserAvatar/index.vue'
import FileThumb from '@/components/business/FileThumb/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { useRecycleCenter } from '@/composables/useRecycleCenter'

const {
  allowed,
  activeType,
  searchKeyword,
  visibleModules,
  currentModule,
  visiblePendingTotal,
  activePendingCount,
  canDelete,
  searchFields,
  list,
  loading,
  refreshing,
  finished,
  empty,
  countFor,
  switchType,
  handleSearch,
  resetSearch,
  handleRefresh,
  loadMore,
  rowTitle,
  rowPreviewLines,
  rowMeta,
  rowThumb,
  goDetail,
  onRestore,
  onDelete,
  bootstrap,
} = useRecycleCenter()

onMounted(bootstrap)

const skipNextShowRefresh = ref(true)
onShow(async () => {
  if (skipNextShowRefresh.value) {
    skipNextShowRefresh.value = false
    return
  }
  if (allowed.value && activeType.value) {
    await handleRefresh()
  }
})

onPullDownRefresh(async () => {
  await handleRefresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.recycle-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
}

.recycle-stats {
  display: flex;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.recycle-stats__item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  padding: 14rpx 20rpx;
  border-radius: $radius-md;
  background: $color-bg-card;
  border: 1px solid $color-border-light;
}

.recycle-stats__num {
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: $color-primary;
  line-height: 1;
}

.recycle-stats__label {
  font-size: $font-size-xs;
  color: $color-text-secondary;
  line-height: 1;
}

.recycle-types-scroll {
  width: 100%;
  margin-bottom: 16rpx;
  white-space: nowrap;
}

.recycle-types {
  display: inline-flex;
  gap: 12rpx;
  padding: 2rpx 0 6rpx;
}

.recycle-type-card {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 12rpx;
  min-width: 168rpx;
  padding: 14rpx 18rpx;
  border-radius: $radius-md;
  background: $color-bg-card;
  border: 1px solid $color-border-light;
  box-shadow: none;
}

.recycle-type-card--active {
  border-color: rgba(99, 102, 241, 0.45);
  box-shadow: 0 6rpx 20rpx rgba(99, 102, 241, 0.12);
}

.recycle-type-card--pending:not(.recycle-type-card--active) {
  border-color: rgba(99, 102, 241, 0.2);
}

.recycle-type-card__body {
  display: flex;
  flex-direction: column;
  gap: 2rpx;
  min-width: 0;
}

.recycle-type-card__label {
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.recycle-type-card__count {
  font-size: 22rpx;
  color: $color-text-secondary;
}

.recycle-type-card__dot {
  position: absolute;
  top: 10rpx;
  right: 10rpx;
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: $color-primary;
}

.recycle-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.recycle-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 32rpx;
  border-bottom: 1px solid $color-border-light;
  background: linear-gradient(180deg, $color-bg-muted 0%, $color-bg-card 100%);
}

.recycle-panel__title-wrap {
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-width: 0;
}

.recycle-panel__title {
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.recycle-panel__refresh {
  flex-shrink: 0;
}

.recycle-refresh-btn {
  gap: 8rpx;
  min-width: auto;
  height: 60rpx;
  padding: 0 24rpx;
}

.recycle-refresh-btn__icon {
  font-size: 28rpx;
  line-height: 1;
  transition: transform 0.2s ease;
}

.recycle-refresh-btn--spinning .recycle-refresh-btn__icon {
  animation: recycle-spin 0.8s linear infinite;
}

@keyframes recycle-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.recycle-hint {
  margin: 16rpx 24rpx 0;
  padding: 16rpx 20rpx;
  border-radius: $radius-md;
  background: rgba(230, 162, 60, 0.12);
  border: 1px solid rgba(230, 162, 60, 0.22);
  font-size: $font-size-xs;
  color: $color-warning;
  line-height: 1.55;
}

.recycle-search {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 24rpx 0;
}

.recycle-search :deep(.search-bar) {
  flex: 1;
  min-width: 0;
  margin-bottom: 0;
}

.recycle-search__reset {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 72rpx;
  padding: 0 28rpx;
  margin: 0;
  font-size: $font-size-sm;
  line-height: 1;
  color: $color-text-secondary;
  background: $color-bg-muted;
  border: none;
  border-radius: 36rpx;

  &::after {
    border: none;
  }

  &:active {
    opacity: 0.82;
  }
}

.recycle-panel__scroll {
  flex: 1;
  min-height: 0;
  padding: 16rpx 24rpx 0;
}

.recycle-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.recycle-row__avatar {
  flex-shrink: 0;
}

.recycle-row__file-thumb,
.recycle-row__thumb {
  flex-shrink: 0;
}

.recycle-row__thumb {
  width: 88rpx;
  height: 88rpx;
  border-radius: 20rpx;
  background: $color-bg-muted;
  border: 1px solid $color-border-light;
}

.recycle-row__main {
  flex: 1;
  min-width: 0;
}

.recycle-row__lines {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  margin-top: 12rpx;
}

.recycle-row__line {
  font-size: $font-size-sm;
  color: $color-text-secondary;
  line-height: 1.45;
}

.recycle-row__time {
  display: block;
  margin-top: 12rpx;
  font-size: $font-size-xs;
  color: $color-text-placeholder;
}

.recycle-row__actions {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 12rpx;
  flex-shrink: 0;
  align-self: center;
  max-width: 220rpx;

  .outline-btn {
    min-width: auto;
    height: 56rpx;
    padding: 0 20rpx;
    font-size: 22rpx;
  }
}
</style>
