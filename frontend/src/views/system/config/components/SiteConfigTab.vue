<template>
  <el-form :model="draft" label-width="120px" class="config-form">
    <el-form-item label="平台名称">
      <el-input v-model="draft.platformName" maxlength="50" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item label="平台副标题">
      <el-input v-model="draft.platformSubtitle" maxlength="80" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item label="登录页标题">
      <el-input v-model="draft.loginWelcome" maxlength="30" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item label="注册页标题">
      <el-input v-model="draft.registerTitle" maxlength="30" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item label="页脚版权">
      <el-input v-model="draft.copyright" maxlength="120" placeholder="选填" :disabled="!canEdit" />
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import type { ConfigGroupMap } from '@/types/config'

defineProps<{
  draft: ConfigGroupMap['site']
  canEdit: boolean
}>()
</script>
