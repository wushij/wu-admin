<template>
  <div class="app-container">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>系统配置</span>
          <span v-if="isDirty" class="dirty-hint">有未保存的修改</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="基础信息" name="site">
          <el-form :model="draft.site" label-width="120px" class="config-form">
            <el-form-item label="平台名称">
              <el-input v-model="draft.site.platformName" maxlength="50" :disabled="!canEdit" />
            </el-form-item>
            <el-form-item label="平台副标题">
              <el-input v-model="draft.site.platformSubtitle" maxlength="80" :disabled="!canEdit" />
            </el-form-item>
            <el-form-item label="登录页标题">
              <el-input v-model="draft.site.loginWelcome" maxlength="30" :disabled="!canEdit" />
            </el-form-item>
            <el-form-item label="注册页标题">
              <el-input v-model="draft.site.registerTitle" maxlength="30" :disabled="!canEdit" />
            </el-form-item>
            <el-form-item label="页脚版权">
              <el-input v-model="draft.site.copyright" maxlength="120" placeholder="选填" :disabled="!canEdit" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="会话令牌" name="session">
          <el-form :model="draft.session" label-width="120px" class="config-form">
            <el-form-item label="Token 有效期">
              <el-input-number v-model="draft.session.tokenExpireHours" :min="1" :max="720" :disabled="!canEdit" />
              <span class="unit">小时（保存全部后生效）</span>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="文件存储" name="file">
          <el-form :model="draft.file" label-width="120px" class="config-form">
            <el-form-item label="单文件上限">
              <el-input-number v-model="draft.file.maxSizeMb" :min="1" :max="platformMaxFileMb" :disabled="!canEdit" />
              <span class="unit">MB（保存全部后生效，最高 {{ platformMaxFileMb }}）</span>
            </el-form-item>
            <el-form-item label="允许扩展名">
              <el-input
                v-model="draft.file.allowedExtensions"
                type="textarea"
                :rows="3"
                placeholder="逗号分隔，如 jpg,png,pdf"
                :disabled="!canEdit"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="接口限流" name="rateLimit">
          <el-form :model="draft.rateLimit" label-width="140px" class="config-form">
            <el-form-item label="验证码(次/分钟/IP)">
              <el-input-number v-model="draft.rateLimit.captchaPerIpMinute" :min="0" :max="200" :disabled="!canEdit" />
            </el-form-item>
            <el-form-item label="登录(次/分钟/IP)">
              <el-input-number v-model="draft.rateLimit.loginPerIpMinute" :min="0" :max="200" :disabled="!canEdit" />
            </el-form-item>
            <el-form-item label="注册(次/分钟/IP)">
              <el-input-number v-model="draft.rateLimit.registerPerIpMinute" :min="0" :max="200" :disabled="!canEdit" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="登录认证" name="login">
          <el-form :model="draft.login" label-width="120px" class="config-form">
            <el-form-item label="启用验证码">
              <el-switch v-model="draft.login.captchaEnabled" :disabled="!canEdit" />
            </el-form-item>
            <el-form-item v-if="draft.login.captchaEnabled" label="验证码类型">
              <el-radio-group v-model="draft.login.captchaType" :disabled="!canEdit">
                <el-radio label="image">图片</el-radio>
                <el-radio label="slider">滑块</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="记住我">
              <el-switch v-model="draft.login.rememberMe" :disabled="!canEdit" />
            </el-form-item>
            <el-form-item label="最大重试次数">
              <el-input-number v-model="draft.login.maxRetryCount" :min="1" :max="20" :disabled="!canEdit" />
            </el-form-item>
            <el-form-item label="锁定时长">
              <el-input-number v-model="draft.login.lockTime" :min="1" :max="120" :disabled="!canEdit" />
              <span class="unit">分钟</span>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册认证" name="register">
          <el-form :model="draft.register" label-width="120px" class="config-form">
            <el-form-item label="开放注册">
              <el-switch v-model="draft.register.enabled" :disabled="!canEdit" />
            </el-form-item>
            <el-form-item label="注册验证码">
              <el-switch v-model="draft.register.captchaEnabled" :disabled="!canEdit || !draft.register.enabled" />
            </el-form-item>
            <el-form-item
              v-if="draft.register.enabled && draft.register.captchaEnabled"
              label="验证码类型"
            >
              <el-radio-group v-model="draft.register.captchaType" :disabled="!canEdit">
                <el-radio label="image">图片</el-radio>
                <el-radio label="slider">滑块</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="密码最小长度">
              <el-input-number
                v-model="draft.register.minPasswordLength"
                :min="6"
                :max="32"
                :disabled="!canEdit || !draft.register.enabled"
              />
            </el-form-item>
            <el-form-item label="默认角色">
              <el-select
                v-model="draft.register.defaultRoleCode"
                :disabled="!canEdit || !draft.register.enabled"
                style="width: 260px"
              >
                <el-option
                  v-for="role in roleOptions"
                  :key="role.code"
                  :label="`${role.name}（${role.code}）`"
                  :value="role.code"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="注册需审核">
              <el-switch v-model="draft.register.needAudit" :disabled="!canEdit || !draft.register.enabled" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="第三方配置" name="thirdParty">
          <el-collapse class="pay-collapse">
            <el-collapse-item title="微信登录" name="wechat">
              <el-form
                :model="draft.thirdParty.wechat"
                label-width="120px"
                class="config-form wide-form"
                autocomplete="off"
                @submit.prevent
              >
                <el-form-item label="启用">
                  <el-switch v-model="draft.thirdParty.wechat.enabled" :disabled="!canEdit" />
                </el-form-item>
                <el-form-item label="AppID">
                  <el-input
                    v-model="draft.thirdParty.wechat.appId"
                    name="oauth-wechat-app-id"
                    autocomplete="off"
                    placeholder="微信开放平台 AppID"
                    :disabled="!canEdit"
                  />
                </el-form-item>
                <el-form-item label="AppSecret">
                  <el-input
                    v-model="draft.thirdParty.wechat.appSecret"
                    name="oauth-wechat-app-secret"
                    type="password"
                    show-password
                    autocomplete="new-password"
                    placeholder="微信开放平台 AppSecret"
                    :disabled="!canEdit"
                  />
                </el-form-item>
              </el-form>
            </el-collapse-item>
            <el-collapse-item title="支付宝登录" name="alipay-oauth">
              <el-form
                :model="draft.thirdParty.alipay"
                label-width="120px"
                class="config-form wide-form"
                autocomplete="off"
                @submit.prevent
              >
                <el-form-item label="启用">
                  <el-switch v-model="draft.thirdParty.alipay.enabled" :disabled="!canEdit" />
                </el-form-item>
                <el-form-item label="AppID">
                  <el-input
                    v-model="draft.thirdParty.alipay.appId"
                    name="oauth-alipay-app-id"
                    autocomplete="off"
                    placeholder="支付宝应用 AppID"
                    :disabled="!canEdit"
                  />
                </el-form-item>
                <el-form-item label="应用私钥">
                  <el-input v-model="draft.thirdParty.alipay.privateKey" type="textarea" :rows="3" :disabled="!canEdit" />
                </el-form-item>
                <el-form-item label="支付宝公钥">
                  <el-input v-model="draft.thirdParty.alipay.publicKey" type="textarea" :rows="3" :disabled="!canEdit" />
                </el-form-item>
              </el-form>
            </el-collapse-item>
            <el-collapse-item title="GitHub 登录" name="github">
              <el-form
                :model="draft.thirdParty.github"
                label-width="120px"
                class="config-form wide-form"
                autocomplete="off"
                @submit.prevent
              >
                <el-form-item label="启用">
                  <el-switch v-model="draft.thirdParty.github.enabled" :disabled="!canEdit" />
                </el-form-item>
                <el-form-item label="Client ID">
                  <el-input
                    v-model="draft.thirdParty.github.clientId"
                    name="oauth-github-client-id"
                    autocomplete="off"
                    placeholder="GitHub OAuth Client ID"
                    :disabled="!canEdit"
                  />
                </el-form-item>
                <el-form-item label="Client Secret">
                  <el-input
                    v-model="draft.thirdParty.github.clientSecret"
                    name="oauth-github-client-secret"
                    type="password"
                    show-password
                    autocomplete="new-password"
                    placeholder="GitHub OAuth Client Secret"
                    :disabled="!canEdit"
                  />
                </el-form-item>
              </el-form>
            </el-collapse-item>
            <el-collapse-item title="Google 登录" name="google">
              <el-form
                :model="draft.thirdParty.google"
                label-width="120px"
                class="config-form wide-form"
                autocomplete="off"
                @submit.prevent
              >
                <el-form-item label="启用">
                  <el-switch v-model="draft.thirdParty.google.enabled" :disabled="!canEdit" />
                </el-form-item>
                <el-form-item label="Client ID">
                  <el-input
                    v-model="draft.thirdParty.google.clientId"
                    name="oauth-google-client-id"
                    autocomplete="off"
                    placeholder="Google Cloud OAuth 客户端 ID"
                    :disabled="!canEdit"
                  />
                </el-form-item>
                <el-form-item label="Client Secret">
                  <el-input
                    v-model="draft.thirdParty.google.clientSecret"
                    name="oauth-google-client-secret"
                    type="password"
                    show-password
                    autocomplete="new-password"
                    placeholder="Google OAuth 客户端密钥"
                    :disabled="!canEdit"
                  />
                </el-form-item>
                <el-form-item label="重定向 URI">
                  <el-input
                    v-model="draft.thirdParty.google.redirectUri"
                    name="oauth-google-redirect-uri"
                    autocomplete="off"
                    placeholder="https://你的域名/api/auth/oauth/google/callback"
                    :disabled="!canEdit"
                  />
                  <span class="unit">须在 Google Cloud 控制台「已获授权的重定向 URI」中配置相同地址</span>
                </el-form-item>
              </el-form>
            </el-collapse-item>
          </el-collapse>
        </el-tab-pane>

        <el-tab-pane label="支付配置" name="payment">
          <el-collapse class="pay-collapse">
            <el-collapse-item title="微信支付" name="wechatPay">
              <el-form
                :model="draft.payment.wechatPay"
                label-width="120px"
                class="config-form wide-form"
                autocomplete="off"
                @submit.prevent
              >
                <el-form-item label="启用">
                  <el-switch v-model="draft.payment.wechatPay.enabled" :disabled="!canEdit" />
                </el-form-item>
                <el-form-item label="商户号">
                  <el-input
                    v-model="draft.payment.wechatPay.mchId"
                    name="wxpay-mch-id"
                    autocomplete="off"
                    placeholder="请输入商户号"
                    :disabled="!canEdit"
                  />
                </el-form-item>
                <el-form-item label="AppID">
                  <el-input
                    v-model="draft.payment.wechatPay.appId"
                    name="wxpay-app-id"
                    autocomplete="off"
                    placeholder="微信支付 AppID（非登录账号）"
                    :disabled="!canEdit"
                  />
                </el-form-item>
                <el-form-item label="APIv3 密钥">
                  <el-input
                    v-model="draft.payment.wechatPay.apiV3Key"
                    name="wxpay-api-v3-key"
                    type="password"
                    show-password
                    autocomplete="new-password"
                    placeholder="32 位 APIv3 密钥"
                    :disabled="!canEdit"
                  />
                </el-form-item>
                <el-form-item label="商户私钥">
                  <el-input
                    v-model="draft.payment.wechatPay.privateKey"
                    name="wxpay-private-key"
                    type="textarea"
                    :rows="4"
                    autocomplete="off"
                    :disabled="!canEdit"
                  />
                </el-form-item>
                <el-form-item label="证书序列号">
                  <el-input
                    v-model="draft.payment.wechatPay.certSerialNo"
                    name="wxpay-cert-serial"
                    autocomplete="off"
                    placeholder="证书序列号"
                    :disabled="!canEdit"
                  />
                </el-form-item>
                <el-form-item label="回调地址">
                  <el-input
                    v-model="draft.payment.wechatPay.notifyUrl"
                    name="wxpay-notify-url"
                    autocomplete="off"
                    :placeholder="defaultWechatNotifyUrl"
                    :disabled="!canEdit"
                  />
                  <span class="unit">须公网 HTTPS，对应 POST /api/pay/notify/wechat</span>
                </el-form-item>
                <el-form-item v-if="draft.payment.wechatPay.enabled && canEdit" label="测试支付">
                  <el-button type="primary" :loading="paymentTesting" @click="handleTestPayment('wechat')">
                    生成测试订单
                  </el-button>
                </el-form-item>
              </el-form>
            </el-collapse-item>
            <el-collapse-item title="支付宝支付" name="alipayPay">
              <el-form
                :model="draft.payment.alipay"
                label-width="120px"
                class="config-form wide-form"
                autocomplete="off"
                @submit.prevent
              >
                <el-form-item label="启用">
                  <el-switch v-model="draft.payment.alipay.enabled" :disabled="!canEdit" />
                </el-form-item>
                <el-form-item label="AppID">
                  <el-input
                    v-model="draft.payment.alipay.appId"
                    name="alipay-app-id"
                    autocomplete="off"
                    placeholder="支付宝 AppID"
                    :disabled="!canEdit"
                  />
                </el-form-item>
                <el-form-item label="应用私钥">
                  <el-input
                    v-model="draft.payment.alipay.privateKey"
                    name="alipay-private-key"
                    type="textarea"
                    :rows="4"
                    autocomplete="off"
                    :disabled="!canEdit"
                  />
                </el-form-item>
                <el-form-item label="支付宝公钥">
                  <el-input
                    v-model="draft.payment.alipay.publicKey"
                    name="alipay-public-key"
                    type="textarea"
                    :rows="4"
                    autocomplete="off"
                    :disabled="!canEdit"
                  />
                </el-form-item>
                <el-form-item label="签名类型">
                  <el-select v-model="draft.payment.alipay.signType" style="width: 160px" :disabled="!canEdit">
                    <el-option label="RSA2" value="RSA2" />
                    <el-option label="RSA" value="RSA" />
                  </el-select>
                </el-form-item>
                <el-form-item label="网关地址">
                  <el-select v-model="draft.payment.alipay.gatewayUrl" style="width: 100%; max-width: 480px" :disabled="!canEdit">
                    <el-option label="正式环境" value="https://openapi.alipay.com/gateway.do" />
                    <el-option label="沙箱环境" value="https://openapi-sandbox.dl.alipaydev.com/gateway.do" />
                  </el-select>
                </el-form-item>
                <el-form-item label="回调地址">
                  <el-input v-model="draft.payment.alipay.notifyUrl" :placeholder="defaultAlipayNotifyUrl" :disabled="!canEdit" />
                  <span class="unit">须公网 HTTPS，对应 POST /api/pay/notify/alipay</span>
                </el-form-item>
                <el-form-item v-if="draft.payment.alipay.enabled && canEdit" label="测试支付">
                  <el-button type="primary" :loading="paymentTesting" @click="handleTestPayment('alipay')">
                    生成测试订单
                  </el-button>
                </el-form-item>
              </el-form>
            </el-collapse-item>
          </el-collapse>
          <el-alert type="info" :closable="false" show-icon class="pay-tip-alert">
            填写后务必点击页底「保存全部」。
          </el-alert>
          <el-alert type="info" :closable="false" show-icon class="pay-tip-alert">
            测试订单金额固定 0.01 元。保存支付配置后再生成订单；支付完成后弹窗会每 2 秒向微信/支付宝主动查单并更新状态。
          </el-alert>
        </el-tab-pane>

        <el-tab-pane label="安全配置" name="security">
          <el-form :model="draft.security" label-width="140px" class="config-form">
            <el-divider content-position="left">前端安全</el-divider>
            <el-form-item label="禁止前端调试">
              <el-switch v-model="draft.security.disableDevtool" :disabled="!canEdit" />
              <span class="unit">开启后将限制打开开发者工具（F12），降低随意查看源码与调试的风险</span>
            </el-form-item>
            <el-divider content-position="left">会话安全</el-divider>
            <el-form-item label="禁止多端同时在线">
              <el-switch v-model="forbidConcurrentLogin" :disabled="!canEdit" />
              <span class="unit">开启后，同一账号再次登录会先踢掉之前的会话，只保留最新一次登录</span>
            </el-form-item>
          </el-form>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="保存全部后立即生效：禁止多端对新登录生效；禁止前端调试需刷新浏览器页面。"
          />
        </el-tab-pane>
      </el-tabs>

      <div v-if="canEdit" class="footer-actions">
        <el-button @click="handleReset">重置</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存全部</el-button>
      </div>
    </el-card>

    <!-- 测试支付弹窗 -->
    <el-dialog v-model="showPaymentModal" title="测试支付" width="420px" :lock-scroll="false" @closed="stopPayPolling">
      <div class="payment-test-modal">
        <div class="payment-info">
          <p>支付方式：{{ paymentResult.type === 'wechat' ? '微信支付' : '支付宝' }}</p>
          <p>订单号：{{ paymentResult.orderNo }}</p>
          <p>金额：<span class="amount">¥ 0.01</span></p>
          <p>
            支付状态：
            <el-tag :type="payOrderStatus === 'PAID' ? 'success' : 'warning'" size="small">
              {{ payOrderStatus === 'PAID' ? '已支付' : '待支付' }}
            </el-tag>
          </p>
        </div>
        <div v-if="paymentResult.qrcode" class="qrcode-container">
          <img :src="paymentResult.qrcode" alt="支付二维码" class="qrcode-img" />
          <p class="qrcode-tip">请使用{{ paymentResult.type === 'wechat' ? '微信' : '支付宝' }}扫码支付</p>
        </div>
        <div class="payment-actions">
          <el-button :loading="payStatusRefreshing" @click="pollPayOrderStatus(true)">刷新支付状态</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getConfigGroup, updateConfigGroup, testPayment, getPayOrderStatus } from '@/api/system/config'
import { getRoleList } from '@/api/system/role'
import { getErrorMessage } from '@/utils/axiosError'
import type { ConfigGroupCode, ConfigGroupMap } from '@/types/config'

import { useUserStore } from '@/store/user'
import { useSiteStore } from '@/store/site'

const userStore = useUserStore()
const siteStore = useSiteStore()
const canEdit = computed(() => (userStore.userInfo?.permissions || []).includes('system:config:update'))
const activeTab = ref('site')
const loading = ref(false)
const saving = ref(false)
interface RoleOption {
  name: string
  code: string
}

const roleOptions = ref<RoleOption[]>([])
const platformMaxFileMb = 500

const GROUP_CODES = [
  'site', 'session', 'file', 'rateLimit', 'login', 'register', 'thirdParty', 'payment', 'security',
] as const satisfies readonly ConfigGroupCode[]

const defaultWechatNotifyUrl = computed(() => `${window.location.origin}/api/pay/notify/wechat`)
const defaultAlipayNotifyUrl = computed(() => `${window.location.origin}/api/pay/notify/alipay`)

const paymentTesting = ref(false)
const showPaymentModal = ref(false)
const payOrderStatus = ref('PENDING')
const payStatusRefreshing = ref(false)
const paymentResult = ref({
  type: '' as 'wechat' | 'alipay' | '',
  orderNo: '',
  qrcode: '',
  payUrl: '',
})
let payPollTimer: ReturnType<typeof setInterval> | null = null

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
    registerPerIpMinute: 10
  },
  login: {
    captchaEnabled: true,
    captchaType: 'image',
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
    minPasswordLength: 6
  },
  thirdParty: {
    wechat: { enabled: false, appId: '', appSecret: '' },
    alipay: { enabled: false, appId: '', privateKey: '', publicKey: '' },
    github: { enabled: false, clientId: '', clientSecret: '' },
    google: { enabled: false, clientId: '', clientSecret: '', redirectUri: '' },
  },
  payment: {
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
  },
  security: {
    disableDevtool: false,
    isConcurrent: false
  }
} satisfies ConfigGroupMap

type ConfigState = ConfigGroupMap

function cloneConfig<T>(data: T): T {
  return JSON.parse(JSON.stringify(data)) as T
}

function setConfigGroup<K extends ConfigGroupCode>(
  state: ConfigState,
  code: K,
  value: ConfigState[K]
) {
  state[code] = value
}

/** 已持久化到数据库的配置快照（仅保存成功或加载后更新） */
const savedSnapshot = reactive(cloneConfig(DEFAULTS) as ConfigState)
/** 页面编辑草稿，修改不会写入数据库 */
const draft = reactive(cloneConfig(DEFAULTS) as ConfigState)

function normalizePayload<K extends ConfigGroupCode>(code: K, payload: ConfigGroupMap[K]): ConfigGroupMap[K] {
  if (code === 'login') {
    const login = payload as ConfigGroupMap['login']
    return (login.captchaEnabled ? login : { ...login, captchaType: 'image' }) as ConfigGroupMap[K]
  }
  if (code === 'register') {
    const register = payload as ConfigGroupMap['register']
    return (register.captchaEnabled ? register : { ...register, captchaType: 'image' }) as ConfigGroupMap[K]
  }
  return payload
}

/** 草稿是否与已保存快照不一致（watchEffect 追踪深层字段，避免开关/数字框修改后按钮仍禁用） */
const isDirty = ref(false)

/** 禁止多端 = isConcurrent 取反，与 Sa-Token 字段对齐 */
const forbidConcurrentLogin = computed({
  get: () => !draft.security.isConcurrent,
  set: (value: boolean) => {
    draft.security.isConcurrent = !value
  }
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

function parseJson(str: string | undefined): unknown {
  try {
    return JSON.parse(str || '{}')
  } catch {
    return {}
  }
}

function applyGroupFromServer<K extends ConfigGroupCode>(code: K, serverJson: Partial<ConfigGroupMap[K]>) {
  const merged = { ...DEFAULTS[code], ...serverJson }
  // 必须为 draft / savedSnapshot 各克隆一份，否则共享引用会导致编辑时快照被同步改掉
  setConfigGroup(savedSnapshot, code, cloneConfig(merged))
  setConfigGroup(draft, code, cloneConfig(merged))
}

async function loadGroup(code: ConfigGroupCode) {
  try {
    const res = await getConfigGroup(code, { silent403: true })
    const serverJson = res.data?.configValue ? parseJson(res.data.configValue) : {}
    applyGroupFromServer(code, serverJson as Partial<ConfigGroupMap[typeof code]>)
  } catch {
    applyGroupFromServer(code, {})
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
    roleOptions.value = (res.data || []).map((r) => ({ name: r.name, code: r.code }))
  } catch {
    roleOptions.value = [{ name: '普通用户', code: 'user' }]
  }
}

async function loadAll() {
  loading.value = true
  let forbidden = false
  const results = await Promise.allSettled([
    ...GROUP_CODES.map((code) => loadGroup(code)),
    loadRoles()
  ])
  for (const r of results) {
    if (r.status === 'rejected') {
      const msg = String(r.reason?.message || '')
      if (msg.includes('权限不足')) forbidden = true
    }
  }
  if (forbidden) {
    ElMessage.warning('部分配置无查看权限，请联系管理员')
  }
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
  if (!isDirty.value) {
    ElMessage.info('暂无修改，无需保存')
    return
  }
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

function confirmLeave() {
  return ElMessageBox.confirm('当前有未保存的修改，确定离开吗？', '提示', {
    confirmButtonText: '离开',
    cancelButtonText: '继续编辑',
    type: 'warning'
  })
}

onBeforeRouteLeave(async () => {
  if (!isDirty.value) return true
  try {
    await confirmLeave()
    return true
  } catch {
    return false
  }
})

onMounted(loadAll)

function stopPayPolling() {
  if (payPollTimer) {
    clearInterval(payPollTimer)
    payPollTimer = null
  }
}

async function pollPayOrderStatus(manual = false) {
  if (!paymentResult.value.orderNo) return
  if (manual) payStatusRefreshing.value = true
  try {
    const res = await getPayOrderStatus(paymentResult.value.orderNo)
    payOrderStatus.value = res.data?.status || 'PENDING'
    if (payOrderStatus.value === 'PAID') {
      stopPayPolling()
      ElMessage.success('支付成功')
    } else if (manual) {
      ElMessage.info('尚未检测到支付成功，请确认已扫码付款后再试')
    }
  } catch {
    if (manual) {
      ElMessage.warning('查询失败，请稍后重试')
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

async function handleTestPayment(type: 'wechat' | 'alipay') {
  if (isDirty.value) {
    ElMessage.warning('请先保存支付配置，再生成测试订单')
    return
  }
  paymentTesting.value = true
  try {
    const res = await testPayment(type)
    paymentResult.value = {
      type,
      orderNo: res.data.orderNo,
      qrcode: res.data.qrcode || '',
      payUrl: res.data.payUrl || '',
    }
    showPaymentModal.value = true
  } catch (e) {
    ElMessage.error(getErrorMessage(e) || '创建测试订单失败')
  } finally {
    paymentTesting.value = false
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.dirty-hint {
  font-size: 13px;
  color: #e6a23c;
}
.config-form {
  max-width: 640px;
  padding-top: 8px;
}
.unit {
  margin-left: 8px;
  color: #909399;
  font-size: 13px;
}
.footer-actions {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
.config-form.wide-form {
  max-width: 720px;
}
.pay-collapse {
  max-width: 760px;
}
.pay-tip-alert {
  max-width: 760px;
  margin-top: 16px;
}
.payment-test-modal {
  text-align: center;
}
.payment-test-modal .payment-info {
  margin-bottom: 20px;
  text-align: left;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}
.payment-test-modal .payment-info p {
  margin: 8px 0;
  color: #374151;
}
.payment-test-modal .amount {
  font-size: 24px;
  font-weight: 600;
  color: #ef4444;
}
.payment-test-modal .qrcode-container {
  padding: 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  display: inline-block;
}
.payment-test-modal .qrcode-img {
  width: 200px;
  height: 200px;
}
.payment-test-modal .qrcode-tip {
  margin-top: 12px;
  color: #6b7280;
  font-size: 14px;
}
.payment-test-modal .payment-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
