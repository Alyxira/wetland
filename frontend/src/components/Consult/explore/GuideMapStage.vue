<template>
  <div ref="mapRef" class="explore-map"></div>
</template>

<script setup>
import { onBeforeUnmount, watch } from 'vue';
import { useExploreMap } from '../../../explore/useExploreMap.js';

const props = defineProps({
  mapConfig: {
    type: Object,
    required: true,
  },
  spots: {
    type: Array,
    default: () => [],
  },
  lastPosition: {
    type: Array,
    default: null,
  },
  routePlan: {
    type: Object,
    default: null,
  },
  currentSpotId: {
    type: [String, Number],
    default: '',
  },
  discoveredSpotIds: {
    type: Array,
    default: () => [],
  },
  pendingEventSpotIds: {
    type: Array,
    default: () => [],
  },
  triggerCooldown: {
    type: Number,
    default: 12000,
  },
});

const emit = defineEmits(['current-spot-change', 'spot-discover', 'position-save', 'spot-panel-open', 'spot-navigate', 'map-error']);

function normalizeId(value) {
  return String(value ?? '').trim();
}

function listSignature(list) {
  if (!Array.isArray(list) || !list.length) return '';
  return list
    .map((item) => normalizeId(item))
    .filter(Boolean)
    .sort()
    .join(',');
}

function spotsSignature(spots) {
  if (!Array.isArray(spots) || !spots.length) return '';
  return spots
    .map((spot) => {
      const id = normalizeId(spot?.id);
      if (!id) return '';
      const lng = Number(spot?.lng);
      const lat = Number(spot?.lat);
      return `${id}:${lng}:${lat}`;
    })
    .filter(Boolean)
    .join('|');
}

const {
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
} = useExploreMap({
  getMapConfig: () => props.mapConfig,
  getSpots: () => props.spots,
  getLastPosition: () => props.lastPosition,
  getCurrentSpotId: () => props.currentSpotId,
  getDiscoveredSpotIds: () => props.discoveredSpotIds,
  getPendingEventSpotIds: () => props.pendingEventSpotIds,
  onCurrentSpotChange: (spotId) => emit('current-spot-change', spotId),
  onSpotDiscovered: (spot) => emit('spot-discover', spot),
  onPositionSave: (position) => emit('position-save', position),
  onSpotPanelOpen: (spotId) => emit('spot-panel-open', spotId),
  onSpotNavigate: (spotId) => emit('spot-navigate', spotId),
  onMapError: (message) => emit('map-error', message),
  triggerCooldown: props.triggerCooldown,
});

watch(
  () => props.routePlan,
  (plan) => {
    renderRoutePlan(plan);
  },
  { deep: true },
);

watch(
  () => [
    normalizeId(props.currentSpotId),
    listSignature(props.discoveredSpotIds),
    listSignature(props.pendingEventSpotIds),
    spotsSignature(props.spots),
  ],
  () => {
    syncMarkerStates();
  },
);

onBeforeUnmount(() => {
  destroyMap();
});

defineExpose({
  initMap,
  getSpotLngLat,
  getExplorerPosition,
  focusSpot,
  jumpExplorerTo,
  fitOverview,
  clearPlanOverlays,
  renderRoutePlan,
  syncMarkerStates,
});
</script>

<style scoped>
.explore-map {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

:deep(.explorer-pin) {
  position: relative;
  width: 34px;
  height: 34px;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  background: linear-gradient(135deg, rgba(117, 189, 155, 0.98), rgba(var(--brand-rgb), 0.96));
  border: 3px solid rgba(255, 251, 242, 0.98);
  box-shadow: 0 18px 34px rgba(47, 95, 75, 0.4);
}

:deep(.explorer-pin)::before {
  content: '';
  position: absolute;
  inset: -14px;
  border-radius: 50%;
  border: 2px solid rgba(74, 139, 111, 0.52);
  transform: rotate(45deg) scale(0.72);
  opacity: 0;
  animation: explorer-pulse 1.8s ease-out infinite;
}

:deep(.explorer-pin)::after {
  content: '';
  position: absolute;
  inset: 6px;
  border-radius: 50%;
  background: rgba(255, 252, 244, 0.94);
  box-shadow: inset 0 0 0 2px rgba(37, 72, 58, 0.24);
}

:deep(.spot-marker) {
  position: relative;
  width: 42px;
  height: 42px;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  border: 2px solid rgba(35, 69, 54, 0.66);
  background: linear-gradient(145deg, rgba(252, 250, 244, 0.98), rgba(235, 227, 208, 0.98));
  box-shadow: 0 12px 24px rgba(21, 26, 22, 0.28);
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.2s ease;
  cursor: pointer;
}

:deep(.spot-marker)::before {
  content: '';
  position: absolute;
  inset: -12px;
  border-radius: 50%;
  border: 2px solid rgba(89, 146, 120, 0.4);
  transform: rotate(45deg);
  opacity: 0;
}

:deep(.spot-marker:hover),
:deep(.spot-marker.is-current) {
  transform: rotate(-45deg) translateY(-4px) scale(1.12);
  box-shadow: 0 20px 34px rgba(21, 26, 22, 0.34);
}

:deep(.spot-marker.is-event-focus) {
  transform: rotate(-45deg) translateY(-6px) scale(1.15);
  box-shadow: 0 24px 38px rgba(21, 26, 22, 0.36);
}

:deep(.spot-marker.is-discovered) {
  border-color: rgba(211, 160, 62, 0.88);
  background: linear-gradient(145deg, rgba(255, 248, 222, 0.98), rgba(243, 223, 170, 0.94));
}

:deep(.spot-marker.has-event) {
  border-color: rgba(54, 115, 88, 0.94);
  background: linear-gradient(145deg, rgba(226, 245, 237, 0.98), rgba(184, 224, 206, 0.96));
}

:deep(.spot-marker.is-event-focus) {
  border-color: rgba(35, 108, 79, 1);
  background: linear-gradient(145deg, rgba(211, 246, 230, 0.99), rgba(150, 219, 188, 0.97));
}

:deep(.spot-marker.is-current) {
  border-color: rgba(41, 109, 82, 0.98);
  background: linear-gradient(145deg, rgba(222, 250, 236, 0.99), rgba(162, 224, 195, 0.97));
}

:deep(.spot-marker.is-event-focus)::before {
  animation: marker-ring 1.25s ease-out infinite;
}

:deep(.spot-marker .spot-pulse) {
  position: absolute;
  inset: -16px;
  border-radius: 50%;
  border: 2px solid rgba(56, 121, 92, 0.5);
  transform: rotate(45deg) scale(0.72);
  opacity: 0;
  animation: marker-ring 1.25s ease-out infinite;
  pointer-events: none;
}

:deep(.spot-marker.is-event-focus i) {
  animation: event-badge-pop 1.1s ease-in-out infinite;
}

:deep(.spot-marker span) {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  font-size: 18px;
  transform: rotate(45deg);
  line-height: 1;
}

:deep(.spot-marker strong) {
  position: absolute;
  left: 50%;
  top: 100%;
  margin-top: 8px;
  transform: translateX(-50%) rotate(45deg) scale(0.96);
  transform-origin: center;
  min-height: 24px;
  border-radius: 999px;
  padding: 0 9px;
  border: 1px solid rgba(35, 69, 54, 0.28);
  background: rgba(255, 253, 246, 0.97);
  color: #214236;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.22s ease, transform 0.24s ease;
}

:deep(.spot-marker:hover strong),
:deep(.spot-marker.is-current strong),
:deep(.spot-marker.is-discovered strong),
:deep(.spot-marker.is-event-focus strong) {
  opacity: 1;
  transform: translateX(-50%) rotate(45deg) scale(1);
}

:deep(.spot-marker i) {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  border: 1px solid rgba(255, 250, 240, 0.9);
  display: grid;
  place-items: center;
  transform: rotate(45deg);
  font-style: normal;
  font-size: 10px;
  font-weight: 700;
  color: #fffef8;
  background: linear-gradient(145deg, #2f5f4b, #3b7a60);
  box-shadow: 0 8px 14px rgba(47, 95, 75, 0.3);
}

:deep(.amap-info-content),
:deep(.amap-info-outer) {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(251, 244, 232, 0.9)),
    radial-gradient(circle at top right, rgba(var(--brand-soft-rgb), 0.2), transparent 52%) !important;
  color: var(--text-light) !important;
  border-radius: 16px !important;
  border: 1px solid rgba(51, 63, 56, 0.16) !important;
  box-shadow: 0 18px 36px rgba(35, 38, 31, 0.2) !important;
}

:deep(.amap-info-sharp) {
  display: none !important;
}

@keyframes explorer-pulse {
  0% {
    opacity: 0;
    transform: rotate(45deg) scale(0.72);
  }
  28% {
    opacity: 0.62;
  }
  100% {
    opacity: 0;
    transform: rotate(45deg) scale(1.46);
  }
}

@keyframes marker-ring {
  0% {
    opacity: 0;
    transform: rotate(45deg) scale(0.78);
  }
  26% {
    opacity: 0.56;
  }
  100% {
    opacity: 0;
    transform: rotate(45deg) scale(1.58);
  }
}

@keyframes event-badge-pop {
  0%,
  100% {
    transform: rotate(45deg) scale(1);
  }
  50% {
    transform: rotate(45deg) scale(1.18);
  }
}

@media (max-width: 920px) {
  .explore-map {
    position: relative;
    height: 56vh;
    min-height: 420px;
  }
}
</style>
