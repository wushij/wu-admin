<template>
  <div class="login-page">
    <!-- 全屏粒子背景 -->
    <div class="sparkles-background">
      <!-- 第一层：大粒子，慢速，靛蓝色 -->
      <div class="particle-layer">
        <vue-particles
          id="sparkles-1"
          @particles-loaded="particlesLoaded"
          :options="authParticleOptions1"
        />
      </div>
      
      <!-- 第二层：中等粒子，中速，紫色 -->
      <div class="particle-layer">
        <vue-particles
          id="sparkles-2"
          @particles-loaded="particlesLoaded"
          :options="authParticleOptions2"
        />
      </div>
      
      <!-- 第三层：小粒子，快速，白色 -->
      <div class="particle-layer">
        <vue-particles
          id="sparkles-3"
          @particles-loaded="particlesLoaded"
          :options="authParticleOptions3"
        />
      </div>
      
      <!-- 径向渐变遮罩 -->
      <div class="gradient-overlay"></div>
    </div>



    <div class="login-container login-split">
      <!-- 左侧：品牌与全息科技地球 -->
      <aside class="login-brand">
        <div class="login-brand__inner">
          <div class="login-brand__visual">
            <div class="login-brand__earth3d" role="img" aria-label="3D 地球 · Admin Platform">
              <Earth3D transparent :show-stars="false" />
            </div>
          </div>
          <h1 class="login-brand__title">{{ sitePlatformName }}</h1>
          <p class="login-brand__tagline">{{ sitePlatformSubtitle }}</p>
        </div>
      </aside>

      <!-- 右侧：登录表单 -->
      <div class="login-form-pane">
        <div class="login-form-wrapper">
          <div class="login-form">
            <header class="login-form__head">
              <h2 class="login-form__title">{{ siteLoginWelcome }}</h2>
            </header>

            <el-form
              ref="formRef"
              :model="formData"
              :rules="formRules"
              :validate-on-rule-change="false"
              :class="['form-container', { 'form-container--submitted': submitAttempted }]"
              size="large"
            >
            <el-form-item prop="username" class="form-item">
              <el-input
                v-model="formData.username"
                name="username"
                autocomplete="username"
                placeholder="请输入用户名"
                maxlength="50"
                @keyup.enter="handleLogin"
                class="form-input"
              >
                <template #prefix>
                  <el-icon class="input-icon"><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            
            <el-form-item prop="password" class="form-item">
              <el-input
                v-model="formData.password"
                name="password"
                type="password"
                autocomplete="current-password"
                placeholder="请输入密码"
                show-password
                maxlength="50"
                @keyup.enter="handleLogin"
                class="form-input"
              >
                <template #prefix>
                  <el-icon class="input-icon"><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            
            <!-- 图片验证码（滑块模式不显示表单项） -->
            <el-form-item v-if="captchaEnabled && captchaType === 'image'" prop="code" class="form-item">
              <div class="captcha-row">
                <el-input
                  v-model="formData.code"
                  placeholder="请输入验证码"
                  maxlength="6"
                  @keyup.enter="handleLogin"
                  class="form-input captcha-input"
                >
                  <template #prefix>
                    <el-icon class="input-icon"><Key /></el-icon>
                  </template>
                </el-input>
                <img
                  v-if="captchaImg"
                  :src="captchaImg"
                  class="captcha-img"
                  @click="loadCaptcha"
                  title="点击刷新"
                />
                <el-skeleton v-else :rows="1" animated style="width: 120px; height: 48px" />
              </div>
            </el-form-item>
            
            <el-form-item v-if="rememberMeEnabled" class="form-item">
              <div class="login-options">
                <el-checkbox v-model="formData.rememberMe" class="remember-checkbox">记住我</el-checkbox>
                <el-link v-if="registerEnabled" type="primary" @click="goRegister" class="register-link">
                  没有账号？立即注册
                </el-link>
              </div>
            </el-form-item>
            
            <el-form-item class="form-item login-button-wrapper">
              <el-button
                type="primary"
                :loading="loading"
                @click="handleLogin"
                class="login-button"
                loading-text="登录中..."
              >
                登 录
              </el-button>
            </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
    </div>

    <SliderCaptcha v-model:show="showSliderModal" @success="doLogin" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getCaptcha, getConfig } from '@/api/system/auth'
import type { LoginForm } from '@/types/api'
import {
  authParticleOptions1,
  authParticleOptions2,
  authParticleOptions3,
} from '@/constants/authParticles'
import { getErrorMessage } from '@/utils/axiosError'
import SliderCaptcha from '@/components/SliderCaptcha.vue'
import Earth3D from '@/components/earth/Earth3D.vue'

type CaptchaMode = 'image' | 'slider'

interface LoginFormModel {
  username: string
  password: string
  code: string
  rememberMe: boolean
}

const router = useRouter()
const userStore = useUserStore()

const particlesLoaded = (container: unknown) => {
  console.log('Particles loaded', container)
}

const captchaEnabled = ref(true)
const captchaType = ref<CaptchaMode>('image')
const rememberMeEnabled = ref(true)
const registerEnabled = ref(true)
const showSliderModal = ref(false)
const sitePlatformName = ref('Admin Platform')
const sitePlatformSubtitle = ref('统一运维 · 高效管控')
const siteLoginWelcome = ref('Welcome')

const captchaImg = ref('')
const captchaUuid = ref('')

const formRef = ref<FormInstance | null>(null)
const loading = ref(false)
const submitAttempted = ref(false)

const formData = reactive<LoginFormModel>({
  username: '',
  password: '',
  code: '',
  rememberMe: false,
})

const formRules = ref<FormRules>({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
})

function parseCaptchaMode(value: string | undefined): CaptchaMode {
  return value === 'slider' ? 'slider' : 'image'
}

async function loadConfig() {
  try {
    const res = await getConfig()
    const config = res.data
    if (!config) return

    if (config.login) {
      captchaEnabled.value = config.login.captchaEnabled !== false
      captchaType.value = parseCaptchaMode(config.login.captchaType)
      rememberMeEnabled.value = config.login.rememberMe !== false
    }
    if (config.register) {
      registerEnabled.value = config.register.enabled !== false
    }
    if (config.site) {
      if (config.site.platformName) sitePlatformName.value = config.site.platformName
      if (config.site.platformSubtitle) sitePlatformSubtitle.value = config.site.platformSubtitle
      if (config.site.loginWelcome) siteLoginWelcome.value = config.site.loginWelcome
    }
    rebuildFormRules()
  } catch (error) {
    console.error('加载配置失败', error)
  }
}

async function loadCaptcha() {
  try {
    const res = await getCaptcha()
    const img = res.data.img ?? res.data.image
    if (img) {
      captchaImg.value = `data:image/png;base64,${img}`
      captchaUuid.value = res.data.uuid
    }
  } catch (error) {
    console.error('获取验证码失败', error)
  }
}

function rebuildFormRules() {
  const next: FormRules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  }
  if (captchaEnabled.value && captchaType.value === 'image') {
    next.code = [{ required: true, message: '请输入验证码', trigger: 'blur' }]
  }
  formRules.value = next
}

/** 浏览器自动填充有时不会更新 v-model，提交前从 DOM 同步一次 */
function syncAutofillFromDom() {
  const root = formRef.value?.$el as HTMLElement | undefined
  if (!root) return
  const inputs = Array.from(root.querySelectorAll<HTMLInputElement>('input.el-input__inner'))
  if (inputs[0]?.value) formData.username = inputs[0].value.trim()
  if (inputs[1]?.value) formData.password = inputs[1].value
  if (inputs[2]?.value && captchaEnabled.value && captchaType.value === 'image') {
    formData.code = inputs[2].value.trim()
  }
}

async function handleLogin() {
  if (!formRef.value) return
  submitAttempted.value = true
  syncAutofillFromDom()
  formData.username = (formData.username || '').trim()

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  if (captchaEnabled.value && captchaType.value === 'slider') {
    showSliderModal.value = true
    return
  }
  await doLogin()
}

async function doLogin() {
  loading.value = true
  try {
    const loginData: LoginForm = {
      username: formData.username,
      password: formData.password,
      rememberMe: formData.rememberMe,
    }
    if (captchaEnabled.value) {
      if (captchaType.value === 'slider') {
        loginData.code = 'slider_verified'
      } else {
        loginData.uuid = captchaUuid.value
        loginData.code = formData.code
      }
    }
    const result = await userStore.loginAction(loginData)
    if (result.code === 200) {
      ElMessage.success('登录成功')
      setTimeout(() => router.push('/'), 500)
    } else {
      ElMessage.error(result.msg || result.message || '登录失败')
      refreshCaptchaAfterFail()
    }
  } catch (error) {
    console.error('登录失败', error)
    const errorMessage = getErrorMessage(error)
    if (errorMessage && !errorMessage.includes('status code')) {
      ElMessage.error(errorMessage)
    } else {
      ElMessage.error('登录失败，请检查网络连接')
    }
    refreshCaptchaAfterFail()
  } finally {
    loading.value = false
  }
}

function refreshCaptchaAfterFail() {
  if (captchaEnabled.value && captchaType.value === 'image') {
    loadCaptcha()
    formData.code = ''
  }
}

function goRegister() {
  router.push('/register')
}

onMounted(async () => {
  await loadConfig()
  if (captchaEnabled.value && captchaType.value === 'image') {
    loadCaptcha()
  }
  await nextTick()
  syncAutofillFromDom()
  formRef.value?.clearValidate()
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

/* 全屏粒子背景 */
.sparkles-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: #000;
  overflow: hidden;
  z-index: 0;
}

.particle-layer {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.gradient-overlay {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse at center,
    transparent 42%,
    rgba(0, 0, 0, 0.22) 72%,
    rgba(0, 0, 0, 0.55) 100%
  );
  pointer-events: none;
}



/* 左右分栏 */
.login-container.login-split {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  display: flex;
  flex-direction: row;
  align-items: stretch;
  justify-content: center;
  gap: 0;
  min-height: 100vh;
  padding: clamp(16px, 4vw, 40px);
  box-sizing: border-box;
}

.login-brand {
  position: sticky;
  top: 0;
  align-self: flex-start;
  flex: 1 1 46%;
  min-width: 0;
  height: 100vh;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(24px, 4vw, 56px) clamp(20px, 3vw, 48px);
  animation: loginBrandIn 0.85s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.login-brand__visual {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: clamp(280px, 38vw, 400px);
  margin-bottom: 0;
}

/* 正方形容器 + 透明画布，透出登录页粒子背景 */
.login-brand__earth3d {
  --earth-view-size: min(420px, 88vw);
  width: var(--earth-view-size);
  height: var(--earth-view-size);
  margin: 0 auto;
  flex-shrink: 0;
  background: transparent;
  pointer-events: auto;
  /* 与星空背景分离，不遮挡透明画布 */
  filter: drop-shadow(0 0 36px rgba(56, 189, 248, 0.22));
}

.login-brand__inner {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 420px;
  transform: translateY(-58px);
}

.login-brand__title {
  margin: -10px 0 2px;
  font-size: clamp(1.75rem, 3.8vw, 2.5rem);
  font-weight: 800;
  letter-spacing: 0.04em;
  line-height: 1.2;
  color: #fff;
  text-shadow:
    0 0 28px rgba(255, 255, 255, 0.2),
    0 4px 20px rgba(0, 0, 0, 0.45);
}

.login-brand__tagline {
  margin: 0;
  font-size: clamp(15px, 1.6vw, 17px);
  color: rgba(230, 228, 250, 0.92);
  letter-spacing: 0.12em;
  font-weight: 500;
}

.login-form-pane {
  flex: 1 1 54%;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(16px, 3vw, 32px);
  border-left: 1px solid rgba(255, 255, 255, 0.12);
  background: transparent;
  animation: loginFormPaneIn 0.85s cubic-bezier(0.22, 1, 0.36, 1) 0.08s both;
}

/* 表单容器 */
.login-form-wrapper {
  width: 100%;
  max-width: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
}

.login-form__head {
  margin-bottom: 24px;
  text-align: center;
}

.login-form__title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.35);
}

@keyframes loginBrandIn {
  from {
    opacity: 0;
    transform: translateX(-28px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes loginFormPaneIn {
  from {
    opacity: 0;
    transform: translateX(28px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.login-form {
  width: 100%;
  /* 与左侧一致：尽量透，仅靠毛玻璃与细边框勾勒卡片 */
  background: linear-gradient(
    160deg,
    rgba(255, 255, 255, 0.055) 0%,
    rgba(255, 255, 255, 0.02) 45%,
    rgba(255, 255, 255, 0.035) 100%
  );
  backdrop-filter: blur(10px) saturate(135%);
  -webkit-backdrop-filter: blur(10px) saturate(135%);
  border-radius: 20px;
  padding: 40px;
  border: 1px solid rgba(255, 255, 255, 0.26);
  box-shadow:
    0 4px 28px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  transition: box-shadow 0.3s ease, border-color 0.3s ease;
}

.login-form:hover {
  border-color: rgba(255, 255, 255, 0.34);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.14);
}

@media (prefers-reduced-motion: reduce) {
  .login-brand,
  .login-form-pane {
    animation: none !important;
  }
}

/* 表单容器：避免 Element 默认底色盖住玻璃 */
.form-container {
  width: 100%;
  background: transparent !important;
}

/* 表单项目 */
.form-item {
  margin-bottom: 24px;
  animation: fadeInUp 0.6s ease;
  animation-fill-mode: both;
}

.form-item:nth-child(2) {
  animation-delay: 0.1s;
}

.form-item:nth-child(3) {
  animation-delay: 0.2s;
}

.form-item:nth-child(4) {
  animation-delay: 0.3s;
}

.form-item:nth-child(5) {
  animation-delay: 0.4s;
}

/* 输入框样式 */
.form-input {
  transition: all 0.3s ease;
}

.form-input :deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.95) !important;
  border-radius: 12px !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  transition: all 0.3s ease;
}

.form-input :deep(.el-input__wrapper):hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3) !important;
  border-color: rgba(64, 158, 255, 0.5) !important;
}

.form-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.2) !important;
  border-color: #409eff !important;
}

.form-input :deep(.el-input__inner) {
  color: #303133 !important;
  height: 48px !important;
  font-size: 16px !important;
  padding: 0 20px !important;
}

.form-input :deep(.el-input__inner::placeholder) {
  color: #909399 !important;
  font-size: 14px !important;
}

/* 输入框图标 */
.input-icon {
  color: #409eff !important;
  font-size: 18px !important;
}

/* 验证码行 */
.captcha-row {
  display: flex;
  gap: 16px;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  height: 48px;
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.captcha-img:hover {
  opacity: 0.9;
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

/* 进入页面前不展示校验红字，避免规则加载 / 自动填充时闪一下 */
.form-container:not(.form-container--submitted) :deep(.el-form-item__error) {
  display: none !important;
}

.form-container:not(.form-container--submitted) :deep(.el-form-item.is-error .el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--el-input-border-color, var(--el-border-color)) inset !important;
}

/* 验证码错误提示 */
.form-container :deep(.el-form-item__error) {
  color: #f56c6c !important;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
  font-size: 12px !important;
  margin-top: 4px !important;
}

/* 登录选项 */
.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-bottom: 8px;
}

/* 记住我复选框样式 */
.login-form .form-container .form-item .login-options .remember-checkbox :deep(.el-checkbox__label) {
  color: #fff !important;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.5);
  font-size: 14px !important;
}

.login-form .form-container .form-item .login-options .remember-checkbox :deep(.el-checkbox__input .el-checkbox__inner) {
  background-color: rgba(255, 255, 255, 0.95) !important;
  border-color: #409eff !important;
  border-radius: 4px !important;
  transition: all 0.3s ease !important;
  width: 18px !important;
  height: 18px !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2) !important;
}

.login-form .form-container .form-item .login-options .remember-checkbox :deep(.el-checkbox__input .el-checkbox__inner:hover) {
  border-color: #409eff !important;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2) !important;
}

.login-form .form-container .form-item .login-options .remember-checkbox :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #409eff !important;
  border-color: #409eff !important;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2) !important;
}

.login-form .form-container .form-item .login-options .remember-checkbox :deep(.el-checkbox__input.is-checked .el-checkbox__inner::after) {
  border-color: #fff !important;
  border-width: 2px !important;
  width: 5px !important;
  height: 8px !important;
  transform: rotate(45deg) scaleY(1) !important;
  left: 5px !important;
  top: 3px !important;
}

/* 注册链接样式 */
.register-link {
  color: #fff !important;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.5);
  font-size: 14px !important;
  transition: all 0.3s ease !important;
}

.register-link:hover {
  color: #409eff !important;
  text-shadow: 0 0 10px rgba(64, 158, 255, 0.5) !important;
}

/* 登录按钮 */
.login-button-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}

.login-button-wrapper :deep(.el-form-item__content) {
  justify-content: center;
  width: 100%;
}

.login-button {
  width: 100% !important;
  height: 52px !important;
  font-size: 18px !important;
  font-weight: 600 !important;
  border-radius: 12px !important;
  background: linear-gradient(135deg, #409eff, #667eea) !important;
  border: none !important;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.4) !important;
  transition: all 0.3s ease !important;
  color: white !important;
  letter-spacing: 2px;
}

.login-button:hover {
  background: linear-gradient(135deg, #667eea, #764ba2) !important;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6) !important;
  transform: translateY(-2px) !important;
}

.login-button:active {
  transform: translateY(0) !important;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.4) !important;
}

/* 动画效果 */
@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 960px) {
  .login-container.login-split {
    flex-direction: column;
    max-width: 460px;
    align-items: stretch;
  }

  .login-brand {
    position: relative;
    align-self: stretch;
    height: auto;
    flex: none;
    width: 100%;
    padding: 28px 20px 22px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  .login-brand__inner {
    transform: translateY(-48px);
  }

  .login-form-pane {
    flex: none;
    width: 100%;
    border-left: none;
    padding-top: 4px;
  }

  .login-brand__visual {
    min-height: 300px;
    margin-bottom: clamp(6px, 2vw, 12px);
  }

  .login-brand__earth3d {
    --earth-view-size: min(340px, 82vw);
  }
}

@media (max-width: 768px) {
  .login-form-wrapper {
    max-width: 100%;
  }

  .login-form {
    padding: 28px 20px;
  }
}

@media (max-width: 480px) {
  .login-brand__visual {
    min-height: 260px;
    margin-bottom: 10px;
  }

  .login-brand__earth3d {
    --earth-view-size: min(300px, 90vw);
  }

  .login-form {
    padding: 22px 16px;
  }

  .login-form__head {
    margin-bottom: 22px;
  }
}
</style>