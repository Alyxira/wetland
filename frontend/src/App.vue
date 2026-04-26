<template>
  <div id="app">
    <div class="architectural-curtain" ref="curtainContainer">
      <div class="curtain-col" v-for="i in 5" :key="i"></div>
      <div class="curtain-brand" ref="curtainBrand">Wetland OS.</div>
    </div>

    <router-view v-slot="{ Component, route }">
      <transition 
        @leave="onLeave" 
        @enter="onEnter" 
        :css="false"
        mode="out-in"
      >
        <div :key="route.path" class="page-transition-wrapper">
          <component :is="Component" />
        </div>
      </transition>
    </router-view>
    
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import gsap from 'gsap'
import { useSiteStore } from './stores/site'
import { normalizeScenicId } from './utils/scenic'

const curtainContainer = ref(null)
const curtainBrand = ref(null)
const route = useRoute()
const siteStore = useSiteStore()
let latestTitleToken = 0

const getCurtainCols = () => curtainContainer.value?.querySelectorAll('.curtain-col') ?? []

const applyDocumentTitle = (title) => {
  document.title = title
}

watch(
  () => route.fullPath,
  async () => {
    const titleToken = ++latestTitleToken
    const baseTitle = String(route.meta?.title || 'Wetland OS')
    applyDocumentTitle(baseTitle)

    if (!route.params?.scenicId) {
      return
    }

    const scenicId = normalizeScenicId(route.params.scenicId)
    const cachedScenic = siteStore.scenicById(scenicId)
    if (cachedScenic?.name) {
      applyDocumentTitle(`${cachedScenic.name} · ${baseTitle}`)
      return
    }

    try {
      const scenic = await siteStore.ensureScenic(scenicId)
      if (titleToken !== latestTitleToken) return
      if (scenic?.name) {
        applyDocumentTitle(`${scenic.name} · ${baseTitle}`)
      }
    } catch {
      if (titleToken !== latestTitleToken) return
      applyDocumentTitle(baseTitle)
    }
  },
  { immediate: true }
)

/**
 * 离开当前页面：恢复最初的幕布升起品牌转场
 */
const onLeave = (el, done) => {
  const cols = getCurtainCols()
  gsap.killTweensOf([el, cols, curtainBrand.value])
  gsap.set(curtainContainer.value, { autoAlpha: 1 })

  const tl = gsap.timeline({ onComplete: done })
  
  tl.to(el, { 
    opacity: 0, 
    duration: 0.5, 
    ease: "power2.inOut" 
  }, 0)

  tl.set(cols, { transformOrigin: "bottom center" }, 0)
  tl.fromTo(cols, 
    { scaleY: 0 }, 
    { scaleY: 1, duration: 0.6, stagger: 0.05, ease: "expo.inOut" }, 
    0
  )

  tl.fromTo(curtainBrand.value,
    { opacity: 0, y: 15, letterSpacing: "0px" },
    { opacity: 1, y: 0, letterSpacing: "4px", duration: 0.5, ease: "power2.out" },
    "-=0.3"
  )
}

/**
 * 进入新页面：恢复最初的幕布向上收起
 */
const onEnter = (el, done) => {
  const cols = getCurtainCols()
  gsap.killTweensOf([el, cols, curtainBrand.value])
  gsap.set(curtainContainer.value, { autoAlpha: 1 })
  const tl = gsap.timeline({ 
    onComplete: () => {
      gsap.set(curtainContainer.value, { autoAlpha: 0 })
      done()
    }
  })
  
  tl.to(curtainBrand.value, { opacity: 0, y: -15, duration: 0.3, ease: "power2.in" }, 0)
  tl.set(cols, { transformOrigin: "top center" }, 0)
  tl.to(cols, { scaleY: 0, duration: 0.6, stagger: 0.05, ease: "expo.inOut" }, 0.2)
  tl.fromTo(el, 
    { opacity: 0 }, 
    { opacity: 1, duration: 0.8, ease: "power2.out" }, 
    0.4
  )
}
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@400;500;600&family=Manrope:wght@400;500;600;700&display=swap');

:root {
  color-scheme: light;
  --bg-base: #f4f7fb;
  --bg-elevated: rgba(255, 255, 255, 0.82);
  --bg-panel: rgba(255, 255, 255, 0.68);
  --bg-panel-strong: rgba(244, 248, 255, 0.92);
  --bg-soft: rgba(255, 255, 255, 0.74);
  --text-primary: rgba(24, 35, 52, 0.96);
  --text-secondary: rgba(58, 76, 102, 0.78);
  --text-muted: rgba(90, 108, 133, 0.58);
  --line-soft: rgba(130, 153, 189, 0.18);
  --line-strong: rgba(108, 136, 182, 0.32);
  --accent: #5e88d8;
  --accent-soft: rgba(94, 136, 216, 0.14);
  --accent-glow: rgba(94, 136, 216, 0.18);
  --success: #8fd8b3;
  --warning: #f1cf8a;
  --danger: #ff8c8c;
  --shadow-soft: 0 24px 80px rgba(125, 147, 182, 0.18);
  --shadow-panel: 0 20px 60px rgba(148, 167, 198, 0.16);
  --radius-xs: 12px;
  --radius-sm: 18px;
  --radius-md: 28px;
  --radius-lg: 40px;
  --container-width: min(1380px, calc(100% - 64px));
  --font-serif: 'Cormorant Garamond', 'STSong', 'Songti SC', 'Noto Serif SC', serif;
  --font-sans: 'Manrope', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  --transition-fast: 0.28s ease;
  --transition-slow: 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

html {
  box-sizing: border-box;
  /* 修复：改为 auto，避免原生平滑滚动与 Lenis 冲突 */
  scroll-behavior: auto;
  background:
    radial-gradient(circle at top, rgba(181, 209, 255, 0.42), transparent 36%),
    linear-gradient(180deg, #f9fbff 0%, #eef4fb 52%, #e8eef7 100%);
}

body {
  margin: 0;
  min-width: 320px;
  min-height: 100%;
  background: transparent;
  color: var(--text-primary);
  font-family: var(--font-sans);
  overflow-x: hidden;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

body::before,
body::after {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: -2;
}

body::before {
  background:
    radial-gradient(circle at 15% 20%, rgba(182, 213, 255, 0.4), transparent 28%),
    radial-gradient(circle at 82% 18%, rgba(198, 234, 220, 0.24), transparent 25%),
    radial-gradient(circle at 50% 100%, rgba(255, 255, 255, 0.88), transparent 34%);
}

body::after {
  z-index: -1;
  opacity: 0.4;
  background-image:
    linear-gradient(rgba(129, 156, 197, 0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(129, 156, 197, 0.08) 1px, transparent 1px);
  background-size: 72px 72px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.72), rgba(0, 0, 0, 0.16));
}

*, *::before, *::after {
  box-sizing: inherit;
}

::selection {
  background: rgba(94, 136, 216, 0.22);
  color: #10233f;
}

a {
  color: inherit;
  text-decoration: none;
}

button,
input,
textarea,
select {
  font: inherit;
}

button {
  color: inherit;
}

img,
svg,
video,
canvas {
  display: block;
  max-width: 100%;
}

#app {
  width: 100%;
  min-height: 100vh;
  position: relative;
  isolation: isolate;
}

#app::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.16), rgba(230, 238, 248, 0.32));
  z-index: -1;
}

h1,
h2,
h3,
h4,
h5,
h6 {
  margin: 0;
  font-family: var(--font-serif);
  font-weight: 500;
  letter-spacing: 0.02em;
}

p {
  margin: 0;
  color: var(--text-secondary);
}

.page-transition-wrapper {
  width: 100%;
  min-height: 100vh;
}

.app-shell,
.page-shell,
.section-shell {
  width: var(--container-width);
  margin: 0 auto;
}

.glass-panel {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(244, 248, 255, 0.62));
  background-color: var(--bg-panel);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-panel);
  backdrop-filter: blur(18px);
}

.section-kicker {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--text-muted);
  font-size: 0.78rem;
  letter-spacing: 0.24em;
  text-transform: uppercase;
}

.section-kicker::before {
  content: '';
  width: 42px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--line-strong));
}

.editorial-title {
  font-size: clamp(3.1rem, 7vw, 6.4rem);
  line-height: 0.98;
}

.editorial-copy {
  max-width: 62ch;
  font-size: 1rem;
  line-height: 1.85;
}

.interactive-ring {
  position: relative;
  overflow: hidden;
}

.interactive-ring::after {
  content: '';
  position: absolute;
  inset: auto -15% -45% auto;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  border: 1px solid rgba(133, 158, 195, 0.18);
  box-shadow:
    0 0 0 52px rgba(173, 194, 227, 0.08),
    0 0 0 104px rgba(173, 194, 227, 0.04);
  pointer-events: none;
}

.architectural-curtain {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999999;
  pointer-events: none;
  display: flex;
  visibility: hidden;
  opacity: 0;
}

.curtain-col {
  flex: 1;
  height: 100%;
  background:
    linear-gradient(180deg, rgba(236, 242, 251, 0.98), rgba(220, 229, 242, 0.98)),
    #e7edf6;
  transform: scaleY(0);
  border-right: 1px solid rgba(119, 145, 183, 0.12);
  box-shadow: inset 0 0 40px rgba(255, 255, 255, 0.38);
}

.curtain-brand {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #24364f;
  font-family: var(--font-serif);
  font-size: clamp(2.2rem, 4vw, 3.2rem);
  font-weight: 500;
  letter-spacing: 0.3em;
  text-transform: uppercase;
  opacity: 0;
  text-shadow: 0 0 32px rgba(128, 164, 221, 0.22);
}

::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

::-webkit-scrollbar-track {
  background: rgba(180, 198, 225, 0.18);
}

::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(94, 136, 216, 0.56), rgba(94, 136, 216, 0.22));
  border: 2px solid rgba(241, 246, 252, 0.92);
  border-radius: 999px;
}

::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(94, 136, 216, 0.72), rgba(94, 136, 216, 0.32));
}

@media (max-width: 768px) {
  :root {
    --container-width: calc(100% - 32px);
    --radius-md: 22px;
    --radius-lg: 28px;
  }

  body::after {
    background-size: 48px 48px;
  }

  .editorial-title {
    font-size: clamp(2.6rem, 12vw, 4.2rem);
  }
}

/* =======================================================
   Lenis Smooth Scroll Global Resets (关键系统护甲)
   ======================================================= */
html.lenis, 
html.lenis body {
  height: auto;
}

.lenis.lenis-smooth {
  scroll-behavior: auto !important;
}

/* 防止触达页面顶部/底部时的系统默认弹性回弹，破坏 3D 沉浸感 */
.lenis.lenis-smooth [data-lenis-prevent] {
  overscroll-behavior: contain;
}

.lenis.lenis-stopped {
  overflow: hidden;
}

/* 防止页面中有 iframe（如 B站/YouTube 视频）时鼠标滚轮失效 */
.lenis.lenis-smooth iframe {
  pointer-events: none;
}
</style>
