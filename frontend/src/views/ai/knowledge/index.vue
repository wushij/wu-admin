<template>
  <div class="app-container module-page ai-knowledge-page">
    <PageHeader
      title="AI 知识库"
      description="管理知识库、文档与向量切片，为 AI 助手提供可检索的企业知识"
      :icon="MODULE_PAGE_ICON.aiKnowledge"
      :stats="[{ label: '知识库', value: total }]"
    />

    <el-card class="search-card module-search-card" shadow="never">
      <el-form :model="queryParams" inline class="module-search-form">
        <el-form-item label="名称">
          <el-input
            v-model="queryParams.name"
            placeholder="请输入知识库名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="索引状态">
          <el-select v-model="queryParams.indexStatus" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="待索引" value="IDLE" />
            <el-option label="索引中" value="INDEXING" />
            <el-option label="已就绪" value="INDEXED" />
            <el-option label="索引异常" value="FAILED" />
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
          <span>知识库列表</span>
          <div class="header-actions">
            <el-button
              type="primary"
              :icon="Plus"
              v-permission="'system:ai-knowledge:create'"
              @click="openDialog()"
            >
              新建知识库
            </el-button>
          </div>
        </div>
      </template>

      <SkeletonBlock v-if="loading && !list.length" mode="cards" :count="3" />

      <div v-else-if="list.length" v-loading="loading" class="kb-grid">
        <KnowledgeBaseCard
          v-for="item in list"
          :key="item.id"
          :item="item"
          @open="openDetail"
          @edit="openDialog"
          @delete="handleDelete"
        />
      </div>

      <EmptyState
        v-else
        title="还没有知识库"
        description="创建知识库后上传 PDF、Word、Markdown 或 TXT 文档，系统会自动切片并生成向量索引"
      >
        <template #actions>
          <el-button
            type="primary"
            :icon="Plus"
            v-permission="'system:ai-knowledge:create'"
            @click="openDialog()"
          >
            新建知识库
          </el-button>
        </template>
      </EmptyState>

      <el-pagination
        v-if="total > 0"
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[9, 18, 36]"
        layout="total, sizes, prev, pager, next, jumper"
        class="table-pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <KnowledgeBaseFormDialog
      ref="dialogComponentRef"
      v-model:visible="dialogVisible"
      v-model:form="form"
      :rules="rules"
      :embedding-models="embeddingModels"
      :submitting="submitting"
      @closed="formRef?.clearValidate()"
      @submit="handleSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import KnowledgeBaseCard from './components/KnowledgeBaseCard.vue'
import KnowledgeBaseFormDialog from './components/KnowledgeBaseFormDialog.vue'
import { useKnowledgeBaseList } from './composables/useKnowledgeBaseList'

const dialogComponentRef = ref<InstanceType<typeof KnowledgeBaseFormDialog> | null>(null)

const {
  loading,
  total,
  list,
  queryParams,
  embeddingModels,
  dialogVisible,
  submitting,
  formRef,
  form,
  rules,
  loadData,
  handleQuery,
  resetQuery,
  openDialog,
  handleSubmit,
  openDetail,
  handleDelete,
} = useKnowledgeBaseList()

watchEffect(() => {
  formRef.value = dialogComponentRef.value?.formRef
})
</script>

<style scoped lang="scss">
.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: var(--space-4);
  min-height: 120px;
}

@media (max-width: 640px) {
  .kb-grid {
    grid-template-columns: 1fr;
  }
}
</style>
