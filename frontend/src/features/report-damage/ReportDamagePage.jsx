import { useCallback, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import AlertBanner from '../../components/AlertBanner'
import BoundingBoxOverlay from '../../components/BoundingBoxOverlay'
import FormField from '../../components/FormField'
import SeverityPill from '../../components/SeverityPill'
import { assetUrl } from '../../api/assetUrl'
import { reverseGeocode } from '../../api/geocode'
import { submitReport } from '../../api/reportApi'

const INITIAL_FORM = { addressText: '', description: '', latitude: '', longitude: '' }

export default function ReportDamagePage() {
  const fileInputRef = useRef(null)
  const cameraInputRef = useRef(null)

  const [imageFile, setImageFile] = useState(null)
  const [previewUrl, setPreviewUrl] = useState(null)
  const [form, setForm] = useState(INITIAL_FORM)
  const [locating, setLocating] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState(null)

  const handleFileSelected = useCallback((e) => {
    const file = e.target.files?.[0]
    if (!file) return
    setImageFile(file)
    setPreviewUrl(URL.createObjectURL(file))
    setResult(null)
    setError('')
  }, [])

  function handleChange(e) {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  function handleUseGps() {
    if (!navigator.geolocation) {
      setError('Geolocation is not supported by this browser. Enter the address manually.')
      return
    }

    setLocating(true)
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const latitude = position.coords.latitude.toFixed(6)
        const longitude = position.coords.longitude.toFixed(6)
        setForm((prev) => ({ ...prev, latitude, longitude }))

        try {
          const address = await reverseGeocode(latitude, longitude)
          setForm((prev) => ({ ...prev, addressText: address }))
        } catch {
          // Non-fatal — coordinates are already filled in; user can type the address manually.
        } finally {
          setLocating(false)
        }
      },
      () => {
        setError('Unable to fetch your location. Please enter the address manually.')
        setLocating(false)
      }
    )
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')

    if (!imageFile) {
      setError('Please upload or capture a photo of the damage first.')
      return
    }

    setSubmitting(true)
    try {
      const response = await submitReport({
        image: imageFile,
        latitude: form.latitude ? Number(form.latitude) : null,
        longitude: form.longitude ? Number(form.longitude) : null,
        addressText: form.addressText || null,
        description: form.description || null,
      })
      setResult(response)
    } catch (err) {
      setError(err.friendlyMessage || 'Unable to submit this report. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  function handleReset() {
    setImageFile(null)
    setPreviewUrl(null)
    setForm(INITIAL_FORM)
    setResult(null)
    setError('')
    if (fileInputRef.current) fileInputRef.current.value = ''
    if (cameraInputRef.current) cameraInputRef.current.value = ''
  }

  if (result) {
    return (
      <div className="row justify-content-center">
        <div className="col-lg-8">
          <div className="card p-4 p-md-5">
            <div className="d-flex align-items-center gap-2 mb-4">
              <span className="result-check">✓</span>
              <h1 className="h5 fw-bold mb-0">Report submitted and analyzed</h1>
            </div>

            <BoundingBoxOverlay imageUrl={assetUrl(result.imageUrl)} boundingBoxes={result.boundingBoxes} />

            <div className="row g-3 mt-1 mb-4">
              <div className="col-6 col-md-3">
                <div className="detail-tile">
                  <div className="detail-tile__label">Severity</div>
                  <SeverityPill severity={result.severity} />
                </div>
              </div>
              <div className="col-6 col-md-3">
                <div className="detail-tile">
                  <div className="detail-tile__label">Confidence</div>
                  <div className="detail-tile__value">{result.confidenceScore}%</div>
                </div>
              </div>
              <div className="col-6 col-md-3">
                <div className="detail-tile">
                  <div className="detail-tile__label">Repair Priority</div>
                  <div className="detail-tile__value">{result.repairPriority}</div>
                </div>
              </div>
              <div className="col-6 col-md-3">
                <div className="detail-tile">
                  <div className="detail-tile__label">Est. Cost</div>
                  <div className="detail-tile__value">₹{result.estimatedCost}</div>
                </div>
              </div>
            </div>

            <div className="d-flex gap-2 flex-wrap">
              <Link to={`/my-reports/${result.id}`} className="btn btn-app-primary px-4">
                View full report
              </Link>
              <button type="button" className="btn btn-outline-secondary px-4" onClick={handleReset}>
                Report another
              </button>
            </div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="row justify-content-center">
      <div className="col-lg-8">
        <div className="mb-4">
          <h1 className="h4 fw-bold mb-1">Report road damage</h1>
          <p className="text-muted-app mb-0">
            Upload or capture a photo — RoadVision AI will detect and classify the damage automatically.
          </p>
        </div>

        <AlertBanner>{error}</AlertBanner>

        <form onSubmit={handleSubmit} noValidate>
          <div className="card p-4 mb-3">
            {previewUrl ? (
              <div className="mb-3">
                <img src={previewUrl} alt="Selected damage preview" className="img-fluid rounded-3" />
              </div>
            ) : (
              <div className="upload-placeholder mb-3">
                <span>No photo selected yet</span>
              </div>
            )}

            <div className="d-flex gap-2 flex-wrap">
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={() => fileInputRef.current?.click()}
              >
                Upload photo
              </button>
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={() => cameraInputRef.current?.click()}
              >
                Capture with camera
              </button>
            </div>

            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className="d-none"
              onChange={handleFileSelected}
            />
            <input
              ref={cameraInputRef}
              type="file"
              accept="image/*"
              capture="environment"
              className="d-none"
              onChange={handleFileSelected}
            />
          </div>

          <div className="card p-4 mb-3">
            <h2 className="h6 fw-semibold mb-3">Location</h2>
            <div className="row g-3">
              <div className="col-12">
                <FormField label="Address (optional)" htmlFor="addressText">
                  <input
                    id="addressText"
                    name="addressText"
                    className="form-control"
                    placeholder="e.g. MG Road, near City Mall"
                    value={form.addressText}
                    onChange={handleChange}
                  />
                </FormField>
              </div>
              <div className="col-6">
                <FormField label="Latitude" htmlFor="latitude">
                  <input
                    id="latitude"
                    name="latitude"
                    type="number"
                    step="any"
                    className="form-control"
                    value={form.latitude}
                    onChange={handleChange}
                  />
                </FormField>
              </div>
              <div className="col-6">
                <FormField label="Longitude" htmlFor="longitude">
                  <input
                    id="longitude"
                    name="longitude"
                    type="number"
                    step="any"
                    className="form-control"
                    value={form.longitude}
                    onChange={handleChange}
                  />
                </FormField>
              </div>
            </div>
            <button
              type="button"
              className="btn btn-sm btn-outline-secondary align-self-start"
              onClick={handleUseGps}
              disabled={locating}
            >
              {locating ? 'Locating…' : '📍 Use my current location'}
            </button>
          </div>

          <div className="card p-4 mb-4">
            <FormField label="Description (optional)" htmlFor="description">
              <textarea
                id="description"
                name="description"
                className="form-control"
                rows={4}
                placeholder="Any additional details about the damage or hazard..."
                value={form.description}
                onChange={handleChange}
              />
            </FormField>
          </div>

          <button type="submit" className="btn btn-app-primary px-4 py-2" disabled={submitting}>
            {submitting ? 'Analyzing photo…' : 'Detect damage & submit'}
          </button>
        </form>
      </div>
    </div>
  )
}
