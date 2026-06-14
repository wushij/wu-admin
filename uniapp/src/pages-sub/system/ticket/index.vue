<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list">
    <ModuleDarkHero
      title="工单管理"
      subtitle="跟踪处理进度"
      icon="records-o"
      theme="ticket"
      :count="total || list.length"
      count-label="工单"
    />
    <SegmentTabs v-model="statusMode" :tabs="statusTabs" scroll compact />
    <SearchBar v-model="keyword" placeholder="搜索工单标题" @search="onSearch" />

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
          <DictTag :dict-type="DICT_TYPE.TICKET_STATUS" :value="item.status" />
        </view>
        <text class="list-card__sub">
          {{ item.assigneeName || '未分配' }} · {{ item.creatorName || '—' }}
          <text v-if="item.priority"> · {{ priorityLabel(item.priority) }}</text>
        </text>
        <text class="list-card__sub" :class="{ 'list-card__sub--danger': isOverdue(item) }">
          {{ formatListTime(item.createTime) }}
          <text v-if="item.deadline"> · 截止 {{ formatDateTime(item.deadline, true) }}</text>
        </text>
      </ListCard>
      <EmptyState v-if="empty" title="暂无工单" icon="notes-o" />
      <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
    </scroll-view>

    <FabButton v-if="canCreate" @click="goCreate" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import FabButton from '@/components/common/FabButton/index.vue'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import ListCard from '@/components/common/ListCard/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import { usePageList } from '@/composables/usePageList'
import { useModulePermission } from '@/composables/useModulePermission'
import { getTicketPage } from '@/api/system/ticket'
import { getDictLabel, preloadDicts } from '@/composables/useDict'
import { formatListTime, formatDateTime } from '@/utils/format'
import { DICT_TYPE } from '@/constants/dict'
import type { TicketVO } from '@/types/system'

const { allowed, hasPerm } = useModulePermission('system:ticket:list')
const canCreate = computed(() => hasPerm('system:ticket:create'))
const keyword = ref('')
const total = ref(0)
const statusMode = ref('all')

const statusTabs = [
  { key: 'all', label: '全部' },
  { key: 'OPEN', label: '待处理' },
  { key: 'IN_PROGRESS', label: '处理中' },
  { key: 'RESOLVED', label: '已解决' },
  { key: 'CLOSED', label: '已关闭' },
]

const { list, loading, finished, empty, refresh, loadMore } = usePageList<TicketVO>(
  async (pageNo, pageSize) => {
    const res = await getTicketPage({
      pageNo,
      pageSize,
      title: keyword.value.trim() || undefined,
      status: statusMode.value === 'all' ? undefined : statusMode.value,
    })
    total.value = res.data?.total || 0
    return { list: res.data?.list || [], total: total.value }
  },
)

function priorityLabel(priority: string) {
  return getDictLabel(DICT_TYPE.TICKET_PRIORITY, priority)
}

function isOverdue(item: TicketVO) {
  if (!item.deadline || item.status === 'CLOSED' || item.status === 'RESOLVED') return false
  return new Date(item.deadline).getTime() < Date.now()
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages-sub/system/ticket/detail?id=${id}` })
}

function goCreate() {
  uni.navigateTo({ url: '/pages-sub/system/ticket/create' })
}

function onSearch() {
  refresh()
}

watch(statusMode, () => refresh())

onMounted(() => {
  preloadDicts([DICT_TYPE.TICKET_STATUS, DICT_TYPE.TICKET_PRIORITY])
  refresh()
})

onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.list-card__sub--danger {
  color: $color-danger;
}
</style>
