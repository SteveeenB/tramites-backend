# Corte de producción: de Railway a la BD compartida con Matrícula (Aiven)

| | |
|---|---|
| **Tarea** | TP-153 · SC02-T5 (épica SC-02, TP-139) |
| **Requiere** | TP-208 (acuerdo), TP-209 (prefijo `tp_`), PR backend #73 (paquete para Matrícula) |
| **Aprueba** | CCB (cambio en la base compartida y paso de `develop` a `main`) |
| **Estado** | Plan. **No se ejecuta sin aprobación del PO** |

## 1. Qué cambia y por qué así

Hoy producción corre `main` en Render contra **Railway**: tablas sin prefijo, `ddl-auto=update` y sin Flyway. Después
del corte, producción corre `develop` (que pasa a ser `main`) contra la **BD de Matrícula en Aiven**: tablas `tp_*`,
Flyway y el usuario `tramites_app`, que solo tiene permisos sobre las tablas `tp_*`.

Decisiones que explican el procedimiento:

- **No se restaura el respaldo de Railway tal cual en Aiven.** Su tabla `roles` chocaría con la `roles` de
  Matrícula (TP-208 §4.1), y `tramites_app` tampoco tiene permiso para crear tablas sin prefijo. Por eso las tablas
  `tp_*` las crea Matrícula con el DDL de #73 y nosotros cargamos **solo los datos**.
- **El renombrado se hace en una copia local, no en Railway.** El respaldo de Railway se restaura en el MySQL local,
  ahí se aplica V2 (renombra a `tp_*`) y de ahí sale el archivo de datos. **Railway no se modifica nunca**, así que
  sirve como vuelta atrás.
- **Se ensaya antes.** Si alguna columna de Railway no coincide con V1 (Railway fue creciendo con `ddl-auto=update`
  y con scripts manuales), el error aparece en el ensayo y no en producción.
- **Se congelan las escrituras durante el corte.** Lo que se escriba en Railway después del respaldo final se
  perdería. Se suspende el backend en Render.
- **El código viejo de `main` nunca debe conectarse a Aiven.** Con `ddl-auto=update` intentaría crear tablas sin
  prefijo. Fallaría por falta de permisos, así que no dañaría nada, pero el backend no arrancaría. Por eso se apaga
  el auto-deploy de Render y las variables se cambian con el servicio suspendido.

## 2. Antes de la ventana (sin afectar producción)

### 2.1 Condiciones para fijar la fecha

| # | Condición | Quién |
|---|---|---|
| 1 | Matrícula ejecutó `01`–`03` de `docs/entrega-matricula/` y envió el resultado | Matrícula |
| 2 | El PO recibió **por privado** la contraseña de `tramites_app`, el host y el puerto de Aiven | Matrícula → PO |
| 3 | `max_connections` de su Aiven conocido; `DB_POOL_SIZE` ajustado (`docs/POOL_CONEXIONES.md`) | Matrícula / Trámites |
| 4 | Confirmado que su Aiven tiene respaldo automático, o que hacen uno justo antes del corte | Matrícula |
| 5 | Decidido qué API de Matrícula se usa (ver 2.4) | PO |
| 6 | Ensayo de la sección 2.3 sin errores | Trámites |
| 7 | PRs `develop` → `main` (backend y frontend) abiertos, con CI verde y **aprobados por otro desarrollador**, sin fusionar | Equipo |
| 8 | Ventana acordada (30–60 min) y aviso a los usuarios | PO |
| 9 | Acta del CCB con la aprobación del corte | PO |

### 2.2 Credenciales en archivos locales (nunca en el chat ni en el repo)

En `C:\Users\<usuario>\.tramites-corte\` (fuera de cualquier repositorio) se crean dos archivos que **escribe el
PO**. Así las contraseñas no quedan en la línea de comandos ni en el historial:

`railway.cnf`: datos de Railway → servicio MySQL → *Connect* → *Public network*:
```ini
[client]
host=<host público de Railway>
port=<puerto>
user=<usuario>
password=<contraseña>
```

`aiven.cnf`: los datos de `tramites_app` que entrega Matrícula:
```ini
[client]
host=<host de Aiven>
port=<puerto>
user=tramites_app
password=<contraseña>
ssl-mode=REQUIRED
```

`local.cnf`: root del MySQL local, para el ensayo.

En los comandos siguientes, `$C` es esa carpeta y `$BIN` es la carpeta `bin` de MySQL
(`/c/Program Files/MySQL/MySQL Server 8.0/bin`). Los comandos son de Git Bash.

### 2.3 Ensayo completo en el MySQL local

```bash
F=$(date +%Y%m%d-%H%M)
# a) Respaldo completo de Railway. Solo lectura, sin bloquear la producción
"$BIN/mysqldump" --defaults-extra-file="$C/railway.cnf" --single-transaction --set-gtid-purged=OFF \
  --no-tablespaces --hex-blob --routines --triggers <base_railway> > "$C/railway_completo_$F.sql"
"$BIN/mysql" --defaults-extra-file="$C/railway.cnf" <base_railway> < docs/sql/corte/conteos_sin_prefijo.sql > "$C/conteos_railway_$F.txt"

# b) Restaurarlo en una base local y renombrar con la misma V2 del proyecto
"$BIN/mysql" --defaults-extra-file="$C/local.cnf" -e "CREATE DATABASE ensayo_railway CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
"$BIN/mysql" --defaults-extra-file="$C/local.cnf" ensayo_railway < "$C/railway_completo_$F.sql"
"$BIN/mysql" --defaults-extra-file="$C/local.cnf" ensayo_railway < src/main/resources/db/migration/V2__prefijo_tp_tablas_tramites.sql

# c) Archivo de datos (DML): solo INSERT, sin LOCK TABLES (tramites_app no tiene ese permiso) y con columnas explícitas
"$BIN/mysqldump" --defaults-extra-file="$C/local.cnf" --no-create-info --complete-insert --skip-add-locks \
  --skip-disable-keys --skip-triggers --set-gtid-purged=OFF --no-tablespaces --hex-blob \
  ensayo_railway $(echo tp_roles tp_estados_estudiantes tp_programa_academico tp_dependencias tp_tipos_solicitud \
  tp_tipo_certificado tp_fechas_grado tp_usuario tp_admins tp_estudiante tp_solicitud \
  tp_historial_estado_solicitud tp_documento_solicitud tp_pagos tp_paz_y_salvo \
  tp_solicitud_certificado tp_notificaciones) > "$C/dml_tramites_$F.sql"
```

d) Base local que imita Aiven: el esquema de Matrícula, el `sql_mode` ANSI, `sql_require_primary_key=ON`, `01` con un
usuario de ensayo y `02` con ese usuario (igual que en el ensayo del PR #73). Luego **cargar el DML con el usuario de
ensayo**, no con root:
```bash
"$BIN/mysql" -h 127.0.0.1 -u tramites_ensayo -p ensayo_compartida < "$C/dml_tramites_$F.sql"
"$BIN/mysql" --defaults-extra-file="$C/local.cnf" ensayo_compartida < docs/sql/corte/conteos_tp.sql > "$C/conteos_ensayo_$F.txt"
diff "$C/conteos_railway_$F.txt" "$C/conteos_ensayo_$F.txt" && echo "CONTEOS IGUALES"
```

e) Arrancar `develop` contra `ensayo_compartida` con `FLYWAY_BASELINE_VERSION=1`. Revisar `Successfully baselined`,
`Started TramitesApplication` y `/api/health`, e **iniciar sesión con una cuenta real** (la tuya) en el frontend local.

f) Si algo falla:
- `Unknown column` al cargar el DML: Railway tiene una columna que V1 no tiene. Hay que decidir si se descarta o si
  se agrega con una migración `V3`.
- `Data too long`: un dato no cabe en la columna de V1 (con ANSI, `STRICT_ALL_TABLES` no recorta).
- `Schema-validation` al arrancar: V1 difiere de las entidades.

Cada caso se corrige con un PR antes del corte.

g) **Limpieza:** `DROP DATABASE ensayo_railway; DROP DATABASE ensayo_compartida; DROP USER tramites_ensayo`.
Los archivos `.sql` tienen **datos personales** (cédulas, correos, hashes de contraseñas): se guardan solo en `$C`,
nunca en un repositorio, chat o nube compartida, y se borran al cerrar TP-153.

### 2.4 Qué API de Matrícula usar al corte

Al quedar en la base real de Matrícula, lo coherente es consultar **su API real** (TP-205: su PR #19 desplegado y la
cuenta técnica creada). Si no está lista para la fecha, el corte puede hacerse con la copia actual
(`posgrados-api-d4yv`) y cambiar `MATRICULA_URL` cuando esté. La base y la API son independientes, pero mientras
tanto se valida contra datos de la copia. **Lo decide el PO.**

## 3. Durante la ventana

Tiempo estimado: 30–60 min. Las tablas de Trámites son pequeñas; el respaldo y la carga tardan pocos minutos.

| # | Paso | Cómo se verifica |
|---|---|---|
| 1 | Render (backend) → *Settings* → **Auto-Deploy: Off** | El ajuste queda en Off |
| 2 | Anotar los valores actuales de `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `DB_POOL_SIZE` **en un lugar privado** (gestor de contraseñas), para la vuelta atrás | — |
| 3 | Render (backend) → **Suspend**. Desde aquí no hay escrituras | La URL del backend deja de responder |
| 4 | Pedir a Matrícula la confirmación del respaldo de su Aiven (2.1 #4) | Mensaje de Matrícula |
| 5 | Repetir **2.3 a–c** con Railway: respaldo final, conteos y DML final | `conteos_railway_<F>.txt` |
| 6 | Comprobar que las `tp_*` de Aiven están vacías: `conteos_tp.sql` con `aiven.cnf` | Todas en 0 |
| 7 | Cargar el DML en Aiven con `tramites_app`: `mysql --defaults-extra-file="$C/aiven.cnf" <base> < dml_tramites_<F>.sql` | Sin errores |
| 8 | Conteos en Aiven y `diff` contra Railway | `CONTEOS IGUALES` |
| 9 | Fusionar en GitHub los PR `develop` → `main` (backend y frontend), ya aprobados | Merge en `main`; con Auto-Deploy en Off no se despliega |
| 10 | Variables del backend en Render (las pone el PO): `DB_URL=jdbc:mysql://<host>:<puerto>/<base>?sslMode=REQUIRED&serverTimezone=UTC&characterEncoding=UTF-8`, `DB_USERNAME=tramites_app`, `DB_PASSWORD`, **`FLYWAY_BASELINE_VERSION=1`**, `DB_POOL_SIZE` y `MATRICULA_*` según 2.4. Guardar **sin desplegar** | Variables guardadas |
| 11 | **Resume** del servicio y *Manual Deploy* → *Deploy latest commit* | Deploy en curso |
| 12 | Revisar los logs | `Successfully baselined schema with version: 1`, `now at version v2`, `Started TramitesApplication` |
| 13 | Pruebas de humo (sección 4) | Todas bien |
| 14 | Auto-Deploy: On. Frontend: confirmar que desplegó su `main` | — |

## 4. Pruebas de humo

1. `GET /api/health` → `{"status":"UP"}`.
2. Login de un estudiante y de un administrador de Posgrados.
3. Ver una solicitud existente con sus documentos: el enlace de Supabase abre.
4. Crear una solicitud nueva y hacer un pago de **sandbox Wompi**. El evento llega y el pago queda aprobado.
5. Logs `[MATRICULA] ... status=200` al consultar un estudiante (si `MATRICULA_MODO=http`).
6. En Aiven, `tp_flyway_schema_history` tiene 2 filas (baseline 1 y V2) y los conteos de `tp_pagos` y `tp_solicitud`
   subieron en 1.
7. **Pagos durante la ventana:** con el backend suspendido no llegan los eventos de Wompi. Hay que revisar en el
   panel de Wompi las transacciones de esa franja y compararlas con `tp_pagos`. Por eso conviene elegir una ventana
   sin pagos en curso.

## 5. Vuelta atrás

**Cuándo:** si falla el paso 7 u 8 (carga o conteos), si el backend no arranca (paso 12) o si falla una prueba de
humo crítica (login, solicitud o pago). Se decide **dentro de la ventana**, antes de avisar a los usuarios que ya
pueden volver.

1. Render (backend) → **Suspend**.
2. Restaurar los valores anotados en el paso 2 (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_POOL_SIZE`) y **borrar**
   `FLYWAY_BASELINE_VERSION`.
3. Render → *Deploys* → **Rollback** al último deploy anterior al corte (el código viejo de `main`), y *Resume*.
4. Comprobar `/api/health` y un login. Producción queda como antes, porque **Railway no se modificó**.
5. GitHub: abrir un PR que revierta el merge de `main`, o dejar Auto-Deploy en Off hasta corregir. Lo decide el CCB.
6. Aiven: las tablas `tp_*` quedan con los datos cargados. Se vacían antes del siguiente intento
   (`TRUNCATE`, con `tramites_app`) o se borran con `04_vuelta_atras.sql` (lo ejecuta Matrícula).

Si la vuelta atrás ocurre **después** de reabrir el servicio, lo escrito en Aiven en ese intervalo no está en Railway
y habría que copiarlo a mano. Por eso las pruebas se hacen antes de avisar a los usuarios.

## 6. Después del corte

- Comentario en TP-153 con la tabla de conteos (Railway frente a Aiven) y los logs del arranque.
- Tag `v2.0.0` en los dos repos (TP-155) y acta del CCB.
- Railway queda **sin uso y sin cambios** como respaldo hasta el cierre del ciclo (TP-156). Su eliminación la decide el PO.
- Borrar los `.sql` y `.txt` de `$C` al cerrar TP-153. Contienen datos personales.
- Rotar las credenciales que estuvieron expuestas (paso 10 del plan general). La de Railway, al darlo de baja.
