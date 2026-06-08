<template>
  <div class="app-container dict-page">
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
import { ref, watchEffect } from 'vue'
import DictSearchCard from './DictSearchCard.vue'
import DictTypePanel from './DictTypePanel.vue'
import DictDataPanel from './DictDataPanel.vue'
import DictTypeFormDialog from './DictTypeFormDialog.vue'
import DictDataFormDialog from './DictDataFormDialog.vue'
import { useDictPage } from '../composables/useDictPage'

const typeDialogRef = ref<InstanceType<typeof DictTypeFormDialog> | null>(null)
const dataDialogRef = ref<InstanceType<typeof DictDataFormDialog> | null>(null)

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
  gap: 12px;
  min-height: 520px;
}
</style>
