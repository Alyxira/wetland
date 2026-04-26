import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

const DEFAULT_OPTIONS = {
  duration: 1.05,
  ease: 'expo.out',
  stagger: 0.025,
  delay: 0,
  start: 'top 88%',
  toggleActions: 'play none none reverse'
}

const AUTO_TEXT_SELECTOR = [
  'h1',
  'h2',
  'h3',
  'h4',
  'h5',
  'h6',
  'p',
  'span',
  'strong',
  'em',
  'small',
  'a',
  'button',
  'label',
  'li',
  'dt',
  'dd',
  'th',
  'td',
  'figcaption'
].join(',')

const SKIP_SELECTOR = [
  '[data-y-slice-skip]',
  '[data-y-slice-ready]',
  'script',
  'style',
  'noscript',
  'svg',
  'canvas',
  'video',
  'input',
  'textarea',
  'select',
  '.split-char',
  '.split-word',
  '.split-line',
  '.el-icon',
  '.custom-cursor-aura',
  '.hero-video',
  '.footer-logo-loop'
].join(',')

const getOptions = (options = {}) => ({
  ...DEFAULT_OPTIONS,
  ...options
})

const shouldSkipElement = (el) => {
  if (!el || el.matches(SKIP_SELECTOR) || el.closest('[data-y-slice-skip]')) return true

  const className = typeof el.className === 'string' ? el.className : ''
  if (className.includes('split-')) return true

  const text = el.textContent?.replace(/\s+/g, ' ').trim() ?? ''
  if (!text || text.length < 2) return true

  // 自动模式只处理叶子级文字，避免破坏包含图标、组件或复杂排版的父元素。
  return Array.from(el.children).some((child) => {
    const tagName = child.tagName?.toLowerCase()
    if (tagName === 'br') return false
    return child.textContent?.trim() || child.querySelector?.(AUTO_TEXT_SELECTOR)
  })
}

export const applyYSlice = (el, options = {}) => {
  if (!el || el.dataset.ySliceReady === 'true') return

  const resolvedOptions = getOptions(options)
  const text = el.textContent ?? ''
  const trimmedText = text.replace(/\s+/g, ' ').trim()
  if (!trimmedText) return

  el.dataset.ySliceReady = 'true'
  el.dataset.ySliceOriginal = text
  el.innerHTML = ''

  const inners = Array.from(text).map((char) => {
    const mask = document.createElement('span')
    mask.style.display = 'inline-block'
    mask.style.overflow = 'hidden'
    mask.style.verticalAlign = 'baseline'
    mask.style.marginRight = '-0.02em'
    mask.style.paddingBottom = '0.16em'
    mask.style.marginBottom = '-0.16em'

    const inner = document.createElement('span')
    inner.style.display = 'inline-block'
    inner.style.transform = 'translateY(115%)'
    inner.textContent = char === ' ' ? '\u00A0' : char

    mask.appendChild(inner)
    el.appendChild(mask)
    return inner
  })

  const animation = gsap.to(inners, {
    y: '0%',
    duration: resolvedOptions.duration,
    ease: resolvedOptions.ease,
    stagger: resolvedOptions.stagger,
    delay: resolvedOptions.delay,
    scrollTrigger: {
      trigger: el,
      start: resolvedOptions.start,
      toggleActions: resolvedOptions.toggleActions
    }
  })

  el._ySliceAnim = animation
}

export const cleanupYSlice = (el) => {
  if (!el?._ySliceAnim) return

  el._ySliceAnim.scrollTrigger?.kill()
  el._ySliceAnim.kill()
  el._ySliceAnim = null
}

export const initGlobalYSlice = (root = document, options = {}) => {
  const scope = root instanceof Element ? root : document
  const candidates = scope.querySelectorAll(AUTO_TEXT_SELECTOR)

  candidates.forEach((el) => {
    if (!shouldSkipElement(el)) {
      applyYSlice(el, options)
    }
  })

  ScrollTrigger.refresh()
}

export const startGlobalYSlice = (rootSelector = '#app', options = {}) => {
  const root = document.querySelector(rootSelector)
  if (!root) return null

  let timer = null
  const run = () => {
    window.clearTimeout(timer)
    timer = window.setTimeout(() => initGlobalYSlice(root, options), 80)
  }

  run()

  const observer = new MutationObserver(run)
  observer.observe(root, {
    childList: true,
    subtree: true
  })

  return () => {
    window.clearTimeout(timer)
    observer.disconnect()
  }
}

export default {
  mounted(el, binding) {
    applyYSlice(el, binding.value)
  },

  updated(el, binding) {
    if (binding.value === binding.oldValue || el.dataset.ySliceReady === 'true') return
    cleanupYSlice(el)
    delete el.dataset.ySliceReady
    applyYSlice(el, binding.value)
  },

  unmounted(el) {
    cleanupYSlice(el)
  }
}
