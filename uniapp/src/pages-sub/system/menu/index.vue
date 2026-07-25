<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="menu-page page-padded">
    <ModuleDarkHero title="菜单管理" subtitle="目录、菜单与按钮权限" icon="apps-o" theme="menu">
      <template #aside>
        <view class="module-dark-hero__stats-row">
          <view class="module-dark-hero__mini-stat">
            <text class="module-dark-hero__mini-stat-num">{{ menuRows.length }}</text>
            <text class="module-dark-hero__mini-stat-label">全部</text>
          </view>
          <view class="module-dark-hero__mini-stat">
            <text class="module-dark-hero__mini-stat-num">{{ typeCount(1) }}</text>
            <text class="module-dark-hero__mini-stat-label">目录</text>
          </view>
          <view class="module-dark-hero__mini-stat">
            <text class="module-dark-hero__mini-stat-num">{{ typeCount(2) }}</text>
            <text class="module-dark-hero__mini-stat-label">菜单</text>
          </view>
        </view>
      </template>
    </ModuleDarkHero>

    <SegmentTabs v-model="statusMode" :tabs="statusTabs" compact />
    <SearchBar v-model="keyword" placeholder="搜索菜单名称、路由、权限" @search="refresh" />

    <ListLoading v-if="loading && !menuRows.length" />

    <scroll-view
      v-else
      scroll-y
      class="menu-page__scroll"
    >
      <view v-if="filteredRows.length" class="menu-tree card--elevated">
        <view
          v-for="(row, index) in filteredRows"
          :key="row.id"
          class="menu-node"
          :class="{
            'menu-node--root': row.level === 0,
            'menu-node--last': isLastAtLevel(index),
            'menu-node--disabled': row.status !== 1,
          }"
          @tap="onCardTap(row)"
        >
          <view class="menu-node__indent" :style="{ width: `${row.level * 36}rpx` }" />
          <view v-if="row.level > 0" class="menu-node__branch" />
          <view class="menu-node__icon" :class="typeIconClass(row.type)">
            <IconFont :name="typeIcon(row.type)" :size="28" />
          </view>
          <view class="menu-node__body">
            <view class="menu-node__head">
              <text class="menu-node__name">{{ row.label }}</text>
              <text class="menu-node__type">{{ typeLabel(row.type) }}</text>
            </view>
            <text v-if="row.path" class="menu-node__meta">路由 {{ row.path }}</text>
            <text v-if="row.permission" class="menu-node__meta menu-node__meta--perm">
              权限 {{ row.permission }}
            </text>
          </view>
          <view class="menu-node__status" :class="row.status === 1 ? 'menu-node__status--on' : 'menu-node__status--off'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </view>
          <text v-if="hasRowActions" class="menu-node__manage">管理</text>
        </view>
      </view>

      <EmptyState v-if="!loading && !filteredRows.length" title="暂无菜单" icon="apps-o" />
    </scroll-view>

    <FabButton v-if="canCreate" @click="goCreate" />

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { useListPageShowRefresh } from '@/composables/useListPageShowRefresh'
import { useAppDialogBackPress } from '@/composables/useAppDialogBackPress'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import IconFont from '@/components/common/IconFont/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import FabButton from '@/components/common/FabButton/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { useModulePermission } from '@/composables/useModulePermission'
import { deleteMenu, getMenuList } from '@/api/system/menu'
import { showConfirm, showActionSheet } from '@/utils/app-dialog'
import type { IconName } from '@/constants/iconfont'
import type { MenuVO } from '@/types/system'

interface MenuRow {
  id: number
  label: string
  level: number
  type?: number
  path?: string
  permission?: string
  status?: number
  raw: MenuVO
}

const { allowed, hasPerm } = useModulePermission('system:menu:list')
const canCreate = computed(() => hasPerm('system:menu:create'))
const canUpdate = computed(() => hasPerm('system:menu:update'))
const canDelete = computed(() => hasPerm('system:menu:delete'))
const hasRowActions = computed(() => canUpdate.value || canCreate.value || canDelete.value)

useAppDialogBackPress()

const keyword = ref('')
const loading = ref(false)
const menuRows = ref<MenuRow[]>([])
const statusMode = ref<'all' | '1' | '0'>('all')
const statusTabs = [
  { key: 'all', label: '全部' },
  { key: '1', label: '启用' },
  { key: '0', label: '停用' },
]

const filteredRows = computed(() => {
  const mode = statusMode.value
  const base =
    mode === 'all'
      ? menuRows.value
      : menuRows.value.filter((r) => (r.status ?? 1) === Number(mode))
  const q = keyword.value.trim().toLowerCase()
  if (!q) return base
  return base.filter(
    (r) =>
      r.label.toLowerCase().includes(q) ||
      (r.path && r.path.toLowerCase().includes(q)) ||
      (r.permission && r.permission.toLowerCase().includes(q)),
  )
})

function typeCount(type: number) {
  return menuRows.value.filter((r) => r.type === type).length
}

function flattenMenus(nodes: MenuVO[], level = 0): MenuRow[] {
  const rows: MenuRow[] = []
  for (const node of nodes) {
    rows.push({
      id: node.id,
      label: node.name,
      level,
      type: node.type,
      path: node.path,
      permission: node.permission,
      status: node.status,
      raw: node,
    })
    if (node.children?.length) {
      rows.push(...flattenMenus(node.children, level + 1))
    }
  }
  return rows
}

function typeLabel(type?: number) {
  if (type === 2) return '菜单'
  if (type === 3) return '按钮'
  return '目录'
}

function typeIcon(type?: number): IconName {
  if (type === 2) return 'balance-list-o'
  if (type === 3) return 'coupon-o'
  return 'apps-o'
}

function typeIconClass(type?: number) {
  if (type === 2) return 'menu-node__icon--menu'
  if (type === 3) return 'menu-node__icon--btn'
  return 'menu-node__icon--dir'
}

function isLastAtLevel(index: number) {
  const current = filteredRows.value[index]
  const next = filteredRows.value[index + 1]
  return !next || next.level <= current.level
}

async function refresh() {
  loading.value = true
  try {
    const res = await getMenuList()
    menuRows.value = flattenMenus(res.data || [])
  } finally {
    loading.value = false
  }
}

async function onCardTap(row: MenuRow) {
  if (!hasRowActions.value) {
    uni.showToast({ title: '暂无操作权限', icon: 'none' })
    return
  }
  const actions: string[] = []
  if (canUpdate.value) actions.push('编辑')
  if (canCreate.value && row.type !== 3) actions.push('新增子项')
  if (canDelete.value) actions.push('删除')
  if (!actions.length) return

  try {
    const index = await showActionSheet({
      title: row.label,
      items: actions.map((label) => ({
        label,
        danger: label === '删除',
      })),
    })
    const action = actions[index]
    await nextTick()
    if (action === '编辑') goEdit(row.id)
    else if (action === '新增子项') goCreateChild(row)
    else if (action === '删除') await confirmDelete(row)
  } catch {
    /* cancelled */
  }
}

function goCreate() {
  uni.navigateTo({ url: '/pages-sub/system/menu/edit?mode=create' })
}

function goCreateChild(row: MenuRow) {
  let childType = 2
  if (row.type === 1) childType = 2
  else if (row.type === 2) childType = 3
  uni.navigateTo({
    url: `/pages-sub/system/menu/edit?mode=create&parentId=${row.id}&type=${childType}`,
  })
}

function goEdit(id: number) {
  uni.navigateTo({ url: `/pages-sub/system/menu/edit?id=${id}` })
}

async function confirmDelete(row: MenuRow) {
  const { confirmed } = await showConfirm({
    title: '删除菜单',
    content: `确定删除「${row.label}」？`,
    confirmText: '删除',
    tone: 'danger',
  })
  if (!confirmed) return
  await deleteMenu(row.id)
  uni.showToast({ title: '已删除', icon: 'success' })
  await refresh()
}

useListPageShowRefresh(refresh, { loading })
onMounted(refresh)
onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.menu-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  /* #ifdef H5 */
  height: calc(100vh - var(--window-top, 0px));
  /* #endif */
  box-sizing: border-box;
  overflow: hidden;
  background: $color-bg-page;
}

.menu-page__scroll {
  flex: 1;
  min-height: 0;
  margin-top: 16rpx;

  :deep(.uni-scroll-view-content) {
    padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
  }
}

.menu-tree {
  overflow: hidden;
  padding: 8rpx 0;
}

.menu-node {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 116rpx;
  padding-right: 20rpx;

  &:active {
    background: rgba(79, 70, 229, 0.04);
  }

  &:not(.menu-node--last)::after {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 1px;
    background: $color-border-light;
    margin-left: 88rpx;
  }
}

.menu-node--root {
  background: linear-gradient(90deg, rgba(79, 70, 229, 0.06), transparent 72%);
}

.menu-node--disabled {
  opacity: 0.72;
}

.menu-node__indent {
  flex-shrink: 0;
}

.menu-node__branch {
  position: relative;
  width: 20rpx;
  height: 2rpx;
  margin-right: 8rpx;
  background: rgba(79, 70, 229, 0.28);
  flex-shrink: 0;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: -28rpx;
    width: 2rpx;
    height: 56rpx;
    background: rgba(79, 70, 229, 0.16);
  }
}

.menu-node__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56rpx;
  height: 56rpx;
  margin-right: 16rpx;
  border-radius: $radius-md;
  flex-shrink: 0;

  &--dir {
    color: $color-primary;
    background: $color-primary-muted;
  }

  &--menu {
    color: #0ea5e9;
    background: rgba(14, 165, 233, 0.12);
  }

  &--btn {
    color: #f59e0b;
    background: rgba(245, 158, 11, 0.12);
  }
}

.menu-node__body {
  flex: 1;
  min-width: 0;
  padding: 18rpx 0;
}

.menu-node__head {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.menu-node__name {
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-node__type {
  flex-shrink: 0;
  padding: 2rpx 12rpx;
  border-radius: $radius-full;
  font-size: 20rpx;
  color: $color-primary;
  background: $color-primary-muted;
}

.menu-node__meta {
  display: block;
  margin-top: 6rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &--perm {
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    font-size: 20rpx;
  }
}

.menu-node__status {
  flex-shrink: 0;
  padding: 6rpx 14rpx;
  border-radius: $radius-full;
  font-size: 20rpx;
  font-weight: $font-weight-semibold;

  &--on {
    color: $color-success;
    background: rgba(103, 194, 58, 0.12);
  }

  &--off {
    color: $color-danger;
    background: rgba(245, 108, 108, 0.12);
  }
}

.menu-node__manage {
  flex-shrink: 0;
  margin-left: 12rpx;
  padding: 8rpx 18rpx;
  border-radius: $radius-full;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  color: $color-primary;
  background: $color-primary-muted;
}
</style>
