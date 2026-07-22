import { useEffect, useState } from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import StatCard from '../../components/StatCard'
import { fetchAnalyticsOverview } from '../../api/analyticsApi'
import { CHART_GRID, CHART_PRIMARY, CHART_TEXT, SEVERITY_COLORS, SEVERITY_ORDER } from './chartTheme'

const SEVERITY_LABELS = { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High' }

function formatMonth(monthKey) {
  const [year, month] = monthKey.split('-')
  return new Date(Number(year), Number(month) - 1, 1).toLocaleDateString(undefined, {
    month: 'short',
    year: '2-digit',
  })
}

export default function AnalyticsPage() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    fetchAnalyticsOverview()
      .then((overview) => {
        if (!cancelled) setData(overview)
      })
      .catch((err) => {
        if (!cancelled) setError(err.friendlyMessage || 'Failed to load analytics.')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  if (loading) {
    return <div className="card p-4 text-center text-muted-app">Loading analytics…</div>
  }

  if (error || !data) {
    return <div className="alert alert-danger">{error || 'No analytics available.'}</div>
  }

  const severityData = SEVERITY_ORDER.map((severity) => ({
    severity,
    label: SEVERITY_LABELS[severity],
    count: data.reportsBySeverity.find((s) => s.severity === severity)?.count || 0,
  }))

  const monthlyData = data.monthlyReports.map((m) => ({ month: formatMonth(m.month), count: m.count }))

  const areaData = [...data.topAffectedAreas].reverse()

  return (
    <div>
      <div className="mb-4">
        <h1 className="h4 fw-bold mb-1">Analytics Dashboard</h1>
        <p className="text-muted-app mb-0">City-wide road condition insights from all reports.</p>
      </div>

      <div className="row g-3 mb-4">
        <div className="col-6 col-lg-3">
          <StatCard label="Total Reports" value={data.totalReports} accent="primary" />
        </div>
        <div className="col-6 col-lg-3">
          <StatCard label="Resolution Rate" value={`${data.resolutionRatePercent}%`} accent="secondary" />
        </div>
        <div className="col-6 col-lg-3">
          <StatCard label="Avg. AI Confidence" value={`${data.averageConfidence}%`} accent="accent" />
        </div>
        <div className="col-6 col-lg-3">
          <StatCard label="Road Safety Index" value={data.roadSafetyIndex} accent="danger" />
        </div>
      </div>

      <div className="row g-3 mb-3">
        <div className="col-lg-5">
          <div className="card p-4 h-100">
            <h2 className="h6 fw-semibold mb-3">Reports by Severity</h2>
            {data.totalReports === 0 ? (
              <div className="empty-state py-4 small">No reports yet.</div>
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={severityData} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke={CHART_GRID} vertical={false} />
                  <XAxis dataKey="label" tick={{ fill: CHART_TEXT, fontSize: 12 }} axisLine={{ stroke: CHART_GRID }} tickLine={false} />
                  <YAxis allowDecimals={false} tick={{ fill: CHART_TEXT, fontSize: 12 }} axisLine={false} tickLine={false} />
                  <Tooltip
                    cursor={{ fill: 'rgba(37, 99, 235, 0.06)' }}
                    formatter={(value) => [`${value} report${value === 1 ? '' : 's'}`, 'Count']}
                  />
                  <Legend
                    payload={SEVERITY_ORDER.map((s) => ({
                      value: SEVERITY_LABELS[s],
                      type: 'square',
                      color: SEVERITY_COLORS[s],
                    }))}
                  />
                  <Bar dataKey="count" radius={[4, 4, 0, 0]} maxBarSize={64}>
                    {severityData.map((entry) => (
                      <Cell key={entry.severity} fill={SEVERITY_COLORS[entry.severity]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        <div className="col-lg-7">
          <div className="card p-4 h-100">
            <h2 className="h6 fw-semibold mb-3">Reports Over Time</h2>
            {monthlyData.length === 0 ? (
              <div className="empty-state py-4 small">No reports yet.</div>
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <LineChart data={monthlyData} margin={{ top: 8, right: 16, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke={CHART_GRID} vertical={false} />
                  <XAxis dataKey="month" tick={{ fill: CHART_TEXT, fontSize: 12 }} axisLine={{ stroke: CHART_GRID }} tickLine={false} />
                  <YAxis allowDecimals={false} tick={{ fill: CHART_TEXT, fontSize: 12 }} axisLine={false} tickLine={false} />
                  <Tooltip formatter={(value) => [`${value} report${value === 1 ? '' : 's'}`, 'Reports']} />
                  <Line
                    type="monotone"
                    dataKey="count"
                    name="Reports"
                    stroke={CHART_PRIMARY}
                    strokeWidth={2}
                    dot={{ r: 4, fill: CHART_PRIMARY }}
                    activeDot={{ r: 6 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      </div>

      <div className="card p-4">
        <h2 className="h6 fw-semibold mb-3">Top Affected Areas</h2>
        {areaData.length === 0 ? (
          <div className="empty-state py-4 small">No reports yet.</div>
        ) : (
          <ResponsiveContainer width="100%" height={Math.max(180, areaData.length * 48)}>
            <BarChart
              data={areaData}
              layout="vertical"
              margin={{ top: 0, right: 24, left: 0, bottom: 0 }}
            >
              <CartesianGrid strokeDasharray="3 3" stroke={CHART_GRID} horizontal={false} />
              <XAxis type="number" allowDecimals={false} tick={{ fill: CHART_TEXT, fontSize: 12 }} axisLine={false} tickLine={false} />
              <YAxis
                type="category"
                dataKey="area"
                width={180}
                tick={{ fill: CHART_TEXT, fontSize: 12 }}
                axisLine={false}
                tickLine={false}
              />
              <Tooltip formatter={(value) => [`${value} report${value === 1 ? '' : 's'}`, 'Reports']} />
              <Bar dataKey="count" name="Reports" fill={CHART_PRIMARY} radius={[0, 4, 4, 0]} maxBarSize={28} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  )
}
