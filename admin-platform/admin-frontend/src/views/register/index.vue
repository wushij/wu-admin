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

    <div class="register-container">
      <!-- 注册表单区域 -->
      <div class="register-form-wrapper">
        <div class="register-form">
          <!-- Logo -->
          <div class="banner-logo">
            <div class="logo-icon">A</div>
            <span class="logo-text">Admin Platform</span>
          </div>
          
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

            <el-form-item class="form-item">
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
                :disabled="!agreeTerms"
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
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
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

// 注册处理
async function handleRegister() {
  if (!agreeTerms.value) {
    ElMessage.warning('请先同意用户协议和隐私政策')
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
    transparent 20%,
    rgba(0, 0, 0, 0.4) 70%,
    rgba(0, 0, 0, 0.8) 100%
  );
  pointer-events: none;
}

/* 注册容器 */
.register-container {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: transparent;
  padding: 20px;
}

/* 表单容器 */
.register-form-wrapper {
  width: 100%;
  max-width: 450px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.register-form {
  width: 100%;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 40px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  transition: box-shadow 0.3s ease;
}

.register-form:hover {
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
}

/* Logo 样式 */
.banner-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 40px;
  animation: fadeInDown 0.6s ease;
}

.logo-icon {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.9), rgba(102, 126, 234, 0.9));
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: bold;
  margin-right: 16px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 20px rgba(64, 158, 255, 0.4);
  color: white;
  transition: all 0.3s ease;
}

.logo-icon:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 24px rgba(64, 158, 255, 0.6);
}

.logo-text {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.5);
  letter-spacing: 1px;
}

/* 表单容器 */
.form-container {
  width: 100%;
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

/* 响应式设计 */
@media (max-width: 768px) {
  .register-form-wrapper {
    max-width: 100%;
  }
  
  .register-form {
    padding: 30px;
  }
}

@media (max-width: 480px) {
  .logo-icon {
    width: 48px;
    height: 48px;
    font-size: 24px;
  }
  
  .logo-text {
    font-size: 20px;
  }
  
  .banner-logo {
    margin-bottom: 30px;
  }
  
  .register-form {
    padding: 24px;
  }
  
  .form-item {
    margin-bottom: 20px;
  }
}
</style>