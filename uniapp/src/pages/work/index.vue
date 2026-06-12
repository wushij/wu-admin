<template>
  <PageTabShell>
    <SearchBar v-model="keyword" placeholder="搜索功能入口" class="work-search" @search="onSearch" />

    <template v-for="group in visibleGroups" :key="group.title">
      <text class="group-title">{{ group.title }}</text>
      <QuickEntryGrid v-if="group.items.length" :items="group.items" @select="onEntryTap" />
    </template>

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

useTabBarPage(1)

const { filterByPerm } = usePermission()
const keyword = ref('')
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

function onEntryTap(item: QuickEntry) {
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

.work-search :deep(.search-bar) {
  margin-bottom: $card-gap;
  background: $color-bg-card;
  border: 1px solid $color-border-light;
  box-shadow: $shadow-card;
}

.group-title {
  display: block;
  margin: 8rpx 0 12rpx;
  padding: 0 4rpx;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-secondary;
}
</style>
