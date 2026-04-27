import axios from 'axios'
import { DEFAULT_SCENIC_ID, normalizeScenicId } from '../utils/scenic'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_ORIGIN || '/',
  timeout: 60000
})

function normalizeApiSegment(segment = '') {
  return String(segment || '').replace(/^\/+|\/+$/g, '')
}

export function buildScenicApiPath(scenicId, segment = '') {
  const id = encodeURIComponent(normalizeScenicId(scenicId))
  const suffix = normalizeApiSegment(segment)
  return suffix ? `/api/consult/scenics/${id}/${suffix}` : `/api/consult/scenics/${id}`
}

export const API = {
  scenicList: '/api/consult/scenics',
  wetlandSpots: '/api/consult/wetlands/spots',
  scenic: (scenicId) => buildScenicApiPath(scenicId),
  home: (scenicId) => buildScenicApiPath(scenicId, 'site/home'),
  explore: (scenicId) => buildScenicApiPath(scenicId, 'site/explore'),
  cloud: (scenicId) => buildScenicApiPath(scenicId, 'site/cloud-tour'),
  spots: (scenicId) => buildScenicApiPath(scenicId, 'spots'),
  spotsSummary: (scenicId) => buildScenicApiPath(scenicId, 'spots/summary'),
  featuredSpots: (scenicId) => buildScenicApiPath(scenicId, 'spots/featured'),
  search: (scenicId) => buildScenicApiPath(scenicId, 'spots/search'),
  routes: (scenicId) => buildScenicApiPath(scenicId, 'routes'),
  route: (scenicId) => buildScenicApiPath(scenicId, 'routes/customize'),
  missions: (scenicId) => buildScenicApiPath(scenicId, 'missions'),
  rewards: (scenicId) => buildScenicApiPath(scenicId, 'rewards'),
  tasks: (scenicId) => buildScenicApiPath(scenicId, 'tasks'),
  completeTask: (scenicId, taskId) => buildScenicApiPath(scenicId, `tasks/${encodeURIComponent(taskId)}/complete`),
  stamps: (scenicId) => buildScenicApiPath(scenicId, 'stamps'),
  collectStamp: (scenicId, stampId) => buildScenicApiPath(scenicId, `stamps/${encodeURIComponent(stampId)}/collect`),
  events: (scenicId) => buildScenicApiPath(scenicId, 'events'),
  interactEvent: (scenicId, eventId) => buildScenicApiPath(scenicId, `events/${encodeURIComponent(eventId)}/interact`),
  guideChat: (scenicId) => buildScenicApiPath(scenicId, 'guide/chat'),
  live2dConfig: (scenicId) => buildScenicApiPath(scenicId, 'live2d/config'),
  live2dChat: (scenicId) => buildScenicApiPath(scenicId, 'live2d/chat')
}

export async function request(url, options = {}) {
  const response = await api.request({
    url,
    method: options.method || 'GET',
    data: options.data,
    params: options.params,
    headers: options.headers
  })
  const payload = response.data
  if (!payload?.success) {
    throw new Error(payload?.message || '请求失败')
  }
  return payload.data
}

function createScenicBucket() {
  return {
    scenic: null,
    home: null,
    explore: null,
    cloud: null,
    missions: null,
    rewards: null,
    live2dConfig: null
  }
}

const state = {
  scenicList: [],
  scenicIndex: null,
  scenicCache: {}
}

const siteStore = {
  request,
  ensureBucket(scenicId = DEFAULT_SCENIC_ID) {
    const id = normalizeScenicId(scenicId)
    if (!state.scenicCache[id]) {
      state.scenicCache[id] = createScenicBucket()
    }
    return { id, bucket: state.scenicCache[id] }
  },
  bucketById(scenicId = DEFAULT_SCENIC_ID) {
    return state.scenicCache[normalizeScenicId(scenicId)] || null
  },
  scenicById(scenicId = DEFAULT_SCENIC_ID) {
    return state.scenicCache[normalizeScenicId(scenicId)]?.scenic || null
  },
  homeById(scenicId = DEFAULT_SCENIC_ID) {
    return state.scenicCache[normalizeScenicId(scenicId)]?.home || null
  },
  exploreById(scenicId = DEFAULT_SCENIC_ID) {
    return state.scenicCache[normalizeScenicId(scenicId)]?.explore || null
  },
  missionsById(scenicId = DEFAULT_SCENIC_ID) {
    return state.scenicCache[normalizeScenicId(scenicId)]?.missions || null
  },
  rewardsById(scenicId = DEFAULT_SCENIC_ID) {
    return state.scenicCache[normalizeScenicId(scenicId)]?.rewards || null
  },
  cloudById(scenicId = DEFAULT_SCENIC_ID) {
    return state.scenicCache[normalizeScenicId(scenicId)]?.cloud || null
  },
  live2dConfigById(scenicId = DEFAULT_SCENIC_ID) {
    return state.scenicCache[normalizeScenicId(scenicId)]?.live2dConfig || null
  },
  async ensureScenicList() {
    if (!state.scenicIndex) {
      state.scenicIndex = await request(API.scenicList)
      state.scenicList = Array.isArray(state.scenicIndex?.scenics) ? state.scenicIndex.scenics : []
    }
    return state.scenicIndex
  },
  async ensureScenic(scenicId = DEFAULT_SCENIC_ID) {
    const { id, bucket } = this.ensureBucket(scenicId)
    if (!bucket.scenic) {
      bucket.scenic = await request(API.scenic(id))
    }
    return bucket.scenic
  },
  async ensureHome(scenicId = DEFAULT_SCENIC_ID) {
    const { id, bucket } = this.ensureBucket(scenicId)
    if (!bucket.home) {
      bucket.home = await request(API.home(id))
    }
    return bucket.home
  },
  async ensureExplore(scenicId = DEFAULT_SCENIC_ID) {
    const { id, bucket } = this.ensureBucket(scenicId)
    if (!bucket.explore) {
      bucket.explore = await request(API.explore(id))
    }
    return bucket.explore
  },
  async ensureMissions(scenicId = DEFAULT_SCENIC_ID) {
    const { id, bucket } = this.ensureBucket(scenicId)
    if (!bucket.missions) {
      bucket.missions = await request(API.missions(id))
    }
    return bucket.missions
  },
  async ensureRewards(scenicId = DEFAULT_SCENIC_ID) {
    const { id, bucket } = this.ensureBucket(scenicId)
    if (!bucket.rewards) {
      bucket.rewards = await request(API.rewards(id))
    }
    return bucket.rewards
  },
  async ensureCloud(scenicId = DEFAULT_SCENIC_ID) {
    const { id, bucket } = this.ensureBucket(scenicId)
    if (!bucket.cloud) {
      bucket.cloud = await request(API.cloud(id))
    }
    return bucket.cloud
  },
  async ensureLive2dConfig(scenicId = DEFAULT_SCENIC_ID) {
    const { id, bucket } = this.ensureBucket(scenicId)
    if (!bucket.live2dConfig) {
      bucket.live2dConfig = await request(API.live2dConfig(id))
    }
    return bucket.live2dConfig
  }
}

export function useSiteStore() {
  return siteStore
}
