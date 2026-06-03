-- ==============================================================================
-- Migración SQL: Certificados solo en Posgrados
-- Descripción: Quita la relación entre Tipo de Certificado y Dependencia.
-- Todas las operaciones relacionadas a la emisión de certificados físicos
-- pasan a ser responsabilidad exclusiva del rol POSGRADOS.
-- ==============================================================================

-- 1. Eliminar la columna de llave foránea en tipo_certificado
ALTER TABLE tipo_certificado DROP COLUMN IF EXISTS dependencia_id;
ALTER TABLE tipo_certificado DROP COLUMN IF EXISTS dependencia_cedula;

-- (Opcional) Si existe alguna restricción FK que no se elimina automáticamente:
-- ALTER TABLE tipo_certificado DROP CONSTRAINT IF EXISTS fk_tipo_certificado_dependencia;
