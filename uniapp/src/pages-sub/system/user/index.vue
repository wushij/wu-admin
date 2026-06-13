<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list">
    <SubPageBackBar />
    <ModuleHero title="用户管理" :count="total || list.length" subtitle="账号、状态与权限" />
    <SegmentTabs v-model="statusMode" :tabs="statusTabs" />
    <SearchBar v-model="keyword" placeholder="搜索用户名 / 昵称 / 手机号" @search="onSearch" />

    <scroll-view
      scroll-y
      class="page-list__scroll page-list__scroll--filter"
      @scrolltolower="loadMore"
    >
      <ListLoading v-if="loading && !list.length" />

      <template v-else>
        <ListCard v-for="user in list" :key="user.id" @click="goDetail(user.id)">
          <view class="user-row">
            <UserAvatar
              :src="user.avatar"
              :name="user.nickname || user.username"
              size="sm"
              class="user-row__avatar"
            />
            <view class="user-row__main">
              <view class="list-card__top">
                <text class="list-card__title">{{ user.nickname || user.username }}</text>
                <view class="user-row__tags">
                  <DictTag
                    :label="user.status === 1 ? '启用' : '停用'"
                    :effect="user.status === 1 ? 'success' : 'danger'"
                  />
                  <DictTag
                    v-if="user.loginLocked"
                    label="登录锁定"
                    effect="warning"
                  />
                  <DictTag
                    v-if="user.loginIpLocked"
                    label="IP 锁定"
                    effect="danger"
                  />
                  <DictTag
                    v-else-if="(user.loginFailCount ?? 0) > 0"
                    :label="`失败 ${user.loginFailCount} 次`"
                    effect="default"
                  />
                </view>
              </view>
              <text class="list-card__sub">{{ user.mobile || user.username }}</text>
              <text v-if="user.deptName" class="list-card__sub">{{ user.deptName }}</text>
            </view>
          </view>
        </ListCard>
        <EmptyState v-if="empty" title="暂无用户" icon="friends-o" />
        <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
      </template>
    </scroll-view>

    <FabButton v-if="canCreate" @click="goCreate" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import SubPageBackBar from '@/components/common/SubPageBackBar/index.vue'
import ModuleHero from '@/components/common/ModuleHero/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import FabButton from '@/components/common/FabButton/index.vue'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import ListCard from '@/components/common/ListCard/index.vue'
import UserAvatar from '@/components/business/UserAvatar/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import { usePageList } from '@/composables/usePageList'
import { useModulePermission } from '@/composables/useModulePermission'
import { getUserPage } from '@/api/system/user'
import type { UserVO } from '@/types/user'

import { useH5ListPageNav } from '@/composables/useH5ListPageNav'

const { allowed, hasPerm } = useModulePermission('system:user:list')
useH5ListPageNav()
const canCreate = computed(() => hasPerm('system:user:create'))
const keyword = ref('')
const total = ref(0)
const statusMode = ref('all')

const statusTabs = [
  { key: 'all', label: '全部' },
  { key: '1', label: '启用' },
  { key: '0', label: '停用' },
]

const { list, loading, refreshing, finished, empty, refresh, loadMore } = usePageList<UserVO>(
  async (pageNo, pageSize) => {
    const q = keyword.value.trim()
    const res = await getUserPage({
      pageNo,
      pageSize,
      keyword: q || undefined,
      status: statusMode.value === 'all' ? undefined : Number(statusMode.value),
    })
    total.value = res.data?.total || 0
    return { list: res.data?.list || [], total: total.value }
  },
)

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages-sub/system/user/detail?id=${id}` })
}

function goCreate() {
  uni.navigateTo({ url: '/pages-sub/system/user/edit?mode=create' })
}

function onSearch() {
  refresh()
}

watch(statusMode, () => refresh())

onMounted(refresh)

const skipNextShowRefresh = ref(true)
onShow(async () => {
  if (skipNextShowRefresh.value) {
    skipNextShowRefresh.value = false
    return
  }
  if (loading.value || refreshing.value) return
  await refresh()
})

onPullDownRefresh(async () => {
  try {
    await refresh()
  } finally {
    uni.stopPullDownRefresh()
  }
})
</script>

<style lang="scss" scoped>
@import '@/styles/common.scss';

.user-row {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
}

.user-row__avatar {
  flex-shrink: 0;
}

.user-row__main {
  flex: 1;
  min-width: 0;
}

.user-row__tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8rpx;
}
</style>
