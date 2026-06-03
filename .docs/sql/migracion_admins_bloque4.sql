-- ============================================================
-- Bloque 4 del refactor de identidad (plan_roles_v2.md §11.8)
-- Cleanup final: elimina columnas zombie de la BD que ya no se
-- leen ni escriben desde el código.
--
-- Prerequisito: Bloques 0-3 ya en producción y estables.
-- Las columnas zombie estaban marcadas en las entities como
-- @Column(insertable=false, updatable=false) — el código las
-- ignoraba pero la columna seguía existiendo en BD.
-- ============================================================

-- ── 1. Drop de FK formales preexistentes contra cédulas viejas ─
-- Ya se dropeó fk_tc_dependencia en bloque 2_3. Aquí re-aseguramos
-- por idempotencia.
ALTER TABLE tipo_certificado DROP CONSTRAINT IF EXISTS fk_tc_dependencia;

-- ── 2. Drop de columnas zombie ────────────────────────────────
ALTER TABLE paz_y_salvo      DROP COLUMN IF EXISTS cedula_responsable;
ALTER TABLE solicitud        DROP COLUMN IF EXISTS cedula_posgrados;
ALTER TABLE tipo_certificado DROP COLUMN IF EXISTS dependencia_cedula;

-- ── 3. Drop de usuario.dependencia_id (huérfana post Bloque 2+3) ─
-- Verificación previa: ningún usuario debería tener dependencia
-- asignada (todos los DEPENDENCIA se movieron a `admins`).
-- Ejecutar este SELECT antes del DROP; debe devolver 0 filas:
--
--   SELECT cedula, codigo, dependencia_id
--   FROM usuario WHERE dependencia_id IS NOT NULL;
--
-- Si devuelve filas, NO ejecutar el DROP y revisar caso por caso.
ALTER TABLE usuario DROP COLUMN IF EXISTS dependencia_id;

-- ── 4. Limpieza catálogo `roles` (opcional, descomentar tras verificar) ─
-- Verificación previa: ninguna fila de `usuario` debería tener rol
-- POSGRADOS, DEPENDENCIA o ADMIN (todos migrados a `admins`).
-- Ejecutar este SELECT primero; debe devolver 0 filas:
--
--   SELECT u.codigo, r.nombre
--   FROM usuario u JOIN roles r ON u.rol_id = r.id
--   WHERE r.nombre IN ('POSGRADOS','DEPENDENCIA','ADMIN');
--
-- Si devuelve 0 filas, descomentar la línea siguiente:
-- DELETE FROM roles WHERE nombre IN ('POSGRADOS','DEPENDENCIA','ADMIN');
