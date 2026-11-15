#!/bin/bash
# TP-211 · solo perfil local de docker-compose.
# Crea el usuario de Trámites (DB_USERNAME / DB_PASSWORD de .env) con permisos
# SOLO sobre las tablas tp_* de la base compartida, como pide el acuerdo TP-208
# (sección 8). No recibe ningún permiso sobre las tablas de Matrícula.
#
# MySQL no admite comodines en nombres de tabla, así que se otorga tabla por
# tabla. Incluir CREATE permite otorgar el permiso antes de que Flyway cree la
# tabla. Para el servidor institucional el script equivalente es
# docs/sql/permisos_usuario_tramites.sql (TP-209); si se agrega una tabla nueva,
# va en los dos.

# Sin "set -u": la imagen de MySQL puede hacer source de este archivo y no se
# debe cambiar el comportamiento de su entrypoint. Las validaciones son explícitas.
for var in DB_USERNAME DB_PASSWORD; do
    if [ -z "${!var:-}" ]; then
        echo "[tramites] ERROR: falta $var en .env"
        exit 1
    fi
done
SOCK="${SOCKET:-/var/run/mysqld/mysqld.sock}"

TABLAS="tp_admins tp_dependencias tp_documento_solicitud tp_estados_estudiantes
tp_estudiante tp_fechas_grado tp_historial_estado_solicitud tp_notificaciones
tp_pagos tp_paz_y_salvo tp_programa_academico tp_roles tp_solicitud
tp_solicitud_certificado tp_tipo_certificado tp_tipos_solicitud tp_usuario
tp_flyway_schema_history"

# Comillas simples para textos y acento grave para nombres: el servidor corre con
# sql_mode ANSI (ANSI_QUOTES), donde las comillas dobles son nombres, no textos.
usuario=${DB_USERNAME//\'/\'\'}
clave=${DB_PASSWORD//\'/\'\'}

{
    echo "CREATE USER IF NOT EXISTS '${usuario}'@'%' IDENTIFIED BY '${clave}';"
    for t in $TABLAS; do
        echo "GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON \`${MYSQL_DATABASE}\`.\`${t}\` TO '${usuario}'@'%';"
    done
    echo "FLUSH PRIVILEGES;"
} | MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=socket --socket="$SOCK" -uroot \
  || { echo "[tramites] ERROR creando el usuario de Trámites"; exit 1; }

echo "[tramites] Usuario '${DB_USERNAME}' creado con permisos solo sobre $(echo $TABLAS | wc -w) tablas tp_* de '${MYSQL_DATABASE}'."
