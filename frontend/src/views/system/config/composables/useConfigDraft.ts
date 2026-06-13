import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfigGroup, updateConfigGroup } from '@/api/system/config'
import { getRoleList } from '@/api/system/role'
import { getUserList } from '@/api/system/user'
import { getErrorMessage } from '@/utils/axiosError'
import { useUserStore } from '@/store/user'
import { useSiteStore } from '@/store/site'
import type { ConfigGroupCode, ConfigGroupMap } from '@/types/config'

export const GROUP_CODES = [
  'site', 'session', 'file', 'rateLimit', 'login', 'register', 'thirdParty', 'payment', 'sms', 'security',
] as const satisfies readonly ConfigGroupCode[]

const DEFAULTS = {
  site: {
    platformName: 'Admin Platform',
    platformSubtitle: '统一运维 · 高效管控',
    loginWelcome: 'Welcome',
    registerTitle: 'Sign Up',
    copyright: ''
  },
  session: { tokenExpireHours: 24 },
  file: {
    maxSizeMb: 50,
    allowedExtensions:
      'jpg,jpeg,png,gif,webp,bmp,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,json,xml,zip,rar,mp4,mp3,wav,avi,mov'
  },
  rateLimit: {
    captchaPerIpMinute: 40,
    loginPerIpMinute: 30,
    registerPerIpMinute: 10,
    smsPerIpMinute: 5,
    smsSendIntervalSeconds: 60,
    smsPerPhoneDaily: 10,
    smsPerIpDaily: 30,
  },
  login: {
    captchaEnabled: true,
    captchaType: 'image',
    smsLoginEnabled: false,
    smsLoginSliderCaptchaEnabled: false,
    rememberMe: true,
    maxRetryCount: 5,
    lockTime: 10
  },
  register: {
    enabled: true,
    captchaEnabled: true,
    captchaType: 'image',
    defaultRoleCode: 'user',
    needAudit: false,
    minPasswordLength: 6,
    auditorUserIds: [],
  },
  thirdParty: {
    wechat: { enabled: false, appId: '', appSecret: '' },
    alipay: { enabled: false, appId: '', privateKey: '', publicKey: '' },
    github: { enabled: false, clientId: '', clientSecret: '' },
    google: { enabled: false, clientId: '', clientSecret: '', redirectUri: '' },
  },
  payment: {
    wechatPay: {
      enabled: false, mchId: '', appId: '', apiV3Key: '',
      privateKey: '', certSerialNo: '', notifyUrl: '',
    },
    alipay: {
      enabled: false, appId: '', privateKey: '', publicKey: '',
      signType: 'RSA2', gatewayUrl: 'https://openapi.alipay.com/gateway.do',
      notifyUrl: '', returnUrl: '',
    },
  },
  security: {
    disableDevtool: false,
    isConcurrent: false
  },
  sms: {
    enabled: false, provider: 'aliyunAuth',
    accessKeyId: '', accessKeySecret: '', signName: '',
    tencentAppId: '',
    templateVerifyCode: '100001', templateModifyPhone: '100002',
    templateResetPassword: '100003', templateBindPhone: '100004',
    templateVerifyBindPhone: '100005',
    schemeName: '', codeExpireMinutes: 5,
  },
} satisfies ConfigGroupMap

type ConfigState = ConfigGroupMap

function cloneConfig<T>(data: T): T {
  return JSON.parse(JSON.stringify(data)) as T
}

function setConfigGroup<K extends ConfigGroupCode>(
  state: ConfigState, code: K, value: ConfigState[K]
) {
  state[code] = value
}

export function normalizePayload<K extends ConfigGroupCode>(code: K, payload: ConfigGroupMap[K]): ConfigGroupMap[K] {
  if (code === 'login') {
    const login = payload as ConfigGroupMap['login']
    let result = login.captchaEnabled ? login : { ...login, captchaType: 'image' }
    if (!result.smsLoginEnabled) {
      result = { ...result, smsLoginSliderCaptchaEnabled: false }
    }
    return result as ConfigGroupMap[K]
  }
  if (code === 'register') {
    const register = payload as ConfigGroupMap['register']
    const base = register.captchaEnabled ? register : { ...register, captchaType: 'image' }
    const ids = Array.isArray(base.auditorUserIds) ? base.auditorUserIds : []
    return {
      ...base,
      auditorUserIds: [...new Set(ids.map(Number).filter((id) => Number.isFinite(id) && id > 0))],
    } as ConfigGroupMap[K]
  }
  return payload
}

function parseJson(str: string | undefined): unknown {
  try { return JSON.parse(str || '{}') } catch { return {} }
}

function applyGroupFromServer<K extends ConfigGroupCode>(
  savedSnapshot: ConfigState, draft: ConfigState, code: K, serverJson: Partial<ConfigGroupMap[K]>
) {
  const merged = { ...DEFAULTS[code], ...serverJson }
  if (code === 'rateLimit') {
    const rl = merged as ConfigGroupMap['rateLimit']
    if (rl.smsPerIpMinute === undefined) rl.smsPerIpMinute = 5
    if (rl.smsSendIntervalSeconds === undefined) rl.smsSendIntervalSeconds = 60
    if (rl.smsPerPhoneDaily === undefined) rl.smsPerPhoneDaily = 10
    if (rl.smsPerIpDaily === undefined) rl.smsPerIpDaily = 30
  }
  if (code === 'login') {
    const login = merged as ConfigGroupMap['login']
    if (login.captchaType === 'sms') { login.smsLoginEnabled = true; login.captchaType = 'image' }
    if (login.smsLoginEnabled === undefined) login.smsLoginEnabled = false
    if (login.smsLoginSliderCaptchaEnabled === undefined) login.smsLoginSliderCaptchaEnabled = false
  }
  if (code === 'register') {
    const reg = merged as ConfigGroupMap['register']
    if (!Array.isArray(reg.auditorUserIds)) reg.auditorUserIds = []
  }
  if (code === 'sms') {
    const sms = merged as ConfigGroupMap['sms']
    if ((sms.provider as string) === 'aliyun') sms.provider = 'aliyunAuth'
    if (!sms.codeExpireMinutes) sms.codeExpireMinutes = 5
    if (sms.provider === 'aliyunAuth') {
      if (!sms.templateVerifyCode) sms.templateVerifyCode = '100001'
      if (!sms.templateModifyPhone) sms.templateModifyPhone = '100002'
      if (!sms.templateResetPassword) sms.templateResetPassword = '100003'
      if (!sms.templateBindPhone) sms.templateBindPhone = '100004'
      if (!sms.templateVerifyBindPhone) sms.templateVerifyBindPhone = '100005'
    }
  }
  setConfigGroup(savedSnapshot, code, cloneConfig(merged))
  setConfigGroup(draft, code, cloneConfig(merged))
}

export interface RoleOption { name: string; code: string }
export interface UserOption { id: number; label: string }

export function useConfigDraft() {
  const userStore = useUserStore()
  const siteStore = useSiteStore()

  const canEdit = computed(() => (userStore.userInfo?.permissions || []).includes('system:config:update'))
  const activeTab = ref('site')
  const loading = ref(false)
  const saving = ref(false)
  const roleOptions = ref<RoleOption[]>([])
  const userOptions = ref<UserOption[]>([])
  const platformMaxFileMb = 500

  const savedSnapshot = reactive(cloneConfig(DEFAULTS) as ConfigState)
  const draft = reactive(cloneConfig(DEFAULTS) as ConfigState)

  const isDirty = ref(false)

  const forbidConcurrentLogin = computed({
    get: () => !draft.security.isConcurrent,
    set: (value: boolean) => { draft.security.isConcurrent = !value }
  })

  function checkDirty() {
    isDirty.value = GROUP_CODES.some((code) => {
      const normalizedDraft = normalizePayload(code, cloneConfig(draft[code]))
      const normalizedSaved = normalizePayload(code, cloneConfig(savedSnapshot[code]))
      return JSON.stringify(normalizedDraft) !== JSON.stringify(normalizedSaved)
    })
  }

  watch(draft, checkDirty, { deep: true })
  watch(savedSnapshot, checkDirty, { deep: true })

  async function loadGroup(code: ConfigGroupCode) {
    try {
      const res = await getConfigGroup(code, { silent403: true })
      const serverJson = res.data?.configValue ? parseJson(res.data.configValue) : {}
      applyGroupFromServer(savedSnapshot, draft, code, serverJson as Partial<ConfigGroupMap[typeof code]>)
    } catch {
      applyGroupFromServer(savedSnapshot, draft, code, {})
    }
  }

  async function loadRoles() {
    const perms = userStore.userInfo?.permissions || []
    if (!perms.includes('system:role:list') && !perms.includes('system:role:query')) {
      roleOptions.value = [{ name: '普通用户', code: 'user' }]
      return
    }
    try {
      const res = await getRoleList({ status: 1 })
      roleOptions.value = (res.data || []).map((r: any) => ({ name: r.name, code: r.code }))
    } catch {
      roleOptions.value = [{ name: '普通用户', code: 'user' }]
    }
  }

  async function loadUsers() {
    const perms = userStore.userInfo?.permissions || []
    if (!perms.includes('system:user:list') && !perms.includes('system:user:query')) {
      userOptions.value = []
      return
    }
    try {
      const res = await getUserList()
      userOptions.value = (res.data || [])
        .filter((u) => u.status !== 0)
        .map((u) => ({
          id: u.id,
          label: `${u.nickname || u.username}${u.deptName ? `（${u.deptName}）` : ''}`,
        }))
    } catch {
      userOptions.value = []
    }
  }

  async function loadAll() {
    loading.value = true
    let forbidden = false
    const results = await Promise.allSettled([
      ...GROUP_CODES.map((code) => loadGroup(code)),
      loadRoles(),
      loadUsers(),
    ])
    for (const r of results) {
      if (r.status === 'rejected') {
        const msg = String((r as any).reason?.message || '')
        if (msg.includes('权限不足')) forbidden = true
      }
    }
    if (forbidden) ElMessage.warning('部分配置无查看权限，请联系管理员')
    loading.value = false
    checkDirty()
  }

  function handleReset() {
    for (const code of GROUP_CODES) {
      setConfigGroup(draft, code, cloneConfig(savedSnapshot[code]))
    }
    checkDirty()
    ElMessage.info('已恢复为上次保存的配置')
  }

  async function handleSave() {
    checkDirty()
    if (!isDirty.value) { ElMessage.info('暂无修改，无需保存'); return }
    if (!draft.site.platformName?.trim()) {
      ElMessage.warning('请填写平台名称')
      activeTab.value = 'site'
      return
    }
    saving.value = true
    const devtoolChanged = draft.security.disableDevtool !== savedSnapshot.security.disableDevtool
    try {
      for (const code of GROUP_CODES) {
        const payload = normalizePayload(code, cloneConfig(draft[code]))
        await updateConfigGroup(code, JSON.stringify(payload))
        const saved = cloneConfig(payload)
        setConfigGroup(savedSnapshot, code, saved)
        setConfigGroup(draft, code, cloneConfig(saved))
      }
      checkDirty()
      siteStore.setDisableDevtool(draft.security.disableDevtool)
      ElMessage.success(
        devtoolChanged
          ? '保存成功，配置已生效；「禁止前端调试」已变更，请刷新页面后生效'
          : '保存成功，配置已生效',
      )
    } catch (e) {
      ElMessage.error(getErrorMessage(e) || '保存失败')
    } finally {
      saving.value = false
    }
  }

  return {
    canEdit, activeTab, loading, saving, roleOptions, userOptions, platformMaxFileMb,
    draft, savedSnapshot, isDirty, forbidConcurrentLogin,
    checkDirty, loadAll, handleReset, handleSave,
  }
}
