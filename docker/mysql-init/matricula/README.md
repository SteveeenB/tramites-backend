# Esquema del Módulo de Matrícula (solo perfil local)

Acuerdo TP-208: Trámites y Matrícula comparten **una sola base**. Las tablas de Trámites llevan el prefijo `tp_`; las de Matrícula conservan sus nombres.

Copia aquí el esquema vigente de Matrícula, solo estructura, por ejemplo el `mysqldump --no-data` de su base, como `10_matricula_esquema.sql`. Al levantar `docker compose --profile local up` por primera vez:

1. `01_matricula.sh` lo carga en la base `MYSQL_DATABASE`.
2. `02_usuario_tramites.sh` crea el usuario de Trámites con permisos solo sobre las tablas `tp_*`.
3. Al arrancar, el backend crea sus tablas `tp_*` con Flyway (`FLYWAY_BASELINE_VERSION=0`).

- El archivo no se versiona en este repositorio: el esquema es del otro equipo. Lo entrega su equipo o el responsable de TP-208.
- Si ya levantaste el compose antes, borra el volumen para que se vuelva a cargar: `docker compose --profile local down -v`.
- Sin este archivo el compose funciona igual; la base solo tendrá las tablas de Trámites.
