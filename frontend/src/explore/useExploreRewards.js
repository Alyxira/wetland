import { computed } from 'vue';

function normalizeText(value) {
  return String(value || '').trim();
}

function normalizeKey(value) {
  return normalizeText(value).toLowerCase().replace(/[^a-z0-9]/g, '');
}

function rarityLabel(value) {
  switch (normalizeKey(value)) {
    case 'legendary':
      return '传说';
    case 'epic':
      return '史诗';
    case 'rare':
      return '稀有';
    case 'common':
    default:
      return '普通';
  }
}

function rewardTypeLabel(value) {
  switch (normalizeKey(value)) {
    case 'stamp':
      return '印章';
    case 'badge':
      return '徽章';
    case 'card':
      return '卡牌';
    default:
      return '奖励';
  }
}

export function useExploreRewards({
  rewardsSource,
  missionsSource,
} = {}) {
  const rewardEnabled = computed(() => Boolean(rewardsSource?.value?.enabled));
  const rewardTitle = computed(() => rewardsSource?.value?.title || '奖励图鉴');
  const rewardSubtitle = computed(() => rewardsSource?.value?.subtitle || '完成任务后逐步解锁印章、徽章与纪念收获。');
  const rewardEmptyState = computed(() => rewardsSource?.value?.emptyState || {
    title: '暂未开放奖励图鉴',
    description: '当前景区尚未配置奖励内容。',
  });
  const rewardCatalog = computed(() => (Array.isArray(rewardsSource?.value?.rewards) ? rewardsSource.value.rewards : []));
  const missions = computed(() => (Array.isArray(missionsSource?.value) ? missionsSource.value : []));

  const rewardMissionMap = computed(() => {
    const map = new Map();
    missions.value.forEach((mission) => {
      (Array.isArray(mission?.rewards) ? mission.rewards : []).forEach((reward) => {
        const rewardId = normalizeText(reward?.rewardId);
        if (!rewardId) {
          return;
        }
        if (!map.has(rewardId)) {
          map.set(rewardId, []);
        }
        map.get(rewardId).push({
          missionId: mission.id,
          missionTitle: mission.title,
          isCompleted: Boolean(mission.isCompleted),
          isTracked: Boolean(mission.isTracked),
          progressText: mission.progressText || '',
        });
      });
    });
    return map;
  });

  const rewards = computed(() => rewardCatalog.value.map((reward, index) => {
    const sourceMissions = rewardMissionMap.value.get(reward.id) || [];
    const unlocked = sourceMissions.length ? sourceMissions.some((item) => item.isCompleted) : false;

    return {
      ...reward,
      index: index + 1,
      typeLabel: rewardTypeLabel(reward.type),
      rarityLabel: rarityLabel(reward.rarity),
      sourceMissions,
      unlocked,
      statusLabel: unlocked ? '已解锁' : (sourceMissions.length ? '待解锁' : '暂未关联'),
      actionMissionId: sourceMissions.find((item) => !item.isCompleted)?.missionId || sourceMissions[0]?.missionId || '',
      actionMissionTitle: sourceMissions.find((item) => !item.isCompleted)?.missionTitle || sourceMissions[0]?.missionTitle || '',
    };
  }));

  const unlockedRewards = computed(() => rewards.value.filter((item) => item.unlocked));
  const lockedRewards = computed(() => rewards.value.filter((item) => !item.unlocked));
  const rewardSummary = computed(() => ({
    totalRewards: rewards.value.length,
    unlockedRewards: unlockedRewards.value.length,
    lockedRewards: lockedRewards.value.length,
  }));

  return {
    rewardEnabled,
    rewardTitle,
    rewardSubtitle,
    rewardEmptyState,
    rewards,
    unlockedRewards,
    lockedRewards,
    rewardSummary,
  };
}
