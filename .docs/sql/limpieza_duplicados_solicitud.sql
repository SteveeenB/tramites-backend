-- ============================================================================
-- Limpieza de solicitudes duplicadas del seed (MySQL / Railway)
--
-- Síntoma: las bandejas muestran decenas de solicitudes idénticas (p. ej. 82
-- aprobadas y 41 rechazadas). Las cifras son múltiplos exactos del seed: cada
-- ejecución de data.sql inserta 1 rechazada, 2 aprobadas y 1 pendiente de pago,
-- y la tabla `solicitud` no tiene ninguna clave única, así que INSERT IGNORE no
-- evita nada. (Mismo origen que los duplicados de tipo_certificado.)
--
-- Regla de borrado (segura): se borra una fila SOLO si
--   a) es del seed (radicado IS NULL: las creadas desde la aplicación siempre
--      traen radicado),
--   b) existe otra idéntica con id menor (se conserva la más antigua), y
--   c) NO tiene datos hijos (paz_y_salvo, pagos, documento_solicitud,
--      historial_estado_solicitud). Así nunca quedan hijos huérfanos.
--
-- Ejecutar en orden, paso por paso.
-- ============================================================================

-- 1) Diagnóstico: cuántas solicitudes hay por estado y cuáles se repiten.
SELECT estado, COUNT(*) AS total FROM solicitud GROUP BY estado;

SELECT cedula, tipo, estado, fecha_solicitud, COUNT(*) AS veces, MIN(id) AS id_mas_antiguo
FROM solicitud
WHERE radicado IS NULL
GROUP BY cedula, tipo, estado, fecha_solicitud, costo, observaciones
HAVING COUNT(*) > 1;

-- 2) Respaldo (solo si no existe ya).
CREATE TABLE IF NOT EXISTS solicitud_respaldo AS SELECT * FROM solicitud;

-- 3) Ids que se van a borrar. REVISA este resultado antes de seguir.
DROP TEMPORARY TABLE IF EXISTS solicitud_a_borrar;
CREATE TEMPORARY TABLE solicitud_a_borrar AS
SELECT DISTINCT s.id
FROM solicitud s
JOIN solicitud k
  ON  k.cedula = s.cedula
  AND k.tipo = s.tipo
  AND k.estado = s.estado
  AND k.fecha_solicitud = s.fecha_solicitud
  AND k.costo = s.costo
  AND k.observaciones = s.observaciones
  AND k.id < s.id
WHERE s.radicado IS NULL
  AND NOT EXISTS (SELECT 1 FROM paz_y_salvo p                  WHERE p.solicitud_id = s.id)
  AND NOT EXISTS (SELECT 1 FROM pagos p                        WHERE p.solicitud_id = s.id)
  AND NOT EXISTS (SELECT 1 FROM documento_solicitud d          WHERE d.solicitud_id = s.id)
  AND NOT EXISTS (SELECT 1 FROM historial_estado_solicitud h   WHERE h.solicitud_id = s.id);

SELECT COUNT(*) AS filas_a_borrar FROM solicitud_a_borrar;

-- 4) Borrado dentro de una transacción.
START TRANSACTION;

DELETE FROM solicitud WHERE id IN (SELECT id FROM solicitud_a_borrar);

-- Verificación: debe quedar ~1 por grupo (más las que tienen hijos).
SELECT estado, COUNT(*) AS total FROM solicitud GROUP BY estado;

-- Si todo cuadra:
COMMIT;
-- Si algo no cuadra: ROLLBACK;

DROP TEMPORARY TABLE IF EXISTS solicitud_a_borrar;
