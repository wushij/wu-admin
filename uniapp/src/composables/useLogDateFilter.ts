import { ref } from 'vue'

export function useLogDateFilter() {
  const beginDate = ref('')
  const endDate = ref('')

  function clearDates() {
    beginDate.value = ''
    endDate.value = ''
  }

  function toBeginTime() {
    return beginDate.value ? `${beginDate.value} 00:00:00` : undefined
  }

  function toEndTime() {
    return endDate.value ? `${endDate.value} 23:59:59` : undefined
  }

  function inRange(time?: string) {
    if (!time || (!beginDate.value && !endDate.value)) return true
    const ts = new Date(time).getTime()
    if (Number.isNaN(ts)) return true
    if (beginDate.value) {
      const start = new Date(`${beginDate.value} 00:00:00`).getTime()
      if (ts < start) return false
    }
    if (endDate.value) {
      const end = new Date(`${endDate.value} 23:59:59`).getTime()
      if (ts > end) return false
    }
    return true
  }

  return { beginDate, endDate, clearDates, toBeginTime, toEndTime, inRange }
}
