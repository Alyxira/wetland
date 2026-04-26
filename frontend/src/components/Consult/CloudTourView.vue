<template>
  <main class="cloud-page">
    <section v-if="loading" class="state-card glass-dark">
      <strong>正在进入沉浸云游</strong>
      <div>正在连接全景资源与场景信息。</div>
    </section>

    <section v-else-if="error" class="state-card glass-dark">
      <strong>云游加载失败</strong>
      <div>{{ error }}</div>
    </section>

    <section v-else-if="!cloudData.enabled || (!cloudData.viewerUrl && !cloudData.scenes?.length)" class="state-card glass-dark">
      <strong>{{ cloudData.emptyState?.title || '暂未开放云游' }}</strong>
      <div>{{ cloudData.emptyState?.description || '当前景区尚未配置可用的云游资源。' }}</div>
      <div class="empty-actions">
        <RouterLink class="btn btn-dark" :to="homePath">返回景区首页</RouterLink>
      </div>
    </section>

    <section v-else class="cloud-stage">
      <RouterLink class="cloud-back" :to="homePath">返回景区首页</RouterLink>

      <section class="viewer-shell">
      <button
        v-if="showSceneHint && cloudData.viewerUrl"
        type="button"
        class="scene-hint"
        @click="showSceneHint = false"
        aria-label="关闭场景切换提示"
      >
        <span class="scene-hint-title">提示</span>
        <span class="scene-hint-text">点击右下角<b>列表按钮</b>切换场景</span>
        <span class="scene-hint-arrow">↘</span>
        <span class="scene-hint-close">×</span>
      </button>

      <iframe
        v-if="cloudData.viewerUrl"
        class="viewer-frame"
        :src="cloudData.viewerUrl"
        title="沉浸云游"
        allowfullscreen
      ></iframe>

      <section v-else class="scene-stage" :style="fallbackSceneStyle">
        <div class="scene-stage__veil"></div>
      </section>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { useSiteStore } from '../stores/site';
import { buildScenicPagePath, normalizeScenicId } from '../utils/scenic';

const store = useSiteStore();
const route = useRoute();

const loading = ref(true);
const error = ref('');
const showSceneHint = ref(true);

const scenicId = computed(() => normalizeScenicId(route.params.scenicId));
const cloudData = computed(() => store.cloudById(scenicId.value) || {
  title: '沉浸云游',
  subtitle: '',
  viewerUrl: '',
  scenes: [],
  enabled: false,
  emptyState: {
    title: '暂未开放云游',
    description: '当前景区尚未配置可用的云游资源。',
  },
});

const homePath = computed(() => buildScenicPagePath(scenicId.value, 'home'));
const fallbackScene = computed(() => {
  const scenes = Array.isArray(cloudData.value.scenes) ? cloudData.value.scenes : [];
  return scenes[0] || null;
});
const fallbackSceneStyle = computed(() => (
  fallbackScene.value?.image
    ? { backgroundImage: `linear-gradient(180deg, rgba(14, 30, 22, 0.12), rgba(14, 30, 22, 0.52)), url("${fallbackScene.value.image}")` }
    : {}
));

async function loadPage() {
  loading.value = true;
  error.value = '';
  try {
    await Promise.all([
      store.ensureScenic(scenicId.value),
      store.ensureCloud(scenicId.value),
    ]);
    showSceneHint.value = true;
  } catch (err) {
    error.value = err?.message || '云游内容加载失败';
  } finally {
    loading.value = false;
  }
}

watch(
  () => route.params.scenicId,
  () => {
    loadPage();
  },
  { immediate: true },
);
</script>

<style scoped>
.cloud-page {
  min-height: calc(100dvh - var(--consult-cloud-offset, 162px));
  padding: 0;
}

.cloud-stage {
  position: relative;
  width: 100%;
  min-height: calc(100dvh - var(--consult-cloud-offset, 162px));
}

.empty-actions {
  margin-top: 18px;
  display: inline-flex;
  gap: 10px;
  flex-wrap: wrap;
}

.viewer-shell {
  position: relative;
  width: 100%;
  height: calc(100dvh - var(--consult-cloud-offset, 162px));
  min-height: calc(100dvh - var(--consult-cloud-offset, 162px));
  overflow: hidden;
  background:
    radial-gradient(circle at 14% 16%, rgba(var(--brand-soft-rgb), 0.24), transparent 22%),
    radial-gradient(circle at 88% 78%, rgba(var(--brand-rgb), 0.24), transparent 26%),
    linear-gradient(180deg, #dce9e1 0%, #cfded4 100%);
}

.viewer-frame {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  border: 0;
}

.scene-stage {
  position: relative;
  inset: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(24, 44, 34, 0.28);
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
}

.scene-stage__veil {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(234, 243, 236, 0.08), rgba(20, 43, 33, 0.18)),
    radial-gradient(circle at 50% 50%, transparent 32%, rgba(15, 33, 25, 0.12) 100%);
}

.cloud-back {
  position: absolute;
  top: 18px;
  left: 18px;
  z-index: 3;
  min-height: 44px;
  border-radius: 999px;
  padding: 0 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(39, 73, 58, 0.18);
  background: rgba(255, 253, 247, 0.86);
  color: #234536;
  backdrop-filter: blur(14px);
  box-shadow: 0 14px 30px rgba(20, 42, 34, 0.16);
  letter-spacing: 0.08em;
  transition: transform 0.22s ease, box-shadow 0.22s ease, background 0.22s ease;
}

.cloud-back:hover {
  transform: translateY(-1px);
  box-shadow: 0 18px 34px rgba(20, 42, 34, 0.2);
  background: rgba(255, 254, 249, 0.94);
}

.scene-hint {
  position: absolute;
  right: 56px;
  bottom: 54px;
  z-index: 2;
  max-width: 240px;
  display: grid;
  gap: 2px;
  text-align: left;
  border: 1px solid rgba(255, 250, 240, 0.36);
  border-radius: 12px;
  background: rgba(255, 252, 245, 0.72);
  color: #214236;
  padding: 10px 12px;
  backdrop-filter: blur(10px);
  box-shadow: 0 12px 24px rgba(20, 42, 34, 0.16);
  cursor: pointer;
}

.scene-hint-title {
  font-size: 11px;
  opacity: 0.72;
}

.scene-hint-text {
  font-size: 12px;
  line-height: 1.5;
}

.scene-hint-text b {
  font-size: 12px;
}

.scene-hint-close {
  justify-self: end;
  font-size: 14px;
  opacity: 0.86;
  line-height: 1;
}

.scene-hint-arrow {
  position: absolute;
  right: -20px;
  bottom: -8px;
  color: rgba(255, 253, 247, 0.96);
  font-size: 18px;
  line-height: 1;
  text-shadow: 0 2px 8px rgba(20, 42, 34, 0.18);
}

@media (max-width: 760px) {
  .viewer-shell {
    height: calc(100dvh - var(--consult-cloud-offset, 190px));
    min-height: calc(100dvh - var(--consult-cloud-offset, 190px));
  }

  .scene-hint {
    right: 46px;
    bottom: calc(44px + env(safe-area-inset-bottom));
    max-width: min(68vw, 220px);
    padding: 8px 10px;
  }

  .scene-hint-arrow {
    right: -18px;
    bottom: -6px;
  }

  .cloud-back {
    top: 14px;
    left: 14px;
    min-height: 40px;
    padding: 0 16px;
  }
}
</style>
