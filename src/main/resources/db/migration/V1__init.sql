-- === Pré-requisitos (para UUIDs gerados) ===
CREATE EXTENSION IF NOT EXISTS pgcrypto; -- gen_random_uuid()

-- ===========================================
-- Tabela principal: tb_sch_schedule (VEVENT)
-- ===========================================
CREATE TABLE IF NOT EXISTS tb_sch_schedule (
  sch_id                 uuid            PRIMARY KEY DEFAULT gen_random_uuid(),
  sch_rrule_json         jsonb           NOT NULL,                     -- RRULE em JSONB
  sch_tzid               text            NOT NULL,                     -- ex.: 'America/Sao_Paulo'
  sch_series_start_local timestamp       NOT NULL,                     -- DTSTART no TZID (sem offset)
  sch_series_start_utc   timestamptz     NOT NULL,                     -- mesmo instante em UTC
  sch_series_until_utc   timestamptz     NULL,
  sch_duration_seconds   integer         NOT NULL CHECK (sch_duration_seconds > 0),
  sch_summary            text            NULL,
  sch_notes              text            NULL,
  sch_has_exdates        boolean         NOT NULL DEFAULT false,
  sch_has_rdates         boolean         NOT NULL DEFAULT false,
  sch_has_overrides      boolean         NOT NULL DEFAULT false,
  sch_created_at         timestamptz     NOT NULL DEFAULT now(),
  sch_updated_at         timestamptz     NOT NULL DEFAULT now(),

  -- Até quando (se existir) deve ser > início
  CONSTRAINT sch_until_after_start
    CHECK (sch_series_until_utc IS NULL OR sch_series_until_utc > sch_series_start_utc),

  -- RRULE mínima: exigir FREQ conhecido (opcional, mas ajuda a sanear)
  CONSTRAINT sch_rrule_freq_valid
    CHECK (lower(coalesce(sch_rrule_json->>'freq','')) IN ('daily','weekly','monthly','yearly'))
);

-- Índices úteis
CREATE INDEX IF NOT EXISTS idx_sch_start_utc    ON tb_sch_schedule (sch_series_start_utc);
CREATE INDEX IF NOT EXISTS idx_sch_until_utc    ON tb_sch_schedule (sch_series_until_utc);
CREATE INDEX IF NOT EXISTS idx_sch_rrule_json   ON tb_sch_schedule USING GIN (sch_rrule_json);

-- Auto-update de sch_updated_at
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.sch_updated_at := now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_tb_sch_schedule_updated ON tb_sch_schedule;
CREATE TRIGGER trg_tb_sch_schedule_updated
BEFORE UPDATE ON tb_sch_schedule
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- ===========================================
-- EXDATEs: tb_sex_schedule_exdate
-- ===========================================
CREATE TABLE IF NOT EXISTS tb_sex_schedule_exdate (
  sex_id            uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
  sex_exdate_local  timestamp   NOT NULL,
  sch_id            uuid        NOT NULL REFERENCES tb_sch_schedule(sch_id) ON DELETE CASCADE
);

-- Unicidade por agenda + data local
CREATE UNIQUE INDEX IF NOT EXISTS uq_sex_sch_exdate
  ON tb_sex_schedule_exdate (sch_id, sex_exdate_local);

CREATE INDEX IF NOT EXISTS idx_sex_sch
  ON tb_sex_schedule_exdate (sch_id);

-- ===========================================
-- RDATEs: tb_srd_schedule_rdate
-- ===========================================
CREATE TABLE IF NOT EXISTS tb_srd_schedule_rdate (
  srd_id               uuid       PRIMARY KEY DEFAULT gen_random_uuid(),
  srd_rdate_local      timestamp  NOT NULL,
  srd_duration_seconds integer    NOT NULL CHECK (srd_duration_seconds > 0),
  sch_id               uuid       NOT NULL REFERENCES tb_sch_schedule(sch_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_srd_sch_rdate
  ON tb_srd_schedule_rdate (sch_id, srd_rdate_local);

CREATE INDEX IF NOT EXISTS idx_srd_sch
  ON tb_srd_schedule_rdate (sch_id);

-- ===========================================
-- Overrides: tb_sov_schedule_override
-- ===========================================
CREATE TABLE IF NOT EXISTS tb_sov_schedule_override (
  sov_id                   uuid       PRIMARY KEY DEFAULT gen_random_uuid(),
  sov_recurrence_id_local  timestamp  NOT NULL, -- "RECURRENCE-ID" original (local)
  sov_new_start_local      timestamp  NOT NULL, -- novo início (local)
  sov_new_duration_seconds integer    NOT NULL CHECK (sov_new_duration_seconds > 0),
  sov_summary              text       NULL,
  sov_notes                text       NULL,
  sch_id                   uuid       NOT NULL REFERENCES tb_sch_schedule(sch_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_sov_sch_recurrence
  ON tb_sov_schedule_override (sch_id, sov_recurrence_id_local);

CREATE INDEX IF NOT EXISTS idx_sov_sch
  ON tb_sov_schedule_override (sch_id);
