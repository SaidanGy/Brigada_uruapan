-- =====================================================================
-- V2__mejoras_operativas.sql
-- Ruta: src/main/resources/db/migration/V2__mejoras_operativas.sql
-- =====================================================================

-- 1. Corporaciones / bases operativas
CREATE TABLE IF NOT EXISTS organizations (
                                             id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(120) NOT NULL,
    unit_type   VARCHAR(30)  NOT NULL,
    phone       VARCHAR(30),
    address     VARCHAR(255),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_organizations_name UNIQUE (name),
    CONSTRAINT ck_organizations_type CHECK (unit_type IN ('POLICE', 'FIRE', 'AMBULANCE', 'CIVIL_PROTECTION'))
    );

ALTER TABLE units
    ADD COLUMN IF NOT EXISTS organization_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_units_organization'
    ) THEN
ALTER TABLE units
    ADD CONSTRAINT fk_units_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE SET NULL;
END IF;
END
$$;

-- 2. Campos extra de trazabilidad en alertas y ofertas
ALTER TABLE alerts
    ADD COLUMN IF NOT EXISTS expired_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS notes      VARCHAR(500);

ALTER TABLE alert_dispatches
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- 3. Solo una oferta ACEPTADA por alerta
CREATE UNIQUE INDEX IF NOT EXISTS uq_dispatch_single_accepted
    ON alert_dispatches (alert_id)
    WHERE status = 'ACCEPTED';

-- 4. Índice para búsqueda rápida de alertas por tipo de unidad y estado
CREATE INDEX IF NOT EXISTS idx_alerts_req_unit_status
    ON alerts (required_unit_type, status);

-- 5. Función y triggers para mantener updated_at al día
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_units_updated_at ON units;
CREATE TRIGGER trg_units_updated_at
    BEFORE UPDATE ON units
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_alerts_updated_at ON alerts;
CREATE TRIGGER trg_alerts_updated_at
    BEFORE UPDATE ON alerts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_dispatches_updated_at ON alert_dispatches;
CREATE TRIGGER trg_dispatches_updated_at
    BEFORE UPDATE ON alert_dispatches
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();