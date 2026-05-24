<template>
  <view class="dict-data-page">
    <view class="dict-data-hero">
      <view class="dict-data-hero__pattern" />
      <view class="dict-data-hero__glow" />
      <view class="dict-data-hero__body">
        <ModuleIcon icon="records-o" theme="dict" size="lg" />
        <view class="dict-data-hero__text">
          <text class="dict-data-hero__title">{{ dictName || '字典项' }}</text>
          <text class="dict-data-hero__sub">{{ dictType }} · 共 {{ filteredList.length }} 项</text>
        </view>
      </view>
    </view>

    <SegmentTabs v-model="statusMode" :tabs="statusTabs" />
    <SearchBar v-model="keyword" placeholder="搜索标签 / 键值" @search="onSearch" />

    <ListLoading v-if="loading" />

    <scroll-view
      v-else
      scroll-y
      class="dict-data-page__scroll"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view
        v-for="item in filteredList"
        :key="item.id"
        class="data-card card--elevated"
        @click="onDataTap(item)"
        @longpress="onDataTap(item)"
      >
        <view class="data-card__sort">{{ item.sort ?? 0 }}</view>
        <view class="data-card__main">
          <view class="data-card__head">
            <DictTag :label="item.dictLabel" :effect="listClassToTagType(item.listClass)" />
            <DictTag v-if="item.isDefault === 1" label="默认" effect="primary" />
            <DictTag
              :label="item.status === 1 ? '启用' : '停用'"
              :effect="item.status === 1 ? 'success' : 'danger'"
            />
          </view>
          <text class="data-card__value">键值：{{ item.dictValue }}</text>
          <text v-if="item.remark" class="data-card__remark">{{ item.remark }}</text>
        </view>
        <IconFont name="arrow" :size="28" color="#cbd5e1" />
      </view>

      <EmptyState v-if="!filteredList.length" title="暂无字典项" icon="notes-o" />
    </scroll-view>

    <FabButton v-if="canCreate" @click="addData" />
    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import IconFont from '@/components/common/IconFont/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import FabButton from '@/components/common/FabButton/index.vue'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { showConfirm, showActionSheet, type ActionSheetItem } from '@/utils/app-dialog'
import { useModulePermission } from '@/composables/useModulePermission'
import { listClassToTagType } from '@/composables/useDict'
import { listDictDataForManage, deleteDictData } from '@/api/system/dict'
import type { DictDataItem } from '@/types/api'

const { hasPerm } = useModulePermission('system:dict:list')
const canCreate = computed(() => hasPerm('system:dict:create'))
const canUpdate = computed(() => hasPerm('system:dict:update'))
const canDelete = computed(() => hasPerm('system:dict:delete'))

const typeId = ref(0)
const dictType = ref('')
const dictName = ref('')
const keyword = ref('')
const statusMode = ref('all')
const loading = ref(false)
const refreshing = ref(false)
const dataList = ref<DictDataItem[]>([])

const statusTabs = [
  { key: 'all', label: '全部' },
  { key: '1', label: '启用' },
  { key: '0', label: '停用' },
]

const filteredList = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return dataList.value
    .filter((item) => {
      if (statusMode.value !== 'all' && item.status !== Number(statusMode.value)) return false
      if (!q) return true
      return (
        (item.dictLabel && item.dictLabel.toLowerCase().includes(q)) ||
        (item.dictValue && String(item.dictValue).toLowerCase().includes(q))
      )
    })
    .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
})

async function loadData(silent = false) {
  if (!dictType.value) return
  if (!silent) loading.value = true
  try {
    const res = await listDictDataForManage(dictType.value)
    dataList.value = res.data || []
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

async function onDataTap(item: DictDataItem) {
  if (!canUpdate.value && !canDelete.value) return
  const actions: ActionSheetItem[] = []
  if (canUpdate.value) actions.push({ label: '编辑' })
  if (canDelete.value) actions.push({ label: '删除', danger: true })

  try {
    const tapIndex = await showActionSheet({
      title: item.dictLabel ? `字典项: ${item.dictLabel}` : '字典项操作',
      items: actions,
    })
    const action = actions[tapIndex]?.label
    if (action === '编辑') {
      uni.navigateTo({
        url: `/pages-sub/system/dict/data-form?dictType=${encodeURIComponent(dictType.value)}&dataId=${item.id}`,
      })
    } else if (action === '删除' && item.id) {
      const { confirmed } = await showConfirm({
        title: '删除字典项',
        content: `确定删除「${item.dictLabel}」？`,
        confirmText: '删除',
        tone: 'danger',
      })
      if (!confirmed) return
      await deleteDictData(item.id!)
      uni.showToast({ title: '已删除', icon: 'success' })
      await loadData(true)
    }
  } catch {}
}

function addData() {
  uni.navigateTo({
    url: `/pages-sub/system/dict/data-form?mode=create&dictType=${encodeURIComponent(dictType.value)}`,
  })
}

function onSearch() {
  // client-side filter
}

async function onRefresh() {
  refreshing.value = true
  await loadData(true)
}

const skipNextShowRefresh = ref(true)
onLoad((options) => {
  typeId.value = Number(options?.typeId || 0)
  dictType.value = decodeURIComponent(String(options?.dictType || ''))
  dictName.value = decodeURIComponent(String(options?.dictName || ''))
  uni.setNavigationBarTitle({ title: dictName.value || '字典项' })
})
onMounted(() => loadData())
onShow(async () => {
  if (skipNextShowRefresh.value) {
    skipNextShowRefresh.value = false
    return
  }
  await loadData(true)
})
onPullDownRefresh(async () => {
  try {
    await onRefresh()
  } finally {
    uni.stopPullDownRefresh()
  }
})
</script>

<style lang="scss" scoped>
@use '@/styles/mine.scss' as *;

.dict-data-page {
  @include mine-page-bg;
  display: flex;
  flex-direction: column;
  height: 100vh;
  /* #ifdef H5 */
  height: calc(100vh - var(--window-top, 0px));
  /* #endif */
  box-sizing: border-box;
  overflow: hidden;
  padding: $page-padding-y $page-padding-x 0;
}

.dict-data-hero {
  @include mine-dark-hero-shell;
  flex-shrink: 0;
}

.dict-data-hero__pattern {
  @include mine-dark-hero-pattern;
}

.dict-data-hero__glow {
  @include mine-dark-hero-glow(rgba(79, 172, 254, 0.32));
}

.dict-data-hero__body {
  @include mine-dark-hero-body;
}

.dict-data-hero__text {
  @include mine-dark-hero-text;
}

.dict-data-hero__title {
  @include mine-dark-hero-title;
}

.dict-data-hero__sub {
  @include mine-dark-hero-sub;
  font-family: monospace;
}

.dict-data-page__scroll {
  flex: 1;
  min-height: 0;
  margin-top: 8rpx;

  :deep(.uni-scroll-view-content) {
    padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
  }
}

.data-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx;
  margin-bottom: $card-gap;
}

.data-card__sort {
  flex-shrink: 0;
  width: 52rpx;
  height: 52rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, #eef2ff, #e0e7ff);
  color: #4f46e5;
  font-size: $font-size-sm;
  font-weight: $font-weight-bold;
  display: flex;
  align-items: center;
  justify-content: center;
}

.data-card__main {
  flex: 1;
  min-width: 0;
}

.data-card__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10rpx;
}

.data-card__value {
  display: block;
  margin-top: 10rpx;
  font-size: $font-size-sm;
  color: $color-text-regular;
}

.data-card__remark {
  display: block;
  margin-top: 6rpx;
  font-size: $font-size-xs;
  color: $color-text-placeholder;
}
</style>
