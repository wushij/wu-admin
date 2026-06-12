<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list">
    <ModuleHero title="操作日志" :count="total" />
    <SearchBar v-model="keyword" placeholder="搜索模块标题" @search="onSearch" />
    <DateRangeFilter v-model:begin-date="beginDate" v-model:end-date="endDate" @change="onSearch" />
    <ListLoading v-if="loading && !list.length" />
    <scroll-view
      v-else
      scroll-y
      class="page-list__scroll"
      @scrolltolower="loadMore"
    >
      <LogExpandRow
        v-for="item in list"
        :key="item.id"
        :title="item.title || item.method || '操作'"
        :meta="`${item.operName || '—'} · ${formatDateTime(item.operTime)} · ${item.operIp || ''}`"
        :detail="[item.operUrl, item.operParam, item.jsonResult, item.errorMsg].filter(Boolean).join('\n\n')"
        :status-label="item.status === 0 ? '正常' : '异常'"
        :status-effect="item.status === 0 ? 'success' : 'danger'"
      />
      <EmptyState v-if="empty" title="暂无日志" icon="records-o" />
      <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleHero from '@/components/common/ModuleHero/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import DateRangeFilter from '@/components/common/DateRangeFilter/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import LogExpandRow from '@/components/common/LogExpandRow/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import { usePageList } from '@/composables/usePageList'
import { useModulePermission } from '@/composables/useModulePermission'
import { useLogDateFilter } from '@/composables/useLogDateFilter'
import { pageOperLog } from '@/api/system/oper-log'
import { formatDateTime } from '@/utils/format'
import type { OperLogVO } from '@/types/system'

const { allowed } = useModulePermission('system:operLog:list')
const keyword = ref('')
const total = ref(0)
const { beginDate, endDate, toBeginTime, toEndTime, inRange } = useLogDateFilter()

const { list, loading, finished, empty, refresh, loadMore } = usePageList<OperLogVO>(
  async (pageNo, pageSize) => {
    const res = await pageOperLog({
      pageNo,
      pageSize,
      title: keyword.value.trim() || undefined,
      beginTime: toBeginTime(),
      endTime: toEndTime(),
    })
    const rows = (res.data?.list || []).filter((item) => inRange(item.operTime))
    total.value = res.data?.total || 0
    return { list: rows, total: total.value }
  },
)

function onSearch() {
  refresh()
}
onMounted(refresh)
onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
.page-list {
  height: 100vh;
  box-sizing: border-box;
}

.page-list__scroll {
  height: calc(100% - 320rpx);
}
</style>
