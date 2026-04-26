import { computed, reactive, watch } from 'vue';
import { DEFAULT_SCENIC_ID, buildScenicStorageKey, normalizeScenicId } from '../../utils/scenic';

function safeParseStorage(key) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : null;
  } catch (error) {
    return null;
  }
}

function normalizeText(value) {
  return String(value || '').trim();
}

function normalizeKey(value) {
  return normalizeText(value).toLowerCase().replace(/[^a-z0-9]/g, '');
}

function createMissionState(raw) {
  return {
    completedStepKeys: Array.from(new Set(Array.isArray(raw?.completedStepKeys) ? raw.completedStepKeys.filter(Boolean) : [])),
    trackedMissionId: normalizeText(raw?.trackedMissionId) || null,
  };
}

function buildStepKey(missionId, stepId) {
  return `${missionId}:${stepId}`;
}

function isVisitStep(stepType) {
  return ['visitspot', 'visit', 'reachspot', 'arrive', 'goto'].includes(normalizeKey(stepType));
}

function isDiscoverStep(stepType) {
  return ['discoverspot', 'discover', 'unlockreward', 'unlock'].includes(normalizeKey(stepType));
}

export function useExploreMissions({
  scenicId = DEFAULT_SCENIC_ID,
  missionsSource,
  findSpotById = () => null,
} = {}) {
  const resolvedScenicId = normalizeScenicId(scenicId);
  const storageKey = buildScenicStorageKey(resolvedScenicId, 'mission');
  const state = reactive(createMissionState(safeParseStorage(storageKey)));

  const missionEnabled = computed(() => Boolean(missionsSource?.value?.enabled));
  const missionTitle = computed(() => missionsSource?.value?.title || '探索任务');
  const missionSubtitle = computed(() => missionsSource?.value?.subtitle || '在探索地图中完成分步目标，逐步解锁景区巡游奖励。');
  const missionEmptyState = computed(() => missionsSource?.value?.emptyState || {
    title: '暂未开放任务',
    description: '当前景区尚未配置探索任务。',
  });
  const rawMissions = computed(() => (Array.isArray(missionsSource?.value?.missions) ? missionsSource.value.missions : []));

  function persistMissionState() {
    localStorage.setItem(storageKey, JSON.stringify({
      completedStepKeys: state.completedStepKeys,
      trackedMissionId: state.trackedMissionId,
    }));
  }

  function missionCompletedWithSet(mission, completedSet) {
    const steps = Array.isArray(mission?.steps) ? mission.steps : [];
    if (!steps.length) {
      return false;
    }
    return steps.every((step) => completedSet.has(buildStepKey(mission.id, step.id)));
  }

  const missions = computed(() => {
    const completedSet = new Set(state.completedStepKeys);
    return rawMissions.value.map((mission, missionIndex) => {
      const steps = Array.isArray(mission?.steps) ? mission.steps : [];
      const stepItems = steps.map((step, stepIndex) => {
        const spot = step?.spotId ? findSpotById(step.spotId) : null;
        const completed = completedSet.has(buildStepKey(mission.id, step.id));
        return {
          ...step,
          order: stepIndex + 1,
          completed,
          spotName: spot?.name || '',
          canOpenSpot: Boolean(step?.spotId),
        };
      });

      const completedSteps = stepItems.filter((item) => item.completed).length;
      const totalSteps = stepItems.length;
      const isCompleted = totalSteps > 0 && completedSteps >= totalSteps;
      const nextStep = stepItems.find((item) => !item.completed) || null;
      const typeKey = normalizeKey(mission?.type);
      const typeLabel = typeKey === 'main' ? '主线任务' : '支线任务';

      return {
        ...mission,
        index: missionIndex + 1,
        typeLabel,
        steps: stepItems,
        totalSteps,
        completedSteps,
        progressPercent: totalSteps ? Math.round((completedSteps / totalSteps) * 100) : 0,
        progressText: `${completedSteps} / ${totalSteps}`,
        isCompleted,
        isTracked: state.trackedMissionId === mission.id,
        statusLabel: isCompleted ? '已完成' : (state.trackedMissionId === mission.id ? '追踪中' : '可进行'),
        nextStep,
      };
    });
  });

  const trackedMission = computed(() => missions.value.find((item) => item.id === state.trackedMissionId) || null);
  const unlockedRewards = computed(() => missions.value
    .filter((mission) => mission.isCompleted)
    .flatMap((mission) => (Array.isArray(mission.rewards) ? mission.rewards : []).map((reward) => ({
      ...reward,
      missionId: mission.id,
      missionTitle: mission.title,
    }))));

  const missionSummary = computed(() => ({
    totalMissions: missions.value.length,
    completedMissions: missions.value.filter((item) => item.isCompleted).length,
    unlockedRewards: unlockedRewards.value.length,
    trackedMissionTitle: trackedMission.value?.title || '',
  }));

  function syncTrackedMission() {
    const availableMissionIds = missions.value.map((item) => item.id);
    if (!availableMissionIds.includes(state.trackedMissionId)) {
      state.trackedMissionId = null;
    }

    const currentTrackedMission = missions.value.find((item) => item.id === state.trackedMissionId);
    if (currentTrackedMission && !currentTrackedMission.isCompleted) {
      return;
    }

    state.trackedMissionId = missions.value.find((item) => !item.isCompleted)?.id || missions.value[0]?.id || null;
  }

  function trackMission(missionId) {
    const target = missions.value.find((item) => item.id === missionId);
    if (!target) {
      return null;
    }
    state.trackedMissionId = target.id;
    persistMissionState();
    return target;
  }

  function applyMissionProgress(stepMatcher) {
    const completedSet = new Set(state.completedStepKeys);
    const completedSteps = [];
    const completedMissions = [];

    rawMissions.value.forEach((mission) => {
      const steps = Array.isArray(mission?.steps) ? mission.steps : [];
      const beforeCompleted = missionCompletedWithSet(mission, completedSet);

      steps.forEach((step) => {
        const stepKey = buildStepKey(mission.id, step.id);
        if (completedSet.has(stepKey)) {
          return;
        }
        if (!stepMatcher(mission, step)) {
          return;
        }
        completedSet.add(stepKey);
        completedSteps.push({
          missionId: mission.id,
          missionTitle: mission.title,
          stepId: step.id,
          stepTitle: step.title,
        });
      });

      const afterCompleted = missionCompletedWithSet(mission, completedSet);
      if (!beforeCompleted && afterCompleted) {
        completedMissions.push({
          missionId: mission.id,
          missionTitle: mission.title,
          rewards: Array.isArray(mission.rewards) ? mission.rewards : [],
        });
      }
    });

    const changed = completedSteps.length > 0;
    if (changed) {
      state.completedStepKeys = Array.from(completedSet);
      syncTrackedMission();
      persistMissionState();
    }

    return {
      changed,
      completedSteps,
      completedMissions,
    };
  }

  function handleVisitSpot(spotId) {
    const resolvedSpotId = normalizeText(spotId);
    if (!resolvedSpotId || !missionEnabled.value) {
      return { changed: false, completedSteps: [], completedMissions: [] };
    }
    return applyMissionProgress((mission, step) => isVisitStep(step.type) && normalizeText(step.spotId) === resolvedSpotId);
  }

  function handleDiscoverSpot(spotId) {
    const resolvedSpotId = normalizeText(spotId);
    if (!resolvedSpotId || !missionEnabled.value) {
      return { changed: false, completedSteps: [], completedMissions: [] };
    }
    return applyMissionProgress((mission, step) => isDiscoverStep(step.type) && normalizeText(step.spotId) === resolvedSpotId);
  }

  function resetMissionProgress() {
    state.completedStepKeys = [];
    state.trackedMissionId = null;
    syncTrackedMission();
    persistMissionState();
  }

  watch(rawMissions, () => {
    if (!rawMissions.value.length) {
      return;
    }
    syncTrackedMission();
    persistMissionState();
  }, { immediate: true, deep: true });

  return {
    missionEnabled,
    missionTitle,
    missionSubtitle,
    missionEmptyState,
    missions,
    trackedMission,
    trackedMissionId: computed(() => state.trackedMissionId),
    missionSummary,
    unlockedRewards,
    trackMission,
    handleVisitSpot,
    handleDiscoverSpot,
    resetMissionProgress,
  };
}
