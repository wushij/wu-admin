<template>
  <div class="register-page">
    <!-- 全屏粒子背景 -->
    <div class="sparkles-background">
      <!-- 第一层：大粒子，慢速，靛蓝色 -->
      <div class="particle-layer">
        <vue-particles
          id="sparkles-1"
          @particles-loaded="particlesLoaded"
          :options="options1"
        />
      </div>
      
      <!-- 第二层：中等粒子，中速，紫色 -->
      <div class="particle-layer">
        <vue-particles
          id="sparkles-2"
          @particles-loaded="particlesLoaded"
          :options="options2"
        />
      </div>
      
      <!-- 第三层：小粒子，快速，白色 -->
      <div class="particle-layer">
        <vue-particles
          id="sparkles-3"
          @particles-loaded="particlesLoaded"
          :options="options3"
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
            <div class="register-brand__glow" aria-hidden="true" />
            <div class="register-brand__logo-wrap">
              <div class="register-brand__earth" role="img" aria-label="全息地球 · Admin Platform">
                <div class="register-brand__earth-scan" aria-hidden="true" />
                <svg
                  class="register-brand__earth-svg"
                  viewBox="0 0 200 200"
                  xmlns="http://www.w3.org/2000/svg"
                  aria-hidden="true"
                >
                  <defs>
                    <linearGradient id="regEarthTechWire" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stop-color="#22d3ee" />
                      <stop offset="45%" stop-color="#38bdf8" />
                      <stop offset="100%" stop-color="#818cf8" />
                    </linearGradient>
                    <radialGradient id="regEarthTechCore" cx="34%" cy="26%" r="68%">
                      <stop offset="0%" stop-color="rgba(56, 189, 248, 0.12)" />
                      <stop offset="45%" stop-color="rgba(15, 23, 42, 0.55)" />
                      <stop offset="100%" stop-color="rgba(2, 8, 20, 0.92)" />
                    </radialGradient>
                    <radialGradient id="regEarthTechHalo" cx="50%" cy="50%" r="50%">
                      <stop offset="72%" stop-color="rgba(34, 211, 238, 0)" />
                      <stop offset="100%" stop-color="rgba(34, 211, 238, 0.5)" />
                    </radialGradient>
                    <clipPath id="regEarthSphereClip">
                      <circle cx="100" cy="100" r="78" />
                    </clipPath>
                    <pattern id="regEarthTechDots" width="12" height="12" patternUnits="userSpaceOnUse">
                      <circle cx="1.5" cy="1.5" r="0.55" fill="rgba(34, 211, 238, 0.14)" />
                      <circle cx="7" cy="7" r="0.45" fill="rgba(129, 140, 248, 0.12)" />
                    </pattern>
                  </defs>

                  <circle cx="100" cy="100" r="84" fill="none" stroke="rgba(34, 211, 238, 0.12)" stroke-width="1" />
                  <circle cx="100" cy="100" r="79" fill="url(#regEarthTechCore)" />
                  <circle cx="100" cy="100" r="79" fill="url(#regEarthTechHalo)" />

                  <g clip-path="url(#regEarthSphereClip)">
                    <rect x="22" y="22" width="156" height="156" fill="url(#regEarthTechDots)" opacity="0.85" />
                    <circle cx="100" cy="100" r="78" fill="rgba(3, 12, 28, 0.45)" />

                    <g class="register-brand__earth-rotate">
                      <ellipse
                        cx="100"
                        cy="100"
                        rx="76"
                        ry="6"
                        fill="none"
                        stroke="url(#regEarthTechWire)"
                        stroke-width="1.15"
                        stroke-dasharray="6 10"
                        stroke-linecap="round"
                        opacity="0.65"
                      />
                      <ellipse
                        cx="100"
                        cy="100"
                        rx="68"
                        ry="26"
                        fill="none"
                        stroke="url(#regEarthTechWire)"
                        stroke-width="0.85"
                        stroke-dasharray="2 5"
                        opacity="0.4"
                      />
                      <ellipse
                        cx="100"
                        cy="100"
                        rx="48"
                        ry="54"
                        fill="none"
                        stroke="url(#regEarthTechWire)"
                        stroke-width="0.75"
                        opacity="0.28"
                      />
                      <ellipse
                        cx="100"
                        cy="100"
                        rx="14"
                        ry="78"
                        fill="none"
                        stroke="url(#regEarthTechWire)"
                        stroke-width="0.95"
                        opacity="0.42"
                        transform="rotate(0 100 100)"
                      />
                      <ellipse
                        cx="100"
                        cy="100"
                        rx="32"
                        ry="78"
                        fill="none"
                        stroke="url(#regEarthTechWire)"
                        stroke-width="0.85"
                        opacity="0.36"
                        transform="rotate(24 100 100)"
                      />
                      <ellipse
                        cx="100"
                        cy="100"
                        rx="50"
                        ry="78"
                        fill="none"
                        stroke="url(#regEarthTechWire)"
                        stroke-width="0.8"
                        opacity="0.32"
                        transform="rotate(48 100 100)"
                      />
                      <ellipse
                        cx="100"
                        cy="100"
                        rx="66"
                        ry="78"
                        fill="none"
                        stroke="url(#regEarthTechWire)"
                        stroke-width="0.75"
                        opacity="0.26"
                        transform="rotate(72 100 100)"
                      />
                      <ellipse
                        cx="100"
                        cy="100"
                        rx="32"
                        ry="78"
                        fill="none"
                        stroke="url(#regEarthTechWire)"
                        stroke-width="0.82"
                        opacity="0.34"
                        transform="rotate(-30 100 100)"
                      />
                      <ellipse
                        cx="100"
                        cy="100"
                        rx="54"
                        ry="78"
                        fill="none"
                        stroke="url(#regEarthTechWire)"
                        stroke-width="0.78"
                        opacity="0.3"
                        transform="rotate(-54 100 100)"
                      />
                      <path
                        fill="none"
                        stroke="rgba(34, 211, 238, 0.22)"
                        stroke-width="0.75"
                        d="M44 96 L88 70 L132 96 L118 124 L72 118 Z"
                      />
                      <path
                        fill="none"
                        stroke="rgba(129, 140, 248, 0.2)"
                        stroke-width="0.7"
                        d="M108 58 L148 78 L138 108 L96 98 Z"
                      />
                      <circle cx="56" cy="92" r="2" fill="#22d3ee" opacity="0.85" />
                      <circle cx="92" cy="74" r="1.5" fill="#a5b4fc" opacity="0.9" />
                      <circle cx="134" cy="88" r="1.6" fill="#22d3ee" opacity="0.75" />
                      <circle cx="118" cy="118" r="1.4" fill="#38bdf8" opacity="0.8" />
                      <circle cx="72" cy="122" r="1.3" fill="#818cf8" opacity="0.75" />
                      <circle cx="142" cy="64" r="1.2" fill="#22d3ee" opacity="0.65" />
                    </g>
                  </g>

                  <circle
                    cx="100"
                    cy="100"
                    r="78"
                    fill="none"
                    stroke="rgba(186, 230, 253, 0.4)"
                    stroke-width="1.2"
                  />
                  <ellipse
                    cx="70"
                    cy="60"
                    rx="20"
                    ry="12"
                    fill="rgba(255, 255, 255, 0.14)"
                    transform="rotate(-28 70 60)"
                  />
                </svg>
              </div>
            </div>
          </div>
          <h1 class="register-brand__title">Admin Platform</h1>
          <p class="register-brand__tagline">统一运维 · 高效管控</p>
        </div>
      </aside>

      <!-- 右侧：注册表单 -->
      <div class="register-form-pane">
        <div class="register-form-wrapper">
          <div class="register-form">
            <header class="register-form__head">
              <h2 class="register-form__title">Sign Up</h2>
            </header>

            <el-form ref="formRef" :model="formData" :rules="rules" size="large" class="form-container">
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

            <!-- 图片验证码 -->
            <el-form-item v-if="captchaEnabled" prop="code" class="form-item">
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key, UserFilled } from '@element-plus/icons-vue'
import { getCaptcha, register, getConfig } from '@/api/system/auth'

const router = useRouter()

// 粒子加载完成回调
const particlesLoaded = async (container) => {
  console.log('Particles loaded', container)
}

// 第一层配置：大粒子，慢速，靛蓝色
const options1 = {
  background: {
    color: {
      value: 'transparent'
    }
  },
  fullScreen: { enable: false },
  fpsLimit: 120,
  particles: {
    color: { value: '#6366f1' },
    move: {
      enable: true,
      speed: 0.5,
      direction: 'none',
      outModes: { default: 'out' }
    },
    number: {
      density: { enable: true, width: 400, height: 400 },
      value: 60
    },
    opacity: {
      value: { min: 0.1, max: 1 },
      animation: { enable: true, speed: 0.5, startValue: 'random', sync: false }
    },
    shape: { type: 'circle' },
    size: { value: { min: 0.6, max: 1.5 } }
  },
  detectRetina: true
}

// 第二层配置：中等粒子，中速，紫色
const options2 = {
  background: {
    color: {
      value: 'transparent'
    }
  },
  fullScreen: { enable: false },
  fpsLimit: 120,
  particles: {
    color: { value: '#a855f7' },
    move: {
      enable: true,
      speed: 0.8,
      direction: 'none',
      outModes: { default: 'out' }
    },
    number: {
      density: { enable: true, width: 400, height: 400 },
      value: 80
    },
    opacity: {
      value: { min: 0.1, max: 1 },
      animation: { enable: true, speed: 0.8, startValue: 'random', sync: false }
    },
    shape: { type: 'circle' },
    size: { value: { min: 0.4, max: 1 } }
  },
  detectRetina: true
}

// 第三层配置：小粒子，快速，白色
const options3 = {
  background: {
    color: {
      value: 'transparent'
    }
  },
  fullScreen: { enable: false },
  fpsLimit: 120,
  particles: {
    color: { value: '#ffffff' },
    move: {
      enable: true,
      speed: 1.2,
      direction: 'none',
      outModes: { default: 'out' }
    },
    number: {
      density: { enable: true, width: 400, height: 400 },
      value: 100
    },
    opacity: {
      value: { min: 0.1, max: 1 },
      animation: { enable: true, speed: 1.2, startValue: 'random', sync: false }
    },
    shape: { type: 'circle' },
    size: { value: { min: 0.2, max: 0.6 } }
  },
  detectRetina: true
}

// 配置
const captchaEnabled = ref(true)
const agreeTerms = ref(false)
/** 未勾选协议时：协议行抖动提示（无 Toast） */
const agreeRowAlert = ref(false)

// 验证码
const captchaImg = ref('')
const captchaUuid = ref('')

// 加载配置
async function loadConfig() {
  try {
    const res = await getConfig()
    if (res.data) {
      const config = res.data
      if (config.login) {
        captchaEnabled.value = config.login.captchaEnabled !== false
      }
      if (config.register && !config.register.enabled) {
        ElMessage.warning('系统暂未开放注册')
        router.push('/login')
      }
    }
  } catch (error) {
    console.error('加载配置失败', error)
  }
}

// 加载验证码
async function loadCaptcha() {
  try {
    const res = await getCaptcha()
    captchaImg.value = 'data:image/png;base64,' + res.data.img
    captchaUuid.value = res.data.uuid
  } catch (error) {
    console.error('获取验证码失败', error)
  }
}

onMounted(async () => {
  await loadConfig()
  if (captchaEnabled.value) {
    loadCaptcha()
  }
})

const formRef = ref(null)
const loading = ref(false)

const formData = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  code: ''
})

// 验证密码一致性
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== formData.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,20}$/, message: '用户名只能包含字母、数字、下划线，长度4-20位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
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

// 注册处理
async function handleRegister() {
  if (!agreeTerms.value) {
    bumpAgreeRow()
    return
  }

  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const registerData = {
        username: formData.username,
        password: formData.password,
        nickname: formData.nickname || undefined
      }

      if (captchaEnabled.value) {
        registerData.uuid = captchaUuid.value
        registerData.code = formData.code
      }

      const res = await register(registerData)
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (error) {
      console.error('注册失败', error)
      if (captchaEnabled.value) {
        loadCaptcha()
        formData.code = ''
      }
    } finally {
      loading.value = false
    }
  })
}

function goLogin() {
  router.push('/login')
}
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
  position: relative;
  flex: 1 1 46%;
  min-width: 0;
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
  min-height: 212px;
  margin-bottom: clamp(8px, 1.8vw, 14px);
}

.register-brand__glow {
  position: absolute;
  left: 50%;
  top: 50%;
  width: min(272px, 80vw);
  height: min(272px, 80vw);
  transform: translate(-50%, -50%);
  background: radial-gradient(
    circle,
    rgba(34, 211, 238, 0.16) 0%,
    rgba(99, 102, 241, 0.08) 38%,
    transparent 68%
  );
  pointer-events: none;
  z-index: 0;
  animation: registerBrandGlow 5s ease-in-out infinite;
}

.register-brand__inner {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 380px;
  transform: translateY(-38px);
}

.register-brand__logo-wrap {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 0;
  animation: registerLogoFloat 6s ease-in-out infinite;
}

/* 左侧：全息科技地球（数据网格 + 扫描线 + 自转） */
.register-brand__earth {
  position: relative;
  z-index: 2;
  width: 172px;
  height: 172px;
  border-radius: 50%;
  overflow: hidden;
  filter:
    drop-shadow(0 0 18px rgba(34, 211, 238, 0.45))
    drop-shadow(0 10px 28px rgba(0, 0, 0, 0.55));
}

.register-brand__earth-scan {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  pointer-events: none;
  z-index: 2;
  background: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 4px,
    rgba(34, 211, 238, 0.045) 4px,
    rgba(34, 211, 238, 0.045) 5px
  );
  mix-blend-mode: screen;
  animation: regEarthHudScan 3.5s linear infinite;
}

.register-brand__earth-svg {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  display: block;
}

.register-brand__earth-rotate {
  transform-origin: 100px 100px;
  transform-box: fill-box;
  animation: registerEarthSpin 38s linear infinite;
}

@keyframes regEarthHudScan {
  from {
    transform: translateY(0);
  }
  to {
    transform: translateY(-6px);
  }
}

@keyframes registerEarthSpin {
  to {
    transform: rotate(360deg);
  }
}

.register-brand__title {
  margin: 0 0 5px;
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

@keyframes registerBrandGlow {
  0%,
  100% {
    opacity: 0.85;
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1.06);
  }
}

@keyframes registerLogoFloat {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
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
  .register-brand__glow,
  .register-brand__logo-wrap,
  .register-brand__earth-rotate,
  .register-brand__earth-scan,
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
    flex: none;
    width: 100%;
    padding: 28px 20px 22px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  .register-form-pane {
    flex: none;
    width: 100%;
    border-left: none;
    padding-top: 4px;
  }

  .register-brand__visual {
    min-height: 196px;
    margin-bottom: clamp(6px, 2vw, 12px);
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
  .register-brand__earth {
    width: 136px;
    height: 136px;
  }

  .register-brand__visual {
    min-height: 172px;
    margin-bottom: 10px;
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