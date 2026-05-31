-- ============================================================
-- Sprint A — Bloque 5e del refactor de identidad
-- (plan_roles_v3.md §4.4)
--
-- Reemplaza el string libre `estudiante.estado_grado` por una
-- FK formal a un catálogo `estados_estudiantes`, alineado con
-- el patrón oficial (clientes_finales_sistema.md §6).
--
-- La columna `estado_grado` queda como zombie temporal (marcada
-- en la entity como insertable=false, updatable=false). Se
-- elimina en un futuro Bloque 5 cleanup, cuando se confirme
-- que ningún código viejo la lee.
--
-- Idempotente: usa IF NOT EXISTS / ON CONFLICT.
-- ============================================================

-- ── 1. Crear catálogo ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS estados_estudiantes (
  id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre VARCHAR(50) UNIQUE NOT NULL
);

-- ── 2. Seed (3 estados que cubren los valores literales usados
--      actualmente en el código + un "ACTIVO" por defecto) ─────
INSERT INTO estados_estudiantes (nombre) VALUES
  ('ACTIVO'),
  ('PAGO_GRADO_PENDIENTE'),
  ('GRADUADO')
ON CONFLICT (nombre) DO NOTHING;

-- ── 3. Añadir FK en estudiante ────────────────────────────────
ALTER TABLE estudiante
  ADD COLUMN IF NOT EXISTS estado_estudiante_id BIGINT REFERENCES estados_estudiantes(id);

-- ── 4. Backfill: mapear estado_grado existente al nuevo id ────
-- 'GRADUADO' y 'PAGO_GRADO_PENDIENTE' se mapean tal cual.
-- NULL o cualquier otro valor se mapea a 'ACTIVO' como default.
UPDATE estudiante e
SET estado_estudiante_id = ee.id
FROM estados_estudiantes ee
WHERE ee.nombre = e.estado_grado
  AND e.estado_estudiante_id IS NULL
  AND e.estado_grado IN ('GRADUADO', 'PAGO_GRADO_PENDIENTE');

UPDATE estudiante
SET estado_estudiante_id = (SELECT id FROM estados_estudiantes WHERE nombre = 'ACTIVO')
WHERE estado_estudiante_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_estudiante_estado_id
  ON estudiante(estado_estudiante_id);

-- ── 5. Verificación post-migración (no destructivo) ───────────
-- Ejecutar después del backfill para confirmar:
--   SELECT ee.nombre, COUNT(*)
--   FROM estudiante e JOIN estados_estudiantes ee ON e.estado_estudiante_id = ee.id
--   GROUP BY ee.nombre;
-- Esperado: distribución coherente con los datos de prueba.
