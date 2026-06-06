<template>
  <div>
    <div class="tab-intro tab-intro--security"><el-icon><Lock /></el-icon><span>定期更换密码可提升账号安全性，密码长度不少于 {{ minPwdLen }} 位。</span></div>

    <div v-if="securityMode === 'password'" class="security-form-card">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top" class="pwd-form pwd-form--modern" @submit.prevent>
        <el-form-item prop="oldPassword" class="pwd-field">
          <template #label>
            <div class="pwd-field-label-row">
              <span class="pwd-field-label">当前密码</span>
              <button v-if="canUseSmsReset" type="button" class="pwd-forgot-btn" @click="$emit('openSmsResetMode')">忘记密码</button>
              <span v-else-if="!hasBoundMobile" class="pwd-forgot-hint">重置密码需先绑定手机号</span>
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

    <div v-else class="security-form-card security-form-card--sms">
      <div class="sms-form-header">
        <button type="button" class="sms-back-btn" @click="$emit('closeSmsResetMode')"><el-icon><ArrowLeft /></el-icon>返回</button>
        <span class="sms-form-header__title">短信验证重置</span>
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
            <el-input v-model="smsPwdForm.smsCode" placeholder="请输入 6 位验证码" maxlength="6" autocomplete="off" />
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
  </div>
</template>

<script setup lang="ts">
import { Lock, ArrowLeft, Iphone } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

defineProps<{
  securityMode: 'password' | 'sms'
  pwdForm: { oldPassword: string; newPassword: string; confirmPassword: string }
  pwdRules: FormRules
  pwdFormRef: FormInstance | undefined
  savingPwd: boolean
  canUseSmsReset: boolean
  hasBoundMobile: boolean
  minPwdLen: number
  smsPwdForm: { smsCode: string; newPassword: string; confirmPassword: string }
  smsPwdRules: FormRules
  smsPwdFormRef: FormInstance | undefined
  maskedMobile: string
  smsCountdown: number
  sendingSmsCode: boolean
  savingSmsPwd: boolean
}>()

defineEmits<{
  openSmsResetMode: []
  closeSmsResetMode: []
  handleChangePassword: []
  resetPwdForm: []
  handleSendResetSmsCode: []
  handleSmsResetPassword: []
  resetSmsPwdForm: []
}>()
</script>
