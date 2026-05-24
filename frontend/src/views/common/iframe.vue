<template>
  <div class="iframe-page" v-loading="loading" element-loading-text="加载中...">
    <iframe
      ref="iframeRef"
      :src="frameSrc"
      class="iframe-content"
      frameborder="0"
      allowfullscreen
      @load="handleLoad"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const iframeRef = ref(null)
const loading = ref(true)

const frameSrc = computed((): string => {
  const src = route.meta.frameSrc
  return typeof src === 'string' && src ? src : '/doc.html'
})

function handleLoad() {
  loading.value = false
}

onMounted(() => {
  setTimeout(() => {
    loading.value = false
  }, 5000)
})
</script>

<style scoped>
.iframe-page {
  width: 100%;
  height: calc(100vh - 120px);
  min-height: 480px;
  position: relative;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.iframe-content {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}
</style>
