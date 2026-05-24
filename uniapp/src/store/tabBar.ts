import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useTabBarStore = defineStore('tabBar', () => {
  const selected = ref(0)

  function setSelected(index: number) {
    selected.value = index
  }

  return { selected, setSelected }
})
