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
      <el-input v-model="draft.copyright" maxlength="120" placeholder="例如：Copyright © 2026 Admin Platform" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item label="ICP备案展示">
      <el-switch v-model="draft.icpEnabled" :disabled="!canEdit" />
      <span class="form-item-tip">开启后将在网站登录页、注册页及系统底部显示ICP备案号</span>
    </el-form-item>
    <el-form-item v-if="draft.icpEnabled" label="ICP备案号">
      <el-input v-model="draft.icpNumber" maxlength="60" placeholder="例如：粤ICP备XXXXXXXX号-1" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item v-if="draft.icpEnabled" label="工信部链接">
      <el-input v-model="draft.icpUrl" maxlength="120" placeholder="默认：https://beian.miit.gov.cn" :disabled="!canEdit" />
    </el-form-item>
  </el-form>
</template>

<style scoped>
.form-item-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
}
</style>

<script setup lang="ts">
import type { ConfigGroupMap } from '@/types/config'

defineProps<{
  draft: ConfigGroupMap['site']
  canEdit: boolean
}>()
</script>
