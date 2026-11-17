-- ============================================================================
-- conteos_sin_prefijo.sql — Conteo exacto de filas de las 17 tablas de Trámites (sin prefijo: Railway)
-- (TP-153 · docs/CORTE_RAILWAY_A_AIVEN.md). Solo lectura.
-- Ejecutar conectado a la base correspondiente y guardar la salida.
-- ============================================================================

          SELECT 'roles' AS tabla, COUNT(*) AS filas FROM roles
UNION ALL SELECT 'estados_estudiantes' AS tabla, COUNT(*) AS filas FROM estados_estudiantes
UNION ALL SELECT 'programa_academico' AS tabla, COUNT(*) AS filas FROM programa_academico
UNION ALL SELECT 'dependencias' AS tabla, COUNT(*) AS filas FROM dependencias
UNION ALL SELECT 'tipos_solicitud' AS tabla, COUNT(*) AS filas FROM tipos_solicitud
UNION ALL SELECT 'tipo_certificado' AS tabla, COUNT(*) AS filas FROM tipo_certificado
UNION ALL SELECT 'fechas_grado' AS tabla, COUNT(*) AS filas FROM fechas_grado
UNION ALL SELECT 'usuario' AS tabla, COUNT(*) AS filas FROM usuario
UNION ALL SELECT 'admins' AS tabla, COUNT(*) AS filas FROM admins
UNION ALL SELECT 'estudiante' AS tabla, COUNT(*) AS filas FROM estudiante
UNION ALL SELECT 'solicitud' AS tabla, COUNT(*) AS filas FROM solicitud
UNION ALL SELECT 'historial_estado_solicitud' AS tabla, COUNT(*) AS filas FROM historial_estado_solicitud
UNION ALL SELECT 'documento_solicitud' AS tabla, COUNT(*) AS filas FROM documento_solicitud
UNION ALL SELECT 'pagos' AS tabla, COUNT(*) AS filas FROM pagos
UNION ALL SELECT 'paz_y_salvo' AS tabla, COUNT(*) AS filas FROM paz_y_salvo
UNION ALL SELECT 'solicitud_certificado' AS tabla, COUNT(*) AS filas FROM solicitud_certificado
UNION ALL SELECT 'notificaciones' AS tabla, COUNT(*) AS filas FROM notificaciones
ORDER BY tabla;
