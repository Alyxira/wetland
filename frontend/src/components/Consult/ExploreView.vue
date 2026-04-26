<template>
  <section v-if="loading" class="state-card glass-dark">
    <strong>正在加载探索页</strong>
    <div>正在连接地图、路线、图鉴与向导助手。</div>
  </section>

  <section v-else-if="error" class="state-card glass-dark">
    <strong>探索页加载失败</strong>
    <div>{{ error }}</div>
  </section>

  <section v-else class="explore-shell">
    <div class="map-canvas-layer">
      <GuideMapStage
        ref="guideMapRef"
        :map-config="mapConfig"
        :spots="spots"
        :last-position="lastPosition"
        :route-plan="routePlan"
        :current-spot-id="currentSpotId"
        :discovered-spot-ids="discoveredSpotIds"
        :pending-event-spot-ids="pendingEventSpotIds"
        :trigger-cooldown="12000"
        @current-spot-change="handleCurrentSpotChange"
        @spot-discover="handleSpotDiscovered"
        @position-save="handlePositionSave"
        @spot-panel-open="handleSpotPanelOpen"
        @spot-navigate="handleSpotNavigate"
        @map-error="handleMapError"
      />
      <div class="map-gradient-overlay" aria-hidden="true"></div>

      <div class="map-controls">
        <RouterLink class="back-home" :to="homePath">返回景点详情</RouterLink>
        <button class="gps-toggle" type="button" @click="toggleSimulation">
          {{ simulationMode ? '模拟模式' : '定位模式' }}
        </button>
      </div>

      <Transition name="nudge-float" appear>
        <div v-if="nudgeEvent" class="event-nudge" role="status" aria-live="polite">
          <div class="event-nudge-icon">✦</div>
          <div class="event-nudge-copy">
            <p>已靠近 {{ eventSpotName(nudgeEvent?.spotId) }}</p>
            <strong>是否要探索 {{ nudgeEvent.title }}？</strong>
          </div>
          <div class="event-nudge-actions">
            <button type="button" class="event-nudge-open" @click="openNudgeEvent">
              立即探索
            </button>
            <button type="button" class="event-nudge-close" @click="dismissNudgeEvent">
              稍后
            </button>
          </div>
        </div>
      </Transition>
    </div>

    <Transition name="stop-nav-float" appear>
      <button
        v-if="hasActiveNavigation"
        class="floating-stop-nav"
        type="button"
        @click="stopNavigation()"
      >
        <span class="floating-stop-nav-icon">✕</span>
        <span>结束导航</span>
      </button>
    </Transition>

    <button
      v-if="isExplorationPanelCollapsed"
      type="button"
      class="explore-panel-reopen"
      @click="setExplorationPanelCollapsed(false)"
      aria-label="展开探索面板"
      title="展开探索面板"
    >
      探索发现
    </button>

    <aside v-if="!isExplorationPanelCollapsed" class="exploration-panel">
      <header class="explore-head">
        <h2>探索发现</h2>
        <button type="button" class="explore-head-collapse" @click="setExplorationPanelCollapsed(true)" aria-label="收起探索面板" title="收起探索面板">›</button>
      </header>

      <div class="search-bar">
        <input
          v-model="searchKeyword"
          placeholder="搜索景点..."
          @keydown.enter.prevent="runSearch"
        />
      </div>

      <div class="journey-tabs">
        <button :class="{ active: activePanel === 'recommend' }" type="button" @click="activePanel = 'recommend'">推荐</button>
        <button :class="{ active: activePanel === 'stamps' }" type="button" @click="activePanel = 'stamps'">图鉴</button>
        <button :class="{ active: activePanel === 'routes' }" type="button" @click="activePanel = 'routes'">路线</button>
      </div>

      <div class="journey-progress-meta">
        <span>探索进度</span>
        <span>{{ visitedSpotsCount }} / {{ recommendationCards.length }}</span>
      </div>
      <div class="journey-progress-bar">
        <span :style="{ width: `${journeyPercent}%` }"></span>
      </div>

      <div class="journey-content">
        <Transition name="panel-swap" mode="out-in">
          <div v-if="activePanel === 'recommend'" key="panel-recommend" class="panel-section">
            <article
              v-for="(item, index) in visibleRecommendationCards"
              :key="item.id"
              class="discover-card reveal-item"
              :style="{ '--stagger-delay': `${Math.min(index, 11) * 34}ms` }"
              @click="openSpotPreview(item)"
            >
              <div class="discover-head">
                <div class="discover-title">
                  <span class="pin-dot"></span>
                  <strong>{{ item.name }}</strong>
                </div>
                <span class="spot-kind">{{ item.category }}</span>
              </div>
              <p>{{ item.description }}</p>
              <div class="discover-actions">
                <button type="button" class="spot-action ghost" @click.stop="openSpotPreview(item)">查看</button>
                <button type="button" class="spot-action" @click.stop="navigateToSpot(item)">导航</button>
              </div>
            </article>
            <article v-if="!visibleRecommendationCards.length" class="empty-card">
              <p>未找到相关景点</p>
            </article>
          </div>

          <div v-else-if="activePanel === 'stamps'" key="panel-stamps" class="panel-section">
            <article
              v-for="(stamp, index) in stamps"
              :key="stamp.id"
              class="stamp-card reveal-item"
              :style="{ '--stagger-delay': `${Math.min(index, 11) * 30}ms` }"
            >
              <img :src="stamp.imageUrl || fallbackImage" :alt="stamp.name" loading="lazy" decoding="async" />
              <div class="stamp-body">
                <strong>{{ stamp.name }}</strong>
                <small>{{ stamp.spotName }} · {{ stamp.rarity }}</small>
                <button
                  type="button"
                  :disabled="stamp.isCollected || collectingStampId === stamp.id"
                  @click="collectStamp(stamp)"
                >
                  {{ stamp.isCollected ? '已收集' : (collectingStampId === stamp.id ? '采集中...' : '收集') }}
                </button>
              </div>
            </article>
            <article v-if="!stamps.length" class="empty-card">
              <p>暂时还没有图鉴数据</p>
            </article>
          </div>

          <div v-else key="panel-routes" class="panel-section">
            <article v-if="routePlan && routePlanSource === 'assistant'" class="route-card assistant-route-card">
              <h3>{{ routePlan.title || '助手推荐路线' }}</h3>
              <p>{{ routePlan.description }}</p>
              <small>{{ routePlan.stops?.length || 0 }} 站 · AI推荐</small>
              <div class="route-actions">
                <button type="button" @click="reapplyAssistantRoute">应用助手路线</button>
                <button type="button" class="ghost" @click="navigateToRouteStart(routePlan)">导航首站</button>
                <button type="button" class="ghost" @click="stopNavigation()">停止导航</button>
              </div>
            </article>
            <article v-if="!routes.length && !routePlan" class="route-card">
              <h3>当前无预设路线</h3>
              <p>请先选择推荐点位或由助手生成路线。</p>
            </article>
            <article
              v-for="(routeItem, index) in routes"
              :key="routeItem.id"
              class="route-card reveal-item"
              :style="{ '--stagger-delay': `${Math.min(index, 11) * 30}ms` }"
            >
              <h3>{{ routeItem.name }}</h3>
              <p>{{ routeItem.description }}</p>
              <small>{{ routeItem.duration }} · {{ routeItem.difficulty }} · {{ routeItem.distance }}</small>
              <div class="route-actions">
                <button type="button" @click="applyRoute(routeItem)">应用路线</button>
                <button type="button" class="ghost" @click="navigateToRouteStart(routeItem)">导航首站</button>
              </div>
            </article>
          </div>
        </Transition>
      </div>
    </aside>

    <Transition name="preview-fade" appear>
      <div v-if="activeSpotPreview" class="spot-preview-mask" @click.self="activeSpotPreview = null">
        <article class="spot-preview-card">
          <div class="spot-preview-cover">
            <img :src="activeSpotPreview.imageUrl" :alt="activeSpotPreview.name" decoding="async" />
          </div>
          <div class="spot-preview-body">
            <div class="spot-badges">
              <span class="badge">{{ activeSpotPreview.category }}</span>
              <span class="badge visited">{{ activeSpotPreview.visited ? '已打卡' : '待打卡' }}</span>
            </div>
            <h3>{{ activeSpotPreview.name }}</h3>
            <p>{{ activeSpotPreview.description }}</p>
            <div class="spot-preview-actions">
              <button type="button" class="spot-action" @click="navigateFromPreview">导航到这里</button>
              <button type="button" class="spot-action ghost" @click="activeSpotPreview = null">关闭</button>
            </div>
          </div>
        </article>
      </div>
    </Transition>

    <Transition name="event-sheet" appear>
      <div v-if="activeEvent" class="event-mask" @click.self="activeEvent = null">
        <article class="event-card">
          <button type="button" class="event-close" aria-label="关闭事件详情" @click="activeEvent = null">
            ×
          </button>

          <div class="event-head">
            <div class="event-head-icon">✦</div>
            <div class="event-head-copy">
              <span>{{ activeEventSpotName }} · 自然发现</span>
              <h3>{{ activeEvent.title }}</h3>
            </div>
          </div>

          <p>{{ activeEvent.content }}</p>

          <article v-if="activeEventStamp" class="event-stamp-row">
            <img :src="activeEventStamp.imageUrl || fallbackImage" :alt="activeEventStamp.name" loading="lazy" decoding="async" />
            <div>
              <small>景区图鉴</small>
              <strong>{{ activeEventStamp.name }}</strong>
            </div>
            <button
              type="button"
              class="event-stamp-action"
              :disabled="activeEventStamp.isCollected || collectingStampId === activeEventStamp.id"
              @click="collectStamp(activeEventStamp)"
            >
              {{ activeEventStamp.isCollected ? '已打卡' : (collectingStampId === activeEventStamp.id ? '打卡中...' : '打卡') }}
            </button>
          </article>

          <div v-if="activeEvent.options?.length" class="event-options">
            <button
              v-for="option in activeEvent.options"
              :key="option"
              type="button"
              @click="interactEvent(option)"
            >
              {{ option }}
            </button>
          </div>
          <div class="event-actions">
            <button type="button" @click="interactEvent('')">完成</button>
            <button type="button" class="ghost" @click="activeEvent = null">稍后</button>
          </div>
        </article>
      </div>
    </Transition>

    <TransitionGroup name="toast-stack" tag="div" class="toast-host">
      <article v-for="toast in toasts" :key="toast.id" class="toast-item">{{ toast.text }}</article>
    </TransitionGroup>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import GuideMapStage from './explore/GuideMapStage.vue';
import { API, request, useSiteStore } from '../../stores/site';
import { buildScenicPagePath, buildScenicStorageKey, normalizeScenicId } from '../../utils/scenic';

const store = useSiteStore();
const route = useRoute();

const fallbackImage = 'https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1000&q=80';

const loading = ref(true);
const error = ref('');
const guideMapRef = ref(null);

const activePanel = ref('recommend');
const searchKeyword = ref('');
const searchResults = ref([]);
const isExplorationPanelCollapsed = ref(false);

const tasks = ref([]);
const routes = ref([]);
const stamps = ref([]);
const events = ref([]);
const spotCatalog = ref([]);
const routePlan = ref(null);
const routePlanSource = ref('');
const lastPosition = ref(null);
const currentSpotId = ref('');
const discoveredSpotIds = ref([]);
const nudgeEvent = ref(null);
const activeEvent = ref(null);
const activeSpotPreview = ref(null);
const toasts = ref([]);
const surfacedEventIds = new Set();

const simulationMode = ref(false);
const geoWatchId = ref(null);
const collectingStampId = ref('');
const AUTO_ANCHOR_ENTRY_SET = new Set(['home', 'scenic-home', 'scenic-index']);
const ASSISTANT_ROUTE_EVENT = 'scenic:assistant-route-plan';
const ASSISTANT_ROUTE_STORAGE_SCOPE = 'assistant-route-plan';
const rawSpotCoordSource = String(import.meta.env.VITE_SPOT_COORD_SOURCE || 'wgs84').trim().toLowerCase();
const SPOT_COORD_SOURCE = (
  rawSpotCoordSource === 'gcj02' || rawSpotCoordSource === 'amap'
    ? rawSpotCoordSource
    : 'wgs84'
);
const GCJ_EARTH_A = 6378245.0;
const GCJ_EE = 0.00669342162296594323;

const CATEGORY_LABEL_MAP = {
  story: '科普故事',
  animal: '动物观察',
  plant: '植物生态',
  scenic: '景点',
  spot: '看点',
};

function toCategoryLabel(value) {
  const raw = String(value || '').trim();
  if (!raw) return '景点';
  const mapped = CATEGORY_LABEL_MAP[raw.toLowerCase()];
  return mapped || raw;
}

function toTagLabels(tags) {
  if (!Array.isArray(tags)) return [];
  return tags.map((tag) => toCategoryLabel(tag));
}

function outOfChina(lng, lat) {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
}

function transformLat(lng, lat) {
  let ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
  ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0;
  ret += (20.0 * Math.sin(lat * Math.PI) + 40.0 * Math.sin(lat / 3.0 * Math.PI)) * 2.0 / 3.0;
  ret += (160.0 * Math.sin(lat / 12.0 * Math.PI) + 320 * Math.sin(lat * Math.PI / 30.0)) * 2.0 / 3.0;
  return ret;
}

function transformLng(lng, lat) {
  let ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
  ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0;
  ret += (20.0 * Math.sin(lng * Math.PI) + 40.0 * Math.sin(lng / 3.0 * Math.PI)) * 2.0 / 3.0;
  ret += (150.0 * Math.sin(lng / 12.0 * Math.PI) + 300.0 * Math.sin(lng / 30.0 * Math.PI)) * 2.0 / 3.0;
  return ret;
}

function wgs84ToGcj02(lng, lat) {
  if (outOfChina(lng, lat)) {
    return [lng, lat];
  }
  let dLat = transformLat(lng - 105.0, lat - 35.0);
  let dLng = transformLng(lng - 105.0, lat - 35.0);
  const radLat = lat / 180.0 * Math.PI;
  let magic = Math.sin(radLat);
  magic = 1 - GCJ_EE * magic * magic;
  const sqrtMagic = Math.sqrt(magic);
  dLat = (dLat * 180.0) / ((GCJ_EARTH_A * (1 - GCJ_EE)) / (magic * sqrtMagic) * Math.PI);
  dLng = (dLng * 180.0) / (GCJ_EARTH_A / sqrtMagic * Math.cos(radLat) * Math.PI);
  return [lng + dLng, lat + dLat];
}

function toMapCoordinateBySource(lng, lat, source = SPOT_COORD_SOURCE) {
  const normalizedLng = Number(lng);
  const normalizedLat = Number(lat);
  if (!Number.isFinite(normalizedLng) || !Number.isFinite(normalizedLat)) {
    return [Number(lng), Number(lat)];
  }
  if (source === 'gcj02' || source === 'amap') {
    return [normalizedLng, normalizedLat];
  }
  return wgs84ToGcj02(normalizedLng, normalizedLat);
}

function normalizeMapSpotCoordinate(spot) {
  if (!spot || typeof spot !== 'object') return spot;
  const lng = Number(spot.lng);
  const lat = Number(spot.lat);
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) {
    return { ...spot };
  }
  const [mapLng, mapLat] = toMapCoordinateBySource(lng, lat);
  return {
    ...spot,
    lng: mapLng,
    lat: mapLat,
  };
}

const scenicId = computed(() => normalizeScenicId(route.params.scenicId));
const homePath = computed(() => buildScenicPagePath(scenicId.value, 'home'));
const exploreData = computed(() => store.exploreById(scenicId.value) || {
  title: '景区探索',
  subtitle: '',
  map: { lng: 103.9272, lat: 33.2306, zoom: 13.8 },
  spots: [],
});
const mapConfig = computed(() => {
  const base = exploreData.value.map || {};
  const rawLng = Number.isFinite(Number(base.lng)) ? Number(base.lng) : 103.9272;
  const rawLat = Number.isFinite(Number(base.lat)) ? Number(base.lat) : 33.2306;
  const [lng, lat] = toMapCoordinateBySource(rawLng, rawLat);
  return {
    ...base,
    lng,
    lat,
    zoom: Number.isFinite(Number(base.zoom)) ? Number(base.zoom) : 13.8,
  };
});
const simulationAnchorPoint = computed(() => [mapConfig.value.lng, mapConfig.value.lat]);
const spots = computed(() => {
  const source = Array.isArray(exploreData.value.spots) ? exploreData.value.spots : [];
  return source.map((item) => normalizeMapSpotCoordinate(item));
});

const recommendationCards = computed(() => {
  const source = spotCatalog.value.length
    ? spotCatalog.value
    : spots.value.map((item, index) => ({
      id: item.id,
      name: item.name,
      description: item.summary,
      category: item.kind || '景点',
      imageUrl: item.image || fallbackImage,
      isVisited: false,
      tags: item.tags || [],
    }));

  const discoveredSet = new Set(discoveredSpotIds.value);
  const collectedSet = new Set(stamps.value.filter((item) => item.isCollected).map((item) => item.spotId));

  return source.map((item, index) => {
    const categoryRaw = item.category || item.kind || '景点';
    const tagsRaw = Array.isArray(item.tags) ? item.tags : [];
    return {
      id: item.id,
      name: item.name || `看点 ${index + 1}`,
      description: item.description || item.summary || '值得停留打卡的精选看点。',
      category: toCategoryLabel(categoryRaw),
      categoryRaw,
      imageUrl: item.imageUrl || item.image || fallbackImage,
      visited: Boolean(item.isVisited) || discoveredSet.has(item.id) || collectedSet.has(item.id),
      tags: toTagLabels(tagsRaw),
      tagsRaw,
    };
  });
});

const visibleRecommendationCards = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  if (searchResults.value.length) {
    const bySearch = new Set(searchResults.value.map((item) => item.id));
    return recommendationCards.value.filter((item) => bySearch.has(item.id));
  }
  if (!keyword) return recommendationCards.value;
  return recommendationCards.value.filter((item) => {
    const text = [item.name, item.description, item.category, item.categoryRaw, ...item.tags, ...(item.tagsRaw || [])].join(' ').toLowerCase();
    return text.includes(keyword);
  });
});

const visitedSpotsCount = computed(() => recommendationCards.value.filter((item) => item.visited).length);
const pendingEventSpotIds = computed(() => events.value
  .filter((item) => !item?.isCompleted && item?.spotId)
  .map((item) => item.spotId));
const activeEventStamp = computed(() => {
  if (!activeEvent.value?.spotId) return null;
  return stamps.value.find((item) => item.spotId === activeEvent.value.spotId) || null;
});
const activeEventSpotName = computed(() => eventSpotName(activeEvent.value?.spotId));
const hasActiveNavigation = computed(() => Array.isArray(routePlan.value?.stops) && routePlan.value.stops.length > 0);
const journeyPercent = computed(() => {
  if (!recommendationCards.value.length) return 0;
  return Math.round((visitedSpotsCount.value / recommendationCards.value.length) * 100);
});

function pushToast(text, duration = 2600) {
  const id = `${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;
  toasts.value.unshift({ id, text });
  window.setTimeout(() => {
    toasts.value = toasts.value.filter((item) => item.id !== id);
  }, duration);
}

function safeParse(raw) {
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch (error) {
    return null;
  }
}

function getAssistantRouteStorageKey() {
  return buildScenicStorageKey(scenicId.value, ASSISTANT_ROUTE_STORAGE_SCOPE, 'v1');
}

function clearPendingAssistantRoutePlan() {
  localStorage.removeItem(getAssistantRouteStorageKey());
}

function normalizeRouteStops(stops) {
  if (!Array.isArray(stops)) return [];
  return stops
    .map((item) => {
      const id = String(item?.id || '').trim();
      const name = String(item?.name || '站点');
      const kind = toCategoryLabel(item?.kind || item?.category || 'spot');
      const coordSource = String(item?.coordSource || item?.coordinateSystem || '').trim().toLowerCase();
      const shouldTreatAsMapCoord = (
        id === 'current-position'
        || coordSource === 'map'
        || coordSource === 'gcj02'
        || coordSource === 'amap'
      );
      const knownSpot = spots.value.find((spot) => String(spot?.id || '').trim() === id);

      let mapLng = NaN;
      let mapLat = NaN;
      if (knownSpot && Number.isFinite(Number(knownSpot.lng)) && Number.isFinite(Number(knownSpot.lat))) {
        mapLng = Number(knownSpot.lng);
        mapLat = Number(knownSpot.lat);
      } else {
        const lng = Number(item?.lng);
        const lat = Number(item?.lat);
        if (!Number.isFinite(lng) || !Number.isFinite(lat)) {
          return {
            id: '',
            name: '',
            kind: '',
            lng: NaN,
            lat: NaN,
          };
        }
        if (shouldTreatAsMapCoord) {
          mapLng = lng;
          mapLat = lat;
        } else {
          [mapLng, mapLat] = toMapCoordinateBySource(lng, lat);
        }
      }

      if (!Number.isFinite(mapLng) || !Number.isFinite(mapLat)) {
        return {
          id: '',
          name: '',
          kind: '',
          lng: NaN,
          lat: NaN,
        };
      }
      return {
        id,
        name,
        kind,
        lng: mapLng,
        lat: mapLat,
      };
    })
    .filter((item) => item.id && Number.isFinite(item.lng) && Number.isFinite(item.lat));
}

function normalizeRoutePlanPayload(rawPlan) {
  if (!rawPlan) return null;
  const stops = normalizeRouteStops(rawPlan.stops);
  if (!stops.length) return null;

  const rawTitle = String(rawPlan.title || '').trim();
  const fallbackTitle = rawTitle || '推荐路线';
  const shouldUsePreferenceTitle = [
    '轻松漫游路线',
    '亲子慢游路线',
    '观察讲解路线',
    '高光拍照路线',
  ].some((item) => fallbackTitle.includes(item));
  const title = shouldUsePreferenceTitle ? '根据喜好推荐的路线' : fallbackTitle;

  const rawDescription = String(rawPlan.description || '').trim();
  const normalizedTags = Array.isArray(rawPlan.tags) ? rawPlan.tags.filter((item) => typeof item === 'string') : [];
  const preferenceLabel = normalizedTags.slice(0, 3).join(' · ');
  const description = rawDescription.includes('慢慢走') || rawDescription.includes('轻松')
    ? `已根据你的偏好生成路线${preferenceLabel ? `（${preferenceLabel}）` : ''}，可一键应用并开始导航。`
    : (rawDescription || `已根据你的偏好生成路线${preferenceLabel ? `（${preferenceLabel}）` : ''}，可一键应用并开始导航。`);

  return {
    title,
    description,
    reasons: Array.isArray(rawPlan.reasons) ? rawPlan.reasons.filter((item) => typeof item === 'string') : [],
    tags: normalizedTags,
    stops,
    displayMode: String(rawPlan.displayMode || '').trim().toLowerCase() === 'full' ? 'full' : 'segment',
    arrivalRadius: Number.isFinite(Number(rawPlan.arrivalRadius)) ? Number(rawPlan.arrivalRadius) : 86,
    showStopCircles: rawPlan.showStopCircles !== false,
    followExplorer: rawPlan.followExplorer === true,
  };
}

function applyResolvedRoutePlan(rawPlan, toastText = '', options = {}) {
  const normalized = normalizeRoutePlanPayload(rawPlan);
  if (!normalized) return false;
  routePlan.value = normalized;
  routePlanSource.value = options.fromAssistant ? 'assistant' : 'preset';
  if (options.fromAssistant) {
    upsertAssistantRoute(normalized);
  }
  if (options.switchPanel !== false) {
    activePanel.value = 'routes';
  }
  if (toastText) {
    pushToast(toastText);
  }
  return true;
}

function upsertAssistantRoute(plan) {
  if (!plan?.stops?.length) return;
  const assistantId = `assistant-${scenicId.value}`;
  const assistantRoute = {
    id: assistantId,
    name: plan.title || '助手推荐路线',
    description: plan.description || '根据你的对话偏好生成',
    duration: plan.tags?.[0] || `${plan.stops.length} 站`,
    difficulty: plan.tags?.[1] || 'AI推荐',
    distance: plan.tags?.[2] || '',
    spots: plan.stops.map((item) => ({
      id: item.id,
      name: item.name,
      kind: item.kind,
      location: {
        lng: Number(item.lng),
        lat: Number(item.lat),
      },
    })),
  };
  const remains = routes.value.filter((item) => item.id !== assistantId);
  routes.value = [assistantRoute, ...remains];
}

function readPendingAssistantRoutePlan() {
  const stored = safeParse(localStorage.getItem(getAssistantRouteStorageKey()));
  if (!stored?.plan) return null;
  if (stored.scenicId && normalizeScenicId(stored.scenicId) !== scenicId.value) return null;
  return stored.plan;
}

function applyPendingAssistantRoutePlan(toastText = '已应用助手推荐路线') {
  const pendingPlan = readPendingAssistantRoutePlan();
  if (!pendingPlan) return false;
  const applied = applyResolvedRoutePlan(pendingPlan, toastText, { fromAssistant: true });
  if (applied) {
    clearPendingAssistantRoutePlan();
  }
  return applied;
}

function handleAssistantRoutePlanEvent(event) {
  const payload = event?.detail || {};
  if (normalizeScenicId(payload.scenicId) !== scenicId.value) return;
  const applied = applyResolvedRoutePlan(payload.plan, '已应用助手推荐路线', { fromAssistant: true });
  if (applied) {
    clearPendingAssistantRoutePlan();
  }
}

async function loadGameData() {
  const [taskData, stampData, eventData, spotData] = await Promise.all([
    request(API.tasks(scenicId.value)).catch(() => []),
    request(API.stamps(scenicId.value)).catch(() => []),
    request(API.events(scenicId.value)).catch(() => []),
    request(API.spots(scenicId.value)).catch(() => []),
  ]);
  tasks.value = Array.isArray(taskData) ? taskData : [];
  stamps.value = Array.isArray(stampData) ? stampData : [];
  routes.value = [];
  events.value = Array.isArray(eventData) ? eventData : [];
  spotCatalog.value = Array.isArray(spotData)
    ? spotData.map((item) => {
      const next = { ...item };
      const rawLng = Number(item?.location?.lng ?? item?.lng);
      const rawLat = Number(item?.location?.lat ?? item?.lat);
      if (Number.isFinite(rawLng) && Number.isFinite(rawLat)) {
        const [mapLng, mapLat] = toMapCoordinateBySource(rawLng, rawLat);
        next.lng = mapLng;
        next.lat = mapLat;
        next.location = {
          ...(item?.location || {}),
          lng: mapLng,
          lat: mapLat,
        };
      }
      return next;
    })
    : [];
}

async function initPage() {
  loading.value = true;
  error.value = '';
  discoveredSpotIds.value = [];
  routePlan.value = null;
  routePlanSource.value = '';
  surfacedEventIds.clear();

  try {
    await Promise.all([store.ensureScenic(scenicId.value), store.ensureExplore(scenicId.value)]);
    await loadGameData();
  } catch (err) {
    error.value = err?.message || 'Explore page load failed.';
    loading.value = false;
    return;
  }

  loading.value = false;
  await nextTick();

  try {
    const stage = guideMapRef.value;
    if (!stage?.initMap) {
      throw new Error('Map stage not ready.');
    }

    stage.destroyMap?.();
    await stage.initMap();
    applyPendingAssistantRoutePlan();

    const hasSpotQuery = focusSpotFromQuery();
    if (!hasSpotQuery && shouldAutoAnchorToScenicEntry()) {
      enableSimulationMode({ forceAnchor: true });
      stage.fitOverview?.();
      pushToast('已自动定位到当前景区范围，可点击地图自由移动探索。');
    } else if (simulationMode.value) {
      placeSimulationStart(false);
    } else {
      startGpsWatch();
    }
  } catch (err) {
    error.value = err?.message || 'Explore page load failed.';
  }
}

function focusSpotFromQuery() {
  const spotId = route.query?.spot ? String(route.query.spot) : '';
  if (!spotId) return false;
  focusSpot(spotId);
  return true;
}

function shouldAutoAnchorToScenicEntry() {
  const entry = String(route.query?.entry || '').trim().toLowerCase();
  return AUTO_ANCHOR_ENTRY_SET.has(entry);
}

function handlePositionSave(position) {
  lastPosition.value = position;
}

function handleCurrentSpotChange(spotId) {
  currentSpotId.value = spotId;
}

function handleSpotPanelOpen(spotId) {
  currentSpotId.value = spotId;
}

function handleMapError(message) {
  if (!message) return;
  pushToast(`地图加载异常：${message}`, 9000);
}

function handleSpotDiscovered(spot) {
  const spotId = String(spot?.id || '').trim();
  if (!spotId) return;
  if (!discoveredSpotIds.value.map((item) => String(item)).includes(spotId)) {
    discoveredSpotIds.value = [...discoveredSpotIds.value, spotId];
  }

  // 自动打卡：到达点位后若有对应图鉴且未收集，则自动收集。
  const pendingStamp = stamps.value.find((item) => String(item.spotId) === spotId && !item.isCollected);
  if (pendingStamp) {
    collectStamp(pendingStamp, { auto: true, silent: true });
  }

  const pending = events.value.find((item) => String(item.spotId) === spotId && !item.isCompleted);
  if (!pending) return;
  surfacedEventIds.add(pending.id);
  if (activeEvent.value) {
    return;
  }
  nudgeEvent.value = pending;
}

function focusSpot(spotId, options = {}) {
  const {
    pan = true,
    openInfo = true,
    moveExplorer = false,
    triggerDiscover = true,
  } = options;
  currentSpotId.value = spotId;
  guideMapRef.value?.focusSpot(spotId, {
    pan,
    openInfo,
    moveExplorer,
    triggerDiscover,
  });
}

function openSpotPreview(spot) {
  if (!spot?.id) return;
  activeSpotPreview.value = spot;
}

function handleSpotNavigate(spotId) {
  const id = String(spotId || '').trim();
  if (!id) return;
  const fromRecommendation = recommendationCards.value.find((item) => String(item.id) === id);
  if (fromRecommendation) {
    navigateToSpot({ id, name: fromRecommendation.name });
    return;
  }
  const fromMap = spots.value.find((item) => String(item?.id || '') === id);
  if (fromMap) {
    navigateToSpot({ id, name: fromMap.name });
    return;
  }
  applyDirectNavigationToSpot(id, '目标看点');
}

function resolveCurrentNavigatorPosition() {
  const live = guideMapRef.value?.getExplorerPosition?.();
  if (Array.isArray(live) && live.length >= 2) {
    const liveLng = Number(live[0]);
    const liveLat = Number(live[1]);
    if (Number.isFinite(liveLng) && Number.isFinite(liveLat)) {
      return [liveLng, liveLat];
    }
  }

  const saved = Array.isArray(lastPosition.value) ? lastPosition.value : [];
  const lng = Number(saved[0]);
  const lat = Number(saved[1]);
  if (Number.isFinite(lng) && Number.isFinite(lat)) {
    return [lng, lat];
  }
  return [Number(mapConfig.value.lng), Number(mapConfig.value.lat)];
}

function resolveMapSpotById(spotId) {
  const id = String(spotId || '').trim();
  if (!id) return null;
  const mapResolvedLngLat = guideMapRef.value?.getSpotLngLat?.(id);
  const fromMap = spots.value.find((item) => String(item?.id || '').trim() === id);
  if (!fromMap && !mapResolvedLngLat) return null;
  const lng = Number(Array.isArray(mapResolvedLngLat) ? mapResolvedLngLat[0] : fromMap?.lng);
  const lat = Number(Array.isArray(mapResolvedLngLat) ? mapResolvedLngLat[1] : fromMap?.lat);
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) return null;
  return {
    id,
    name: String(fromMap?.name || '目的地'),
    kind: toCategoryLabel(fromMap?.kind || 'spot'),
    lng,
    lat,
  };
}

function applyDirectNavigationToSpot(spotId, spotName = '') {
  const target = resolveMapSpotById(spotId);
  if (!target) {
    pushToast('该看点缺少有效坐标，暂无法导航');
    return false;
  }

  const [fromLng, fromLat] = resolveCurrentNavigatorPosition();
  const normalizedTargetName = String(spotName || target.name || '目标看点').trim() || '目标看点';
  applyResolvedRoutePlan({
    title: `前往${normalizedTargetName}`,
    description: `已根据你当前所在位置规划前往${normalizedTargetName}的导航路线。`,
    tags: ['实时导航', '从当前位置出发'],
    displayMode: 'segment',
    arrivalRadius: 72,
    showStopCircles: false,
    followExplorer: true,
    stops: [
      {
        id: 'current-position',
        name: '当前位置',
        kind: '导航起点',
        coordSource: 'map',
        lng: fromLng,
        lat: fromLat,
      },
      {
        ...target,
        coordSource: 'map',
      },
    ],
  }, `已生成前往${normalizedTargetName}的路线`, {
    switchPanel: false,
  });
  return true;
}

function navigateToSpot(spot) {
  if (!spot?.id) return;
  const applied = applyDirectNavigationToSpot(spot.id, spot.name);
  if (!applied) return;
  focusSpot(spot.id, {
    pan: true,
    openInfo: true,
    moveExplorer: false,
    triggerDiscover: false,
  });
}

function navigateFromPreview() {
  if (!activeSpotPreview.value?.id) return;
  navigateToSpot(activeSpotPreview.value);
  activeSpotPreview.value = null;
}

function resolveRouteStart(routeLike) {
  if (Array.isArray(routeLike?.stops) && routeLike.stops.length) {
    return routeLike.stops[0];
  }
  if (Array.isArray(routeLike?.spots) && routeLike.spots.length) {
    return routeLike.spots[0];
  }
  return null;
}

function navigateToRouteStart(routeLike) {
  const start = resolveRouteStart(routeLike);
  if (!start?.id) {
    pushToast('当前路线缺少可导航的首站');
    return;
  }
  const startName = start.name || '路线起点';
  const applied = applyDirectNavigationToSpot(start.id, startName);
  if (!applied) return;
  focusSpot(start.id, {
    pan: true,
    openInfo: true,
    moveExplorer: false,
    triggerDiscover: false,
  });
}

async function runSearch() {
  const keyword = searchKeyword.value.trim();
  if (!keyword) {
    searchResults.value = [];
    return;
  }
  const result = await request(API.search(scenicId.value), { params: { keyword } }).catch(() => []);
  searchResults.value = Array.isArray(result) ? result : [];
}

function setExplorationPanelCollapsed(next) {
  isExplorationPanelCollapsed.value = Boolean(next);
}

function applyRoute(routeItem) {
  const routeSpots = Array.isArray(routeItem.spots) ? routeItem.spots : [];
  const mappedStops = routeSpots
    .map((item) => {
      const lng = Number(item.location?.lng ?? item.lng);
      const lat = Number(item.location?.lat ?? item.lat);
      if (!Number.isFinite(lng) || !Number.isFinite(lat)) {
        return {
          id: '',
          name: '',
          kind: '',
          lng: NaN,
          lat: NaN,
        };
      }
      const [mapLng, mapLat] = toMapCoordinateBySource(lng, lat);
      return {
        id: item.id,
        name: item.name,
        kind: toCategoryLabel(item.category || item.kind || 'spot'),
        coordSource: 'map',
        lng: mapLng,
        lat: mapLat,
      };
    })
    .filter((item) => item.id && Number.isFinite(item.lng) && Number.isFinite(item.lat));

  const applied = applyResolvedRoutePlan({
    title: routeItem.name,
    description: routeItem.description,
    reasons: [routeItem.description],
    tags: [routeItem.duration, routeItem.difficulty, routeItem.distance].filter(Boolean),
    displayMode: 'segment',
    arrivalRadius: 86,
    stops: mappedStops,
  }, `已应用路线：${routeItem.name}`);

  if (!applied) {
    pushToast('当前路线暂无可用的地图坐标');
  }
}

function reapplyAssistantRoute() {
  if (!routePlan.value) return;
  applyResolvedRoutePlan(routePlan.value, '已应用助手推荐路线', { fromAssistant: true });
}

function stopNavigation(options = {}) {
  const { silent = false } = options;
  routePlan.value = null;
  routePlanSource.value = '';
  clearPendingAssistantRoutePlan();
  guideMapRef.value?.clearPlanOverlays?.();
  if (!silent) {
    pushToast('已停止当前导航');
  }
}

async function collectStamp(stamp, options = {}) {
  const { auto = false, silent = false } = options;
  if (!stamp || stamp.isCollected) return;
  if (collectingStampId.value === stamp.id) return;
  collectingStampId.value = stamp.id;
  try {
    const updated = await request(API.collectStamp(scenicId.value, stamp.id), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    });
    stamps.value = stamps.value.map((item) => (item.id === updated.id ? updated : item));
    if (!silent) {
      pushToast(auto ? `已自动打卡：${updated.name}` : `已收集图鉴：${updated.name}`);
    }
  } catch (err) {
    if (!silent) {
      pushToast(err?.message || '收集图鉴失败');
    }
  } finally {
    collectingStampId.value = '';
  }
}

function openNudgeEvent() {
  const pendingId = String(nudgeEvent.value?.id || '');
  if (!pendingId) return;
  const latest = events.value.find((item) => String(item.id) === pendingId && !item.isCompleted);
  activeEvent.value = latest || nudgeEvent.value;
  nudgeEvent.value = null;
}

function dismissNudgeEvent() {
  nudgeEvent.value = null;
}

function eventSpotName(spotId) {
  if (!spotId) return '自然事件';
  const fromCard = recommendationCards.value.find((item) => item.id === spotId);
  if (fromCard?.name) return fromCard.name;
  const fromMap = spots.value.find((item) => item.id === spotId);
  if (fromMap?.name) return fromMap.name;
  return '自然事件';
}

async function interactEvent(answer = '') {
  if (!activeEvent.value) return;
  try {
    const result = await request(API.interactEvent(scenicId.value, activeEvent.value.id), {
      method: 'POST',
      data: { answer },
      headers: { 'Content-Type': 'application/json' },
    });
    if (result?.event) {
      events.value = events.value.map((item) => (item.id === result.event.id ? result.event : item));
      pushToast(result.message || '事件已完成');
    }
    activeEvent.value = null;
  } catch (err) {
    pushToast(err?.message || '提交事件失败');
  }
}

function stopGpsWatch() {
  if (geoWatchId.value == null || typeof navigator === 'undefined' || !navigator.geolocation) return;
  navigator.geolocation.clearWatch(geoWatchId.value);
  geoWatchId.value = null;
}

function hasSavedPosition() {
  return Array.isArray(lastPosition.value)
    && lastPosition.value.length >= 2
    && Number.isFinite(Number(lastPosition.value[0]))
    && Number.isFinite(Number(lastPosition.value[1]));
}

function placeSimulationStart(force = false) {
  if (!force && hasSavedPosition()) return;
  guideMapRef.value?.jumpExplorerTo(simulationAnchorPoint.value, {
    pan: true,
    save: true,
    check: true,
  });
}

function enableSimulationMode({ forceAnchor = false } = {}) {
  simulationMode.value = true;
  stopGpsWatch();
  placeSimulationStart(forceAnchor);
}

function startGpsWatch() {
  if (typeof navigator === 'undefined' || !navigator.geolocation) {
    enableSimulationMode({ forceAnchor: true });
    pushToast('当前设备无法使用定位功能，已切换到模拟定位。');
    return false;
  }
  stopGpsWatch();
  geoWatchId.value = navigator.geolocation.watchPosition(
    (position) => {
      const lng = Number(position.coords.longitude);
      const lat = Number(position.coords.latitude);
      const [mapLng, mapLat] = toMapCoordinateBySource(lng, lat);
      // Keep user free to drag the map; only update explorer position.
      guideMapRef.value?.jumpExplorerTo([mapLng, mapLat], {
        pan: false,
        save: true,
        check: true,
      });
    },
    () => {
      enableSimulationMode({ forceAnchor: true });
      pushToast('获取定位失败，已切换到模拟定位。');
    },
    { enableHighAccuracy: true, timeout: 6000, maximumAge: 10000 },
  );
  return true;
}

function toggleSimulation() {
  simulationMode.value = !simulationMode.value;
  if (simulationMode.value) {
    enableSimulationMode({ forceAnchor: true });
    pushToast('已开启模拟模式，点击地图即可自由移动探索。');
  } else {
    const started = startGpsWatch();
    if (started) {
      pushToast('已切换到实时定位模式。');
    }
  }
}

watch(() => route.query.assistantRouteApply, () => {
  if (!loading.value) {
    applyPendingAssistantRoutePlan();
  }
});

watch(() => route.query.spot, () => {
  if (!loading.value) focusSpotFromQuery();
});

watch(() => route.params.scenicId, () => {
  activeSpotPreview.value = null;
  stopGpsWatch();
  initPage();
});

onMounted(() => {
  window.addEventListener(ASSISTANT_ROUTE_EVENT, handleAssistantRoutePlanEvent);
  initPage();
});

onBeforeUnmount(() => {
  window.removeEventListener(ASSISTANT_ROUTE_EVENT, handleAssistantRoutePlanEvent);
  stopGpsWatch();
  guideMapRef.value?.destroyMap?.();
});
</script>
<style scoped>
.explore-shell {
  position: relative;
  width: 100%;
  height: 100dvh;
  overflow: hidden;
  color: #1f3e32;
  font-family: 'Plus Jakarta Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: #d9d1c3;
}

.map-canvas-layer {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.map-gradient-overlay {
  position: absolute;
  inset: 0;
  z-index: 3;
  pointer-events: none;
  background:
    radial-gradient(circle at 12% 50%, rgba(26, 74, 58, 0.16), transparent 34%),
    radial-gradient(circle at 90% 56%, rgba(26, 74, 58, 0.16), transparent 32%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(26, 74, 58, 0.14));
}

.map-controls {
  position: absolute;
  top: 14px;
  left: 14px;
  z-index: 20;
  display: flex;
  gap: 8px;
}

.back-home,
.gps-toggle {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid rgba(26, 74, 58, 0.16);
  background: rgba(245, 240, 232, 0.9);
  color: #245041;
  font-size: 12px;
  font-weight: 600;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px rgba(26, 74, 58, 0.16);
  cursor: pointer;
}

.back-home:hover,
.gps-toggle:hover {
  background: rgba(248, 244, 237, 0.98);
}

.exploration-panel {
  position: absolute;
  top: 18px;
  bottom: 18px;
  border-radius: 24px;
  background: rgba(245, 240, 232, 0.9);
  border: 1px solid rgba(46, 139, 87, 0.14);
  backdrop-filter: blur(20px);
  box-shadow: 0 10px 42px rgba(26, 74, 58, 0.18), 0 0 0 1px rgba(46, 139, 87, 0.08);
  overflow: hidden;
  z-index: 12;
}

.exploration-panel {
  right: 16px;
  width: min(290px, calc(100vw - 32px));
  display: grid;
  grid-template-rows: auto auto auto auto auto minmax(0, 1fr);
}

.explore-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(26, 74, 58, 0.14);
}

.explore-head h2 {
  margin: 0;
  font-size: 24px;
  line-height: 1.08;
  color: #1f4337;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
}

.explore-head-collapse {
  width: 24px;
  height: 24px;
  border-radius: 999px;
  border: 1px solid rgba(26, 74, 58, 0.14);
  background: rgba(255, 255, 255, 0.56);
  color: rgba(31, 67, 55, 0.62);
  font-size: 18px;
  line-height: 1;
  display: grid;
  place-items: center;
  cursor: pointer;
}

.explore-head-collapse:hover {
  background: rgba(255, 255, 255, 0.9);
}

.explore-panel-reopen {
  position: absolute;
  top: 18px;
  right: 16px;
  z-index: 25;
  min-height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(46, 139, 87, 0.2);
  background: rgba(245, 240, 232, 0.92);
  color: #235040;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 10px 24px rgba(26, 74, 58, 0.18);
  cursor: pointer;
}

.explore-panel-reopen:hover {
  background: rgba(248, 244, 237, 0.98);
}

.search-bar {
  padding: 10px 12px;
}

.search-bar input {
  width: 100%;
  min-height: 34px;
  border: 1px solid rgba(26, 74, 58, 0.14);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.56);
  color: #20483b;
  padding: 0 12px;
  font-size: 13px;
}

.search-bar input:focus-visible {
  outline: none;
  border-color: rgba(42, 122, 95, 0.48);
}

.journey-tabs {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px 6px;
  border-bottom: 1px solid rgba(26, 74, 58, 0.1);
}

.journey-tabs button {
  position: relative;
  min-height: 32px;
  border: 0;
  padding: 0;
  background: transparent;
  color: rgba(31, 67, 55, 0.52);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.journey-tabs button.active {
  color: #205846;
}

.journey-tabs button.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  border-radius: 999px;
  background: #2e8b57;
}

.journey-progress-meta {
  padding: 8px 16px 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: rgba(31, 67, 55, 0.66);
  font-size: 11px;
}

.journey-progress-bar {
  margin: 6px 16px 10px;
  height: 6px;
  border-radius: 999px;
  background: rgba(26, 74, 58, 0.14);
  overflow: hidden;
}

.journey-progress-bar span {
  height: 100%;
  display: block;
  background: linear-gradient(90deg, #2d7d5f, #3fa17c);
}

.journey-content {
  min-height: 0;
  overflow-y: auto;
  padding: 0 10px 10px;
}

.panel-section {
  display: grid;
  gap: 10px;
}

.discover-card,
.stamp-card,
.route-card,
.empty-card {
  border-radius: 16px;
  border: 1px solid rgba(26, 74, 58, 0.12);
  background: rgba(255, 255, 255, 0.58);
  box-shadow: 0 6px 16px rgba(26, 74, 58, 0.08);
}

.discover-card {
  padding: 10px;
  display: grid;
  gap: 7px;
  cursor: pointer;
}

.discover-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.discover-title {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.pin-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #2e8b57;
  box-shadow: 0 0 0 3px rgba(46, 139, 87, 0.15);
}

.discover-title strong {
  color: #1f4a3b;
  font-size: 15px;
  line-height: 1.2;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.spot-kind {
  flex-shrink: 0;
  min-height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  border: 1px solid rgba(26, 74, 58, 0.12);
  color: rgba(31, 67, 55, 0.52);
  font-size: 10px;
  display: inline-flex;
  align-items: center;
}

.discover-card p {
  margin: 0;
  color: rgba(31, 67, 55, 0.76);
  font-size: 12px;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.discover-actions,
.route-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.spot-action,
.stamp-body button,
.route-card button,
.event-stamp-action,
.event-options button,
.event-actions button,
.event-nudge-open,
.event-nudge-close {
  min-height: 28px;
  padding: 0 10px;
  border-radius: 10px;
  border: 1px solid rgba(26, 74, 58, 0.2);
  font-size: 12px;
  cursor: pointer;
}

.spot-action,
.stamp-body button,
.route-card button,
.event-stamp-action,
.event-options button,
.event-actions button,
.event-nudge-open {
  color: #fff;
  background: linear-gradient(135deg, #2f6f58, #3b8b6b);
}

.spot-action.ghost,
.route-card button.ghost,
.event-actions .ghost,
.event-nudge-close {
  color: #255241;
  background: rgba(255, 255, 255, 0.68);
  border-color: rgba(26, 74, 58, 0.14);
}

.empty-card {
  padding: 18px 12px;
  text-align: center;
  color: rgba(31, 67, 55, 0.54);
  font-size: 12px;
}

.stamp-card {
  padding: 8px;
  display: grid;
  grid-template-columns: 62px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.stamp-card img {
  width: 62px;
  height: 62px;
  border-radius: 10px;
  object-fit: cover;
}

.stamp-body {
  display: grid;
  gap: 4px;
}

.stamp-body strong,
.route-card h3,
.spot-preview-body h3,
.event-card h3 {
  margin: 0;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  color: #1f4537;
}

.stamp-body strong {
  font-size: 16px;
}

.stamp-body small,
.route-card small {
  color: rgba(31, 67, 55, 0.6);
  font-size: 11px;
}

.route-card {
  padding: 10px;
  display: grid;
  gap: 6px;
}

.route-card p,
.spot-preview-body p,
.event-card p {
  margin: 0;
  color: rgba(31, 67, 55, 0.76);
  font-size: 13px;
  line-height: 1.62;
}

.assistant-route-card {
  border-color: rgba(46, 139, 87, 0.26);
  background: linear-gradient(160deg, rgba(233, 249, 241, 0.92), rgba(246, 255, 250, 0.88));
}

.floating-stop-nav {
  position: absolute;
  top: 14px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 26;
  min-height: 38px;
  min-width: 132px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(148, 58, 41, 0.35);
  background: linear-gradient(135deg, rgba(166, 76, 56, 0.95), rgba(148, 58, 41, 0.95));
  color: #fff5f1;
  display: inline-flex;
  gap: 6px;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 10px 20px rgba(89, 34, 22, 0.28);
}

.floating-stop-nav-icon {
  font-size: 13px;
  line-height: 1;
}

.event-nudge {
  position: absolute;
  left: 50%;
  bottom: 20px;
  transform: translateX(-50%);
  z-index: 21;
  width: min(560px, calc(100% - 28px));
  border-radius: 16px;
  border: 1px solid rgba(53, 102, 82, 0.34);
  background: linear-gradient(132deg, rgba(255, 252, 245, 0.95), rgba(246, 242, 233, 0.92));
  color: #1f3a30;
  box-shadow: 0 10px 20px rgba(25, 33, 28, 0.2);
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.event-nudge-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 17px;
  color: #fff;
  background: linear-gradient(145deg, #2f5f4b, #3b7a60);
}

.event-nudge-copy {
  min-width: 0;
  flex: 1;
}

.event-nudge-copy p {
  margin: 0;
  font-size: 11px;
  color: rgba(31, 67, 55, 0.64);
}

.event-nudge-copy strong {
  display: block;
  margin-top: 2px;
  font-size: 14px;
  line-height: 1.2;
  color: #1d4134;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.event-nudge-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.spot-preview-mask,
.event-mask {
  position: fixed;
  inset: 0;
  z-index: 38;
  background: rgba(0, 0, 0, 0.45);
}

.spot-preview-mask {
  display: grid;
  place-items: center;
  padding: 14px;
}

.spot-preview-card {
  width: min(520px, 100%);
  border-radius: 18px;
  border: 1px solid rgba(51, 63, 56, 0.16);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(251, 245, 232, 0.9));
  overflow: hidden;
  box-shadow: 0 24px 48px rgba(18, 21, 17, 0.28);
}

.spot-preview-cover {
  height: 190px;
}

.spot-preview-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.spot-preview-body {
  padding: 14px;
  display: grid;
  gap: 10px;
}

.spot-preview-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.spot-badges {
  display: inline-flex;
  gap: 6px;
  flex-wrap: wrap;
}

.badge {
  min-height: 22px;
  border-radius: 999px;
  padding: 0 8px;
  border: 1px solid rgba(58, 64, 55, 0.14);
  background: #faf8f3;
  color: #2e4139;
  font-size: 11px;
  display: inline-flex;
  align-items: center;
}

.badge.visited {
  border-color: rgba(179, 139, 44, 0.34);
  color: #7f6121;
  background: rgba(245, 220, 149, 0.28);
}

.event-mask {
  z-index: 40;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.event-card {
  width: min(720px, 100%);
  border-radius: 24px 24px 0 0;
  border: 1px solid rgba(51, 63, 56, 0.18);
  border-bottom: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(251, 245, 232, 0.94));
  color: #204439;
  padding: 20px 20px calc(18px + env(safe-area-inset-bottom));
  box-shadow: 0 -8px 24px rgba(18, 21, 17, 0.2);
  position: relative;
  max-height: min(86vh, 720px);
  overflow-y: auto;
}

.event-close {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 36px;
  height: 36px;
  border-radius: 999px;
  border: 1px solid rgba(41, 73, 59, 0.2);
  background: rgba(255, 255, 255, 0.72);
  color: #27473a;
  font-size: 20px;
  cursor: pointer;
}

.event-head {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

.event-head-icon {
  width: 44px;
  height: 44px;
  border-radius: 13px;
  display: grid;
  place-items: center;
  font-size: 20px;
  color: #fff;
  background: linear-gradient(145deg, #2f5f4b, #3b7a60);
}

.event-head-copy span {
  display: inline-flex;
  min-height: 22px;
  align-items: center;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(41, 73, 59, 0.18);
  color: rgba(36, 80, 65, 0.78);
  font-size: 11px;
  background: rgba(255, 255, 255, 0.66);
  margin-bottom: 6px;
}

.event-stamp-row {
  margin-top: 14px;
  padding: 10px;
  border-radius: 14px;
  border: 1px solid rgba(41, 73, 59, 0.18);
  background: rgba(255, 255, 255, 0.66);
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
}

.event-stamp-row img {
  width: 52px;
  height: 52px;
  border-radius: 11px;
  object-fit: cover;
}

.event-stamp-row small {
  display: block;
  color: rgba(31, 67, 55, 0.66);
  font-size: 11px;
  margin-bottom: 3px;
}

.event-stamp-row strong {
  display: block;
  color: #17392f;
  font-size: 14px;
  line-height: 1.25;
}

.event-options {
  margin-top: 14px;
  display: grid;
  gap: 8px;
}

.event-actions {
  margin-top: 12px;
  display: inline-flex;
  gap: 8px;
  flex-wrap: wrap;
}

.toast-host {
  position: fixed;
  top: 16px;
  right: 16px;
  z-index: 44;
  display: grid;
  gap: 8px;
}

.toast-item {
  border-radius: 11px;
  border: 1px solid rgba(35, 69, 54, 0.18);
  background: rgba(255, 252, 244, 0.92);
  color: #234536;
  box-shadow: 0 10px 20px rgba(18, 21, 17, 0.14);
  padding: 8px 10px;
  font-size: 12px;
}

.panel-swap-enter-active,
.panel-swap-leave-active,
.nudge-float-enter-active,
.nudge-float-leave-active,
.stop-nav-float-enter-active,
.stop-nav-float-leave-active,
.preview-fade-enter-active,
.preview-fade-leave-active,
.event-sheet-enter-active,
.event-sheet-leave-active,
.toast-stack-enter-active,
.toast-stack-leave-active {
  transition: opacity 0.28s ease, transform 0.32s ease;
}

.panel-swap-enter-from,
.panel-swap-leave-to,
.toast-stack-enter-from,
.toast-stack-leave-to {
  opacity: 0;
  transform: translateY(14px);
}

.nudge-float-enter-from,
.nudge-float-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(26px) scale(0.92);
}

.stop-nav-float-enter-from,
.stop-nav-float-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-8px) scale(0.94);
}

.preview-fade-enter-from,
.preview-fade-leave-to,
.event-sheet-enter-from,
.event-sheet-leave-to {
  opacity: 0;
}

.preview-fade-enter-from .spot-preview-card,
.preview-fade-leave-to .spot-preview-card {
  transform: translateY(30px) scale(0.94);
}

.event-sheet-enter-from .event-card,
.event-sheet-leave-to .event-card {
  transform: translateY(100%);
}

.toast-stack-move {
  transition: transform 0.28s ease;
}

.reveal-item {
  opacity: 0;
  transform: translateY(18px);
  animation: item-reveal 0.46s cubic-bezier(0.2, 0.9, 0.28, 1) var(--stagger-delay, 0ms) forwards;
}

@keyframes item-reveal {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1180px) {
  .explore-panel-reopen {
    top: 12px;
    right: 12px;
  }

  .exploration-panel {
    top: 12px;
    bottom: auto;
    height: min(46vh, 430px);
  }

  .exploration-panel {
    right: 12px;
    width: min(300px, calc(100vw - 24px));
  }
}

@media (max-width: 900px) {
  .explore-shell {
    overflow-y: auto;
  }

  .map-canvas-layer {
    min-height: 62vh;
  }

  .exploration-panel {
    position: relative;
    left: auto;
    right: auto;
    top: auto;
    bottom: auto;
    width: calc(100% - 24px);
    height: auto;
    margin: 12px;
  }

  .exploration-panel {
    margin-top: 74px;
    min-height: 52vh;
  }

  .explore-panel-reopen {
    top: 10px;
    right: 10px;
  }

  .map-controls {
    top: 10px;
    left: 10px;
  }

  .floating-stop-nav {
    top: 10px;
    left: 50%;
    min-height: 34px;
    min-width: 118px;
  }

  .event-nudge {
    width: calc(100% - 20px);
    bottom: 10px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .reveal-item {
    animation: none;
    opacity: 1;
    transform: none;
  }

  .panel-swap-enter-active,
  .panel-swap-leave-active,
  .nudge-float-enter-active,
  .nudge-float-leave-active,
  .stop-nav-float-enter-active,
  .stop-nav-float-leave-active,
  .preview-fade-enter-active,
  .preview-fade-leave-active,
  .event-sheet-enter-active,
  .event-sheet-leave-active,
  .toast-stack-enter-active,
  .toast-stack-leave-active {
    transition: none;
  }
}
</style>




