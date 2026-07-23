import axiosClient from './axiosClient'

export function login(email, password) {
  return axiosClient.post('/auth/login', { email, password }).then((res) => res.data.data)
}

export function register(payload) {
  return axiosClient.post('/auth/register', payload).then((res) => res.data.data)
}

export function verifyEmail(token) {
  return axiosClient.post('/auth/verify-email', { token }).then((res) => res.data.data)
}

export function resendVerification(email) {
  return axiosClient.post('/auth/resend-verification', { email }).then((res) => res.data.message)
}

export function forgotPassword(email) {
  return axiosClient.post('/auth/forgot-password', { email }).then((res) => res.data.message)
}

export function resetPassword(token, newPassword) {
  return axiosClient.post('/auth/reset-password', { token, newPassword }).then((res) => res.data.message)
}

export function fetchCurrentUser() {
  return axiosClient.get('/users/me').then((res) => res.data.data)
}
