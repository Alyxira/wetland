<template>
  <section class="mission-panel">
    <header class="mission-head">
      <div>
        <h3>{{ title }}</h3>
        <p>{{ subtitle }}</p>
      </div>
      <button v-if="enabled && missions.length" class="btn btn-dark" type="button" @click="emit('reset-missions')">重置任务</button>
    </header>

    <div v-if="enabled && missions.length" class="mission-summary-grid">
      <article class="metric-card">
        <span class="metric-label">任务总数</span>
        <strong class="metric-value">{{ summary.totalMissions }}</strong>
      </article>
      <article class="metric-card">
        <span class="metric-label">已完成</span>
        <strong class="metric-value">{{ summary.completedMissions }}</strong>
      </article>
      <article class="metric-card">
        <span class="metric-label">已解锁奖励</span>
        <strong class="metric-value">{{ summary.unlockedRewards }}</strong>
      </article>
    </div>

    <div v-if="!enabled || !missions.length" class="empty-state mission-empty-state">
      <strong>{{ emptyState.title || '暂未开放任务' }}</strong>
      <p>{{ emptyState.description || '当前景区尚未配置探索任务。' }}</p>
    </div>

    <template v-else>
      <div class="section-caption">任务列表</div>
      <div class="mission-list">
        <article v-for="mission in missions" :key="mission.id" :class="['mission-card', { tracked: mission.id === trackedMissionId, completed: mission.isCompleted }]">
          <div class="mission-card-head">
            <div class="mission-tags">
              <span class="tag">{{ mission.typeLabel }}</span>
              <span class="tag">{{ mission.statusLabel }}</span>
            </div>
            <button
              v-if="!mission.isCompleted"
              :class="['btn', mission.id === trackedMissionId ? 'btn-primary' : 'btn-dark']"
              type="button"
              @click="emit('track-mission', mission.id)"
            >
              {{ mission.id === trackedMissionId ? '已追踪' : '追踪任务' }}
            </button>
          </div>

          <div class="mission-copy">
            <h4>{{ mission.title }}</h4>
            <p>{{ mission.description }}</p>
          </div>

          <div class="mission-progress">
            <div class="progress-meta">
              <span>进度 {{ mission.progressText }}</span>
              <span>{{ mission.progressPercent }}%</span>
            </div>
            <div class="progress-track">
              <span class="progress-bar" :style="{ width: `${mission.progressPercent}%` }"></span>
            </div>
          </div>

          <div class="section-caption">任务步骤</div>
          <div class="step-list">
            <button
              v-for="step in mission.steps"
              :key="step.id"
              :class="['step-card', { completed: step.completed, actionable: step.canOpenSpot }]"
              type="button"
              @click="step.canOpenSpot && emit('open-spot', step.spotId)"
            >
              <div class="step-state">{{ step.completed ? '已完成' : `步骤 ${step.order}` }}</div>
              <strong>{{ step.title }}</strong>
              <p>{{ step.description || (step.spotName ? `目标看点：${step.spotName}` : '前往目标区域继续探索。') }}</p>
              <span v-if="step.spotName" class="step-spot-name">{{ step.spotName }}</span>
            </button>
          </div>

          <div v-if="mission.rewards?.length" class="reward-area">
            <div class="section-caption">完成奖励</div>
            <div class="reward-list">
              <article v-for="reward in mission.rewards" :key="reward.rewardId" :class="['reward-card', { unlocked: mission.isCompleted }]">
                <span class="reward-icon">{{ reward.icon || '🎁' }}</span>
                <div>
                  <strong>{{ reward.title }}</strong>
                  <p>{{ reward.description }}</p>
                </div>
              </article>
            </div>
          </div>
        </article>
      </div>

      <template v-if="unlockedRewards.length">
        <div class="section-caption">已解锁奖励</div>
        <div class="reward-list unlocked-list">
          <article v-for="reward in unlockedRewards" :key="`${reward.missionId}_${reward.rewardId}`" class="reward-card unlocked">
            <span class="reward-icon">{{ reward.icon || '🎁' }}</span>
            <div>
              <strong>{{ reward.title }}</strong>
              <p>{{ reward.description }}</p>
              <span class="reward-source">来自任务：{{ reward.missionTitle }}</span>
            </div>
          </article>
        </div>
      </template>
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
    default: '探索任务',
  },
  subtitle: {
    type: String,
    default: '',
  },
  summary: {
    type: Object,
    default: () => ({ totalMissions: 0, completedMissions: 0, unlockedRewards: 0, trackedMissionTitle: '' }),
  },
  missions: {
    type: Array,
    default: () => [],
  },
  trackedMissionId: {
    type: String,
    default: '',
  },
  unlockedRewards: {
    type: Array,
    default: () => [],
  },
  emptyState: {
    type: Object,
    default: () => ({ title: '暂未开放任务', description: '当前景区尚未配置探索任务。' }),
  },
});

const emit = defineEmits(['track-mission', 'open-spot', 'reset-missions']);
</script>

<style scoped>
.mission-panel {
  display: grid;
  gap: 14px;
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
}

.mission-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.mission-head h3 {
  margin: 0 0 8px;
  font-size: 24px;
}

.mission-head p,
.mission-copy p,
.step-card p,
.reward-card p,
.reward-source,
.mission-empty-state p {
  margin: 0;
  color: var(--muted-dark);
  line-height: 1.78;
  font-size: 13px;
}

.mission-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.metric-card,
.mission-card,
.step-card,
.reward-card {
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

.mission-list,
.step-list,
.reward-list {
  display: grid;
  gap: 10px;
}

.mission-card {
  display: grid;
  gap: 14px;
  padding: 16px;
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.14);
}

.mission-card.tracked {
  border-color: rgba(var(--brand-soft-rgb), 0.24);
  box-shadow: inset 0 0 0 1px rgba(var(--brand-soft-rgb), 0.08), 0 20px 36px rgba(var(--brand-rgb), 0.16);
}

.mission-card.completed {
  border-color: rgba(var(--brand-rgb), 0.24);
  background: linear-gradient(180deg, rgba(var(--brand-rgb), 0.14), rgba(255, 255, 255, 0.04));
}

.mission-card-head,
.mission-tags,
.progress-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.mission-copy h4 {
  margin: 0 0 8px;
  font-size: 20px;
}

.progress-track {
  position: relative;
  overflow: hidden;
  height: 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
}

.progress-bar {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: inherit;
  background: linear-gradient(135deg, rgba(var(--brand-soft-rgb), 0.92), rgba(var(--brand-rgb), 0.92));
}

.step-card {
  display: grid;
  gap: 8px;
  width: 100%;
  padding: 14px;
  text-align: left;
  border: 1px solid rgba(255, 255, 255, 0.08);
  cursor: default;
}

.step-card.actionable {
  cursor: pointer;
}

.step-card.actionable:hover {
  transform: translateY(-1px);
  border-color: rgba(var(--brand-soft-rgb), 0.28);
  background: linear-gradient(180deg, rgba(var(--brand-soft-rgb), 0.12), rgba(255, 255, 255, 0.04));
}

.step-card.completed {
  border-color: rgba(var(--brand-rgb), 0.24);
  background: linear-gradient(180deg, rgba(var(--brand-rgb), 0.14), rgba(255, 255, 255, 0.04));
}

.step-state,
.reward-source,
.step-spot-name {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.62);
}

.reward-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: start;
  padding: 14px;
}

.reward-card.unlocked {
  border-color: rgba(var(--brand-soft-rgb), 0.24);
  background: linear-gradient(180deg, rgba(var(--brand-soft-rgb), 0.12), rgba(255, 255, 255, 0.04));
  box-shadow: 0 18px 32px rgba(var(--brand-rgb), 0.12);
}

.reward-card strong {
  display: block;
  margin-bottom: 6px;
}

.reward-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: rgba(var(--brand-rgb), 0.12);
  font-size: 20px;
}

.section-caption {
  padding: 0 4px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.56);
}

.mission-empty-state strong {
  display: block;
  margin-bottom: 10px;
  font-size: 18px;
}

@media (max-width: 760px) {
  .mission-summary-grid {
    grid-template-columns: 1fr;
  }

  .mission-head {
    flex-direction: column;
  }
}
</style>
