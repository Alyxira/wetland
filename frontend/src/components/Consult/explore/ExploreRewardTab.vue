<template>
  <section class="reward-panel">
    <header class="reward-head">
      <div>
        <h3>{{ title }}</h3>
        <p>{{ subtitle }}</p>
      </div>
    </header>

    <div v-if="enabled && rewards.length" class="reward-summary-grid">
      <article class="metric-card">
        <span class="metric-label">奖励总数</span>
        <strong class="metric-value">{{ summary.totalRewards }}</strong>
      </article>
      <article class="metric-card">
        <span class="metric-label">已解锁</span>
        <strong class="metric-value">{{ summary.unlockedRewards }}</strong>
      </article>
      <article class="metric-card">
        <span class="metric-label">待解锁</span>
        <strong class="metric-value">{{ summary.lockedRewards }}</strong>
      </article>
    </div>

    <div v-if="!enabled || !rewards.length" class="empty-state reward-empty-state">
      <strong>{{ emptyState.title || '暂未开放奖励图鉴' }}</strong>
      <p>{{ emptyState.description || '当前景区尚未配置奖励内容。' }}</p>
    </div>

    <template v-else>
      <template v-if="unlockedRewards.length">
        <div class="section-caption">已解锁奖励</div>
        <div class="reward-list">
          <article v-for="reward in unlockedRewards" :key="reward.id" :class="['reward-card', 'unlocked', { selected: reward.id === selectedRewardId }]">
            <div class="reward-card-head">
              <span class="reward-icon">{{ reward.icon || '🎁' }}</span>
              <div class="reward-meta">
                <strong>{{ reward.title }}</strong>
                <div class="tag-list compact-tags">
                  <span class="tag">{{ reward.typeLabel }}</span>
                  <span class="tag">{{ reward.rarityLabel }}</span>
                  <span class="tag">{{ reward.statusLabel }}</span>
                </div>
              </div>
            </div>
            <p>{{ reward.description }}</p>
            <div v-if="reward.sourceMissions.length" class="source-list">
              <span class="source-label">来源任务</span>
              <div class="tag-list compact-tags">
                <span v-for="mission in reward.sourceMissions" :key="mission.missionId" class="tag">{{ mission.missionTitle }}</span>
              </div>
            </div>
          </article>
        </div>
      </template>

      <div class="section-caption">待解锁奖励</div>
      <div class="reward-list">
        <article v-for="reward in lockedRewards" :key="reward.id" :class="['reward-card', 'locked', { selected: reward.id === selectedRewardId }]">
          <div class="reward-card-head">
            <span class="reward-icon muted">{{ reward.icon || '🎁' }}</span>
            <div class="reward-meta">
              <strong>{{ reward.title }}</strong>
              <div class="tag-list compact-tags">
                <span class="tag">{{ reward.typeLabel }}</span>
                <span class="tag">{{ reward.rarityLabel }}</span>
                <span class="tag">{{ reward.statusLabel }}</span>
              </div>
            </div>
          </div>
          <p>{{ reward.description }}</p>
          <div v-if="reward.sourceMissions.length" class="source-list">
            <span class="source-label">关联任务</span>
            <div class="task-list">
              <article v-for="mission in reward.sourceMissions" :key="`${reward.id}_${mission.missionId}`" class="task-chip-card">
                <div>
                  <strong>{{ mission.missionTitle }}</strong>
                  <p>{{ mission.isCompleted ? '已完成' : `当前进度 ${mission.progressText || '待开始'}` }}</p>
                </div>
                <button
                  v-if="!mission.isCompleted"
                  class="btn btn-dark"
                  type="button"
                  @click="emit('track-mission', mission.missionId)"
                >
                  追踪任务
                </button>
              </article>
            </div>
          </div>
        </article>
      </div>
    </template>
  </section>
</template>

<script setup>
defineProps({
  enabled: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: '奖励图鉴',
  },
  subtitle: {
    type: String,
    default: '',
  },
  summary: {
    type: Object,
    default: () => ({ totalRewards: 0, unlockedRewards: 0, lockedRewards: 0 }),
  },
  rewards: {
    type: Array,
    default: () => [],
  },
  unlockedRewards: {
    type: Array,
    default: () => [],
  },
  lockedRewards: {
    type: Array,
    default: () => [],
  },
  selectedRewardId: {
    type: String,
    default: '',
  },
  emptyState: {
    type: Object,
    default: () => ({ title: '暂未开放奖励图鉴', description: '当前景区尚未配置奖励内容。' }),
  },
});

const emit = defineEmits(['track-mission']);
</script>

<style scoped>
.reward-panel {
  display: grid;
  gap: 14px;
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
}

.reward-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.reward-head h3 {
  margin: 0 0 8px;
  font-size: 24px;
}

.reward-head p,
.reward-card p,
.task-chip-card p,
.reward-empty-state p {
  margin: 0;
  color: var(--muted-dark);
  line-height: 1.78;
  font-size: 13px;
}

.reward-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.metric-card,
.reward-card,
.task-chip-card {
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.03));
  color: var(--text-dark);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.02);
}

.metric-card {
  display: grid;
  gap: 8px;
  padding: 16px;
  background: linear-gradient(180deg, rgba(var(--brand-soft-rgb), 0.14), rgba(255, 255, 255, 0.04));
  border-color: rgba(var(--brand-soft-rgb), 0.18);
}

.metric-label {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: rgba(255, 255, 255, 0.58);
}

.metric-value {
  font-size: 28px;
  line-height: 1;
}

.reward-list,
.task-list {
  display: grid;
  gap: 10px;
}

.reward-card {
  display: grid;
  gap: 12px;
  padding: 16px;
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.14);
}

.reward-card.unlocked {
  border-color: rgba(var(--brand-soft-rgb), 0.26);
  background: linear-gradient(180deg, rgba(var(--brand-soft-rgb), 0.12), rgba(255, 255, 255, 0.04));
  box-shadow: 0 18px 32px rgba(var(--brand-rgb), 0.12);
}

.reward-card.locked {
  border-color: rgba(255, 255, 255, 0.08);
}

.reward-card.selected {
  border-color: rgba(var(--brand-rgb), 0.34);
  box-shadow: inset 0 0 0 1px rgba(var(--brand-soft-rgb), 0.14), 0 16px 32px rgba(var(--brand-deep-rgb), 0.12);
}

.reward-card-head {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.reward-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: rgba(var(--brand-rgb), 0.12);
  font-size: 22px;
}

.reward-icon.muted {
  filter: grayscale(0.2);
  opacity: 0.72;
}

.reward-meta {
  display: grid;
  gap: 8px;
}

.reward-meta strong,
.task-chip-card strong,
.reward-empty-state strong {
  display: block;
}

.compact-tags {
  gap: 8px;
}

.source-list {
  display: grid;
  gap: 8px;
}

.source-label,
.section-caption {
  padding: 0 4px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.56);
}

.task-chip-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.04));
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12);
}

.reward-empty-state strong {
  margin-bottom: 10px;
  font-size: 18px;
}

@media (max-width: 760px) {
  .reward-summary-grid {
    grid-template-columns: 1fr;
  }

  .task-chip-card {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
