<template>
  <div class="error-page">
    <div class="error-code">{{ code }}</div>
    <div class="error-title">{{ title }}</div>
    <div class="error-desc">{{ desc }}</div>
    <div class="error-actions">
      <el-button @click="goBack">返回上一页</el-button>
      <el-button type="primary" @click="goHome">回到工作台</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

defineProps<{
  code: string
  title: string
  desc: string
}>()

const router = useRouter()

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    goHome()
  }
}

function goHome() {
  router.replace('/dashboard')
}
</script>

<style scoped>
.error-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--el-bg-color-page, #f5f7fa);
  padding: 24px;
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  line-height: 1;
  color: var(--el-color-primary);
  letter-spacing: 4px;
}

.error-title {
  margin-top: 16px;
  font-size: 22px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.error-desc {
  margin-top: 8px;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.error-actions {
  margin-top: 32px;
}
</style>
