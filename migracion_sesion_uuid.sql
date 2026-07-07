-- ============================================
-- Migración: UUID de INT → VARCHAR(36)
-- y aseguramos que se guarde la IP real
-- ============================================

-- Cambiar tipo de columna UUID de INT a VARCHAR(36)
ALTER TABLE Sesion ALTER COLUMN UUID DROP DEFAULT;
ALTER TABLE Sesion ALTER COLUMN UUID TYPE VARCHAR(36) USING UUID::text;
ALTER TABLE Sesion ALTER COLUMN UUID SET DEFAULT gen_random_uuid();
