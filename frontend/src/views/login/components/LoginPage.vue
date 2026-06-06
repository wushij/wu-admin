<template>
  <div class="login-page-root">
  <AuthSplitLayout :title="sitePlatformName" :tagline="sitePlatformSubtitle">
    <div class="auth-form-wrapper">
      <div class="auth-glass-form">
        <header class="auth-form__head">
          <h2 class="auth-form__title">{{ siteLoginWelcome }}</h2>
        </header>

        <div v-if="showLoginModeSwitch" class="login-mode-switch">
          <button
            type="button"
            class="login-mode-switch__item"
            :class="{ 'login-mode-switch__item--active': loginMode === 'account' }"
            @click="switchLoginMode('account')"
          >
            账号登录
          </button>
          <button
            type="button"
            class="login-mode-switch__item"
            :class="{ 'login-mode-switch__item--active': loginMode === 'sms' }"
            @click="switchLoginMode('sms')"
          >
            短信登录
          </button>
        </div>

        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          :validate-on-rule-change="false"
          :class="['form-container', { 'form-container--submitted': submitAttempted }]"
          size="large"
        >
          <el-form-item v-if="loginMode === 'account'" prop="username" class="form-item">
            <el-input
              v-model="formData.username"
              name="username"
              autocomplete="username"
              placeholder="请输入用户名"
              maxlength="50"
              class="form-input"
              @keyup.enter="handleLogin"
            >
              <template #prefix><el-icon class="input-icon"><User /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item v-if="loginMode === 'account'" prop="password" class="form-item">
            <el-input
              v-model="formData.password"
              name="password"
              type="password"
              autocomplete="current-password"
              placeholder="请输入密码"
              show-password
              maxlength="50"
              class="form-input"
              @keyup.enter="handleLogin"
            >
              <template #prefix><el-icon class="input-icon"><Lock /></el-icon></template>
            </el-input>
          </el-form-item>

          <template v-if="loginMode === 'sms'">
            <el-form-item prop="phone" class="form-item sms-form-item">
              <el-input
                v-model="formData.phone"
                placeholder="请输入绑定的手机号"
                maxlength="11"
                clearable
                autocomplete="off"
                class="form-input"
              >
                <template #prefix><el-icon class="input-icon"><Iphone /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item prop="code" class="form-item sms-form-item">
              <div class="sms-code-row">
                <el-input
                  v-model="formData.code"
                  placeholder="请输入验证码"
                  maxlength="6"
                  autocomplete="off"
                  class="form-input sms-code-field"
                  @keyup.enter="handleLogin"
                >
                  <template #prefix><el-icon class="input-icon"><Key /></el-icon></template>
                </el-input>
                <el-button
                  class="sms-send-btn"
                  :disabled="smsCountdown > 0 || sendingSms || !smsEnabled"
                  :loading="sendingSms"
                  @click="handleSendSmsCode"
                >
                  {{ smsCountdown > 0 ? `${smsCountdown}s` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <p v-if="!smsEnabled" class="sms-disabled-tip">短信功能未启用，请联系管理员</p>
          </template>

          <el-form-item
            v-if="loginMode === 'account' && captchaEnabled && captchaType === 'image'"
            prop="code"
            class="form-item"
          >
            <AuthCaptchaField
              v-model="formData.code"
              :captcha-img="captchaImg"
              @refresh="loadCaptcha"
              @enter="handleLogin"
            />
          </el-form-item>

          <el-form-item
            v-if="loginMode === 'account' && (rememberMeEnabled || registerEnabled)"
            class="form-item"
          >
            <div
              class="login-options"
              :class="{ 'login-options--register-only': !rememberMeEnabled && registerEnabled }"
            >
              <el-checkbox v-if="rememberMeEnabled" v-model="formData.rememberMe" class="remember-checkbox">
                记住我
              </el-checkbox>
              <el-link v-if="registerEnabled" type="primary" class="register-link" @click="goRegister">
                没有账号？立即注册
              </el-link>
            </div>
          </el-form-item>

          <el-form-item class="form-item login-button-wrapper">
            <el-button
              type="primary"
              :loading="loading"
              class="login-button"
              loading-text="登录中..."
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </AuthSplitLayout>
  <SliderCaptcha v-model:show="showSliderModal" @success="onSliderSuccess" />
  </div>
</template>

<script setup lang="ts">
import { User, Lock, Key, Iphone } from '@element-plus/icons-vue'
import AuthSplitLayout from '@/views/auth/components/AuthSplitLayout.vue'
import AuthCaptchaField from '@/views/auth/components/AuthCaptchaField.vue'
import SliderCaptcha from '@/components/SliderCaptcha.vue'
import { useLoginForm } from '../composables/useLoginForm'

const {
  sitePlatformName,
  sitePlatformSubtitle,
  siteLoginWelcome,
  showLoginModeSwitch,
  loginMode,
  switchLoginMode,
  formRef,
  formData,
  formRules,
  submitAttempted,
  captchaEnabled,
  captchaType,
  captchaImg,
  smsEnabled,
  rememberMeEnabled,
  registerEnabled,
  sendingSms,
  smsCountdown,
  loading,
  showSliderModal,
  loadCaptcha,
  handleSendSmsCode,
  handleLogin,
  onSliderSuccess,
  goRegister,
} = useLoginForm()
</script>

<style scoped src="./login-form.css"></style>
