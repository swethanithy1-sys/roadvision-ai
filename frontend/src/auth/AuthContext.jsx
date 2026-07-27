import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import * as authApi from '../api/authApi'
import { clearSession, getStoredUser, getToken, saveSession } from './tokenStorage'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getStoredUser)
  const [token, setToken] = useState(getToken)

  const login = useCallback(async (email, password) => {
    const data = await authApi.login(email, password)
    saveSession(data.token, data.user)
    setToken(data.token)
    setUser(data.user)
    return data.user
  }, [])

  const register = useCallback(async (payload) => {
    // No session yet — the account exists but is unverified until the emailed code is confirmed.
    return authApi.register(payload)
  }, [])

  const completeRegistration = useCallback(async (payload) => {
    const data = await authApi.setPassword(payload)
    saveSession(data.token, data.user)
    setToken(data.token)
    setUser(data.user)
    return data.user
  }, [])

  const logout = useCallback(() => {
    clearSession()
    setToken(null)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({
      user,
      token,
      isAuthenticated: Boolean(token),
      isAdmin: user?.role === 'ADMIN',
      login,
      register,
      completeRegistration,
      logout,
    }),
    [user, token, login, register, completeRegistration, logout]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return ctx
}
