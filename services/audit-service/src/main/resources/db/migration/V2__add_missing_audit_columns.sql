ALTER TABLE audit.audit_logs ADD COLUMN IF NOT EXISTS metadata  JSONB;
ALTER TABLE audit.audit_logs ADD COLUMN IF NOT EXISTS user_agent TEXT;
