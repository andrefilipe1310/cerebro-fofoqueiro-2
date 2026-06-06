-- Adiciona colunas presentes na migration V1 mas ausentes no 03_tables.sql (init script)
-- Usa IF NOT EXISTS para ser idempotente

ALTER TABLE auth.users
    ADD COLUMN IF NOT EXISTS backup_codes TEXT[],
    ADD COLUMN IF NOT EXISTS last_login TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS failed_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMPTZ;

ALTER TABLE auth.refresh_tokens
    ADD COLUMN IF NOT EXISTS revoked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS ip_address TEXT,
    ADD COLUMN IF NOT EXISTS user_agent TEXT;
