import { reactive } from 'vue'
import { navigateToParent } from '@/utils/nav-history'

export const h5BackButtonState = reactive({
  visible: false,
})

export function syncH5BackButtonForRoute(route: string) {
  // #ifdef H5
  h5BackButtonState.visible = route.startsWith('pages-sub/')
  // #endif
}

export function triggerH5BackButton() {
  navigateToParent()
}
