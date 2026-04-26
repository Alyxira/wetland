<script setup>
import { useRouter } from 'vue-router'
import ChinaMap from '../components/Global/ChinaMap.vue'

const router = useRouter()

const goBackToOverview = () => {
  router.push('/overview')
}
</script>

<template>
  <div class="screen-page">
    <div class="screen-ambient" aria-hidden="true"></div>
    <button class="screen-back-button" type="button" @click="goBackToOverview">
      返回总览
    </button>
    <div class="screen-stage">
      <div class="screen-stage-shell">
        <ChinaMap />
      </div>
    </div>
    <div class="screen-map-attribution">
      地图 GeoJSON
      <span class="screen-map-attribution-separator">|</span>
      数据来自 DataV.GeoAtlas（高德开放平台/阿里云）
    </div>
  </div>
</template>

<style scoped>
:global(html),
:global(body),
:global(#app) {
  width: 100%;
  height: 100%;
}

.screen-page {
  --screen-nav-safe-top: 8px;
  position: relative;
  width: 100%;
  min-height: 100vh;
  height: 100vh;
  background: linear-gradient(160deg, #ece8e3 0%, #e0dbd5 30%, #d5d0c9 60%, #cec9c2 100%);
  overflow: hidden;
}

.screen-ambient {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 14% 18%, rgba(255, 255, 255, 0.34), transparent 28%),
    radial-gradient(circle at 84% 16%, rgba(172, 164, 154, 0.16), transparent 24%),
    radial-gradient(circle at 50% 100%, rgba(118, 106, 92, 0.12), transparent 38%);
}

.screen-stage {
  position: relative;
  width: 100%;
  min-height: 100vh;
  height: 100vh;
}

.screen-stage-shell {
  position: relative;
  width: 100%;
  min-height: 100vh;
  height: 100vh;
}

.screen-back-button {
  position: fixed;
  top: 12px;
  left: 20px;
  z-index: 1400;
  min-height: 42px;
  padding: 0 18px;
  border-radius: 999px;
  border: 1px solid rgba(90, 82, 74, 0.16);
  background: rgba(248, 244, 238, 0.88);
  color: #5b554c;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.08em;
  box-shadow: 0 12px 28px rgba(58, 45, 34, 0.12);
  backdrop-filter: blur(14px);
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.screen-back-button:hover {
  transform: translateY(-1px);
  background: rgba(252, 249, 244, 0.96);
  box-shadow: 0 14px 30px rgba(58, 45, 34, 0.16);
}

.screen-map-attribution {
  position: fixed;
  right: 16px;
  bottom: 12px;
  z-index: 1400;
  max-width: min(44vw, 560px);
  color: rgba(78, 72, 65, 0.82);
  font-size: 11px;
  line-height: 1.25;
  letter-spacing: 0.01em;
  text-align: right;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.7);
  pointer-events: none;
}

.screen-map-attribution-separator {
  margin: 0 8px;
  color: rgba(100, 92, 82, 0.5);
}

@media (max-width: 1280px) {
  .screen-page {
    --screen-nav-safe-top: 8px;
  }
}

@media (max-width: 768px) {
  .screen-map-attribution {
    right: 10px;
    bottom: 10px;
    max-width: calc(100vw - 24px);
    font-size: 10px;
  }
}
</style>
