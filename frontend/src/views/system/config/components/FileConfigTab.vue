<template>
  <el-form :model="draft" label-width="120px" class="config-form">
    <el-form-item label="单文件上限">
      <el-input-number v-model="draft.maxSizeMb" :min="1" :max="platformMaxFileMb" :disabled="!canEdit" />
      <span class="unit">MB（保存全部后生效，最高 {{ platformMaxFileMb }}）</span>
    </el-form-item>
    <el-form-item label="允许扩展名">
      <el-input v-model="draft.allowedExtensions" type="textarea" :rows="3" placeholder="逗号分隔，如 jpg,png,pdf" :disabled="!canEdit" />
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import type { ConfigGroupMap } from '@/types/config'

defineProps<{
  draft: ConfigGroupMap['file']
  canEdit: boolean
  platformMaxFileMb: number
}>()
</script>
