import { ref, computed } from 'vue'

export function useFormDirty(getSnapshot: () => unknown) {
  const baseline = ref<string | null>(null)

  const isDirty = computed(() => {
    if (baseline.value === null) return false
    return JSON.stringify(getSnapshot()) !== baseline.value
  })

  function markClean() {
    baseline.value = JSON.stringify(getSnapshot())
  }

  function resetBaseline() {
    markClean()
  }

  return { isDirty, markClean, resetBaseline }
}
