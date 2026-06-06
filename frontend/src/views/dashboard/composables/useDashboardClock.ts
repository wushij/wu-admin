import { ref, computed, onMounted, onUnmounted } from 'vue'

export function useDashboardClock() {
  const currentTime = ref('')
  let timeTimer: ReturnType<typeof setInterval> | null = null

  const greetingMessage = computed(() => {
    const hour = new Date().getHours()
    if (hour < 6) return '夜深了，注意休息'
    if (hour < 9) return '早上好，开启美好的一天'
    if (hour < 12) return '上午好，工作顺利'
    if (hour < 14) return '中午好，记得休息'
    if (hour < 18) return '下午好，继续加油'
    if (hour < 22) return '晚上好，辛苦了'
    return '夜深了，早点休息'
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
