import axiosClient from './axiosClient'

export function generateWorkOrder(reportId) {
  return axiosClient.post(`/reports/${reportId}/work-order`).then((res) => res.data.data)
}

export function fetchWorkOrder(reportId) {
  return axiosClient.get(`/reports/${reportId}/work-order`).then((res) => res.data.data)
}
