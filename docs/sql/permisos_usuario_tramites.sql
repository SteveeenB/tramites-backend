-- ============================================================================
-- permisos_usuario_tramites.sql — Usuario de BD del Módulo de Trámites
-- (TP-209 · acuerdo TP-208, sección 8, docs/BD_COMPARTIDA_MATRICULA.md)
--
-- Para el servidor institucional (base compartida con Matrícula). Lo ejecuta un
-- usuario administrador ANTES del primer arranque del backend.
--
-- Antes de ejecutarlo:
--   1. Reemplazar posgrados por el nombre real de la base (todas las líneas).
--   2. Reemplazar <contraseña> por la de DB_PASSWORD (openssl rand -base64 24).
--   3. Si el usuario no es tramites_app, cambiarlo aquí y en DB_USERNAME.
--
-- El usuario tiene permisos SOLO sobre las tablas tp_* de Trámites y su
-- historial de Flyway; ninguno sobre las tablas de Matrícula ni a nivel de
-- toda la base. MySQL no admite comodines en nombres de tabla, así que se
-- otorga tabla por tabla; como incluye CREATE, se puede otorgar antes de que
-- Flyway cree la tabla.
--
-- La lista es la misma de docker/mysql-init/02_usuario_tramites.sh (perfil
-- local). Cada tabla nueva de una migración futura va en los dos archivos.
-- Solo comillas simples y acento grave: el servidor corre con sql_mode ANSI.
-- ============================================================================

CREATE USER IF NOT EXISTS 'tramites_app'@'%' IDENTIFIED BY '<contraseña>';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `posgrados`.`tp_admins`                     TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `posgrados`.`tp_dependencias`               TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `posgrados`.`tp_documento_solicitud`        TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `posgrados`.`tp_estados_estudiantes`        TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `posgrados`.`tp_estudiante`                 TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `posgrados`.`tp_fechas_grado`               TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `posgrados`.`tp_historial_estado_solicitud` TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `posgrados`.`tp_notificaciones`             TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `posgrados`.`tp_pagos`                      TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `posgrados`.`tp_paz_y_salvo`                TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `posgrados`.`tp_programa_academico`         TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `posgrados`.`tp_roles`                      TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `posgrados`.`tp_solicitud`                  TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `posgrados`.`tp_solicitud_certificado`      TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `posgrados`.`tp_tipo_certificado`           TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `posgrados`.`tp_tipos_solicitud`            TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `posgrados`.`tp_usuario`                    TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `posgrados`.`tp_flyway_schema_history`      TO 'tramites_app'@'%';

FLUSH PRIVILEGES;

-- Comprobación: debe listar las 18 tablas y nada a nivel de base.
-- SHOW GRANTS FOR 'tramites_app'@'%';
