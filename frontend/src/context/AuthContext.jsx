import { createContext, useContext, useEffect, useState } from 'react'
import { authService } from '../services/authService'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('user'))
    } catch {
      return null
    }
  })
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    const token = localStorage.getItem('token')
    if (token && !user) {
      authService.me().then((r) => {
        const u = r.data
        setUser(u)
        localStorage.setItem('user', JSON.stringify(u))
      }).catch(() => {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
      })
    }
    // Restores the session once on page load.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const login = async (email, password) => {
    setLoading(true)
    try {
      const r = await authService.login({ email, password })
      const { token, ...payload } = r.data
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(payload))
      setUser(payload)
      return payload
    } finally {
      setLoading(false)
    }
  }

  const register = async (data) => {
    setLoading(true)
    try {
      const r = await authService.register(data)
      const { token, ...payload } = r.data
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(payload))
      setUser(payload)
      return payload
    } finally {
      setLoading(false)
    }
  }

  const updateUser = (changes) => {
    const next = { ...user, ...changes }
    localStorage.setItem('user', JSON.stringify(next))
    setUser(next)
  }

  const logout = async () => {
    // Tokens are stateless, so logging out locally still works if this call fails.
    await authService.logout().catch(() => {})
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, updateUser, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}