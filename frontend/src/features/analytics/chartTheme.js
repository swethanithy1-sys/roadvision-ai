// Severity uses the app's status palette (also used by SeverityPill) so meaning stays
// consistent between the pill badges and the charts. Always paired with a text legend/
// tooltip — never color alone — to stay usable for colorblind viewers.
export const SEVERITY_COLORS = {
  LOW: '#10b981',
  MEDIUM: '#f59e0b',
  HIGH: '#ef4444',
}

export const SEVERITY_ORDER = ['LOW', 'MEDIUM', 'HIGH']

export const CHART_PRIMARY = '#2563eb'

// Grid/axis ink needs its own light/dark step — chart SVGs can't read CSS
// variables directly, so this mirrors --color-border / --color-text-muted
// from theme.css for each scheme.
export const CHART_COLORS = {
  light: { grid: '#e2e8f0', text: '#64748b' },
  dark: { grid: '#24304a', text: '#94a3b8' },
}
