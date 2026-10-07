-- ============================================================
-- Bloques 2 + 3 del refactor de identidad (plan_roles_v2.md §4)
-- Migra los 5 admins (ADMIN1, POS001, DEP001-3) de `usuario`
-- a `admins`. Refactoriza las FK lógicas que apuntaban a sus
-- cédulas viejas:
--   - solicitud.cedula_posgrados  → solicitud.posgrados_admin_id
--   - paz_y_salvo.cedula_responsable → paz_y_salvo.responsable_admin_id
--                                    + paz_y_salvo.responsable_usuario_id
--     (uno de los dos según tipo: DIRECTOR=Usuario, DEPENDENCIA/POSGRADOS=Admin)
--
-- Hash BCrypt de "123456" (igual que el ya usado en AUTH.md):
--   $2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq
--
-- FIX TP-194 (Diego Bermúdez, 07/10/2026): se migra de la sintaxis
-- PostgreSQL (ON CONFLICT, UPDATE ... FROM, DO $$ ... $$, CREATE INDEX
-- IF NOT EXISTS) a MySQL 8+, que es el motor real del stack. Se
-- preserva idempotencia (INSERT IGNORE, ADD COLUMN IF NOT EXISTS,
-- comprobación previa de índices y constraints).
-- ============================================================

-- ── 1. Sembrar admins en la tabla `admins` ────────────────────
INSERT IGNORE INTO admins (codigo, nombre_completo, email, password, tipo, es_super_admin, dependencia_id)
VALUES
  ('ADMIN1', 'Administrador',         'admin@ufps.edu.co',
      '$2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq',
      'SUPER',       true,  NULL),
  ('POS001', 'Oficina Posgrados',     'posgrados@ufps.edu.co',
      '$2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq',
      'POSGRADOS',   false, NULL),
  ('DEP001', 'Biblioteca Central',    'kevarias.2195@gmail.com',
      '$2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq',
      'DEPENDENCIA', false, (SELECT id FROM dependencias WHERE nombre = 'Biblioteca' LIMIT 1)),
  ('DEP002', 'División Financiera',   'financiera@test.com',
      '$2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq',
      'DEPENDENCIA', false, (SELECT id FROM dependencias WHERE nombre = 'Financiera' LIMIT 1)),
  ('DEP003', 'Admisiones y Registro', 'admisiones@test.com',
      '$2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq',
      'DEPENDENCIA', false, (SELECT id FROM dependencias WHERE nombre = 'Admisiones' LIMIT 1));

-- ── 2. Solicitud: posgrados_admin_id (FK formal a admins) ─────
ALTER TABLE solicitud
  ADD COLUMN IF NOT EXISTS posgrados_admin_id BIGINT NULL,
  ADD CONSTRAINT fk_solicitud_posgrados_admin
      FOREIGN KEY (posgrados_admin_id) REFERENCES admins(id);

-- Backfill: la cédula vieja 4000000001 era POS001
UPDATE solicitud s
JOIN admins a ON a.codigo = 'POS001'
SET s.posgrados_admin_id = a.id
WHERE s.cedula_posgrados = '4000000001'
  AND s.posgrados_admin_id IS NULL;

SET @idx := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name  = 'solicitud'
    AND index_name  = 'idx_solicitud_posgrados_admin'
);
SET @sql := IF(@idx = 0,
  'CREATE INDEX idx_solicitud_posgrados_admin ON solicitud(posgrados_admin_id)',
  'SELECT ''index idx_solicitud_posgrados_admin ya existe'' AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── 3a. Prerequisito: usuario.id UNIQUE para poder referenciarse como FK.
--      MySQL: añadimos la constraint sólo si no existe ya.
SET @cnt := (
  SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE table_schema   = DATABASE()
    AND table_name     = 'usuario'
    AND constraint_name= 'usuario_id_unique'
);
SET @sql := IF(@cnt = 0,
  'ALTER TABLE usuario ADD CONSTRAINT usuario_id_unique UNIQUE (id)',
  'SELECT ''constraint usuario_id_unique ya existe'' AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── 3b. PazYSalvo: separar responsable según tipo ──────────────
ALTER TABLE paz_y_salvo
  ADD COLUMN IF NOT EXISTS responsable_admin_id   BIGINT NULL,
  ADD COLUMN IF NOT EXISTS responsable_usuario_id BIGINT NULL,
  ADD CONSTRAINT fk_pyss_resp_admin
      FOREIGN KEY (responsable_admin_id) REFERENCES admins(id),
  ADD CONSTRAINT fk_pyss_resp_usuario
      FOREIGN KEY (responsable_usuario_id) REFERENCES usuario(id);

-- Backfill: mapeo de cédulas viejas → códigos de admin.
--   3000000001 → DEP001
--   3000000002 → DEP002
--   3000000003 → DEP003
--   4000000001 → POS001
--   9999999990 → ADMIN1
UPDATE paz_y_salvo ps
JOIN admins a ON (
    (ps.cedula_responsable = '3000000001' AND a.codigo = 'DEP001')
 OR (ps.cedula_responsable = '3000000002' AND a.codigo = 'DEP002')
 OR (ps.cedula_responsable = '3000000003' AND a.codigo = 'DEP003')
 OR (ps.cedula_responsable = '4000000001' AND a.codigo = 'POS001')
 OR (ps.cedula_responsable = '9999999990' AND a.codigo = 'ADMIN1')
)
SET ps.responsable_admin_id = a.id
WHERE ps.responsable_admin_id IS NULL;

UPDATE paz_y_salvo ps
JOIN usuario u ON u.cedula = ps.cedula_responsable
SET ps.responsable_usuario_id = u.id
WHERE ps.responsable_usuario_id IS NULL
  AND ps.responsable_admin_id IS NULL;

SET @idx := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name  = 'paz_y_salvo'
    AND index_name  = 'idx_paz_y_salvo_resp_admin'
);
SET @sql := IF(@idx = 0,
  'CREATE INDEX idx_paz_y_salvo_resp_admin ON paz_y_salvo(responsable_admin_id)',
  'SELECT ''index idx_paz_y_salvo_resp_admin ya existe'' AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name  = 'paz_y_salvo'
    AND index_name  = 'idx_paz_y_salvo_resp_usuario'
);
SET @sql := IF(@idx = 0,
  'CREATE INDEX idx_paz_y_salvo_resp_usuario ON paz_y_salvo(responsable_usuario_id)',
  'SELECT ''index idx_paz_y_salvo_resp_usuario ya existe'' AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── 4. Dropear FK zombie de tipo_certificado.dependencia_cedula ──
SET @fk := (
  SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE table_schema   = DATABASE()
    AND table_name     = 'tipo_certificado'
    AND constraint_name= 'fk_tc_dependencia'
);
SET @sql := IF(@fk > 0,
  'ALTER TABLE tipo_certificado DROP FOREIGN KEY fk_tc_dependencia',
  'SELECT ''fk fk_tc_dependencia no existe (nada que dropear)'' AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── 5. Borrar los 5 admins viejos de `usuario` ────────────────
DELETE FROM usuario
WHERE cedula IN ('3000000001','3000000002','3000000003','4000000001','9999999990');
