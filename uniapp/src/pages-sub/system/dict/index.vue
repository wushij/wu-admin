<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="dict-page">
    <ModuleDarkHero
      title="字典管理"
      :subtitle="`类型与字典项维护 · 共 ${total} 类`"
      icon="notes-o"
      theme="dict"
    >
      <template #extra>
        <view v-if="canUpdate" class="module-dark-hero__chip" @click.stop="refreshCache">
          <IconFont name="chart-trending-o" :size="24" color="rgba(255,255,255,0.9)" />
          <text>刷新缓存</text>
        </view>
        <view class="module-dark-hero__chip" @click.stop="goRecycle">
          <IconFont name="balance-list-o" :size="24" color="rgba(255,255,255,0.9)" />
          <text>回收中心</text>
        </view>
      </template>
    </ModuleDarkHero>

    <SegmentTabs v-model="statusMode" :tabs="statusTabs" />
    <SearchBar v-model="keyword" placeholder="搜索字典名称 / 类型" @search="onSearch" />

    <ListLoading v-if="loading && !list.length" />

    <scroll-view
      v-else
      scroll-y
      class="dict-page__scroll"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="loadMore"
    >
      <view
        v-for="item in list"
        :key="item.id"
        class="dict-card card--elevated"
        @click="goDataList(item)"
        @longpress="onTypeMenu(item)"
      >
        <ModuleIcon icon="records-o" theme="dict" size="sm" />
        <view class="dict-card__main">
          <view class="dict-card__head">
            <text class="dict-card__title">{{ item.dictName }}</text>
            <DictTag
              :label="item.status === 1 ? '启用' : '停用'"
              :effect="item.status === 1 ? 'success' : 'danger'"
            />
          </view>
          <text class="dict-card__code">{{ item.dictType }}</text>
          <view class="dict-card__meta">
            <text class="dict-card__count">{{ item.dataCount ?? 0 }} 项字典数据</text>
            <text v-if="item.remark" class="dict-card__remark">{{ item.remark }}</text>
          </view>
        </view>
        <view class="dict-card__more" @click.stop="onTypeMenu(item)">
          <IconFont name="apps-o" :size="32" color="#94a3b8" />
        </view>
      </view>

      <EmptyState v-if="empty" title="暂无字典" icon="notes-o" />
      <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
    </scroll-view>

    <FabButton v-if="canCreate" @click="goCreateType" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import IconFont from '@/components/common/IconFont/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import FabButton from '@/components/common/FabButton/index.vue'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import { usePageList } from '@/composables/usePageList'
import { useModulePermission } from '@/composables/useModulePermission'
import { reloadDictTypes } from '@/composables/useDict'
import { clearDictCache as clearStorageDictCache } from '@/utils/cache'
import {
  pageDictType,
  deleteDictType,
  copyDictType,
  refreshDictCache,
} from '@/api/system/dict'
import type { DictTypeVO } from '@/types/system'

const { allowed, hasPerm } = useModulePermission('system:dict:list')
const canCreate = computed(() => hasPerm('system:dict:create'))
const canUpdate = computed(() => hasPerm('system:dict:update'))
const canDelete = computed(() => hasPerm('system:dict:delete'))
const canCopy = computed(() => hasPerm('system:dict:copy'))

const keyword = ref('')
const total = ref(0)
const statusMode = ref('all')
const statusTabs = [
  { key: 'all', label: '全部' },
  { key: '1', label: '启用' },
  { key: '0', label: '停用' },
]

const { list, loading, refreshing, finished, empty, refresh, loadMore } = usePageList<DictTypeVO>(
  async (pageNo, pageSize) => {
    const q = keyword.value.trim()
    const res = await pageDictType({
      pageNo,
      pageSize,
      dictName: q || undefined,
      dictType: q || undefined,
      status: statusMode.value === 'all' ? undefined : Number(statusMode.value),
    })
    total.value = res.data?.total || 0
    return { list: res.data?.list || [], total: total.value }
  },
)

function goDataList(item: DictTypeVO) {
  uni.navigateTo({
    url: `/pages-sub/system/dict/data-list?typeId=${item.id}&dictType=${encodeURIComponent(item.dictType)}&dictName=${encodeURIComponent(item.dictName)}`,
  })
}

function onTypeMenu(item: DictTypeVO) {
  const actions: string[] = ['查看字典项']
  if (canUpdate.value) actions.push('编辑类型')
  if (canCopy.value) actions.push('复制类型')
  if (canDelete.value) actions.push('删除类型')
  uni.showActionSheet({
    itemList: actions,
    success: async (res) => {
      const action = actions[res.tapIndex]
      if (action === '查看字典项') goDataList(item)
      else if (action === '编辑类型') editType(item.id)
      else if (action === '复制类型') {
        await copyDictType(item.id)
        uni.showToast({ title: '已复制', icon: 'success' })
        await refresh({ silent: true })
      } else if (action === '删除类型') {
        const n = item.dataCount ?? 0
        uni.showModal({
          title: '删除字典类型',
          content: `确定删除「${item.dictName}」？将同时删除其下 ${n} 条字典数据。`,
          confirmColor: '#f56c6c',
          success: async (r) => {
            if (!r.confirm) return
            await deleteDictType(item.id)
            uni.showToast({ title: '已删除', icon: 'success' })
            await refresh({ silent: true })
          },
        })
      }
    },
  })
}

function goCreateType() {
  uni.navigateTo({ url: '/pages-sub/system/dict/type-form?mode=create' })
}

function editType(id: number) {
  uni.navigateTo({ url: `/pages-sub/system/dict/type-form?id=${id}` })
}

function goRecycle() {
  uni.navigateTo({ url: '/pages-sub/system/recycle/index?type=dict' })
}

async function refreshCache() {
  await refreshDictCache()
  clearStorageDictCache()
  await reloadDictTypes(list.value.map((t) => t.dictType))
  uni.showToast({ title: '缓存已刷新', icon: 'success' })
}

function onSearch() {
  refresh({ silent: true })
}

async function onRefresh() {
  await refresh()
}

watch(statusMode, () => refresh({ silent: true }))

const skipNextShowRefresh = ref(true)
onMounted(() => refresh())
onShow(async () => {
  if (skipNextShowRefresh.value) {
    skipNextShowRefresh.value = false
    return
  }
  if (loading.value || refreshing.value) return
  await refresh({ silent: true })
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
@use '@/styles/mine.scss' as *;

.dict-page {
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

.dict-page__scroll {
  flex: 1;
  min-height: 0;
  margin-top: 8rpx;

  :deep(.uni-scroll-view-content) {
    padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
  }
}

.dict-card {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  padding: 28rpx 24rpx;
  margin-bottom: $card-gap;
}

.dict-card__main {
  flex: 1;
  min-width: 0;
}

.dict-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.dict-card__title {
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.dict-card__code {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-sm;
  color: $color-primary;
  font-family: monospace;
}

.dict-card__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12rpx;
  margin-top: 10rpx;
}

.dict-card__count {
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.dict-card__remark {
  font-size: $font-size-xs;
  color: $color-text-placeholder;
}

.dict-card__more {
  flex-shrink: 0;
  padding: 8rpx;
  margin: -8rpx -8rpx 0 0;
}
</style>
