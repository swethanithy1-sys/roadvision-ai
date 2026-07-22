import { useCallback, useEffect, useState } from 'react'
import { CircleMarker, MapContainer, Popup, TileLayer } from 'react-leaflet'
import ErrorState from '../../components/ErrorState'
import SeverityPill from '../../components/SeverityPill'
import StatusPill from '../../components/StatusPill'
import { assetUrl } from '../../api/assetUrl'
import { fetchReportMap } from '../../api/reportApi'
import { useColorScheme } from '../../hooks/useColorScheme'
import { SEVERITY_COLORS } from '../analytics/chartTheme'

const DEFAULT_CENTER = [12.9716, 77.5946] // Bengaluru
const DEFAULT_ZOOM = 12

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export default function HazardMapPage() {
  const [markers, setMarkers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const scheme = useColorScheme()

  const load = useCallback(() => {
    setLoading(true)
    setError('')

    fetchReportMap()
      .then(setMarkers)
      .catch((err) => setError(err.friendlyMessage || 'Failed to load the hazard map.'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const center = markers.length > 0 ? [markers[0].latitude, markers[0].longitude] : DEFAULT_CENTER

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
        <div>
          <h1 className="h4 fw-bold mb-1">Hazard Map</h1>
          <p className="text-muted-app mb-0">Reported road damage across the city, colored by severity.</p>
        </div>
        <div className="d-flex gap-3 small">
          <span className="legend-dot">
            <span className="legend-dot__swatch" style={{ backgroundColor: SEVERITY_COLORS.LOW }} />
            Low
          </span>
          <span className="legend-dot">
            <span className="legend-dot__swatch" style={{ backgroundColor: SEVERITY_COLORS.MEDIUM }} />
            Medium
          </span>
          <span className="legend-dot">
            <span className="legend-dot__swatch" style={{ backgroundColor: SEVERITY_COLORS.HIGH }} />
            High
          </span>
        </div>
      </div>

      {error && <ErrorState message={error} onRetry={load} />}

      {!loading && !error && markers.length === 0 && (
        <div className="alert alert-info small mb-3">No hazards have been reported yet — the map will populate as citizens submit reports.</div>
      )}

      <div className="card p-2 hazard-map-card">
        {loading ? (
          <div className="text-center text-muted-app py-5">Loading map…</div>
        ) : (
          <MapContainer center={center} zoom={DEFAULT_ZOOM} scrollWheelZoom className="hazard-map">
            <TileLayer
              className={scheme === 'dark' ? 'hazard-map__tiles--dark' : ''}
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            {markers.map((marker) => (
              <CircleMarker
                key={marker.id}
                center={[marker.latitude, marker.longitude]}
                radius={9}
                pathOptions={{
                  color: '#fff',
                  weight: 2,
                  fillColor: SEVERITY_COLORS[marker.severity],
                  fillOpacity: 0.9,
                }}
              >
                <Popup minWidth={220}>
                  <div className="map-popup">
                    <img src={assetUrl(marker.imageUrl)} alt="Reported damage" className="map-popup__image" />
                    <div className="d-flex gap-2 align-items-center mb-1">
                      <SeverityPill severity={marker.severity} />
                      <StatusPill status={marker.status} />
                    </div>
                    <div className="fw-semibold small">{marker.addressText || 'Location not specified'}</div>
                    <div className="small text-muted-app">
                      {marker.damageType.replace('_', ' ')} · {marker.confidenceScore}% confidence
                    </div>
                    <div className="small text-muted-app">
                      Priority: {marker.repairPriority} · Est. ₹{marker.estimatedCost}
                    </div>
                    <div className="small text-muted-app">{formatDate(marker.createdAt)}</div>
                  </div>
                </Popup>
              </CircleMarker>
            ))}
          </MapContainer>
        )}
      </div>
    </div>
  )
}
