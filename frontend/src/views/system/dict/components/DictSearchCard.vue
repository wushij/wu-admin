<template>
  <el-card class="search-card" shadow="never">
    <el-form :model="queryParams" inline>
      <el-form-item label="字典名称">
        <el-input v-model="queryParams.dictName" placeholder="请输入字典名称" clearable @keyup.enter="$emit('query')" />
      </el-form-item>
      <el-form-item label="字典类型">
        <el-input v-model="queryParams.dictType" placeholder="编码，如 sys_xxx" clearable @keyup.enter="$emit('query')" />
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect
          v-model="queryParams.status"
          :dict-type="DICT_TYPE.NORMAL_DISABLE"
          value-type="number"
          placeholder="全部状态"
          width="120px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="$emit('query')">搜索</el-button>
        <el-button @click="$emit('reset')">重置</el-button>
      </el-form-item>
    </el-form>
    <p class="dict-hint">
      左侧选择字典类型，右侧维护选项；业务表单通过
      <code>DictSelect</code> / <code>DictTag</code> 引用类型编码。保存后已自动同步本地缓存，其他已打开页面请刷新。
    </p>
  </el-card>
</template>

<script setup lang="ts">
import type { DictTypePageQuery } from '@/api/system/dict'
import { DICT_TYPE } from '@/constants/dict'

defineProps<{ queryParams: DictTypePageQuery }>()
defineEmits<{ query: []; reset: [] }>()
</script>

<style scoped lang="scss">
.search-card {
  margin-bottom: 12px;
}
.dict-hint {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  code {
    padding: 0 4px;
    background: var(--el-fill-color-light);
    border-radius: 4px;
  }
}
</style>
