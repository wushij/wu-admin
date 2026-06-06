<template>
  <el-card class="dict-type-card" shadow="never" v-loading="loading">
    <template #header>
      <div class="card-header">
        <span>字典类型</span>
        <el-button v-permission="'system:dict:create'" type="primary" size="small" @click="$emit('add-type')">
          新增
        </el-button>
      </div>
    </template>
    <el-input
      :model-value="typeFilter"
      placeholder="筛选名称/编码"
      clearable
      class="type-filter"
      @update:model-value="$emit('update:typeFilter', $event)"
    />
    <div class="type-list">
      <div
        v-for="row in filteredTypes"
        :key="row.id"
        class="type-item"
        :class="{ active: selectedType?.id === row.id }"
        @click="$emit('select-type', row)"
      >
        <div class="type-item-main">
          <span class="type-name">{{ row.dictName }}</span>
          <el-tag size="small" type="info">{{ row.dataCount ?? 0 }} 项</el-tag>
        </div>
        <div class="type-code">{{ row.dictType }}</div>
        <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" class="type-status">
          {{ row.status === 1 ? '启用' : '停用' }}
        </el-tag>
      </div>
      <el-empty v-if="!filteredTypes.length" description="暂无字典类型" :image-size="64" />
    </div>
    <el-pagination
      v-model:current-page="queryParams.pageNo"
      v-model:page-size="queryParams.pageSize"
      :total="total"
      small
      layout="total, prev, pager, next"
      class="type-pager"
      @current-change="$emit('load-types')"
      @size-change="$emit('load-types')"
    />
  </el-card>
</template>

<script setup lang="ts">
import type { DictTypeVO, DictTypePageQuery } from '@/api/system/dict'

defineProps<{
  loading: boolean
  typeFilter: string
  filteredTypes: DictTypeVO[]
  selectedType: DictTypeVO | null
  queryParams: DictTypePageQuery
  total: number
}>()

defineEmits<{
  'update:typeFilter': [value: string]
  'add-type': []
  'select-type': [row: DictTypeVO]
  'load-types': []
}>()
</script>

<style scoped lang="scss">
.dict-type-card {
  flex: 0 0 320px;
  display: flex;
  flex-direction: column;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.type-filter {
  margin-bottom: 12px;
}
.type-list {
  flex: 1;
  overflow-y: auto;
  max-height: 420px;
}
.type-item {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid transparent;
  cursor: pointer;
  margin-bottom: 6px;
  transition: background 0.15s;
  &:hover {
    background: var(--el-fill-color-light);
  }
  &.active {
    background: var(--el-color-primary-light-9);
    border-color: var(--el-color-primary-light-5);
  }
}
.type-item-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.type-name {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.type-code {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  word-break: break-all;
}
.type-status {
  margin-top: 6px;
}
.type-pager {
  margin-top: 12px;
  justify-content: center;
}
</style>
