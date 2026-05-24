<template>
  <div class="register-page">
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

    <div class="register-container register-split">
      <!-- 左侧：品牌与全息科技地球 -->
      <aside class="register-brand">
        <div class="register-brand__inner">
          <div class="register-brand__visual">
            <div class="register-brand__earth3d" role="img" aria-label="3D 地球 · Admin Platform">
              <Earth3D transparent :show-stars="false" />
            </div>
          </div>
          <h1 class="register-brand__title">{{ sitePlatformName }}</h1>
          <p class="register-brand__tagline">{{ sitePlatformSubtitle }}</p>
        </div>
      </aside>

      <!-- 右侧：注册表单 -->
      <div class="register-form-pane">
        <div class="register-form-wrapper">
          <div class="register-form">
            <header class="register-form__head">
              <h2 class="register-form__title">{{ siteRegisterTitle }}</h2>
            </header>

            <el-form
              ref="formRef"
              :model="formData"
              :rules="rules"
              :validate-on-rule-change="false"
              size="large"
              :class="['form-container', { 'form-container--submitted': submitAttempted }]"
            >
            <el-form-item prop="username" class="form-item">
              <el-input
                v-model="formData.username"
                placeholder="用户名（4-20位字母数字下划线）"
                maxlength="20"
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
                type="password"
                placeholder="请输入密码（6-20位）"
                show-password
                maxlength="20"
                class="form-input"
              >
                <template #prefix>
                  <el-icon class="input-icon"><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="confirmPassword" class="form-item">
              <el-input
                v-model="formData.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                show-password
                maxlength="20"
                class="form-input"
              >
                <template #prefix>
                  <el-icon class="input-icon"><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="nickname" class="form-item">
              <el-input
                v-model="formData.nickname"
                placeholder="昵称（可选）"
                maxlength="20"
                class="form-input"
              >
                <template #prefix>
                  <el-icon class="input-icon"><UserFilled /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <!-- 图片验证码（滑块模式不显示表单项，提交时弹窗） -->
            <el-form-item v-if="captchaEnabled && captchaType === 'image'" prop="code" class="form-item">
              <div class="captcha-row">
                <el-input
                  v-model="formData.code"
                  placeholder="请输入验证码"
                  maxlength="6"
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

            <el-form-item
              class="form-item register-agree-row"
              :class="{ 'register-agree-row--alert': agreeRowAlert }"
            >
              <el-checkbox v-model="agreeTerms" class="agree-checkbox">
                我已阅读并同意
                <el-link type="primary" class="terms-link">《用户协议》</el-link>
                和
                <el-link type="primary" class="terms-link">《隐私政策》</el-link>
              </el-checkbox>
            </el-form-item>

            <el-form-item class="form-item register-button-wrapper">
              <el-button
                type="primary"
                :loading="loading"
                @click="handleRegister"
                class="register-button"
                loading-text="注册中..."
              >
                注 册
              </el-button>
            </el-form-item>
          </el-form>

          <div class="register-footer">
            <span>已有账号？</span>
            <el-link type="primary" @click="goLogin" class="login-link">立即登录</el-link>
          </div>
          </div>
        </div>
      </div>
    </div>
    <SliderCaptcha v-model:show="showSliderModal" @success="doRegister" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Key, UserFilled } from '@element-plus/icons-vue'
import { getCaptcha, register, getConfig } from '@/api/system/auth'
import type { RegisterForm } from '@/types/api'
import {
  authParticleOptions1,
  authParticleOptions2,
  authParticleOptions3,
} from '@/constants/authParticles'
import { getErrorMessage } from '@/utils/axiosError'
import SliderCaptcha from '@/components/SliderCaptcha.vue'
import Earth3D from '@/components/earth/Earth3D.vue'

type CaptchaMode = 'image' | 'slider'

interface RegisterFormModel {
  username: string
  password: string
  confirmPassword: string
  nickname: string
  code: string
}

const router = useRouter()
const showSliderModal = ref(false)

const particlesLoaded = (container: unknown) => {
  console.log('Particles loaded', container)
}

const captchaEnabled = ref(true)
const captchaType = ref<CaptchaMode>('image')
const minPasswordLength = ref(6)
const sitePlatformName = ref('Admin Platform')
const sitePlatformSubtitle = ref('统一运维 · 高效管控')
const siteRegisterTitle = ref('Sign Up')
const agreeTerms = ref(false)
const agreeRowAlert = ref(false)

const captchaImg = ref('')
const captchaUuid = ref('')

const formRef = ref<FormInstance | null>(null)
const loading = ref(false)
const submitAttempted = ref(false)

const formData = reactive<RegisterFormModel>({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  code: '',
})

function parseCaptchaMode(value: string | undefined): CaptchaMode {
  return value === 'slider' ? 'slider' : 'image'
}

const validateConfirmPassword = (
  _rule: unknown,
  value: string,
  callback: (error?: Error) => void,
) => {
  if (value !== formData.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = computed<FormRules>(() => {
  const base: FormRules = {
    username: [
      { required: true, message: '请输入用户名', trigger: 'blur' },
      {
        pattern: /^[a-zA-Z0-9_]{4,20}$/,
        message: '用户名只能包含字母、数字、下划线，长度4-20位',
        trigger: 'blur',
      },
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      {
        min: minPasswordLength.value,
        max: 32,
        message: `密码长度至少 ${minPasswordLength.value} 位`,
        trigger: 'blur',
      },
    ],
    confirmPassword: [
      { required: true, message: '请确认密码', trigger: 'blur' },
      { validator: validateConfirmPassword, trigger: 'blur' },
    ],
  }
  if (captchaEnabled.value && captchaType.value === 'image') {
    base.code = [{ required: true, message: '请输入验证码', trigger: 'blur' }]
  }
  return base
})

async function loadConfig() {
  try {
    const res = await getConfig()
    const config = res.data
    if (!config) return

    if (config.register) {
      if (config.register.captchaEnabled !== undefined) {
        captchaEnabled.value = config.register.captchaEnabled !== false
      }
      if (config.register.captchaType) {
        captchaType.value = parseCaptchaMode(config.register.captchaType)
      }
      if (config.register.minPasswordLength) {
        minPasswordLength.value = Number(config.register.minPasswordLength) || 6
      }
      if (config.register.enabled === false) {
        ElMessage.warning('系统暂未开放注册')
        router.push('/login')
      }
    }
    if (config.site) {
      if (config.site.platformName) sitePlatformName.value = config.site.platformName
      if (config.site.platformSubtitle) sitePlatformSubtitle.value = config.site.platformSubtitle
      if (config.site.registerTitle) siteRegisterTitle.value = config.site.registerTitle
    }
  } catch (error) {
    console.error('加载配置失败', error)
  }
}

async function loadCaptcha() {
  try {
    const res = await getCaptcha('register')
    const img = res.data.img ?? res.data.image
    if (img) {
      captchaImg.value = `data:image/png;base64,${img}`
      captchaUuid.value = res.data.uuid
    }
  } catch (error) {
    console.error('获取验证码失败', error)
  }
}

function bumpAgreeRow() {
  agreeRowAlert.value = false
  nextTick(() => {
    agreeRowAlert.value = true
    window.setTimeout(() => {
      agreeRowAlert.value = false
    }, 540)
  })
}

async function handleRegister() {
  if (!agreeTerms.value) {
    bumpAgreeRow()
    return
  }
  if (!formRef.value) return

  submitAttempted.value = true
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
  await doRegister()
}

async function doRegister() {
  loading.value = true
  try {
    const registerData: RegisterForm = {
      username: formData.username,
      password: formData.password,
      nickname: formData.nickname || undefined,
    }

    if (captchaEnabled.value) {
      if (captchaType.value === 'slider') {
        registerData.code = 'slider_verified'
      } else {
        registerData.uuid = captchaUuid.value
        registerData.code = formData.code
      }
    }

    const res = await register(registerData)
    ElMessage.success(res.message || res.msg || '注册成功，请登录')
    router.push('/login')
  } catch (error) {
    console.error('注册失败', error)
    const errorMessage = getErrorMessage(error)
    if (errorMessage && !errorMessage.includes('status code')) {
      ElMessage.error(errorMessage)
    }
    if (captchaEnabled.value && captchaType.value === 'image') {
      loadCaptcha()
      formData.code = ''
    }
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}

onMounted(async () => {
  await loadConfig()
  if (captchaEnabled.value && captchaType.value === 'image') {
    loadCaptcha()
  }
  await nextTick()
  formRef.value?.clearValidate()
})
</script>

<style scoped>
.register-page {
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
.register-container.register-split {
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

.register-brand {
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
  animation: registerBrandIn 0.85s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.register-brand__visual {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: clamp(280px, 38vw, 400px);
  margin-bottom: 0;
}

.register-brand__earth3d {
  --earth-view-size: min(420px, 88vw);
  width: var(--earth-view-size);
  height: var(--earth-view-size);
  margin: 0 auto;
  flex-shrink: 0;
  background: transparent;
  pointer-events: auto;
  filter: drop-shadow(0 0 36px rgba(56, 189, 248, 0.22));
}

.register-brand__inner {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 420px;
  transform: translateY(-58px);
}

.register-brand__title {
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

.register-brand__tagline {
  margin: 0;
  font-size: clamp(15px, 1.6vw, 17px);
  color: rgba(230, 228, 250, 0.92);
  letter-spacing: 0.12em;
  font-weight: 500;
}

.register-form-pane {
  flex: 1 1 54%;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(16px, 3vw, 32px);
  border-left: 1px solid rgba(255, 255, 255, 0.12);
  background: transparent;
  animation: registerFormPaneIn 0.85s cubic-bezier(0.22, 1, 0.36, 1) 0.08s both;
}

/* 表单容器 */
.register-form-wrapper {
  width: 100%;
  max-width: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  /* 表单项较多，整体略上移避免视觉偏下 */
  transform: translateY(-14px);
}

.register-form__head {
  margin-bottom: 24px;
  text-align: center;
}

.register-form__title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.35);
}

@keyframes registerBrandIn {
  from {
    opacity: 0;
    transform: translateX(-28px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes registerFormPaneIn {
  from {
    opacity: 0;
    transform: translateX(28px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.register-form {
  width: 100%;
  /* 与登录页一致：高透玻璃，透出星空 */
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
  border: 1px solid rgba(255, 255, 255, 0.28);
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.12);
  transition: box-shadow 0.3s ease, border-color 0.3s ease;
}

.register-form:hover {
  border-color: rgba(255, 255, 255, 0.34);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.14);
}

/* 表单容器 */
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

.form-item:nth-child(6) {
  animation-delay: 0.5s;
}

.form-item:nth-child(7) {
  animation-delay: 0.6s;
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

/* 进入页面前不展示校验红字，避免规则加载时闪一下 */
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

/* 未勾选协议：整行抖动提示（无文字 Toast） */
.register-agree-row--alert :deep(.el-form-item__content) {
  animation: registerAgreeNudge 0.52s cubic-bezier(0.33, 1, 0.68, 1);
  transform-origin: 50% 100%;
}

@keyframes registerAgreeNudge {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  18% {
    transform: translate3d(-5px, -3px, 0) rotate(-0.55deg);
  }
  36% {
    transform: translate3d(5px, 2px, 0) rotate(0.55deg);
  }
  54% {
    transform: translate3d(-3px, -2px, 0) rotate(-0.35deg);
  }
  72% {
    transform: translate3d(2px, 1px, 0) rotate(0.2deg);
  }
  88% {
    transform: translate3d(-1px, -1px, 0);
  }
}

/* 同意协议复选框 */
.register-form .form-container .form-item .agree-checkbox :deep(.el-checkbox__label) {
  color: #fff !important;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.5);
  font-size: 14px !important;
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  line-height: 1.5;
}

.register-form .form-container .form-item .agree-checkbox :deep(.el-checkbox__input .el-checkbox__inner) {
  background-color: rgba(255, 255, 255, 0.95) !important;
  border-color: #409eff !important;
  border-radius: 4px !important;
  transition: all 0.3s ease !important;
  width: 18px !important;
  height: 18px !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2) !important;
}

.register-form .form-container .form-item .agree-checkbox :deep(.el-checkbox__input .el-checkbox__inner:hover) {
  border-color: #409eff !important;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2) !important;
}

.register-form .form-container .form-item .agree-checkbox :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #409eff !important;
  border-color: #409eff !important;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2) !important;
}

.register-form .form-container .form-item .agree-checkbox :deep(.el-checkbox__input.is-checked .el-checkbox__inner::after) {
  border-color: #fff !important;
  border-width: 2px !important;
  width: 5px !important;
  height: 8px !important;
  transform: rotate(45deg) scaleY(1) !important;
  left: 5px !important;
  top: 3px !important;
}

/* 协议链接 */
.terms-link {
  color: #409eff !important;
  text-shadow: 0 0 10px rgba(64, 158, 255, 0.5) !important;
  transition: all 0.3s ease !important;
}

.terms-link:hover {
  color: #667eea !important;
}

/* 注册按钮 */
.register-button-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}

.register-button-wrapper :deep(.el-form-item__content) {
  justify-content: center;
  width: 100%;
}

.register-button {
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

.register-button:hover {
  background: linear-gradient(135deg, #667eea, #764ba2) !important;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6) !important;
  transform: translateY(-2px) !important;
}

.register-button:active {
  transform: translateY(0) !important;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.4) !important;
}

.register-button:disabled {
  background: #c0c4cc !important;
  box-shadow: none !important;
  cursor: not-allowed !important;
}

/* 注册底部 */
.register-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #fff;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  animation: fadeInUp 0.6s ease 0.7s both;
}

/* 登录链接 */
.login-link {
  color: #409eff !important;
  text-shadow: 0 0 10px rgba(64, 158, 255, 0.5) !important;
  transition: all 0.3s ease !important;
}

.login-link:hover {
  color: #667eea !important;
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

@media (prefers-reduced-motion: reduce) {
  .register-brand,
  .register-form-pane,
  .register-agree-row--alert :deep(.el-form-item__content) {
    animation: none !important;
  }
}

/* 响应式设计 */
@media (max-width: 960px) {
  .register-container.register-split {
    flex-direction: column;
    max-width: 460px;
    align-items: stretch;
  }

  .register-brand {
    position: relative;
    align-self: stretch;
    height: auto;
    flex: none;
    width: 100%;
    padding: 28px 20px 22px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  .register-brand__inner {
    transform: translateY(-48px);
  }

  .register-form-pane {
    flex: none;
    width: 100%;
    border-left: none;
    padding-top: 0;
  }

  .register-form-wrapper {
    transform: translateY(-10px);
  }

  .register-brand__visual {
    min-height: 300px;
    margin-bottom: clamp(6px, 2vw, 12px);
  }

  .register-brand__earth3d {
    --earth-view-size: min(340px, 82vw);
  }
}

@media (max-width: 768px) {
  .register-form-wrapper {
    max-width: 100%;
  }

  .register-form {
    padding: 28px 20px;
  }
}

@media (max-width: 480px) {
  .register-brand__visual {
    min-height: 260px;
    margin-bottom: 10px;
  }

  .register-brand__earth3d {
    --earth-view-size: min(300px, 90vw);
  }

  .register-form {
    padding: 22px 16px;
  }

  .register-form__head {
    margin-bottom: 22px;
  }

  .form-item {
    margin-bottom: 20px;
  }
}
</style>