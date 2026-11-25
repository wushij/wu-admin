<template>
  <div class="app-container module-page recycle-center-page">
    <PageHeader
      title="回收中心"
      :description="`${visibleTypes.length} 类业务数据 · 支持一键恢复或物理彻底清除`"
      :icon="MODULE_PAGE_ICON.recycle"
      :stats="[
        { label: '待处理', value: visiblePendingTotal },
        { label: '当前分类', value: activePendingCount },
      ]"
    />

    <RecycleTypeGrid
      :visible-types="visibleTypes"
      :active-type="activeType"
      :summary="summary"
      @select="switchType"
    />

    <RecycleTablePanel
      v-model:search-form="searchForm"
      v-model:query="query"
      :current-type="currentType"
      :table-data="tableData"
      :loading="loading"
      :refreshing="refreshing"
      :total="total"
      :can-restore="canRestore"
      :can-delete-permanent="canDeletePermanent"
      @refresh="handleRefresh"
      @search="handleSearch"
      @reset="resetSearch"
      @restore="handleRestore"
      @delete-permanent="handlePermanentDelete"
      @page-change="loadList"
    />
  </div>
</template>

<script setup lang="ts">
import PageHeader from '@/components/ui/PageHeader.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import RecycleTypeGrid from './components/RecycleTypeGrid.vue'
import RecycleTablePanel from './components/RecycleTablePanel.vue'
import { useRecyclePage } from './composables/useRecyclePage'

const {
  summary,
  activeType,
  loading,
  refreshing,
  tableData,
  total,
  searchForm,
  query,
  visibleTypes,
  currentType,
  activePendingCount,
  visiblePendingTotal,
  canRestore,
  canDeletePermanent,
  switchType,
  handleRefresh,
  loadList,
  handleSearch,
  resetSearch,
  handleRestore,
  handlePermanentDelete,
} = useRecyclePage()
</script>
