<template>
  <header class="explore-header glass-dark">
    <div class="header-copy">
      <span class="header-eyebrow">景区探索工作台</span>
      <h1>{{ title }}</h1>
    </div>

    <div class="header-status">
      <span class="chip chip-dark">已发现 {{ discoveredCount }} / {{ totalSpots }}</span>
      <span class="chip chip-dark">当前看点：{{ currentSpotName || '等待定位' }}</span>
      <span class="chip chip-dark">{{ statusModeLabel }}</span>
    </div>

    <div class="header-actions">
      <button
        v-if="missionEnabled"
        class="toolbar-btn btn btn-dark badge-btn toolbar-action"
        type="button"
        @click="emit('open-mission')"
      >
        <span class="toolbar-icon">🎯</span>
        <span>探索任务</span>
        <span v-if="pendingMissionCount > 0" class="toolbar-badge">{{ pendingMissionCount }}</span>
      </button>
      <button
        v-if="rewardEnabled"
        class="toolbar-btn btn btn-dark toolbar-action"
        type="button"
        @click="emit('open-reward')"
      >
        <span class="toolbar-icon">🏆</span>
        <span>奖励图鉴</span>
      </button>
      <RouterLink class="btn btn-dark toolbar-action" :to="homeHref">
        <span class="toolbar-icon">↩</span>
        <span>返回详情</span>
      </RouterLink>
      <RouterLink class="btn btn-dark toolbar-action" to="/">
        <span class="toolbar-icon">🔄</span>
        <span>切换景区</span>
      </RouterLink>
      <RouterLink v-if="showCloudTour" class="btn btn-dark toolbar-action" :to="cloudHref">
        <span class="toolbar-icon">🌥</span>
        <span>沉浸云游</span>
      </RouterLink>
      <button v-if="showRouteCustomize" class="btn btn-primary toolbar-action" type="button" @click="emit('open-route')">
        <span class="toolbar-icon">🗺️</span>
        <span>{{ routeActionLabel }}</span>
      </button>
    </div>
  </header>
</template>

<script setup>
import { RouterLink } from 'vue-router';

defineProps({
  title: {
    type: String,
    default: '景区探索',
  },
  discoveredCount: {
    type: Number,
    default: 0,
  },
  totalSpots: {
    type: Number,
    default: 0,
  },
  currentSpotName: {
    type: String,
    default: '',
  },
  statusModeLabel: {
    type: String,
    default: '自由漫游中',
  },
  homeHref: {
    type: [String, Object],
    default: '/',
  },
  cloudHref: {
    type: [String, Object],
    default: '/cloud-tour',
  },
  showCloudTour: {
    type: Boolean,
    default: true,
  },
  showRouteCustomize: {
    type: Boolean,
    default: true,
  },
  missionEnabled: {
    type: Boolean,
    default: false,
  },
  rewardEnabled: {
    type: Boolean,
    default: false,
  },
  pendingMissionCount: {
    type: Number,
    default: 0,
  },
  routeActionLabel: {
    type: String,
    default: '路线定制',
  },
});

const emit = defineEmits(['open-route', 'open-mission', 'open-reward']);
</script>

<style scoped>
.header-copy {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.header-eyebrow {
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(var(--brand-soft-rgb), 0.92);
}

.header-copy h1 {
  margin: 0;
  font-size: 30px;
  line-height: 1.04;
}

.header-status,
.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.header-actions {
  justify-content: flex-end;
}

.toolbar-action {
  gap: 8px;
  min-height: 46px;
  padding-inline: 18px;
}

.toolbar-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
}

.toolbar-btn {
  position: relative;
}

.badge-btn {
  padding-right: 42px;
}

.toolbar-badge {
  position: absolute;
  top: 7px;
  right: 10px;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #ff7d66, #ff4f4f);
  box-shadow: 0 8px 16px rgba(255, 79, 79, 0.28);
}

@media (max-width: 1380px) {
  .header-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 760px) {
  .header-copy h1 {
    font-size: 24px;
  }

  .header-eyebrow {
    font-size: 10px;
  }
}
</style>
