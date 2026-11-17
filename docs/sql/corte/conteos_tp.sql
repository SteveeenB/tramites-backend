-- ============================================================================
-- conteos_tp.sql — Conteo exacto de filas de las 17 tablas de Trámites (con prefijo tp_: ensayo local y base compartida)
-- (TP-153 · docs/CORTE_RAILWAY_A_AIVEN.md). Solo lectura.
-- Ejecutar conectado a la base correspondiente y guardar la salida.
-- ============================================================================

          SELECT 'roles' AS tabla, COUNT(*) AS filas FROM tp_roles
UNION ALL SELECT 'estados_estudiantes' AS tabla, COUNT(*) AS filas FROM tp_estados_estudiantes
UNION ALL SELECT 'programa_academico' AS tabla, COUNT(*) AS filas FROM tp_programa_academico
UNION ALL SELECT 'dependencias' AS tabla, COUNT(*) AS filas FROM tp_dependencias
UNION ALL SELECT 'tipos_solicitud' AS tabla, COUNT(*) AS filas FROM tp_tipos_solicitud
UNION ALL SELECT 'tipo_certificado' AS tabla, COUNT(*) AS filas FROM tp_tipo_certificado
UNION ALL SELECT 'fechas_grado' AS tabla, COUNT(*) AS filas FROM tp_fechas_grado
UNION ALL SELECT 'usuario' AS tabla, COUNT(*) AS filas FROM tp_usuario
UNION ALL SELECT 'admins' AS tabla, COUNT(*) AS filas FROM tp_admins
UNION ALL SELECT 'estudiante' AS tabla, COUNT(*) AS filas FROM tp_estudiante
UNION ALL SELECT 'solicitud' AS tabla, COUNT(*) AS filas FROM tp_solicitud
UNION ALL SELECT 'historial_estado_solicitud' AS tabla, COUNT(*) AS filas FROM tp_historial_estado_solicitud
UNION ALL SELECT 'documento_solicitud' AS tabla, COUNT(*) AS filas FROM tp_documento_solicitud
UNION ALL SELECT 'pagos' AS tabla, COUNT(*) AS filas FROM tp_pagos
UNION ALL SELECT 'paz_y_salvo' AS tabla, COUNT(*) AS filas FROM tp_paz_y_salvo
UNION ALL SELECT 'solicitud_certificado' AS tabla, COUNT(*) AS filas FROM tp_solicitud_certificado
UNION ALL SELECT 'notificaciones' AS tabla, COUNT(*) AS filas FROM tp_notificaciones
ORDER BY tabla;
