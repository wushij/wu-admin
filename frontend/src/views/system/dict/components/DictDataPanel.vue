<template>
  <el-card class="dict-data-card" shadow="never">
    <template #header>
      <div class="card-header">
        <span v-if="selectedType">
          {{ selectedType.dictName }}
          <el-text type="info" size="small" class="header-code">（{{ selectedType.dictType }}）</el-text>
        </span>
        <span v-else class="text-muted">请从左侧选择字典类型</span>
        <div v-if="selectedType" class="header-actions">
          <el-button size="small" @click="$emit('refresh-cache')">刷新缓存</el-button>
          <el-button v-permission="'system:dict:copy'" size="small" @click="$emit('copy-type')">复制类型</el-button>
          <el-button v-permission="'system:dict:update'" size="small" @click="$emit('edit-type', selectedType)">
            编辑类型
          </el-button>
          <el-button v-permission="'system:dict:delete'" type="danger" size="small" @click="$emit('delete-type', selectedType)">
            删除类型
          </el-button>
          <el-button v-permission="'system:dict:create'" type="primary" size="small" @click="$emit('add-data')">
            新增数据
          </el-button>
        </div>
      </div>
    </template>

    <template v-if="selectedType">
      <el-input
        :model-value="dataFilter"
        placeholder="筛选标签或键值"
        clearable
        class="data-filter"
        @update:model-value="$emit('update:dataFilter', $event)"
      />
      <el-table
        :data="filteredDataList"
        v-loading="dataLoading"
        border
        stripe
        size="small"
        :header-cell-style="{ textAlign: 'center' }"
        :cell-style="{ textAlign: 'center' }"
      >
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column prop="dictLabel" label="字典标签" min-width="110" />
        <el-table-column prop="dictValue" label="字典键值" min-width="100" />
        <el-table-column label="回显" width="100">
          <template #default="{ row }">
            <el-tag
              :type="listClassToTagType(row.listClass) as 'success' | 'primary' | 'warning' | 'info' | 'danger'"
              size="small"
            >
              {{ row.dictLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="默认" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault === 1" type="warning" size="small">默认</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <DictTag :value="row.status" :dict-type="DICT_TYPE.NORMAL_DISABLE" />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button v-permission="'system:dict:update'" type="primary" size="small" @click="$emit('edit-data', row)">
                编辑
              </el-button>
              <el-button v-permission="'system:dict:delete'" type="danger" size="small" @click="$emit('delete-data', row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </template>
    <el-empty v-else description="选择左侧字典类型后在此维护选项" />
  </el-card>
</template>

<script setup lang="ts">
import type { DictDataItem } from '@/types/api'
import type { DictTypeVO } from '@/api/system/dict'
import { listClassToTagType } from '@/composables/useDict'
import { DICT_TYPE } from '@/constants/dict'

defineProps<{
  selectedType: DictTypeVO | null
  dataFilter: string
  filteredDataList: DictDataItem[]
  dataLoading: boolean
}>()

defineEmits<{
  'update:dataFilter': [value: string]
  'refresh-cache': []
  'copy-type': []
  'edit-type': [row: DictTypeVO]
  'delete-type': [row: DictTypeVO]
  'add-data': []
  'edit-data': [row: DictDataItem]
  'delete-data': [row: DictDataItem]
}>()
</script>

<style scoped lang="scss">
.dict-data-card {
  flex: 1;
  min-width: 0;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.header-code {
  margin-left: 4px;
}
.data-filter {
  margin-bottom: 12px;
}
.text-muted {
  color: var(--el-text-color-secondary);
}
.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
</style>
