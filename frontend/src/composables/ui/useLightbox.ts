import { reactive } from 'vue'

export interface LightboxImage {
  src: string
  title?: string
  subtitle?: string
}

interface LightboxState {
  visible: boolean
  images: LightboxImage[]
  activeIndex: number
}

export const lightboxState = reactive<LightboxState>({
  visible: false,
  images: [],
  activeIndex: 0,
})

export function openLightbox(images: LightboxImage[] | LightboxImage | string, startIndex = 0): void {
  const list: LightboxImage[] = Array.isArray(images)
    ? images
    : typeof images === 'string'
      ? [{ src: images }]
      : [images]
  if (!list.length) return
  lightboxState.images = list
  lightboxState.activeIndex = Math.max(0, Math.min(startIndex, list.length - 1))
  lightboxState.visible = true
}

export function closeLightbox(): void {
  lightboxState.visible = false
  lightboxState.images = []
  lightboxState.activeIndex = 0
}
