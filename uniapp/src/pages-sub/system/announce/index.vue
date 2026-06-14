<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list">
    <ModuleDarkHero
      title="通知管理"
      subtitle="发布与管理系统通知"
      icon="bell"
      theme="notice"
      :count="total || list.length"
      count-label="通知"
    />
    <SegmentTabs v-model="statusMode" :tabs="statusTabs" />
    <SearchBar v-model="keyword" placeholder="搜索通知标题" @search="onSearch" />

    <ListLoading v-if="loading && !list.length" />

    <scroll-view
      v-else
      scroll-y
      class="page-list__scroll page-list__scroll--filter"
      @scrolltolower="loadMore"
    >
      <ListCard v-for="item in list" :key="item.id" @click="goDetail(item.id)">
        <view class="list-card__top">
          <text class="list-card__title">{{ item.title }}</text>
          <DictTag :label="item.status === 1 ? '已发布' : '草稿'" :effect="item.status === 1 ? 'success' : 'warning'" />
        </view>
        <text class="list-card__sub">{{ noticeTypeLabel(item.noticeType) }} · {{ item.createName || '—' }}</text>
        <text class="list-card__sub">{{ formatListTime(item.createTime) }}</text>
      </ListCard>
      <EmptyState v-if="empty" title="暂无通知" icon="bell" />
      <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
    </scroll-view>

    <FabButton v-if="canCreate" @click="goCreate" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { useListPageShowRefresh } from '@/composables/useListPageShowRefresh'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import FabButton from '@/components/common/FabButton/index.vue'
import ListCard from '@/components/common/ListCard/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import { usePageList } from '@/composables/usePageList'
import { useModulePermission } from '@/composables/useModulePermission'
import { getAnnouncePage } from '@/api/message'
import { formatListTime } from '@/utils/format'
import type { AnnounceVO } from '@/types/message'

const { allowed, hasPerm } = useModulePermission('system:announce:list')
const canCreate = computed(() => hasPerm('system:announce:create'))
const keyword = ref('')
const total = ref(0)
const statusMode = ref('all')
const statusTabs = [
  { key: 'all', label: '全部' },
  { key: '0', label: '草稿' },
  { key: '1', label: '已发布' },
]

const { list, loading, finished, empty, refresh, loadMore, refreshing } = usePageList<AnnounceVO>(
  async (pageNo, pageSize) => {
    const res = await getAnnouncePage({
      pageNo,
      pageSize,
      title: keyword.value.trim() || undefined,
      status: statusMode.value === 'all' ? undefined : Number(statusMode.value),
    })
    total.value = res.data?.total || 0
    return { list: res.data?.list || [], total: total.value }
  },
)

function noticeTypeLabel(type?: number) {
  return type === 2 ? '公告' : '通知'
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages-sub/system/announce/detail?id=${id}` })
}

function goCreate() {
  uni.navigateTo({ url: '/pages-sub/system/announce/form?mode=create' })
}

function onSearch() { refresh() }
watch(statusMode, () => refresh())
useListPageShowRefresh(refresh, { loading, refreshing })
onMounted(refresh)
onPullDownRefresh(async () => { await refresh(); uni.stopPullDownRefresh() })
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;
</style>
