<template>
  <div>
    <div class="sms-config-layout">
      <div class="sms-config-left">
        <el-card shadow="never" class="sms-section-card">
          <template #header><span class="sms-card-title">基础配置</span></template>
          <el-form :model="draft" label-width="130px" class="config-form sms-form" autocomplete="off" @submit.prevent>
            <el-form-item label="启用短信">
              <el-switch v-model="draft.enabled" :disabled="!canEdit" />
              <span class="unit">开启后业务侧可发送验证码短信</span>
            </el-form-item>
            <el-form-item label="短信服务商">
              <el-select v-model="draft.provider" style="width: 100%" :disabled="!canEdit">
                <el-option label="阿里云" value="aliyunAuth" /><el-option label="腾讯云" value="tencent" />
              </el-select>
            </el-form-item>
            <el-form-item label="AccessKeyId">
              <el-input v-model="draft.accessKeyId" name="sms-access-key-id" type="password" show-password autocomplete="new-password" placeholder="阿里云 AccessKeyId / 腾讯云 SecretId" :disabled="!canEdit" />
            </el-form-item>
            <el-form-item label="AccessKeySecret">
              <el-input v-model="draft.accessKeySecret" name="sms-access-key-secret" type="password" show-password autocomplete="new-password" placeholder="阿里云 AccessKeySecret / 腾讯云 SecretKey" :disabled="!canEdit" />
            </el-form-item>
            <el-form-item label="签名">
              <el-input v-model="draft.signName" name="sms-sign-name" autocomplete="off" placeholder="控制台已审核的短信签名" :disabled="!canEdit" />
            </el-form-item>
            <el-form-item v-if="draft.provider === 'tencent'" label="腾讯云 AppId">
              <el-input v-model="draft.tencentAppId" name="sms-tencent-app-id" type="password" show-password autocomplete="new-password" placeholder="SmsSdkAppId" :disabled="!canEdit" />
            </el-form-item>
            <el-form-item v-if="draft.provider === 'aliyunAuth'" label="验证码有效期">
              <el-input-number v-model="draft.codeExpireMinutes" :min="1" :max="30" :disabled="!canEdit" />
              <span class="unit">分钟</span>
            </el-form-item>
          </el-form>
        </el-card>
        <el-card shadow="never" class="sms-section-card">
          <template #header><span class="sms-card-title">模板配置</span></template>
          <el-form :model="draft" label-width="148px" class="config-form sms-form" autocomplete="off" @submit.prevent>
            <template v-if="draft.provider === 'aliyunAuth'">
              <el-form-item label="登录/注册模板"><el-input v-model="draft.templateVerifyCode" placeholder="100001" :disabled="!canEdit" /></el-form-item>
              <el-form-item label="修改绑定手机号"><el-input v-model="draft.templateModifyPhone" placeholder="100002" :disabled="!canEdit" /></el-form-item>
              <el-form-item label="重置密码模板"><el-input v-model="draft.templateResetPassword" placeholder="100003" :disabled="!canEdit" /></el-form-item>
              <el-form-item label="绑定新手机号"><el-input v-model="draft.templateBindPhone" placeholder="100004" :disabled="!canEdit" /></el-form-item>
              <el-form-item label="验证绑定手机号"><el-input v-model="draft.templateVerifyBindPhone" placeholder="100005" :disabled="!canEdit" /></el-form-item>
            </template>
            <template v-else>
              <el-form-item label="验证码模板 ID"><el-input v-model="draft.templateVerifyCode" placeholder="如 SMS_123456789" :disabled="!canEdit" /></el-form-item>
              <el-form-item label="重置密码模板 ID"><el-input v-model="draft.templateResetPassword" placeholder="如 SMS_123456790" :disabled="!canEdit" /></el-form-item>
            </template>
          </el-form>
        </el-card>
      </div>
      <div class="sms-config-right">
        <el-card shadow="never" class="sms-section-card">
          <template #header><span class="sms-card-title">测试发送</span></template>
          <el-form label-width="72px" class="sms-test-form" autocomplete="off" @submit.prevent>
            <el-form-item v-if="draft.provider === 'aliyunAuth'" label="模板">
              <el-select v-model="testSmsTemplate" style="width: 100%" :disabled="!canEdit">
                <el-option label="100001 登录/注册" value="100001" />
                <el-option label="100002 修改绑定手机号" value="100002" />
                <el-option label="100003 重置密码" value="100003" />
                <el-option label="100004 绑定新手机号" value="100004" />
                <el-option label="100005 验证绑定手机号" value="100005" />
              </el-select>
            </el-form-item>
            <el-form-item label="手机号">
              <div class="sms-test-row">
                <el-input v-model="testSmsPhone" name="sms-test-phone" autocomplete="off" readonly placeholder="请输入 11 位手机号" maxlength="11" @focus="($event.target as HTMLInputElement).removeAttribute('readonly')" />
                <el-button type="primary" :loading="smsTesting" :disabled="!canEdit" @click="$emit('testSms')">发送</el-button>
              </div>
            </el-form-item>
          </el-form>
          <el-alert type="info" :closable="false" show-icon title="将发送一条随机 6 位验证码到该手机，用于测试短信配置是否正确。密钥未配置时会在服务端控制台打印。" />
        </el-card>
        <el-card shadow="never" class="sms-section-card">
          <template #header>
            <div class="sms-log-header">
              <span class="sms-card-title">发送记录</span>
              <el-button link type="primary" @click="$emit('showAllSmsLogs')">查看全部</el-button>
            </div>
          </template>
          <el-table v-if="recentSmsLogs.length" :data="recentSmsLogs" size="small" stripe>
            <el-table-column prop="phone" label="手机号" width="118" />
            <el-table-column prop="content" label="验证码" width="88" />
            <el-table-column label="状态" width="72">
              <template #default="{ row }">
                <el-tag :type="smsStatusTagType(row.status)" size="small">{{ smsStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作" width="60" fixed="right">
              <template #default="{ row }">
                <el-popconfirm title="确定删除该条记录吗？" @confirm="$emit('deleteSmsLog', row.id)">
                  <template #reference>
                    <el-button link type="danger" size="small" :disabled="!canEdit">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无发送记录" :image-size="64" />
        </el-card>
      </div>
    </div>
    <el-alert type="info" :closable="false" show-icon class="sms-tip-alert">
      填写密钥与模板后请点击页底「保存全部」；保存后再使用测试发送验证配置是否正确。
    </el-alert>
  </div>
</template>

<script setup lang="ts">
import type { ConfigGroupMap } from '@/types/config'
import type { SmsLogRecord } from '@/types/config'

defineProps<{
  draft: ConfigGroupMap['sms']
  canEdit: boolean
  smsTesting: boolean
  recentSmsLogs: SmsLogRecord[]
  smsStatusText: (status: number) => string
  smsStatusTagType: (status: number) => 'success' | 'danger' | 'warning'
}>()

const testSmsPhone = defineModel<string>('testSmsPhone', { required: true })
const testSmsTemplate = defineModel<string>('testSmsTemplate', { required: true })

defineEmits<{
  testSms: []
  showAllSmsLogs: []
  deleteSmsLog: [id: number | string]
}>()
</script>
