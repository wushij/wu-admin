<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded page-list">
    <ModuleHero title="字典管理" :count="total || list.length" subtitle="类型与字典项维护" />
    <SearchBar v-model="keyword" placeholder="搜索字典名称 / 类型" @search="onSearch" />

    <view v-if="canUpdate" class="dict-toolbar">
      <button class="dict-toolbar__btn" size="mini" @click="refreshCache">刷新缓存</button>
    </view>

    <ListLoading v-if="loading && !list.length" />

    <scroll-view
      v-else
      scroll-y
      class="page-list__scroll"
      :class="{ 'page-list__scroll--filter': canUpdate }"
      @scrolltolower="loadMore"
    >
      <ListCard v-for="item in list" :key="item.id" @click="onTypeTap(item)">
        <view class="list-card__top">
          <text class="list-card__title">{{ item.dictName }}</text>
          <DictTag
            :label="item.status === 1 ? '启用' : '停用'"
            :effect="item.status === 1 ? 'success' : 'danger'"
          />
        </view>
        <text class="list-card__sub">{{ item.dictType }} · {{ item.dataCount ?? 0 }} 项</text>
        <view v-if="expandedId === item.id && dictData.length" class="dict-data">
          <view
            v-for="d in dictData"
            :key="d.id"
            class="dict-data__row"
            @click.stop="onDataTap(item.dictType, d)"
          >
            <text class="dict-data__label">{{ d.dictLabel }}</text>
            <text class="dict-data__value">{{ d.dictValue }}</text>
          </view>
          <button v-if="canCreate" class="dict-data__add" size="mini" @click.stop="addData(item.dictType)">+ 新增字典项</button>
        </view>
      </ListCard>
      <EmptyState v-if="empty" title="暂无字典" icon="notes-o" />
      <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
    </scroll-view>

    <FabButton v-if="canCreate" @click="goCreateType" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleHero from '@/components/common/ModuleHero/index.vue'
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
import {
  pageDictType,
  listDictDataForManage,
  deleteDictType,
  deleteDictData,
  copyDictType,
  refreshDictCache,
} from '@/api/system/dict'
import type { DictDataItem } from '@/types/api'
import type { DictTypeVO } from '@/types/system'

const { allowed, hasPerm } = useModulePermission('system:dict:list')
const canCreate = computed(() => hasPerm('system:dict:create'))
const canUpdate = computed(() => hasPerm('system:dict:update'))
const canDelete = computed(() => hasPerm('system:dict:delete'))
const canCopy = computed(() => hasPerm('system:dict:copy'))

const keyword = ref('')
const total = ref(0)
const expandedId = ref<number | null>(null)
const dictData = ref<DictDataItem[]>([])
const expandedType = ref('')

const { list, loading, finished, empty, refresh, loadMore } = usePageList<DictTypeVO>(
  async (pageNo, pageSize) => {
    const q = keyword.value.trim()
    const res = await pageDictType({ pageNo, pageSize, dictName: q || undefined, dictType: q || undefined })
    total.value = res.data?.total || 0
    return { list: res.data?.list || [], total: total.value }
  },
)

async function toggleExpand(item: DictTypeVO) {
  if (expandedId.value === item.id) {
    expandedId.value = null
    dictData.value = []
    return
  }
  expandedId.value = item.id
  expandedType.value = item.dictType
  const res = await listDictDataForManage(item.dictType)
  dictData.value = res.data || []
}

function onTypeTap(item: DictTypeVO) {
  const actions = ['展开字典项']
  if (canUpdate.value) actions.push('编辑类型')
  if (canCopy.value) actions.push('复制类型')
  if (canDelete.value) actions.push('删除类型')
  uni.showActionSheet({
    itemList: actions,
    success: async (res) => {
      const action = actions[res.tapIndex]
      if (action === '展开字典项') await toggleExpand(item)
      else if (action === '编辑类型') editType(item.id)
      else if (action === '复制类型') {
        await copyDictType(item.id)
        uni.showToast({ title: '已复制', icon: 'success' })
        await refresh()
      } else if (action === '删除类型') {
        uni.showModal({
          title: '删除字典类型',
          content: `确定删除「${item.dictName}」？`,
          confirmColor: '#f56c6c',
          success: async (r) => {
            if (!r.confirm) return
            await deleteDictType(item.id)
            uni.showToast({ title: '已删除', icon: 'success' })
            await refresh()
          },
        })
      }
    },
  })
}

function onDataTap(dictType: string, d: DictDataItem) {
  if (!canUpdate.value && !canDelete.value) return
  const actions: string[] = []
  if (canUpdate.value) actions.push('编辑')
  if (canDelete.value) actions.push('删除')
  uni.showActionSheet({
    itemList: actions,
    success: async (res) => {
      if (actions[res.tapIndex] === '编辑') {
        uni.navigateTo({
          url: `/pages-sub/system/dict/data-form?dictType=${dictType}&dataId=${d.id}`,
        })
      } else if (actions[res.tapIndex] === '删除' && d.id) {
        await deleteDictData(d.id)
        uni.showToast({ title: '已删除', icon: 'success' })
        const type = list.value.find((t) => t.dictType === dictType)
        if (type) await toggleExpand(type)
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

function addData(dictType: string) {
  uni.navigateTo({ url: `/pages-sub/system/dict/data-form?mode=create&dictType=${dictType}` })
}

async function refreshCache() {
  await refreshDictCache()
  uni.showToast({ title: '缓存已刷新', icon: 'success' })
}

function onSearch() { refresh() }
onMounted(refresh)
onPullDownRefresh(async () => { await refresh(); uni.stopPullDownRefresh() })
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.dict-toolbar {
  margin-bottom: 16rpx;
  text-align: right;
}

.dict-toolbar__btn {
  background: $color-primary-muted;
  color: $color-primary;
}

.dict-data {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1px solid $color-border-light;
}

.dict-data__row {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  padding: 12rpx 0;
  font-size: $font-size-sm;
}

.dict-data__label { color: $color-text-regular; }
.dict-data__value { color: $color-text-secondary; }

.dict-data__add {
  margin-top: 12rpx;
  background: $color-primary-muted;
  color: $color-primary;
}
</style>
