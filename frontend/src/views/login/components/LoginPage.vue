<template>
  <div class="login-page-root">
    <AuthSplitLayout :title="sitePlatformName" :tagline="sitePlatformSubtitle">
      <div class="auth-form-wrapper">
        <div class="auth-glass-form">
          <header class="auth-form__head">
            <h2 class="auth-form__title">{{ siteLoginWelcome }}</h2>
            <p class="auth-form__sub">请验证身份以进入企业管理控制台</p>
          </header>

          <div v-if="showLoginModeSwitch" class="login-mode-switch" role="tablist" aria-label="登录方式">
            <button
              type="button"
              role="tab"
              :aria-selected="loginMode === 'account'"
              class="login-mode-switch__item"
              :class="{ 'login-mode-switch__item--active': loginMode === 'account' }"
              @click="switchLoginMode('account')"
            >
              <el-icon :size="15"><User /></el-icon>
              <span>账号登录</span>
            </button>
            <button
              v-if="smsLoginEnabled && smsEnabled"
              type="button"
              role="tab"
              :aria-selected="loginMode === 'sms'"
              class="login-mode-switch__item"
              :class="{ 'login-mode-switch__item--active': loginMode === 'sms' }"
              @click="switchLoginMode('sms')"
            >
              <el-icon :size="15"><Iphone /></el-icon>
              <span>短信登录</span>
            </button>
            <button
              v-if="emailLoginEnabled && emailEnabled"
              type="button"
              role="tab"
              :aria-selected="loginMode === 'email'"
              class="login-mode-switch__item"
              :class="{ 'login-mode-switch__item--active': loginMode === 'email' }"
              @click="switchLoginMode('email')"
            >
              <el-icon :size="15"><Message /></el-icon>
              <span>邮箱登录</span>
            </button>
          </div>

          <el-form
            ref="formRef"
            :model="formData"
            class="form-container"
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
              <InlineAlert
                v-if="!smsEnabled"
                type="warning"
                title="短信登录功能未启用"
                description="请联系系统管理员在安全配置中开启短信通道。"
                compact
                class="auth-inline-alert"
              />
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
                    <template #prefix><el-icon class="input-icon"><Lock /></el-icon></template>
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
            </template>

            <template v-if="loginMode === 'email'">
              <InlineAlert
                v-if="!emailEnabled"
                type="warning"
                title="邮件登录功能未启用"
                description="请联系系统管理员在通知配置中开启邮件服务。"
                compact
                class="auth-inline-alert"
              />
              <el-form-item prop="email" class="form-item sms-form-item">
                <el-input
                  v-model="formData.email"
                  placeholder="请输入绑定的邮箱地址"
                  maxlength="60"
                  clearable
                  autocomplete="off"
                  class="form-input"
                >
                  <template #prefix><el-icon class="input-icon"><Message /></el-icon></template>
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
                    <template #prefix><el-icon class="input-icon"><Lock /></el-icon></template>
                  </el-input>
                  <el-button
                    class="sms-send-btn"
                    :disabled="emailCountdown > 0 || sendingEmail || !emailEnabled"
                    :loading="sendingEmail"
                    @click="handleSendEmailCode"
                  >
                    {{ emailCountdown > 0 ? `${emailCountdown}s` : '获取验证码' }}
                  </el-button>
                </div>
              </el-form-item>
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
              <div class="auth-magnetic-wrap">
                <AuthSubmitButton
                  label="登 录"
                  loading-text="登录中…"
                  success-label="登录成功"
                  :loading="loading"
                  :success="loginSuccess"
                  @click="handleLogin"
                />
              </div>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </AuthSplitLayout>
    <SliderCaptcha
      v-model:show="showSliderModal"
      :scene="sliderPurpose"
      @success="onSliderSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { User, Lock, Iphone, Message } from '@element-plus/icons-vue'
import AuthSplitLayout from '@/components/auth/AuthSplitLayout.vue'
import AuthCaptchaField from '@/components/auth/AuthCaptchaField.vue'
import AuthSubmitButton from '@/components/auth/AuthSubmitButton.vue'
import SliderCaptcha from '@/components/SliderCaptcha.vue'
import InlineAlert from '@/components/ui/InlineAlert.vue'
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
  captchaEnabled,
  captchaType,
  captchaImg,
  smsEnabled,
  emailEnabled,
  smsLoginEnabled,
  emailLoginEnabled,
  rememberMeEnabled,
  registerEnabled,
  sendingSms,
  sendingEmail,
  smsCountdown,
  emailCountdown,
  loading,
  loginSuccess,
  showSliderModal,
  sliderPurpose,
  loadCaptcha,
  handleSendSmsCode,
  handleSendEmailCode,
  handleLogin,
  onSliderSuccess,
  goRegister,
} = useLoginForm()
defineExpose({ formRef })
</script>

<style scoped src="@/components/auth/auth-glass-form.css"></style>
<style scoped src="./login-form.css"></style>
