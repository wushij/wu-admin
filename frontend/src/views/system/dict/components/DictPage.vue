<template>
  <div class="app-container module-page dict-page">
    <el-card class="search-card module-hero-card" shadow="never">
      <div class="module-hero-row">
        <div class="module-hero-text">
          <div class="module-hero-title">
            <ModulePageIcon :icon="MODULE_PAGE_ICON.dict" />
            <span>字典管理</span>
          </div>
          <p class="module-hero-desc">左侧选择字典类型，右侧维护选项；业务表单通过 DictSelect / DictTag 引用类型编码</p>
        </div>
        <div class="module-hero-stats">
          <div class="stat-num">{{ total }}</div>
          <div class="stat-label">字典类型</div>
        </div>
      </div>
    </el-card>

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
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
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
