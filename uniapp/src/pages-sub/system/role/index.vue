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
        <text class="list-card__sub">数据范围：{{ dataScopeLabel(item.dataScope) }}</text>
        <text v-if="item.remark" class="list-card__sub">{{ item.remark }}</text>
      </ListCard>
      <EmptyState v-if="empty" title="暂无角色" icon="shield-o" />
      <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
    </scroll-view>

    <FabButton v-if="canCreate" @click="goCreate" />
    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { useListPageShowRefresh } from '@/composables/base/useListPageShowRefresh'
import ModuleDarkHero from '@/components/layout/ModuleDarkHero/index.vue'
import SearchBar from '@/components/base/SearchBar/index.vue'
import ListFooter from '@/components/base/ListFooter/index.vue'
import ListLoading from '@/components/base/ListLoading/index.vue'
import EmptyState from '@/components/base/EmptyState/index.vue'
import DictTag from '@/components/base/DictTag/index.vue'
import FabButton from '@/components/base/FabButton/index.vue'
import ListCard from '@/components/base/ListCard/index.vue'
import PermissionBlock from '@/components/base/PermissionBlock/index.vue'
import AppDialogHost from '@/components/base/AppDialogHost/index.vue'
import { showConfirm, showActionSheet, type ActionSheetItem } from '@/utils/app-dialog'
import { usePageList } from '@/composables/base/usePageList'
import { useModulePermission } from '@/composables/base/useModulePermission'
import { getRolePage, deleteRole } from '@/api/system/role'
import type { RoleVO } from '@/types/system'

const { allowed, hasPerm } = useModulePermission('system:role:list')
const canCreate = computed(() => hasPerm('system:role:create'))
const canUpdate = computed(() => hasPerm('system:role:update'))
const canDelete = computed(() => hasPerm('system:role:delete'))
const keyword = ref('')
const total = ref(0)

/** 与 PC 端角色列表的数据范围文案保持一致 */
const DATA_SCOPE_LABELS: Record<number, string> = {
  1: '全部数据',
  2: '自定义部门',
  3: '本部门',
  4: '本部门及以下',
  5: '仅本人',
}

function dataScopeLabel(scope?: number) {
  return DATA_SCOPE_LABELS[scope ?? 1] || '全部数据'
}

const { list, loading, finished, empty, refresh, loadMore, refreshing } = usePageList<RoleVO>(
  async (pageNo, pageSize) => {
    const res = await getRolePage({ pageNo, pageSize, name: keyword.value.trim() || undefined })
    total.value = res.data?.total || 0
    return { list: res.data?.list || [], total: total.value }
  },
)

async function onCardTap(item: RoleVO) {
  const actions: ActionSheetItem[] = []
  if (canUpdate.value) {
    actions.push({ label: '编辑' }, { label: '分配权限' })
  }
  if (canDelete.value) {
    actions.push({ label: '删除', danger: true })
  }
  if (!actions.length) return
  try {
    const tapIndex = await showActionSheet({
      title: item.name ? `角色: ${item.name}` : '角色操作',
      items: actions,
    })
    const action = actions[tapIndex]?.label
    if (action === '编辑') goEdit(item.id)
    else if (action === '分配权限') goMenuAssign(item)
    else if (action === '删除') confirmDelete(item)
  } catch {}
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

async function confirmDelete(item: RoleVO) {
  const { confirmed } = await showConfirm({
    title: '删除角色',
    content: `确定删除「${item.name}」？`,
    confirmText: '删除',
    tone: 'danger',
  })
  if (!confirmed) return
  await deleteRole(item.id)
  uni.showToast({ title: '已删除', icon: 'success' })
  await refresh()
}

function onSearch() { refresh() }
useListPageShowRefresh(refresh, { loading, refreshing })
onMounted(refresh)
onPullDownRefresh(async () => { await refresh(); uni.stopPullDownRefresh() })
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;
</style>
