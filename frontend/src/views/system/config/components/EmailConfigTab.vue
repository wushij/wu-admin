<template>
  <div>
    <div class="sms-config-layout">
      <div class="sms-config-left">
        <!-- 基础发件配置 -->
        <el-card shadow="never" class="sms-section-card">
          <template #header><span class="sms-card-title">SMTP 发件基础配置</span></template>
          <el-form :model="draft" label-width="140px" class="config-form sms-form" autocomplete="off" @submit.prevent>
            <el-form-item label="启用邮件服务">
              <el-switch v-model="draft.enabled" :disabled="!canEdit" />
              <span class="unit">开启后系统可发送验证码与业务通知邮件</span>
            </el-form-item>

            <el-form-item label="邮件服务商">
              <el-select v-model="draft.provider" style="width: 100%" :disabled="!canEdit" @change="onProviderChange">
                <el-option label="QQ 邮箱 (smtp.qq.com)" value="qq" />
                <el-option label="网易 163 邮箱 (smtp.163.com)" value="163" />
                <el-option label="Google Gmail (smtp.gmail.com)" value="gmail" />
                <el-option label="自定义 SMTP 服务器" value="custom" />
              </el-select>
            </el-form-item>

            <el-form-item label="SMTP 服务器">
              <el-input v-model="draft.host" name="email-host" autocomplete="off" placeholder="如 smtp.qq.com 或 smtp.gmail.com" :disabled="!canEdit" />
            </el-form-item>

            <el-form-item label="SMTP 端口">
              <div class="input-with-unit">
                <el-input-number v-model="draft.port" :min="1" :max="65535" :disabled="!canEdit" style="width: 130px" />
                <span class="unit">常规: 465 (SSL) / 587 (TLS/STARTTLS) / 25</span>
              </div>
            </el-form-item>

            <el-form-item label="加密传输方式">
              <el-radio-group v-model="draft.securityType" :disabled="!canEdit">
                <el-radio value="SSL">SSL (推荐465)</el-radio>
                <el-radio value="TLS">TLS (587)</el-radio>
                <el-radio value="STARTTLS">STARTTLS</el-radio>
                <el-radio value="NONE">无加密 (25)</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="启用 SMTP 认证">
              <el-switch v-model="draft.authEnabled" :disabled="!canEdit" />
              <span class="unit">绝大多数发件箱均需开启密码/授权码校验</span>
            </el-form-item>

            <el-form-item label="发件人邮箱账号">
              <el-input v-model="draft.username" name="email-username" autocomplete="off" placeholder="如 wu@gmail.com" :disabled="!canEdit" />
            </el-form-item>

            <el-form-item label="SMTP 授权码/密码">
              <el-input v-model="draft.password" name="email-password" type="password" show-password autocomplete="new-password" placeholder="邮箱服务端生成的专有授权码" :disabled="!canEdit" />
            </el-form-item>

            <el-form-item label="发件人显示名称">
              <el-input v-model="draft.fromName" name="email-from-name" autocomplete="off" placeholder="如 ERP 管理系统" :disabled="!canEdit" />
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 高级网络与超时配置 -->
        <el-card shadow="never" class="sms-section-card" style="margin-top: 16px;">
          <template #header><span class="sms-card-title">高级超时与通信配置</span></template>
          <el-form :model="draft" label-width="140px" class="config-form sms-form" autocomplete="off" @submit.prevent>
            <el-form-item label="连接超时 (ms)">
              <div class="input-with-unit">
                <el-input-number v-model="draft.connectionTimeoutMs" :min="1000" :max="30000" :step="1000" :disabled="!canEdit" style="width: 130px" />
                <span class="unit">默认 5000ms，连接 SMTP 服务器超时限</span>
              </div>
            </el-form-item>

            <el-form-item label="读取超时 (ms)">
              <div class="input-with-unit">
                <el-input-number v-model="draft.timeoutMs" :min="1000" :max="30000" :step="1000" :disabled="!canEdit" style="width: 130px" />
                <span class="unit">默认 5000ms，等待服务器响应超时限</span>
              </div>
            </el-form-item>

            <el-form-item label="写入超时 (ms)">
              <div class="input-with-unit">
                <el-input-number v-model="draft.writeTimeoutMs" :min="1000" :max="30000" :step="1000" :disabled="!canEdit" style="width: 130px" />
                <span class="unit">默认 5000ms，数据报文发送超时限</span>
              </div>
            </el-form-item>

            <el-form-item label="默认字符编码">
              <el-select v-model="draft.encoding" style="width: 220px" :disabled="!canEdit">
                <el-option label="UTF-8 (推荐通用编码)" value="UTF-8" />
                <el-option label="GBK (简体中文扩展)" value="GBK" />
                <el-option label="GB2312 (国标简体)" value="GB2312" />
                <el-option label="ISO-8859-1 (西欧编码)" value="ISO-8859-1" />
              </el-select>
              <span class="unit">避免中文或特殊字符出现乱码</span>
            </el-form-item>

            <el-form-item label="开启 Debug 日志">
              <el-switch v-model="draft.debug" :disabled="!canEdit" />
              <span class="unit">开启后在服务端控制台输出详细 SMTP 通信报文</span>
            </el-form-item>
          </el-form>
        </el-card>
      </div>

      <div class="sms-config-right">
        <!-- 验证码与防刷规则 -->
        <el-card shadow="never" class="sms-section-card">
          <template #header><span class="sms-card-title">验证码与防刷规则</span></template>
          <el-form :model="draft" label-width="110px" class="config-form sms-form" autocomplete="off" @submit.prevent>
            <el-form-item label="验证码有效期">
              <div class="input-with-unit">
                <el-input-number v-model="draft.codeExpireMinutes" :min="1" :max="30" :disabled="!canEdit" style="width: 120px" />
                <span class="unit-text">分钟</span>
              </div>
            </el-form-item>

            <el-form-item label="验证码长度">
              <div class="input-with-unit">
                <el-input-number v-model="draft.codeLength" :min="4" :max="8" :disabled="!canEdit" style="width: 120px" />
                <span class="unit-text">位</span>
              </div>
            </el-form-item>

            <el-form-item label="发送防刷间隔">
              <div class="input-with-unit">
                <el-input-number v-model="draft.sendIntervalSeconds" :min="10" :max="300" :disabled="!canEdit" style="width: 120px" />
                <span class="unit-text">秒</span>
              </div>
            </el-form-item>

            <el-form-item label="每日单箱上限">
              <div class="input-with-unit">
                <el-input-number v-model="draft.dailyLimitPerEmail" :min="1" :max="100" :disabled="!canEdit" style="width: 120px" />
                <span class="unit-text">次</span>
              </div>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 测试发送邮件 -->
        <el-card shadow="never" class="sms-section-card" style="margin-top: 16px;">
          <template #header><span class="sms-card-title">测试发送</span></template>
          <el-form label-width="72px" class="sms-test-form" autocomplete="off" @submit.prevent>
            <el-form-item label="接收邮箱">
              <div class="sms-test-row">
                <el-input v-model="testEmail" name="email-test-to" autocomplete="off" placeholder="请输入接收测试邮件的邮箱" maxlength="100" />
                <el-button type="primary" :loading="emailTesting" :disabled="!canEdit" @click="$emit('testEmail', testEmail)">发送</el-button>
              </div>
            </el-form-item>
          </el-form>
          <el-alert type="info" :closable="false" show-icon title="发送一条测试报文到该邮箱，验证 SMTP 连通性、账号密码及安全端口。" />
        </el-card>

        <!-- 邮件发送记录 -->
        <el-card shadow="never" class="sms-section-card" style="margin-top: 16px;">
          <template #header>
            <div class="sms-log-header">
              <span class="sms-card-title">发送记录</span>
              <el-button link type="primary" @click="$emit('showAllEmailLogs')">查看全部</el-button>
            </div>
          </template>
          <el-table v-if="recentEmailLogs.length" :data="recentEmailLogs" size="small" stripe>
            <el-table-column prop="email" label="接收邮箱" min-width="140" show-overflow-tooltip />
            <el-table-column prop="content" label="验证码/摘要" width="100" show-overflow-tooltip />
            <el-table-column label="状态" width="72">
              <template #default="{ row }">
                <el-tag :type="emailStatusTagType(row.status)" size="small">{{ emailStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="140" show-overflow-tooltip />
          </el-table>
          <el-empty v-else description="暂无发送记录" :image-size="64" />
        </el-card>
      </div>
    </div>
    <el-alert type="info" :closable="false" show-icon class="sms-tip-alert" style="margin-top: 16px;">
      修改配置参数后，请务必点击页面底部的「保存全部」生效；保存后再进行【测试发送】联通校验。
    </el-alert>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { EmailLogRecord } from '@/types/config'

const props = defineProps<{
  draft: {
    enabled: boolean
    provider: string
    host: string
    port: number
    username: string
    password: string
    fromName: string
    authEnabled: boolean
    securityType: 'SSL' | 'TLS' | 'STARTTLS' | 'NONE'
    connectionTimeoutMs: number
    timeoutMs: number
    writeTimeoutMs: number
    encoding: string
    debug: boolean
    codeExpireMinutes: number
    codeLength: number
    dailyLimitPerEmail: number
    sendIntervalSeconds: number
  }
  savedEmail?: {
    provider: string
    host: string
    port: number
    username: string
    password: string
    securityType: 'SSL' | 'TLS' | 'STARTTLS' | 'NONE'
  }
  canEdit: boolean
  emailTesting: boolean
  recentEmailLogs: EmailLogRecord[]
  emailStatusText: (status: number) => string
  emailStatusTagType: (status: number) => 'success' | 'danger' | 'warning'
}>()

defineEmits<{
  testEmail: [toEmail: string]
  showAllEmailLogs: []
}>()

const testEmail = ref('')

function onProviderChange(val: string) {
  const saved = props.savedEmail
  // 如果切回当前数据库已保存绑定的服务商，恢复原保存的账号与授权码
  if (saved && val && val === saved.provider) {
    props.draft.host = saved.host || 'smtp.qq.com'
    props.draft.port = saved.port || 465
    props.draft.securityType = (saved.securityType as any) || 'SSL'
    props.draft.username = saved.username || ''
    props.draft.password = saved.password || ''
    return
  }

  // 否则切到未绑定的新服务商，填充预设网络参数并清空账号与密码
  if (val === 'qq') {
    props.draft.host = 'smtp.qq.com'
    props.draft.port = 465
    props.draft.securityType = 'SSL'
  } else if (val === '163') {
    props.draft.host = 'smtp.163.com'
    props.draft.port = 465
    props.draft.securityType = 'SSL'
  } else if (val === 'gmail') {
    props.draft.host = 'smtp.gmail.com'
    props.draft.port = 587
    props.draft.securityType = 'TLS'
  }

  props.draft.username = ''
  props.draft.password = ''
}
</script>

<style scoped>
.input-with-unit {
  display: flex;
  align-items: center;
  gap: 8px;
}

.unit {
  font-size: 13px;
  color: #909399;
  margin-left: 8px;
}

.unit-text {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  white-space: nowrap;
}
</style>
