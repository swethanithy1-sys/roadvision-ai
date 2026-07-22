const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
const BACKEND_ORIGIN = new URL(API_BASE_URL).origin

export function assetUrl(path) {
  if (!path) return ''
  return `${BACKEND_ORIGIN}${path}`
}
