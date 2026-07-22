CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name       VARCHAR(150)  NOT NULL,
    email           VARCHAR(255)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255)  NOT NULL,
    phone           VARCHAR(20),
    role            VARCHAR(20)   NOT NULL CHECK (role IN ('CITIZEN', 'ADMIN')),
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE reports (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    image_path          VARCHAR(500) NOT NULL,
    latitude            DOUBLE PRECISION,
    longitude           DOUBLE PRECISION,
    address_text        VARCHAR(500),
    description          TEXT,
    damage_type         VARCHAR(30)  NOT NULL CHECK (damage_type IN ('POTHOLE', 'CRACK', 'SURFACE_DAMAGE')),
    severity            VARCHAR(20)  NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    confidence_score     NUMERIC(5,2) NOT NULL CHECK (confidence_score >= 0 AND confidence_score <= 100),
    bounding_boxes       JSONB        NOT NULL DEFAULT '[]',
    repair_priority      VARCHAR(20)  NOT NULL CHECK (repair_priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    estimated_cost       NUMERIC(12,2) NOT NULL DEFAULT 0,
    status               VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED'
                          CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED')),
    assigned_to          UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE report_status_history (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id     UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    status        VARCHAR(20) NOT NULL,
    note          VARCHAR(500),
    changed_by    UUID REFERENCES users(id) ON DELETE SET NULL,
    changed_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE work_orders (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id                 UUID NOT NULL UNIQUE REFERENCES reports(id) ON DELETE CASCADE,
    materials_required        JSONB NOT NULL DEFAULT '[]',
    estimated_labor_hours     NUMERIC(6,2) NOT NULL DEFAULT 0,
    estimated_duration_days   NUMERIC(5,2) NOT NULL DEFAULT 0,
    generated_by              UUID REFERENCES users(id) ON DELETE SET NULL,
    generated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reports_severity ON reports(severity);
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_created_at ON reports(created_at);
CREATE INDEX idx_reports_reporter_id ON reports(reporter_id);
CREATE INDEX idx_report_status_history_report_id ON report_status_history(report_id);
