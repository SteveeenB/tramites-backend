-- ============================================================
-- Sprint A — Bloque 5e del refactor de identidad
-- (plan_roles_v3.md §4.4)
--
-- Reemplaza el string libre `estudiante.estado_grado` por una
-- FK formal a un catálogo `estados_estudiantes`.
--
-- FIX TP-194 (Diego Bermúdez, 07/10/2026): la versión anterior usaba
-- sintaxis PostgreSQL (`GENERATED ALWAYS AS IDENTITY`, `ON CONFLICT`,
-- `UPDATE ... FROM`, `CREATE INDEX IF NOT EXISTS`) que no corre en
-- MySQL. Se reescribe en sintaxis MySQL 8+, que es el motor real del
-- stack (ver application.properties, dialect MySQLDialect).
-- Idempotente mediante INSERT IGNORE, IF NOT EXISTS en DDL de
-- tabla/columna y comprobación previa del índice.
-- ============================================================

-- ── 1. Crear catálogo ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS estados_estudiantes (
  id     BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(50) UNIQUE NOT NULL
) ENGINE=InnoDB;

-- ── 2. Seed (3 estados que cubren los valores literales usados
--      actualmente en el código + un "ACTIVO" por defecto) ─────
INSERT IGNORE INTO estados_estudiantes (nombre) VALUES
  ('ACTIVO'),
  ('PAGO_GRADO_PENDIENTE'),
  ('GRADUADO');

-- ── 3. Añadir FK en estudiante ────────────────────────────────
ALTER TABLE estudiante
  ADD COLUMN IF NOT EXISTS estado_estudiante_id BIGINT NULL,
  ADD CONSTRAINT fk_estudiante_estado
      FOREIGN KEY (estado_estudiante_id) REFERENCES estados_estudiantes(id);

-- ── 4. Backfill: mapear estado_grado existente al nuevo id ────
-- 'GRADUADO' y 'PAGO_GRADO_PENDIENTE' se mapean tal cual.
-- NULL o cualquier otro valor se mapea a 'ACTIVO' como default.
UPDATE estudiante e
JOIN estados_estudiantes ee
  ON ee.nombre = e.estado_grado
SET e.estado_estudiante_id = ee.id
WHERE e.estado_estudiante_id IS NULL
  AND e.estado_grado IN ('GRADUADO', 'PAGO_GRADO_PENDIENTE');

UPDATE estudiante
SET estado_estudiante_id = (SELECT id FROM estados_estudiantes WHERE nombre = 'ACTIVO')
WHERE estado_estudiante_id IS NULL;

-- MySQL no soporta `CREATE INDEX IF NOT EXISTS` hasta 8.0.29. Esta
-- variante lo emula con information_schema para que sea idempotente
-- en versiones anteriores también.
SET @idx := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name  = 'estudiante'
    AND index_name  = 'idx_estudiante_estado_id'
);
SET @sql := IF(@idx = 0,
  'CREATE INDEX idx_estudiante_estado_id ON estudiante(estado_estudiante_id)',
  'SELECT ''index idx_estudiante_estado_id ya existe'' AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── 5. Verificación post-migración (no destructivo) ───────────
-- Ejecutar después del backfill para confirmar:
--   SELECT ee.nombre, COUNT(*)
--   FROM estudiante e JOIN estados_estudiantes ee ON e.estado_estudiante_id = ee.id
--   GROUP BY ee.nombre;
-- Esperado: distribución coherente con los datos de prueba.
