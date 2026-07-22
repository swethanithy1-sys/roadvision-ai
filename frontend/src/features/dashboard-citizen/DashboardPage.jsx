import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import StatCard from '../../components/StatCard'

export default function DashboardPage() {
  const { user } = useAuth()

  return (
    <div>
      <div className="mb-4">
        <h1 className="h4 fw-bold mb-1">Welcome back, {user?.fullName?.split(' ')[0]}</h1>
        <p className="text-muted-app mb-0">Here&apos;s the current state of your road reports.</p>
      </div>

      <div className="row g-3 mb-4">
        <div className="col-6 col-lg-3">
          <StatCard label="Total Reports" value="0" accent="primary" />
        </div>
        <div className="col-6 col-lg-3">
          <StatCard label="Pending Repairs" value="0" accent="accent" />
        </div>
        <div className="col-6 col-lg-3">
          <StatCard label="Resolved Reports" value="0" accent="secondary" />
        </div>
        <div className="col-6 col-lg-3">
          <StatCard label="High Severity" value="0" accent="danger" />
        </div>
      </div>

      <div className="card p-4">
        <div className="empty-state">
          <h2 className="h6 fw-semibold mb-2">Report your first road hazard</h2>
          <p className="mb-3 small">
            Upload a photo and RoadVision AI will detect the damage, classify severity, and
            estimate repair priority and cost automatically.
          </p>
          <Link to="/report-damage" className="btn btn-app-primary">
            Report damage
          </Link>
        </div>
      </div>
    </div>
  )
}
