<template>
  <div class="profile-page" v-loading="pageLoading">
    <!-- 顶部资料横幅 -->
    <div class="profile-hero">
      <div class="hero-bg-pattern" />
      <div class="hero-content">
        <div class="hero-left">
          <div class="avatar-uploader" @click="triggerAvatarUpload">
            <el-avatar :size="88" :src="avatarSrc" class="hero-avatar">
              {{ avatarFallback }}
            </el-avatar>
            <div class="avatar-mask">
              <el-icon :size="22"><Camera /></el-icon>
              <span>更换头像</span>
            </div>
            <input
              ref="avatarInputRef"
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              class="avatar-input"
              @change="handleAvatarChange"
            />
          </div>
          <div class="hero-info">
            <h1 class="hero-name">{{ profile.nickname || profile.username || '用户' }}</h1>
            <p class="hero-username">@{{ profile.username }}</p>
            <div class="hero-tags">
              <el-tag
                v-for="role in profile.roleNames || []"
                :key="role"
                effect="dark"
                round
                class="role-tag"
              >
                {{ role }}
              </el-tag>
              <el-tag :type="statusTagType" effect="plain" round>
                {{ statusLabel }}
              </el-tag>
            </div>
          </div>
        </div>
        <div class="hero-stats">
          <div class="stat-item">
            <div class="stat-value">{{ profile.deptName || '未分配' }}</div>
            <div class="stat-label">所属部门</div>
          </div>
          <div class="stat-divider" />
          <div class="stat-item">
            <div class="stat-value">{{ postDisplay }}</div>
            <div class="stat-label">岗位</div>
          </div>
          <div class="stat-divider" />
          <div class="stat-item">
            <div class="stat-value">{{ lastLoginDisplay }}</div>
            <div class="stat-label">最近登录</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <el-row :gutter="24" class="profile-main">
      <el-col :xs="24" :lg="18" class="profile-col-left">
        <el-card class="content-card" shadow="never">
          <el-tabs v-model="activeTab" class="profile-tabs">
            <!-- 基本资料 -->
            <el-tab-pane label="基本资料" name="info">
              <div class="tab-intro">
                <el-icon><User /></el-icon>
                <span>维护您的昵称与联系方式，便于同事识别与系统通知触达。</span>
              </div>
              <el-form
                ref="infoFormRef"
                :model="infoForm"
                :rules="infoRules"
                label-width="88px"
                class="info-form"
                @submit.prevent
              >
                <el-form-item label="用户名">
                  <el-input :model-value="profile.username" disabled />
                </el-form-item>
                <el-form-item label="昵称" prop="nickname">
                  <el-input v-model="infoForm.nickname" placeholder="请输入昵称" maxlength="30" show-word-limit />
                </el-form-item>
                <el-form-item label="手机号" prop="mobile">
                  <el-input v-model="infoForm.mobile" placeholder="请输入手机号" maxlength="11" clearable />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="infoForm.email" placeholder="请输入邮箱" maxlength="100" clearable />
                </el-form-item>
                <el-form-item label="所属部门">
                  <el-input :model-value="profile.deptName || '未分配'" disabled />
                </el-form-item>
                <el-form-item label="岗位">
                  <el-input :model-value="postDisplay" disabled />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="savingInfo" @click="handleSaveInfo">
                    保存资料
                  </el-button>
                  <el-button @click="resetInfoForm">重置</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- 安全设置 -->
            <el-tab-pane label="安全设置" name="security">
              <div class="tab-intro tab-intro--security">
                <el-icon><Lock /></el-icon>
                <span>定期更换密码可提升账号安全性，密码长度不少于 {{ minPwdLen }} 位。</span>
              </div>

              <div v-if="securityMode === 'password'" class="security-form-card">
                <el-form
                  ref="pwdFormRef"
                  :model="pwdForm"
                  :rules="pwdRules"
                  label-position="top"
                  class="pwd-form pwd-form--modern"
                  @submit.prevent
                >
                  <el-form-item prop="oldPassword" class="pwd-field">
                    <template #label>
                      <div class="pwd-field-label-row">
                        <span class="pwd-field-label">当前密码</span>
                        <button
                          v-if="canUseSmsReset"
                          type="button"
                          class="pwd-forgot-btn"
                          @click="openSmsResetMode"
                        >
                          忘记密码
                        </button>
                        <span v-else-if="!hasBoundMobile" class="pwd-forgot-hint">重置密码需先绑定手机号</span>
                      </div>
                    </template>
                    <el-input
                      v-model="pwdForm.oldPassword"
                      type="password"
                      placeholder="请输入当前密码"
                      show-password
                      autocomplete="current-password"
                    />
                  </el-form-item>
                  <el-form-item label="新密码" prop="newPassword" class="pwd-field">
                    <el-input
                      v-model="pwdForm.newPassword"
                      type="password"
                      placeholder="请输入新密码"
                      show-password
                      autocomplete="new-password"
                    />
                  </el-form-item>
                  <el-form-item label="确认新密码" prop="confirmPassword" class="pwd-field">
                    <el-input
                      v-model="pwdForm.confirmPassword"
                      type="password"
                      placeholder="请再次输入新密码"
                      show-password
                      autocomplete="new-password"
                    />
                  </el-form-item>
                  <div class="pwd-form-actions">
                    <el-button type="primary" :loading="savingPwd" @click="handleChangePassword">
                      修改密码
                    </el-button>
                    <el-button @click="resetPwdForm">清空</el-button>
                  </div>
                </el-form>
              </div>

              <div v-else class="security-form-card security-form-card--sms">
                <div class="sms-form-header">
                  <button type="button" class="sms-back-btn" @click="closeSmsResetMode">
                    <el-icon><ArrowLeft /></el-icon>
                    返回
                  </button>
                  <span class="sms-form-header__title">短信验证重置</span>
                </div>
                <div class="sms-reset-banner">
                  <div class="sms-reset-banner__icon">
                    <el-icon><Iphone /></el-icon>
                  </div>
                  <div class="sms-reset-banner__body">
                    <p class="sms-reset-banner__label">验证码将发送至</p>
                    <p class="sms-reset-banner__phone">{{ maskedMobile }}</p>
                  </div>
                </div>
                <el-form
                  ref="smsPwdFormRef"
                  :model="smsPwdForm"
                  :rules="smsPwdRules"
                  label-position="top"
                  class="pwd-form pwd-form--modern"
                  @submit.prevent
                >
                  <el-form-item label="短信验证码" prop="smsCode" class="pwd-field">
                    <div class="sms-code-row">
                      <el-input
                        v-model="smsPwdForm.smsCode"
                        placeholder="请输入 6 位验证码"
                        maxlength="6"
                        autocomplete="off"
                      />
                      <el-button
                        type="primary"
                        plain
                        class="sms-send-btn"
                        :disabled="smsCountdown > 0 || sendingSmsCode || !canUseSmsReset"
                        :loading="sendingSmsCode"
                        @click="handleSendResetSmsCode"
                      >
                        {{ smsCountdown > 0 ? `${smsCountdown}s` : '获取验证码' }}
                      </el-button>
                    </div>
                  </el-form-item>
                  <el-form-item label="新密码" prop="newPassword" class="pwd-field">
                    <el-input
                      v-model="smsPwdForm.newPassword"
                      type="password"
                      placeholder="请输入新密码"
                      show-password
                      autocomplete="new-password"
                    />
                  </el-form-item>
                  <el-form-item label="确认新密码" prop="confirmPassword" class="pwd-field">
                    <el-input
                      v-model="smsPwdForm.confirmPassword"
                      type="password"
                      placeholder="请再次输入新密码"
                      show-password
                      autocomplete="new-password"
                    />
                  </el-form-item>
                  <div class="pwd-form-actions">
                    <el-button type="primary" :loading="savingSmsPwd" @click="handleSmsResetPassword">
                      确认重置
                    </el-button>
                    <el-button @click="resetSmsPwdForm">清空</el-button>
                  </div>
                </el-form>
              </div>
            </el-tab-pane>

            <!-- 登录记录 -->
            <el-tab-pane label="登录记录" name="logs">
              <div class="tab-intro">
                <el-icon><Clock /></el-icon>
                <span>查看您账号的最近登录活动，发现异常请及时修改密码。</span>
              </div>
              <el-table
                :data="loginLogs"
                v-loading="logsLoading"
                stripe
                class="logs-table"
                :header-cell-style="{ textAlign: 'center', background: '#f9fafb' }"
                :cell-style="{ textAlign: 'center' }"
              >
                <el-table-column prop="loginTime" label="登录时间" width="180" />
                <el-table-column prop="ipaddr" label="IP 地址" width="140" />
                <el-table-column prop="loginLocation" label="登录地点" min-width="120" show-overflow-tooltip />
                <el-table-column prop="browser" label="浏览器" width="120" show-overflow-tooltip />
                <el-table-column prop="os" label="操作系统" width="120" show-overflow-tooltip />
                <el-table-column prop="status" label="结果" width="90">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small" effect="plain">
                      {{ row.status === 0 ? '成功' : '失败' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <el-pagination
                v-if="logsTotal > 0"
                v-model:current-page="logsQuery.pageNo"
                v-model:page-size="logsQuery.pageSize"
                :total="logsTotal"
                :page-sizes="[5, 10, 20]"
                layout="total, prev, pager, next"
                class="logs-pagination"
                @current-change="loadLoginLogs"
                @size-change="loadLoginLogs"
              />
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>

      <!-- 右侧信息面板 -->
      <el-col :xs="24" :lg="6" class="profile-col-right">
        <el-card class="side-card account-card" shadow="never">
          <template #header>
            <div class="side-card-header">
              <el-icon><Postcard /></el-icon>
              <span>账号概览</span>
            </div>
          </template>
          <ul class="meta-list">
            <li>
              <span class="meta-label">账号 ID</span>
              <span class="meta-value">{{ profile.userId ?? '—' }}</span>
            </li>
            <li>
              <span class="meta-label">注册时间</span>
              <span class="meta-value">{{ formatTime(profile.createTime) }}</span>
            </li>
            <li>
              <span class="meta-label">资料更新</span>
              <span class="meta-value">{{ formatTime(profile.updateTime) }}</span>
            </li>
            <li>
              <span class="meta-label">最近登录 IP</span>
              <span class="meta-value">{{ profile.lastLoginIp || '—' }}</span>
            </li>
            <li>
              <span class="meta-label">登录地点</span>
              <span class="meta-value">{{ profile.lastLoginLocation || '—' }}</span>
            </li>
          </ul>
        </el-card>

        <el-card class="side-card tips-card" shadow="never">
          <template #header>
            <div class="side-card-header">
              <el-icon><InfoFilled /></el-icon>
              <span>安全提示</span>
            </div>
          </template>
          <ul class="tips-list">
            <li>请勿将账号密码告知他人或在公共设备勾选「记住我」。</li>
            <li>发现陌生登录记录时，请立即修改密码并联系管理员。</li>
            <li>部门、岗位、角色由管理员分配，如需调整请联系系统管理员。</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>

    <SliderCaptcha v-model:show="showSliderModal" @success="onSliderSuccess" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  User, Lock, Clock, Camera, Postcard, InfoFilled, Iphone, ArrowLeft,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import {
  getProfile, updateProfile, changePassword, uploadAvatar, getMyLoginLogs,
  sendProfilePasswordSmsCode, resetPasswordBySms,
} from '@/api/system/profile'
import { getConfig } from '@/api/system/auth'
import SliderCaptcha from '@/components/SliderCaptcha.vue'
import type { UserProfile } from '@/types/profile'
import type { LoginLogVO } from '@/api/system/login-log'

const userStore = useUserStore()

const activeTab = ref('info')
const profile = ref<UserProfile>({} as UserProfile)
const pageLoading = ref(false)
const savingInfo = ref(false)
const savingPwd = ref(false)
const avatarInputRef = ref<HTMLInputElement>()
const infoFormRef = ref<FormInstance>()
const pwdFormRef = ref<FormInstance>()
const smsPwdFormRef = ref<FormInstance>()

const securityMode = ref<'password' | 'sms'>('password')
const smsEnabled = ref(false)
const showSliderModal = ref(false)
const sendingSmsCode = ref(false)
const savingSmsPwd = ref(false)
const smsCountdown = ref(0)
let smsTimer: ReturnType<typeof setInterval> | null = null

const infoForm = reactive({
  nickname: '',
  mobile: '',
  email: '',
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const smsPwdForm = reactive({
  smsCode: '',
  newPassword: '',
  confirmPassword: '',
})

const logsLoading = ref(false)
const loginLogs = ref<LoginLogVO[]>([])
const logsTotal = ref(0)
const logsQuery = reactive({ pageNo: 1, pageSize: 8 })

const minPwdLen = computed(() => profile.value.minPasswordLength ?? 6)

const avatarSrc = computed(() => {
  const url = profile.value.avatar || userStore.userInfo.avatar
  if (!url) return undefined
  return url.startsWith('http') ? url : url
})

const avatarFallback = computed(() => {
  const name = profile.value.nickname || profile.value.username || 'U'
  return name.charAt(0).toUpperCase()
})

const statusLabel = computed(() => {
  const map: Record<number, string> = {
    0: '已停用',
    1: '正常',
    2: '待审核',
    3: '审核驳回',
  }
  return map[profile.value.status ?? 1] ?? '未知'
})

const statusTagType = computed(() => {
  const map: Record<number, 'success' | 'warning' | 'danger' | 'info'> = {
    0: 'danger',
    1: 'success',
    2: 'warning',
    3: 'danger',
  }
  return map[profile.value.status ?? 1] ?? 'info'
})

const postDisplay = computed(() => {
  const names = profile.value.postNames
  if (!names?.length) return '未分配'
  return names.join('、')
})

const lastLoginDisplay = computed(() => {
  if (!profile.value.lastLoginTime) return '暂无记录'
  return formatTime(profile.value.lastLoginTime, true)
})

const maskedMobile = computed(() => {
  const m = (profile.value.mobile || '').trim()
  if (!/^1[3-9]\d{9}$/.test(m)) return '未绑定手机号'
  return `${m.slice(0, 3)} **** ${m.slice(-4)}`
})

const hasBoundMobile = computed(() =>
  /^1[3-9]\d{9}$/.test((profile.value.mobile || '').trim()),
)

const canUseSmsReset = computed(
  () => smsEnabled.value && hasBoundMobile.value,
)

const infoRules: FormRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 30, message: '昵称长度 2～30 个字符', trigger: 'blur' },
  ],
  mobile: [
    { pattern: /^$|^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
}

const pwdRules = computed<FormRules>(() => ({
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: minPwdLen.value, message: `密码长度不能少于 ${minPwdLen.value} 位`, trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}))

const smsPwdRules = computed<FormRules>(() => ({
  smsCode: [
    { required: true, message: '请输入短信验证码', trigger: 'blur' },
    { pattern: /^\d{4,6}$/, message: '请输入正确的验证码', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: minPwdLen.value, message: `密码长度不能少于 ${minPwdLen.value} 位`, trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== smsPwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}))

function startSmsCountdown(seconds = 60) {
  if (smsTimer) {
    clearInterval(smsTimer)
    smsTimer = null
  }
  smsCountdown.value = seconds
  smsTimer = setInterval(() => {
    if (smsCountdown.value <= 1) {
      smsCountdown.value = 0
      if (smsTimer) {
        clearInterval(smsTimer)
        smsTimer = null
      }
    } else {
      smsCountdown.value -= 1
    }
  }, 1000)
}

async function loadSmsConfig() {
  try {
    const res = await getConfig()
    smsEnabled.value = res.data?.login?.smsEnabled === true
  } catch {
    smsEnabled.value = false
  }
}

function openSmsResetMode() {
  if (!smsEnabled.value) {
    ElMessage.warning('短信功能未启用，请联系管理员')
    return
  }
  if (!hasBoundMobile.value) {
    ElMessage.warning('请先在「基本资料」中绑定手机号')
    activeTab.value = 'info'
    return
  }
  securityMode.value = 'sms'
  resetSmsPwdForm()
}

function closeSmsResetMode() {
  securityMode.value = 'password'
  resetSmsPwdForm()
}

function resetSmsPwdForm() {
  smsPwdForm.smsCode = ''
  smsPwdForm.newPassword = ''
  smsPwdForm.confirmPassword = ''
  smsPwdFormRef.value?.clearValidate()
}

function handleSendResetSmsCode() {
  if (!canUseSmsReset.value || sendingSmsCode.value || smsCountdown.value > 0) return
  showSliderModal.value = true
}

async function onSliderSuccess() {
  sendingSmsCode.value = true
  try {
    await sendProfilePasswordSmsCode('slider_verified')
    ElMessage.success('验证码已发送')
    startSmsCountdown()
  } catch {
    // 错误提示由 request 拦截器统一弹出，避免重复
  } finally {
    sendingSmsCode.value = false
  }
}

async function handleSmsResetPassword() {
  const valid = await smsPwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  savingSmsPwd.value = true
  try {
    await resetPasswordBySms({
      smsCode: smsPwdForm.smsCode.trim(),
      newPassword: smsPwdForm.newPassword,
      confirmPassword: smsPwdForm.confirmPassword,
    })
    ElMessage.success('密码已重置，请使用新密码登录')
    resetSmsPwdForm()
    securityMode.value = 'password'
  } catch {
    // 错误提示由 request 拦截器统一弹出，避免重复
  } finally {
    savingSmsPwd.value = false
  }
}

function formatTime(time?: string, short = false) {
  if (!time) return '—'
  if (short) {
    return time.replace('T', ' ').slice(0, 16)
  }
  return time.replace('T', ' ').slice(0, 19)
}

function syncInfoForm() {
  infoForm.nickname = profile.value.nickname || ''
  infoForm.mobile = profile.value.mobile || ''
  infoForm.email = profile.value.email || ''
}

async function loadProfile() {
  pageLoading.value = true
  try {
    const res = await getProfile()
    profile.value = res.data
    syncInfoForm()
  } finally {
    pageLoading.value = false
  }
}

async function loadLoginLogs() {
  logsLoading.value = true
  try {
    const res = await getMyLoginLogs(logsQuery)
    loginLogs.value = res.data?.list || []
    logsTotal.value = res.data?.total || 0
  } finally {
    logsLoading.value = false
  }
}

function resetInfoForm() {
  syncInfoForm()
  infoFormRef.value?.clearValidate()
}

function resetPwdForm() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdFormRef.value?.clearValidate()
}

async function handleSaveInfo() {
  const valid = await infoFormRef.value?.validate().catch(() => false)
  if (!valid) return
  savingInfo.value = true
  try {
    await updateProfile({
      nickname: infoForm.nickname.trim(),
      mobile: infoForm.mobile.trim(),
      email: infoForm.email.trim(),
    })
    ElMessage.success('资料已保存')
    await loadProfile()
    userStore.patchUserInfo({
      nickname: infoForm.nickname.trim(),
    })
  } finally {
    savingInfo.value = false
  }
}

async function handleChangePassword() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  savingPwd.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword,
    })
    ElMessage.success('密码修改成功，请妥善保管新密码')
    resetPwdForm()
  } finally {
    savingPwd.value = false
  }
}

function triggerAvatarUpload() {
  avatarInputRef.value?.click()
}

async function handleAvatarChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像大小不能超过 2MB')
    return
  }
  try {
    const res = await uploadAvatar(file)
    const url = res.data?.url
    if (url) {
      profile.value.avatar = url
      userStore.patchUserInfo({ avatar: url })
      ElMessage.success('头像已更新')
    }
  } catch {
    /* request 拦截器已提示 */
  }
}

watch(activeTab, (tab) => {
  if (tab === 'logs') {
    loadLoginLogs()
  }
})

onMounted(async () => {
  await Promise.all([loadProfile(), loadSmsConfig()])
  if (activeTab.value === 'logs') {
    await loadLoginLogs()
  }
})

onUnmounted(() => {
  if (smsTimer) {
    clearInterval(smsTimer)
    smsTimer = null
  }
})
</script>

<style scoped>
.profile-page {
  min-height: 100%;
}

/* 顶部横幅 */
.profile-hero {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  margin-bottom: 20px;
  background: linear-gradient(135deg, var(--theme-primary, #111827) 0%, #374151 100%);
  color: #fff;
}

.hero-bg-pattern {
  position: absolute;
  inset: 0;
  opacity: 0.08;
  background-image: radial-gradient(circle at 20% 50%, #fff 1px, transparent 1px),
    radial-gradient(circle at 80% 20%, #fff 1px, transparent 1px);
  background-size: 40px 40px;
}

.hero-content {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 32px;
  flex-wrap: wrap;
}

.hero-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.avatar-uploader {
  position: relative;
  cursor: pointer;
  flex-shrink: 0;
}

.hero-avatar {
  width: 80px !important;
  height: 80px !important;
  border: 3px solid rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.15);
  font-size: 28px;
  font-weight: 600;
}

.avatar-mask {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.25s;
  font-size: 11px;
  color: #fff;
}

.avatar-uploader:hover .avatar-mask {
  opacity: 1;
}

.avatar-input {
  display: none;
}

.hero-name {
  margin: 0 0 4px;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.hero-username {
  margin: 0 0 12px;
  font-size: 14px;
  opacity: 0.75;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.role-tag {
  background: rgba(255, 255, 255, 0.2) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  color: #fff !important;
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 0;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px 24px;
  backdrop-filter: blur(8px);
}

.stat-item {
  text-align: center;
  padding: 0 20px;
  min-width: 100px;
}

.stat-value {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 140px;
}

.stat-label {
  font-size: 12px;
  opacity: 0.7;
}

.stat-divider {
  width: 1px;
  height: 36px;
  background: rgba(255, 255, 255, 0.2);
}

/* 主内容卡片 */
.profile-main {
  align-items: stretch;
}

.profile-col-left,
.profile-col-right {
  display: flex;
  flex-direction: column;
}

.content-card,
.side-card {
  border-radius: 10px;
  border: 1px solid var(--theme-border, #e5e7eb);
  margin-bottom: 16px;
}

@media (min-width: 992px) {
  .profile-col-left .content-card {
    flex: 1;
    margin-bottom: 0;
    display: flex;
    flex-direction: column;
  }

  .profile-col-left .content-card :deep(.el-card__body) {
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  .profile-tabs {
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  .profile-tabs :deep(.el-tabs__content) {
    flex: 1;
  }
}

.content-card :deep(.el-card__body) {
  padding: 12px 24px 24px;
}

.profile-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.profile-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
}

.tab-intro {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  margin-bottom: 20px;
  background: var(--theme-bg, #f9fafb);
  border-radius: 8px;
  font-size: 12.5px;
  color: var(--theme-text-secondary, #6b7280);
  line-height: 1.5;
}

.tab-intro .el-icon {
  color: var(--theme-primary, #111827);
  flex-shrink: 0;
}

.info-form,
.pwd-form {
  max-width: 680px;
}

.info-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.info-form :deep(.el-form-item__label) {
  font-size: 13.5px;
  font-weight: 500;
  color: #374151;
}

.tab-intro--security {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px solid #e2e8f0;
  color: #475569;
}

.security-form-card {
  max-width: 600px;
  padding: 28px 32px 24px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.06);
}

.pwd-form--modern {
  max-width: none;
}

.pwd-form--modern :deep(.el-form-item) {
  margin-bottom: 22px;
}

.pwd-form--modern :deep(.el-form-item__label) {
  display: flex !important;
  align-items: center;
  width: 100% !important;
  padding: 0;
  margin-bottom: 8px;
  line-height: 1.4;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.pwd-form--modern :deep(.el-form-item__label::before) {
  flex-shrink: 0;
  margin-right: 4px;
}

.pwd-form--modern :deep(.el-input__wrapper) {
  border-radius: 8px;
  padding: 4px 12px;
  box-shadow: 0 0 0 1px #d1d5db inset;
  transition: box-shadow 0.2s;
}

.pwd-form--modern :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #9ca3af inset;
}

.pwd-form--modern :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--theme-primary, #111827) inset;
}

.pwd-field-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  min-width: 0;
  padding-right: 0;
}

.pwd-field-label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.pwd-forgot-btn {
  padding: 0;
  border: none;
  background: transparent;
  font-size: 13px;
  font-weight: 500;
  color: var(--theme-primary, #111827);
  cursor: pointer;
  line-height: 1.4;
}

.pwd-forgot-btn:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
  opacity: 0.85;
}

.pwd-forgot-hint {
  font-size: 12px;
  font-weight: 400;
  color: #94a3b8;
  line-height: 1.4;
  white-space: nowrap;
}

.pwd-form-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
  padding-top: 20px;
  border-top: 1px solid #f3f4f6;
}

.pwd-form-actions .el-button--primary {
  min-width: 108px;
}

/* 短信重置 */
.security-form-card--sms {
  max-width: 600px;
}

.sms-form-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f3f4f6;
}

.sms-back-btn {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 6px 10px;
  margin-left: -10px;
  border: none;
  border-radius: 8px;
  background: #f3f4f6;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.sms-back-btn:hover {
  background: #e5e7eb;
  color: #111827;
}

.sms-back-btn .el-icon {
  font-size: 14px;
}

.sms-form-header__title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.sms-reset-banner {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
  padding: 16px 18px;
  border-radius: 10px;
  background: linear-gradient(135deg, #eff6ff 0%, #f0f9ff 100%);
  border: 1px solid #bfdbfe;
}

.sms-reset-banner__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: #fff;
  color: #2563eb;
  font-size: 22px;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.12);
}

.sms-reset-banner__label {
  margin: 0 0 4px;
  font-size: 12px;
  color: #64748b;
}

.sms-reset-banner__phone {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #0f172a;
}

.sms-code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.sms-code-row .el-input {
  flex: 1;
  min-width: 0;
}

.sms-send-btn {
  flex-shrink: 0;
  min-width: 108px;
}

.logs-table {
  border-radius: 8px;
  overflow: hidden;
}

.logs-pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

/* 右侧面板 */
.side-card {
  margin-bottom: 16px;
  border-radius: 10px;
  overflow: hidden;
}

.side-card :deep(.el-card__header) {
  padding: 14px 16px;
  border-bottom: 1px solid #f3f4f6;
}

.side-card :deep(.el-card__body) {
  padding: 12px 16px;
}

@media (min-width: 992px) {
  .profile-col-right .tips-card {
    margin-bottom: 0;
  }
}

.side-card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 14px;
  color: var(--theme-text-base, #1f2937);
  padding-bottom: 2px;
}

.meta-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.meta-list li {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
  padding: 10px 0;
  border-bottom: 1px dashed #e5e7eb;
  font-size: 13px;
}

.meta-list li:last-child {
  border-bottom: none;
}

.meta-label {
  color: var(--theme-text-secondary, #6b7280);
  flex-shrink: 0;
  font-weight: 500;
}

.meta-value {
  color: var(--theme-text-base, #1f2937);
  text-align: right;
  word-break: break-all;
}

.tips-list {
  margin: 0;
  padding: 0 0 0 16px;
  font-size: 12.5px;
  color: var(--theme-text-secondary, #6b7280);
  line-height: 1.7;
}

.tips-list li + li {
  margin-top: 8px;
}

@media (max-width: 768px) {
  .hero-content {
    padding: 24px 20px;
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-stats {
    width: 100%;
    justify-content: space-around;
    padding: 12px 8px;
  }

  .stat-item {
    padding: 0 8px;
    min-width: 0;
    flex: 1;
  }

  .stat-value {
    max-width: 100%;
    font-size: 13px;
  }
}
</style>
