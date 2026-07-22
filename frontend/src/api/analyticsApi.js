import axiosClient from './axiosClient'

export function fetchCitizenSummary() {
  return axiosClient.get('/analytics/citizen-summary').then((res) => res.data.data)
}

export function fetchAnalyticsOverview() {
  return axiosClient.get('/analytics/overview').then((res) => res.data.data)
}
