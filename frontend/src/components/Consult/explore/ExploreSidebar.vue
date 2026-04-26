<template>
  <aside class="explore-sidebar glass-dark" :class="[activeTab === 'search' ? 'is-search-mode' : 'is-drawer-mode']">
    <div class="panel-head explore-panel-head">
      <div class="panel-title-block">
        <span class="panel-eyebrow">{{ activeTab === 'search' ? '探索工作台' : '交互抽屉' }}</span>
        <h2 class="panel-title">{{ panelTitle }}</h2>
        <p class="panel-copy">{{ panelCopy }}</p>
      </div>
      <button v-if="activeTab !== 'search'" class="panel-close" type="button" @click="emit('close-drawer')">×</button>
    </div>

    <div class="panel-tabs" :class="{ compact: activeTab !== 'search' }">
      <button
        v-for="tab in visibleTabs"
        :key="tab.id"
        :class="['panel-tab', { active: activeTab === tab.id }]"
        type="button"
        @click="emit('change-tab', tab.id)"
      >
        <span class="panel-tab-icon">{{ tab.icon }}</span>
        <span class="panel-tab-label">{{ tab.label }}</span>
      </button>
    </div>

    <transition name="panel-swap" mode="out-in" appear>
    <section v-if="activeTab === 'search'" class="panel-body search-panel-body">
      <div class="search-bar">
        <input
          v-model="keywordModel"
          class="search-input"
          type="text"
          placeholder="搜索看点"
          @keydown.enter.prevent="emitSearch()"
        />
        <button class="btn btn-primary" type="button" @click="emitSearch()">搜索</button>
      </div>

      <div class="section-caption">快捷筛选</div>
      <div class="quick-list">
        <button v-for="item in quickSearches" :key="item" class="quick-chip" type="button" @click="emitSearch(item)">
          {{ item }}
        </button>
      </div>

      <div class="result-meta">
        <span class="section-caption">匹配结果</span>
        <span class="result-count">{{ searchResults.length }} 项</span>
      </div>

      <div class="result-list result-list-tall">
        <div v-if="!searchResults.length" class="empty-state">暂无结果，可以换个关键词或切换其他探索入口。</div>
        <article
          v-for="item in searchResults"
          :key="item.id"
          class="result-card"
          @click="emitSpot(item.id, { pan: true, openInfo: true, moveExplorer: true })"
        >
          <div class="result-card-head">
            <strong>{{ item.name }}</strong>
            <span class="chip chip-dark result-chip">看点</span>
          </div>
          <p>{{ item.summary }}</p>

          <div class="tag-list">
            <span v-for="tag in item.tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
        </article>
      </div>
    </section>

    <ExploreMissionTab
      v-else-if="activeTab === 'mission'"
      :enabled="missionEnabled"
      :title="missionTitle"
      :subtitle="missionSubtitle"
      :summary="missionSummary"
      :missions="missions"
      :tracked-mission-id="trackedMissionId"
      :unlocked-rewards="missionUnlockedRewards"
      :empty-state="missionEmptyState"
      @track-mission="emit('track-mission', $event)"
      @open-spot="emitSpot($event, { pan: true, openInfo: true, moveExplorer: true })"
      @reset-missions="emit('reset-missions')"
    />

    <ExploreRewardTab
      v-else-if="activeTab === 'reward'"
      :enabled="rewardEnabled"
      :title="rewardTitle"
      :subtitle="rewardSubtitle"
      :summary="rewardSummary"
      :rewards="rewards"
      :unlocked-rewards="unlockedRewards"
      :locked-rewards="lockedRewards"
      :selected-reward-id="selectedRewardId"
      :empty-state="rewardEmptyState"
      @track-mission="emit('track-mission', $event)"
    />

    <section v-else-if="activeTab === 'collection'" class="panel-body drawer-panel-body">
      <div class="status-grid">
        <article class="metric-card">
          <span class="metric-label">已记录条目</span>
          <strong class="metric-value">{{ collectionEntries.length }}</strong>
        </article>
        <article class="metric-card">
          <span class="metric-label">冷却时间</span>
          <strong class="metric-value">{{ cooldownSeconds }} 秒</strong>
        </article>
      </div>

      <div class="panel-inline-actions">
        <button class="btn btn-dark" type="button" @click="emit('reset-collection')">重置记录</button>
      </div>

      <div class="section-caption">漫游日志</div>

      <div class="collection-list result-list-tall">
        <div v-if="!collectionEntries.length" class="empty-state">暂无记录</div>
        <article
          v-for="entry in collectionEntries"
          :key="entry.title"
          class="collection-card"
          @click="entry.id && emitSpot(entry.id, { pan: true, openInfo: true, moveExplorer: false })"
        >
          <strong>{{ entry.title }}</strong>
          <p>{{ entry.text }}</p>
        </article>
      </div>
    </section>

    <section v-else class="panel-body drawer-panel-body">
      <div class="settings-stack">
        <article class="feature-card">
          <strong>探索方式</strong>
          <p>保留真实地图探索、任务收集、路线推荐与后端智能助手的联动体验。</p>
          <div class="tag-list top-gap">
            <span class="tag">地图自由漫游</span>
            <span class="tag">热点可视化</span>
            <span class="tag">后端搜索 / 路线 / 助手</span>
          </div>
        </article>

        <article class="feature-card">
          <strong>路线管理</strong>
          <div v-if="showRouteTools" class="panel-inline-actions top-gap">
            <button class="btn btn-primary" type="button" @click="emit('open-plan')">路线定制</button>
            <button class="btn btn-dark" type="button" @click="emit('open-route-panel')">查看路线</button>
            <button class="btn btn-dark" type="button" @click="emit('clear-plan')">清除路线</button>
          </div>
          <p v-else class="top-gap">
            当前景区暂未开放路线定制能力，你仍然可以继续自由漫游和查看看点。
          </p>
        </article>

        <article class="feature-card">
          <strong>地图视角</strong>
          <div class="panel-inline-actions top-gap">
            <button class="btn btn-dark" type="button" @click="emit('fit-overview')">查看全景</button>
            <button class="btn btn-dark" type="button" @click="emit('move-to-current-spot')">移动到当前看点</button>
          </div>
        </article>
      </div>
    </section>
    </transition>
  </aside>
</template>

<script setup>
import { computed } from 'vue';
import ExploreMissionTab from './ExploreMissionTab.vue';
import ExploreRewardTab from './ExploreRewardTab.vue';

const props = defineProps({
  activeTab: {
    type: String,
    default: 'search',
  },
  keyword: {
    type: String,
    default: '',
  },
  quickSearches: {
    type: Array,
    default: () => [],
  },
  searchResults: {
    type: Array,
    default: () => [],
  },
  collectionEntries: {
    type: Array,
    default: () => [],
  },
  missionEnabled: {
    type: Boolean,
    default: false,
  },
  missionTitle: {
    type: String,
    default: '探索任务',
  },
  missionSubtitle: {
    type: String,
    default: '',
  },
  missionSummary: {
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
  missionUnlockedRewards: {
    type: Array,
    default: () => [],
  },
  missionEmptyState: {
    type: Object,
    default: () => ({ title: '暂未开放任务', description: '当前景区尚未配置探索任务。' }),
  },
  rewardEnabled: {
    type: Boolean,
    default: false,
  },
  rewardTitle: {
    type: String,
    default: '奖励图鉴',
  },
  rewardSubtitle: {
    type: String,
    default: '',
  },
  rewardSummary: {
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
  cooldownSeconds: {
    type: Number,
    default: 12,
  },
  showCollectionTab: {
    type: Boolean,
    default: true,
  },
  showMissionTab: {
    type: Boolean,
    default: false,
  },
  rewardEmptyState: {
    type: Object,
    default: () => ({ title: '暂未开放奖励图鉴', description: '当前景区尚未配置奖励内容。' }),
  },
  showRewardTab: {
    type: Boolean,
    default: false,
  },
  showRouteTools: {
    type: Boolean,
    default: true,
  },
});

const emit = defineEmits([
  'change-tab',
  'update:keyword',
  'search',
  'open-spot',
  'reset-collection',
  'open-plan',
  'track-mission',
  'reset-missions',
  'open-route-panel',
  'clear-plan',
  'fit-overview',
  'move-to-current-spot',
  'close-drawer',
]);

const keywordModel = computed({
  get: () => props.keyword,
  set: (value) => emit('update:keyword', value),
});

const visibleTabs = computed(() => {
  const list = [
    { id: 'search', label: '看点搜索', icon: '🔍' },
    ...(props.showMissionTab ? [{ id: 'mission', label: '探索任务', icon: '🎯' }] : []),
    ...(props.showRewardTab ? [{ id: 'reward', label: '奖励图鉴', icon: '🏆' }] : []),
    ...(props.showCollectionTab ? [{ id: 'collection', label: '图鉴收集', icon: '📖' }] : []),
    { id: 'settings', label: '更多设置', icon: '⚙️' },
  ];
  return list;
});

const panelTitle = computed(() => {
  const mapping = {
    search: '探索工作台',
    mission: props.missionTitle,
    reward: props.rewardTitle,
    collection: '图鉴收集',
    settings: '更多设置',
  };
  return mapping[props.activeTab] || '探索工作台';
});

const panelCopy = computed(() => {
  const mapping = {
    search: '用搜索、标签和结果列表快速锁定你要看的景区看点。',
    mission: props.missionSubtitle || '查看当前任务、步骤推进与奖励解锁状态。',
    reward: props.rewardSubtitle || '查看当前景区可收集奖励与未解锁奖励来源。',
    collection: '记录你的漫游轨迹和已发现条目，方便回看探索过程。',
    settings: '在这里管理路线与地图视角，切换当前探索方式。',
  };
  return mapping[props.activeTab] || '';
});

function emitSearch(value = props.keyword) {
  emit('search', value);
}

function emitSpot(spotId, options) {
  emit('open-spot', { spotId, options });
}
</script>

<style scoped>
.explore-sidebar {
  gap: 14px;
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

.explore-sidebar.is-search-mode {
  color: var(--text-light);
  border: 1px solid rgba(255, 255, 255, 0.74);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(247, 251, 250, 0.86));
  box-shadow: 0 30px 80px rgba(9, 34, 44, 0.18);
  backdrop-filter: blur(22px);
}

.explore-sidebar.is-drawer-mode {
  color: var(--text-dark);
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: linear-gradient(180deg, rgba(8, 21, 34, 0.94), rgba(8, 21, 34, 0.86));
  box-shadow: 0 28px 72px rgba(0, 0, 0, 0.34);
}

.explore-sidebar.is-search-mode :deep(.btn-dark) {
  color: var(--text-light);
  background: rgba(23, 60, 73, 0.06);
  border-color: rgba(18, 48, 62, 0.08);
}

.explore-sidebar.is-search-mode :deep(.btn-primary) {
  box-shadow: 0 16px 32px rgba(var(--brand-rgb), 0.18);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.panel-title-block {
  display: grid;
  gap: 8px;
}

.panel-eyebrow {
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(var(--brand-soft-rgb), 0.92);
}

.explore-sidebar.is-search-mode .panel-eyebrow {
  color: rgba(var(--brand-rgb), 0.72);
}

.explore-sidebar.is-search-mode .panel-title,
.explore-sidebar.is-search-mode .panel-close {
  color: var(--text-light);
}

.panel-title {
  margin: 0;
  line-height: 1.15;
  font-size: 24px;
}

.panel-copy,
.result-card p,
.collection-card p,
.feature-card p {
  margin: 0;
  color: var(--muted-dark);
  font-size: 13px;
  line-height: 1.78;
}

.explore-sidebar.is-search-mode .panel-copy,
.explore-sidebar.is-search-mode .result-card p,
.explore-sidebar.is-search-mode .collection-card p,
.explore-sidebar.is-search-mode .feature-card p,
.explore-sidebar.is-search-mode .result-count,
.explore-sidebar.is-search-mode .metric-label,
.explore-sidebar.is-search-mode .section-caption {
  color: var(--muted-light);
}

.explore-sidebar.is-search-mode .metric-value {
  color: var(--text-light);
}

.panel-close {
  width: 38px;
  height: 38px;
  border: 0;
  border-radius: 50%;
  color: var(--text-dark);
  background: rgba(255, 255, 255, 0.08);
  cursor: pointer;
}

.explore-sidebar.is-search-mode .panel-close {
  background: rgba(23, 60, 73, 0.06);
  box-shadow: inset 0 0 0 1px rgba(18, 48, 62, 0.06);
}

.panel-tabs {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
  padding: 8px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.explore-sidebar.is-search-mode .panel-tabs {
  background: rgba(23, 60, 73, 0.04);
  border-color: rgba(18, 48, 62, 0.08);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.panel-tabs.compact {
  grid-template-columns: repeat(auto-fit, minmax(96px, 1fr));
}

.panel-tab {
  display: grid;
  justify-items: center;
  gap: 6px;
  min-width: 0;
  min-height: 70px;
  padding: 12px 10px;
  border: 0;
  border-radius: 18px;
  background: transparent;
  color: rgba(255, 255, 255, 0.72);
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease, transform 0.18s ease, box-shadow 0.18s ease;
}

.explore-sidebar.is-search-mode .panel-tab {
  color: var(--muted-light);
}

.explore-sidebar.is-search-mode .panel-tab.active {
  color: #fff;
  box-shadow: inset 0 0 0 1px rgba(var(--brand-soft-rgb), 0.14), 0 14px 28px rgba(var(--brand-rgb), 0.16);
}

.explore-sidebar.is-search-mode :deep(.tag),
.explore-sidebar.is-search-mode :deep(.quick-chip),
.explore-sidebar.is-search-mode :deep(.chip) {
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
  background: linear-gradient(135deg, rgba(var(--brand-soft-rgb), 0.22), rgba(var(--brand-deep-rgb), 0.44));
  box-shadow: inset 0 0 0 1px rgba(var(--brand-soft-rgb), 0.18), 0 12px 24px rgba(var(--brand-deep-rgb), 0.16);
}

.panel-tab-icon {
  font-size: 18px;
}

.panel-tab-label {
  font-size: 13px;
  line-height: 1.2;
  text-align: center;
}

.panel-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  flex: 1;
  min-height: 0;
}

.search-panel-body,
.drawer-panel-body {
  overflow: auto;
  padding-right: 4px;
}

.search-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.search-input {
  min-height: 48px;
  padding: 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--text-light);
  outline: none;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.4), 0 12px 28px rgba(12, 36, 46, 0.08);
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.search-input:focus {
  border-color: rgba(var(--brand-soft-rgb), 0.34);
  box-shadow: 0 0 0 3px rgba(var(--brand-soft-rgb), 0.12);
}

.search-input::placeholder {
  color: rgba(96, 119, 135, 0.9);
}

.section-caption {
  padding: 0 4px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.56);
}

.result-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.result-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.result-list,
.collection-list {
  display: grid;
  gap: 10px;
}

.result-list-tall {
  flex: 1;
  min-height: 0;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.metric-card,
.feature-card,
.result-card,
.collection-card {
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.03));
  color: var(--text-dark);
}

.explore-sidebar.is-search-mode .metric-card,
.explore-sidebar.is-search-mode .feature-card,
.explore-sidebar.is-search-mode .result-card,
.explore-sidebar.is-search-mode .collection-card {
  border-color: rgba(18, 48, 62, 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(248, 251, 251, 0.82));
  color: var(--text-light);
  box-shadow: 0 18px 36px rgba(10, 32, 42, 0.08);
}

.metric-card {
  display: grid;
  gap: 8px;
  padding: 16px;
}

.explore-sidebar.is-search-mode .result-card:hover,
.explore-sidebar.is-search-mode .collection-card:hover {
  border-color: rgba(var(--brand-rgb), 0.2);
  background: linear-gradient(180deg, rgba(var(--brand-soft-rgb), 0.14), rgba(255, 255, 255, 0.92));
  box-shadow: 0 20px 36px rgba(var(--brand-rgb), 0.14);
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

.feature-card {
  display: grid;
  gap: 8px;
  padding: 16px;
}

.result-card,
.collection-card {
  display: grid;
  gap: 10px;
  padding: 14px;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.result-card:hover,
.collection-card:hover {
  transform: translateY(-1px);
  border-color: rgba(var(--brand-soft-rgb), 0.28);
  background: linear-gradient(180deg, rgba(var(--brand-soft-rgb), 0.1), rgba(255, 255, 255, 0.04));
}

.result-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.result-card strong,
.collection-card strong,
.feature-card strong {
  display: block;
}

.result-chip {
  min-height: 28px;
}

.quick-list,
.tag-list,
.panel-inline-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.settings-stack {
  display: grid;
  gap: 14px;
}

.top-gap {
  margin-top: 4px;
}

.explore-sidebar.is-search-mode .status-grid {
  grid-template-columns: 1fr;
}

.explore-sidebar.is-search-mode .panel-inline-actions {
  justify-content: flex-start;
}

.explore-sidebar.is-search-mode :deep(.empty-state) {
  border-color: rgba(18, 48, 62, 0.1);
  background: rgba(255, 255, 255, 0.74);
  color: var(--muted-light);
}

@media (max-width: 920px) {
  .panel-tabs {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .search-bar {
    grid-template-columns: 1fr;
  }

  .panel-title {
    font-size: 20px;
  }

  .status-grid {
    grid-template-columns: 1fr;
  }
}
</style>
