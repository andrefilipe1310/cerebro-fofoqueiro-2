-- @path infra/postgres/init/02_extensions_per_schema.sql
-- @owner infra
-- @responsibility Funções utilitárias disponíveis em todos os schemas
-- @see docs/ARCHITECTURE.md#banco-dados

-- Função global de updated_at (disponível no schema public)
-- Cada serviço também cria sua versão local no schema próprio via migration
CREATE OR REPLACE FUNCTION trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
