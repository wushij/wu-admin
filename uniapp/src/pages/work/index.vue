<template>
  <PageTabShell>
    <view class="work-header">
      <view class="work-hero">
        <view class="work-hero__pattern" />
        <view class="work-hero__glow" />
        <view class="work-hero__body">
          <view class="work-hero__main">
            <text class="work-hero__eyebrow">WORKSPACE</text>
            <text class="work-hero__title">功能中心</text>
            <text class="work-hero__subtitle">高效管理，触手可及</text>
          </view>
          <view class="work-hero__metrics">
            <view class="work-hero__metric">
              <text class="work-hero__metric-value">{{ entries.length }}</text>
              <text class="work-hero__metric-label">可用功能</text>
            </view>
            <view class="work-hero__metric">
              <text class="work-hero__metric-value">{{ visibleGroups.length }}</text>
              <text class="work-hero__metric-label">功能模块</text>
            </view>
          </view>
        </view>
      </view>

      <SearchBar
        v-model="keyword"
        placeholder="搜索功能入口"
        class="work-search"
        @search="onSearch"
      />
    </view>

    <view v-if="recentItems.length && !keyword.trim()" class="section-block">
      <view class="section-head">
        <view class="section-head__left">
          <view class="section-head__accent section-head__accent--gold" />
          <text class="section-head__title">最近使用</text>
        </view>
      </view>
      <QuickEntryGrid :items="recentItems" show-desc @select="onEntryTap" />
    </view>

    <view v-for="group in visibleGroups" :key="group.title" class="section-block">
      <QuickEntryGrid
        v-if="group.items.length"
        :title="group.title"
        :items="group.items"
        @select="onEntryTap"
      />
    </view>

    <EmptyState
      v-if="!visibleGroups.length"
      :title="entries.length ? '无匹配入口' : '暂无可用功能'"
      :description="entries.length ? '换个关键词试试' : '请联系管理员分配菜单权限'"
      icon="apps-o"
    />
  </PageTabShell>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import PageTabShell from '@/components/common/PageTabShell/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import QuickEntryGrid from '@/components/common/QuickEntryGrid/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import { usePermission } from '@/composables/usePermission'
import { useTabBarPage } from '@/composables/useTabBarPage'
import {
  mobileQuickEntries,
  quickEntryGroups,
  entryMap,
  type QuickEntry,
} from '@/constants/quickEntries'

const RECENT_STORAGE_KEY = 'work_recent_entries'
const MAX_RECENT = 4

useTabBarPage(1)

const { filterByPerm } = usePermission()
const keyword = ref('')
const recentKeys = ref<string[]>(loadRecentKeys())
const entries = computed(() => filterByPerm(mobileQuickEntries))

const filteredEntries = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q) return entries.value
  return entries.value.filter(
    (item) => item.name.toLowerCase().includes(q) || item.desc.toLowerCase().includes(q),
  )
})

const visibleGroups = computed(() => {
  const allowed = new Set(filteredEntries.value.map((e) => e.key))
  return quickEntryGroups
    .map((g) => ({
      title: g.title,
      items: g.keys.map((k) => entryMap[k]).filter((e) => e && allowed.has(e.key)),
    }))
    .filter((g) => g.items.length > 0)
})

const recentItems = computed(() => {
  const allowed = new Set(entries.value.map((e) => e.key))
  return recentKeys.value
    .map((key) => entryMap[key])
    .filter((e): e is QuickEntry => !!e && allowed.has(e.key))
    .slice(0, MAX_RECENT)
})

function loadRecentKeys(): string[] {
  try {
    const raw = uni.getStorageSync(RECENT_STORAGE_KEY)
    return raw ? (JSON.parse(raw as string) as string[]) : []
  } catch {
    return []
  }
}

function saveRecentKey(key: string) {
  const next = [key, ...recentKeys.value.filter((k) => k !== key)].slice(0, MAX_RECENT)
  recentKeys.value = next
  uni.setStorageSync(RECENT_STORAGE_KEY, JSON.stringify(next))
}

function onEntryTap(item: QuickEntry) {
  saveRecentKey(item.key)
  if (item.path) {
    uni.navigateTo({ url: item.path })
    return
  }
  uni.showToast({ title: `${item.name} 即将上线`, icon: 'none' })
}

function onSearch() {
  /* computed 过滤 */
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.work-header {
  margin-bottom: $section-gap;
}

.work-hero {
  position: relative;
  border-radius: $radius-lg;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(135deg, #010710 0%, #0f1a2e 55%, #1a1040 100%);
  box-shadow: 0 12rpx 40rpx rgba(0, 0, 0, 0.14);
}

.work-hero__pattern {
  position: absolute;
  inset: 0;
  opacity: 0.07;
  background-image: radial-gradient(rgba(255, 255, 255, 0.8) 1px, transparent 1px);
  background-size: 32rpx 32rpx;
  pointer-events: none;
}

.work-hero__glow {
  position: absolute;
  top: -40%;
  right: -6%;
  width: 280rpx;
  height: 280rpx;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.28) 0%, transparent 70%);
  pointer-events: none;
}

.work-hero__body {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24rpx;
  padding: 36rpx 32rpx;
}

.work-hero__main {
  flex: 1;
  min-width: 0;
}

.work-hero__eyebrow {
  display: block;
  font-size: 20rpx;
  font-weight: $font-weight-semibold;
  letter-spacing: 0.16em;
  color: rgba(255, 255, 255, 0.45);
}

.work-hero__title {
  display: block;
  margin-top: 10rpx;
  font-size: $font-size-2xl;
  font-weight: $font-weight-bold;
  color: #fff;
  line-height: 1.2;
}

.work-hero__subtitle {
  display: block;
  margin-top: 10rpx;
  font-size: $font-size-sm;
  color: rgba(255, 255, 255, 0.62);
  line-height: 1.5;
}

.work-hero__metrics {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  flex-shrink: 0;
}

.work-hero__metric {
  min-width: 120rpx;
  padding: 14rpx 20rpx;
  border-radius: $radius-md;
  background: rgba(255, 255, 255, 0.1);
  text-align: center;
}

.work-hero__metric-value {
  display: block;
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: #fff;
  line-height: 1.2;
}

.work-hero__metric-label {
  display: block;
  margin-top: 2rpx;
  font-size: 18rpx;
  color: rgba(255, 255, 255, 0.55);
}

.work-search :deep(.search-bar) {
  margin-top: 20rpx;
  margin-bottom: 0;
  height: 80rpx;
  padding: 0 28rpx;
  border-radius: $radius-lg;
  background: $color-bg-card;
  border: 1px solid $color-border-light;
  box-shadow: $shadow-card;
}

.section-block {
  margin-bottom: $card-gap;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4rpx;
  padding: 0 8rpx;
}

.section-head__left {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.section-head__accent {
  width: 6rpx;
  height: 28rpx;
  border-radius: 3rpx;
  background: linear-gradient(180deg, $color-primary 0%, #8b5cf6 100%);

  &--gold {
    background: linear-gradient(180deg, #f59e0b 0%, #fbbf24 100%);
  }
}

.section-head__title {
  font-size: $font-size-md;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}
</style>
