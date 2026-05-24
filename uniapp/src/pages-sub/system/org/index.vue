<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="org-page page-padded">
    <ModuleDarkHero title="组织管理" subtitle="部门与岗位树 · 支持新增编辑" icon="cluster-o" theme="dept">
      <template #aside>
        <view class="module-dark-hero__stat">
          <text class="module-dark-hero__stat-num">{{ rows.length }}</text>
          <text class="module-dark-hero__stat-label">{{ mode === 'dept' ? '部门节点' : '岗位节点' }}</text>
        </view>
      </template>
    </ModuleDarkHero>

    <SegmentTabs v-model="mode" :tabs="tabs" compact />
    <SegmentTabs v-model="statusMode" :tabs="statusTabs" compact />

    <scroll-view scroll-y class="org-page__scroll">
      <view v-if="rows.length" class="org-tree card--elevated">
        <view
          v-for="(node, index) in rows"
          :key="`${node.entity}-${node.id}`"
          class="org-node"
          :class="{
            'org-node--root': node.level === 0,
            'org-node--last': isLastAtLevel(index),
            'org-node--disabled': node.status === 0,
          }"
          @tap="onRowTap(node)"
        >
          <view class="org-node__indent" :style="{ width: `${node.level * 36}rpx` }" />
          <view v-if="node.level > 0" class="org-node__branch" />
          <view
            class="org-node__icon"
            :class="mode === 'dept' ? 'org-node__icon--dept' : 'org-node__icon--post'"
          >
            <IconFont :name="mode === 'dept' ? 'cluster-o' : 'manager-o'" :size="28" />
          </view>
          <view class="org-node__body">
            <text class="org-node__name">{{ node.label }}</text>
            <text v-if="node.extra" class="org-node__meta">{{ node.extra }}</text>
          </view>
          <text v-if="hasRowActions" class="org-node__manage">管理</text>
        </view>
      </view>

      <EmptyState v-if="!loading && !rows.length" title="暂无数据" icon="cluster-o" />
    </scroll-view>

    <FabButton v-if="canCreate" @click="goCreate" />

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { useAppDialogBackPress } from '@/composables/useAppDialogBackPress'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import IconFont from '@/components/common/IconFont/index.vue'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import FabButton from '@/components/common/FabButton/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { useModulePermission } from '@/composables/useModulePermission'
import { deleteDept, deletePost, getDeptTree, getPostTree } from '@/api/system/dept'
import { showConfirm, showActionSheet } from '@/utils/app-dialog'
import { flattenDeptTree, flattenPostTree, type FlatTreeNode } from '@/utils/org-tree'

const tabs = [
  { key: 'dept', label: '部门' },
  { key: 'post', label: '岗位' },
]

const { allowed, hasPerm } = useModulePermission('system:dept:list')

const mode = ref<'dept' | 'post'>('dept')
const statusMode = ref<'all' | '1' | '0'>('all')
const statusTabs = [
  { key: 'all', label: '全部' },
  { key: '1', label: '启用' },
  { key: '0', label: '停用' },
]
const deptRows = ref<FlatTreeNode[]>([])
const postRows = ref<FlatTreeNode[]>([])
const loading = ref(false)

const rows = computed(() => {
  const base = mode.value === 'dept' ? deptRows.value : postRows.value
  const s = statusMode.value
  if (s === 'all') return base
  return base.filter((n) => (n.status ?? 1) === Number(s))
})

const canCreate = computed(() =>
  mode.value === 'dept' ? hasPerm('system:dept:create') : hasPerm('system:post:create'),
)
const canUpdate = computed(() =>
  mode.value === 'dept' ? hasPerm('system:dept:update') : hasPerm('system:post:update'),
)
const canDelete = computed(() =>
  mode.value === 'dept' ? hasPerm('system:dept:delete') : hasPerm('system:post:delete'),
)
const hasRowActions = computed(() => canUpdate.value || canCreate.value || canDelete.value)

useAppDialogBackPress()

function isLastAtLevel(index: number) {
  const current = rows.value[index]
  const next = rows.value[index + 1]
  return !next || next.level <= current.level
}

async function refresh() {
  loading.value = true
  try {
    const [deptRes, postRes] = await Promise.all([getDeptTree(), getPostTree()])
    deptRows.value = flattenDeptTree(deptRes.data || [])
    postRows.value = flattenPostTree(postRes.data || [])
  } finally {
    loading.value = false
  }
}

async function onRowTap(node: FlatTreeNode) {
  if (!hasRowActions.value) {
    uni.showToast({ title: '暂无操作权限', icon: 'none' })
    return
  }
  const actions: string[] = []
  if (canUpdate.value) actions.push('编辑')
  if (canCreate.value) actions.push('新增子项')
  if (canDelete.value) actions.push('删除')
  try {
    const index = await showActionSheet({
      title: node.label,
      items: actions.map((label) => ({ label, danger: label === '删除' })),
    })
    const action = actions[index]
    await nextTick()
    if (action === '编辑') goEdit(node)
    else if (action === '新增子项') goCreateChild(node)
    else if (action === '删除') await confirmDelete(node)
  } catch {
    /* cancelled */
  }
}

function goCreate() {
  uni.navigateTo({ url: `/pages-sub/system/org/edit?entity=${mode.value}&mode=create` })
}

function goCreateChild(node: FlatTreeNode) {
  uni.navigateTo({
    url: `/pages-sub/system/org/edit?entity=${mode.value}&mode=create&parentId=${node.id}`,
  })
}

function goEdit(node: FlatTreeNode) {
  uni.navigateTo({ url: `/pages-sub/system/org/edit?entity=${mode.value}&id=${node.id}` })
}

async function confirmDelete(node: FlatTreeNode) {
  const { confirmed } = await showConfirm({
    title: '删除确认',
    content: `确定删除「${node.label}」？`,
    confirmText: '删除',
    tone: 'danger',
  })
  if (!confirmed) return
  if (mode.value === 'dept') await deleteDept(node.id)
  else await deletePost(node.id)
  uni.showToast({ title: '已删除', icon: 'success' })
  await refresh()
}

onMounted(refresh)

const skipNextShowRefresh = ref(true)
onShow(async () => {
  if (skipNextShowRefresh.value) {
    skipNextShowRefresh.value = false
    return
  }
  await refresh()
})

onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.org-page {
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

.org-page__scroll {
  flex: 1;
  min-height: 0;

  :deep(.uni-scroll-view-content) {
    padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
  }
}

.org-tree {
  overflow: hidden;
  padding: 8rpx 0;
}

.org-node {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 108rpx;
  padding-right: 20rpx;

  &:active {
    background: rgba(79, 70, 229, 0.04);
  }

  &:not(.org-node--last)::after {
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

.org-node--root {
  background: linear-gradient(90deg, rgba(79, 70, 229, 0.06), transparent 72%);
}

.org-node--disabled {
  opacity: 0.72;
}

.org-node__indent {
  flex-shrink: 0;
}

.org-node__branch {
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
    top: -24rpx;
    width: 2rpx;
    height: 48rpx;
    background: rgba(79, 70, 229, 0.16);
  }
}

.org-node__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56rpx;
  height: 56rpx;
  margin-right: 16rpx;
  border-radius: $radius-md;
  flex-shrink: 0;

  &--dept {
    color: $color-primary;
    background: $color-primary-muted;
  }

  &--post {
    color: #0ea5e9;
    background: rgba(14, 165, 233, 0.12);
  }
}

.org-node__body {
  flex: 1;
  min-width: 0;
  padding: 18rpx 0;
}

.org-node__name {
  display: block;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.org-node__meta {
  display: block;
  margin-top: 6rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.org-node__manage {
  flex-shrink: 0;
  padding: 8rpx 18rpx;
  border-radius: $radius-full;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  color: $color-primary;
  background: $color-primary-muted;
}
</style>
