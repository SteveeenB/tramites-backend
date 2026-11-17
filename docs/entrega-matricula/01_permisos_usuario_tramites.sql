-- ============================================================================
-- 01_permisos_usuario_tramites.sql — Usuario de BD del Módulo de Trámites
-- (TP-209 · acuerdo TP-208, sección 8, docs/BD_COMPARTIDA_MATRICULA.md)
--
-- Para la BD de Matrícula en Aiven (base compartida). Lo ejecuta un
-- usuario administrador (p. ej. avnadmin) ANTES de 02_ddl_tablas_tramites.sql.
--
-- Antes de ejecutarlo:
--   1. Si la base no se llama defaultdb, reemplazar defaultdb en todas las líneas.
--   2. Reemplazar <contraseña> por una nueva (openssl rand -base64 24) y pasarla
--      al PO de Trámites por un canal privado (nunca por el grupo).
--   3. Si el usuario no se llama tramites_app, cambiarlo aquí y avisar al PO.
--
-- El usuario tiene permisos SOLO sobre las tablas tp_* de Trámites y su
-- historial de Flyway; ninguno sobre las tablas de Matrícula ni a nivel de
-- toda la base. MySQL no admite comodines en nombres de tabla, así que se
-- otorga tabla por tabla; como incluye CREATE, se puede otorgar antes de que
-- Flyway cree la tabla.
--
-- La lista es la misma de docker/mysql-init/02_usuario_tramites.sh (perfil
-- local) y la de docs/sql/permisos_usuario_tramites.sql. Esta carpeta es una
-- copia para la entrega a Matrícula (16/11/2026).
-- Solo comillas simples y acento grave: el servidor corre con sql_mode ANSI.
-- ============================================================================

CREATE USER IF NOT EXISTS 'tramites_app'@'%' IDENTIFIED BY '<contraseña>';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `defaultdb`.`tp_admins`                     TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `defaultdb`.`tp_dependencias`               TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `defaultdb`.`tp_documento_solicitud`        TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `defaultdb`.`tp_estados_estudiantes`        TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `defaultdb`.`tp_estudiante`                 TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `defaultdb`.`tp_fechas_grado`               TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `defaultdb`.`tp_historial_estado_solicitud` TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `defaultdb`.`tp_notificaciones`             TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `defaultdb`.`tp_pagos`                      TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `defaultdb`.`tp_paz_y_salvo`                TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `defaultdb`.`tp_programa_academico`         TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `defaultdb`.`tp_roles`                      TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `defaultdb`.`tp_solicitud`                  TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `defaultdb`.`tp_solicitud_certificado`      TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `defaultdb`.`tp_tipo_certificado`           TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `defaultdb`.`tp_tipos_solicitud`            TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `defaultdb`.`tp_usuario`                    TO 'tramites_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `defaultdb`.`tp_flyway_schema_history`      TO 'tramites_app'@'%';

FLUSH PRIVILEGES;

-- Comprobación: debe listar las 18 tablas y nada a nivel de base.
-- SHOW GRANTS FOR 'tramites_app'@'%';
