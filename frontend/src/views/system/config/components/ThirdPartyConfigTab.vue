<template>
  <el-collapse class="pay-collapse">
    <el-collapse-item title="微信登录" name="wechat">
      <el-form :model="draft.wechat" label-width="120px" class="config-form wide-form" autocomplete="off" @submit.prevent>
        <el-form-item label="启用"><el-switch v-model="draft.wechat.enabled" :disabled="!canEdit" /></el-form-item>
        <el-form-item label="AppID">
          <el-input v-model="draft.wechat.appId" name="oauth-wechat-app-id" autocomplete="off" placeholder="微信开放平台 AppID" :disabled="!canEdit" />
        </el-form-item>
        <el-form-item label="AppSecret">
          <el-input v-model="draft.wechat.appSecret" name="oauth-wechat-app-secret" type="password" show-password autocomplete="new-password" placeholder="微信开放平台 AppSecret" :disabled="!canEdit" />
        </el-form-item>
      </el-form>
    </el-collapse-item>
    <el-collapse-item title="支付宝登录" name="alipay-oauth">
      <el-form :model="draft.alipay" label-width="120px" class="config-form wide-form" autocomplete="off" @submit.prevent>
        <el-form-item label="启用"><el-switch v-model="draft.alipay.enabled" :disabled="!canEdit" /></el-form-item>
        <el-form-item label="AppID">
          <el-input v-model="draft.alipay.appId" name="oauth-alipay-app-id" autocomplete="off" placeholder="支付宝应用 AppID" :disabled="!canEdit" />
        </el-form-item>
        <el-form-item label="应用私钥"><el-input v-model="draft.alipay.privateKey" type="textarea" :rows="3" :disabled="!canEdit" /></el-form-item>
        <el-form-item label="支付宝公钥"><el-input v-model="draft.alipay.publicKey" type="textarea" :rows="3" :disabled="!canEdit" /></el-form-item>
      </el-form>
    </el-collapse-item>
    <el-collapse-item title="GitHub 登录" name="github">
      <el-form :model="draft.github" label-width="120px" class="config-form wide-form" autocomplete="off" @submit.prevent>
        <el-form-item label="启用"><el-switch v-model="draft.github.enabled" :disabled="!canEdit" /></el-form-item>
        <el-form-item label="Client ID">
          <el-input v-model="draft.github.clientId" name="oauth-github-client-id" autocomplete="off" placeholder="GitHub OAuth Client ID" :disabled="!canEdit" />
        </el-form-item>
        <el-form-item label="Client Secret">
          <el-input v-model="draft.github.clientSecret" name="oauth-github-client-secret" type="password" show-password autocomplete="new-password" placeholder="GitHub OAuth Client Secret" :disabled="!canEdit" />
        </el-form-item>
      </el-form>
    </el-collapse-item>
    <el-collapse-item title="Google 登录" name="google">
      <el-form :model="draft.google" label-width="120px" class="config-form wide-form" autocomplete="off" @submit.prevent>
        <el-form-item label="启用"><el-switch v-model="draft.google.enabled" :disabled="!canEdit" /></el-form-item>
        <el-form-item label="Client ID">
          <el-input v-model="draft.google.clientId" name="oauth-google-client-id" autocomplete="off" placeholder="Google Cloud OAuth 客户端 ID" :disabled="!canEdit" />
        </el-form-item>
        <el-form-item label="Client Secret">
          <el-input v-model="draft.google.clientSecret" name="oauth-google-client-secret" type="password" show-password autocomplete="new-password" placeholder="Google OAuth 客户端密钥" :disabled="!canEdit" />
        </el-form-item>
        <el-form-item label="重定向 URI">
          <el-input v-model="draft.google.redirectUri" name="oauth-google-redirect-uri" autocomplete="off" placeholder="https://你的域名/api/auth/oauth/google/callback" :disabled="!canEdit" />
          <span class="unit">须在 Google Cloud 控制台「已获授权的重定向 URI」中配置相同地址</span>
        </el-form-item>
      </el-form>
    </el-collapse-item>
  </el-collapse>
</template>

<script setup lang="ts">
import type { ConfigGroupMap } from '@/types/config'

defineProps<{
  draft: ConfigGroupMap['thirdParty']
  canEdit: boolean
}>()
</script>
