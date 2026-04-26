const API_ORIGIN = import.meta.env.VITE_API_ORIGIN || 'http://localhost:8080'

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
