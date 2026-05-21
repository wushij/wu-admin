<template>
  <div class="app-container">
    <el-card v-loading="loading">
      <template #header>
        <span>系统配置</span>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="基础信息" name="site">
          <el-form :model="siteForm" label-width="120px" class="config-form">
            <el-form-item label="平台名称">
              <el-input v-model="siteForm.platformName" maxlength="50" />
            </el-form-item>
            <el-form-item label="平台副标题">
              <el-input v-model="siteForm.platformSubtitle" maxlength="80" />
            </el-form-item>
            <el-form-item label="登录页标题">
              <el-input v-model="siteForm.loginWelcome" maxlength="30" />
            </el-form-item>
            <el-form-item label="注册页标题">
              <el-input v-model="siteForm.registerTitle" maxlength="30" />
            </el-form-item>
            <el-form-item label="页脚版权">
              <el-input v-model="siteForm.copyright" maxlength="120" placeholder="选填" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="会话令牌" name="session">
          <el-form :model="sessionForm" label-width="120px" class="config-form">
            <el-form-item label="Token 有效期">
              <el-input-number v-model="sessionForm.tokenExpireHours" :min="1" :max="720" />
              <span class="unit">小时</span>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="文件存储" name="file">
          <el-form :model="fileForm" label-width="120px" class="config-form">
            <el-form-item label="单文件上限">
              <el-input-number v-model="fileForm.maxSizeMb" :min="1" :max="platformMaxFileMb" />
              <span class="unit">MB（保存后立即生效，最高 {{ platformMaxFileMb }}）</span>
            </el-form-item>
            <el-form-item label="允许扩展名">
              <el-input
                v-model="fileForm.allowedExtensions"
                type="textarea"
                :rows="3"
                placeholder="逗号分隔，如 jpg,png,pdf"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="接口限流" name="rateLimit">
          <el-form :model="rateLimitForm" label-width="140px" class="config-form">
            <el-form-item label="验证码(次/分钟/IP)">
              <el-input-number v-model="rateLimitForm.captchaPerIpMinute" :min="0" :max="200" />
            </el-form-item>
            <el-form-item label="登录(次/分钟/IP)">
              <el-input-number v-model="rateLimitForm.loginPerIpMinute" :min="0" :max="200" />
            </el-form-item>
            <el-form-item label="注册(次/分钟/IP)">
              <el-input-number v-model="rateLimitForm.registerPerIpMinute" :min="0" :max="200" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="登录认证" name="login">
          <el-form :model="loginForm" label-width="120px" class="config-form">
            <el-form-item label="启用验证码">
              <el-switch v-model="loginForm.captchaEnabled" />
            </el-form-item>
            <el-form-item v-if="loginForm.captchaEnabled" label="验证码类型">
              <el-radio-group v-model="loginForm.captchaType">
                <el-radio label="image">图片</el-radio>
                <el-radio label="slider">滑块</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="记住我">
              <el-switch v-model="loginForm.rememberMe" />
            </el-form-item>
            <el-form-item label="最大重试次数">
              <el-input-number v-model="loginForm.maxRetryCount" :min="1" :max="20" />
            </el-form-item>
            <el-form-item label="锁定时长">
              <el-input-number v-model="loginForm.lockTime" :min="1" :max="120" />
              <span class="unit">分钟</span>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册认证" name="register">
          <el-form :model="registerForm" label-width="120px" class="config-form">
            <el-form-item label="开放注册">
              <el-switch v-model="registerForm.enabled" />
            </el-form-item>
            <el-form-item label="注册验证码">
              <el-switch v-model="registerForm.captchaEnabled" :disabled="!registerForm.enabled" />
            </el-form-item>
            <el-form-item label="密码最小长度">
              <el-input-number
                v-model="registerForm.minPasswordLength"
                :min="6"
                :max="32"
                :disabled="!registerForm.enabled"
              />
            </el-form-item>
            <el-form-item label="默认角色">
              <el-select
                v-model="registerForm.defaultRoleCode"
                :disabled="!registerForm.enabled"
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
              <el-switch v-model="registerForm.needAudit" :disabled="!registerForm.enabled" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="footer-actions">
        <el-button @click="loadAll">重置</el-button>
        <el-button v-permission="'system:config:update'" type="primary" :loading="saving" @click="handleSave">
          保存全部
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfigGroup, updateConfigGroup } from '@/api/system/config'
import { getRoleList } from '@/api/system/role'

const activeTab = ref('site')
const loading = ref(false)
const saving = ref(false)
const roleOptions = ref([])
const platformMaxFileMb = 500

const siteForm = reactive({
  platformName: 'Admin Platform',
  platformSubtitle: '统一运维 · 高效管控',
  loginWelcome: 'Welcome',
  registerTitle: 'Sign Up',
  copyright: ''
})

const sessionForm = reactive({ tokenExpireHours: 24 })

const fileForm = reactive({
  maxSizeMb: 50,
  allowedExtensions:
    'jpg,jpeg,png,gif,webp,bmp,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,json,xml,zip,rar,mp4,mp3,wav,avi,mov'
})

const rateLimitForm = reactive({
  captchaPerIpMinute: 40,
  loginPerIpMinute: 30,
  registerPerIpMinute: 10
})

const loginForm = reactive({
  captchaEnabled: true,
  captchaType: 'image',
  rememberMe: true,
  maxRetryCount: 5,
  lockTime: 10
})

const registerForm = reactive({
  enabled: true,
  captchaEnabled: true,
  defaultRoleCode: 'user',
  needAudit: false,
  minPasswordLength: 6
})

const GROUP_MAP = {
  site: siteForm,
  session: sessionForm,
  file: fileForm,
  rateLimit: rateLimitForm,
  login: loginForm,
  register: registerForm
}

function parseJson(str) {
  try {
    return JSON.parse(str || '{}')
  } catch {
    return {}
  }
}

async function loadGroup(code) {
  const res = await getConfigGroup(code)
  if (res.data?.configValue) {
    Object.assign(GROUP_MAP[code], parseJson(res.data.configValue))
  }
}

async function loadRoles() {
  try {
    const res = await getRoleList({ status: 1 })
    roleOptions.value = (res.data || []).map((r) => ({ name: r.name, code: r.code }))
  } catch {
    roleOptions.value = [{ name: '普通用户', code: 'user' }]
  }
}

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([
      ...Object.keys(GROUP_MAP).map((code) => loadGroup(code)),
      loadRoles()
    ])
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '加载失败，请执行 sql/admin_platform.sql 文末升级段')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!siteForm.platformName?.trim()) {
    ElMessage.warning('请填写平台名称')
    activeTab.value = 'site'
    return
  }
  saving.value = true
  try {
    for (const code of Object.keys(GROUP_MAP)) {
      const payload = { ...GROUP_MAP[code] }
      if (code === 'login' && !payload.captchaEnabled) {
        payload.captchaType = 'image'
      }
      await updateConfigGroup(code, JSON.stringify(payload))
    }
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
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
