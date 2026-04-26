import api from '../utils/api'
import { resolveAssetUrl } from '../utils/assets'

export const WaterQualityParameter = {
  CHLA: 'CHLA',
  TSS: 'TSS',
  TURBIDITY: 'TURBIDITY'
}

export const regionApi = {
  getRegions(wetlandId = null) {
    return api.get('/v1/regions', {
      params: wetlandId == null ? {} : { wetlandId }
    })
  },
  getRegion(regionId) {
    return api.get(`/v1/regions/${regionId}`)
  },
  createRegion(name, geometry, wetlandId = null) {
    return api.post('/v1/regions', { name, geometry, wetlandId })
  },
  deleteRegion(regionId) {
    return api.delete(`/v1/regions/${regionId}`)
  }
}

export const analyticsApi = {
  getTimeSeries(regionId, start, end, parameter, interval = 'month') {
    return api.get(`/v1/regions/${regionId}/timeseries`, {
      params: { start, end, parameter, interval }
    })
  },
  getStatistics(regionId, start, end, parameters = []) {
    return api.get(`/v1/regions/${regionId}/statistics`, {
      params: {
        start,
        end,
        parameters: Array.isArray(parameters) ? parameters.join(',') : parameters
      }
    })
  },
  getWetlandHistory(wetlandName, start = null, end = null) {
    return api.get(`/v1/history/wetlands/${encodeURIComponent(wetlandName)}`, {
      params: {
        ...(start ? { start } : {}),
        ...(end ? { end } : {})
      }
    })
  }
}

export const waterbodyApi = {
  getFiles() {
    return api.get('/v1/waterbody/files')
  },
  getAllGeoJson() {
    return api.get('/v1/waterbody/all-geojson')
  },
  getBounds(filename) {
    return api.get(`/v1/waterbody/bounds/${encodeURIComponent(filename)}`)
  }
}

export const imageApi = {
  getLocalImages() {
    return api.get('/v1/images/local')
  },
  getDistributionMapPngs() {
    return api.get('/v1/distribution-maps/pngs')
  },
  getImageBounds(imageId) {
    return api.get(`/v1/images/${imageId}/bounds`)
  },
  getImageFileUrl(imageId) {
    return `/api/v1/images/${imageId}/file`
  }
}

export const screenApi = {
  getDashboard() {
    const endpoint = import.meta.env.VITE_SCREEN_DASHBOARD_ENDPOINT || '/v1/screen/dashboard'
    return api.get(endpoint)
  },
  getWetlandSpots() {
    const endpoint = import.meta.env.VITE_SCREEN_WETLAND_SPOTS_ENDPOINT || '/consult/wetlands/spots'
    return api.get(endpoint)
  },
  getChinaGeoJsonUrl() {
    return import.meta.env.VITE_SCREEN_CHINA_GEOJSON_URL || 'https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json'
  }
}

export function coordinatesToGeoJSON(coords, type = 'Polygon') {
  if (!Array.isArray(coords) || coords.length < 3) {
    return { type, coordinates: [] }
  }
  const ring = [...coords]
  const first = ring[0]
  const last = ring[ring.length - 1]
  if (!last || first[0] !== last[0] || first[1] !== last[1]) {
    ring.push(first)
  }
  return {
    type,
    coordinates: [ring]
  }
}

export function boundsToGeoJSON(minLon, maxLon, minLat, maxLat) {
  return {
    type: 'Polygon',
    coordinates: [[
      [minLon, minLat],
      [minLon, maxLat],
      [maxLon, maxLat],
      [maxLon, minLat],
      [minLon, minLat]
    ]]
  }
}

export { resolveAssetUrl }
