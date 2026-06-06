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
                :profile="profile" :info-form="infoForm" :info-rules="infoRules"
                :info-form-ref="infoFormRef" :has-bound-mobile="hasBoundMobile"
                :masked-mobile="maskedMobile" :mobile-bind-editing="mobileBindEditing"
                :sms-enabled="smsEnabled" :show-mobile-bind-fields="showMobileBindFields"
                :bind-sms-countdown="bindSmsCountdown" :sending-bind-sms-code="sendingBindSmsCode"
                :binding-mobile="bindingMobile" :saving-info="savingInfo" :post-display="postDisplay"
                @open-mobile-bind-editing="openMobileBindEditing"
                @cancel-mobile-bind-editing="cancelMobileBindEditing"
                @handle-send-bind-sms-code="handleSendBindSmsCode"
                @handle-bind-mobile="handleBindMobile"
                @handle-save-info="handleSaveInfo"
                @reset-info-form="resetInfoForm"
              />
            </el-tab-pane>
            <el-tab-pane label="安全设置" name="security">
              <SecuritySettings
                :security-mode="securityMode" :pwd-form="pwdForm" :pwd-rules="pwdRules"
                :pwd-form-ref="pwdFormRef" :saving-pwd="savingPwd"
                :can-use-sms-reset="canUseSmsReset" :has-bound-mobile="hasBoundMobile"
                :min-pwd-len="minPwdLen" :sms-pwd-form="smsPwdForm" :sms-pwd-rules="smsPwdRules"
                :sms-pwd-form-ref="smsPwdFormRef" :masked-mobile="maskedMobile"
                :sms-countdown="smsCountdown" :sending-sms-code="sendingSmsCode" :saving-sms-pwd="savingSmsPwd"
                @open-sms-reset-mode="openSmsResetModeWrapper"
                @close-sms-reset-mode="closeSmsResetMode"
                @handle-change-password="handleChangePassword"
                @reset-pwd-form="resetPwdForm"
                @handle-send-reset-sms-code="handleSendResetSmsCode"
                @handle-sms-reset-password="handleSmsResetPassword"
                @reset-sms-pwd-form="resetSmsPwdForm"
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
    <SliderCaptcha v-model:show="showSliderModal" @success="onSliderSuccess" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { getMyLoginLogs } from '@/api/system/profile'
import { getConfig } from '@/api/system/auth'
import SliderCaptcha from '@/components/SliderCaptcha.vue'
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

const {
  profile, pageLoading, savingInfo, avatarInputRef, infoFormRef,
  mobileBindEditing, infoForm,
  avatarSrc, avatarFallback, statusLabel, statusTagType,
  postDisplay, lastLoginDisplay, maskedMobile, hasBoundMobile,
  formatTime, loadProfile, resetInfoForm, handleSaveInfo,
  triggerAvatarUpload, handleAvatarChange,
  openMobileBindEditing, cancelMobileBindEditing,
} = useProfileInfo()

const minPwdLen = computed(() => profile.value.minPasswordLength ?? 6)

const showMobileBindFields = computed(
  () => smsEnabled.value && (!hasBoundMobile.value || mobileBindEditing.value),
)

const {
  savingPwd, pwdFormRef, smsPwdFormRef, securityMode, showSliderModal,
  sendingSmsCode, sendingBindSmsCode, savingSmsPwd, bindingMobile,
  smsCountdown, bindSmsCountdown,
  pwdForm, smsPwdForm, canUseSmsReset,
  pwdRules, smsPwdRules, infoRules,
  openSmsResetMode, closeSmsResetMode, resetSmsPwdForm, resetPwdForm,
  handleSendResetSmsCode, handleSendBindSmsCode, onSliderSuccess,
  handleBindMobile, handleChangePassword, handleSmsResetPassword,
} = useProfileSecurity(
  () => minPwdLen.value,
  () => smsEnabled.value,
  () => hasBoundMobile.value,
  () => maskedMobile.value,
  () => (profile.value.mobile || '').trim(),
  loadProfile,
  () => infoFormRef.value,
  () => infoForm,
)

function openSmsResetModeWrapper() {
  openSmsResetMode(
    (tab) => { activeTab.value = tab },
    () => { mobileBindEditing.value = true },
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

<style scoped>
.profile-page { min-height: 100%; }
.profile-hero { position: relative; border-radius: 14px; overflow: hidden; margin-bottom: 20px; background: linear-gradient(135deg, var(--theme-primary, #111827) 0%, #374151 100%); color: #fff; }
.hero-bg-pattern { position: absolute; inset: 0; opacity: 0.08; background-image: radial-gradient(circle at 20% 50%, #fff 1px, transparent 1px), radial-gradient(circle at 80% 20%, #fff 1px, transparent 1px); background-size: 40px 40px; }
.hero-content { position: relative; display: flex; align-items: center; justify-content: space-between; gap: 24px; padding: 28px 32px; flex-wrap: wrap; }
.hero-left { display: flex; align-items: center; gap: 24px; }
.avatar-uploader { position: relative; cursor: pointer; flex-shrink: 0; }
.hero-avatar { width: 80px !important; height: 80px !important; border: 3px solid rgba(255, 255, 255, 0.35); background: rgba(255, 255, 255, 0.15); font-size: 28px; font-weight: 600; }
.avatar-mask { position: absolute; inset: 0; border-radius: 50%; background: rgba(0, 0, 0, 0.45); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; opacity: 0; transition: opacity 0.25s; font-size: 11px; color: #fff; }
.avatar-uploader:hover .avatar-mask { opacity: 1; }
.avatar-input { position: absolute; width: 0; height: 0; opacity: 0; pointer-events: none; }
.hero-info { display: flex; flex-direction: column; gap: 6px; }
.hero-name { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 0.3px; }
.hero-username { margin: 0; font-size: 14px; color: rgba(255, 255, 255, 0.65); }
.hero-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 4px; }
.role-tag { border: none; }
.hero-stats { display: flex; align-items: center; gap: 28px; }
.stat-item { text-align: center; }
.stat-value { font-size: 16px; font-weight: 600; }
.stat-label { font-size: 12px; color: rgba(255, 255, 255, 0.6); margin-top: 2px; }
.stat-divider { width: 1px; height: 32px; background: rgba(255, 255, 255, 0.2); }
.profile-main { margin-top: 0; }
.profile-col-left { min-width: 0; }
.content-card { border-radius: 12px; }
.profile-tabs :deep(.el-tabs__header) { margin-bottom: 0; }
.profile-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.profile-tabs :deep(.el-tabs__content) { padding: 20px 8px 8px; }
.tab-intro { display: flex; align-items: center; gap: 8px; color: #909399; font-size: 13px; margin-bottom: 20px; padding: 10px 14px; background: #f9fafb; border-radius: 8px; }
.tab-intro--security { background: #fefce8; color: #92400e; }
.info-form { max-width: 520px; }
.mobile-bound-row { display: flex; align-items: center; gap: 8px; width: 100%; }
.mobile-bound-input { flex: 1; }
.mobile-change-btn { flex-shrink: 0; }
.mobile-bind-action-row { display: flex; align-items: center; gap: 8px; width: 100%; }
.mobile-bind-sms-input { width: 140px; }
.sms-send-btn { flex-shrink: 0; }
.mobile-bind-hint { color: #909399; font-size: 13px; }
.security-form-card { max-width: 440px; padding: 20px; background: #f9fafb; border-radius: 12px; }
.security-form-card--sms { background: #fefce8; }
.pwd-form--modern :deep(.el-form-item) { margin-bottom: 18px; }
.pwd-field-label-row { display: flex; align-items: center; gap: 12px; width: 100%; }
.pwd-field-label { font-weight: 500; }
.pwd-forgot-btn { background: none; border: none; color: var(--el-color-primary); cursor: pointer; font-size: 13px; padding: 0; }
.pwd-forgot-btn:hover { text-decoration: underline; }
.pwd-forgot-hint { font-size: 12px; color: #909399; }
.pwd-form-actions { display: flex; gap: 8px; margin-top: 8px; }
.sms-back-btn { display: flex; align-items: center; gap: 4px; background: none; border: none; color: #606266; cursor: pointer; font-size: 14px; padding: 0; margin-bottom: 12px; }
.sms-back-btn:hover { color: var(--el-color-primary); }
.sms-form-header__title { font-size: 16px; font-weight: 600; color: #303133; }
.sms-reset-banner { display: flex; align-items: center; gap: 14px; padding: 14px 16px; background: #fff; border-radius: 10px; margin-bottom: 20px; border: 1px solid #fde68a; }
.sms-reset-banner__icon { font-size: 28px; color: #f59e0b; }
.sms-reset-banner__label { margin: 0; font-size: 12px; color: #909399; }
.sms-reset-banner__phone { margin: 2px 0 0; font-size: 18px; font-weight: 600; color: #303133; letter-spacing: 1px; }
.sms-code-row { display: flex; gap: 8px; width: 100%; }
.logs-table { border-radius: 8px; overflow: hidden; }
.logs-pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.profile-col-right { display: flex; flex-direction: column; gap: 16px; }
.side-card { border-radius: 12px; }
.side-card-header { display: flex; align-items: center; gap: 8px; font-weight: 600; }
.meta-list { list-style: none; margin: 0; padding: 0; }
.meta-list li { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f0f0f0; font-size: 13px; }
.meta-list li:last-child { border-bottom: none; }
.meta-label { color: #909399; }
.meta-value { color: #303133; font-weight: 500; }
.tips-list { list-style: none; margin: 0; padding: 0; }
.tips-list li { position: relative; padding: 6px 0 6px 16px; font-size: 13px; color: #606266; line-height: 1.6; }
.tips-list li::before { content: ''; position: absolute; left: 0; top: 13px; width: 6px; height: 6px; border-radius: 50%; background: #e6a23c; }
@media (max-width: 1200px) { .hero-stats { gap: 16px; } .stat-value { font-size: 14px; } }
@media (max-width: 768px) {
  .hero-content { flex-direction: column; text-align: center; padding: 20px 16px; }
  .hero-left { flex-direction: column; }
  .hero-stats { flex-wrap: wrap; justify-content: center; }
  .profile-col-right { margin-top: 16px; }
}
</style>
