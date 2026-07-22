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

export function fetchAllReports(page = 0, size = 10, status = null) {
  return axiosClient
    .get('/reports', { params: { page, size, ...(status ? { status } : {}) } })
    .then((res) => res.data.data)
}

export function updateReportStatus(id, status, note) {
  return axiosClient.patch(`/reports/${id}/status`, { status, note }).then((res) => res.data.data)
}

export function updateReportPriority(id, repairPriority) {
  return axiosClient.patch(`/reports/${id}/priority`, { repairPriority }).then((res) => res.data.data)
}
