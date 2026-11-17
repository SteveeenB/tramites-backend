-- ============================================================================
-- V2__prefijo_tp_tablas_tramites.sql — Prefijo tp_ en las tablas de Trámites
-- (TP-209 · SC02-T9, acuerdo TP-208: docs/BD_COMPARTIDA_MATRICULA.md)
--
-- Renombra las 17 tablas de Trámites en un solo RENAME TABLE (atómico),
-- conservando datos, índices y llaves foráneas. Solo lo hace si existen LAS 17
-- tablas sin prefijo; si no, no hace nada:
--
-- * BD nueva o compartida (FLYWAY_BASELINE_VERSION=0): V1 ya creó las tablas
--   tp_*. Aquí no existen las 17 viejas (Matrícula tiene su propia tabla
--   roles, que es solo una), así que no se toca nada. Nunca renombra tablas de
--   Matrícula.
-- * BD actual de Trámites en Aiven (FLYWAY_BASELINE_VERSION=1): V1 se saltó y
--   aquí están las 17 tablas sin prefijo: se renombran a tp_*.
--
-- Solo comillas simples y nombres sin comillas: el servidor compartido corre
-- con sql_mode ANSI (ANSI_QUOTES).
-- ============================================================================

SET @tp_tablas_sin_prefijo = (
    SELECT COUNT(*)
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name IN ('roles', 'estados_estudiantes', 'programa_academico',
                          'dependencias', 'tipos_solicitud', 'tipo_certificado',
                          'fechas_grado', 'usuario', 'admins', 'estudiante',
                          'solicitud', 'historial_estado_solicitud',
                          'documento_solicitud', 'pagos', 'paz_y_salvo',
                          'solicitud_certificado', 'notificaciones')
);

SET @tp_renombrar_sql = IF(@tp_tablas_sin_prefijo = 17,
    'RENAME TABLE
         roles                      TO tp_roles,
         estados_estudiantes        TO tp_estados_estudiantes,
         programa_academico         TO tp_programa_academico,
         dependencias               TO tp_dependencias,
         tipos_solicitud            TO tp_tipos_solicitud,
         tipo_certificado           TO tp_tipo_certificado,
         fechas_grado               TO tp_fechas_grado,
         usuario                    TO tp_usuario,
         admins                     TO tp_admins,
         estudiante                 TO tp_estudiante,
         solicitud                  TO tp_solicitud,
         historial_estado_solicitud TO tp_historial_estado_solicitud,
         documento_solicitud        TO tp_documento_solicitud,
         pagos                      TO tp_pagos,
         paz_y_salvo                TO tp_paz_y_salvo,
         solicitud_certificado      TO tp_solicitud_certificado,
         notificaciones             TO tp_notificaciones',
    'DO 0');

PREPARE tp_renombrar FROM @tp_renombrar_sql;
EXECUTE tp_renombrar;
DEALLOCATE PREPARE tp_renombrar;
