-- ============================================================
-- Bloque 1 del refactor de identidad (plan_roles_v2.md §4)
-- Reemplaza `tipo_certificado.dependencia_cedula` (FK lógica a
-- `usuario.cedula`) por `tipo_certificado.dependencia_id`
-- (FK formal a `dependencias.id`).
--
-- La columna vieja NO se elimina aquí — se mantiene como zombie
-- hasta Bloque 4. La entidad TipoCertificado la marca como
-- read-only (insertable=false, updatable=false) para que todo
-- el código nuevo trabaje con la nueva FK.
-- ============================================================

ALTER TABLE tipo_certificado
  ADD COLUMN IF NOT EXISTS dependencia_id BIGINT REFERENCES dependencias(id);

-- Backfill: la cédula vieja apuntaba a un usuario DEPENDENCIA que
-- a su vez tiene FK a `dependencias`. Resolvemos el id correcto.
UPDATE tipo_certificado tc
SET dependencia_id = u.dependencia_id
FROM usuario u
WHERE u.cedula = tc.dependencia_cedula
  AND tc.dependencia_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_tipo_certificado_dep_id
  ON tipo_certificado(dependencia_id);
