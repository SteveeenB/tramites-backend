# Despliegue del Módulo de Trámites de Posgrado

> Guía para el equipo que despliega en el servidor institucional de la UFPS. TP-211 · SC02-T11 (épica SC-02).
> El módulo se entrega como dos imágenes Docker (backend y frontend) y un `docker-compose.yml`. La base de datos es **la misma base MySQL del Módulo de Matrícula**: las tablas de Trámites llevan el prefijo `tp_` (acuerdo TP-208, `docs/BD_COMPARTIDA_MATRICULA.md`).

## 1. Requisitos

- Docker Engine 24 o superior con Docker Compose **2.20 o superior** (`docker compose version`). Probado con Docker 29.9 y Compose 5.7.
- Unos 5 GB libres en disco para las imágenes y el build.
- Acceso a internet durante el build (descarga de dependencias de Maven y npm).
- Para el perfil institucional: la base MySQL compartida con Matrícula y un usuario de Trámites con los permisos de la sección 6.
- Puertos libres en el host: 8080 (backend) y 8081 (frontend). Se cambian con `BACKEND_PORT` y `FRONTEND_PORT`.

## 2. Obtener el código

Los dos repositorios deben quedar en la misma carpeta, uno al lado del otro. Usa el tag de la entrega en ambos (por ejemplo `v2.0.0`):

```bash
mkdir tramites && cd tramites
git clone --branch v2.0.0 https://github.com/SteveeenB/tramites-backend.git
git clone --branch v2.0.0 https://github.com/SteveeenB/tramites-frontend.git
cd tramites-backend
cp .env.example .env
```

Todos los comandos siguientes se ejecutan dentro de `tramites-backend/`. Si el frontend queda en otra ruta, indícala en `FRONTEND_DIR`.

## 3. Variables de entorno

Se configuran en `.env`, que no se sube al repositorio. `.env.example` trae todas con valores de ejemplo. Ninguna imagen contiene credenciales.

### Obligatorias

| Variable | Contenedor | Para qué |
|---|---|---|
| `DB_URL` | backend | JDBC de la base compartida, ej. `jdbc:mysql://host:3306/posgrados?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8` |
| `DB_USERNAME`, `DB_PASSWORD` | backend | Usuario de BD de Trámites, con permisos solo sobre las tablas `tp_*` (sección 6). La contraseña se genera con `openssl rand -base64 24` |
| `FLYWAY_BASELINE_VERSION` | backend | **`0` en la base compartida**, para que Flyway cree las tablas `tp_*` aunque la base ya tenga las de Matrícula (sección 5) |
| `JWT_SECRET` | backend | Firma de los tokens. 32 caracteres o más (`openssl rand -base64 48`). Sin él el backend no arranca, a propósito |
| `CORS_ORIGIN` | backend | URL pública del frontend, ej. `https://tramites.ufps.edu.co` |
| `FRONTEND_URL` | backend | La misma URL pública del frontend; va en el QR de los certificados |
| `API_URL` | frontend | URL pública del backend **como la ve el navegador**, terminada en `/api`, ej. `https://tramites-api.ufps.edu.co/api` |
| `MATRICULA_MODO`, `MATRICULA_URL`, `MATRICULA_USUARIO`, `MATRICULA_PASSWORD`, `MATRICULA_SINCRONIZAR` | backend | Integración con Matrícula. En producción: `MATRICULA_MODO=http`, la URL con el prefijo `/posgrados-project`, la cuenta técnica y `MATRICULA_SINCRONIZAR=true` |
| `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_BUCKET` | backend | Almacenamiento de los documentos que suben los estudiantes |
| `WOMPI_PUBLIC_KEY`, `WOMPI_INTEGRITY_KEY`, `WOMPI_EVENTS_SECRET`, `WOMPI_REDIRECT_URL` | backend | Pagos. `WOMPI_REDIRECT_URL` = `<URL del frontend>/pago/resultado` |

### Opcionales

| Variable | Por defecto | Para qué |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` | Dejar `local`. **Nunca `dev`**: activa un login sin contraseña |
| `DB_POOL_SIZE`, `DB_POOL_MIN_IDLE` | 10 / 2 | Pool de conexiones. Debe cumplir `DB_POOL_SIZE × réplicas + conexiones de Matrícula < max_connections`. Ver `docs/POOL_CONEXIONES.md` |
| `DDL_AUTO` | `validate` | Solo emergencia: `update` temporal si la validación del esquema falla (sección 5) |
| `PORT` | 8080 | Puerto interno del backend. El compose lo fija en 8080 |
| `BACKEND_PORT`, `FRONTEND_PORT` | 8080 / 8081 | Puertos publicados en el host |
| `TAG` | `latest` | Etiqueta de las imágenes construidas |
| `FRONTEND_DIR` | `../tramites-frontend` | Ruta del repositorio del frontend |
| `BUILDX_BAKE_ENTITLEMENTS_FS` | `0` en `.env.example` | Permite que Compose construya el frontend desde `FRONTEND_DIR`, que está fuera de la carpeta del backend. Dejarla en `0` |
| `GOOGLE_CLIENT_ID` | vacío | Login con Google (backend y frontend) |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | vacío | Correo SMTP (Gmail). Sin ellas no se envían correos y el resto funciona |
| `REACT_APP_DEMO_MODE` | `false` | Solo en el build del frontend. Debe quedar en `false` |
| `JAVA_OPTS` | `-XX:MaxRAMPercentage=75.0` | Opciones de la JVM del backend |
| `MYSQL_VERSION`, `MYSQL_ROOT_PASSWORD`, `MYSQL_DATABASE`, `MYSQL_PORT` | `8.4.11` / — / `posgrados` / 3307 | Solo perfil `local` (MySQL del compose). `MYSQL_ROOT_PASSWORD` es obligatoria (`openssl rand -base64 24`) |

## 4. Levantar el módulo

El compose tiene dos perfiles. Sin `--profile` no levanta nada.

| Perfil | Servicios | Uso |
|---|---|---|
| `local` | `mysql` + `backend` + `frontend` | Probar en una máquina limpia, con un MySQL propio que reproduce la base compartida: esquema de Matrícula + tablas `tp_*` de Trámites, mismo `sql_mode` ANSI |
| `institucional` | `backend` + `frontend` | Servidor de la UFPS: el backend usa el MySQL indicado en `DB_URL` |

### Perfil institucional (servidor UFPS)

1. Crea el usuario de Trámites con el script de la sección 6.
2. En `.env`, apunta `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` a la base compartida, deja `FLYWAY_BASELINE_VERSION=0` y define las URL públicas (`API_URL`, `CORS_ORIGIN`, `FRONTEND_URL`, `WOMPI_REDIRECT_URL`).
3. Construye y levanta:
   ```bash
   docker compose --profile institucional up -d --build
   ```
4. Sigue el arranque del backend (tarda 30 a 60 s) y verifica (sección 7):
   ```bash
   docker compose --profile institucional logs -f backend
   ```

### Perfil local (prueba en una máquina limpia)

1. En `.env` deja `DB_URL` con el host `mysql` y la base `MYSQL_DATABASE` (como vienen en `.env.example`), deja `FLYWAY_BASELINE_VERSION=0` y define `MYSQL_ROOT_PASSWORD`, `DB_PASSWORD` y `JWT_SECRET`.
2. Opcional, pero es lo que prueba la convivencia: copia el esquema de Matrícula (solo estructura) en `docker/mysql-init/matricula/`. Se carga en **la misma base** `MYSQL_DATABASE` (ver el README de esa carpeta).

Al crear el volumen, el MySQL local:
- arranca con el `sql_mode` ANSI y la collation `utf8mb4_0900_ai_ci` del servidor de Matrícula;
- carga el esquema de Matrícula (`docker/mysql-init/01_matricula.sh`);
- crea el usuario de Trámites con permisos solo sobre las tablas `tp_*`, tabla por tabla (`docker/mysql-init/02_usuario_tramites.sh`).

Luego, al arrancar, el backend crea sus tablas `tp_*` con Flyway.

3. Levanta:
   ```bash
   docker compose --profile local up -d --build
   ```
4. Abre el frontend en <http://localhost:8081> y el backend en <http://localhost:8080/swagger-ui.html>.

La base local arranca sin datos (sin usuarios). Sirve para comprobar que el módulo levanta y crea su esquema junto al de Matrícula; los datos reales se cargan en la BD institucional.

### Orden de arranque

1. `mysql` (solo perfil local) hasta que su health check responde.
2. `backend`: Flyway aplica las migraciones y Hibernate valida el esquema; luego queda escuchando en 8080.
3. `frontend`: genera `/config.js` con `API_URL` y sirve la aplicación con nginx en el puerto interno 8080.

### Comandos útiles

```bash
docker compose --profile institucional ps            # estado y salud de los contenedores
docker compose --profile institucional logs backend  # logs del backend
docker compose --profile institucional down          # detener (los datos de la BD no se tocan)
docker compose --profile local down -v               # detener y BORRAR el MySQL local
```

## 5. Esquema de base de datos (Flyway)

- Las tablas `tp_*` de Trámites las crea y evoluciona **Flyway** al arrancar el backend, con los scripts de `src/main/resources/db/migration`. Hibernate no altera tablas: solo valida (`DDL_AUTO=validate`).
- Migraciones actuales: `V1` crea las 17 tablas `tp_*`; `V2` renombra a `tp_*` las tablas sin prefijo **solo si existen las 17** (la base de Aiven). En cualquier otra base no hace nada. Nunca renombra el `roles` de Matrícula.
- **`FLYWAY_BASELINE_VERSION` según la base:**

  | Base | Valor | Qué corre |
  |---|---|---|
  | Nueva (vacía) | `0` (por defecto) | V1 crea las tablas `tp_*`; V2 no hace nada |
  | Compartida con Matrícula (institucional o perfil local) | `0` (por defecto) | Igual. La base ya tiene las tablas de Matrícula y Flyway la considera "no vacía": con `0`, de todos modos ejecuta V1 |
  | Actual de Trámites (Aiven, tablas sin prefijo y con datos) | `1` | Se salta V1; V2 renombra las 17 tablas a `tp_*` conservando los datos |

  - Si se pone `1` en la base compartida, el backend **falla al arrancar** en la validación (`missing table [tp_...]`), sin tocar ningún dato.
  - Si se pone `0` en la base de Aiven, V1 crea tablas `tp_*` vacías y V2 falla (`Table 'tp_roles' already exists`): el backend no arranca y los datos siguen en las tablas sin prefijo (sección 9).
- El historial queda en la tabla `tp_flyway_schema_history`. Ninguna migración de Trámites crea, altera ni borra tablas del Módulo de Matrícula.
- En los logs, el aviso `Using MySQL 8.4 which is newer than the version Flyway has been verified with` es informativo: V1 se probó sobre MySQL 8.4 y funciona.
- Si el backend no arranca por un error de validación del esquema, se puede arrancar una vez con `DDL_AUTO=update` y avisar al equipo de Trámites para que escriba la migración. No debe quedarse así.

Detalle en `docs/MIGRACIONES.md`.

## 6. Usuario de base de datos

El usuario de Trámites debe tener permisos **solo sobre las tablas `tp_*`**, para que un error del módulo no pueda dañar los datos de Matrícula:

- `SELECT`, `INSERT`, `UPDATE`, `DELETE` para la aplicación;
- `CREATE`, `ALTER`, `INDEX`, `REFERENCES` para que Flyway aplique las migraciones;
- ningún permiso a nivel de toda la base: un `ALTER` o `DROP` sobre `base.*` permitiría modificar tablas de Matrícula.

MySQL no admite comodines en nombres de tabla (`tp_%`), así que los permisos se otorgan **tabla por tabla** sobre las 17 tablas `tp_*` y `tp_flyway_schema_history`. Como el permiso incluye `CREATE`, se puede otorgar antes de que Flyway cree la tabla:

```sql
CREATE USER 'tramites_app'@'%' IDENTIFIED BY '<contraseña>';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON `posgrados`.`tp_solicitud` TO 'tramites_app'@'%';
-- … una línea igual por cada tabla tp_*
```

El script completo para el servidor institucional es `docs/sql/permisos_usuario_tramites.sql` (TP-209): se reemplazan la base y la contraseña y se ejecuta con un usuario administrador. El perfil local aplica los mismos permisos en `docker/mysql-init/02_usuario_tramites.sh`, que sirve de referencia con la lista de tablas. Cada tabla nueva de una migración futura necesita su línea en los dos.

## 7. Verificación

```bash
curl http://localhost:8080/api/health
# {"status":"UP"}
```

- Swagger: `http://<host>:8080/swagger-ui.html` debe abrir la documentación de la API.
- Frontend: `http://<host>:8081` muestra el inicio de sesión. Al entrar directo a una ruta interna (por ejemplo `/tramites`) y recargar no debe dar 404.
- `docker compose ps` muestra el backend como `healthy`. La imagen trae un `HEALTHCHECK` contra `/api/health` (90 s de gracia al arrancar).
- En los logs del backend debe aparecer la línea de Flyway (`Successfully applied` o `Schema ... is up to date`) y `Started TramitesApplication`.
- Con Matrícula en modo `http`, al iniciar sesión un estudiante aparecen en los logs las líneas `[MATRICULA] ... status=200`.

## 8. Seguridad de las imágenes

- Backend: corre con el usuario sin privilegios `tramites` (uid 10001). Frontend: nginx sin root (`nginx-unprivileged`), puerto interno 8080.
- Ninguna credencial está en las imágenes ni en el repositorio; todo llega por `.env`. Comprobar con `docker history --no-trunc tramites-backend`.
- Imágenes base fijadas por versión. Backend: `maven:3.9.9-eclipse-temurin-17` y `eclipse-temurin:17.0.15_6-jre-jammy`. Frontend: las que fija su `Dockerfile` (Node y `nginxinc/nginx-unprivileged`, TP-210). Perfil local: `mysql:${MYSQL_VERSION}`, por defecto `8.4.11`.

## 9. Problemas comunes

| Síntoma | Causa probable | Solución |
|---|---|---|
| El backend se reinicia con `Could not resolve placeholder 'JWT_SECRET'` | Falta `JWT_SECRET` en `.env` | Definirla (32 caracteres o más) |
| `Communications link failure` al arrancar | `DB_URL` incorrecta o MySQL inaccesible desde el contenedor | Revisar host y puerto. Dentro del compose local el host es `mysql`, no `localhost` |
| `Schema-validation: missing table [tp_...]` en la base compartida | Falta `FLYWAY_BASELINE_VERSION=0`: Flyway hizo *baseline* y no creó las tablas `tp_*` | Poner `FLYWAY_BASELINE_VERSION=0`, borrar la fila de baseline de `tp_flyway_schema_history` y volver a arrancar |
| `Table 'tp_roles' already exists` en V2, contra la base de Aiven | Se arrancó con `FLYWAY_BASELINE_VERSION=0`: V1 creó tablas `tp_*` vacías y V2 no pudo renombrar las que tienen los datos | Hacer respaldo, borrar las 17 tablas `tp_*` vacías y `tp_flyway_schema_history`, poner `FLYWAY_BASELINE_VERSION=1` y volver a arrancar |
| `Schema-validation: missing table` en otra situación | Esquema desalineado con las entidades | Revisar `tp_flyway_schema_history`; emergencia: `DDL_AUTO=update` una vez |
| `SELECT command denied` o `CREATE command denied` sobre una tabla `tp_*` | Al usuario de Trámites le falta el permiso de esa tabla | Agregar su línea `GRANT` (sección 6) |
| El navegador muestra errores de CORS | `CORS_ORIGIN` no coincide con la URL del frontend | Usar exactamente la URL pública del frontend, sin `/` al final |
| El frontend llama a `localhost:8080` en producción | `API_URL` no definida o apunta al contenedor | Definir la URL pública del backend y reiniciar solo el frontend (no hace falta reconstruir) |
| `Connection is not available, request timed out` | Pool agotado o `max_connections` del servidor alcanzado | Ajustar `DB_POOL_SIZE` según `docs/POOL_CONEXIONES.md` |
| `service "mysql" ... not enabled` o `required` no reconocido | Docker Compose anterior a 2.20 | Actualizar Docker Compose |
| El build se detiene con `additional privileges requested: pass "--allow=fs.read=.."` | Falta `BUILDX_BAKE_ENTITLEMENTS_FS=0` en `.env` | Agregarla como viene en `.env.example` |
| `unable to prepare context: path ".../tramites-frontend" not found` | El frontend no está al lado del backend | Indicar la ruta correcta en `FRONTEND_DIR` |
| El frontend se reinicia con `set: illegal option -` | El repo se clonó en Windows y `docker-entrypoint.sh` quedó con fin de línea CRLF | Clonar en Linux, o en Windows clonar con `git clone -c core.autocrlf=input` |
| MySQL se reinicia con `exec format error` | La imagen `mysql` se descargó incompleta (por ejemplo, con el disco lleno) | `docker image rm mysql:<MYSQL_VERSION>` y volver a levantar |

## 10. Contacto

Equipo de Trámites de Posgrado (Ingeniería de Sistemas, UFPS). Product Owner: Johan Steven Bueno Rojas. Responsable del empaquetado: Santiago Danilo Cepeda Galeano.
