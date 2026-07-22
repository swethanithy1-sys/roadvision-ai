# RoadVision AI

AI-powered Smart Road Infrastructure Monitoring and Maintenance Management System. Citizens report road damage with photos; a pluggable AI detection service (mock by default, or a real detector via Roboflow's hosted API or a self-hosted FastAPI+YOLOv8 microservice) classifies severity, estimates repair cost, and feeds municipal dashboards, a hazard map, and repair workflows.

> **Status: Phase 5 of 5 — complete.** Every module from the original spec is implemented and verified end-to-end: auth, reporting + mock AI detection, citizen dashboard, Analytics, Hazard Map, Admin/Repair Management/Work Orders, plus a final polish pass (dark mode across charts/map, reusable loading/error/empty states, success toasts, responsive fixes, lazy-loaded routes). See [Roadmap](#roadmap) for what shipped in each phase.

## Architecture

```
Browser (React 18 + Vite)
        │  REST (JSON) + JWT bearer token
        ▼
Spring Boot 3 API  ──────────────►  PostgreSQL 16
  Controller → Service → Repository → Entity
  │
  ├── security/    JWT issuance & validation, Spring Security filter chain
  ├── auth/        register / login
  ├── user/        user profile
  ├── detection/    AIDetectionService interface → Mock (default), Roboflow-hosted, or self-hosted ai-service/ impl
  ├── storage/      FileStorageService interface → LocalFileStorageServiceImpl
  ├── report/       submit / list / get / status timeline / map markers / admin list+status+priority, RepairEstimator
  ├── analytics/    citizen + admin dashboard summaries, city-wide overview (aggregated in-memory from ReportRepository)
  ├── workorder/    printable work order generation (materials/labor/duration from RepairEstimator); race-safe create-or-fetch via an isolated REQUIRES_NEW transaction
  └── common/       ApiResponse<T> envelope, GlobalExceptionHandler, shared enums
```

Every endpoint returns a consistent envelope:

```json
{ "success": true, "message": "...", "data": { ... }, "timestamp": "..." }
```

The AI layer is designed so `MockAIDetectionServiceImpl` can be swapped for a real detection backend purely by adding a new `AIDetectionService` implementation — no frontend or controller changes required. Same pattern for file storage: `LocalFileStorageServiceImpl` today, `S3FileStorageServiceImpl` later, behind `FileStorageService`.

### Mock AI Detection

`MockAIDetectionServiceImpl` seeds a `Random` from a SHA-256 hash of the uploaded image bytes, so **the same photo always produces the same damage type, severity, confidence, and bounding boxes** — useful for demos and repeatable testing — while different photos plausibly vary. Repair priority and estimated cost are computed separately by `RepairEstimator` from the detected severity/damage type, kept out of the AI service since that's pricing/policy logic, not computer vision. This is the active implementation by default (`AI_PROVIDER=mock`).

### Real AI Detection (optional)

`ai-service/` is a standalone FastAPI + YOLOv8 microservice (deployable free on Hugging Face Spaces — see `ai-service/README.md`) that does the actual computer-vision inference. `RealAIDetectionServiceImpl` calls it over HTTP, then applies the same severity-classification responsibility the mock has (box size + confidence → LOW/MEDIUM/HIGH) — the Python service intentionally returns only raw detections (class name, confidence, box), keeping business rules in the backend. Switch to it with:

```bash
AI_PROVIDER=real
AI_SERVICE_URL=https://<your-space>.hf.space   # or http://localhost:8000 for local docker compose
```

### Roboflow Hosted AI Detection (optional, recommended for a quick real-model demo)

`RoboflowAIDetectionServiceImpl` calls [Roboflow](https://roboflow.com)'s hosted inference API directly over REST — no self-hosting, no training. Roboflow's free tier is 15 credits/month (≈15,000 inference calls, since 1 credit = 1,000 calls), more than enough for a project like this. Note: Roboflow only lets you export raw `.pt` weights for models trained under *your own* workspace — public Universe models are hosted-inference-only, which is exactly what this implementation uses. Switch to it with:

```bash
AI_PROVIDER=roboflow
ROBOFLOW_API_KEY=<your private API key, from Workspace Settings -> API Keys>
ROBOFLOW_MODEL_ID=<project-slug>/<version>   # e.g. pathole-aynu3/1, from a Universe project's "Deploy Model" modal
```

Roboflow returns pixel-space, center-point boxes (`x`, `y`, `width`, `height`, `confidence`, `class`); `RoboflowAIDetectionServiceImpl` converts these to our normalized top-left contract and applies the same severity heuristic (box-size + confidence) as the other implementations.

All three implementations are `@ConditionalOnProperty`-gated on `app.ai.provider` (`mock` / `real` / `roboflow`) so only one is ever active — no code changes needed to switch, just the env var. Locally, `docker compose --profile ai up` builds and runs `ai-service/` alongside Postgres (requires a `pothole.pt` weights file in `ai-service/weights/` — gitignored, not committed; see `ai-service/README.md` for where to get one free).

Uploaded images are written to `backend/uploads/reports/` and served back at `/api/uploads/reports/<file>` via a Spring resource handler (see `WebConfig`).

### Location: GPS + Reverse Geocoding

"Use my current location" on the Report Damage page fills latitude/longitude from the browser's Geolocation API, then reverse-geocodes those coordinates into a human-readable address via OpenStreetMap's free Nominatim API (`frontend/src/api/geocode.js`) — same free stack as the Hazard Map, no API key needed. The address field stays editable afterward if Nominatim's guess isn't quite right.

### Road Safety Score

Computed by `AnalyticsService` (not a stored value) as `100 − average severity penalty` across a scope's **currently unresolved** reports (LOW=5, MEDIUM=15, HIGH=30 penalty); resolved/rejected reports no longer count as live hazards. The same formula powers both the per-citizen score (`/analytics/citizen-summary`) and the city-wide Road Safety Index (`/analytics/overview`) — just over a different report set.

## Tech Stack

| Layer      | Technology                                                        |
|------------|--------------------------------------------------------------------|
| Frontend   | React 18, Vite, React Router v6, Axios, Bootstrap 5 + CSS variables |
| Backend    | Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA, Hibernate, Lombok, Flyway |
| Database   | PostgreSQL 16                                                     |
| Maps       | Leaflet + react-leaflet + OpenStreetMap tiles                      |
| Charts     | Recharts 3                                                         |
| Auth       | JWT (stateless), BCrypt password hashing, role-based authorization |
| AI service (optional) | Roboflow hosted inference API (free tier), or self-hosted Python 3.11 + FastAPI + Ultralytics YOLOv8 on Hugging Face Spaces (free) |
| Geocoding  | OpenStreetMap Nominatim (free, reverse geocoding for GPS-based reports)          |

## Project Structure

```
roadvision-ai/
├── backend/       Spring Boot API (Maven)
├── frontend/      React + Vite SPA
├── ai-service/    Optional FastAPI + YOLOv8 detection microservice
└── docker-compose.yml   PostgreSQL + pgAdmin (+ ai-service under the "ai" profile)
```

Backend package layout follows a **feature-package** convention (`auth/`, `user/`, `report/`, ...) with each feature internally layered Controller → Service → Repository → DTO/Mapper, plus a `common/` package for cross-cutting concerns (response envelope, exceptions, shared enums) and `security/`/`config/` for infrastructure. Frontend mirrors this with `features/<name>/` per module, `components/` for shared UI, `api/` for Axios clients, and `auth/` for session state.

## Prerequisites

- Java 21 (Temurin recommended)
- Maven 3.8+
- Node.js 20+
- Docker Desktop (for PostgreSQL via Compose)

## Getting Started

### 1. Start the database

```bash
docker compose up -d postgres
```

This starts PostgreSQL on `localhost:5432` (db `roadvision`, user/password `roadvision`) and pgAdmin on `http://localhost:5050` (`admin@roadvision.ai` / `admin`).

### 2. Run the backend

```bash
cd backend
mvn spring-boot:run
```

The API starts at `http://localhost:8080/api`. On first run:
- Flyway applies `V1__init_schema.sql` automatically.
- `DataSeeder` seeds three demo accounts (see below) since the `users` table starts empty.
- `ReportSeeder` seeds 9 sample reports spanning all severities, statuses, and the last ~5 months (with generated placeholder photos) so the dashboard, analytics, and hazard map aren't empty on first run.

Configuration lives in `backend/src/main/resources/application.yml`, all overridable via environment variables — see `backend/.env.example`.

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The app starts at `http://localhost:5173` and talks to the backend at `VITE_API_BASE_URL` (defaults to `http://localhost:8080/api`, see `frontend/.env.example`).

### Demo accounts

| Role    | Email                    | Password        |
|---------|---------------------------|------------------|
| Admin   | admin@roadvision.ai       | Password123!     |
| Citizen | citizen1@roadvision.ai    | Password123!     |
| Citizen | citizen2@roadvision.ai    | Password123!     |

New citizen accounts can also self-register via `/register`; the API always assigns the `CITIZEN` role on self-registration (admin accounts are provisioned via the seeder only, by design). After login, citizens land on `/dashboard`; admins land on `/admin` — both the frontend router (`RoleRoute`) and every admin endpoint (`@PreAuthorize("hasRole('ADMIN')")`) enforce this independently.

## API Reference

| Method | Endpoint                    | Auth        | Description                                    |
|--------|-------------------------------|-------------|--------------------------------------------------|
| POST   | `/api/auth/register`         | Public      | Create a citizen account, returns JWT             |
| POST   | `/api/auth/login`            | Public      | Authenticate, returns JWT                         |
| GET    | `/api/users/me`              | Bearer JWT  | Current authenticated user profile                |
| GET    | `/api/users/admin/ping`      | Bearer JWT (ADMIN) | Role-guard smoke test                      |
| POST   | `/api/reports`               | Bearer JWT  | Submit a report (multipart: `image` + `report` JSON part); runs mock AI detection synchronously |
| GET    | `/api/reports/mine`          | Bearer JWT  | Paginated list of the current user's reports (`?page=&size=`) |
| GET    | `/api/reports/{id}`          | Bearer JWT  | Report detail (owner or ADMIN only)               |
| GET    | `/api/reports/{id}/timeline` | Bearer JWT  | Status change history for a report                |
| GET    | `/api/reports/map`           | Bearer JWT  | All reports with coordinates, for the hazard map (reporter identity omitted) |
| GET    | `/api/analytics/citizen-summary` | Bearer JWT | Current user's dashboard stats + road safety score + recent activity |
| GET    | `/api/analytics/overview`    | Bearer JWT  | City-wide severity breakdown, monthly trend, resolution rate, avg. confidence, top affected areas, road safety index |
| GET    | `/api/reports`               | Bearer JWT (ADMIN) | All reports, paginated, optional `?status=` filter |
| PATCH  | `/api/reports/{id}/status`   | Bearer JWT (ADMIN) | Update status (`{ status, note }`); appends to the status timeline; `ASSIGNED` records the acting admin |
| PATCH  | `/api/reports/{id}/priority` | Bearer JWT (ADMIN) | Override the repair priority (`{ repairPriority }`) |
| GET    | `/api/analytics/admin-summary` | Bearer JWT (ADMIN) | Total/pending reports, high-priority count, total estimated outstanding cost, recent reports |
| POST   | `/api/reports/{id}/work-order` | Bearer JWT (ADMIN) | Generate a work order for a report (idempotent — returns the existing one if already generated) |
| GET    | `/api/reports/{id}/work-order` | Bearer JWT (ADMIN) | Fetch a report's work order (404 if none generated yet) |

`POST /api/reports` expects `multipart/form-data` with two parts: `image` (the photo file) and `report` (a JSON blob: `{ latitude, longitude, addressText, description }`, all optional except the image).

## Environment Variables

**Backend** (`backend/.env.example`): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `CORS_ALLOWED_ORIGINS`, `UPLOAD_DIR`, `UPLOAD_PUBLIC_PATH`, `SEED_ENABLED`, `LOG_LEVEL`, `AI_PROVIDER`, `AI_SERVICE_URL`, `AI_SERVICE_TIMEOUT_MS`, `ROBOFLOW_BASE_URL`, `ROBOFLOW_API_KEY`, `ROBOFLOW_MODEL_ID`, `ROBOFLOW_CONFIDENCE_THRESHOLD`.

> Set `SEED_ENABLED=false` once you've moved past demo data — `DataSeeder`/`ReportSeeder` only insert when their tables are empty, so disabling seeding after a manual reset (`DELETE FROM ...`) keeps the fake sample reports from coming back on the next restart.

**Frontend** (`frontend/.env.example`): `VITE_API_BASE_URL`.

**AI service** (`ai-service/README.md`): `MODEL_PATH`, `CONFIDENCE_THRESHOLD`.

> `JWT_SECRET` ships with a development-only default — set a strong secret via environment variable before any real deployment.

## Deployment Notes

The app's own JWT auth and local-disk file storage are provider-agnostic, so:

- **Database (Supabase, or any managed Postgres)** — Supabase's Postgres is just Postgres; point `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` at its connection details and Flyway migrates it on first boot exactly like local Postgres. (This app doesn't use Supabase's own Auth or Storage products — it has its own JWT auth and `FileStorageService` abstraction. Swapping file storage to Supabase Storage or S3 later is a new `FileStorageService` implementation, same pattern as the AI provider switch above.)
- **Backend (Render)** — deploy `backend/` as a Docker or Maven web service; set the env vars above (`DB_*`, `JWT_SECRET` — generate a real one, don't ship the dev default, `CORS_ALLOWED_ORIGINS` to your Vercel domain, `AI_PROVIDER=real` + `AI_SERVICE_URL` if using the AI microservice).
- **Frontend (Vercel)** — deploy `frontend/`; set `VITE_API_BASE_URL` to your Render backend's public URL + `/api`.
- **AI service (Hugging Face Spaces)** — see `ai-service/README.md`; set the backend's `AI_SERVICE_URL` to the Space's URL.

## Roadmap

1. **Phase 1 (done)** — Monorepo scaffold, PostgreSQL schema, JWT auth, role-based authorization, app shell, theme system (light/dark).
2. **Phase 2 (done)** — Report submission (image upload/capture + GPS/manual location), mock AI detection (deterministic, per-image), automatic repair priority/cost estimation, My Reports, Report Details with bounding-box overlay and status timeline.
3. **Phase 3 (done)** — Live citizen dashboard (stats, road safety score, recent activity), Analytics dashboard (severity breakdown, monthly trend, resolution rate, avg. confidence, top affected areas — Recharts), Hazard Map (Leaflet/OSM, severity-colored markers, popup details).
4. **Phase 4 (done)** — Admin Dashboard (fleet-wide stats + recent reports), Repair Management (list/filter all reports, assign priority, update status), printable Work Order generation (materials, labor, duration, signature lines).
5. **Phase 5 (done)** — Polish pass: theme-aware charts and map tiles in dark mode (Recharts axes/tooltips/grid, Leaflet tile filter + popup styling), reusable `LoadingState`/`ErrorState`/`EmptyState`/`Toast` components applied across every page, success toasts on Repair Management actions, a null-pointer fix on the citizen dashboard's error path, responsive fixes (work order materials list, detail list, hazard map height on small screens), and route-level code-splitting for the Recharts/Leaflet pages (818 KB → 255 KB main bundle).

**Post-launch additions**: real AI detection via Roboflow's hosted API and/or a self-hosted `ai-service/` (FastAPI+YOLOv8), reverse geocoding on GPS-based reports (OpenStreetMap Nominatim), and a fix for a work-order creation race condition (concurrent "generate" requests for the same report could hit the DB's unique constraint and leak a raw SQL error — `WorkOrderWriter` now isolates the insert attempt in its own transaction and gracefully falls back to the winning row on conflict).

## Design System

Theme tokens live in `frontend/src/styles/theme.css` as CSS custom properties (`--color-primary: #2563EB`, `--color-secondary: #10B981`, `--color-accent: #F59E0B`, `--color-bg: #F8FAFC`), with a dark-mode override block and a `data-theme` toggle persisted to `localStorage`. Bootstrap 5 supplies grid/utility classes only; visual identity (cards, buttons, pills, stat tiles) is defined in `global.css`/`shell.css` on top of the token layer.

Recharts and Leaflet render to `<canvas>`/SVG, so they can't read CSS variables directly — `frontend/src/hooks/useColorScheme.js` tracks the active theme (manual toggle + OS preference) so `AnalyticsPage` and `HazardMapPage` can pick matching colors/tile filters per scheme instead of hardcoding one.

## UI Building Blocks

Every data-fetching page follows the same shape — a `load()` callback (retryable), and `LoadingState` / `ErrorState` / `EmptyState` from `frontend/src/components/` for the three non-happy-path states, so the pattern only had to be designed once:

```jsx
const load = useCallback(() => { setLoading(true); setError(''); fetchX().then(setData).catch(e => setError(e.friendlyMessage)).finally(() => setLoading(false)) }, [])
useEffect(() => { load() }, [load])

if (loading) return <LoadingState label="…" />
if (error) return <ErrorState message={error} onRetry={load} />
// empty-list case: <EmptyState title=".." description=".." action={<Link .. />} />
```

`components/Toast.jsx` gives lightweight, auto-dismissing success feedback (used on Repair Management's status/priority updates); `ApiResponse`'s validation-error shape flows through to inline `FormField` errors on the auth/report forms.
