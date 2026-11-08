import axios from 'axios'

/** Base URL of the API. VITE_API_BASE_URL may be given with or without the /api suffix. */
export const API_BASE = (() => {
  const configured = import.meta.env.VITE_API_BASE_URL
  if (!configured) return '/api'
  const trimmed = configured.replace(/\/+$/, '')
  return trimmed.endsWith('/api') ? trimmed : `${trimmed}/api`
})()

const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  }
)

/** The most useful message available: field errors first, then the server's message. */
export function errorMessage(err, fallback = 'Something went wrong. Please try again.') {
  const data = err?.response?.data
  if (data?.details) {
    const first = Object.values(data.details)[0]
    if (first) return first
  }
  if (data?.message) return data.message
  if (err?.request && !err?.response) return 'Cannot reach the server. Check your connection.'
  return fallback
}

export default api
