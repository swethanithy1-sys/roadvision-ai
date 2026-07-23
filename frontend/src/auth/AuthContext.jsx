import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import * as authApi from '../api/authApi'
import { clearSession, getStoredUser, getToken, saveSession } from './tokenStorage'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getStoredUser)
  const [token, setToken] = useState(getToken)
  const [isLoading, setIsLoading] = useState(false)

  const login = useCallback(async (email, password) => {
    setIsLoading(true)
    try {
      const data = await authApi.login(email, password)
      saveSession(data.token, data.user)
      setToken(data.token)
      setUser(data.user)
      return data.user
    } finally {
      setIsLoading(false)
    }
  }, [])

  const register = useCallback(async (payload) => {
    setIsLoading(true)
    try {
      // Accounts start unverified — no session yet; the user must confirm their email first.
      return await authApi.register(payload)
    } finally {
      setIsLoading(false)
    }
  }, [])

  const verifyEmail = useCallback(async (token) => {
    setIsLoading(true)
    try {
      const data = await authApi.verifyEmail(token)
      saveSession(data.token, data.user)
      setToken(data.token)
      setUser(data.user)
      return data.user
    } finally {
      setIsLoading(false)
    }
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
      isLoading,
      login,
      register,
      verifyEmail,
      logout,
    }),
    [user, token, isLoading, login, register, verifyEmail, logout]
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
