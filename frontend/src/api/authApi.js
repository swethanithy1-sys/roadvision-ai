import axiosClient from './axiosClient'

export function register(payload) {
  return axiosClient.post('/auth/register', payload).then((res) => res.data.data)
}

export function verifyOtp(payload) {
  return axiosClient.post('/auth/verify-otp', payload).then((res) => res.data.data)
}

export function setPassword(payload) {
  return axiosClient.post('/auth/set-password', payload).then((res) => res.data.data)
}

export function login(email, password) {
  return axiosClient.post('/auth/login', { email, password }).then((res) => res.data.data)
}

export function forgotPassword(email) {
  return axiosClient.post('/auth/forgot-password', { email }).then((res) => res.data.message)
}

export function resetPassword(payload) {
  return axiosClient.post('/auth/reset-password', payload).then((res) => res.data.message)
}

export function fetchCurrentUser() {
  return axiosClient.get('/users/me').then((res) => res.data.data)
}
