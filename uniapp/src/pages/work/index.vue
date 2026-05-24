<template>
  <PageTabShell>
    <view class="work-top">
      <SearchBar v-model="keyword" placeholder="搜索功能入口" @search="onSearch" />
    </view>

    <view v-if="recentItems.length && !keyword.trim()" class="section-block">
      <view class="section-head">
        <view class="section-head__left">
          <view class="section-head__accent section-head__accent--gold" />
          <text class="section-head__title">最近使用</text>
        </view>
      </view>
      <QuickEntryGrid :items="recentItems" @select="onEntryTap" />
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
import { onShow } from '@dcloudio/uni-app'
import PageTabShell from '@/components/common/PageTabShell/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import QuickEntryGrid from '@/components/common/QuickEntryGrid/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import { usePermission } from '@/composables/usePermission'
import { useTabBarPage } from '@/composables/useTabBarPage'
import { useUserStore } from '@/store/user'
import {
  mobileQuickEntries,
  quickEntryGroups,
  entryMap,
  type QuickEntry,
} from '@/constants/quickEntries'

const RECENT_STORAGE_KEY = 'work_recent_entries'
const MAX_RECENT = 4

useTabBarPage(1)

const userStore = useUserStore()
const { filterByEnabledMenu } = usePermission()
const keyword = ref('')
const recentKeys = ref<string[]>(loadRecentKeys())
const entries = computed(() => filterByEnabledMenu(mobileQuickEntries))

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

onShow(() => {
  if (userStore.isLoggedIn) {
    userStore.refreshUserStore().catch(() => {})
  }
})
</script>

<style lang="scss" scoped>

.work-top :deep(.search-bar) {
  margin-bottom: $card-gap;
  background: $color-bg-card;
  border: 1px solid $color-border;
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
