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
      <el-col :xs="24" :lg="16" class="profile-col-left">
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
              <div class="tab-intro">
                <el-icon><Lock /></el-icon>
                <span>定期更换密码可提升账号安全性，密码长度不少于 {{ minPwdLen }} 位。</span>
              </div>
              <el-form
                ref="pwdFormRef"
                :model="pwdForm"
                :rules="pwdRules"
                label-width="100px"
                class="pwd-form"
                @submit.prevent
              >
                <el-form-item label="当前密码" prop="oldPassword">
                  <el-input
                    v-model="pwdForm.oldPassword"
                    type="password"
                    placeholder="请输入当前密码"
                    show-password
                    autocomplete="current-password"
                  />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input
                    v-model="pwdForm.newPassword"
                    type="password"
                    placeholder="请输入新密码"
                    show-password
                    autocomplete="new-password"
                  />
                  <div v-if="pwdForm.newPassword" class="pwd-strength">
                    <div class="pwd-strength-bar">
                      <div class="pwd-strength-fill" :class="pwdStrength.level" :style="{ width: pwdStrength.percent + '%' }" />
                    </div>
                    <span class="pwd-strength-text" :class="pwdStrength.level">{{ pwdStrength.label }}</span>
                  </div>
                </el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input
                    v-model="pwdForm.confirmPassword"
                    type="password"
                    placeholder="请再次输入新密码"
                    show-password
                    autocomplete="new-password"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="savingPwd" @click="handleChangePassword">
                    修改密码
                  </el-button>
                  <el-button @click="resetPwdForm">清空</el-button>
                </el-form-item>
              </el-form>
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
      <el-col :xs="24" :lg="8" class="profile-col-right">
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  User, Lock, Clock, Camera, Postcard, InfoFilled,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import {
  getProfile, updateProfile, changePassword, uploadAvatar, getMyLoginLogs,
} from '@/api/system/profile'
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

const pwdStrength = computed(() => {
  const pwd = pwdForm.newPassword
  if (!pwd) return { level: 'weak', label: '', percent: 0 }
  let score = 0
  if (pwd.length >= minPwdLen.value) score++
  if (pwd.length >= minPwdLen.value + 4) score++
  if (/[A-Z]/.test(pwd) && /[a-z]/.test(pwd)) score++
  if (/\d/.test(pwd)) score++
  if (/[^A-Za-z0-9]/.test(pwd)) score++
  if (score <= 2) return { level: 'weak', label: '弱', percent: 33 }
  if (score <= 3) return { level: 'medium', label: '中', percent: 66 }
  return { level: 'strong', label: '强', percent: 100 }
})

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
  await loadProfile()
  if (activeTab.value === 'logs') {
    await loadLoginLogs()
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
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 24px;
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
  gap: 32px;
  padding: 32px 36px;
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
  border: 3px solid rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.15);
  font-size: 32px;
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
  border-radius: 12px;
  border: 1px solid var(--theme-border, #e5e7eb);
  margin-bottom: 20px;
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

  .profile-col-right .tips-card {
    margin-bottom: 0;
  }
}

.content-card :deep(.el-card__body) {
  padding: 8px 24px 24px;
}

.profile-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.profile-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
}

.tab-intro {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  margin-bottom: 24px;
  background: var(--theme-bg, #f9fafb);
  border-radius: 8px;
  font-size: 13px;
  color: var(--theme-text-secondary, #6b7280);
  line-height: 1.5;
}

.tab-intro .el-icon {
  color: var(--theme-primary, #111827);
  flex-shrink: 0;
}

.info-form,
.pwd-form {
  max-width: 520px;
}

.pwd-strength {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}

.pwd-strength-bar {
  flex: 1;
  height: 4px;
  background: #e5e7eb;
  border-radius: 2px;
  overflow: hidden;
}

.pwd-strength-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s, background 0.3s;
}

.pwd-strength-fill.weak { background: #ef4444; }
.pwd-strength-fill.medium { background: #f59e0b; }
.pwd-strength-fill.strong { background: #10b981; }

.pwd-strength-text {
  font-size: 12px;
  min-width: 16px;
}

.pwd-strength-text.weak { color: #ef4444; }
.pwd-strength-text.medium { color: #f59e0b; }
.pwd-strength-text.strong { color: #10b981; }

.logs-table {
  border-radius: 8px;
  overflow: hidden;
}

.logs-pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

/* 右侧面板 */
.side-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
  color: var(--theme-text-base, #1f2937);
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
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
  font-size: 14px;
}

.meta-list li:last-child {
  border-bottom: none;
}

.meta-label {
  color: var(--theme-text-secondary, #6b7280);
  flex-shrink: 0;
}

.meta-value {
  color: var(--theme-text-base, #1f2937);
  text-align: right;
  word-break: break-all;
}

.tips-list {
  margin: 0;
  padding: 0 0 0 18px;
  font-size: 13px;
  color: var(--theme-text-secondary, #6b7280);
  line-height: 1.8;
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
