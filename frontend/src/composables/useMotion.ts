/**
 * 16 种标准动效工程化 Composable 工具集
 * 严格保证：
 * - 检测 prefers-reduced-motion 与精细指针设备
 * - 所有 rAF / IntersectionObserver / Timer 在组件卸载时自动清理
 */
import { ref, watch, onMounted, onUnmounted, type Ref } from 'vue'

export function prefersReducedMotion(): boolean {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return false
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

export function supportsFinePointer(): boolean {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return false
  return window.matchMedia('(hover: hover) and (pointer: fine)').matches
}

/**
 * 13. Count Up：数值递增（带 tabular-nums 配合，rAF 结束后立即停止）
 */
export function useCountUp(
  target: Ref<number> | (() => number),
  options: { duration?: number } = {},
) {
  const duration = options.duration ?? 550
  const displayValue = ref(0)
  let rafId = 0

  const getTarget = () => (typeof target === 'function' ? target() : target.value)

  function animateTo(nextVal: number) {
    if (rafId) cancelAnimationFrame(rafId)
    const end = Number.isFinite(nextVal) ? nextVal : 0
    if (prefersReducedMotion() || duration <= 0) {
      displayValue.value = Math.round(end)
      return
    }
    const start = displayValue.value
    if (start === end) return
    const startAt = performance.now()
    const step = (now: number) => {
      const progress = Math.min(1, (now - startAt) / duration)
      // easeOutExpo 缓动
      const eased = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress)
      displayValue.value = Math.round(start + (end - start) * eased)
      if (progress < 1) {
        rafId = requestAnimationFrame(step)
      } else {
        displayValue.value = Math.round(end)
        rafId = 0
      }
    }
    rafId = requestAnimationFrame(step)
  }

  onMounted(() => {
    animateTo(getTarget())
  })

  watch(
    () => getTarget(),
    (val) => animateTo(val),
  )

  onUnmounted(() => {
    if (rafId) cancelAnimationFrame(rafId)
  })

  return { displayValue }
}

/**
 * 4. Magnetic Button：仅限核心 CTA 的轻磁吸偏移（触屏与 reduced-motion 自动禁用）
 */
export function useMagneticButton(elRef: Ref<HTMLElement | null>, strength = 0.22) {
  let rafId = 0

  function onPointerMove(e: PointerEvent) {
    const el = elRef.value
    if (!el || prefersReducedMotion() || !supportsFinePointer()) return
    const rect = el.getBoundingClientRect()
    const dx = Math.max(-10, Math.min(10, (e.clientX - (rect.left + rect.width / 2)) * strength))
    const dy = Math.max(-6, Math.min(6, (e.clientY - (rect.top + rect.height / 2)) * strength))
    if (rafId) cancelAnimationFrame(rafId)
    rafId = requestAnimationFrame(() => {
      el.style.transform = `translate3d(${dx.toFixed(1)}px, ${dy.toFixed(1)}px, 0)`
      rafId = 0
    })
  }

  function onPointerLeave() {
    const el = elRef.value
    if (rafId) {
      cancelAnimationFrame(rafId)
      rafId = 0
    }
    if (el) el.style.transform = ''
  }

  onMounted(() => {
    const el = elRef.value
    if (!el) return
    el.addEventListener('pointermove', onPointerMove)
    el.addEventListener('pointerleave', onPointerLeave)
  })

  onUnmounted(() => {
    if (rafId) cancelAnimationFrame(rafId)
    const el = elRef.value
    if (!el) return
    el.removeEventListener('pointermove', onPointerMove)
    el.removeEventListener('pointerleave', onPointerLeave)
  })
}

/**
 * 5. Spotlight：局部 CSS 变量驱动的鼠标聚光灯
 */
export function useSpotlight(elRef: Ref<HTMLElement | null>) {
  let rafId = 0

  function onPointerMove(e: PointerEvent) {
    const el = elRef.value
    if (!el || prefersReducedMotion() || !supportsFinePointer()) return
    const rect = el.getBoundingClientRect()
    const x = `${(e.clientX - rect.left).toFixed(1)}px`
    const y = `${(e.clientY - rect.top).toFixed(1)}px`
    if (rafId) cancelAnimationFrame(rafId)
    rafId = requestAnimationFrame(() => {
      el.style.setProperty('--spot-x', x)
      el.style.setProperty('--spot-y', y)
      rafId = 0
    })
  }

  onMounted(() => {
    elRef.value?.addEventListener('pointermove', onPointerMove)
  })

  onUnmounted(() => {
    if (rafId) cancelAnimationFrame(rafId)
    elRef.value?.removeEventListener('pointermove', onPointerMove)
  })
}

/**
 * 7. 3D Tilt：轻角度空间倾斜（严格限制最大倾角 <= 6deg）
 */
export function useTilt3D(elRef: Ref<HTMLElement | null>, maxDeg = 5) {
  let rafId = 0
  const clampedMax = Math.min(6, Math.max(1, maxDeg))

  function onPointerMove(e: PointerEvent) {
    const el = elRef.value
    if (!el || prefersReducedMotion() || !supportsFinePointer()) return
    const rect = el.getBoundingClientRect()
    const px = (e.clientX - rect.left) / rect.width - 0.5
    const py = (e.clientY - rect.top) / rect.height - 0.5
    const rx = (-py * clampedMax * 2).toFixed(2)
    const ry = (px * clampedMax * 2).toFixed(2)
    if (rafId) cancelAnimationFrame(rafId)
    rafId = requestAnimationFrame(() => {
      el.style.transform = `perspective(900px) rotateX(${rx}deg) rotateY(${ry}deg)`
      rafId = 0
    })
  }

  function onPointerLeave() {
    const el = elRef.value
    if (rafId) {
      cancelAnimationFrame(rafId)
      rafId = 0
    }
    if (el) el.style.transform = ''
  }

  onMounted(() => {
    const el = elRef.value
    if (!el) return
    el.addEventListener('pointermove', onPointerMove)
    el.addEventListener('pointerleave', onPointerLeave)
  })

  onUnmounted(() => {
    if (rafId) cancelAnimationFrame(rafId)
    const el = elRef.value
    if (!el) return
    el.removeEventListener('pointermove', onPointerMove)
    el.removeEventListener('pointerleave', onPointerLeave)
  })
}

/**
 * 8. Scroll Reveal：IntersectionObserver 触发一次即 unobserve
 */
export function useScrollReveal(rootRef: Ref<HTMLElement | null>, selector = '.motion-reveal') {
  let observer: IntersectionObserver | null = null

  onMounted(() => {
    const root = rootRef.value
    if (!root) return
    const items = Array.from(root.querySelectorAll<HTMLElement>(selector))
    if (prefersReducedMotion() || typeof IntersectionObserver === 'undefined') {
      items.forEach((item) => item.classList.add('is-revealed'))
      return
    }
    observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-revealed')
            observer?.unobserve(entry.target)
          }
        }
      },
      { threshold: 0.08 },
    )
    items.forEach((item) => observer?.observe(item))
  })

  onUnmounted(() => {
    observer?.disconnect()
    observer = null
  })
}

/**
 * 15. Text Scramble：极短科技文字解码效果（仅限短标题/状态码，并保留完整 aria-label）
 */
const SCRAMBLE_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'

export function useTextScramble(targetText: Ref<string> | string, duration = 420) {
  const initial = typeof targetText === 'string' ? targetText : targetText.value
  const displayText = ref(initial)
  let rafId = 0

  function run(next: string) {
    if (rafId) cancelAnimationFrame(rafId)
    if (prefersReducedMotion() || !next) {
      displayText.value = next
      return
    }
    const startAt = performance.now()
    const chars = next.split('')
    const step = (now: number) => {
      const progress = Math.min(1, (now - startAt) / duration)
      const lockedCount = Math.floor(progress * chars.length)
      displayText.value = chars
        .map((ch, idx) => {
          if (ch === ' ' || idx < lockedCount) return ch
          return SCRAMBLE_CHARS[Math.floor(Math.random() * SCRAMBLE_CHARS.length)]
        })
        .join('')
      if (progress < 1) {
        rafId = requestAnimationFrame(step)
      } else {
        displayText.value = next
        rafId = 0
      }
    }
    rafId = requestAnimationFrame(step)
  }

  onMounted(() => {
    run(typeof targetText === 'string' ? targetText : targetText.value)
  })

  if (typeof targetText !== 'string') {
    watch(targetText, (val) => run(val))
  }

  onUnmounted(() => {
    if (rafId) cancelAnimationFrame(rafId)
  })

  return { displayText, trigger: () => run(typeof targetText === 'string' ? targetText : targetText.value) }
}

/**
 * 14. Typewriter：轻量化受控打字机切片文本（卸载时清理定时器）
 */
export function useTypewriter(sourceText: Ref<string> | (() => string), speedMs = 32) {
  const output = ref('')
  const typing = ref(false)
  let timer: ReturnType<typeof setInterval> | null = null

  function clear() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  function start(text: string) {
    clear()
    if (prefersReducedMotion() || !text) {
      output.value = text
      typing.value = false
      return
    }
    output.value = ''
    typing.value = true
    let idx = 0
    timer = setInterval(() => {
      idx += 1
      output.value = text.slice(0, idx)
      if (idx >= text.length) {
        clear()
        typing.value = false
      }
    }, speedMs)
  }

  const getVal = () => (typeof sourceText === 'function' ? sourceText() : sourceText.value)

  onMounted(() => start(getVal()))
  watch(getVal, (val) => start(val))
  onUnmounted(clear)

  return { output, typing }
}
