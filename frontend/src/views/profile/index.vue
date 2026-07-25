<template>
  <div class="profile-page" v-loading="pageLoading">
    <ProfileHero
      :profile="profile" :avatar-src="avatarSrc" :avatar-fallback="avatarFallback"
      :status-label="statusLabel" :status-tag-type="statusTagType"
      :post-display="postDisplay" :last-login-display="lastLoginDisplay"
      @trigger-avatar="triggerAvatarUpload"
    />

    <el-row :gutter="24" class="profile-main">
      <el-col :xs="24" :lg="18" class="profile-col-left">
        <el-card class="content-card" shadow="never">
          <el-tabs v-model="activeTab" class="profile-tabs">
            <el-tab-pane label="基本资料" name="info">
              <BasicInfoForm
                ref="basicInfoFormRef"
                :profile="profile" :info-form="infoForm" :info-rules="infoRules"
                :has-bound-mobile="hasBoundMobile"
                :masked-mobile="maskedMobile" :mobile-bind-editing="mobileBindEditing"
                :has-bound-email="hasBoundEmail"
                :masked-email="maskedEmail" :email-bind-editing="emailBindEditing"
                :sms-enabled="smsEnabled" :show-mobile-bind-fields="showMobileBindFields"
                :bind-sms-countdown="bindSmsCountdown" :sending-bind-sms-code="sendingBindSmsCode"
                :binding-mobile="bindingMobile"
                :bind-email-countdown="bindEmailCountdown" :sending-bind-email-code="sendingBindEmailCode"
                :binding-email="bindingEmail"
                :saving-info="savingInfo" :post-display="postDisplay"
                @open-mobile-bind-editing="openMobileBindEditing"
                @cancel-mobile-bind-editing="cancelMobileBindEditing"
                @handle-send-bind-sms-code="handleSendBindSmsCode"
                @handle-bind-mobile="handleBindMobileWrapper"
                @open-email-bind-editing="openEmailBindEditing"
                @cancel-email-bind-editing="cancelEmailBindEditing"
                @handle-send-bind-email-code="handleSendBindEmailCode"
                @handle-bind-email="handleBindEmailWrapper"
                @handle-save-info="handleSaveInfo"
                @reset-info-form="resetInfoForm"
              />
            </el-tab-pane>
            <el-tab-pane label="安全设置" name="security">
              <SecuritySettings
                ref="securitySettingsRef"
                :security-mode="securityMode" :pwd-form="pwdForm" :pwd-rules="pwdRules"
                :saving-pwd="savingPwd"
                :can-use-sms-reset="canUseSmsReset" :has-bound-mobile="hasBoundMobile"
                :can-use-email-reset="canUseEmailReset" :has-bound-email="hasBoundEmail"
                :min-pwd-len="minPwdLen" :sms-pwd-form="smsPwdForm" :sms-pwd-rules="smsPwdRules"
                :email-pwd-form="emailPwdForm" :email-pwd-rules="emailPwdRules"
                :masked-mobile="maskedMobile" :masked-email="maskedEmail"
                :sms-countdown="smsCountdown" :sending-sms-code="sendingSmsCode" :saving-sms-pwd="savingSmsPwd"
                :email-reset-countdown="emailResetCountdown" :sending-email-reset-code="sendingEmailResetCode" :saving-email-pwd="savingEmailPwd"
                @open-sms-reset-mode="openSmsResetModeWrapper"
                @close-sms-reset-mode="closeSmsResetMode"
                @open-email-reset-mode="openEmailResetModeWrapper"
                @close-email-reset-mode="closeEmailResetMode"
                @handle-change-password="handleChangePassword"
                @reset-pwd-form="resetPwdForm"
                @handle-send-reset-sms-code="handleSendResetSmsCode"
                @handle-sms-reset-password="handleSmsResetPassword"
                @reset-sms-pwd-form="resetSmsPwdForm"
                @handle-send-reset-email-code="handleSendResetEmailCode"
                @handle-email-reset-password="handleEmailResetPassword"
                @reset-email-pwd-form="resetEmailPwdForm"
              />
            </el-tab-pane>
            <el-tab-pane label="登录记录" name="logs">
              <LoginLogsTab :login-logs="loginLogs" :loading="logsLoading" :total="logsTotal" :query="logsQuery" @load-logs="loadLoginLogs" />
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="6" class="profile-col-right">
        <AccountSidebar :profile="profile" :format-time="formatTime" />
      </el-col>
    </el-row>

    <input ref="avatarInputRef" type="file" accept="image/jpeg,image/png,image/gif,image/webp" class="avatar-input" @change="handleAvatarChange" />
    <SliderCaptcha v-model:show="showSliderModal" scene="profile" @success="onSliderSuccess" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { getMyLoginLogs } from '@/api/system/profile'
import { getConfig } from '@/api/system/auth'
import SliderCaptcha from '@/components/SliderCaptcha.vue'
import type { FormInstance } from 'element-plus'
import type { LoginLogVO } from '@/api/system/login-log'
import ProfileHero from './components/ProfileHero.vue'
import BasicInfoForm from './components/BasicInfoForm.vue'
import SecuritySettings from './components/SecuritySettings.vue'
import LoginLogsTab from './components/LoginLogsTab.vue'
import AccountSidebar from './components/AccountSidebar.vue'
import { useProfileInfo } from './composables/useProfileInfo'
import { useProfileSecurity } from './composables/useProfileSecurity'

const activeTab = ref('info')
const smsEnabled = ref(false)
const securitySettingsRef = ref<{ pwdFormRef?: FormInstance; smsPwdFormRef?: FormInstance; emailPwdFormRef?: FormInstance }>()

const {
  profile, pageLoading, savingInfo, avatarInputRef, basicInfoFormRef, getInfoFormRef,
  mobileBindEditing, emailBindEditing, infoForm,
  avatarSrc, avatarFallback, statusLabel, statusTagType,
  postDisplay, lastLoginDisplay, maskedMobile, hasBoundMobile,
  maskedEmail, hasBoundEmail,
  formatTime, loadProfile, resetInfoForm, handleSaveInfo,
  triggerAvatarUpload, handleAvatarChange,
  openMobileBindEditing, cancelMobileBindEditing,
  openEmailBindEditing, cancelEmailBindEditing,
} = useProfileInfo()

const minPwdLen = computed(() => profile.value.minPasswordLength ?? 6)

const showMobileBindFields = computed(
  () => smsEnabled.value && (!hasBoundMobile.value || mobileBindEditing.value),
)

const {
  savingPwd, securityMode, showSliderModal,
  sendingSmsCode, sendingBindSmsCode, sendingBindEmailCode, sendingEmailResetCode, savingSmsPwd, savingEmailPwd, bindingMobile, bindingEmail,
  smsCountdown, emailResetCountdown, bindSmsCountdown, bindEmailCountdown,
  pwdForm, smsPwdForm, emailPwdForm, canUseSmsReset, canUseEmailReset,
  pwdRules, smsPwdRules, emailPwdRules, infoRules,
  openSmsResetMode, closeSmsResetMode, openEmailResetMode, closeEmailResetMode, resetSmsPwdForm, resetEmailPwdForm, resetPwdForm,
  handleSendResetSmsCode, handleSendResetEmailCode, handleSendBindSmsCode, handleSendBindEmailCode, onSliderSuccess,
  handleBindMobile, handleBindEmail, handleChangePassword, handleSmsResetPassword, handleEmailResetPassword,
} = useProfileSecurity(
  () => minPwdLen.value,
  () => smsEnabled.value,
  () => hasBoundMobile.value,
  () => maskedMobile.value,
  () => (profile.value.mobile || '').trim(),
  () => hasBoundEmail.value,
  () => maskedEmail.value,
  loadProfile,
  getInfoFormRef,
  () => infoForm,
  () => securitySettingsRef.value?.pwdFormRef,
  () => securitySettingsRef.value?.smsPwdFormRef,
  () => securitySettingsRef.value?.emailPwdFormRef,
)

function handleBindMobileWrapper() {
  handleBindMobile(() => {
    mobileBindEditing.value = false
  })
}

function handleBindEmailWrapper() {
  handleBindEmail(() => {
    emailBindEditing.value = false
  })
}

function openSmsResetModeWrapper() {
  openSmsResetMode(
    (tab) => { activeTab.value = tab },
    () => { mobileBindEditing.value = true },
  )
}

function openEmailResetModeWrapper() {
  openEmailResetMode(
    (tab) => { activeTab.value = tab },
    () => { emailBindEditing.value = true },
  )
}

const logsLoading = ref(false)
const loginLogs = ref<LoginLogVO[]>([])
const logsTotal = ref(0)
const logsQuery = reactive({ pageNo: 1, pageSize: 8 })

async function loadLoginLogs() {
  logsLoading.value = true
  try {
    const res = await getMyLoginLogs(logsQuery)
    loginLogs.value = res.data?.list || []
    logsTotal.value = res.data?.total || 0
  } finally { logsLoading.value = false }
}

async function loadSmsConfig() {
  try {
    const res = await getConfig()
    smsEnabled.value = res.data?.login?.smsEnabled === true
  } catch { smsEnabled.value = false }
}

watch(activeTab, (tab) => { if (tab === 'logs') loadLoginLogs() })

onMounted(async () => {
  await Promise.all([loadProfile(), loadSmsConfig()])
  if (activeTab.value === 'logs') await loadLoginLogs()
})
</script>

<style lang="scss">
@import './profile-page.scss';
</style>
