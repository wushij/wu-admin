<template>
  <el-card shadow="never" class="table-card">
    <template #header>
      <div class="table-header">
        <div class="table-header-left">
          <span v-if="currentType" class="table-header-icon" aria-hidden="true">
            <el-icon><component :is="currentType.icon" /></el-icon>
          </span>
          <span>{{ currentType?.label }}回收列表</span>
        </div>
        <el-button type="primary" link class="refresh-btn" @click="$emit('refresh')">
          <span class="refresh-label-wrap">
            <el-icon class="refresh-icon" :class="{ 'refresh-icon--spinning': refreshing }">
              <Refresh />
            </el-icon>
            刷新
          </span>
        </el-button>
      </div>
    </template>

    <InlineAlert
      v-if="currentType?.hint"
      type="warning"
      :title="currentType.hint"
      compact
      class="type-hint"
    />

    <el-form v-if="currentType?.searchFields?.length" inline class="search-form">
      <el-form-item v-for="field in currentType.searchFields" :key="field.key" :label="field.label">
        <el-input
          v-model="searchForm[field.key]"
          :placeholder="field.placeholder"
          clearable
          style="width: 200px"
          @keyup.enter="$emit('search')"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="$emit('search')">搜索</el-button>
        <el-button @click="$emit('reset')">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table
      v-if="tableData.length || loading"
      :data="tableData"
      v-loading="loading"
      border
      stripe
      :header-cell-style="{ textAlign: 'center' }"
      :cell-style="{ textAlign: 'center' }"
    >
      <el-table-column
        v-for="col in currentType?.columns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        show-overflow-tooltip
      >
        <template v-if="col.tag && col.dictType" #default="{ row }">
          <DictTag :value="row[col.prop]" :dict-type="col.dictType" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <div class="action-buttons">
            <el-button
              v-if="canRestore"
              type="primary"
              plain
              size="small"
              @click="$emit('restore', row)"
            >
              恢复
            </el-button>
            <el-button
              v-if="canDeletePermanent"
              type="danger"
              plain
              size="small"
              @click="$emit('deletePermanent', row)"
            >
              彻底清除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <EmptyState
      v-else
      title="暂无已删除数据"
      :description="`当前「${currentType?.label || ''}」分类下没有待恢复或待清理的记录`"
    />

    <el-pagination
      v-if="total > 0"
      v-model:current-page="query.pageNo"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      class="table-pagination"
      @size-change="$emit('pageChange')"
      @current-change="$emit('pageChange')"
    />
  </el-card>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue'
import DictTag from '@/components/DictTag.vue'
import InlineAlert from '@/components/ui/InlineAlert.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import type { RecycleTypeConfig } from '../recycle-config'

const searchForm = defineModel<Record<string, string>>('searchForm', { required: true })
const query = defineModel<{ pageNo: number; pageSize: number }>('query', { required: true })

defineProps<{
  currentType: RecycleTypeConfig | undefined
  tableData: Record<string, unknown>[]
  loading: boolean
  refreshing: boolean
  total: number
  canRestore: boolean
  canDeletePermanent: boolean
}>()

defineEmits<{
  refresh: []
  search: []
  reset: []
  restore: [row: Record<string, unknown>]
  deletePermanent: [row: Record<string, unknown>]
  pageChange: []
}>()
</script>

<style scoped lang="scss">
.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 15px;
}

.table-header-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.table-header-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.refresh-label-wrap {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.refresh-icon--spinning {
  animation: recycle-refresh-spin 0.75s linear infinite;
}

@keyframes recycle-refresh-spin {
  to {
    transform: rotate(360deg);
  }
}

.type-hint {
  margin-bottom: var(--space-3);
}

.search-form {
  margin-bottom: var(--space-3);
}
</style>
