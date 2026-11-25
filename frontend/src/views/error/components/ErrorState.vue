<template>
  <div class="error-page">
    <div class="error-card">
      <div class="error-code motion-scramble" :aria-label="`错误代码 ${code}`">{{ scrambledCode }}</div>
      <h1 class="error-title">{{ title }}</h1>
      <p class="error-desc">{{ desc }}</p>
      <div class="error-actions">
        <el-button @click="goBack">返回上一页</el-button>
        <el-button type="primary" @click="goHome">回到工作台</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useTextScramble } from '@/composables/useMotion'

const props = defineProps<{
  code: string
  title: string
  desc: string
}>()

const router = useRouter()
const { displayText: scrambledCode } = useTextScramble(computed(() => props.code), 400)

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
  align-items: center;
  justify-content: center;
  background: var(--color-bg-canvas);
  padding: var(--space-5);
}

.error-card {
  width: 100%;
  max-width: 460px;
  padding: var(--space-7) var(--space-6);
  border-radius: var(--radius-xl);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  text-align: center;
}

.error-code {
  font-size: 92px;
  font-weight: 800;
  line-height: 1;
  color: var(--el-color-primary);
  letter-spacing: 4px;
}

.error-title {
  margin: var(--space-4) 0 0;
  font-size: var(--text-h1);
  font-weight: 600;
  color: var(--color-text-primary);
}

.error-desc {
  margin: var(--space-2) 0 0;
  font-size: var(--text-body);
  color: var(--color-text-secondary);
}

.error-actions {
  margin-top: var(--space-6);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
}
</style>
