<template>
  <div class="app-container module-page ai-model-page">
    <PageHeader
      title="AI 模型配置"
      description="管理 AI 对话模型与知识库向量模型，支持 DeepSeek / Kimi / GLM / 自定义网关"
      :icon="MODULE_PAGE_ICON.aiModel"
      :stats="[{ label: '模型总数', value: total }]"
    />

    <el-card class="search-card module-search-card" shadow="never">
      <el-form :model="queryParams" inline class="module-search-form">
        <el-form-item label="配置名称">
          <el-input
            v-model="queryParams.name"
            placeholder="请输入配置名称"
            clearable
            style="width: 180px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="queryParams.provider" placeholder="全部供应商" clearable style="width: 160px">
            <el-option v-for="p in PROVIDER_OPTIONS" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryParams.modelType" placeholder="全部类型" clearable style="width: 130px">
            <el-option v-for="t in MODEL_TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>模型列表</span>
          <div class="header-actions">
            <el-button
              type="primary"
              :icon="Plus"
              v-permission="'system:ai-model:create'"
              @click="openDialog()"
            >
              新增模型
            </el-button>
          </div>
        </div>
      </template>

      <SkeletonBlock v-if="loading && !list.length" mode="cards" :count="4" />

      <div v-else-if="list.length" v-loading="loading" class="model-grid">
        <AiModelCard
          v-for="item in list"
          :key="item.id"
          :item="item"
          :can-update="canUpdate"
          :testing="testingId === item.id"
          :test-latency="testResults[item.id]"
          @toggle-status="toggleStatus"
          @test="handleTest"
          @set-default="handleSetDefault"
          @edit="openDialog"
          @delete="handleDelete"
        />
      </div>

      <EmptyState
        v-else
        title="暂无模型配置"
        description="点击右上角「新增模型」即可接入 DeepSeek、Kimi、GLM 或自定义网关"
      />

      <el-pagination
        v-if="total > 0"
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[8, 16, 32]"
        layout="total, sizes, prev, pager, next, jumper"
        class="table-pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <AiModelFormDialog
      ref="dialogComponentRef"
      v-model:visible="dialogVisible"
      v-model:form="form"
      :rules="rules"
      :model-options="modelOptions"
      :base-url-placeholder="effectiveBaseUrlPlaceholder"
      :edit-has-key="editHasKey"
      :edit-is-default="editIsDefault"
      :fetching-models="fetchingModels"
      :form-testing="formTesting"
      :submitting="submitting"
      @closed="resetForm"
      @select-provider="selectProvider"
      @protocol-change="onProtocolChange"
      @fetch-models="handleFetchModels"
      @form-test="handleFormTest"
      @submit="handleSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import { MODEL_TYPE_OPTIONS, PROVIDER_OPTIONS } from './constants/aiModelMeta'
import AiModelCard from './components/AiModelCard.vue'
import AiModelFormDialog from './components/AiModelFormDialog.vue'
import { useAiModelPage } from './composables/useAiModelPage'

const dialogComponentRef = ref<InstanceType<typeof AiModelFormDialog> | null>(null)

const {
  canUpdate,
  loading,
  total,
  list,
  testingId,
  testResults,
  queryParams,
  dialogVisible,
  submitting,
  formTesting,
  fetchingModels,
  formRef,
  editHasKey,
  editIsDefault,
  form,
  effectiveBaseUrlPlaceholder,
  rules,
  modelOptions,
  loadData,
  handleQuery,
  resetQuery,
  selectProvider,
  onProtocolChange,
  openDialog,
  resetForm,
  handleSubmit,
  handleTest,
  handleFormTest,
  handleFetchModels,
  handleSetDefault,
  toggleStatus,
  handleDelete,
} = useAiModelPage()

watchEffect(() => {
  formRef.value = dialogComponentRef.value?.formRef
})
</script>

<style scoped lang="scss">
.model-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-4);
  min-height: 120px;
}

@media (max-width: 1200px) {
  .model-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 780px) {
  .model-grid {
    grid-template-columns: 1fr;
  }
}
</style>
