<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list">
    <ModuleDarkHero
      title="角色管理"
      subtitle="角色、权限与菜单"
      icon="shield-o"
      theme="role"
      :count="total || list.length"
      count-label="角色"
    />
    <SearchBar v-model="keyword" placeholder="搜索角色名称" @search="onSearch" />

    <ListLoading v-if="loading && !list.length" />

    <scroll-view
      v-else
      scroll-y
      class="page-list__scroll"
      @scrolltolower="loadMore"
    >
      <ListCard v-for="item in list" :key="item.id" @click="onCardTap(item)">
        <view class="list-card__top">
          <text class="list-card__title">{{ item.name }}</text>
          <DictTag
            :label="item.status === 1 ? '启用' : '停用'"
            :effect="item.status === 1 ? 'success' : 'danger'"
          />
        </view>
        <text class="list-card__sub">编码：{{ item.code }}</text>
        <text v-if="item.remark" class="list-card__sub">{{ item.remark }}</text>
      </ListCard>
      <EmptyState v-if="empty" title="暂无角色" icon="shield-o" />
      <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
    </scroll-view>

    <FabButton v-if="canCreate" @click="goCreate" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import FabButton from '@/components/common/FabButton/index.vue'
import ListCard from '@/components/common/ListCard/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import { usePageList } from '@/composables/usePageList'
import { useModulePermission } from '@/composables/useModulePermission'
import { getRolePage, deleteRole } from '@/api/system/role'
import type { RoleVO } from '@/types/system'

const { allowed, hasPerm } = useModulePermission('system:role:list')
const canCreate = computed(() => hasPerm('system:role:create'))
const canUpdate = computed(() => hasPerm('system:role:update'))
const canDelete = computed(() => hasPerm('system:role:delete'))
const keyword = ref('')
const total = ref(0)

const { list, loading, finished, empty, refresh, loadMore } = usePageList<RoleVO>(
  async (pageNo, pageSize) => {
    const res = await getRolePage({ pageNo, pageSize, name: keyword.value.trim() || undefined })
    total.value = res.data?.total || 0
    return { list: res.data?.list || [], total: total.value }
  },
)

function onCardTap(item: RoleVO) {
  const actions: string[] = []
  if (canUpdate.value) actions.push('编辑', '分配权限')
  if (canDelete.value) actions.push('删除')
  if (!actions.length) return
  uni.showActionSheet({
    itemList: actions,
    success: (res) => {
      const action = actions[res.tapIndex]
      if (action === '编辑') goEdit(item.id)
      else if (action === '分配权限') goMenuAssign(item)
      else if (action === '删除') confirmDelete(item)
    },
  })
}

function goCreate() {
  uni.navigateTo({ url: '/pages-sub/system/role/edit?mode=create' })
}

function goEdit(id: number) {
  uni.navigateTo({ url: `/pages-sub/system/role/edit?id=${id}` })
}

function goMenuAssign(item: RoleVO) {
  uni.navigateTo({
    url: `/pages-sub/system/role/menu-assign?id=${item.id}&name=${encodeURIComponent(item.name)}`,
  })
}

function confirmDelete(item: RoleVO) {
  uni.showModal({
    title: '删除角色',
    content: `确定删除「${item.name}」？`,
    confirmColor: '#f56c6c',
    success: async (res) => {
      if (!res.confirm) return
      await deleteRole(item.id)
      uni.showToast({ title: '已删除', icon: 'success' })
      await refresh()
    },
  })
}

function onSearch() { refresh() }
onMounted(refresh)
onPullDownRefresh(async () => { await refresh(); uni.stopPullDownRefresh() })
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;
</style>
