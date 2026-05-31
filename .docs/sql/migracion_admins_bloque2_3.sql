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
-- Las columnas viejas (cedula_posgrados, cedula_responsable) se
-- mantienen como zombie hasta Bloque 4 para no romper código
-- intermedio que aún las lea.
--
-- Hash BCrypt de "123456" (igual que el ya usado en AUTH.md):
--   $2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq
-- ============================================================

-- ── 1. Sembrar admins en la tabla `admins` ────────────────────
INSERT INTO admins (codigo, nombre_completo, email, password, tipo, es_super_admin, dependencia_id)
VALUES
  ('ADMIN1', 'Administrador',         'admin@ufps.edu.co',
      '$2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq',
      'SUPER',       true,  NULL),
  ('POS001', 'Oficina Posgrados',     'posgrados@ufps.edu.co',
      '$2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq',
      'POSGRADOS',   false, NULL),
  ('DEP001', 'Biblioteca Central',    'kevarias.2195@gmail.com',
      '$2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq',
      'DEPENDENCIA', false, (SELECT id FROM dependencias WHERE nombre = 'Biblioteca')),
  ('DEP002', 'División Financiera',   'financiera@test.com',
      '$2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq',
      'DEPENDENCIA', false, (SELECT id FROM dependencias WHERE nombre = 'Financiera')),
  ('DEP003', 'Admisiones y Registro', 'admisiones@test.com',
      '$2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq',
      'DEPENDENCIA', false, (SELECT id FROM dependencias WHERE nombre = 'Admisiones'))
ON CONFLICT (codigo) DO NOTHING;

-- ── 2. Solicitud: posgrados_admin_id (FK formal a admins) ─────
ALTER TABLE solicitud
  ADD COLUMN IF NOT EXISTS posgrados_admin_id BIGINT REFERENCES admins(id);

-- Backfill: la cédula vieja 4000000001 era POS001
UPDATE solicitud s
SET posgrados_admin_id = a.id
FROM admins a
WHERE a.codigo = 'POS001'
  AND s.cedula_posgrados = '4000000001'
  AND s.posgrados_admin_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_solicitud_posgrados_admin
  ON solicitud(posgrados_admin_id);

-- ── 3a. Prerequisito: `usuario.id` debe ser UNIQUE para poder
--      referenciarse como FK. Hoy `usuario.cedula` es la PK real
--      y `usuario.id` existe como columna IDENTITY pero sin
--      constraint UNIQUE — es deuda preexistente (documentada en
--      plan_integracion_bd_oficial.md §3.1). Hibernate trata `id`
--      como PK por convenio JPA, pero PostgreSQL no lo sabe.
--      Añadir UNIQUE es aditivo, no rompe nada.
DO $$ BEGIN
  ALTER TABLE usuario ADD CONSTRAINT usuario_id_unique UNIQUE (id);
EXCEPTION
  WHEN duplicate_table THEN NULL;
  WHEN duplicate_object THEN NULL;
END $$;

-- ── 3b. PazYSalvo: separar responsable según tipo ──────────────
ALTER TABLE paz_y_salvo
  ADD COLUMN IF NOT EXISTS responsable_admin_id   BIGINT REFERENCES admins(id),
  ADD COLUMN IF NOT EXISTS responsable_usuario_id BIGINT REFERENCES usuario(id);

-- Backfill: si la cédula del responsable existe en admins (vía mapeo de
-- cédula vieja → codigo nuevo) → responsable_admin_id; si existe en
-- usuario (DIRECTOR) → responsable_usuario_id.
UPDATE paz_y_salvo ps
SET responsable_admin_id = a.id
FROM admins a, (VALUES
   ('3000000001','DEP001'),
   ('3000000002','DEP002'),
   ('3000000003','DEP003'),
   ('4000000001','POS001'),
   ('9999999990','ADMIN1')
) AS map(cedula_vieja, codigo_admin)
WHERE ps.cedula_responsable = map.cedula_vieja
  AND a.codigo = map.codigo_admin
  AND ps.responsable_admin_id IS NULL;

UPDATE paz_y_salvo ps
SET responsable_usuario_id = u.id
FROM usuario u
WHERE u.cedula = ps.cedula_responsable
  AND ps.responsable_usuario_id IS NULL
  AND ps.responsable_admin_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_paz_y_salvo_resp_admin
  ON paz_y_salvo(responsable_admin_id);
CREATE INDEX IF NOT EXISTS idx_paz_y_salvo_resp_usuario
  ON paz_y_salvo(responsable_usuario_id);

-- ── 4. Dropear FK zombie de tipo_certificado.dependencia_cedula ──
-- En Bloque 1 migramos a tipo_certificado.dependencia_id (FK a
-- dependencias.id). La columna `dependencia_cedula` quedó como zombie
-- (insertable=false/updatable=false en la entity) pero el FK formal
-- en BD a usuario(cedula) sigue vivo y bloquearía el DELETE de admins
-- viejos en el siguiente paso. La columna se borra entera en Bloque 4.
ALTER TABLE tipo_certificado DROP CONSTRAINT IF EXISTS fk_tc_dependencia;

-- ── 5. Borrar los 5 admins viejos de `usuario` ────────────────
DELETE FROM usuario
WHERE cedula IN ('3000000001','3000000002','3000000003','4000000001','9999999990');
