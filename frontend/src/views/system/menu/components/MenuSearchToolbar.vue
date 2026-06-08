<template>
  <el-form :model="queryParams" inline class="module-search-form search-form menu-search-form">
    <el-form-item label="菜单名称">
      <el-input
        v-model="queryParams.name"
        placeholder="请输入菜单名称"
        clearable
        style="width: 200px"
        @keyup.enter="$emit('query')"
      />
    </el-form-item>
    <el-form-item label="菜单类型">
      <el-select v-model="queryParams.type" placeholder="全部类型" clearable style="width: 130px">
        <el-option label="目录" :value="1" />
        <el-option label="菜单" :value="2" />
        <el-option label="按钮" :value="3" />
      </el-select>
    </el-form-item>
    <el-form-item label="状态">
      <DictSelect
        v-model="queryParams.status"
        dict-type="sys_normal_disable"
        value-type="number"
        placeholder="全部状态"
        width="120px"
      />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" :icon="Search" @click="$emit('query')">搜索</el-button>
      <el-button :icon="Refresh" @click="$emit('reset')">重置</el-button>
    </el-form-item>
  </el-form>

  <div class="menu-stats">
    <span class="stat-item"><el-tag type="info" size="small">目录</el-tag> {{ menuStats.dir }}</span>
    <span class="stat-item"><el-tag type="success" size="small">菜单</el-tag> {{ menuStats.menu }}</span>
    <span class="stat-item"><el-tag type="warning" size="small">按钮</el-tag> {{ menuStats.button }}</span>
    <span class="stat-hint">目录 {{ menuStats.dir }} · 菜单 {{ menuStats.menu }} · 按钮 {{ menuStats.button }}</span>
  </div>

  <div class="table-toolbar">
    <div class="toolbar-left">
      <el-button v-permission="'system:menu:create'" type="primary" :icon="Plus" @click="$emit('add')">
        新增菜单
      </el-button>
      <el-button :icon="Sort" @click="$emit('toggle-expand')">{{ expandAll ? '全部折叠' : '全部展开' }}</el-button>
    </div>
    <div class="toolbar-right">
      <RecycleCenterLink tab="menu" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Search, Refresh, Plus, Sort } from '@element-plus/icons-vue'
import RecycleCenterLink from '@/components/RecycleCenterLink.vue'
import type { MenuListQuery } from '@/api/system/menu'

defineProps<{
  queryParams: MenuListQuery
  menuStats: { dir: number; menu: number; button: number; total: number }
  expandAll: boolean
}>()

defineEmits<{
  query: []
  reset: []
  add: []
  'toggle-expand': []
}>()
</script>

<style scoped>
.search-form {
  margin-bottom: 4px;
}

.menu-search-form {
  justify-content: center;
}

.menu-search-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.menu-search-form :deep(.el-form-item__label),
.menu-search-form :deep(.el-form-item__content) {
  align-items: center;
}

.menu-stats {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 14px;
  padding: 10px 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  font-size: 13px;
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.stat-hint {
  color: var(--el-text-color-secondary);
  margin-left: auto;
  font-size: 12px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 768px) {
  .stat-hint {
    margin-left: 0;
    width: 100%;
  }
}
</style>
