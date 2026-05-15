function inferApiOrigin() {
  const configured = import.meta.env.VITE_API_ORIGIN
  if (configured && String(configured).trim()) {
    return String(configured).trim().replace(/\/+$/, '')
  }

  if (typeof window !== 'undefined' && window.location?.origin) {
    const { origin, hostname, protocol } = window.location

    if (hostname === 'www.mcpmap.cn' || hostname === 'mcpmap.cn') {
      return 'https://api.mcpmap.cn'
    }

    if (hostname === 'api.mcpmap.cn') {
      return origin.replace(/\/+$/, '')
    }

    if (hostname === 'localhost' || hostname === '127.0.0.1') {
      return `${protocol}//${hostname}:8080`
    }
  }

  return 'http://localhost:8080'
}

const API_ORIGIN = inferApiOrigin()

export const resolveAssetUrl = (path, fallback = '') => {
  if (!path || !String(path).trim()) return fallback

  const normalized = String(path).trim().replace(/\\/g, '/')

  if (/^https?:\/\//i.test(normalized)) return normalized
  if (/^file:\/\//i.test(normalized)) return fallback
  if (/^[a-zA-Z]:\//.test(normalized)) return fallback

  if (normalized.startsWith('/uploads/') || normalized.startsWith('/defaults/')) {
    return `${API_ORIGIN}/api${normalized}`
  }

  if (normalized.startsWith('uploads/') || normalized.startsWith('defaults/')) {
    return `${API_ORIGIN}/api/${normalized}`
  }

  return fallback
}
