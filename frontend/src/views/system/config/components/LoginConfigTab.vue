<template>
  <el-form :model="draft" label-width="120px" class="config-form">
    <el-form-item label="登录人机校检">
      <el-switch v-model="draft.captchaEnabled" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item v-if="draft.captchaEnabled" label="验证码类型">
      <el-radio-group v-model="draft.captchaType" :disabled="!canEdit">
        <el-radio label="image">图片</el-radio>
        <el-radio label="slider">滑块</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="短信验证码登录">
      <el-switch v-model="draft.smsLoginEnabled" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item v-if="draft.smsLoginEnabled" label="发送前滑块验证">
      <el-switch v-model="draft.smsLoginSliderCaptchaEnabled" :disabled="!canEdit" />
      <span class="unit">获取短信验证码前需完成滑块验证</span>
    </el-form-item>
    <el-form-item v-if="draft.smsLoginEnabled && !smsEnabled" label=" ">
      <el-alert type="warning" :closable="false" show-icon title="请先在「短信配置」中开启短信功能，否则无法保存" />
    </el-form-item>
    <el-form-item label="邮箱验证码登录">
      <el-switch v-model="draft.emailLoginEnabled" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item v-if="draft.emailLoginEnabled" label="邮箱发送前滑块">
      <el-switch v-model="draft.emailLoginSliderCaptchaEnabled" :disabled="!canEdit" />
      <span class="unit">获取邮箱登录验证码前需完成滑块验证</span>
    </el-form-item>
    <el-form-item v-if="draft.emailLoginEnabled && !emailEnabled" label=" ">
      <el-alert type="warning" :closable="false" show-icon title="请先在「邮件配置」中开启邮件功能，否则无法保存" />
    </el-form-item>
    <el-form-item label="记住我">
      <el-switch v-model="draft.rememberMe" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item label="账号最大重试">
      <el-input-number v-model="draft.maxRetryCount" :min="1" :max="20" :disabled="!canEdit" />
      <span class="unit">次后锁定该账号</span>
    </el-form-item>
    <el-form-item label="IP 最大重试">
      <el-input-number v-model="draft.maxRetryCountIp" :min="1" :max="50" :disabled="!canEdit" />
      <span class="unit">次后锁定该 IP（同一出口共享计数）</span>
    </el-form-item>
    <el-form-item label="锁定时长">
      <el-input-number v-model="draft.lockTime" :min="1" :max="120" :disabled="!canEdit" />
      <span class="unit">分钟</span>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import type { ConfigGroupMap } from '@/types/config'

defineProps<{
  draft: ConfigGroupMap['login']
  canEdit: boolean
  smsEnabled: boolean
  emailEnabled?: boolean
}>()
</script>
