import { resolveAssetUrl } from './assets'

const FALLBACK_IMAGE = resolveAssetUrl('/uploads/wetland/广东深圳福田红树林国家重要湿地.png')

export const SEARCH_TYPE_OPTIONS = [
  { label: '全部', value: 'all' },
  { label: '湿地', value: 'wetland' },
  { label: '珍稀动植物', value: 'flora' },
  { label: '帖子', value: 'post' }
]

export const formatSearchType = (type) => {
  if (type === 'wetland') return 'Wetland'
  if (type === 'flora') return 'Species'
  if (type === 'post') return 'Community'
  return 'Result'
}

export const resolveSearchResultRoute = (item) => {
  const id = item?.id != null ? String(item.id) : ''
  const explicitPath = typeof item?.path === 'string' ? item.path.trim() : ''
  const isDetailLikePath = explicitPath.startsWith('/detail/') || explicitPath.startsWith('/flora/')

  if (isDetailLikePath) return explicitPath

  if (!id) return '/'

  if (item?.type === 'wetland') {
    return `/detail/${id}`
  }

  if (item?.type === 'flora') {
    const query = item?.wetlandId != null ? `?wetlandId=${encodeURIComponent(String(item.wetlandId))}` : ''
    return `/flora/${id}${query}`
  }

  if (item?.type === 'post') {
    return `/community`
  }

  return '/'
}

export const fetchSearchResults = async (api, { keyword, type = 'all' }) => {
  const response = await api.get('/search', {
    params: {
      keyword,
      type
    }
  })

  if (!response.data?.success) {
    throw new Error(response.data?.message || '搜索失败')
  }

  return (response.data.items || []).map((item) => ({
    ...item,
    image: resolveAssetUrl(item.image, FALLBACK_IMAGE)
  }))
}
