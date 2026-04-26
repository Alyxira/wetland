import { computed, reactive, ref } from 'vue';

const DEFAULT_PLAN_FORM = {
  duration: 'short',
  pace: 'relax',
  group: 'solo',
  interests: ['animal', 'plant'],
};

export function useExploreRoute({
  requestFn,
  apiPath,
  findSpotById,
  loadStoredPlan,
  saveStoredPlan,
  clearStoredPlan,
} = {}) {
  const routePlan = ref(null);
  const planForm = reactive({ ...DEFAULT_PLAN_FORM });

  const routePlanReasons = computed(() => routePlan.value?.reasons || []);
  const routePlanTags = computed(() => routePlan.value?.tags || []);
  const routePlanStops = computed(() => routePlan.value?.stops || []);

  function normalizeStoredPlan(raw) {
    if (!raw) return null;
    if (Array.isArray(raw.stops) && raw.stops.length) {
      return raw;
    }
    if (Array.isArray(raw.spotIds) && raw.spotIds.length) {
      const mappedStops = raw.spotIds
        .map((id) => findSpotById(id))
        .filter(Boolean)
        .map((spot) => ({ id: spot.id, name: spot.name, kind: spot.kind, lng: spot.lng, lat: spot.lat }));
      if (!mappedStops.length) return null;
      return {
        title: raw.title || '推荐路线',
        description: raw.desc || raw.description || '系统已为你保留上一条推荐路线。',
        reasons: Array.isArray(raw.reasons)
          ? raw.reasons.map((item) => (typeof item === 'string' ? item : item?.desc || item?.title || '推荐理由'))
          : [],
        tags: Array.isArray(raw.tags) ? raw.tags : [],
        stops: mappedStops,
      };
    }
    return null;
  }

  function setRoutePlan(plan, persist = true) {
    const normalized = normalizeStoredPlan(plan);
    if (!normalized) {
      return null;
    }
    routePlan.value = normalized;
    if (persist) {
      saveStoredPlan(normalized);
    }
    return normalized;
  }

  function clearRoutePlan() {
    routePlan.value = null;
    clearStoredPlan();
  }

  async function requestRoutePlan() {
    return requestFn(apiPath, {
      method: 'POST',
      data: {
        duration: planForm.duration,
        pace: planForm.pace,
        group: planForm.group,
        interests: planForm.interests,
      },
      headers: {
        'Content-Type': 'application/json',
      },
    });
  }

  function loadPersistedPlan() {
    return normalizeStoredPlan(loadStoredPlan());
  }

  return {
    routePlan,
    planForm,
    routePlanReasons,
    routePlanTags,
    routePlanStops,
    normalizeStoredPlan,
    setRoutePlan,
    clearRoutePlan,
    requestRoutePlan,
    loadPersistedPlan,
  };
}
