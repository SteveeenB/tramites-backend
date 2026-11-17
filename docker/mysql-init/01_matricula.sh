#!/bin/bash
# TP-211 · solo perfil local de docker-compose.
# Acuerdo TP-208: Trámites y Matrícula comparten UNA base (MYSQL_DATABASE).
# Carga en esa base los .sql que haya en docker/mysql-init/matricula/ (el
# esquema de Matrícula), en orden alfabético. La imagen de MySQL ejecuta este
# archivo una sola vez, cuando el volumen está vacío.
# Un error en el esquema de Matrícula no detiene el arranque: se avisa y se sigue.

DIR=/docker-entrypoint-initdb.d/matricula
SOCK="${SOCKET:-/var/run/mysqld/mysqld.sock}"

shopt -s nullglob
archivos=("$DIR"/*.sql)

if [ ${#archivos[@]} -eq 0 ]; then
    echo "[matricula] No hay .sql en $DIR: la base '$MYSQL_DATABASE' solo tendrá las tablas de Trámites."
else
    for f in "${archivos[@]}"; do
        echo "[matricula] Cargando $(basename "$f") en la base '$MYSQL_DATABASE'..."
        if MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=socket --socket="$SOCK" \
                -uroot --force --default-character-set=utf8mb4 "$MYSQL_DATABASE" < "$f"; then
            echo "[matricula] $(basename "$f") cargado."
        else
            echo "[matricula] AVISO: $(basename "$f") terminó con errores (ver arriba); se continúa."
        fi
    done
fi
shopt -u nullglob
