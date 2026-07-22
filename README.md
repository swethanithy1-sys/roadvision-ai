# RoadVision AI

AI-powered Smart Road Infrastructure Monitoring and Maintenance Management System. Citizens report road damage with photos; a pluggable AI detection service (mock today, YOLOv8/OpenCV-ready tomorrow) classifies severity, estimates repair cost, and feeds municipal dashboards, a hazard map, and repair workflows.

> **Status: Phase 2 of 5** — scaffold, auth, report submission, mock AI detection, My Reports, and Report Details are complete and verified end-to-end. Dashboards analytics, the hazard map, and admin/repair workflows are built in subsequent phases (see [Roadmap](#roadmap)).

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
  ├── detection/    AIDetectionService interface → MockAIDetectionServiceImpl
  ├── storage/      FileStorageService interface → LocalFileStorageServiceImpl
  ├── report/       submit / list / get / status timeline, RepairEstimator
  └── common/       ApiResponse<T> envelope, GlobalExceptionHandler, shared enums
```

Every endpoint returns a consistent envelope:

```json
{ "success": true, "message": "...", "data": { ... }, "timestamp": "..." }
```

The AI layer is designed so `MockAIDetectionServiceImpl` can be swapped for a real FastAPI/YOLOv8 service later purely by adding a new `AIDetectionService` implementation — no frontend or controller changes required. Same pattern for file storage: `LocalFileStorageServiceImpl` today, `S3FileStorageServiceImpl` later, behind `FileStorageService`.

### Mock AI Detection

`MockAIDetectionServiceImpl` seeds a `Random` from a SHA-256 hash of the uploaded image bytes, so **the same photo always produces the same damage type, severity, confidence, and bounding boxes** — useful for demos and repeatable testing — while different photos plausibly vary. Repair priority and estimated cost are computed separately by `RepairEstimator` from the detected severity/damage type, kept out of the AI service since that's pricing/policy logic, not computer vision.

Uploaded images are written to `backend/uploads/reports/` and served back at `/api/uploads/reports/<file>` via a Spring resource handler (see `WebConfig`).

## Tech Stack

| Layer      | Technology                                                        |
|------------|--------------------------------------------------------------------|
| Frontend   | React 18, Vite, React Router v6, Axios, Bootstrap 5 + CSS variables |
| Backend    | Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA, Hibernate, Lombok, Flyway |
| Database   | PostgreSQL 16                                                     |
| Maps       | Leaflet + OpenStreetMap (Phase 3)                                  |
| Charts     | Recharts (Phase 3)                                                 |
| Auth       | JWT (stateless), BCrypt password hashing, role-based authorization |

## Project Structure

```
roadvision-ai/
├── backend/     Spring Boot API (Maven)
├── frontend/    React + Vite SPA
└── docker-compose.yml   PostgreSQL + pgAdmin
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

New citizen accounts can also self-register via `/register`; the API always assigns the `CITIZEN` role on self-registration (admin accounts are provisioned via the seeder only, by design).

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

`POST /api/reports` expects `multipart/form-data` with two parts: `image` (the photo file) and `report` (a JSON blob: `{ latitude, longitude, addressText, description }`, all optional except the image).

## Environment Variables

**Backend** (`backend/.env.example`): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `CORS_ALLOWED_ORIGINS`, `UPLOAD_DIR`, `UPLOAD_PUBLIC_PATH`, `SEED_ENABLED`, `LOG_LEVEL`.

**Frontend** (`frontend/.env.example`): `VITE_API_BASE_URL`.

> `JWT_SECRET` ships with a development-only default — set a strong secret via environment variable before any real deployment.

## Roadmap

1. **Phase 1 (done)** — Monorepo scaffold, PostgreSQL schema, JWT auth, role-based authorization, app shell, theme system (light/dark).
2. **Phase 2 (done)** — Report submission (image upload/capture + GPS/manual location), mock AI detection (deterministic, per-image), automatic repair priority/cost estimation, My Reports, Report Details with bounding-box overlay and status timeline.
3. **Phase 3** — Citizen dashboard live stats, Analytics dashboard (Recharts), Hazard Map (Leaflet/OSM).
4. **Phase 4** — Admin dashboard, Repair Management, Work Order generation (printable).
5. **Phase 5** — Polish: dark mode completeness, responsive audit, empty/loading/error states, final documentation pass.

## Design System

Theme tokens live in `frontend/src/styles/theme.css` as CSS custom properties (`--color-primary: #2563EB`, `--color-secondary: #10B981`, `--color-accent: #F59E0B`, `--color-bg: #F8FAFC`), with a dark-mode override block and a `data-theme` toggle persisted to `localStorage`. Bootstrap 5 supplies grid/utility classes only; visual identity (cards, buttons, pills, stat tiles) is defined in `global.css`/`shell.css` on top of the token layer.
