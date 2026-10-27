<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded config-page">
    <ModuleDarkHero title="系统配置" subtitle="修改后请点击「保存全部」生效" icon="setting-o" theme="config" />
    <SegmentTabs v-model="tab" :tabs="tabs" scroll />

    <ListLoading v-if="loading" />

    <scroll-view v-else scroll-y class="config-page__scroll">
      <!-- 站点 -->
      <view v-if="tab === 'site'" class="form-panel">
        <FormCell v-model="siteDraft.platformName" label="平台名称" editable boxed :disabled="!canEdit" />
        <FormCell v-model="siteDraft.platformSubtitle" label="平台副标题" editable boxed :disabled="!canEdit" />
        <FormCell v-model="siteDraft.loginWelcome" label="登录页标题" editable boxed :disabled="!canEdit" />
        <FormCell v-model="siteDraft.registerTitle" label="注册页标题" editable boxed :disabled="!canEdit" />
        <FormCell v-model="siteDraft.copyright" label="页脚版权" editable boxed placeholder="选填" :disabled="!canEdit" />
        <FormCell label="ICP备案展示" :last="!siteDraft.icpEnabled">
          <switch :checked="siteDraft.icpEnabled" :disabled="!canEdit" color="#6366f1" @change="siteDraft.icpEnabled = $event.detail.value" />
        </FormCell>
        <template v-if="siteDraft.icpEnabled">
          <FormCell v-model="siteDraft.icpNumber" label="ICP备案号" editable boxed placeholder="粤ICP备XXXXXXXX号-1" :disabled="!canEdit" />
          <FormCell v-model="siteDraft.icpUrl" label="工信部链接" editable boxed placeholder="https://beian.miit.gov.cn" :disabled="!canEdit" last />
        </template>
      </view>

      <!-- 会话 -->
      <view v-else-if="tab === 'session'" class="form-panel form-panel--session">
        <FormCell label="Token 有效期">
          <NumberStepper v-model="sessionDraft.tokenExpireHours" :min="1" :max="720" :disabled="!canEdit" />
          <text class="config-unit">小时</text>
        </FormCell>
        <FormCell label="签名密钥有效期" last>
          <NumberStepper v-model="sessionDraft.sessionSignExpireHours" :min="1" :max="120" :disabled="!canEdit" />
          <text class="config-unit">小时</text>
        </FormCell>
      </view>
      <view v-if="tab === 'session'" class="session-alert-box">
        <text class="session-alert-icon">ℹ</text>
        <text class="session-alert-text">建议：将签名密钥有效期与 Token 有效期设为相同的时长（例如 24 小时）。</text>
      </view>

      <!-- 文件 -->
      <FileConfigPanel
        v-else-if="tab === 'file'"
        :file-draft="fileDraft"
        :can-edit="canEdit"
        :storage-form="storageForm"
        :storage-engine="storageEngine"
        :storage-active-type="storageActiveType"
        :storage-healthy="storageHealthy"
        :storage-saving="storageSaving"
        :storage-testing="storageTesting"
        :storage-engine-options="storageEngineOptions"
        :storage-active-label="storageActiveLabel"
        :storage-selected-label="storageSelectedLabel"
        :platform-max-file-mb="platformMaxFileMb"
        :select-storage-engine="selectStorageEngine"
        :test-storage-connection="testStorageConnection"
        :save-storage-config="saveStorageConfig"
      />

      <!-- 限流 -->
      <RateLimitConfigPanel
        v-else-if="tab === 'rateLimit'"
        :rate-draft="rateDraft"
        :can-edit="canEdit"
      />

      <!-- 登录 -->
      <LoginConfigPanel
        v-else-if="tab === 'login'"
        :login-draft="loginDraft"
        :sms-draft="smsDraft"
        :email-draft="emailDraft"
        :can-edit="canEdit"
        :captcha-type-options="captchaTypeOptions"
        :on-login-switch="onLoginSwitch"
      />

      <!-- 注册 -->
      <RegisterConfigPanel
        v-else-if="tab === 'register'"
        :register-draft="registerDraft"
        :can-edit="canEdit"
        :captcha-type-options="captchaTypeOptions"
        :default-role-label="defaultRoleLabel"
        :register-auditor-label="registerAuditorLabel"
        :on-register-switch="onRegisterSwitch"
        :pick-default-role="pickDefaultRole"
        :pick-register-auditors="pickRegisterAuditors"
      />

      <!-- 第三方 -->
      <ThirdPartyConfigPanel
        v-else-if="tab === 'thirdParty'"
        :third-draft="thirdDraft"
        :can-edit="canEdit"
        :on-third-switch="onThirdSwitch"
      />

      <!-- 支付 -->
      <PaymentConfigPanel
        v-else-if="tab === 'payment'"
        :payment-draft="paymentDraft"
        :can-edit="canEdit"
        :payment-testing="paymentTesting"
        :alipay-gateway-label="alipayGatewayLabel"
        :on-pay-switch="onPaySwitch"
        :pick-alipay-sign="pickAlipaySign"
        :pick-alipay-gateway="pickAlipayGateway"
        :send-test-payment="sendTestPayment"
      />

      <!-- 短信 -->
      <SmsConfigPanel
        v-else-if="tab === 'sms'"
        :sms-draft="smsDraft"
        :can-edit="canEdit"
        :sms-testing="smsTesting"
        :provider-label="providerLabel"
        :test-template-label="testTemplateLabel"
        v-model:test-sms-phone="testSmsPhone"
        :recent-sms-logs="recentSmsLogs"
        :on-sms-switch="onSmsSwitch"
        :pick-provider="pickProvider"
        :pick-test-template="pickTestTemplate"
        :send-test-sms="sendTestSms"
        :open-sms-logs="openSmsLogs"
        :sms-status-text="smsStatusText"
      />

      <!-- 邮件 -->
      <EmailConfigPanel
        v-else-if="tab === 'email'"
        :email-draft="emailDraft"
        :can-edit="canEdit"
        :email-testing="emailTesting"
        v-model:test-email-to="testEmailTo"
        :email-provider-label="emailProviderLabel"
        :email-security-label="emailSecurityLabel"
        :email-encoding-label="emailEncodingLabel"
        :recent-email-logs="recentEmailLogs"
        :pick-email-provider="pickEmailProvider"
        :pick-email-security-type="pickEmailSecurityType"
        :pick-email-encoding="pickEmailEncoding"
        :send-test-email-action="sendTestEmailAction"
        :open-email-logs="openEmailLogs"
        :email-status-text="emailStatusText"
      />

      <!-- 安全 -->
      <SecurityConfigPanel
        v-else-if="tab === 'security'"
        :security-draft="securityDraft"
        :can-edit="canEdit"
        :forbid-concurrent-login="forbidConcurrentLogin"
        :on-security-switch="onSecuritySwitch"
        :on-forbid-concurrent-change="onForbidConcurrentChange"
      />

      <!-- AI 助手 -->
      <AiConfigPanel
        v-else-if="tab === 'ai'"
        :ai-draft="aiDraft"
        :can-edit="canEdit"
        :answer-scope-options="answerScopeOptions"
        :quota-role-label="quotaRoleLabel"
        :pick-quota-role="pickQuotaRole"
        :add-quota-row="addQuotaRow"
        :remove-quota-row="removeQuotaRow"
      />
    </scroll-view>

    <view v-if="canEdit && !loading" class="config-page-footer">
      <view v-if="isDirty" class="config-dirty config-dirty--footer">
        <text>有未保存的修改</text>
      </view>
      <view class="config-page-footer__actions">
        <button class="page-footer__btn page-footer__btn--ghost" @click="resetAll">重置</button>
        <button class="page-footer__btn" :loading="saving" @click="saveAll">保存全部</button>
      </view>
    </view>

    <!-- 支付测试弹窗 -->
    <view v-if="showPaymentModal" class="pay-modal" @click="closePaymentModal">
      <view class="pay-modal__card" @click.stop>
        <text class="pay-modal__title">测试支付</text>
        <text class="pay-modal__row">方式：{{ paymentResult.type === 'wechat' ? '微信支付' : '支付宝' }}</text>
        <text class="pay-modal__row">订单：{{ paymentResult.orderNo }}</text>
        <text class="pay-modal__row">金额：¥ 0.01</text>
        <text class="pay-modal__row">状态：{{ payOrderStatus === 'PAID' ? '已支付' : '待支付' }}</text>
        <image v-if="paymentResult.qrcode" class="pay-modal__qr" :src="paymentResult.qrcode" mode="aspectFit" />
        <button class="config-test-btn" :loading="payStatusRefreshing" @click="pollPayOrderStatus(true)">刷新支付状态</button>
        <button class="pay-modal__close" @click="closePaymentModal">关闭</button>
      </view>
    </view>

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/layout/ModuleDarkHero/index.vue'
import SegmentTabs from '@/components/base/SegmentTabs/index.vue'
import ListLoading from '@/components/base/ListLoading/index.vue'
import FormCell from '@/components/base/FormCell/index.vue'
import PermissionBlock from '@/components/base/PermissionBlock/index.vue'
import NumberStepper from '@/components/base/NumberStepper/index.vue'
import AppDialogHost from '@/components/base/AppDialogHost/index.vue'
import FileConfigPanel from './components/FileConfigPanel.vue'
import RateLimitConfigPanel from './components/RateLimitConfigPanel.vue'
import LoginConfigPanel from './components/LoginConfigPanel.vue'
import RegisterConfigPanel from './components/RegisterConfigPanel.vue'
import ThirdPartyConfigPanel from './components/ThirdPartyConfigPanel.vue'
import PaymentConfigPanel from './components/PaymentConfigPanel.vue'
import SmsConfigPanel from './components/SmsConfigPanel.vue'
import EmailConfigPanel from './components/EmailConfigPanel.vue'
import SecurityConfigPanel from './components/SecurityConfigPanel.vue'
import AiConfigPanel from './components/AiConfigPanel.vue'
import { useConfigEditor } from '@/composables/system/useConfigEditor'
import { useUnsavedLeaveGuard } from '@/composables/base/useUnsavedLeaveGuard'
import { useModulePermission } from '@/composables/base/useModulePermission'
import type { EmailAdminConfig, StorageEngineType } from '@/types/config-types'
import { showActionSheet, showConfirm } from '@/utils/app-dialog'
import { appendNavFromParam } from '@/utils/nav/nav-from'
import { consumePagePickerResult, clearPagePickerResult } from '@/utils/nav/page-picker-result'
import { setUserSelectMultiIds } from '@/utils/tree/user-select-multi-init'

const { allowed, hasPerm } = useModulePermission('system:config:list')
const canEdit = computed(() => hasPerm('system:config:update'))

const tab = ref('site')
const tabs = [
  { key: 'site', label: '基础' },
  { key: 'session', label: '会话' },
  { key: 'file', label: '文件' },
  { key: 'rateLimit', label: '限流' },
  { key: 'login', label: '登录' },
  { key: 'register', label: '注册' },
  { key: 'thirdParty', label: '第三方' },
  { key: 'payment', label: '支付' },
  { key: 'sms', label: '短信' },
  { key: 'email', label: '邮件' },
  { key: 'security', label: '安全' },
  { key: 'ai', label: 'AI' },
]

const {
  loading, saving, smsTesting, emailTesting, testEmailTo, paymentTesting, platformMaxFileMb, isDirty, forbidConcurrentLogin,
  siteDraft, sessionDraft, securityDraft, loginDraft, registerDraft, smsDraft, emailDraft,
  fileDraft, rateDraft, thirdDraft, paymentDraft, aiDraft, savedSnapshot,
  storageForm, storageEngine, storageActiveType, storageHealthy, storageSaving, storageTesting,
  testStorageConnection, saveStorageConfig,
  roleOptions, userOptions, testSmsPhone, testSmsTemplate, recentSmsLogs, recentEmailLogs,
  showPaymentModal, payOrderStatus, payStatusRefreshing, paymentResult,
  captchaTypeOptions, providerOptions, smsTemplateOptions, alipaySignOptions, alipayGatewayOptions,
  smsStatusText, emailStatusText, load, saveAll, resetAll, loadRecentSmsLogs, openSmsLogs, loadRecentEmailLogs, openEmailLogs,
  sendTestSms, sendTestEmailAction, sendTestPayment, pollPayOrderStatus, closePaymentModal,
} = useConfigEditor()

const storageEngineOptions: { type: StorageEngineType; name: string; desc: string }[] = [
  { type: 'local', name: '本地磁盘', desc: '服务端本地目录，单机部署首选' },
  { type: 'minio', name: 'MinIO', desc: '私有化 S3 兼容对象存储' },
  { type: 'cos', name: '腾讯云 COS', desc: '腾讯云高可用对象存储' },
  { type: 'oss', name: '阿里云 OSS', desc: '阿里云企业级对象存储' },
]

function storageEngineLabel(type: StorageEngineType): string {
  return storageEngineOptions.find((item) => item.type === type)?.name || type
}

const storageSelectedLabel = computed(() => storageEngineLabel(storageEngine.value))
const storageActiveLabel = computed(() => storageEngineLabel(storageActiveType.value))

function selectStorageEngine(type: StorageEngineType) {
  storageEngine.value = type
}

useUnsavedLeaveGuard()

watch(tab, (t) => {
  if (t === 'sms') loadRecentSmsLogs()
  if (t === 'email') loadRecentEmailLogs()
})

const defaultRoleLabel = computed(() => {
  const role = roleOptions.value.find((r) => r.code === registerDraft.defaultRoleCode)
  return role ? `${role.name}（${role.code}）` : registerDraft.defaultRoleCode
})
const registerAuditorLabel = computed(() => {
  const ids = registerDraft.auditorUserIds || []
  if (!ids.length) return '默认超级管理员'
  const labels = ids.map((id) => {
    const user = userOptions.value.find((u) => Number(u.id) === Number(id))
    if (!user) return String(id)
    const name = user.nickname || user.username
    return user.deptName ? `${name}（${user.deptName}）` : name
  })
  if (labels.length <= 2) return labels.join('、')
  return `${labels.slice(0, 2).join('、')} 等${labels.length}人`
})
const providerLabel = computed(() =>
  providerOptions.find((o) => o.value === smsDraft.provider)?.label || smsDraft.provider,
)
const testTemplateLabel = computed(() =>
  smsTemplateOptions.find((o) => o.value === testSmsTemplate.value)?.label || testSmsTemplate.value,
)
const alipayGatewayLabel = computed(() =>
  alipayGatewayOptions.find((o) => o.value === paymentDraft.alipay.gatewayUrl)?.label || '自定义',
)

const emailProviderOptions = [
  { label: 'QQ 邮箱 (smtp.qq.com)', value: 'qq' },
  { label: '网易 163 邮箱 (smtp.163.com)', value: '163' },
  { label: 'Google Gmail (smtp.gmail.com)', value: 'gmail' },
  { label: '自定义 SMTP 服务器', value: 'custom' },
]
const emailSecurityOptions = [
  { label: 'SSL (推荐 465)', value: 'SSL' },
  { label: 'TLS (587)', value: 'TLS' },
  { label: 'STARTTLS', value: 'STARTTLS' },
  { label: '无加密 (25)', value: 'NONE' },
]
const emailEncodingOptions = [
  { label: 'UTF-8 (推荐通用编码)', value: 'UTF-8' },
  { label: 'GBK (简体中文扩展)', value: 'GBK' },
  { label: 'GB2312 (国标简体)', value: 'GB2312' },
  { label: 'ISO-8859-1 (西欧编码)', value: 'ISO-8859-1' },
]

const emailProviderLabel = computed(() =>
  emailProviderOptions.find((o) => o.value === emailDraft.provider)?.label || emailDraft.provider || '自定义',
)
const emailSecurityLabel = computed(() =>
  emailSecurityOptions.find((o) => o.value === emailDraft.securityType)?.label || emailDraft.securityType || 'SSL',
)
const emailEncodingLabel = computed(() =>
  emailEncodingOptions.find((o) => o.value === emailDraft.encoding)?.label || emailDraft.encoding || 'UTF-8',
)

function onLoginSwitch(key: keyof typeof loginDraft, e: { detail: { value: boolean } }) {
  ;(loginDraft as Record<string, unknown>)[key] = e.detail.value
}
function onRegisterSwitch(key: keyof typeof registerDraft, e: { detail: { value: boolean } }) {
  ;(registerDraft as Record<string, unknown>)[key] = e.detail.value
}
function onSmsSwitch(key: keyof typeof smsDraft, e: { detail: { value: boolean } }) {
  ;(smsDraft as Record<string, unknown>)[key] = e.detail.value
}
function onSecuritySwitch(key: keyof typeof securityDraft, e: { detail: { value: boolean } }) {
  ;(securityDraft as Record<string, unknown>)[key] = e.detail.value
}
function onForbidConcurrentChange(e: { detail: { value: boolean } }) {
  forbidConcurrentLogin.value = e.detail.value
}
function onThirdSwitch(provider: 'wechat' | 'alipay' | 'github' | 'google', key: string, e: { detail: { value: boolean } }) {
  ;(thirdDraft[provider] as Record<string, unknown>)[key] = e.detail.value
}
function onPaySwitch(provider: 'wechatPay' | 'alipay', key: string, e: { detail: { value: boolean } }) {
  ;(paymentDraft[provider] as Record<string, unknown>)[key] = e.detail.value
}

async function pickFromOptions(itemList: string[], onPick: (index: number) => void) {
  try {
    const index = await showActionSheet({ items: itemList.map((label) => ({ label })) })
    onPick(index)
  } catch {
    // 用户取消
  }
}

function pickEmailProvider() {
  if (!canEdit.value) return
  pickFromOptions(emailProviderOptions.map((o) => o.label), (i) => {
    const item = emailProviderOptions[i]
    if (item) {
      emailDraft.provider = item.value as 'qq' | '163' | 'gmail' | 'custom'
      const saved = savedSnapshot.email as EmailAdminConfig | undefined
      // 如果切回当前数据库已保存绑定的服务商，恢复原保存的账号与授权码
      if (saved && item.value && item.value === saved.provider) {
        emailDraft.host = saved.host || 'smtp.qq.com'
        emailDraft.port = saved.port || 465
        emailDraft.securityType = saved.securityType || 'SSL'
        emailDraft.username = saved.username || ''
        emailDraft.password = saved.password || ''
        return
      }

      // 否则切到未绑定的新服务商，填充预设网络参数并清空账号与密码
      if (item.value === 'qq') {
        emailDraft.host = 'smtp.qq.com'
        emailDraft.port = 465
        emailDraft.securityType = 'SSL'
      } else if (item.value === '163') {
        emailDraft.host = 'smtp.163.com'
        emailDraft.port = 465
        emailDraft.securityType = 'SSL'
      } else if (item.value === 'gmail') {
        emailDraft.host = 'smtp.gmail.com'
        emailDraft.port = 587
        emailDraft.securityType = 'TLS'
      }

      emailDraft.username = ''
      emailDraft.password = ''
    }
  })
}

function pickEmailSecurityType() {
  if (!canEdit.value) return
  pickFromOptions(emailSecurityOptions.map((o) => o.label), (i) => {
    const item = emailSecurityOptions[i]
    if (item) emailDraft.securityType = item.value as any
  })
}

function pickEmailEncoding() {
  if (!canEdit.value) return
  pickFromOptions(emailEncodingOptions.map((o) => o.label), (i) => {
    const item = emailEncodingOptions[i]
    if (item) emailDraft.encoding = item.value
  })
}

function pickDefaultRole() {
  if (!roleOptions.value.length) return
  pickFromOptions(roleOptions.value.map((r) => `${r.name}（${r.code}）`), (i) => {
    const role = roleOptions.value[i]
    if (role) registerDraft.defaultRoleCode = role.code
  })
}

const answerScopeOptions = [
  { label: '聚焦本系统', value: 'focus' },
  { label: '开放问答', value: 'open' },
]

/** 角色配额行：展示已选角色标签 */
function quotaRoleLabel(roleId: number) {
  const role = roleOptions.value.find((r) => r.id === roleId)
  return role ? `${role.name}（${role.code}）` : '请选择角色'
}

/** 选择角色：排除其它行已用角色，避免重复配置 */
function pickQuotaRole(idx: number) {
  if (!canEdit.value) return
  const used = new Set(
    aiDraft.roleTokenQuotas.filter((_, i) => i !== idx).map((q) => q.roleId),
  )
  const available = roleOptions.value.filter((r) => !used.has(r.id))
  if (!available.length) {
    uni.showToast({ title: '没有可选角色', icon: 'none' })
    return
  }
  pickFromOptions(available.map((r) => `${r.name}（${r.code}）`), (i) => {
    const role = available[i]
    if (role) aiDraft.roleTokenQuotas[idx].roleId = role.id
  })
}

function addQuotaRow() {
  if (!canEdit.value) return
  aiDraft.roleTokenQuotas.push({ roleId: 0, tokensDaily: 0 })
}

async function removeQuotaRow(idx: number) {
  if (!canEdit.value) return
  const { confirmed } = await showConfirm({
    title: '提示',
    content: '确定要删除该角色的 Token 配额规则吗？',
    confirmText: '确定',
    cancelText: '取消',
    tone: 'danger',
  })
  if (confirmed) {
    aiDraft.roleTokenQuotas.splice(idx, 1)
  }
}
function pickRegisterAuditors() {
  if (!canEdit.value || !registerDraft.enabled || !registerDraft.needAudit) return
  clearPagePickerResult()
  setUserSelectMultiIds(registerDraft.auditorUserIds || [])
  uni.navigateTo({
    url: appendNavFromParam(
      `/pages-sub/system/user-select?multi=1&allowEmpty=1&emptyLabel=${encodeURIComponent('未选择')}&title=${encodeURIComponent('选择审核人')}`,
    ),
  })
}
function applyRegisterAuditorPick() {
  const result = consumePagePickerResult('user-multi')
  if (!result?.ids) return
  registerDraft.auditorUserIds = result.ids
}
function pickProvider() {
  pickFromOptions(providerOptions.map((o) => o.label), (i) => {
    const item = providerOptions[i]
    if (item) smsDraft.provider = item.value as typeof smsDraft.provider
  })
}
function pickTestTemplate() {
  pickFromOptions(smsTemplateOptions.map((o) => o.label), (i) => {
    const item = smsTemplateOptions[i]
    if (item) testSmsTemplate.value = item.value
  })
}
function pickAlipaySign() {
  pickFromOptions(alipaySignOptions.map((o) => o.label), (i) => {
    const item = alipaySignOptions[i]
    if (item) paymentDraft.alipay.signType = item.value
  })
}
function pickAlipayGateway() {
  pickFromOptions(alipayGatewayOptions.map((o) => o.label), (i) => {
    const item = alipayGatewayOptions[i]
    if (item) paymentDraft.alipay.gatewayUrl = item.value
  })
}

onMounted(load)
onShow(() => {
  applyRegisterAuditorPick()
})
onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.config-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
}

.config-page :deep(.form-cell) {
  flex-direction: row;
  align-items: center;
  gap: 20rpx;
  min-height: 88rpx;
  padding: 16rpx 24rpx;
}

.config-page :deep(.form-cell__label-wrap) {
  width: 240rpx;
  flex-shrink: 0;
  padding-top: 0;
}

.config-page :deep(.form-panel--rate .form-cell__label-wrap) {
  width: 280rpx;
}

.config-page :deep(.form-cell__label) {
  display: block;
  font-size: $font-size-sm;
  line-height: 1.35;
  text-align: left;
}

.config-page :deep(.form-cell__hint) {
  display: none;
}

.config-page :deep(.form-cell__body) {
  flex: 1;
  min-width: 0;
  justify-content: flex-start;
}

.config-page :deep(.form-panel--file .form-cell__body),
.config-page :deep(.form-panel--session .form-cell__body),
.config-page :deep(.form-panel--login .form-cell__body) {
  justify-content: flex-start;
  align-items: center;
  gap: 12rpx;
}

.config-page :deep(.form-panel--file .form-cell--stacked) {
  align-items: flex-start;
  min-height: auto;
  padding-top: 20rpx;
  padding-bottom: 20rpx;
}

.config-page :deep(.form-panel--file .form-cell--stacked .form-cell__label-wrap) {
  padding-top: 14rpx;
}

.config-page :deep(.form-panel--file .form-cell--stacked .form-cell__body) {
  align-items: stretch;
}

.config-page :deep(.form-panel--rate .form-cell) {
  padding-right: 12rpx;
}

.config-page :deep(.form-panel--rate .form-cell__body) {
  justify-content: flex-end;
}

.config-page :deep(.form-cell--switch .form-cell__body) {
  justify-content: flex-start;
}

.config-security-wrap :deep(.form-cell--switch .form-cell__body) {
  justify-content: flex-end;
}

.config-page :deep(.form-cell--boxed .form-cell__body) {
  flex: 1;
  min-width: 0;
  justify-content: flex-start;
}

.config-page :deep(.picker-value) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.config-unit {
  flex-shrink: 0;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  white-space: nowrap;
}

.session-alert-box {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin: 20rpx 24rpx 0;
  padding: 18rpx 24rpx;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 12rpx;
}

.session-alert-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32rpx;
  height: 32rpx;
  border-radius: 50%;
  background: #2563eb;
  color: #ffffff;
  font-size: 20rpx;
  font-weight: bold;
  flex-shrink: 0;
  line-height: 1;
}

.session-alert-text {
  flex: 1;
  font-size: 24rpx;
  line-height: 1.4;
  color: #2563eb;
}

.config-page :deep(.form-cell__body .number-stepper) {
  flex-shrink: 0;
}

.config-page :deep(.form-panel) {
  margin-top: 0;
}

.config-page__scroll > .form-panel {
  margin-top: 16rpx;
}

.config-page :deep(.form-panel--flat) {
  border: none;
  border-radius: 0;
  box-shadow: none;
}

.config-collapse-wrap {
  padding-bottom: 8rpx;
}

.config-dirty {
  padding: 12rpx 24rpx;
  font-size: $font-size-sm;
  color: $color-warning;
  background: rgba(230, 162, 60, 0.12);
  border-top: 1px solid rgba(230, 162, 60, 0.22);
  text-align: center;
}

.config-dirty--footer {
  margin: 0;
  border-radius: 0;
}

.config-page-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 50;
  background: $color-bg-card;
  box-shadow: $shadow-footer;
  padding-bottom: env(safe-area-inset-bottom);
}

.config-page-footer__actions {
  display: flex;
  gap: 16rpx;
  padding: 16rpx 24rpx;
}

.config-page__scroll {
  flex: 1;
  height: 0;
  min-height: 0;
}

.config-test-btn {
  margin: 16rpx 32rpx;
  background: $color-primary;
  color: #fff;
  border-radius: $radius-md;
  font-size: $font-size-base;
}

.pay-modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  padding: 48rpx;
}

.pay-modal__card {
  width: 100%;
  max-width: 600rpx;
  padding: 40rpx 32rpx;
  border-radius: $radius-lg;
  background: $color-bg-card;
}

.pay-modal__title {
  display: block;
  margin-bottom: 24rpx;
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  text-align: center;
}

.pay-modal__row {
  display: block;
  margin-bottom: 12rpx;
  font-size: $font-size-sm;
  color: $color-text-regular;
}

.pay-modal__qr {
  display: block;
  width: 360rpx;
  height: 360rpx;
  margin: 24rpx auto;
}

.pay-modal__close {
  margin-top: 16rpx;
  background: $color-bg-muted;
  color: $color-text-secondary;
  border-radius: $radius-md;
  font-size: $font-size-base;
}

</style>
