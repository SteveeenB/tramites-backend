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

- `src/main/resources/db/migration/V1__esquema_base.sql`: línea base (17 tablas `tp_*`, índices y llaves foráneas).
- `src/main/resources/db/migration/V2__prefijo_tp_tablas_tramites.sql`: renombra a `tp_*` las tablas sin prefijo de la BD actual de Trámites en Railway (TP-209).
- `src/main/resources/db/migration/V3__catalogos_base.sql`: catálogos obligatorios (roles, estados del estudiante, programas con su código de Matrícula, dependencias, tipos de certificado y de solicitud). Usa `INSERT IGNORE`, así que en una base que ya los tiene no cambia nada (TP-153).
- `application.properties`: configuración `spring.flyway.*` (historial en `tp_flyway_schema_history`, baseline con `FLYWAY_BASELINE_VERSION`) y `ddl-auto=${DDL_AUTO:validate}`.
- `application-h2.properties` y las pruebas: Flyway desactivado (ver más abajo).
- `pom.xml`: `spring-boot-starter-flyway` y `flyway-mysql`.

## Cómo se comporta en cada entorno

Desde TP-209 la base se comparte con el Módulo de Matrícula (acuerdo TP-208, `docs/BD_COMPARTIDA_MATRICULA.md`). Las 17 tablas de Trámites llevan el prefijo `tp_` y el historial de Flyway es `tp_flyway_schema_history` (`spring.flyway.table`), para no chocar con Matrícula.

| Entorno | `FLYWAY_BASELINE_VERSION` | Qué hace Flyway al arrancar |
|---|---|---|
| **BD vacía** (local, staging, BD nueva) | `0` (por defecto) | Ejecuta `V1` (crea las tablas `tp_*`) y `V2` (no hace nada). Hibernate valida. |
| **BD compartida con Matrícula** (institucional, perfil `local` del compose) | `0` (por defecto) | La base ya tiene tablas de Matrícula, así que Flyway la considera "no vacía" y hace *baseline*. Con `0`, el baseline queda **por debajo** de V1 y se ejecutan `V1` y `V2` igual que en una BD vacía. No toca ninguna tabla de Matrícula. |
| **BD actual de Trámites en Railway** (tablas sin prefijo, con datos, sin `tp_flyway_schema_history`) | `1` | Crea `tp_flyway_schema_history` y marca la versión 1 **sin ejecutar V1**. `V2` renombra las 17 tablas a `tp_*` en un solo `RENAME TABLE`, conservando datos, índices y llaves foráneas. |
| **BD de Matrícula en Aiven con las tablas `tp_*` ya creadas** por el DDL entregado (`docs/entrega-matricula/02_ddl_tablas_tramites.sql`, copia de V1), vacías y sin `tp_flyway_schema_history` | `1` | Crea `tp_flyway_schema_history` y marca la versión 1 **sin ejecutar V1**, porque las tablas ya existen. `V2` no hace nada: en esa base solo está el `roles` de Matrícula, no las 17 tablas sin prefijo. Hibernate valida contra las tablas de V1. |
| **Perfil `h2` y pruebas** | — | Flyway desactivado. Hibernate crea el esquema `tp_*` (`create-drop`) y `data.sql` siembra los datos demo. |

> Los scripts están escritos para MySQL. Por eso no se ejecutan sobre H2.

Por qué el baseline es configurable: con `baseline-version=1` fijo, una base compartida (que ya tiene las tablas de Matrícula) se marcaría como versión 1 y **V1 nunca crearía nuestras tablas**. Al revés, en Railway V1 no debe correr porque las tablas ya existen.

Errores al elegir el valor:

- `1` en una base compartida: V1 se salta, V2 no encuentra las 17 tablas viejas y no hace nada, y Hibernate falla con `Schema-validation: missing table [tp_...]`. No se toca ningún dato. Se corrige poniendo `0`, borrando la tabla `tp_flyway_schema_history` completa (no basta con la fila de baseline: V2 también quedó registrada como aplicada) y volviendo a arrancar.
- `0` en Railway: V1 crea tablas `tp_*` vacías y la migración falla, en V1 con `Duplicate foreign key constraint name 'fk_...'` (si las llaves foráneas existentes tienen los nombres de V1) o en V2 con `Table 'tp_roles' already exists`. Los datos siguen en las tablas sin prefijo. Se corrige con un respaldo, borrando las tablas `tp_*` vacías y `tp_flyway_schema_history`, poniendo `1` y volviendo a arrancar.

Estos cuatro casos (base vacía o compartida con `0`, Railway con `1` y los dos valores equivocados), y la recuperación del segundo, se probaron en TP-209 sobre MySQL 8.0.44 con el `sql_mode` ANSI del servidor compartido y el esquema de Matrícula cargado.

Si Railway ya tenía una tabla `flyway_schema_history` (sin prefijo) de un despliegue anterior, queda huérfana: Flyway ya no la usa y se puede borrar después de verificar el arranque.

V2 solo renombra si existen **las 17** tablas sin prefijo. En la base compartida Matrícula tiene su propia `roles`, pero es una sola, así que V2 nunca la toca.

V3 corre en todos los escenarios con MySQL, con cualquier valor de `FLYWAY_BASELINE_VERSION`, porque su versión es mayor que el baseline. En una base nueva carga los catálogos; en una que ya los tiene (la de Railway) no inserta nada. No trae usuarios ni datos demo: las cuentas iniciales de una base nueva se crean aparte y no van en el repositorio. En el perfil `h2` los catálogos siguen viniendo de `data.sql`.

## Cómo hacer un cambio de esquema desde ahora

1. Modificar la entidad (agregar campo, tabla, relación…).
2. Crear `src/main/resources/db/migration/V<siguiente>__<descripcion_en_snake_case>.sql` con el `ALTER TABLE` / `CREATE TABLE` correspondiente. Ejemplo: `V3__agregar_telefono_admin.sql`. Las tablas nuevas llevan el prefijo `tp_`, y cada una necesita su `GRANT` en `docs/sql/permisos_usuario_tramites.sql` y en `docker/mysql-init/02_usuario_tramites.sh`.
3. Probar contra una BD MySQL (ver "Verificación").
4. Subir entidad y migración **en el mismo PR**.

Reglas:

- **Nunca** editar una migración ya aplicada en algún entorno (Flyway falla por checksum). Si algo quedó mal, se corrige con una migración nueva.
- Un archivo por cambio lógico, con nombre descriptivo en español.
- Los datos demo no van en migraciones; siguen en `data.sql` (solo perfil `h2`). Los catálogos que sí deban existir en producción (roles, estados) pueden ir en una migración propia si el equipo lo decide.

## Verificación recomendada antes de desplegar

1. **BD vacía o compartida.** Con `docker compose --profile local up` y un volumen limpio (`down -v`), o con una base temporal en el mismo MySQL, con `FLYWAY_BASELINE_VERSION=0`. En el log deben aparecer `Migrating schema ... to version "1 - esquema base"` y `... to version "2 - prefijo tp tablas tramites"`, y la app debe levantar sin errores de `validate`. `SHOW TABLES` lista las `tp_*` junto a las de Matrícula.
2. **Renombrado sobre la BD de Railway.** Hacer un respaldo (`mysqldump`) y arrancar con `FLYWAY_BASELINE_VERSION=1`. En el log deben aparecer `Successfully baselined schema with version: 1` y `Migrating schema ... to version "2 - prefijo tp tablas tramites"`. Después, `SHOW TABLES` lista solo las `tp_*` de Trámites, con los mismos conteos de filas que antes.

## Salida de emergencia

Si un despliegue falla porque `validate` encuentra una diferencia y no hay tiempo de escribir la migración, en Render se puede definir `DDL_AUTO=update` temporalmente. Después hay que escribir la migración que corresponda y quitar la variable.
