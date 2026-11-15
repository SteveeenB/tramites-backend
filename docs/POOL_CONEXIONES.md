# Credenciales y pool de conexiones

> TP-152 · SC02-T4 (épica SC-02). Cierra el riesgo R4 (credenciales versionadas). Revisado el 15 nov 2026 sobre `develop`.

## 1. Credenciales fuera del código

Toda la configuración sensible se lee de variables de entorno en `src/main/resources/application.properties`. Ninguna tiene un valor real por defecto:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET` no tienen valor por defecto. Si faltan, el arranque falla a propósito.
- `SUPABASE_*`, `WOMPI_*`, `MAIL_*`, `MATRICULA_USUARIO`, `MATRICULA_PASSWORD` y `GOOGLE_CLIENT_ID` tienen valor por defecto vacío.
- `application-local.properties` y `application-prod.properties` están en `.gitignore`.
- Los únicos valores escritos en el repo son de prueba: `jwt.secret` de `application-h2.properties` y de `TramitesApplicationTests` (los dos dicen *not-for-production*) y el usuario `sa` sin contraseña de H2.

Verificación (debe devolver solo referencias `${...}` o valores de prueba):

```bash
git grep -n -i "password=\|secret=\|key=" -- src
```

Resultado del 15 nov 2026: sin valores reales. Las coincidencias son `${DB_PASSWORD}`, `${JWT_SECRET}`, `${SUPABASE_SERVICE_ROLE_KEY:}`, `${MAIL_PASSWORD:}`, `${WOMPI_*:}`, `${MATRICULA_PASSWORD:}`, el `?public-key=` que `WompiService` arma con la variable y los valores de prueba de H2.

### Credenciales antiguas del historial (rotadas en TP-170)

El repositorio es público y su **historial de git** conserva credenciales que se escribieron en versiones antiguas de `application*.properties`, antes de pasarlas a variables de entorno:

- la contraseña de la base Postgres de Supabase usada al inicio;
- dos contraseñas de aplicación de Gmail (`spring.mail.password`);
- la *service-role key* de Supabase.

Borrarlas del código no las borra del historial, por eso se **rotaron** en TP-170 en lugar de reescribir el historial: los valores que aparecen ahí ya no son válidos. Si alguna vez se vuelve a versionar una credencial por error, el procedimiento es el mismo: rotarla con el dueño de la cuenta y actualizar la variable de entorno donde se despliega.

## 2. Tamaño del pool (HikariCP)

| Variable | Valor por defecto | Para qué |
|---|---|---|
| `DB_POOL_SIZE` | 10 | Máximo de conexiones abiertas por réplica del backend |
| `DB_POOL_MIN_IDLE` | 2 | Conexiones que se mantienen abiertas sin uso |

Los tiempos (`connection-timeout` 20 s, `idle-timeout` 5 min, `max-lifetime` 10 min, `keepalive-time` 2 min) son fijos en `application.properties`.

### Regla

La BD MySQL se comparte con el Módulo de Matrícula, así que el pool de Trámites no puede quedarse con todas las conexiones del servidor:

```
DB_POOL_SIZE × réplicas del backend  +  pool de Matrícula  +  margen de administración  <  max_connections
```

El valor de `max_connections` se consulta en el servidor destino:

```sql
SHOW VARIABLES LIKE 'max_connections';
SHOW STATUS LIKE 'Max_used_connections';
```

### Valor recomendado

**`DB_POOL_SIZE=10` con una réplica.** Por qué:

- Con 3 conexiones, tres peticiones lentas a la vez (subida de documentos a Supabase, generación de un PDF) dejaban al resto del backend esperando y devolvían 500 `Connection is not available`.
- `spring.jpa.open-in-view=false` evita que las suscripciones SSE de notificaciones retengan una conexión cada una, así que 10 alcanzan para la carga de un módulo de posgrados.
- Spring Boot usa 10 por defecto en HikariCP.

Ejemplos con una réplica de Trámites y un pool de 10 en Matrícula (el valor por defecto de Spring Boot), dejando 5 conexiones para administración:

| `max_connections` del servidor | `DB_POOL_SIZE` de Trámites |
|---|---|
| 151 (valor por defecto de MySQL 8) | 10 (sobra margen) |
| 50 | 10 |
| 25 | 8 (10 + 8 + 5 = 23) |
| 20 | 4 (10 + 4 + 5 = 19) |

Si se levantan **dos réplicas** del backend, el pool se multiplica por dos: hay que volver a hacer la cuenta.

**Pendiente (TP-149):** el valor real de `max_connections` de la BD compartida lo confirma el equipo del ing. Matías. Con ese dato se revisa esta tabla; si es 50 o más, no hay que cambiar nada.

## 3. Variables del backend (para `.env.example` y `DEPLOY.md`, TP-211)

Obligatorias: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (32 caracteres o más), `CORS_ORIGIN`, `FRONTEND_URL`; en producción también `MATRICULA_MODO=http`, `MATRICULA_URL`, `MATRICULA_USUARIO`, `MATRICULA_PASSWORD`, `MATRICULA_SINCRONIZAR`, `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` y las cuatro `WOMPI_*`.

Opcionales, con su valor por defecto: `PORT` (8080), `SPRING_PROFILES_ACTIVE` (`local`; nunca `dev` en producción), `DB_POOL_SIZE` (10), `DB_POOL_MIN_IDLE` (2), `DDL_AUTO` (`validate`; el esquema lo gestiona Flyway), `SHOW_SQL` (false), `SUPABASE_BUCKET` (`tramites-documentos`), `WOMPI_REDIRECT_URL`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_TIMEOUT_MS` (5000), `GOOGLE_CLIENT_ID`, `MATRICULA_TIMEOUT_CONEXION_MS` (2000), `MATRICULA_TIMEOUT_RESPUESTA_MS` (5000), `MATRICULA_MAX_REINTENTOS` (2), `MATRICULA_PRESUPUESTO_MS` (15000), `MATRICULA_SNAPSHOT_VIGENCIA_HORAS` (24).
