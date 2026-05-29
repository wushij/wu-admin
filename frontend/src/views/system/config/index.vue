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
      </el-tabs>

      <div v-if="canEdit" class="footer-actions">
        <el-button @click="handleReset">重置</el-button>
        <el-button type="primary" :loading="saving" :disabled="!isDirty" @click="handleSave">保存全部</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getConfigGroup, updateConfigGroup } from '@/api/system/config'
import { getRoleList } from '@/api/system/role'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const canEdit = computed(() => (userStore.userInfo?.permissions || []).includes('system:config:update'))
const activeTab = ref('site')
const loading = ref(false)
const saving = ref(false)
const roleOptions = ref([])
const platformMaxFileMb = 500

const GROUP_CODES = ['site', 'session', 'file', 'rateLimit', 'login', 'register']

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
  }
}

function cloneConfig(data) {
  return JSON.parse(JSON.stringify(data))
}

/** 已持久化到数据库的配置快照（仅保存成功或加载后更新） */
const savedSnapshot = reactive(cloneConfig(DEFAULTS))
/** 页面编辑草稿，修改不会写入数据库 */
const draft = reactive(cloneConfig(DEFAULTS))

const isDirty = computed(() =>
  GROUP_CODES.some((code) => JSON.stringify(draft[code]) !== JSON.stringify(savedSnapshot[code]))
)

function parseJson(str) {
  try {
    return JSON.parse(str || '{}')
  } catch {
    return {}
  }
}

function applyGroupFromServer(code, serverJson) {
  const merged = { ...DEFAULTS[code], ...serverJson }
  savedSnapshot[code] = cloneConfig(merged)
  draft[code] = cloneConfig(merged)
}

async function loadGroup(code) {
  const res = await getConfigGroup(code, { silent403: true })
  const serverJson = res.data?.configValue ? parseJson(res.data.configValue) : {}
  applyGroupFromServer(code, serverJson)
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
}

function handleReset() {
  for (const code of GROUP_CODES) {
    draft[code] = cloneConfig(savedSnapshot[code])
  }
  ElMessage.info('已恢复为上次保存的配置')
}

function normalizePayload(code, payload) {
  const next = { ...payload }
  if (code === 'login' && !next.captchaEnabled) {
    next.captchaType = 'image'
  }
  if (code === 'register' && !next.captchaEnabled) {
    next.captchaType = 'image'
  }
  return next
}

async function handleSave() {
  if (!draft.site.platformName?.trim()) {
    ElMessage.warning('请填写平台名称')
    activeTab.value = 'site'
    return
  }
  saving.value = true
  try {
    for (const code of GROUP_CODES) {
      const payload = normalizePayload(code, cloneConfig(draft[code]))
      await updateConfigGroup(code, JSON.stringify(payload))
      savedSnapshot[code] = cloneConfig(payload)
      draft[code] = cloneConfig(payload)
    }
    ElMessage.success('保存成功，配置已生效')
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '保存失败')
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

onBeforeRouteLeave((_to, _from, next) => {
  if (!isDirty.value) {
    next()
    return
  }
  confirmLeave()
    .then(() => next())
    .catch(() => next(false))
})

onMounted(loadAll)
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
</style>
