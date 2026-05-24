<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list">
    <ModuleDarkHero
      title="审批中心"
      subtitle="流程审批与归档"
      icon="completed"
      theme="approval"
      :count="total || list.length"
      count-label="审批"
    />
    <SegmentTabs v-model="statusMode" :tabs="statusTabs" scroll />
    <SearchBar v-model="keyword" placeholder="搜索审批标题" @search="onSearch" />

    <scroll-view
      scroll-y
      class="page-list__scroll page-list__scroll--filter"
      @scrolltolower="loadMore"
    >
      <ListLoading v-if="loading && !list.length" />

      <template v-else>
        <ListCard v-for="item in list" :key="item.id" @click="goDetail(item.id)">
          <view class="list-card__top">
            <text class="list-card__title">{{ item.title }}</text>
            <DictTag :dict-type="DICT_TYPE.APPROVAL_STATUS" :value="item.status" />
          </view>
          <text class="list-card__sub">{{ formTypeLabel(item.formType) }} · {{ applicantLabel(item) }}</text>
          <text class="list-card__sub">{{ formatListTime(item.createTime) }}</text>
        </ListCard>
        <EmptyState v-if="empty" title="暂无审批单" icon="completed" />
        <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
      </template>
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
import { useListPageShowRefresh } from '@/composables/useListPageShowRefresh'
import { useModulePermission } from '@/composables/useModulePermission'
import { getApprovalPage } from '@/api/system/approval'
import { getDictLabel, preloadDicts } from '@/composables/useDict'
import { DICT_TYPE } from '@/constants/dict'
import { formatListTime } from '@/utils/format'
import { resolveApplicantDisplayName } from '@/utils/approval-display'
import type { ApprovalVO } from '@/types/system'

const { allowed, hasPerm } = useModulePermission('system:approval:list')
const canCreate = computed(() => hasPerm('system:approval:create'))
const keyword = ref('')
const total = ref(0)
const statusMode = ref('all')

const statusTabs = [
  { key: 'all', label: '全部' },
  { key: 'SUBMITTED', label: '待审批' },
  { key: 'APPROVED', label: '已通过' },
  { key: 'REJECTED', label: '已驳回' },
  { key: 'ARCHIVED', label: '已归档' },
]

const { list, loading, finished, empty, refresh, loadMore, refreshing } = usePageList<ApprovalVO>(
  async (pageNo, pageSize) => {
    const res = await getApprovalPage({
      pageNo,
      pageSize,
      title: keyword.value.trim() || undefined,
      status: statusMode.value === 'all' ? undefined : statusMode.value,
    })
    total.value = res.data?.total || 0
    return { list: res.data?.list || [], total: total.value }
  },
)

function formTypeLabel(type?: string) {
  return type ? getDictLabel(DICT_TYPE.APPROVAL_FORM_TYPE, type) : '—'
}

function applicantLabel(item: ApprovalVO) {
  return resolveApplicantDisplayName(item)
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages-sub/system/approval/detail?id=${id}` })
}

function goCreate() {
  uni.navigateTo({ url: '/pages-sub/system/approval/create' })
}

function onSearch() {
  refresh()
}

watch(statusMode, () => refresh())

useListPageShowRefresh(refresh, { loading, refreshing })

onMounted(() => {
  preloadDicts([DICT_TYPE.APPROVAL_STATUS, DICT_TYPE.APPROVAL_FORM_TYPE])
  refresh()
})

onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;
</style>
