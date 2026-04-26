<template>
  <div
    :class="[
      'consult-shell',
      `page-${route.meta.page || 'home'}`,
      {
        'is-fullscreen-page': isFullscreenPage,
        'is-scenic-fullscreen': hideConsultChrome
      }
    ]"
    :style="shellStyle"
  >
    <div class="consult-background"></div>
    <SystemNav v-if="!hideConsultChrome" />

    <main
      :class="[
        'consult-main',
        {
          'consult-main--fullscreen': isFullscreenPage,
          'consult-main--immersive': isImmersivePage,
          'consult-main--scenic': hideConsultChrome
        }
      ]"
    >
      <section v-if="showScenicNav" class="consult-subnav">
        <div class="consult-subnav__eyebrow"></div>
        <div class="consult-subnav__actions">
          <RouterLink
            v-for="item in scenicNavItems"
            :key="item.key"
            :to="item.to"
            :class="['consult-subnav__link', { 'is-active': isScenicNavActive(item.key) }]"
          >
            {{ item.label }}
          </RouterLink>
        </div>
      </section>

      <router-view :key="route.path" />
    </main>

    <Live2DWidget
      v-if="showLive2D"
      :key="`${scenicId}_${route.meta.page || 'home'}`"
      :page="route.meta.page || 'home'"
      ui-mode="explore"
      :scenic-id="scenicId"
    />
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import SystemNav from '../components/SystemNav.vue'
import Live2DWidget from '../components/Consult/Live2DWidget.vue'
import { useSiteStore } from '../stores/site'
import { buildScenicPagePath, buildScenicThemeStyle, normalizeScenicId } from '../utils/scenic'

const route = useRoute()
const store = useSiteStore()

const hasScenicContext = computed(() => Boolean(route.params.scenicId))
const scenicId = computed(() => normalizeScenicId(route.params.scenicId))
const currentScenic = computed(() => (hasScenicContext.value ? store.scenicById(scenicId.value) : null))
const showLive2D = computed(() => hasScenicContext.value && route.meta?.enableAssistant !== false)
const isFullscreenPage = computed(() => route.meta?.page === 'travel-map')
const isExplorePage = computed(() => route.meta?.page === 'explore')
const isCloudPage = computed(() => route.meta?.page === 'cloud')
const isImmersivePage = computed(() => isExplorePage.value || isCloudPage.value)
const hideConsultChrome = computed(() => hasScenicContext.value || isFullscreenPage.value)
const shellStyle = computed(() => (
  {
    ...(currentScenic.value?.theme ? buildScenicThemeStyle(currentScenic.value.theme) : {}),
    ...(hideConsultChrome.value
      ? {
          '--consult-cloud-offset': '0px',
          '--consult-explore-offset': '0px',
          '--consult-page-offset': '0px'
        }
      : {})
  }
))

const showScenicNav = computed(() => hasScenicContext.value && !isImmersivePage.value && !hideConsultChrome.value)

const homePath = computed(() => buildScenicPagePath(scenicId.value, 'home'))
const explorePath = computed(() => buildScenicPagePath(scenicId.value, 'explore'))
const cloudPath = computed(() => buildScenicPagePath(scenicId.value, 'cloud-tour'))

const scenicNavItems = computed(() => [
  {
    key: 'home',
    label: '景区介绍',
    to: homePath.value
  },
  {
    key: 'cloud',
    label: '沉浸云游',
    to: cloudPath.value
  },
  {
    key: 'explore',
    label: '景区探索',
    to: explorePath.value
  }
])

function isScenicNavActive(key) {
  if (key === 'home') {
    return route.path === homePath.value
  }
  if (key === 'explore') {
    return route.path.startsWith(explorePath.value)
  }
  if (key === 'cloud') {
    return route.path.startsWith(cloudPath.value)
  }
  return false
}

watch(
  () => route.params.scenicId,
  async (value) => {
    if (!value) return
    await store.ensureScenic(value).catch(() => null)
  },
  { immediate: true }
)
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@300;400;500;700&family=Noto+Serif+SC:wght@400;600;700;900&display=swap');

:root {
  --bg-light: #f4f1e8;
  --surface-light: rgba(255, 255, 255, 0.84);
  --surface-strong: #fffdf8;
  --bg-dark: #192a23;
  --bg-dark-rgb: 25, 42, 35;
  --panel-dark: rgba(26, 43, 35, 0.82);
  --line-dark: rgba(240, 236, 226, 0.2);
  --text-light: #273129;
  --text-dark: #f8f4e9;
  --muted-light: #6d786f;
  --muted-dark: rgba(248, 244, 233, 0.72);
  --brand: #2f5f4b;
  --brand-deep: #234536;
  --brand-soft: #d6a74c;
  --brand-rgb: 47, 95, 75;
  --brand-soft-rgb: 214, 167, 76;
  --brand-deep-rgb: 35, 69, 54;
  --page-light-start: #f4f1e8;
  --page-light-end: #ece4d3;
}

* {
  box-sizing: border-box;
}

html,
body,
#app {
  min-height: 100%;
}

body {
  margin: 0;
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
  background: var(--bg-light);
  color: var(--text-light);
}

h1,
h2,
h3,
h4,
h5,
h6 {
  font-family: 'Noto Serif SC', 'PingFang SC', serif;
  font-weight: 700;
}

a {
  color: inherit;
  text-decoration: none;
}

button,
input,
textarea {
  font: inherit;
}

img {
  display: block;
  width: 100%;
}

.amap-container img,
.amap-layer img,
.amap-logo img,
.amap-copyright img {
  width: auto !important;
  max-width: none !important;
}

.consult-shell {
  min-height: 100vh;
  position: relative;
  color: var(--text-light);
  background:
    radial-gradient(circle at 5% 10%, rgba(var(--brand-rgb), 0.08), transparent 28%),
    radial-gradient(circle at 96% 92%, rgba(var(--brand-soft-rgb), 0.14), transparent 30%),
    linear-gradient(180deg, var(--page-light-start) 0%, var(--page-light-end) 100%);
}

.consult-shell.page-cloud {
  --consult-cloud-offset: 162px;
  color: var(--text-light);
  background:
    radial-gradient(circle at 10% 14%, rgba(var(--brand-soft-rgb), 0.18), transparent 24%),
    radial-gradient(circle at 86% 82%, rgba(var(--brand-rgb), 0.16), transparent 28%),
    linear-gradient(180deg, #eef4f0 0%, #e5eee8 42%, #edf3ef 100%);
}

.consult-shell.page-explore {
  --consult-explore-offset: 162px;
  background:
    radial-gradient(circle at 8% 12%, rgba(214, 167, 76, 0.2), transparent 26%),
    radial-gradient(circle at 88% 88%, rgba(47, 95, 75, 0.18), transparent 28%),
    linear-gradient(180deg, #f2eee3 0%, #e9e2d2 100%);
}

.consult-background {
  position: fixed;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(90deg, rgba(var(--brand-rgb), 0.05) 0, rgba(var(--brand-rgb), 0.05) 1px, transparent 1px, transparent 120px);
  opacity: 0.4;
}

.consult-main {
  position: relative;
  z-index: 1;
  width: min(1380px, calc(100% - 28px));
  margin: 0 auto;
  padding: 204px 0 72px;
}

.consult-main--scenic {
  width: 100%;
  max-width: none;
  min-height: 100vh;
  padding: 0;
}

.consult-main--fullscreen {
  width: 100%;
  max-width: none;
  min-height: 100vh;
  padding: 0;
}

.consult-main--immersive {
  width: 100%;
  max-width: none;
  min-height: 100vh;
  padding:
    var(--consult-page-offset, var(--consult-explore-offset, var(--consult-cloud-offset, 162px)))
    0
    0;
}

.consult-subnav {
  margin-bottom: 28px;
  padding: 18px;
  border: 1px solid rgba(var(--brand-rgb), 0.14);
  background: rgba(255, 255, 255, 0.74);
  backdrop-filter: blur(14px);
  box-shadow: 0 18px 40px rgba(35, 38, 31, 0.08);
}

.consult-shell.is-scenic-fullscreen .consult-background {
  opacity: 0.22;
}

.consult-subnav__eyebrow {
  margin-bottom: 14px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: var(--muted-light);
}

.consult-subnav__actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.consult-subnav__link {
  min-height: 52px;
  padding: 0 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(39, 49, 41, 0.14);
  background: rgba(255, 255, 255, 0.66);
  color: var(--text-light);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  transition: transform 0.24s ease, border-color 0.24s ease, background 0.24s ease;
}

.consult-subnav__link:hover,
.consult-subnav__link.is-active {
  transform: translateY(-2px);
  border-color: rgba(var(--brand-rgb), 0.42);
  background: rgba(255, 255, 255, 0.92);
}

@media (max-width: 980px) {
  .consult-main {
    width: calc(100% - 20px);
    padding-top: 210px;
  }

  .consult-main--scenic {
    width: 100%;
    padding-top: 0;
  }

  .consult-shell.page-explore {
    --consult-explore-offset: 176px;
  }

  .consult-shell.page-cloud {
    --consult-cloud-offset: 176px;
  }

  .consult-main--fullscreen {
    width: 100%;
    padding-top: 0;
  }

  .consult-subnav__actions {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .consult-main {
    padding-top: 226px;
    padding-bottom: 40px;
  }

  .consult-main--scenic {
    padding: 0;
  }

  .consult-shell.page-explore {
    --consult-explore-offset: 190px;
  }

  .consult-shell.page-cloud {
    --consult-cloud-offset: 190px;
  }

  .consult-main--fullscreen {
    padding: 0;
  }

  .consult-main--immersive {
    padding-top: var(--consult-page-offset, var(--consult-explore-offset, var(--consult-cloud-offset, 190px)));
    padding-bottom: 0;
  }

  .consult-subnav {
    padding: 16px;
  }

  .consult-subnav__actions {
    gap: 10px;
  }
}
</style>
