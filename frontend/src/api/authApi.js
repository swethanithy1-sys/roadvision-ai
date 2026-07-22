import axiosClient from './axiosClient'

export function login(email, password) {
  return axiosClient.post('/auth/login', { email, password }).then((res) => res.data.data)
}

export function register(payload) {
  return axiosClient.post('/auth/register', payload).then((res) => res.data.data)
}

export function fetchCurrentUser() {
  return axiosClient.get('/users/me').then((res) => res.data.data)
}
