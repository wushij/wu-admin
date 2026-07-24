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
      <el-divider content-position="left">API 安全防线与防重放</el-divider>
      <el-form-item label="SM4 数据加密">
        <el-switch :model-value="draft.sm4EncryptEnabled ?? false" :disabled="!canEdit" @update:model-value="(v) => draft.sm4EncryptEnabled = !!v" />
        <span class="unit">是否启用接口请求/响应数据加密（国密 SM4）</span>
      </el-form-item>
      <el-form-item label="SM2 数字签名">
        <el-switch :model-value="draft.sm2SignEnabled ?? false" :disabled="!canEdit" @update:model-value="(v) => draft.sm2SignEnabled = !!v" />
        <span class="unit">是否启用接口签名验签（国密 SM2）</span>
      </el-form-item>
      <el-form-item label="时间戳校验">
        <el-switch :model-value="draft.timestampEnabled ?? true" :disabled="!canEdit" @update:model-value="(v) => draft.timestampEnabled = !!v" />
        <span class="unit">校验请求时间，防止过期请求（5 分钟时间窗口）</span>
      </el-form-item>
      <el-form-item label="Nonce 校验">
        <el-switch :model-value="draft.nonceEnabled ?? true" :disabled="!canEdit" @update:model-value="(v) => draft.nonceEnabled = !!v" />
        <span class="unit">校验随机数，防止高频重放攻击（Redis 单次随机数查重）</span>
      </el-form-item>
    </el-form>
    <el-alert type="info" :closable="false" show-icon title="保存全部后立即生效：接口安全开关将即时作用于系统所有 REST 接口；时间戳与 Nonce 建议保持开启。" />
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
