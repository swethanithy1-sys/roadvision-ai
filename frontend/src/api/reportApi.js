import axiosClient from './axiosClient'

export function submitReport({ image, latitude, longitude, addressText, description }) {
  const formData = new FormData()
  formData.append('image', image)
  formData.append(
    'report',
    new Blob([JSON.stringify({ latitude, longitude, addressText, description })], {
      type: 'application/json',
    })
  )

  return axiosClient
    .post('/reports', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((res) => res.data.data)
}

export function fetchMyReports(page = 0, size = 10) {
  return axiosClient.get('/reports/mine', { params: { page, size } }).then((res) => res.data.data)
}

export function fetchReportById(id) {
  return axiosClient.get(`/reports/${id}`).then((res) => res.data.data)
}

export function fetchReportTimeline(id) {
  return axiosClient.get(`/reports/${id}/timeline`).then((res) => res.data.data)
}

export function fetchReportMap() {
  return axiosClient.get('/reports/map').then((res) => res.data.data)
}
