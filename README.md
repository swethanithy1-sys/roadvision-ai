# RoadVision AI

AI-powered Smart Road Infrastructure Monitoring and Maintenance Management System. Citizens report road damage with photos; a pluggable AI detection service (mock by default, or a real detector via Roboflow's hosted API or a self-hosted FastAPI+YOLOv8 microservice) classifies severity, estimates repair cost, and feeds municipal dashboards, a hazard map, and repair workflows.

> **Status: Phase 5 of 5 — complete.** Every module from the original spec is implemented and verified end-to-end: auth, reporting + mock AI detection, citizen dashboard, Analytics, Hazard Map, Admin/Repair Management/Work Orders, plus a final polish pass (dark mode across charts/map, reusable loading/error/empty states, success toasts, responsive fixes, lazy-loaded routes). See [Roadmap](#roadmap) for what shipped in each phase.

## Architecture

```
Browser (React 18 + Vite)
        │  REST (JSON) + JWT bearer token (issued by Supabase Auth)
        ▼
Spring Boot 3 API  ──────────────►  Supabase Postgres
        │                            (auth.users + app tables)
        └──► Supabase Auth · Supabase Storage · Resend · Roboflow · Gemini
  Controller → Service → Repository → Entity
  │
  ├── security/    Supabase JWT verification (JWKS), role resolution, Spring Security filter chain
  ├── auth/        register / verify-otp / set-password / login / forgot+reset password (proxies Supabase Auth)
  ├── user/        user profile (mirrors auth.users)
  ├── detection/    AIDetectionService interface → Mock (default), Roboflow-hosted, or self-hosted ai-service/ impl
  ├── storage/      FileStorageService interface → LocalFileStorageServiceImpl (default) or SupabaseStorageServiceImpl
  ├── email/        EmailService interface → LogEmailServiceImpl (default) or ResendEmailServiceImpl
  ├── report/       submit / list / get / status timeline / map markers / admin list+status+priority, RepairEstimator
  ├── analytics/    citizen + admin dashboard summaries, city-wide overview (aggregated in-memory from ReportRepository)
  ├── workorder/    printable work order generation (materials/labor/duration from RepairEstimator); race-safe create-or-fetch via an isolated REQUIRES_NEW transaction
  └── common/       ApiResponse<T> envelope, GlobalExceptionHandler, shared enums
```

Every endpoint returns a consistent envelope:

```json
{ "success": true, "message": "...", "data": { ... }, "timestamp": "..." }
```

The AI layer is designed so `MockAIDetectionServiceImpl` can be swapped for a real detection backend purely by adding a new `AIDetectionService` implementation — no frontend or controller changes required. Same pattern for file storage (`FileStorageService`) and email (`EmailService`) — see below.

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

### Repair Cost Estimation

Estimated repair cost is a separate concern from damage detection, behind its own `CostEstimator` abstraction — same pluggable-provider pattern as `AIDetectionService`.

- **Rule-based (default, `COST_PROVIDER=rule` or unset)** — `RuleBasedCostEstimatorImpl` delegates to `RepairEstimator`'s deterministic formula: a base cost per damage type (pothole/crack/surface damage) multiplied by a severity multiplier (LOW ×1.0, MEDIUM ×1.6, HIGH ×2.4). Free, instant, no external dependency.
- **Gemini LLM-based (optional, `COST_PROVIDER=gemini`)** — `GeminiCostEstimatorImpl` asks Google's free Gemini API (`gemini-3.5-flash-lite` by default) to estimate a realistic INR repair cost from the damage type, severity, AI confidence, and report location/description, via the Gemini Interactions API with a JSON response schema constraining the output shape. The reply text is nested inside `steps[]` (the `model_output` step's `content[].text`), parsed into `GeminiCostEstimate`. **On any failure** (network error, malformed response, rate limit) it transparently falls back to the same rule-based formula above — a report submission never fails just because the LLM call did. Switch to it with:

```bash
COST_PROVIDER=gemini
GEMINI_API_KEY=<your free key from aistudio.google.com/apikey>
```

Both implementations are `@ConditionalOnProperty`-gated on `app.cost.provider` (`rule` / `gemini`), so only one is active at a time — no code changes needed to switch providers.

### Report Photo Storage

Uploaded report photos go through `FileStorageService`, same pluggable-provider pattern as everywhere else. `store()` always returns the file's final publicly-resolvable URL — callers never construct URLs themselves, which is what lets the two implementations differ so much internally.

- **Local disk (default, `STORAGE_PROVIDER=local` or unset)** — `LocalFileStorageServiceImpl` writes to `backend/uploads/` and serves it back at `UPLOAD_PUBLIC_PATH` (`/api/uploads` by default) via a Spring resource handler (`WebConfig`). Simple, but the filesystem is only as durable as the host — fine for a single long-lived server, **lost on every redeploy/restart on hosts with no persistent disk** (e.g. Render's free tier).
- **Supabase Storage (optional, `STORAGE_PROVIDER=supabase`)** — `SupabaseStorageServiceImpl` uploads to a public Supabase Storage bucket over its REST API instead, so photos survive redeploys regardless of the backend host's disk. Switch to it with:

```bash
STORAGE_PROVIDER=supabase
SUPABASE_PROJECT_URL=https://<project-ref>.supabase.co
SUPABASE_STORAGE_BUCKET=report-images   # must exist and be set to Public in the Supabase dashboard
SUPABASE_SECRET_KEY=<your secret key (sb_secret_...), Project Settings -> API Keys>
```

Use the **secret key**, not the publishable key — Storage uploads need to bypass Row Level Security, which only the secret key (the modern replacement for the legacy `service_role` key) can do. Both implementations are `@ConditionalOnProperty`-gated on `app.storage.provider` (`local` / `supabase`).

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
| Auth       | Supabase Auth (proxied server-side), ES256 JWTs verified via JWKS, role-based authorization |
| AI service (optional) | Roboflow hosted inference API (free tier), or self-hosted Python 3.11 + FastAPI + Ultralytics YOLOv8 on Hugging Face Spaces (free) |
| Cost estimation (optional) | Google's free Gemini API (`gemini-3.5-flash-lite`), with automatic fallback to a rule-based formula |
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

New citizen accounts can also self-register via `/register`; the role is always `CITIZEN` on self-registration — that's enforced in the database by the `auth.users` insert trigger, not just in application code, so an admin account can only be provisioned deliberately (the seeder, or a direct role update). After login, citizens land on `/dashboard`; admins land on `/admin` — both the frontend router (`RoleRoute`) and every admin endpoint (`@PreAuthorize("hasRole('ADMIN')")`) enforce this independently.

### Authentication (Supabase Auth + Resend)

Passwords, sessions, and email confirmation are owned by **Supabase Auth** — this app never stores or hashes a password. Two deliberate choices shape the design:

**1. Supabase is proxied through this backend, not called from the browser.** The React app only ever talks to our own `/auth/*` endpoints; `SupabaseAuthService` calls Supabase's REST API server-side. This keeps a single API surface for the frontend and keeps Supabase credentials out of the browser bundle.

**2. Verification is a one-time code, and *this app* emails it — not Supabase.** `POST /auth/register` mints the code through Supabase's Admin `generate_link` endpoint, which returns the plaintext `email_otp` **without sending anything**, and hands it to our own `EmailService` to deliver. Supabase's built-in mailer is bypassed on purpose: its email templates are only editable once custom SMTP is configured, and its default templates send a confirmation *link* rather than the code this UI asks the user to type. Doing it this way keeps the email template in version-controlled code and works on a free Supabase project with no dashboard configuration at all.

Registration is three steps, so the user only picks a password once their address is proven:

1. `POST /auth/register` — name/email/phone. Creates the unconfirmed account (with a throwaway random password) and emails an 8-digit code. No JWT is returned.
2. `POST /auth/verify-otp` — email + code. Returns a short-lived Supabase access token; the account still has no usable password.
3. `POST /auth/set-password` — that token + the chosen password. Completes the account and returns a JWT, so the user lands straight in the app.

"Forgot password" mirrors it: `POST /auth/forgot-password` mints a `recovery` code the same way, and `POST /auth/reset-password` verifies the code and sets the new password. It returns an identical generic message whether or not the email is registered, so it can't be used to discover which addresses have accounts.

Delivery sits behind a pluggable `EmailService` (`app.email.provider`, same `@ConditionalOnProperty` pattern as everywhere else):

- **Log (default, `EMAIL_PROVIDER=log` or unset)** — `LogEmailServiceImpl` prints the code to the console. Zero setup for local development.
- **Resend (optional, `EMAIL_PROVIDER=resend`)** — `ResendEmailServiceImpl` sends branded HTML email via [Resend](https://resend.com)'s free REST API (100/day, no card required):

```bash
EMAIL_PROVIDER=resend
RESEND_API_KEY=<your free key from resend.com/api-keys>
```

> Resend's **REST API** is used rather than its SMTP relay: SMTP requires a verified sending domain, while the REST API works immediately with the shared `onboarding@resend.dev` sender. That shared sender only reliably delivers to your own Resend account address, so verify a domain you own and point `RESEND_FROM_EMAIL` at it (e.g. `RoadVision AI <noreply@yourdomain.com>`) before real users sign up.

The `profiles` table is a 1:1 mirror of `auth.users`, created automatically by a Postgres trigger on insert and tied to it by a foreign key with `ON DELETE CASCADE` — deleting a Supabase Auth user cleans up its profile (and that user's reports) rather than orphaning them.

## API Reference

| Method | Endpoint                    | Auth        | Description                                    |
|--------|-------------------------------|-------------|--------------------------------------------------|
| POST   | `/api/auth/register`         | Public      | Step 1 — create the unconfirmed account, email an 8-digit code |
| POST   | `/api/auth/verify-otp`       | Public      | Step 2 — verify the code, returns a short-lived access token |
| POST   | `/api/auth/set-password`     | Public      | Step 3 — set the password with that token, returns JWT |
| POST   | `/api/auth/login`            | Public      | Authenticate, returns JWT (401 on bad credentials) |
| POST   | `/api/auth/forgot-password`  | Public      | Email a password reset code                        |
| POST   | `/api/auth/reset-password`   | Public      | Verify the reset code and set a new password       |
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

**Backend** (`backend/.env.example`): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`, `CORS_ALLOWED_ORIGINS`, `SEED_ENABLED`, `LOG_LEVEL`, `SUPABASE_PROJECT_URL`, `SUPABASE_SECRET_KEY`, `SUPABASE_PUBLISHABLE_KEY`, `STORAGE_PROVIDER`, `UPLOAD_DIR`, `UPLOAD_PUBLIC_PATH`, `SUPABASE_STORAGE_BUCKET`, `SUPABASE_TIMEOUT_MS`, `AI_PROVIDER`, `AI_SERVICE_URL`, `AI_SERVICE_TIMEOUT_MS`, `ROBOFLOW_BASE_URL`, `ROBOFLOW_API_KEY`, `ROBOFLOW_MODEL_ID`, `ROBOFLOW_CONFIDENCE_THRESHOLD`, `COST_PROVIDER`, `GEMINI_BASE_URL`, `GEMINI_API_KEY`, `GEMINI_MODEL`, `EMAIL_PROVIDER`, `RESEND_BASE_URL`, `RESEND_API_KEY`, `RESEND_FROM_EMAIL`, `RESEND_TIMEOUT_MS`.

> There is no `JWT_SECRET` — this app doesn't issue its own JWTs. Supabase signs them (ES256) and the backend verifies them against Supabase's JWKS endpoint, so the only auth credentials to configure are the Supabase project URL and keys. `SUPABASE_SECRET_KEY` (`sb_secret_…`) is server-only; `SUPABASE_PUBLISHABLE_KEY` (`sb_publishable_…`) is the low-privilege key used for user-facing sign-in/OTP calls.

> The database must be a real Supabase Postgres project (its `auth.users` table and insert trigger are what the profile mirror depends on), connected via the **Session Pooler** — see Deployment Notes.

> Set `SEED_ENABLED=false` once you've moved past demo data — `DataSeeder`/`ReportSeeder` only insert when their tables are empty, so disabling seeding after a manual reset (`DELETE FROM ...`) keeps the fake sample reports from coming back on the next restart.

**Frontend** (`frontend/.env.example`): `VITE_API_BASE_URL`.

**AI service** (`ai-service/README.md`): `MODEL_PATH`, `CONFIDENCE_THRESHOLD`.

## Deployment Notes

Authentication is delegated to Supabase, and file storage, email, AI detection, and cost estimation are all behind pluggable abstractions (see above), so:

- **Database + Auth (Supabase)** — point `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` at the project's connection details and Flyway migrates it on first boot. **Use the Session Pooler connection** (Project Settings → Database → "Session pooler" tab — host like `aws-0-<region>.pooler.supabase.com`, port `5432`, user `postgres.<project-ref>`), not the Direct connection: Supabase's direct connection requires IPv6, which many hosts (including Render) don't reliably support outbound, so it can work fine locally and then fail to connect once deployed. A real Supabase project is required rather than any managed Postgres, since `auth.users` and the profile trigger live there. Verified end-to-end against a live project: all four migrations apply cleanly to a fresh database, and registration → OTP email → verification → set password → login → password reset all work against it with zero code changes beyond env vars.
- **File storage & email** — `STORAGE_PROVIDER=supabase` + `EMAIL_PROVIDER=resend` for a fully-managed deployment with no local disk dependency; see the "Report Photo Storage" and "Authentication" sections above for the exact env vars.
- **Backend (Render)** — deploy `backend/` as a Docker or Maven web service; set the env vars above (`DB_*` from the Supabase session pooler, `SUPABASE_PROJECT_URL`/`SUPABASE_SECRET_KEY`/`SUPABASE_PUBLISHABLE_KEY`, `CORS_ALLOWED_ORIGINS` to your Vercel domain, `STORAGE_PROVIDER=supabase` since Render's free tier has no persistent disk, `AI_PROVIDER=roboflow` + `COST_PROVIDER=gemini` for real AI, `EMAIL_PROVIDER=resend` for real email).
- **Frontend (Vercel)** — deploy `frontend/`; set `VITE_API_BASE_URL` to your Render backend's public URL + `/api`.
- **AI service (Hugging Face Spaces)** — only needed if using `AI_PROVIDER=real` instead of the simpler `AI_PROVIDER=roboflow`; see `ai-service/README.md`; set the backend's `AI_SERVICE_URL` to the Space's URL.

## Roadmap

1. **Phase 1 (done)** — Monorepo scaffold, PostgreSQL schema, authentication, role-based authorization, app shell, theme system (light/dark). *(Auth was later migrated to Supabase Auth — see [Authentication](#authentication-supabase-auth--resend).)*
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
