-- ============================================================
-- Sprint B — Bloque 5a del refactor de identidad
-- (plan_roles_v3.md §4.1)
--
-- Reemplaza las FK lógicas string-cédula por FKs formales Long
-- a estudiante(id), alineado con el patrón oficial donde las
-- relaciones a estudiantes van por id (no por cédula).
--
-- Estrategia: doble-write durante la transición.
--   - Las columnas string viejas (cedula, cedula_estudiante)
--     se mantienen y se siguen poblando por compatibilidad.
--   - La nueva columna estudiante_id se añade y se backfilla.
--   - El código nuevo escribe AMBOS (cedula viejo + FK nuevo).
--   - El drop de las columnas viejas se hace en un sprint
--     posterior, una vez todo el código lea del FK.
--
-- Idempotente: usa IF NOT EXISTS.
-- ============================================================

-- ── 1. Solicitud (TERMINACION_MATERIAS / GRADO) ───────────────
ALTER TABLE solicitud
  ADD COLUMN IF NOT EXISTS estudiante_id BIGINT REFERENCES estudiante(id);

UPDATE solicitud s
SET estudiante_id = e.id
FROM estudiante e
WHERE e.cedula = s.cedula
  AND s.estudiante_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_solicitud_estudiante
  ON solicitud(estudiante_id);

-- ── 2. SolicitudCertificado ───────────────────────────────────
ALTER TABLE solicitud_certificado
  ADD COLUMN IF NOT EXISTS estudiante_id BIGINT REFERENCES estudiante(id);

UPDATE solicitud_certificado sc
SET estudiante_id = e.id
FROM estudiante e
WHERE e.cedula = sc.cedula
  AND sc.estudiante_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_solicitud_cert_estudiante
  ON solicitud_certificado(estudiante_id);

-- ── 3. PazYSalvo ──────────────────────────────────────────────
ALTER TABLE paz_y_salvo
  ADD COLUMN IF NOT EXISTS estudiante_id BIGINT REFERENCES estudiante(id);

UPDATE paz_y_salvo ps
SET estudiante_id = e.id
FROM estudiante e
WHERE e.cedula = ps.cedula_estudiante
  AND ps.estudiante_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_paz_y_salvo_estudiante
  ON paz_y_salvo(estudiante_id);

-- ── 4. Verificaciones post-migración ──────────────────────────
-- Confirmar que todas las filas con cedula tienen estudiante_id:
--   SELECT 'solicitud' AS tabla,
--          COUNT(*) AS total,
--          COUNT(estudiante_id) AS con_fk,
--          COUNT(*) - COUNT(estudiante_id) AS sin_fk
--   FROM solicitud
--   UNION ALL
--   SELECT 'solicitud_certificado', COUNT(*), COUNT(estudiante_id),
--          COUNT(*) - COUNT(estudiante_id) FROM solicitud_certificado
--   UNION ALL
--   SELECT 'paz_y_salvo', COUNT(*), COUNT(estudiante_id),
--          COUNT(*) - COUNT(estudiante_id) FROM paz_y_salvo;
--
-- "sin_fk" > 0 significa que hay filas con cédula que NO matchea
-- ningún estudiante en la tabla `estudiante`. Investigar caso por
-- caso antes del drop de cedula (sprint futuro).
