<template>
  <el-form :model="draft" label-width="120px" class="config-form">
    <el-form-item label="Token 有效期">
      <el-input-number v-model="draft.tokenExpireHours" :min="1" :max="720" :disabled="!canEdit" />
      <span class="unit">小时（保存全部后生效）</span>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import type { ConfigGroupMap } from '@/types/config'

defineProps<{
  draft: ConfigGroupMap['session']
  canEdit: boolean
}>()
</script>
