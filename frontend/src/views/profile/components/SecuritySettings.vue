<template>
  <div>
    <div class="tab-intro tab-intro--security"><el-icon><Lock /></el-icon><span>定期更换密码可提升账号安全性，密码长度不少于 {{ minPwdLen }} 位。</span></div>

    <div v-if="securityMode === 'password'" class="security-form-card">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top" class="pwd-form pwd-form--modern" @submit.prevent>
        <el-form-item prop="oldPassword" class="pwd-field">
          <template #label>
            <div class="pwd-field-label-row">
              <span class="pwd-field-label">当前密码</span>
              <div class="pwd-forgot-group">
                <button v-if="canUseEmailReset" type="button" class="pwd-forgot-btn pwd-forgot-btn--email" @click="$emit('openEmailResetMode')">
                  <el-icon><Message /></el-icon> 邮箱重置密码
                </button>
                <button v-if="canUseSmsReset" type="button" class="pwd-forgot-btn" @click="$emit('openSmsResetMode')">
                  <el-icon><Iphone /></el-icon> 短信重置密码
                </button>
                <span v-if="!canUseSmsReset && !canUseEmailReset" class="pwd-forgot-hint">重置密码需先绑定手机号或邮箱</span>
              </div>
            </div>
          </template>
          <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入当前密码" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword" class="pwd-field">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="请输入新密码" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword" class="pwd-field">
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password autocomplete="new-password" />
        </el-form-item>
        <div class="pwd-form-actions">
          <el-button type="primary" :loading="savingPwd" @click="$emit('handleChangePassword')">修改密码</el-button>
          <el-button @click="$emit('resetPwdForm')">清空</el-button>
        </div>
      </el-form>
    </div>

    <div v-else-if="securityMode === 'sms'" class="security-form-card security-form-card--sms">
      <div class="sms-form-header">
        <button type="button" class="sms-back-btn" @click="$emit('closeSmsResetMode')"><el-icon><ArrowLeft /></el-icon>返回</button>
        <span class="sms-form-header__title">短信验证重置密码</span>
      </div>
      <div class="sms-reset-banner">
        <div class="sms-reset-banner__icon"><el-icon><Iphone /></el-icon></div>
        <div class="sms-reset-banner__body">
          <p class="sms-reset-banner__label">验证码将发送至</p>
          <p class="sms-reset-banner__phone">{{ maskedMobile }}</p>
        </div>
      </div>
      <el-form ref="smsPwdFormRef" :model="smsPwdForm" :rules="smsPwdRules" label-position="top" class="pwd-form pwd-form--modern" @submit.prevent>
        <el-form-item label="短信验证码" prop="smsCode" class="pwd-field">
          <div class="sms-code-row">
            <el-input v-model="smsPwdForm.smsCode" placeholder="请输入验证码" maxlength="6" autocomplete="off" />
            <el-button type="primary" plain class="sms-send-btn" :disabled="smsCountdown > 0 || sendingSmsCode || !canUseSmsReset" :loading="sendingSmsCode" @click="$emit('handleSendResetSmsCode')">
              {{ smsCountdown > 0 ? `${smsCountdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword" class="pwd-field">
          <el-input v-model="smsPwdForm.newPassword" type="password" placeholder="请输入新密码" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword" class="pwd-field">
          <el-input v-model="smsPwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password autocomplete="new-password" />
        </el-form-item>
        <div class="pwd-form-actions">
          <el-button type="primary" :loading="savingSmsPwd" @click="$emit('handleSmsResetPassword')">确认重置</el-button>
          <el-button @click="$emit('resetSmsPwdForm')">清空</el-button>
        </div>
      </el-form>
    </div>

    <div v-else-if="securityMode === 'email'" class="security-form-card security-form-card--email">
      <div class="sms-form-header">
        <button type="button" class="sms-back-btn" @click="$emit('closeEmailResetMode')"><el-icon><ArrowLeft /></el-icon>返回</button>
        <span class="sms-form-header__title">邮箱验证重置密码</span>
      </div>
      <div class="sms-reset-banner sms-reset-banner--email">
        <div class="sms-reset-banner__icon"><el-icon><Message /></el-icon></div>
        <div class="sms-reset-banner__body">
          <p class="sms-reset-banner__label">验证码将发送至已绑定邮箱</p>
          <p class="sms-reset-banner__phone">{{ maskedEmail }}</p>
        </div>
      </div>
      <el-form ref="emailPwdFormRef" :model="emailPwdForm" :rules="emailPwdRules" label-position="top" class="pwd-form pwd-form--modern" @submit.prevent>
        <el-form-item label="邮箱验证码" prop="emailCode" class="pwd-field">
          <div class="sms-code-row">
            <el-input v-model="emailPwdForm.emailCode" placeholder="请输入 6 位验证码" maxlength="6" autocomplete="off" />
            <el-button type="primary" plain class="sms-send-btn" :disabled="emailResetCountdown > 0 || sendingEmailResetCode || !canUseEmailReset" :loading="sendingEmailResetCode" @click="$emit('handleSendResetEmailCode')">
              {{ emailResetCountdown > 0 ? `${emailResetCountdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword" class="pwd-field">
          <el-input v-model="emailPwdForm.newPassword" type="password" placeholder="请输入新密码" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword" class="pwd-field">
          <el-input v-model="emailPwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password autocomplete="new-password" />
        </el-form-item>
        <div class="pwd-form-actions">
          <el-button type="primary" :loading="savingEmailPwd" @click="$emit('handleEmailResetPassword')">确认重置密码</el-button>
          <el-button @click="$emit('resetEmailPwdForm')">清空</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Lock, ArrowLeft, Iphone, Message } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

defineProps<{
  securityMode: 'password' | 'sms' | 'email'
  pwdForm: { oldPassword: string; newPassword: string; confirmPassword: string }
  pwdRules: FormRules
  savingPwd: boolean
  canUseSmsReset: boolean
  hasBoundMobile: boolean
  canUseEmailReset: boolean
  hasBoundEmail: boolean
  minPwdLen: number
  smsPwdForm: { smsCode: string; newPassword: string; confirmPassword: string }
  smsPwdRules: FormRules
  emailPwdForm: { emailCode: string; newPassword: string; confirmPassword?: string }
  emailPwdRules: FormRules
  maskedMobile: string
  maskedEmail: string
  smsCountdown: number
  sendingSmsCode: boolean
  savingSmsPwd: boolean
  emailResetCountdown: number
  sendingEmailResetCode: boolean
  savingEmailPwd: boolean
}>()

const pwdFormRef = ref<FormInstance>()
const smsPwdFormRef = ref<FormInstance>()
const emailPwdFormRef = ref<FormInstance>()
defineExpose({ pwdFormRef, smsPwdFormRef, emailPwdFormRef })

defineEmits<{
  openSmsResetMode: []
  closeSmsResetMode: []
  openEmailResetMode: []
  closeEmailResetMode: []
  handleChangePassword: []
  resetPwdForm: []
  handleSendResetSmsCode: []
  handleSmsResetPassword: []
  resetSmsPwdForm: []
  handleSendResetEmailCode: []
  handleEmailResetPassword: []
  resetEmailPwdForm: []
}>()
</script>

<style scoped>
.pwd-forgot-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pwd-forgot-btn--email {
  color: #6366f1;
}

.sms-reset-banner--email {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.08) 0%, rgba(79, 70, 229, 0.05) 100%);
  border-color: rgba(99, 102, 241, 0.2);
}
</style>
