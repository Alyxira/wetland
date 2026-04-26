<template>
  <aside class="explore-focus glass-dark">
    <div class="panel-head explore-panel-head">
      <div>
        <h2 class="panel-title">{{ showRouteTab && activeTab === 'route' ? '推荐路线' : '当前看点' }}</h2>
        <p class="panel-copy">{{ showRouteTab && activeTab === 'route' ? '查看当前路线建议与停靠点顺序。' : '聚焦当前看点，并快速继续浏览。' }}</p>
      </div>
      <div class="panel-head-actions">
        <span class="chip chip-dark">{{ showRouteTab && activeTab === 'route' ? '路线模式' : focusStatus }}</span>
        <button class="panel-minimize" type="button" @click="emit('toggle-minimize')">－</button>
      </div>
    </div>

    <div class="panel-tabs compact-tabs">
      <button :class="['panel-tab', { active: activeTab === 'spot' }]" type="button" @click="emit('change-tab', 'spot')">当前看点</button>
      <button v-if="showRouteTab" :class="['panel-tab', { active: activeTab === 'route' }]" type="button" @click="emit('change-tab', 'route')">推荐路线</button>
    </div>

    <transition name="panel-swap" mode="out-in" appear>
    <section v-if="activeTab === 'spot'" class="panel-body focus-panel-body">
      <article v-if="currentSpot" class="focus-card focus-card-hero">
        <img class="focus-cover" :src="currentSpot.image" :alt="currentSpot.name" />
        <div class="focus-body">
          <div class="tag-list">
            <span class="tag">{{ focusStatus }}</span>
            <span v-for="tag in currentSpot.tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
          <h3 class="focus-title">{{ currentSpot.name }}</h3>
          <p class="focus-copy">{{ currentSpot.summary }}</p>

          <div class="progress-block">
            <div class="progress-meta">
              <span>探索进度</span>
              <strong>{{ discoveredCount }} / {{ totalSpots }}</strong>
            </div>
            <div class="progress-track">
              <span class="progress-bar" :style="{ width: `${progressPercent}%` }"></span>
            </div>
          </div>

          <div class="focus-actions top-gap">
            <button class="btn btn-primary" type="button" @click="emit('move-to-current-spot')">移动到这里</button>
            <button class="btn btn-dark" type="button" @click="emitSpot(currentSpot.id, { pan: true, openInfo: true, moveExplorer: false })">查看详情</button>
            <button v-if="showRouteTab" class="btn btn-dark" type="button" @click="emit('change-tab', 'route')">查看路线</button>
          </div>
        </div>
      </article>
      <div v-else class="empty-state">暂无看点</div>
    </section>

    <section v-else class="panel-body focus-panel-body">
      <div v-if="!routePlan" class="empty-state">暂未生成路线，可以先点击顶部“路线定制”生成一条推荐路线。</div>
      <template v-else>
        <div class="plan-hero">
          <strong class="plan-title">{{ routePlan.title }}</strong>
          <p class="plan-copy">{{ routePlan.description }}</p>
        </div>

        <div class="plan-tags">
          <span v-for="tag in routePlanTags" :key="tag" class="tag">{{ tag }}</span>
        </div>

        <div v-if="routePlanReasons.length" class="section-caption">推荐说明</div>
        <div v-if="routePlanReasons.length" class="reason-list">
          <article v-for="(item, index) in routePlanReasons" :key="index" class="reason-card">
            <strong>推荐说明 {{ index + 1 }}</strong>
            <p>{{ item }}</p>
          </article>
        </div>

        <div class="section-caption">路线停靠点</div>
        <div class="plan-stop-list route-stop-list">
          <article
            v-for="(item, index) in routePlanStops"
            :key="item.id"
            class="plan-stop"
            @click="emit('focus-route-stop', { spotId: item.id, options: { pan: true, openInfo: true, moveExplorer: false } })"
          >
            <strong>{{ index + 1 }}. {{ item.name }}</strong>
            <p>{{ item.kind || '推荐停靠点' }}</p>
          </article>
        </div>

        <div class="focus-actions top-gap">
          <button
            v-if="routePlanStops.length"
            class="btn btn-primary"
            type="button"
            @click="emitSpot(routePlanStops[0].id, { pan: true, openInfo: true, moveExplorer: true })"
          >
            前往路线起点
          </button>
          <button v-if="showRouteTab" class="btn btn-dark" type="button" @click="emit('open-plan')">重新定制</button>
        </div>
      </template>
    </section>
    </transition>
  </aside>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  activeTab: {
    type: String,
    default: 'spot',
  },
  currentSpot: {
    type: Object,
    default: null,
  },
  focusStatus: {
    type: String,
    default: '未记录',
  },
  discoveredCount: {
    type: Number,
    default: 0,
  },
  totalSpots: {
    type: Number,
    default: 0,
  },
  routePlan: {
    type: Object,
    default: null,
  },
  routePlanTags: {
    type: Array,
    default: () => [],
  },
  routePlanReasons: {
    type: Array,
    default: () => [],
  },
  routePlanStops: {
    type: Array,
    default: () => [],
  },
  showRouteTab: {
    type: Boolean,
    default: true,
  },
});

const emit = defineEmits(['change-tab', 'move-to-current-spot', 'open-spot', 'focus-route-stop', 'open-plan', 'toggle-minimize']);

const progressPercent = computed(() => {
  if (!props.totalSpots) {
    return 0;
  }
  return Math.round((props.discoveredCount / props.totalSpots) * 100);
});

function emitSpot(spotId, options) {
  emit('open-spot', { spotId, options });
}
</script>

<style scoped>
.explore-focus {
  transition: background 0.24s ease, border-color 0.24s ease, box-shadow 0.24s ease;
}

.panel-swap-enter-active,
.panel-swap-leave-active {
  transition: opacity 0.24s ease, transform 0.28s cubic-bezier(0.22, 1, 0.36, 1), filter 0.24s ease;
}

.panel-swap-enter-from,
.panel-swap-leave-to {
  opacity: 0;
  transform: translateY(12px);
  filter: blur(8px);
}

.explore-focus.focus-spot-mode {
  color: var(--text-light);
  border: 1px solid rgba(255, 255, 255, 0.74);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(247, 251, 250, 0.86));
  box-shadow: 0 30px 80px rgba(9, 34, 44, 0.18);
}

.explore-focus.focus-route-mode {
  color: var(--text-dark);
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: linear-gradient(180deg, rgba(8, 21, 34, 0.94), rgba(8, 21, 34, 0.86));
  box-shadow: 0 28px 72px rgba(0, 0, 0, 0.34);
}

.focus-spot-mode :deep(.btn-dark) {
  color: var(--text-light);
  background: rgba(23, 60, 73, 0.06);
  border-color: rgba(18, 48, 62, 0.08);
}

.focus-spot-mode :deep(.btn-primary) {
  box-shadow: 0 16px 32px rgba(var(--brand-rgb), 0.18);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.explore-panel-head {
  margin-bottom: 14px;
}

.panel-title {
  margin: 0;
  line-height: 1.15;
  font-size: 22px;
}

.panel-copy,
.focus-copy,
.plan-copy,
.reason-card p,
.plan-stop p {
  margin: 8px 0 0;
  color: var(--muted-dark);
  font-size: 13px;
  line-height: 1.78;
}

.focus-spot-mode .panel-title,
.focus-spot-mode .panel-minimize,
.focus-spot-mode .panel-copy,
.focus-spot-mode .focus-copy,
.focus-spot-mode .plan-copy,
.focus-spot-mode .reason-card p,
.focus-spot-mode .plan-stop p,
.focus-spot-mode .progress-meta span,
.focus-spot-mode .section-caption {
  color: var(--muted-light);
}

.focus-spot-mode .panel-title { color: var(--text-light); }

.panel-head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-minimize {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 50%;
  color: var(--text-dark);
  background: rgba(255, 255, 255, 0.08);
  cursor: pointer;
}

.focus-spot-mode .panel-minimize {
  color: var(--text-light);
  background: rgba(23, 60, 73, 0.06);
  box-shadow: inset 0 0 0 1px rgba(18, 48, 62, 0.06);
}

.panel-tabs {
  display: flex;
  gap: 8px;
  padding: 6px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.focus-spot-mode .panel-tabs {
  background: rgba(23, 60, 73, 0.04);
  border-color: rgba(18, 48, 62, 0.08);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.panel-tab {
  flex: 1;
  min-width: 0;
  min-height: 38px;
  padding: 0 14px;
  border: 0;
  border-radius: 14px;
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease, transform 0.18s ease;
}

.focus-spot-mode .panel-tab {
  color: var(--muted-light);
}

.focus-spot-mode :deep(.tag),
.focus-spot-mode :deep(.chip) {
  color: var(--text-light);
  background: rgba(23, 60, 73, 0.06);
  border-color: rgba(18, 48, 62, 0.08);
}

.panel-tab:hover {
  color: #fff;
  transform: translateY(-1px);
}

.panel-tab.active {
  color: #fff;
  background: linear-gradient(135deg, rgba(var(--brand-soft-rgb), 0.26), rgba(var(--brand-deep-rgb), 0.46));
  box-shadow: inset 0 0 0 1px rgba(var(--brand-soft-rgb), 0.18), 0 10px 22px rgba(var(--brand-deep-rgb), 0.16);
}

.panel-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  flex: 1;
  min-height: 0;
  margin-top: 14px;
}

.focus-panel-body {
  overflow: auto;
  padding-right: 4px;
}

.tag-list,
.plan-tags,
.focus-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.section-caption {
  padding: 0 4px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.56);
}

.reason-list,
.plan-stop-list {
  display: grid;
  gap: 10px;
  overflow: auto;
  padding-right: 4px;
}

.plan-stop,
.reason-card,
.focus-card,
.plan-hero,
.progress-block {
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.03));
  color: var(--text-dark);
}

.focus-spot-mode .focus-card,
.focus-spot-mode .progress-block {
  border-color: rgba(18, 48, 62, 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(247, 251, 250, 0.86));
  color: var(--text-light);
  box-shadow: 0 18px 36px rgba(10, 32, 42, 0.08);
}

.focus-route-mode .plan-hero {
  background: linear-gradient(180deg, rgba(var(--brand-soft-rgb), 0.14), rgba(255, 255, 255, 0.04));
  border-color: rgba(var(--brand-soft-rgb), 0.18);
}

.focus-route-mode .plan-stop,
.focus-route-mode .reason-card,
.focus-route-mode .progress-block {
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.02);
}

.plan-stop,
.reason-card,
.progress-block {
  padding: 14px;
}

.plan-stop {
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.plan-stop:hover {
  transform: translateY(-1px);
  border-color: rgba(var(--brand-soft-rgb), 0.28);
  background: linear-gradient(180deg, rgba(var(--brand-soft-rgb), 0.1), rgba(255, 255, 255, 0.04));
}

.plan-stop strong,
.reason-card strong {
  display: block;
  margin-bottom: 8px;
}

.focus-card-hero {
  position: relative;
  overflow: hidden;
  padding: 0;
}

.focus-cover {
  width: 100%;
  height: 180px;
  object-fit: cover;
}

.focus-card-hero::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(7, 17, 31, 0) 0%, rgba(7, 17, 31, 0.08) 100%);
  pointer-events: none;
}

.focus-body {
  display: grid;
  gap: 12px;
  padding: 16px;
}

.focus-title,
.plan-title {
  display: block;
  margin: 0;
  font-size: 28px;
  line-height: 1.16;
}

.progress-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.progress-meta span {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: rgba(255, 255, 255, 0.58);
}

.focus-spot-mode .progress-track {
  background: rgba(19, 49, 62, 0.08);
}

.progress-meta strong {
  font-size: 14px;
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

.plan-hero {
  display: grid;
  gap: 8px;
  padding: 16px;
}

.top-gap {
  margin-top: 4px;
}

.route-stop-list {
  margin-top: 0;
}

@media (max-width: 1220px) {
  .focus-title,
  .plan-title {
    font-size: 24px;
  }
}

@media (max-width: 760px) {
  .focus-cover {
    height: 150px;
  }
}
</style>
