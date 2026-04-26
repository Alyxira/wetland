<template>
  <footer class="explore-statusbar glass-dark">
    <div class="status-summary">
      <article class="status-pill status-pill-wide">
        <span>探索模式</span>
        <strong>{{ statusModeLabel }}</strong>
      </article>
      <article class="status-pill">
        <span>当前看点</span>
        <strong>{{ currentSpotName || '等待定位' }}</strong>
      </article>
      <article class="status-pill">
        <span>记录进度</span>
        <strong>{{ discoveredCount }} / {{ totalSpots }}</strong>
      </article>
    </div>

    <div class="status-actions">
      <button class="btn btn-dark action-btn" type="button" @click="emit('open-settings')">
        <span>⚙️</span>
        <strong>更多设置</strong>
      </button>
      <button class="btn btn-dark action-btn" type="button" @click="emit('fit-overview')">
        <span>🧭</span>
        <strong>查看全景</strong>
      </button>
    </div>
  </footer>
</template>

<script setup>
defineProps({
  statusModeLabel: {
    type: String,
    default: '自由漫游中',
  },
  currentSpotName: {
    type: String,
    default: '',
  },
  discoveredCount: {
    type: Number,
    default: 0,
  },
  totalSpots: {
    type: Number,
    default: 0,
  },
});

const emit = defineEmits(['open-settings', 'fit-overview']);
</script>

<style scoped>
.status-summary,
.status-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.status-pill {
  min-width: 170px;
  padding: 12px 14px;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.03));
  color: var(--text-dark);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.02);
}

.status-pill-wide {
  min-width: 190px;
}

.status-pill span {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.58);
}

.status-pill strong {
  display: block;
  font-size: 15px;
}

.action-btn {
  gap: 8px;
  min-height: 50px;
  padding-inline: 18px;
  border-radius: 20px;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.12);
}

.action-btn span {
  font-size: 16px;
}

.action-btn strong {
  font-size: 14px;
  font-weight: 600;
}

@media (max-width: 920px) {
  .status-summary {
    width: 100%;
  }
}
</style>
