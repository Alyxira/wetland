import { reactive } from 'vue';
import { DEFAULT_SCENIC_ID, buildScenicStorageKey, normalizeScenicId } from '../../utils/scenic';

const LEGACY_STORAGE_KEY = 'wetland_explore_save_v1';
const LEGACY_PLAN_KEY = 'wetland_ai_plan_v1';

const LEGACY_ENTRY_MAP = {
  'animal-egret': { title: '白鹭', text: '白鹭常在浅水地带缓慢移动觅食，安静观察时更容易注意到它们的身影。', id: 'egret-bank' },
  'animal-frog': { title: '小青蛙同伴', text: '两栖动物对环境湿度和水体状态很敏感，能在这里出现通常说明生态条件不错。', id: 'frog-pool' },
  'plant-reed': { title: '芦苇', text: '芦苇是湿地常见植物群落之一，能够为鸟类和小型生物提供遮蔽与停驻空间。', id: 'reed-isle' },
  'plant-lotus': { title: '睡莲 / 荷花（示例）', text: '漂浮或挺水植物可以为水面提供阴影，也会形成更丰富的微型生境。', id: 'lotus-mirror' },
  'item-stamp': { title: '巡游印章', text: '你获得了一枚巡游印章。后续可以继续扩展成成就、兑换或剧情钥匙。', id: 'stamp-spark' },
  'story-note': { title: '湿地小知识', text: '湿地常被称为“地球之肾”，在净化水质、调蓄洪水和维持生物多样性方面都很重要。', id: 'note-deck' },
};

function safeParseStorage(key) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : null;
  } catch (error) {
    return null;
  }
}

function resolveLegacyStorage(scenicId, key, legacyKey) {
  const next = safeParseStorage(key);
  if (next) {
    return next;
  }
  if (normalizeScenicId(scenicId) === DEFAULT_SCENIC_ID) {
    return safeParseStorage(legacyKey);
  }
  return null;
}

function loadExploreStorage(scenicId, storageKey) {
  const raw = resolveLegacyStorage(scenicId, storageKey, LEGACY_STORAGE_KEY) || {};
  const discoveredEntries = [];

  if (Array.isArray(raw.discoveredEntries)) {
    raw.discoveredEntries.forEach((item) => {
      if (!item) return;
      const title = item.title || '';
      const text = item.text || item.desc || '';
      if (!title || !text) return;
      discoveredEntries.push({ title, text, id: item.id || null });
    });
  }

  if (!discoveredEntries.length && Array.isArray(raw.discoveredEncounters)) {
    raw.discoveredEncounters.forEach((legacyId) => {
      const mapped = LEGACY_ENTRY_MAP[legacyId];
      if (mapped) {
        discoveredEntries.push({ ...mapped });
      }
    });
  }

  let lastPosition = Array.isArray(raw.lastPosition) && raw.lastPosition.length === 2
    ? [Number(raw.lastPosition[0]), Number(raw.lastPosition[1])]
    : null;

  if ((!lastPosition || !Number.isFinite(lastPosition[0]) || !Number.isFinite(lastPosition[1])) && raw.lastPositionByRoute && typeof raw.lastPositionByRoute === 'object') {
    const legacyPosition = Object.values(raw.lastPositionByRoute).find((item) => Array.isArray(item) && item.length === 2);
    if (legacyPosition) {
      lastPosition = [Number(legacyPosition[0]), Number(legacyPosition[1])];
    }
  }

  return {
    discoveredSpotIds: Array.from(new Set(Array.isArray(raw.discoveredSpotIds) ? raw.discoveredSpotIds.filter(Boolean) : [])),
    discoveredEntries: discoveredEntries.filter((entry, index, list) => list.findIndex((item) => item.title === entry.title) === index),
    lastPosition,
  };
}

export function useExploreStorage(scenicId = DEFAULT_SCENIC_ID) {
  const resolvedScenicId = normalizeScenicId(scenicId);
  const storageKey = buildScenicStorageKey(resolvedScenicId, 'explore');
  const planKey = buildScenicStorageKey(resolvedScenicId, 'route-plan');
  const storage = reactive(loadExploreStorage(resolvedScenicId, storageKey));

  function persistStorage() {
    localStorage.setItem(storageKey, JSON.stringify({
      discoveredSpotIds: storage.discoveredSpotIds,
      discoveredEntries: storage.discoveredEntries,
      lastPosition: storage.lastPosition,
    }));
  }

  function loadStoredPlan() {
    return resolveLegacyStorage(resolvedScenicId, planKey, LEGACY_PLAN_KEY);
  }

  function saveStoredPlan(plan) {
    localStorage.setItem(planKey, JSON.stringify(plan));
  }

  function clearStoredPlan() {
    localStorage.removeItem(planKey);
  }

  return {
    storage,
    persistStorage,
    loadStoredPlan,
    saveStoredPlan,
    clearStoredPlan,
  };
}
