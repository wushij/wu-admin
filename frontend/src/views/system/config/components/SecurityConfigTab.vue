<template>
  <div>
    <el-form :model="draft" label-width="140px" class="config-form">
      <el-divider content-position="left">前端安全</el-divider>
      <el-form-item label="禁止前端调试">
        <el-switch :model-value="draft.disableDevtool" :disabled="!canEdit" @update:model-value="(v) => draft.disableDevtool = !!v" />
        <span class="unit">开启后将限制打开开发者工具（F12），降低随意查看源码与调试的风险</span>
      </el-form-item>
      <el-divider content-position="left">会话安全</el-divider>
      <el-form-item label="禁止多端同时在线">
        <el-switch :model-value="forbidConcurrentLogin" :disabled="!canEdit" @update:model-value="(v) => $emit('update:forbidConcurrentLogin', !!v)" />
        <span class="unit">开启后，同一账号再次登录会先踢掉之前的会话，只保留最新一次登录</span>
      </el-form-item>
    </el-form>
    <el-alert type="info" :closable="false" show-icon title="保存全部后立即生效：禁止多端对新登录生效；禁止前端调试需刷新浏览器页面。" />
  </div>
</template>

<script setup lang="ts">
import type { ConfigGroupMap } from '@/types/config'

defineProps<{
  draft: ConfigGroupMap['security']
  canEdit: boolean
  forbidConcurrentLogin: boolean
}>()

defineEmits<{
  'update:forbidConcurrentLogin': [value: boolean]
}>()
</script>
