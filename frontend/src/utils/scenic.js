export const DEFAULT_SCENIC_ID = 'jiuzhaigou'

const SCENIC_ID_ALIAS_MAP = {
  'muguang-wetland': 'jiuzhaigou'
}

const DEFAULT_THEME = {
  brand: '#2ea67c',
  accent: '#58d2ad',
  background: '#07111f'
}

export function normalizeScenicId(value) {
  const scenicId = String(value || '').trim()
  if (!scenicId) return DEFAULT_SCENIC_ID
  return SCENIC_ID_ALIAS_MAP[scenicId] || scenicId
}

export function buildScenicPagePath(scenicId, page = 'home') {
  const id = encodeURIComponent(normalizeScenicId(scenicId))
  switch (page) {
    case 'explore':
      return `/consult/${id}/explore`
    case 'cloud':
    case 'cloud-tour':
      return `/consult/${id}/cloud-tour`
    default:
      return `/consult/${id}`
  }
}

export function buildScenicStorageKey(scenicId, scope, version = 'v1') {
  return `scenic:${normalizeScenicId(scenicId)}:${scope}:${version}`
}

function normalizeHex(value, fallback) {
  const raw = String(value || fallback || '').trim()
  const hex = raw.startsWith('#') ? raw.slice(1) : raw
  if (/^[0-9a-fA-F]{6}$/.test(hex)) {
    return `#${hex.toLowerCase()}`
  }
  if (/^[0-9a-fA-F]{3}$/.test(hex)) {
    return `#${hex.split('').map((char) => `${char}${char}`).join('').toLowerCase()}`
  }
  return fallback
}

function hexToRgbTuple(hex) {
  const normalized = normalizeHex(hex, DEFAULT_THEME.brand).slice(1)
  return [
    Number.parseInt(normalized.slice(0, 2), 16),
    Number.parseInt(normalized.slice(2, 4), 16),
    Number.parseInt(normalized.slice(4, 6), 16)
  ]
}

function rgbTupleToHex([r, g, b]) {
  return `#${[r, g, b].map((value) => Math.max(0, Math.min(255, Math.round(value))).toString(16).padStart(2, '0')).join('')}`
}

function mixRgbTuple(base, target, weight = 0.5) {
  return base.map((channel, index) => (channel * (1 - weight)) + (target[index] * weight))
}

export function buildScenicThemeStyle(theme = {}) {
  const brand = normalizeHex(theme.brand, DEFAULT_THEME.brand)
  const accent = normalizeHex(theme.accent, DEFAULT_THEME.accent)
  const background = normalizeHex(theme.background, DEFAULT_THEME.background)

  const brandRgb = hexToRgbTuple(brand)
  const accentRgb = hexToRgbTuple(accent)
  const backgroundRgb = hexToRgbTuple(background)
  const brandDeep = rgbTupleToHex(mixRgbTuple(brandRgb, [0, 0, 0], 0.26))
  const brandDeepRgb = hexToRgbTuple(brandDeep)
  const pageLightStart = rgbTupleToHex(mixRgbTuple(brandRgb, [255, 255, 255], 0.92))
  const pageLightEnd = rgbTupleToHex(mixRgbTuple(accentRgb, [255, 255, 255], 0.96))

  return {
    '--brand': brand,
    '--brand-soft': accent,
    '--brand-deep': brandDeep,
    '--brand-rgb': brandRgb.join(', '),
    '--brand-soft-rgb': accentRgb.join(', '),
    '--brand-deep-rgb': brandDeepRgb.join(', '),
    '--bg-dark': background,
    '--bg-dark-rgb': backgroundRgb.join(', '),
    '--page-light-start': pageLightStart,
    '--page-light-end': pageLightEnd,
    '--panel-dark': `rgba(${backgroundRgb.join(', ')}, 0.78)`,
    '--panel-dark-strong': `rgba(${backgroundRgb.join(', ')}, 0.88)`
  }
}
