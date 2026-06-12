<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list">
    <ModuleHero title="登录日志" :count="total" />
    <SearchBar v-model="keyword" placeholder="搜索用户名" @search="onSearch" />
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
        :title="item.username || '未知用户'"
        :meta="`${formatDateTime(item.loginTime)} · ${item.ipaddr || ''} ${item.loginLocation || ''}`"
        :detail="[item.browser, item.os, item.msg].filter(Boolean).join(' · ')"
        :status-label="item.status === 0 ? '成功' : '失败'"
        :status-effect="item.status === 0 ? 'success' : 'danger'"
      />
      <EmptyState v-if="empty" title="暂无日志" icon="contact-o" />
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
import { getLoginLogList } from '@/api/system/login-log'
import { formatDateTime } from '@/utils/format'
import type { LoginLogVO } from '@/types/system'

const { allowed } = useModulePermission('system:loginLog:list')
const keyword = ref('')
const total = ref(0)
const { beginDate, endDate, toBeginTime, toEndTime, inRange } = useLogDateFilter()

const { list, loading, finished, empty, refresh, loadMore } = usePageList<LoginLogVO>(
  async (pageNo, pageSize) => {
    const res = await getLoginLogList({
      pageNo,
      pageSize,
      username: keyword.value.trim() || undefined,
      beginTime: toBeginTime(),
      endTime: toEndTime(),
    })
    const rows = (res.data?.list || []).filter((item) => inRange(item.loginTime))
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
