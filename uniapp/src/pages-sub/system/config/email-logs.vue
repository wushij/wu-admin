<template>
  <view class="page-padded email-logs-page">
    <SearchBar v-model="email" placeholder="搜索接收邮箱" @search="refresh" />
    <SegmentTabs v-model="statusFilter" :tabs="statusTabs" compact />

    <scroll-view
      scroll-y
      class="email-logs-page__scroll"
      @scrolltolower="loadMore"
    >
      <ListCard v-for="log in list" :key="log.id || `${log.email}-${log.createTime}`">
        <view class="list-card__top">
          <text class="list-card__title">{{ log.email }}</text>
          <DictTag
            :label="emailStatusText(log.status)"
            :effect="log.status === 1 ? 'success' : log.status === 2 ? 'danger' : 'warning'"
          />
        </view>
        <text class="list-card__sub">{{ log.subject || '系统邮件' }}</text>
        <text class="list-card__sub">{{ log.provider || '—' }} · {{ log.createTime || '—' }}</text>
        <text v-if="log.resultMsg" class="list-card__sub">{{ log.resultMsg }}</text>
      </ListCard>
      <EmptyState v-if="empty" title="暂无发送记录" icon="contact-o" />
      <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import SearchBar from '@/components/common/SearchBar/index.vue'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import ListCard from '@/components/common/ListCard/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import { useEmailLogs } from '@/composables/useEmailLogs'

const statusFilter = ref('all')
const statusTabs = [
  { key: 'all', label: '全部' },
  { key: 'success', label: '成功' },
  { key: 'fail', label: '失败' },
]

const { list, loading, finished, empty, email, emailStatusText, refresh, loadMore, setStatusFilter } =
  useEmailLogs()

watch(statusFilter, setStatusFilter)

onMounted(refresh)
onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.email-logs-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.email-logs-page__scroll {
  flex: 1;
  min-height: 0;
  margin-top: 16rpx;
}
</style>
