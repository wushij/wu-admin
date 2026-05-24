import { ref, computed, onMounted, onUnmounted } from 'vue'

/** 与 PC 端 `useDashboardClock` 问候文案与秒级刷新保持一致 */
export function useDashboardClock() {
  const currentTime = ref('')
  let timeTimer: ReturnType<typeof setInterval> | null = null

  const greetingMessage = computed(() => {
    const hour = new Date().getHours()
    if (hour >= 5 && hour < 12) return '早上好，愿今日轻松又顺遂'
    if (hour >= 12 && hour < 18) return '下午好，心情在线，自在前行'
    return '晚上好，愿夜色温柔，一夜好梦'
  })

  function updateTime() {
    const now = new Date()
    currentTime.value = now.toLocaleString('zh-CN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      weekday: 'long',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    })
  }

  onMounted(() => {
    updateTime()
    timeTimer = setInterval(updateTime, 1000)
  })

  onUnmounted(() => {
    if (timeTimer) clearInterval(timeTimer)
  })

  return { currentTime, greetingMessage }
}
