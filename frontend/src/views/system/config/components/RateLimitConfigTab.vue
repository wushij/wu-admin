<template>
  <el-form :model="draft" label-width="180px" class="config-form">
    <el-form-item label="验证码(次/分钟/IP)">
      <el-input-number v-model="draft.captchaPerIpMinute" :min="0" :max="200" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item label="登录(次/分钟/IP)">
      <el-input-number v-model="draft.loginPerIpMinute" :min="0" :max="200" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item label="注册(次/分钟/IP)">
      <el-input-number v-model="draft.registerPerIpMinute" :min="0" :max="200" :disabled="!canEdit" />
    </el-form-item>
    <el-divider content-position="left">短信发送防刷</el-divider>
    <el-form-item label="短信(次/分钟/IP)">
      <el-input-number v-model="draft.smsPerIpMinute" :min="0" :max="200" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item label="同号发送间隔(秒)">
      <el-input-number v-model="draft.smsSendIntervalSeconds" :min="30" :max="300" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item label="同号每日上限(次)">
      <el-input-number v-model="draft.smsPerPhoneDaily" :min="0" :max="500" :disabled="!canEdit" />
      <span class="unit">0 表示不限制</span>
    </el-form-item>
    <el-form-item label="同 IP 每日上限(次)">
      <el-input-number v-model="draft.smsPerIpDaily" :min="0" :max="500" :disabled="!canEdit" />
      <span class="unit">0 表示不限制</span>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import type { ConfigGroupMap } from '@/types/config'

defineProps<{
  draft: ConfigGroupMap['rateLimit']
  canEdit: boolean
}>()
</script>
