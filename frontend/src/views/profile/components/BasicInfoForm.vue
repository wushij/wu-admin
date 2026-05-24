<template>
  <div class="tab-pane-fill">
    <div class="tab-intro"><el-icon><User /></el-icon><span>维护您的昵称与联系方式，便于同事识别与系统通知触达。</span></div>
    <el-form ref="formRef" :model="infoForm" :rules="infoRules" label-width="96px" class="info-form" @submit.prevent>
      <el-form-item label="用户名"><el-input :model-value="profile.username" disabled /></el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="infoForm.nickname" placeholder="请输入昵称" maxlength="30" show-word-limit />
      </el-form-item>

      <!-- 手机号展示与更换 -->
      <el-form-item v-if="hasBoundMobile && !mobileBindEditing" label="手机号" class="mobile-form-item">
        <div class="mobile-bound-row">
          <el-input :model-value="maskedMobile" disabled class="mobile-bound-input" />
          <el-button v-if="smsEnabled" plain class="mobile-change-btn" @click="$emit('openMobileBindEditing')">更换手机号</el-button>
        </div>
      </el-form-item>
      <template v-else-if="showMobileBindFields">
        <el-form-item label="手机号" prop="bindMobile">
          <el-input v-model="infoForm.bindMobile" placeholder="请输入手机号" maxlength="11" clearable />
        </el-form-item>
        <el-form-item label="短信验证码" prop="bindSmsCode">
          <div class="mobile-bind-action-row">
            <el-input v-model="infoForm.bindSmsCode" class="mobile-bind-sms-input" placeholder="请输入验证码" maxlength="6" autocomplete="off" />
            <el-button type="primary" plain class="sms-send-btn" :disabled="bindSmsCountdown > 0 || sendingBindSmsCode" :loading="sendingBindSmsCode" @click="$emit('handleSendBindSmsCode')">
              {{ bindSmsCountdown > 0 ? `${bindSmsCountdown}s` : '获取验证码' }}
            </el-button>
            <el-button type="primary" :loading="bindingMobile" @click="$emit('handleBindMobile')">{{ hasBoundMobile ? '确认更换' : '绑定手机号' }}</el-button>
            <el-button v-if="hasBoundMobile" @click="$emit('cancelMobileBindEditing')">取消</el-button>
          </div>
        </el-form-item>
      </template>
      <el-form-item v-else label="手机号" class="mobile-form-item">
        <span class="mobile-bind-hint">短信功能未启用，无法绑定手机号，请联系管理员</span>
      </el-form-item>

      <!-- 邮箱展示与绑定更换 -->
      <el-form-item v-if="hasBoundEmail && !emailBindEditing" label="邮箱" class="mobile-form-item">
        <div class="mobile-bound-row">
          <el-input :model-value="maskedEmail" disabled class="mobile-bound-input" />
          <el-button plain class="mobile-change-btn" @click="$emit('openEmailBindEditing')">更换邮箱</el-button>
        </div>
      </el-form-item>
      <template v-else>
        <el-form-item label="邮箱地址" prop="bindEmail">
          <el-input v-model="infoForm.bindEmail" placeholder="请输入新邮箱地址" maxlength="64" clearable />
        </el-form-item>
        <el-form-item label="邮箱验证码" prop="bindEmailCode">
          <div class="mobile-bind-action-row">
            <el-input v-model="infoForm.bindEmailCode" class="mobile-bind-sms-input" placeholder="请输入 6 位验证码" maxlength="6" autocomplete="off" />
            <el-button type="primary" plain class="sms-send-btn" :disabled="bindEmailCountdown > 0 || sendingBindEmailCode" :loading="sendingBindEmailCode" @click="$emit('handleSendBindEmailCode')">
              {{ bindEmailCountdown > 0 ? `${bindEmailCountdown}s` : '获取验证码' }}
            </el-button>
            <el-button type="primary" :loading="bindingEmail" @click="$emit('handleBindEmail')">{{ hasBoundEmail ? '确认更换' : '绑定邮箱' }}</el-button>
            <el-button v-if="hasBoundEmail" @click="$emit('cancelEmailBindEditing')">取消</el-button>
          </div>
        </el-form-item>
      </template>

      <el-form-item label="所属部门"><el-input :model-value="profile.deptName || '未分配'" disabled /></el-form-item>
      <el-form-item label="岗位"><el-input :model-value="postDisplay" disabled /></el-form-item>
      <div class="info-form-actions">
        <el-button type="primary" :loading="savingInfo" @click="$emit('handleSaveInfo')">保存资料</el-button>
        <el-button @click="$emit('resetInfoForm')">重置</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { User } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { UserProfile } from '@/types/profile'

defineProps<{
  profile: UserProfile
  infoForm: { nickname: string; email: string; bindMobile: string; bindSmsCode: string; bindEmail: string; bindEmailCode: string }
  infoRules: FormRules
  hasBoundMobile: boolean
  maskedMobile: string
  mobileBindEditing: boolean
  hasBoundEmail: boolean
  maskedEmail: string
  emailBindEditing: boolean
  smsEnabled: boolean
  showMobileBindFields: boolean
  bindSmsCountdown: number
  sendingBindSmsCode: boolean
  bindingMobile: boolean
  bindEmailCountdown: number
  sendingBindEmailCode: boolean
  bindingEmail: boolean
  savingInfo: boolean
  postDisplay: string
}>()

defineEmits<{
  openMobileBindEditing: []
  cancelMobileBindEditing: []
  handleSendBindSmsCode: []
  handleBindMobile: []
  openEmailBindEditing: []
  cancelEmailBindEditing: []
  handleSendBindEmailCode: []
  handleBindEmail: []
  handleSaveInfo: []
  resetInfoForm: []
}>()

const formRef = ref<FormInstance>()
defineExpose({ formRef })
</script>
