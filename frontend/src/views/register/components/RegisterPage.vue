<template>
  <div class="register-page-root">
    <AuthSplitLayout :title="sitePlatformName" :tagline="sitePlatformSubtitle">
      <div class="auth-form-wrapper">
        <div class="auth-glass-form">
          <header class="auth-form__head">
            <h2 class="auth-form__title">{{ siteRegisterTitle }}</h2>
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
                placeholder="用户名（4-12位字母数字下划线）"
                maxlength="12"
                class="form-input"
              >
                <template #prefix><el-icon class="input-icon"><User /></el-icon></template>
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
                <template #prefix><el-icon class="input-icon"><Lock /></el-icon></template>
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
                <template #prefix><el-icon class="input-icon"><Lock /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item prop="nickname" class="form-item">
              <el-input
                v-model="formData.nickname"
                placeholder="昵称（可选）"
                maxlength="20"
                class="form-input"
              >
                <template #prefix><el-icon class="input-icon"><UserFilled /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item
              v-if="captchaEnabled && captchaType === 'image'"
              prop="code"
              class="form-item"
            >
              <AuthCaptchaField
                v-model="formData.code"
                :captcha-img="captchaImg"
                @refresh="loadCaptcha"
              />
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
                class="register-button"
                loading-text="注册中..."
                @click="handleRegister"
              >
                注 册
              </el-button>
            </el-form-item>
          </el-form>

          <div class="register-footer">
            <span>已有账号？</span>
            <el-link type="primary" class="login-link" @click="goLogin">立即登录</el-link>
          </div>
        </div>
      </div>
    </AuthSplitLayout>
    <SliderCaptcha v-model:show="showSliderModal" scene="register" @success="onSliderSuccess" />
  </div>
</template>

<script setup lang="ts">
import { User, Lock, UserFilled } from '@element-plus/icons-vue'
import AuthSplitLayout from '@/views/auth/components/AuthSplitLayout.vue'
import AuthCaptchaField from '@/views/auth/components/AuthCaptchaField.vue'
import SliderCaptcha from '@/components/SliderCaptcha.vue'
import { useRegisterForm } from '../composables/useRegisterForm'

const {
  sitePlatformName,
  sitePlatformSubtitle,
  siteRegisterTitle,
  formRef,
  formData,
  rules,
  submitAttempted,
  captchaEnabled,
  captchaType,
  captchaImg,
  agreeTerms,
  agreeRowAlert,
  loading,
  showSliderModal,
  onSliderSuccess,
  loadCaptcha,
  handleRegister,
  goLogin,
} = useRegisterForm()
</script>

<style scoped src="./register-form.css"></style>
