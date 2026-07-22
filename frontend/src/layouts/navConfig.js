export const CITIZEN_NAV = [
  { label: 'Dashboard', path: '/dashboard', icon: 'grid', available: true },
  { label: 'Report Damage', path: '/report-damage', icon: 'camera', available: false },
  { label: 'My Reports', path: '/my-reports', icon: 'list', available: false },
  { label: 'Analytics', path: '/analytics', icon: 'bar-chart', available: false },
  { label: 'Hazard Map', path: '/hazard-map', icon: 'map', available: false },
]

export const ADMIN_NAV = [
  { label: 'Admin Dashboard', path: '/admin', icon: 'shield', available: false },
  { label: 'Repair Management', path: '/admin/repairs', icon: 'tool', available: false },
  { label: 'Analytics', path: '/analytics', icon: 'bar-chart', available: false },
  { label: 'Hazard Map', path: '/hazard-map', icon: 'map', available: false },
]
