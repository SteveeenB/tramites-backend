-- ============================================================
-- SC-03 · N5/N6 — Copia del estudiante desde el Módulo de Matrícula
-- (docs/integracion-matricula-plan.md §6.3 y §6.4)
--
-- programa_academico.codigo: llave para mapear la respuesta de
--   Matrícula. Queda en NULL y se llena en la primera sincronización
--   (se adopta del programa con el mismo nombre).
-- estudiante.estado_matricula: estado del estudiante en Matrícula
--   (ACTIVO, ...). No reemplaza estado_estudiante_id.
-- estudiante.matricula_sincronizado_en: última consulta exitosa; si
--   Matrícula no responde, la copia vale mientras tenga menos de
--   matricula.snapshot-vigencia-horas (24 h).
--
-- MySQL 8+. Idempotente: comprueba cada columna e índice antes de crearlo.
-- ============================================================

-- ── 1. programa_academico.codigo ─────────────────────────────
SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'programa_academico'
                  AND COLUMN_NAME = 'codigo');
SET @sql := IF(@existe = 0,
    'ALTER TABLE programa_academico ADD COLUMN codigo VARCHAR(50) NULL',
    'SELECT 1');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

SET @existe := (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'programa_academico'
                  AND INDEX_NAME = 'uk_programa_academico_codigo');
SET @sql := IF(@existe = 0,
    'ALTER TABLE programa_academico ADD CONSTRAINT uk_programa_academico_codigo UNIQUE (codigo)',
    'SELECT 1');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- ── 2. estudiante.estado_matricula ───────────────────────────
SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'estudiante'
                  AND COLUMN_NAME = 'estado_matricula');
SET @sql := IF(@existe = 0,
    'ALTER TABLE estudiante ADD COLUMN estado_matricula VARCHAR(50) NULL',
    'SELECT 1');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- ── 3. estudiante.matricula_sincronizado_en ──────────────────
SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'estudiante'
                  AND COLUMN_NAME = 'matricula_sincronizado_en');
SET @sql := IF(@existe = 0,
    'ALTER TABLE estudiante ADD COLUMN matricula_sincronizado_en DATETIME(6) NULL',
    'SELECT 1');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;
