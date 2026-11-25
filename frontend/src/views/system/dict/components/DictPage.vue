<template>
  <div class="app-container module-page dict-page">
    <PageHeader
      title="字典管理"
      description="左侧选择字典类型，右侧维护选项；业务表单通过 DictSelect / DictTag 引用类型编码"
      :icon="MODULE_PAGE_ICON.dict"
      :stats="[{ label: '字典类型', value: total }]"
    />

    <DictSearchCard :query-params="queryParams" @query="handleQuery" @reset="resetQuery" />

    <div class="dict-layout">
      <DictTypePanel
        v-model:type-filter="typeFilter"
        :loading="loading"
        :filtered-types="filteredTypes"
        :selected-type="selectedType"
        :query-params="queryParams"
        :total="total"
        @add-type="handleAddType"
        @select-type="selectType"
        @load-types="loadTypes"
      />

      <DictDataPanel
        v-model:data-filter="dataFilter"
        :selected-type="selectedType"
        :filtered-data-list="filteredDataList"
        :data-loading="dataLoading"
        @refresh-cache="handleRefreshCache"
        @copy-type="handleCopyType"
        @edit-type="handleEditType"
        @delete-type="handleDeleteType"
        @add-data="handleAddData"
        @edit-data="handleEditData"
        @delete-data="handleDeleteData"
      />
    </div>

    <DictTypeFormDialog
      ref="typeDialogRef"
      v-model:visible="typeDialogVisible"
      :title="typeDialogTitle"
      :form="typeForm"
      :rules="typeRules"
      :submitting="typeSubmitting"
      @submit="submitType"
    />

    <DictDataFormDialog
      ref="dataDialogRef"
      v-model:visible="dataFormVisible"
      :title="dataFormTitle"
      :form="dataForm"
      :rules="dataRules"
      :submitting="dataSubmitting"
      @submit="submitData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect, defineAsyncComponent } from 'vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import DictSearchCard from './DictSearchCard.vue'
import DictTypePanel from './DictTypePanel.vue'
import DictDataPanel from './DictDataPanel.vue'
import { useDictPage } from '../composables/useDictPage'

const DictTypeFormDialog = defineAsyncComponent(() => import('./DictTypeFormDialog.vue'))
const DictDataFormDialog = defineAsyncComponent(() => import('./DictDataFormDialog.vue'))

const typeDialogRef = ref<any>(null)
const dataDialogRef = ref<any>(null)

const {
  loading,
  total,
  typeFilter,
  selectedType,
  queryParams,
  filteredTypes,
  typeDialogVisible,
  typeDialogTitle,
  typeSubmitting,
  typeFormRef,
  typeForm,
  typeRules,
  dataLoading,
  dataFilter,
  filteredDataList,
  dataFormVisible,
  dataFormTitle,
  dataSubmitting,
  dataFormRef,
  dataForm,
  dataRules,
  loadTypes,
  selectType,
  handleQuery,
  resetQuery,
  handleAddType,
  handleEditType,
  submitType,
  handleDeleteType,
  handleCopyType,
  handleRefreshCache,
  handleAddData,
  handleEditData,
  submitData,
  handleDeleteData,
} = useDictPage()

watchEffect(() => {
  typeFormRef.value = typeDialogRef.value?.formRef
  dataFormRef.value = dataDialogRef.value?.formRef
})
</script>

<style scoped lang="scss">
.dict-layout {
  display: flex;
  gap: var(--space-4);
  min-height: 520px;
}

@media (max-width: 960px) {
  .dict-layout {
    flex-direction: column;
  }
}
</style>
