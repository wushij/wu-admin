import { ref, reactive, computed, watch, onUnmounted } from 'vue'
import {
  getConfigGroup,
  updateConfigGroup,
  testSms,
  testEmail,
  testPayment,
  getPayOrderStatus,
  getRecentSmsLogs,
  getSmsLogs,
} from '@/api/system/config'
import { getRoleList } from '@/api/system/role'
import { getUserList } from '@/api/system/user'
import type {
  ConfigGroupCode,
  EmailAdminConfig,
  FileStorageConfig,
  LoginAdminConfig,
  PaymentConfig,
  RateLimitConfig,
  RegisterAdminConfig,
  SecurityAdminConfig,
  SessionAdminConfig,
  SiteAdminConfig,
  SmsAdminConfig,
  SmsLogRecord,
  ThirdPartyConfig,
} from '@/types/config-types'
import type { RoleVO } from '@/types/system'
import type { UserVO } from '@/types/user'

const GROUP_CODES: ConfigGroupCode[] = [
  'site', 'session', 'file', 'rateLimit', 'login', 'register', 'thirdParty', 'payment', 'sms', 'email', 'security',
]

const SITE_DEFAULTS: SiteAdminConfig = {
  platformName: 'Admin Platform',
  platformSubtitle: '统一运维 · 高效管控',
  loginWelcome: 'Welcome',
  registerTitle: 'Sign Up',
  copyright: '',
}

const SESSION_DEFAULTS: SessionAdminConfig = { tokenExpireHours: 24 }
const SECURITY_DEFAULTS: SecurityAdminConfig = { disableDevtool: false, isConcurrent: false }
const LOGIN_DEFAULTS: LoginAdminConfig = {
  captchaEnabled: true,
  captchaType: 'image',
  smsLoginEnabled: false,
  smsLoginSliderCaptchaEnabled: false,
  emailLoginEnabled: false,
  emailLoginSliderCaptchaEnabled: false,
  rememberMe: true,
  maxRetryCount: 5,
  maxRetryCountIp: 20,
  lockTime: 10,
}
const REGISTER_DEFAULTS: RegisterAdminConfig = {
  enabled: true,
  captchaEnabled: true,
  captchaType: 'image',
  defaultRoleCode: 'user',
  needAudit: false,
  minPasswordLength: 6,
  auditorUserIds: [],
}
const SMS_DEFAULTS: SmsAdminConfig = {
  enabled: false,
  provider: 'aliyunAuth',
  accessKeyId: '',
  accessKeySecret: '',
  signName: '',
  tencentAppId: '',
  templateVerifyCode: '100001',
  templateModifyPhone: '100002',
  templateResetPassword: '100003',
  templateBindPhone: '100004',
  templateVerifyBindPhone: '100005',
  schemeName: '',
  codeExpireMinutes: 5,
}
const EMAIL_DEFAULTS: EmailAdminConfig = {
  enabled: true,
  provider: 'qq',
  host: 'smtp.qq.com',
  port: 465,
  username: '',
  password: '',
  fromName: 'wu-admin 系统团队',
  authEnabled: true,
  securityType: 'SSL',
  connectionTimeoutMs: 5000,
  timeoutMs: 5000,
  writeTimeoutMs: 5000,
  encoding: 'UTF-8',
  debug: false,
  codeExpireMinutes: 5,
  codeLength: 6,
  dailyLimitPerEmail: 20,
  sendIntervalSeconds: 60,
}
const FILE_DEFAULTS: FileStorageConfig = {
  maxSizeMb: 50,
  allowedExtensions:
    'jpg,jpeg,png,gif,webp,bmp,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,json,xml,zip,rar,mp4,mp3,wav,avi,mov',
}
const RATE_DEFAULTS: RateLimitConfig = {
  captchaPerIpMinute: 40,
  loginPerIpMinute: 30,
  registerPerIpMinute: 10,
  smsPerIpMinute: 5,
  smsSendIntervalSeconds: 60,
  smsPerPhoneDaily: 10,
  smsPerIpDaily: 30,
}
const THIRD_DEFAULTS: ThirdPartyConfig = {
  wechat: { enabled: false, appId: '', appSecret: '' },
  alipay: { enabled: false, appId: '', privateKey: '', publicKey: '' },
  github: { enabled: false, clientId: '', clientSecret: '' },
  google: { enabled: false, clientId: '', clientSecret: '', redirectUri: '' },
}
const PAYMENT_DEFAULTS: PaymentConfig = {
  wechatPay: {
    enabled: false,
    mchId: '',
    appId: '',
    apiV3Key: '',
    privateKey: '',
    certSerialNo: '',
    notifyUrl: '',
  },
  alipay: {
    enabled: false,
    appId: '',
    privateKey: '',
    publicKey: '',
    signType: 'RSA2',
    gatewayUrl: 'https://openapi.alipay.com/gateway.do',
    notifyUrl: '',
    returnUrl: '',
  },
}

function clone<T>(data: T): T {
  return JSON.parse(JSON.stringify(data)) as T
}

function parseConfig<T>(raw?: string, defaults?: T): T {
  try {
    return { ...(defaults as object), ...JSON.parse(raw || '{}') } as T
  } catch {
    return clone(defaults as T)
  }
}

function normalizeLogin(payload: LoginAdminConfig): LoginAdminConfig {
  let result = payload.captchaEnabled ? payload : { ...payload, captchaType: 'image' }
  if (!result.smsLoginEnabled) result = { ...result, smsLoginSliderCaptchaEnabled: false }
  if (result.maxRetryCountIp === undefined) result = { ...result, maxRetryCountIp: 20 }
  return result
}

function normalizeRegisterAuditorIds(ids: unknown): number[] {
  if (!Array.isArray(ids)) return []
  return [...new Set(ids.map(Number).filter((id) => Number.isFinite(id) && id > 0))]
}

function normalizeRegister(payload: RegisterAdminConfig): RegisterAdminConfig {
  const base = payload.captchaEnabled ? payload : { ...payload, captchaType: 'image' }
  return { ...base, auditorUserIds: normalizeRegisterAuditorIds(base.auditorUserIds) }
}

function normalizeSms(payload: SmsAdminConfig): SmsAdminConfig {
  const sms = { ...payload }
  if ((sms.provider as string) === 'aliyun') sms.provider = 'aliyunAuth'
  if (!sms.codeExpireMinutes) sms.codeExpireMinutes = 5
  if (sms.provider === 'aliyunAuth') {
    if (!sms.templateVerifyCode) sms.templateVerifyCode = '100001'
    if (!sms.templateModifyPhone) sms.templateModifyPhone = '100002'
    if (!sms.templateResetPassword) sms.templateResetPassword = '100003'
    if (!sms.templateBindPhone) sms.templateBindPhone = '100004'
    if (!sms.templateVerifyBindPhone) sms.templateVerifyBindPhone = '100005'
  }
  return sms
}

function normalizeGroup<K extends ConfigGroupCode>(code: K, payload: unknown): unknown {
  if (code === 'login') return normalizeLogin(payload as LoginAdminConfig)
  if (code === 'register') return normalizeRegister(payload as RegisterAdminConfig)
  if (code === 'sms') return normalizeSms(payload as SmsAdminConfig)
  return payload
}

function mergeLoadedConfig<K extends ConfigGroupCode>(code: K, raw: string | undefined, defaults: unknown) {
  let merged = parseConfig(raw, defaults)
  if (code === 'login') {
    const login = merged as LoginAdminConfig
    if (login.captchaType === 'sms') {
      login.smsLoginEnabled = true
      login.captchaType = 'image'
    }
    if (login.smsLoginEnabled === undefined) login.smsLoginEnabled = false
    if (login.smsLoginSliderCaptchaEnabled === undefined) login.smsLoginSliderCaptchaEnabled = false
    if (login.maxRetryCountIp === undefined) login.maxRetryCountIp = 20
    merged = login
  }
  if (code === 'sms') merged = normalizeSms(merged as SmsAdminConfig)
  if (code === 'rateLimit') {
    const rl = merged as RateLimitConfig
    if (rl.smsPerIpMinute === undefined) rl.smsPerIpMinute = 5
    if (rl.smsSendIntervalSeconds === undefined) rl.smsSendIntervalSeconds = 60
    if (rl.smsPerPhoneDaily === undefined) rl.smsPerPhoneDaily = 10
    if (rl.smsPerIpDaily === undefined) rl.smsPerIpDaily = 30
    merged = rl
  }
  if (code === 'register') {
    merged = normalizeRegister(merged as RegisterAdminConfig)
  }
  return merged
}

export function useConfigEditor() {
  const loading = ref(false)
  const saving = ref(false)
  const smsTesting = ref(false)
  const paymentTesting = ref(false)
  const roleOptions = ref<RoleVO[]>([])
  const userOptions = ref<UserVO[]>([])
  const testSmsPhone = ref('')
  const testSmsTemplate = ref('100001')
  const recentSmsLogs = ref<SmsLogRecord[]>([])
  const smsLogsExpanded = ref(false)
  const smsLogsLoading = ref(false)
  const smsLogsList = ref<SmsLogRecord[]>([])
  const smsLogsTotal = ref(0)
  const smsLogsPage = ref(1)
  const smsLogsPhone = ref('')
  const smsLogsStatus = ref<number | null>(null)

  const showPaymentModal = ref(false)
  const payOrderStatus = ref('PENDING')
  const payStatusRefreshing = ref(false)
  const paymentResult = reactive({
    type: '' as 'wechat' | 'alipay' | '',
    orderNo: '',
    qrcode: '',
    payUrl: '',
  })
  let payPollTimer: ReturnType<typeof setInterval> | null = null

  const platformMaxFileMb = 500

  const siteDraft = reactive<SiteAdminConfig>({ ...SITE_DEFAULTS })
  const sessionDraft = reactive<SessionAdminConfig>({ ...SESSION_DEFAULTS })
  const securityDraft = reactive<SecurityAdminConfig>({ ...SECURITY_DEFAULTS })
  const loginDraft = reactive<LoginAdminConfig>({ ...LOGIN_DEFAULTS })
  const registerDraft = reactive<RegisterAdminConfig>({ ...REGISTER_DEFAULTS })
  const smsDraft = reactive<SmsAdminConfig>({ ...SMS_DEFAULTS })
  const emailDraft = reactive<EmailAdminConfig>({ ...EMAIL_DEFAULTS })
  const fileDraft = reactive<FileStorageConfig>({ ...FILE_DEFAULTS })
  const rateDraft = reactive<RateLimitConfig>({ ...RATE_DEFAULTS })
  const thirdDraft = reactive<ThirdPartyConfig>(clone(THIRD_DEFAULTS))
  const paymentDraft = reactive<PaymentConfig>(clone(PAYMENT_DEFAULTS))

  const savedSnapshot = reactive<Record<ConfigGroupCode, unknown>>({
    site: clone(SITE_DEFAULTS),
    session: clone(SESSION_DEFAULTS),
    security: clone(SECURITY_DEFAULTS),
    login: clone(LOGIN_DEFAULTS),
    register: clone(REGISTER_DEFAULTS),
    sms: clone(SMS_DEFAULTS),
    email: clone(EMAIL_DEFAULTS),
    file: clone(FILE_DEFAULTS),
    rateLimit: clone(RATE_DEFAULTS),
    thirdParty: clone(THIRD_DEFAULTS),
    payment: clone(PAYMENT_DEFAULTS),
  })

  const drafts = {
    site: siteDraft,
    session: sessionDraft,
    security: securityDraft,
    login: loginDraft,
    register: registerDraft,
    sms: smsDraft,
    email: emailDraft,
    file: fileDraft,
    rateLimit: rateDraft,
    thirdParty: thirdDraft,
    payment: paymentDraft,
  } as const

  const captchaTypeOptions = [
    { label: '图片', value: 'image' },
    { label: '滑块', value: 'slider' },
  ]
  const providerOptions = [
    { label: '阿里云', value: 'aliyunAuth' },
    { label: '腾讯云', value: 'tencent' },
  ]
  const smsTemplateOptions = [
    { label: '100001 登录/注册', value: '100001' },
    { label: '100002 修改绑定手机号', value: '100002' },
    { label: '100003 重置密码', value: '100003' },
    { label: '100004 绑定新手机号', value: '100004' },
    { label: '100005 验证绑定手机号', value: '100005' },
  ]
  const alipaySignOptions = [
    { label: 'RSA2', value: 'RSA2' },
    { label: 'RSA', value: 'RSA' },
  ]
  const alipayGatewayOptions = [
    { label: '正式环境', value: 'https://openapi.alipay.com/gateway.do' },
    { label: '沙箱环境', value: 'https://openapi-sandbox.dl.alipaydev.com/gateway.do' },
  ]

  const forbidConcurrentLogin = computed({
    get: () => !securityDraft.isConcurrent,
    set: (v: boolean) => { securityDraft.isConcurrent = !v },
  })

  const isDirty = computed(() =>
    GROUP_CODES.some((code) => {
      const normalizedDraft = normalizeGroup(code, clone(drafts[code]))
      const normalizedSaved = normalizeGroup(code, clone(savedSnapshot[code]))
      return JSON.stringify(normalizedDraft) !== JSON.stringify(normalizedSaved)
    }),
  )

  async function load() {
    loading.value = true
    try {
      const defaultsMap: Record<ConfigGroupCode, unknown> = {
        site: SITE_DEFAULTS,
        session: SESSION_DEFAULTS,
        security: SECURITY_DEFAULTS,
        login: LOGIN_DEFAULTS,
        register: REGISTER_DEFAULTS,
        sms: SMS_DEFAULTS,
        email: EMAIL_DEFAULTS,
        file: FILE_DEFAULTS,
        rateLimit: RATE_DEFAULTS,
        thirdParty: THIRD_DEFAULTS,
        payment: PAYMENT_DEFAULTS,
      }
      const configResults = await Promise.allSettled(GROUP_CODES.map((code) => getConfigGroup(code)))
      GROUP_CODES.forEach((code, i) => {
        const res = configResults[i]
        const raw = res.status === 'fulfilled' ? res.value.data?.configValue : undefined
        const merged = mergeLoadedConfig(code, raw, defaultsMap[code])
        Object.assign(drafts[code], merged)
        savedSnapshot[code] = clone(merged)
      })
      try {
        const roleRes = await getRoleList({ status: 1 })
        roleOptions.value = roleRes.data || []
      } catch {
        roleOptions.value = []
      }
      try {
        const userRes = await getUserList()
        userOptions.value = (userRes.data || []).filter((u) => u.status !== 0)
      } catch {
        userOptions.value = []
      }
      testSmsTemplate.value = smsDraft.templateVerifyCode || '100001'
      await loadRecentSmsLogs()
    } finally {
      loading.value = false
    }
  }

  async function saveGroup(code: ConfigGroupCode, title: string) {
    saving.value = true
    try {
      const payload = normalizeGroup(code, clone(drafts[code]))
      await updateConfigGroup(code, JSON.stringify(payload))
      Object.assign(drafts[code], payload as object)
      savedSnapshot[code] = clone(payload)
      uni.showToast({ title: `${title}已保存`, icon: 'success' })
    } finally {
      saving.value = false
    }
  }

  async function saveAll() {
    if (!siteDraft.platformName?.trim()) {
      uni.showToast({ title: '请填写平台名称', icon: 'none' })
      return
    }
    if (!isDirty.value) {
      uni.showToast({ title: '暂无修改', icon: 'none' })
      return
    }
    saving.value = true
    try {
      for (const code of GROUP_CODES) {
        const payload = normalizeGroup(code, clone(drafts[code]))
        await updateConfigGroup(code, JSON.stringify(payload))
        Object.assign(drafts[code], payload)
        savedSnapshot[code] = clone(payload)
      }
      uni.showToast({ title: '配置已全部保存', icon: 'success' })
    } finally {
      saving.value = false
    }
  }

  function resetAll() {
    for (const code of GROUP_CODES) {
      Object.assign(drafts[code], clone(savedSnapshot[code]) as object)
    }
    uni.showToast({ title: '已恢复为上次保存', icon: 'none' })
  }

  const saveSite = () => saveGroup('site', '站点配置')
  const saveSession = () => saveGroup('session', '会话配置')
  const saveSecurity = () => saveGroup('security', '安全配置')
  const saveFile = () => saveGroup('file', '文件配置')
  const saveRateLimit = () => saveGroup('rateLimit', '限流配置')
  const saveThirdParty = () => saveGroup('thirdParty', '第三方配置')
  const savePayment = () => saveGroup('payment', '支付配置')
  const saveLogin = () => saveGroup('login', '登录配置')
  const saveRegister = () => saveGroup('register', '注册配置')
  const saveSms = () => saveGroup('sms', '短信配置')

  function smsStatusText(status?: number) {
    if (status === 1) return '成功'
    if (status === 2) return '失败'
    return '发送中'
  }

  async function loadRecentSmsLogs() {
    try {
      const res = await getRecentSmsLogs(5)
      recentSmsLogs.value = res.data || []
    } catch {
      recentSmsLogs.value = []
    }
  }

  async function loadSmsLogs(append = false) {
    smsLogsLoading.value = true
    try {
      const res = await getSmsLogs({
        page: smsLogsPage.value,
        size: 10,
        phone: smsLogsPhone.value.trim() || undefined,
        status: smsLogsStatus.value,
      })
      const rows = res.data?.list || []
      smsLogsList.value = append ? [...smsLogsList.value, ...rows] : rows
      smsLogsTotal.value = res.data?.total || 0
    } finally {
      smsLogsLoading.value = false
    }
  }

  async function openSmsLogs() {
    uni.navigateTo({ url: '/pages-sub/system/config/sms-logs' })
  }

  async function sendTestSms() {
    if (isDirty.value) {
      uni.showToast({ title: '请先保存短信配置', icon: 'none' })
      return
    }
    const phone = testSmsPhone.value.trim()
    if (!/^1\d{10}$/.test(phone)) {
      uni.showToast({ title: '请输入 11 位手机号', icon: 'none' })
      return
    }
    smsTesting.value = true
    try {
      const templateCode = smsDraft.provider === 'aliyunAuth' ? testSmsTemplate.value : undefined
      await testSms(phone, templateCode)
      uni.showToast({ title: '测试短信已发送', icon: 'success' })
      await loadRecentSmsLogs()
    } finally {
      smsTesting.value = false
    }
  }

  function stopPayPolling() {
    if (payPollTimer) {
      clearInterval(payPollTimer)
      payPollTimer = null
    }
  }

  async function pollPayOrderStatus(manual = false) {
    if (!paymentResult.orderNo) return
    if (manual) payStatusRefreshing.value = true
    try {
      const res = await getPayOrderStatus(paymentResult.orderNo)
      payOrderStatus.value = res.data?.status || 'PENDING'
      if (payOrderStatus.value === 'PAID') {
        stopPayPolling()
        uni.showToast({ title: '支付成功', icon: 'success' })
      } else if (manual) {
        uni.showToast({ title: '尚未支付成功', icon: 'none' })
      }
    } finally {
      payStatusRefreshing.value = false
    }
  }

  watch(showPaymentModal, (visible) => {
    stopPayPolling()
    if (visible) {
      payOrderStatus.value = 'PENDING'
      pollPayOrderStatus()
      payPollTimer = setInterval(() => pollPayOrderStatus(), 2000)
    }
  })

  onUnmounted(stopPayPolling)

  async function sendTestPayment(type: 'wechat' | 'alipay') {
    if (isDirty.value) {
      uni.showToast({ title: '请先保存支付配置', icon: 'none' })
      return
    }
    paymentTesting.value = true
    try {
      const res = await testPayment(type)
      paymentResult.type = type
      paymentResult.orderNo = res.data?.orderNo || ''
      paymentResult.qrcode = res.data?.qrcode || ''
      paymentResult.payUrl = res.data?.payUrl || ''
      showPaymentModal.value = true
    } finally {
      paymentTesting.value = false
    }
  }

  const emailTesting = ref(false)
  const testEmailTo = ref('')

  async function sendTestEmailAction() {
    if (isDirty.value) {
      uni.showToast({ title: '请先保存邮件配置', icon: 'none' })
      return
    }
    const toEmail = testEmailTo.value.trim()
    if (!toEmail || !toEmail.includes('@')) {
      uni.showToast({ title: '请输入正确的接收邮箱', icon: 'none' })
      return
    }
    emailTesting.value = true
    try {
      await testEmail(toEmail)
      uni.showToast({ title: '测试邮件已发送，请查收', icon: 'success' })
    } finally {
      emailTesting.value = false
    }
  }

  function closePaymentModal() {
    showPaymentModal.value = false
  }

  return {
    loading,
    saving,
    smsTesting,
    emailTesting,
    testEmailTo,
    paymentTesting,
    platformMaxFileMb,
    isDirty,
    forbidConcurrentLogin,
    siteDraft,
    sessionDraft,
    securityDraft,
    loginDraft,
    registerDraft,
    smsDraft,
    emailDraft,
    fileDraft,
    rateDraft,
    thirdDraft,
    paymentDraft,
    savedSnapshot,
    roleOptions,
    userOptions,
    testSmsPhone,
    testSmsTemplate,
    recentSmsLogs,
    smsLogsExpanded,
    smsLogsLoading,
    smsLogsList,
    smsLogsTotal,
    smsLogsPage,
    smsLogsPhone,
    smsLogsStatus,
    showPaymentModal,
    payOrderStatus,
    payStatusRefreshing,
    paymentResult,
    captchaTypeOptions,
    providerOptions,
    smsTemplateOptions,
    alipaySignOptions,
    alipayGatewayOptions,
    smsStatusText,
    load,
    saveAll,
    resetAll,
    saveSite,
    saveSession,
    saveSecurity,
    saveLogin,
    saveRegister,
    saveSms,
    saveFile,
    saveRateLimit,
    saveThirdParty,
    savePayment,
    loadRecentSmsLogs,
    loadSmsLogs,
    openSmsLogs,
    sendTestSms,
    sendTestEmailAction,
    sendTestPayment,
    pollPayOrderStatus,
    closePaymentModal,
  }
}
