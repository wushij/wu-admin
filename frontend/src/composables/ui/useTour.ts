import { ref, computed } from 'vue'

export interface TourStep {
  /** 目标选择器 */
  selector: string
  /** 步骤标题 */
  title: string
  /** 步骤说明 */
  description: string
  /** 气泡首选方向 */
  placement?: 'top' | 'bottom' | 'left' | 'right'
}

const TOUR_DONE_KEY = 'wu-admin-tour-v1-done'

const active = ref(false)
const currentIndex = ref(0)
const steps = ref<TourStep[]>([])

export function useTour() {
  const currentStep = computed(() => steps.value[currentIndex.value] ?? null)
  const totalSteps = computed(() => steps.value.length)
  const isFirst = computed(() => currentIndex.value <= 0)
  const isLast = computed(() => currentIndex.value >= steps.value.length - 1)

  function startTour(nextSteps: TourStep[], startAt = 0) {
    if (!nextSteps.length) return
    steps.value = nextSteps
    currentIndex.value = Math.max(0, Math.min(startAt, nextSteps.length - 1))
    active.value = true
  }

  function nextStep() {
    if (isLast.value) {
      finishTour()
      return
    }
    currentIndex.value += 1
  }

  function prevStep() {
    if (currentIndex.value > 0) {
      currentIndex.value -= 1
    }
  }

  function finishTour() {
    active.value = false
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(TOUR_DONE_KEY, '1')
    }
  }

  function skipTour() {
    finishTour()
  }

  function hasCompletedTour(): boolean {
    if (typeof localStorage === 'undefined') return true
    return localStorage.getItem(TOUR_DONE_KEY) === '1'
  }

  return {
    active,
    steps,
    currentIndex,
    currentStep,
    totalSteps,
    isFirst,
    isLast,
    startTour,
    nextStep,
    prevStep,
    finishTour,
    skipTour,
    hasCompletedTour,
  }
}
