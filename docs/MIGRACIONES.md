# Migraciones de esquema con Flyway

**HU / tarea:** TP-150 · SC02-T2 — Adoptar Flyway sobre el esquema actual
**Solicitud de cambio:** SC-02 (migración al servidor y BD institucional UFPS)
**Riesgo que cierra:** R6 — sin migraciones de BD formales (`ddl-auto=update`)

## Qué cambió

| Antes | Ahora |
|---|---|
| Hibernate creaba y alteraba tablas al arrancar (`ddl-auto=update`). | Flyway crea y evoluciona el esquema con scripts SQL versionados. |
| No había forma de reproducir el esquema exacto en otro entorno. | `V1__esquema_base.sql` crea el esquema completo en una BD vacía. |
| Los cambios de esquema quedaban implícitos en las entidades. | Cada cambio queda en un archivo `V<n>__*.sql` revisable en el PR. |
| Hibernate podía tocar una BD que no es nuestra. | Hibernate solo valida (`ddl-auto=validate`). |

Archivos:

- `src/main/resources/db/migration/V1__esquema_base.sql`: línea base (17 tablas, índices y llaves foráneas).
- `application.properties`: configuración `spring.flyway.*` y `ddl-auto=${DDL_AUTO:validate}`.
- `application-h2.properties` y las pruebas: Flyway desactivado (ver más abajo).
- `pom.xml`: `spring-boot-starter-flyway` y `flyway-mysql`.

## Cómo se comporta en cada entorno

| Entorno | Qué hace Flyway al arrancar |
|---|---|
| **BD vacía** (local, staging, BD institucional nueva) | Ejecuta `V1` y crea todo el esquema. Hibernate valida. |
| **BD existente sin historial de Flyway** (Railway, producción actual) | `baseline-on-migrate`: crea la tabla `flyway_schema_history` y marca la versión 1 **sin ejecutar V1**. Desde ahí aplica solo V2, V3… Hibernate valida. |
| **Perfil `h2` y pruebas** | Flyway desactivado. Hibernate crea el esquema (`create-drop`) y `data.sql` siembra los datos demo, igual que antes. |

> Los scripts están escritos para MySQL. Por eso no se ejecutan sobre H2.

## Cómo hacer un cambio de esquema desde ahora

1. Modificar la entidad (agregar campo, tabla, relación…).
2. Crear `src/main/resources/db/migration/V<siguiente>__<descripcion_en_snake_case>.sql` con el `ALTER TABLE` / `CREATE TABLE` correspondiente. Ejemplo: `V2__agregar_telefono_admin.sql`.
3. Probar contra una BD MySQL (ver "Verificación").
4. Subir entidad y migración **en el mismo PR**.

Reglas:

- **Nunca** editar una migración ya aplicada en algún entorno (Flyway falla por checksum). Si algo quedó mal, se corrige con una migración nueva.
- Un archivo por cambio lógico, con nombre descriptivo en español.
- Los datos demo no van en migraciones; siguen en `data.sql` (solo perfil `h2`). Los catálogos que sí deban existir en producción (roles, estados) pueden ir en una migración propia si el equipo lo decide.

## Verificación recomendada antes de desplegar

1. **V1 sobre una BD vacía.** Crear una base temporal en el mismo MySQL (por ejemplo `CREATE DATABASE tramites_flyway_prueba;`), apuntar `DB_URL` a ella y arrancar. En el log debe aparecer `Migrating schema ... to version "1 - esquema base"` y la app debe levantar sin errores de `validate`. Al terminar, borrar esa base.
2. **Baseline sobre la BD real.** Hacer un respaldo (`mysqldump`) y arrancar contra la BD de Railway. En el log debe aparecer `Successfully baselined schema with version: 1`. Si `validate` reporta una columna faltante o con otro tipo, esa diferencia se corrige con `V2__...sql`.

## Salida de emergencia

Si un despliegue falla porque `validate` encuentra una diferencia y no hay tiempo de escribir la migración, en Render se puede definir `DDL_AUTO=update` temporalmente. Después hay que escribir la migración que corresponda y quitar la variable.
