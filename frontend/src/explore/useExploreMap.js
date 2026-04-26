import { ref } from 'vue';

const AMAP_KEY = String(import.meta.env.VITE_AMAP_KEY || '').trim();
const AMAP_SECURITY_JS_CODE = String(import.meta.env.VITE_AMAP_SECURITY_JS_CODE || '').trim();
const AMAP_PLUGIN_LIST = 'AMap.GeometryUtil,AMap.Scale,AMap.ToolBar,AMap.Walking,AMap.PlaceSearch';
const MIN_SPOT_TRIGGER_RADIUS_METERS = 420;
const MAX_SPOT_TRIGGER_RADIUS_METERS = 900;
const SPOT_TRIGGER_RADIUS_SCALE = 1.8;
const ROUTE_SEGMENT_ARRIVAL_RADIUS_METERS = 86;
const ROUTE_DESTINATION_REACHED_METERS = 14;
let amapLoaderPromise = null;

function cleanupRuntimeScripts() {
  if (typeof document === 'undefined') return;
  Array.from(document.querySelectorAll('script'))
    .filter((item) => String(item?.src || '').includes('webapi.amap.com/maps'))
    .forEach((item) => item.remove());
}

function resetAmapRuntime() {
  if (typeof window === 'undefined') return;
  cleanupRuntimeScripts();
  try {
    delete window.AMap;
  } catch {
    window.AMap = undefined;
  }
  try {
    delete window._AMapSecurityConfig;
  } catch {
    window._AMapSecurityConfig = undefined;
  }
}

function loadAmapScriptOnce(securityJsCode = '') {
  return new Promise((resolve, reject) => {
    resetAmapRuntime();
    if (securityJsCode) {
      window._AMapSecurityConfig = { securityJsCode };
    }

    const src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}&plugin=${AMAP_PLUGIN_LIST}&_ts=${Date.now()}`;
    const script = document.createElement('script');
    let timeoutId = null;

    const cleanup = () => {
      if (timeoutId) {
        window.clearTimeout(timeoutId);
        timeoutId = null;
      }
      script.removeEventListener('load', handleLoad);
      script.removeEventListener('error', handleError);
    };

    const handleLoad = () => {
      cleanup();
      if (window.AMap) {
        resolve(window.AMap);
      } else {
        reject(new Error('AMap script loaded but window.AMap is missing.'));
      }
    };

    const handleError = () => {
      cleanup();
      reject(new Error('AMap script load failed.'));
    };

    timeoutId = window.setTimeout(() => {
      cleanup();
      reject(new Error('AMap load timeout.'));
    }, 15000);

    script.src = src;
    script.async = true;
    script.defer = true;
    script.setAttribute('data-amap-runtime-loader', 'true');
    script.addEventListener('load', handleLoad, { once: true });
    script.addEventListener('error', handleError, { once: true });
    document.head.appendChild(script);
  });
}

function ensureAMapScriptLoaded() {
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    return Promise.reject(new Error('Current environment does not support map loading.'));
  }
  if (window.AMap) {
    return Promise.resolve(window.AMap);
  }
  if (amapLoaderPromise) {
    return amapLoaderPromise;
  }

  if (!AMAP_KEY) {
    return Promise.reject(new Error('AMap key missing. Set VITE_AMAP_KEY in fronted/.env.local and restart dev server.'));
  }

  const securityJsCode = AMAP_SECURITY_JS_CODE || String(window.__AMAP_SECURITY_JS_CODE || '').trim();

  amapLoaderPromise = (async () => {
    try {
      return await loadAmapScriptOnce(securityJsCode);
    } catch (firstError) {
      if (!securityJsCode) {
        throw new Error(`${firstError.message} Check key, localhost domain whitelist, and webapi.amap.com access.`);
      }
      try {
        return await loadAmapScriptOnce('');
      } catch (retryError) {
        throw new Error(
          `AMap load failed with/without security code. First: ${firstError.message}; Retry: ${retryError.message}`,
        );
      }
    }
  })().catch((error) => {
    amapLoaderPromise = null;
    throw error;
  });

  return amapLoaderPromise;
}
export function useExploreMap({
  getMapConfig,
  getSpots,
  getLastPosition,
  getCurrentSpotId,
  getDiscoveredSpotIds,
  getPendingEventSpotIds,
  onCurrentSpotChange,
  onSpotDiscovered,
  onPositionSave,
  onSpotPanelOpen,
  onSpotNavigate,
  onMapError,
  triggerCooldown = 12000,
} = {}) {
  const mapRef = ref(null);
  const mapState = {
    map: null,
    infoWindow: null,
    explorerMarker: null,
    spotMarkers: new Map(),
    activePolyline: null,
    activeCircles: [],
    activeRouteFullPath: [],
    activeRouteStops: [],
    activeRouteTargetStopIndex: -1,
    activeRouteMode: 'segment',
    activeRouteFollowExplorer: false,
    activeRouteProgressIndex: 0,
    walkingService: null,
    placeSearchService: null,
    resolvedSpotCoords: new Map(),
    isResolvingSpotCoords: false,
    routeRenderToken: 0,
    cooldownUntil: new Map(),
    baseLayerCheckTimer: null,
    markerStateFingerprint: '',
    markerStateBySpotId: new Map(),
    markerStateSyncRafId: null,
    lastCurrentSpotId: '',
    limitBounds: null,
  };
  const KIND_EMOJI_MAP = {
    animal: '&#128054;',
    plant: '&#127793;',
    story: '&#128214;',
    item: '&#127919;',
    view: '&#127748;',
    service: '&#9733;',
    scenic: '&#128205;',
    spot: '&#128205;',
  };
  const SPOT_EMOJI_BY_ID = {
    'nuorilang-waterfall': '🌊',
    'pearl-shoal-waterfall': '💧',
    'wuhua-lake': '🌈',
    'mirror-lake': '🪞',
    'panda-lake': '🐼',
    'arrow-bamboo-lake': '🎋',
    'rhino-lake': '🌲',
    'shuzheng-waterfall': '🏞️',
    'sleeping-dragon-lake': '🐉',
    'spark-lake': '✨',
    'wucaichi-pond': '🎨',
    'long-lake': '⛰️',
  };
  const POI_ALIGN_SKIP_SPOT_IDS = new Set([
    'nuorilang-waterfall',
  ]);
  const POI_ALIGN_MAX_DISTANCE_METERS = 1500;

  function getMapFitPadding() {
    if (typeof window !== 'undefined' && window.innerWidth <= 920) {
      return [56, 52, 72, 52];
    }
    // Explore page uses a side-by-side layout (panel is outside map area),
    // so oversized horizontal padding will force map to zoom out too much.
    return [72, 72, 96, 72];
  }

  function toNumericLngLatPair(value) {
    if (!Array.isArray(value) || value.length < 2) return null;
    const lng = Number(value[0]);
    const lat = Number(value[1]);
    if (!Number.isFinite(lng) || !Number.isFinite(lat)) return null;
    return [lng, lat];
  }

  function resolveLimitBounds(config) {
    if (!config || typeof config !== 'object') return null;
    const fromArray = Array.isArray(config.limitBounds) ? config.limitBounds : (Array.isArray(config.bounds) ? config.bounds : null);
    if (!fromArray || fromArray.length < 2) return null;

    let sw = toNumericLngLatPair(fromArray[0]);
    let ne = toNumericLngLatPair(fromArray[1]);
    if (!sw || !ne) return null;

    const minLng = Math.min(sw[0], ne[0]);
    const maxLng = Math.max(sw[0], ne[0]);
    const minLat = Math.min(sw[1], ne[1]);
    const maxLat = Math.max(sw[1], ne[1]);
    sw = [minLng, minLat];
    ne = [maxLng, maxLat];

    if (Math.abs(maxLng - minLng) < 1e-6 || Math.abs(maxLat - minLat) < 1e-6) return null;

    return { sw, ne };
  }

  function clampLngLatToLimitBounds(value) {
    if (!mapState.limitBounds) return value;
    const [lng, lat] = value;
    const minLng = mapState.limitBounds.sw[0];
    const maxLng = mapState.limitBounds.ne[0];
    const minLat = mapState.limitBounds.sw[1];
    const maxLat = mapState.limitBounds.ne[1];
    return [
      Math.min(Math.max(lng, minLng), maxLng),
      Math.min(Math.max(lat, minLat), maxLat),
    ];
  }

  function toIdKey(value) {
    return String(value ?? '').trim();
  }

  function toIdSet(list) {
    if (!Array.isArray(list)) return new Set();
    return new Set(list.map((item) => toIdKey(item)).filter(Boolean));
  }

  function escapeHtml(text) {
    return String(text ?? '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  }

  function normalizeEntity(value) {
    const raw = String(value || '').trim();
    if (!raw) return '';
    if (/^&#\d+;?$/.test(raw) || /^&#x[0-9a-f]+;?$/i.test(raw)) {
      return raw.endsWith(';') ? raw : `${raw};`;
    }
    return escapeHtml(raw);
  }

  function resolveSpotEmoji(spot) {
    const spotId = toIdKey(spot?.id);
    if (spotId && SPOT_EMOJI_BY_ID[spotId]) {
      return normalizeEntity(SPOT_EMOJI_BY_ID[spotId]);
    }
    const direct = normalizeEntity(spot?.emoji);
    if (direct) return direct;
    const kind = String(spot?.kind || '').trim().toLowerCase();
    return KIND_EMOJI_MAP[kind] || '&#128205;';
  }

  function resolveSpotShortName(spot) {
    const source = String(spot?.shortName || spot?.name || '').trim();
    if (!source) return '看点';
    return source.slice(0, 6);
  }

  function getExplorerPosition() {
    const position = mapState.explorerMarker?.getPosition?.();
    if (!position) return null;
    const normalized = normalizeLngLat(position);
    if (normalized) return normalized;
    if (typeof position?.toArray === 'function') {
      return normalizeLngLat(position.toArray());
    }
    return null;
  }

  function distanceMeters(fromLngLat, toLngLat) {
    if (
      window.AMap
      && window.AMap.GeometryUtil
      && typeof window.AMap.GeometryUtil.distance === 'function'
    ) {
      return window.AMap.GeometryUtil.distance(fromLngLat, toLngLat);
    }

    // Fallback: Haversine distance.
    const [lng1, lat1] = fromLngLat;
    const [lng2, lat2] = toLngLat;
    const r = 6371000;
    const toRad = (v) => (v * Math.PI) / 180;
    const dLat = toRad(lat2 - lat1);
    const dLng = toRad(lng2 - lng1);
    const a = Math.sin(dLat / 2) ** 2
      + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
    return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  function resolveAdaptiveZoom(distanceValue) {
    const distance = Number(distanceValue);
    if (!Number.isFinite(distance) || distance <= 0) return 12.8;

    if (distance < 300) return 15.2;
    if (distance < 800) return 14.5;
    if (distance < 1500) return 13.9;
    if (distance < 3000) return 13.3;
    if (distance < 6000) return 12.6;
    if (distance < 12000) return 11.9;
    if (distance < 24000) return 11.2;
    if (distance < 50000) return 10.4;
    if (distance < 100000) return 9.7;
    if (distance < 200000) return 9.0;
    if (distance < 400000) return 8.3;
    return 7.6;
  }

  function stabilizeMapZoomByDistance(distanceValue) {
    if (!mapState.map || typeof mapState.map.getZoom !== 'function' || typeof mapState.map.setZoom !== 'function') {
      return;
    }
    const currentZoom = Number(mapState.map.getZoom());
    if (!Number.isFinite(currentZoom)) return;

    const preferredZoom = resolveAdaptiveZoom(distanceValue);
    const minZoom = preferredZoom - 0.8;
    const maxZoom = preferredZoom + 1.0;
    if (currentZoom < minZoom) {
      mapState.map.setZoom(minZoom);
      return;
    }
    if (currentZoom > maxZoom) {
      mapState.map.setZoom(maxZoom);
    }
  }

  function estimatePathDistanceMeters(path = []) {
    if (!Array.isArray(path) || path.length < 2) return 0;
    let total = 0;
    for (let index = 1; index < path.length; index += 1) {
      const from = normalizeLngLat(path[index - 1]);
      const to = normalizeLngLat(path[index]);
      if (!from || !to) continue;
      total += distanceMeters(from, to);
    }
    return total;
  }

  function resolveNearestPendingEventSpotId(pendingEventSet = toIdSet(getPendingEventSpotIds?.())) {
    if (!pendingEventSet.size) return '';
    const explorer = getExplorerPosition();
    if (!explorer) return '';

    let nearestId = '';
    let minDistance = Number.POSITIVE_INFINITY;

    getSpotsList().forEach((spot) => {
      const spotId = toIdKey(spot?.id);
      if (!spotId || !pendingEventSet.has(spotId)) return;
      const spotLngLat = resolveSpotLngLat(spot);
      if (!spotLngLat) return;
      const distance = distanceMeters(explorer, spotLngLat);
      if (distance < minDistance) {
        minDistance = distance;
        nearestId = spotId;
      }
    });

    return nearestId;
  }

  function buildMarkerStateContext() {
    const currentSpotId = toIdKey(getCurrentSpotId?.());
    const discoveredSet = toIdSet(getDiscoveredSpotIds?.());
    const pendingEventSet = toIdSet(getPendingEventSpotIds?.());
    const nearestEventSpotId = resolveNearestPendingEventSpotId(pendingEventSet);
    return {
      currentSpotId,
      discoveredSet,
      pendingEventSet,
      nearestEventSpotId,
    };
  }

  function getSpotState(spot, context) {
    const safeContext = context || buildMarkerStateContext();
    const spotId = toIdKey(spot?.id);
    return {
      isCurrent: Boolean(spotId && spotId === safeContext.currentSpotId),
      isDiscovered: Boolean(spotId && safeContext.discoveredSet.has(spotId)),
      hasEvent: Boolean(spotId && safeContext.pendingEventSet.has(spotId)),
      isEventFocus: Boolean(spotId && spotId === safeContext.nearestEventSpotId),
    };
  }

  function createMarkerContent(spot, context, state = null) {
    const resolvedState = state || getSpotState(spot, context);
    const classNames = [
      'spot-marker',
      resolvedState.isCurrent ? 'is-current' : '',
      resolvedState.isDiscovered ? 'is-discovered' : '',
      resolvedState.hasEvent ? 'has-event' : '',
      resolvedState.isEventFocus ? 'is-event-focus' : '',
    ].filter(Boolean).join(' ');

    const shortName = escapeHtml(resolveSpotShortName(spot));
    const emojiHtml = resolveSpotEmoji(spot);
    const pulse = resolvedState.isEventFocus ? '<b class="spot-pulse"></b>' : '';
    const eventBadge = resolvedState.hasEvent ? '<i>!</i>' : '';
    const discoveredBadge = resolvedState.isDiscovered && !resolvedState.hasEvent ? '<i>✓</i>' : '';

    return `<div class="${classNames}">${pulse}<span>${emojiHtml}</span><strong>${shortName}</strong>${eventBadge || discoveredBadge}</div>`;
  }

  function getSpotsList() {
    return getSpots?.() || [];
  }

  function findSpotById(spotId) {
    const target = toIdKey(spotId);
    if (!target) return null;
    return getSpotsList().find((item) => toIdKey(item.id) === target) || null;
  }

  function getSpotLngLat(spotId) {
    const spot = findSpotById(spotId);
    if (!spot) return null;
    const lngLat = resolveSpotLngLat(spot);
    if (!lngLat) return null;
    return [Number(lngLat[0]), Number(lngLat[1])];
  }

  function getSpotBaseCoord(spot) {
    const lng = Number(spot?.lng);
    const lat = Number(spot?.lat);
    if (!Number.isFinite(lng) || !Number.isFinite(lat)) return null;
    return [lng, lat];
  }

  function resolveSpotLngLat(spot) {
    const spotId = toIdKey(spot?.id);
    if (spotId && mapState.resolvedSpotCoords.has(spotId)) {
      return mapState.resolvedSpotCoords.get(spotId);
    }
    return getSpotBaseCoord(spot);
  }

  function resolvePlaceSearchService() {
    if (!(window.AMap && window.AMap.PlaceSearch)) return null;
    if (!mapState.placeSearchService) {
      mapState.placeSearchService = new window.AMap.PlaceSearch({
        citylimit: false,
        pageSize: 6,
        extensions: 'base',
      });
    }
    return mapState.placeSearchService;
  }

  function extractPoiLngLat(poi) {
    const lnglat = normalizeLngLat(poi?.location);
    if (lnglat) return lnglat;
    if (typeof poi?.location === 'string' && poi.location.includes(',')) {
      const parts = poi.location.split(',');
      const lng = Number(parts[0]);
      const lat = Number(parts[1]);
      if (Number.isFinite(lng) && Number.isFinite(lat)) {
        return [lng, lat];
      }
    }
    return null;
  }

  function searchSpotPoiCandidates(spot) {
    return new Promise((resolve) => {
      const placeSearch = resolvePlaceSearchService();
      if (!placeSearch || !spot?.name) {
        resolve([]);
        return;
      }
      const keyword = `九寨沟 ${String(spot.name).trim()}`;
      placeSearch.search(keyword, (status, result) => {
        if (String(status).toLowerCase() !== 'complete') {
          resolve([]);
          return;
        }
        const pois = Array.isArray(result?.poiList?.pois) ? result.poiList.pois : [];
        resolve(pois);
      });
    });
  }

  function pickBestPoiCoordinateForSpot(spot, pois) {
    if (!Array.isArray(pois) || !pois.length) return null;
    const original = getSpotBaseCoord(spot);
    const normalizedName = String(spot?.name || '').trim();
    const normalizedNameCompact = normalizedName.replace(/\s+/g, '');

    const candidates = pois
      .map((poi) => {
        const lnglat = extractPoiLngLat(poi);
        if (!lnglat) return null;
        const poiName = String(poi?.name || '').trim();
        const poiNameCompact = poiName.replace(/\s+/g, '');
        const district = String(poi?.district || '');
        const address = String(poi?.address || '');
        const scenicHint = `${district} ${address} ${poiName}`;
        const exactNameMatch = normalizedNameCompact
          && poiNameCompact
          && poiNameCompact === normalizedNameCompact;
        const fuzzyNameMatch = !exactNameMatch
          && normalizedNameCompact
          && poiNameCompact
          && (poiNameCompact.includes(normalizedNameCompact) || normalizedNameCompact.includes(poiNameCompact));
        const scoreName = exactNameMatch ? 16 : (fuzzyNameMatch ? 9 : 0);
        const scoreScenic = /九寨|沟/.test(scenicHint) ? 3 : 0;
        const distance = original ? distanceMeters(original, lnglat) : 0;
        if (!scoreName) return null;
        if (original && Number.isFinite(distance) && distance > POI_ALIGN_MAX_DISTANCE_METERS) {
          return null;
        }
        return {
          lnglat,
          score: scoreName + scoreScenic,
          distance,
        };
      })
      .filter(Boolean);

    if (!candidates.length) return null;
    candidates.sort((a, b) => {
      if (b.score !== a.score) return b.score - a.score;
      return a.distance - b.distance;
    });
    return candidates[0].lnglat;
  }

  async function resolveSpotCoordinatesByPoi() {
    if (mapState.isResolvingSpotCoords) return;
    mapState.isResolvingSpotCoords = true;
    try {
      const spots = getSpotsList();
      let hasResolved = false;
      for (const spot of spots) {
        const spotId = toIdKey(spot?.id);
        if (!spotId || mapState.resolvedSpotCoords.has(spotId)) {
          continue;
        }
        if (POI_ALIGN_SKIP_SPOT_IDS.has(spotId)) {
          continue;
        }
        const poiCandidates = await searchSpotPoiCandidates(spot);
        const lnglat = pickBestPoiCoordinateForSpot(spot, poiCandidates);
        if (!lnglat) continue;
        mapState.resolvedSpotCoords.set(spotId, lnglat);
        const marker = mapState.spotMarkers.get(spotId);
        marker?.setPosition(lnglat);
        hasResolved = true;
      }
      if (hasResolved && mapState.map) {
        fitOverview();
      }
      updateNearestSpot();
      triggerSpotChecks();
      syncMarkerStates({ immediate: true });
    } finally {
      mapState.isResolvingSpotCoords = false;
    }
  }

  function stateSignature(state) {
    return [
      state.isCurrent ? '1' : '0',
      state.isDiscovered ? '1' : '0',
      state.hasEvent ? '1' : '0',
      state.isEventFocus ? '1' : '0',
    ].join('');
  }

  function updateCurrentSpot(spotId) {
    const next = toIdKey(spotId);
    if (!next || mapState.lastCurrentSpotId === next) return;
    mapState.lastCurrentSpotId = next;
    onCurrentSpotChange?.(spotId);
  }

  function flushMarkerStatesSync() {
    if (!mapState.spotMarkers.size) return;
    const context = buildMarkerStateContext();
    const discoveredKey = Array.from(context.discoveredSet).sort().join(',');
    const pendingKey = Array.from(context.pendingEventSet).sort().join(',');
    const fingerprint = [
      context.currentSpotId,
      context.nearestEventSpotId,
      discoveredKey,
      pendingKey,
    ].join('|');

    if (mapState.markerStateFingerprint === fingerprint) return;
    mapState.markerStateFingerprint = fingerprint;

    mapState.spotMarkers.forEach((marker, spotId) => {
      const spot = findSpotById(spotId);
      if (!spot) return;
      const state = getSpotState(spot, context);
      const nextSignature = stateSignature(state);
      const prevSignature = mapState.markerStateBySpotId.get(spotId);
      if (prevSignature === nextSignature) return;
      mapState.markerStateBySpotId.set(spotId, nextSignature);
      marker.setContent(createMarkerContent(spot, context, state));
    });
  }

  function cancelQueuedMarkerStatesSync() {
    if (mapState.markerStateSyncRafId == null) return;
    if (typeof window !== 'undefined' && typeof window.cancelAnimationFrame === 'function') {
      window.cancelAnimationFrame(mapState.markerStateSyncRafId);
    }
    mapState.markerStateSyncRafId = null;
  }

  function syncMarkerStates(options = {}) {
    const immediate = options?.immediate === true;
    if (immediate) {
      cancelQueuedMarkerStatesSync();
      flushMarkerStatesSync();
      return;
    }
    if (typeof window === 'undefined' || typeof window.requestAnimationFrame !== 'function') {
      flushMarkerStatesSync();
      return;
    }
    if (mapState.markerStateSyncRafId != null) return;
    mapState.markerStateSyncRafId = window.requestAnimationFrame(() => {
      mapState.markerStateSyncRafId = null;
      flushMarkerStatesSync();
    });
  }

  function updateNearestSpot() {
    if (!mapState.explorerMarker) {
      return;
    }
    const position = mapState.explorerMarker.getPosition();
    if (!position) {
      return;
    }
    const explorerLng = Number(position.lng);
    const explorerLat = Number(position.lat);
    if (!Number.isFinite(explorerLng) || !Number.isFinite(explorerLat)) return;
    const explorerPoint = [explorerLng, explorerLat];

    let nearest = null;
    let minDistance = Number.POSITIVE_INFINITY;
    getSpotsList().forEach((spot) => {
      const spotLngLat = resolveSpotLngLat(spot);
      if (!spotLngLat) return;
      const distance = distanceMeters(explorerPoint, spotLngLat);
      if (distance < minDistance) {
        minDistance = distance;
        nearest = spot;
      }
    });

    if (nearest) updateCurrentSpot(nearest.id);
  }

  function triggerSpotChecks() {
    if (!mapState.explorerMarker) {
      return;
    }
    const position = mapState.explorerMarker.getPosition();
    if (!position) {
      return;
    }
    const explorerLng = Number(position.lng);
    const explorerLat = Number(position.lat);
    if (!Number.isFinite(explorerLng) || !Number.isFinite(explorerLat)) return;
    const explorerPoint = [explorerLng, explorerLat];

    const now = Date.now();
    let nearest = null;
    let minDistance = Number.POSITIVE_INFINITY;

    getSpotsList().forEach((spot) => {
      const spotLngLat = resolveSpotLngLat(spot);
      if (!spotLngLat) return;
      const distance = distanceMeters(explorerPoint, spotLngLat);
      if (distance < minDistance) {
        minDistance = distance;
        nearest = spot;
      }
      const triggerRadius = Number(spot.radius);
      const safeRadius = Number.isFinite(triggerRadius) && triggerRadius > 0
        ? Math.min(
          Math.max(triggerRadius * SPOT_TRIGGER_RADIUS_SCALE, MIN_SPOT_TRIGGER_RADIUS_METERS),
          MAX_SPOT_TRIGGER_RADIUS_METERS,
        )
        : MIN_SPOT_TRIGGER_RADIUS_METERS;
      if (distance <= safeRadius) {
        const spotIdKey = toIdKey(spot.id);
        const nextTime = mapState.cooldownUntil.get(spotIdKey) || 0;
        if (now >= nextTime) {
          mapState.cooldownUntil.set(spotIdKey, now + triggerCooldown);
          onSpotDiscovered?.(spot);
        }
      }
    });

    if (nearest) updateCurrentSpot(nearest.id);
  }

  function jumpExplorerTo(lnglat, options = {}) {
    if (!mapState.explorerMarker) {
      return;
    }
    const {
      pan = false,
      save = false,
      check = true,
      clearRouteOnMove = false,
    } = options;
    const raw = Array.isArray(lnglat) ? [Number(lnglat[0]), Number(lnglat[1])] : [Number(lnglat.lng), Number(lnglat.lat)];
    const next = clampLngLatToLimitBounds(raw);
    mapState.explorerMarker.setPosition(next);
    if (clearRouteOnMove) {
      clearPlanOverlays({ invalidateToken: false });
    } else if (mapState.activeRouteFollowExplorer) {
      updateActiveRouteProgress();
    }
    if (pan && mapState.map) {
      mapState.map.panTo(next);
    }
    if (save) {
      onPositionSave?.(next);
    }
    if (check) {
      triggerSpotChecks();
    } else {
      updateNearestSpot();
    }
    syncMarkerStates();
  }

  function openSpotInfo(spot) {
    if (!spot || !mapState.infoWindow || !mapState.map) {
      return;
    }
    const targetPosition = resolveSpotLngLat(spot);
    if (!targetPosition) {
      return;
    }
    const safeName = escapeHtml(spot.name);
    const safeSummary = escapeHtml(spot.summary || '');
    const spotId = escapeHtml(spot.id);
    mapState.infoWindow.setContent(`
      <div style="padding:8px 10px;min-width:220px;">
        <strong style="display:block;font-size:16px;margin-bottom:6px;">${safeName}</strong>
        <div style="font-size:13px;line-height:1.8;color:#44564f;">${safeSummary}</div>
        <button
          type="button"
          class="explore-info-nav-btn"
          data-spot-id="${spotId}"
          style="margin-top:10px;padding:6px 12px;border:0;border-radius:999px;background:#2f7f63;color:#fff;font-size:12px;cursor:pointer;"
        >
          导航到这里
        </button>
      </div>
    `);
    mapState.infoWindow.open(mapState.map, targetPosition);
    window.setTimeout(() => {
      const container = mapState.map?.getContainer?.() || mapRef.value;
      if (!container) return;
      const button = container.querySelector('.explore-info-nav-btn');
      if (!button || button.dataset.bound === '1') return;
      button.dataset.bound = '1';
      button.addEventListener('click', (event) => {
        event.preventDefault();
        event.stopPropagation();
        onSpotNavigate?.(spot.id);
      });
    }, 0);
  }

  function focusSpot(spotId, options = {}) {
    const {
      pan = true,
      openInfo = true,
      moveExplorer = false,
      triggerDiscover = true,
      adjustZoom = true,
    } = options;
    const spot = findSpotById(spotId);
    if (!spot) {
      return;
    }
    const targetPosition = resolveSpotLngLat(spot);
    if (!targetPosition) {
      return;
    }
    onCurrentSpotChange?.(spot.id);
    if (pan && mapState.map) {
      if (adjustZoom && typeof mapState.map.setZoomAndCenter === 'function') {
        const explorer = getExplorerPosition();
        const distance = explorer ? distanceMeters(explorer, targetPosition) : 0;
        mapState.map.setZoomAndCenter(resolveAdaptiveZoom(distance), targetPosition);
      } else {
        mapState.map.panTo(targetPosition);
      }
    }
    if (moveExplorer) {
      jumpExplorerTo(targetPosition, {
        pan: false,
        save: true,
        check: triggerDiscover,
      });
    }
    if (openInfo) {
      openSpotInfo(spot);
    }
  }

  function fitOverview() {
    if (!mapState.map) {
      return;
    }
    const overlays = [mapState.explorerMarker, ...Array.from(mapState.spotMarkers.values())].filter(Boolean);
    mapState.map.setFitView(overlays, false, getMapFitPadding());
  }

  function updateActiveRouteProgress() {
    if (!mapState.activeRouteFollowExplorer) return;

    const explorer = getExplorerPosition();
    if (!explorer) return;

    if (mapState.activeRouteMode === 'segment' && mapState.activeRouteStops.length) {
      const currentIndex = Math.max(0, Math.min(mapState.activeRouteTargetStopIndex, mapState.activeRouteStops.length - 1));
      const currentStop = mapState.activeRouteStops[currentIndex];
      if (currentStop) {
        const targetDistance = distanceMeters(explorer, [currentStop.lng, currentStop.lat]);
        if (Number.isFinite(targetDistance) && targetDistance <= ROUTE_DESTINATION_REACHED_METERS) {
          if (currentIndex < mapState.activeRouteStops.length - 1) {
            mapState.activeRouteTargetStopIndex = currentIndex + 1;
            renderRoutePlan({
              stops: mapState.activeRouteStops,
              followExplorer: true,
              showStopCircles: true,
              displayMode: 'segment',
              arrivalRadius: ROUTE_SEGMENT_ARRIVAL_RADIUS_METERS,
            });
          } else {
            clearPlanOverlays({ invalidateToken: false });
          }
          return;
        }
      }
      // Segment mode keeps a single destination leg; avoid path trimming here
      // to prevent accidental short clipping when the nearest-point heuristic misfires.
      return;
    }

    if (!mapState.activePolyline || typeof mapState.activePolyline.setPath !== 'function') return;
    if (!Array.isArray(mapState.activeRouteFullPath) || mapState.activeRouteFullPath.length < 2) return;

    const fullPath = mapState.activeRouteFullPath;
    const searchStart = Math.max(0, Math.min(mapState.activeRouteProgressIndex, fullPath.length - 1));
    let nearestIndex = searchStart;
    let minDistance = Number.POSITIVE_INFINITY;

    for (let index = searchStart; index < fullPath.length; index += 1) {
      const point = fullPath[index];
      const distance = distanceMeters(explorer, point);
      if (distance < minDistance) {
        minDistance = distance;
        nearestIndex = index;
      }
    }

    mapState.activeRouteProgressIndex = Math.max(mapState.activeRouteProgressIndex, nearestIndex);
    const destination = fullPath[fullPath.length - 1];
    const destinationDistance = distanceMeters(explorer, destination);
    const nearRouteTail = mapState.activeRouteProgressIndex >= Math.max(0, fullPath.length - 2);
    if (nearRouteTail && Number.isFinite(destinationDistance) && destinationDistance <= 12) {
      clearPlanOverlays({ invalidateToken: false });
      return;
    }

    const tailStart = Math.min(mapState.activeRouteProgressIndex + 1, fullPath.length - 1);
    const remainingTail = fullPath.slice(tailStart);
    const remainingPath = [explorer, ...remainingTail];
    if (remainingPath.length < 2) {
      clearPlanOverlays({ invalidateToken: false });
      return;
    }

    mapState.activePolyline.setPath(remainingPath);
  }

  function clearPlanOverlays({ invalidateToken = true } = {}) {
    if (invalidateToken) {
      mapState.routeRenderToken += 1;
    }
    mapState.activeRouteFullPath = [];
    mapState.activeRouteStops = [];
    mapState.activeRouteTargetStopIndex = -1;
    mapState.activeRouteMode = 'segment';
    mapState.activeRouteFollowExplorer = false;
    mapState.activeRouteProgressIndex = 0;
    if (!mapState.map) {
      mapState.activePolyline = null;
      mapState.activeCircles = [];
      return;
    }
    if (mapState.activePolyline) {
      mapState.map.remove(mapState.activePolyline);
      mapState.activePolyline = null;
    }
    if (mapState.activeCircles.length) {
      mapState.map.remove(mapState.activeCircles);
      mapState.activeCircles = [];
    }
  }
  function clearBaseLayerCheckTimer() {
    if (mapState.baseLayerCheckTimer) {
      window.clearTimeout(mapState.baseLayerCheckTimer);
      mapState.baseLayerCheckTimer = null;
    }
  }

  function hasRenderableBaseLayer() {
    if (!mapRef.value) return false;
    const canvases = Array.from(mapRef.value.querySelectorAll('.amap-layer canvas'));
    const images = Array.from(mapRef.value.querySelectorAll('.amap-layer img'));
    const canvasReady = canvases.some((item) => Number(item?.width) > 0 && Number(item?.height) > 0);
    const imageReady = images.some((item) => Boolean(item?.complete) && Number(item?.naturalWidth) > 0);
    return canvasReady || imageReady;
  }
  function normalizeLngLat(value) {
    if (Array.isArray(value) && value.length >= 2) {
      const lng = Number(value[0]);
      const lat = Number(value[1]);
      if (Number.isFinite(lng) && Number.isFinite(lat)) {
        return [lng, lat];
      }
      return null;
    }

    if (value && typeof value === 'object') {
      const lngGetter = typeof value.getLng === 'function' ? Number(value.getLng()) : Number(value.lng);
      const latGetter = typeof value.getLat === 'function' ? Number(value.getLat()) : Number(value.lat);
      if (Number.isFinite(lngGetter) && Number.isFinite(latGetter)) {
        return [lngGetter, latGetter];
      }
    }

    return null;
  }

  function appendUniquePoint(path, point) {
    const normalized = normalizeLngLat(point);
    if (!normalized) return;
    const last = path[path.length - 1];
    if (last && Math.abs(last[0] - normalized[0]) < 1e-7 && Math.abs(last[1] - normalized[1]) < 1e-7) {
      return;
    }
    path.push(normalized);
  }

  function resolveWalkingService() {
    if (!(window.AMap && window.AMap.Walking)) return null;
    if (!mapState.walkingService) {
      mapState.walkingService = new window.AMap.Walking({
        hideMarkers: true,
        autoFitView: false,
      });
    }
    return mapState.walkingService;
  }

  function extractRoutePathFromWalkingResult(result) {
    const route = Array.isArray(result?.routes) ? result.routes[0] : null;
    if (!route) return [];

    const output = [];
    if (Array.isArray(route.steps)) {
      route.steps.forEach((step) => {
        const points = Array.isArray(step?.path) ? step.path : [];
        points.forEach((point) => appendUniquePoint(output, point));
      });
    }

    if (!output.length && Array.isArray(route.path)) {
      route.path.forEach((point) => appendUniquePoint(output, point));
    }

    return output;
  }

  function searchWalkingSegment(start, end) {
    return new Promise((resolve, reject) => {
      const walking = resolveWalkingService();
      if (!walking) {
        reject(new Error('AMap.Walking unavailable'));
        return;
      }

      let settled = false;
      const timer = window.setTimeout(() => {
        if (settled) return;
        settled = true;
        reject(new Error('Walking route timeout'));
      }, 12000);

      walking.search(start, end, (status, result) => {
        if (settled) return;
        settled = true;
        window.clearTimeout(timer);
        if (String(status).toLowerCase() === 'complete') {
          resolve(extractRoutePathFromWalkingResult(result));
          return;
        }
        reject(new Error(`Walking route failed: ${status || 'unknown'}`));
      });
    });
  }

  async function buildWalkingPathFromStops(stops, renderToken) {
    const output = [];
    for (let index = 0; index < stops.length - 1; index += 1) {
      if (renderToken !== mapState.routeRenderToken) return [];

      const from = normalizeLngLat([stops[index].lng, stops[index].lat]);
      const to = normalizeLngLat([stops[index + 1].lng, stops[index + 1].lat]);
      if (!from || !to) continue;

      try {
        const segmentPath = await searchWalkingSegment(from, to);
        if (segmentPath.length) {
          segmentPath.forEach((point) => appendUniquePoint(output, point));
        } else {
          appendUniquePoint(output, from);
          appendUniquePoint(output, to);
        }
      } catch {
        appendUniquePoint(output, from);
        appendUniquePoint(output, to);
      }
    }
    return output;
  }

  function resolveRouteMode(plan) {
    const raw = String(plan?.displayMode || '').trim().toLowerCase();
    return raw === 'full' ? 'full' : 'segment';
  }

  function resolveRouteArrivalRadius(plan) {
    const raw = Number(plan?.arrivalRadius);
    if (Number.isFinite(raw) && raw > 0) {
      return Math.min(Math.max(raw, 24), 280);
    }
    return ROUTE_SEGMENT_ARRIVAL_RADIUS_METERS;
  }

  function resolveNextRouteStopIndex(stops, explorer, arrivalRadius, minIndex = 0) {
    if (!Array.isArray(stops) || !stops.length) return -1;
    const lowerBound = Math.max(0, Math.min(minIndex, stops.length - 1));
    if (!explorer) return lowerBound;

    for (let index = lowerBound; index < stops.length; index += 1) {
      const stop = stops[index];
      const distance = distanceMeters(explorer, [stop.lng, stop.lat]);
      if (!Number.isFinite(distance) || distance > arrivalRadius) {
        return index;
      }
    }

    return stops.length - 1;
  }

  function buildFallbackPath(start, end) {
    const from = normalizeLngLat(start);
    const to = normalizeLngLat(end);
    if (!from || !to) return [];
    if (Math.abs(from[0] - to[0]) < 1e-7 && Math.abs(from[1] - to[1]) < 1e-7) {
      return [from];
    }
    return [from, to];
  }

  async function buildSegmentWalkingPath(start, end, renderToken) {
    const from = normalizeLngLat(start);
    const to = normalizeLngLat(end);
    if (!from || !to) return [];

    try {
      const segmentPath = await searchWalkingSegment(from, to);
      if (renderToken !== mapState.routeRenderToken) return [];
      if (Array.isArray(segmentPath) && segmentPath.length >= 2) {
        return segmentPath;
      }
    } catch {
      // Fallback below keeps navigation usable even when route API is unstable.
    }

    return buildFallbackPath(from, to);
  }

  async function renderRoutePlan(plan) {
    const renderToken = mapState.routeRenderToken + 1;
    mapState.routeRenderToken = renderToken;

    if (!plan) {
      clearPlanOverlays({ invalidateToken: false });
      return;
    }
    if (!mapState.map || !(window.AMap && window.AMap.Polyline && window.AMap.Circle)) {
      return;
    }

    const previousTargetStopIndex = Math.max(0, mapState.activeRouteTargetStopIndex);
    const stops = Array.isArray(plan.stops)
      ? plan.stops
        .map((item) => {
          const resolved = getSpotLngLat(item?.id);
          return {
            id: item?.id,
            lng: Number.isFinite(Number(resolved?.[0])) ? Number(resolved[0]) : Number(item?.lng),
            lat: Number.isFinite(Number(resolved?.[1])) ? Number(resolved[1]) : Number(item?.lat),
          };
        })
        .filter((item) => item.id && Number.isFinite(item.lng) && Number.isFinite(item.lat))
      : [];

    clearPlanOverlays({ invalidateToken: false });
    if (!stops.length) return;

    const routeMode = resolveRouteMode(plan);
    const arrivalRadius = resolveRouteArrivalRadius(plan);
    const explorer = getExplorerPosition();
    let routePath = [];
    let targetStopIndex = 0;

    if (routeMode === 'full') {
      const fallbackPath = stops.map((item) => [item.lng, item.lat]);
      routePath = fallbackPath;
      if (stops.length >= 2 && window.AMap && window.AMap.Walking) {
        const walkingPath = await buildWalkingPathFromStops(stops, renderToken);
        if (renderToken !== mapState.routeRenderToken) return;
        if (walkingPath.length >= 2) {
          routePath = walkingPath;
        }
      }
      targetStopIndex = resolveNextRouteStopIndex(stops, explorer, arrivalRadius);
    } else {
      const minIndex = previousTargetStopIndex > 0 ? previousTargetStopIndex : 0;
      targetStopIndex = resolveNextRouteStopIndex(stops, explorer, arrivalRadius, minIndex);
      if (!explorer && targetStopIndex === 0 && stops.length > 1) {
        // When explorer position is temporarily unavailable, prefer first real destination
        // instead of the synthetic "current-position" stop.
        targetStopIndex = 1;
      }
      const targetStop = stops[targetStopIndex] || stops[stops.length - 1];
      const routeStart = explorer
        || (targetStopIndex > 0
          ? [stops[targetStopIndex - 1].lng, stops[targetStopIndex - 1].lat]
          : [targetStop.lng, targetStop.lat]);
      const routeEnd = [targetStop.lng, targetStop.lat];
      routePath = await buildSegmentWalkingPath(routeStart, routeEnd, renderToken);
      if (renderToken !== mapState.routeRenderToken) return;
    }

    if (routePath.length >= 2) {
      mapState.activePolyline = new window.AMap.Polyline({
        path: routePath,
        strokeColor: routeMode === 'segment' ? '#36cfa7' : '#58d2ad',
        strokeWeight: routeMode === 'segment' ? 7 : 6,
        strokeOpacity: routeMode === 'segment' ? 0.95 : 0.88,
        lineJoin: 'round',
        lineCap: 'round',
        showDir: false,
      });
    } else {
      mapState.activePolyline = null;
    }

    mapState.activeRouteMode = routeMode;
    mapState.activeRouteStops = stops;
    mapState.activeRouteTargetStopIndex = targetStopIndex;
    mapState.activeRouteFullPath = routePath.length >= 2
      ? routePath.map((item) => [Number(item[0]), Number(item[1])])
      : [];
    mapState.activeRouteFollowExplorer = plan?.followExplorer === true || routeMode === 'segment';
    mapState.activeRouteProgressIndex = 0;

    const showStopCircles = plan?.showStopCircles !== false;
    if (showStopCircles) {
      if (routeMode === 'segment') {
        const focusStop = stops[targetStopIndex] || stops[stops.length - 1];
        mapState.activeCircles = focusStop
          ? [new window.AMap.Circle({
            center: [focusStop.lng, focusStop.lat],
            radius: 68,
            strokeWeight: 1,
            strokeColor: 'rgba(52,180,143,0.6)',
            fillColor: 'rgba(58,214,170,0.16)',
            fillOpacity: 1,
          })]
          : [];
      } else {
        mapState.activeCircles = stops.map((item) => new window.AMap.Circle({
          center: [item.lng, item.lat],
          radius: 60,
          strokeWeight: 1,
          strokeColor: 'rgba(88,210,173,0.5)',
          fillColor: 'rgba(88,210,173,0.12)',
          fillOpacity: 1,
        }));
      }
    } else {
      mapState.activeCircles = [];
    }

    const overlays = [mapState.explorerMarker, ...mapState.activeCircles].filter(Boolean);
    if (mapState.activePolyline) {
      overlays.push(mapState.activePolyline);
    }
    if (overlays.length) {
      mapState.map.add(overlays);
      mapState.map.setFitView(overlays, false, getMapFitPadding());
      const routeDistance = estimatePathDistanceMeters(routePath);
      stabilizeMapZoomByDistance(routeDistance);
    }
    if (mapState.activeRouteFollowExplorer) {
      updateActiveRouteProgress();
    }
  }

  async function initMap() {
    if (!mapRef.value) {
      throw new Error('Map container initialization failed.');
    }

    try {
      await ensureAMapScriptLoaded();
    } catch (error) {
      const message = error?.message || 'AMap load failed.';
      onMapError?.(message);
      throw error;
    }
    if (!window.AMap) {
      throw new Error('AMap load failed.');
    }

    const mapConfig = getMapConfig?.() || {};
    const center = getLastPosition?.() || [Number(mapConfig.lng), Number(mapConfig.lat)];
    const zoom = Number.isFinite(Number(mapConfig.zoom)) ? Number(mapConfig.zoom) : 13.8;
    const configuredStyle = typeof mapConfig.style === 'string' ? mapConfig.style.trim() : '';
    const limitBounds = resolveLimitBounds(mapConfig);
    mapState.limitBounds = limitBounds;
    const mapOptions = {
      zoom,
      center,
      resizeEnable: true,
      showIndoorMap: false,
      mapStyle: configuredStyle || 'amap://styles/fresh',
    };
    mapState.map = new window.AMap.Map(mapRef.value, mapOptions);
    if (limitBounds && typeof window.AMap?.Bounds === 'function') {
      const bounds = new window.AMap.Bounds(limitBounds.sw, limitBounds.ne);
      if (typeof mapState.map.setLimitBounds === 'function') {
        mapState.map.setLimitBounds(bounds);
      }
    }
    mapState.map.addControl(new window.AMap.Scale());
    mapState.map.addControl(new window.AMap.ToolBar({ position: 'RB' }));
    mapState.infoWindow = new window.AMap.InfoWindow({ offset: new window.AMap.Pixel(0, -24) });
    mapState.explorerMarker = new window.AMap.Marker({
      position: center,
      anchor: 'bottom-center',
      offset: new window.AMap.Pixel(0, -6),
      content: "<div class='explorer-pin'></div>",
    });
    mapState.map.add(mapState.explorerMarker);

    const spots = getSpotsList();

    spots.forEach((spot) => {
      const targetPosition = resolveSpotLngLat(spot);
      if (!targetPosition) return;
      const marker = new window.AMap.Marker({
        position: targetPosition,
        offset: new window.AMap.Pixel(-18, -34),
        content: createMarkerContent(spot),
      });
      marker.on('click', () => {
        focusSpot(spot.id, { pan: true, openInfo: true, moveExplorer: false });
        onSpotPanelOpen?.(spot.id);
      });
      mapState.spotMarkers.set(toIdKey(spot.id), marker);
      mapState.map.add(marker);
    });
    syncMarkerStates({ immediate: true });
    resolveSpotCoordinatesByPoi().catch(() => null);

    mapState.map.on('click', (event) => {
      jumpExplorerTo(event.lnglat, {
        pan: true,
        save: true,
        check: true,
      });
    });
    mapState.map.on('complete', () => {
      clearBaseLayerCheckTimer();
    });
    mapState.map.on('tileloaded', () => {
      clearBaseLayerCheckTimer();
    });
    mapState.map.on('tilesloaded', () => {
      clearBaseLayerCheckTimer();
    });
    mapState.map.on('error', (event) => {
      const message = String(event?.info || event?.message || 'Map load failed').trim();
      onMapError?.(message);
      if (typeof console !== 'undefined') {
        console.error('[AMap] map error:', event);
      }
    });
    clearBaseLayerCheckTimer();
    mapState.baseLayerCheckTimer = window.setTimeout(() => {
      if (hasRenderableBaseLayer()) return;
      const message = 'AMap base tiles did not render. Possible causes: key/domain/security mismatch or tile-domain network block.';
      onMapError?.(message);
      if (typeof console !== 'undefined') {
        console.error('[AMap] base layer not rendered after timeout', {
          href: typeof window !== 'undefined' ? window.location.href : '',
          keyPreview: AMAP_KEY ? `${AMAP_KEY.slice(0, 6)}...` : 'missing',
        });
      }
    }, 7000);

    // Layout can still settle after mount; force several resize passes so tiles render reliably.
    if (typeof window !== 'undefined' && typeof window.requestAnimationFrame === 'function') {
      window.requestAnimationFrame(() => {
        mapState.map?.resize();
        fitOverview();
      });
      window.setTimeout(() => {
        mapState.map?.resize();
      }, 120);
      window.setTimeout(() => {
        mapState.map?.resize();
      }, 360);
    } else {
      mapState.map.resize();
      fitOverview();
    }

    fitOverview();
    updateNearestSpot();
    triggerSpotChecks();
  }

  function destroyMap() {
    clearBaseLayerCheckTimer();
    clearPlanOverlays();
    cancelQueuedMarkerStatesSync();
    if (mapState.map && typeof mapState.map.destroy === 'function') {
      mapState.map.destroy();
    }
    mapState.map = null;
    mapState.infoWindow = null;
    mapState.explorerMarker = null;
    mapState.walkingService = null;
    mapState.placeSearchService = null;
    mapState.spotMarkers.clear();
    mapState.resolvedSpotCoords.clear();
    mapState.isResolvingSpotCoords = false;
    mapState.cooldownUntil.clear();
    mapState.markerStateFingerprint = '';
    mapState.markerStateBySpotId.clear();
    mapState.markerStateSyncRafId = null;
    mapState.lastCurrentSpotId = '';
    mapState.limitBounds = null;
    mapState.routeRenderToken = 0;
  }

  return {
    mapRef,
    initMap,
    destroyMap,
    getSpotLngLat,
    getExplorerPosition,
    focusSpot,
    jumpExplorerTo,
    fitOverview,
    clearPlanOverlays,
    renderRoutePlan,
    syncMarkerStates,
  };
}



