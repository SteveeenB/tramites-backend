# Tablas del Módulo de Trámites en la BD de Matrícula

| | |
|---|---|
| **Para** | Equipo del Módulo de Matrícula (administradores de su BD en Northflank) |
| **De** | Módulo de Trámites de Posgrado · PO: Johan Steven Bueno Rojas |
| **Tarea** | TP-209 · acuerdo TP-208 (`docs/BD_COMPARTIDA_MATRICULA.md`) |
| **Fecha** | 16/11/2026 |

## Qué es

Trámites deja de usar su base propia y pasa a la misma base de Matrícula, como se acordó en TP-208. Con estos scripts
se crean:

- un usuario de BD, `tramites_app`, con permisos **solo** sobre las tablas de Trámites;
- las **17 tablas de Trámites**, todas con el prefijo `tp_` (`tp_solicitud`, `tp_usuario`, …), vacías.

**No se cambia nada de Matrícula:** ninguna de sus tablas se crea, altera, lee ni borra, y ninguna llave foránea
apunta a ellas. Los datos de Matrícula que Trámites necesita los sigue pidiendo por su API REST (SC-03).

Los datos de Trámites los carga después el equipo de Trámites con `tramites_app`, el día del cambio. Ustedes no
tienen que cargar datos.

## Qué hacer, en orden

| # | Qué | Con qué usuario | Resultado esperado |
|---|---|---|---|
| 0 | Respaldo de la base (`mysqldump`) | administrador | Archivo de respaldo |
| 1 | `03_comprobacion.sql`, consulta 3: anotar cuántas tablas tiene la base | administrador | Por ejemplo 53 |
| 2 | `01_permisos_usuario_tramites.sql` (cambiar antes `<contraseña>`, y `defaultdb` si su base se llama distinto) | administrador (`avnadmin`) | Sin errores |
| 3 | `02_ddl_tablas_tramites.sql` | **`tramites_app`** (así se prueban los permisos). Si falla por permisos, avísennos antes de correrlo con el administrador | Termina mostrando `17` |
| 4 | `03_comprobacion.sql` completo | administrador | Ver abajo |
| 5 | Enviar al PO de Trámites el resultado del paso 4 y, **por privado**, la contraseña de `tramites_app` | — | — |

Resultado esperado del paso 4:

1. `SHOW GRANTS`: 18 líneas sobre tablas `tp_*` y `GRANT USAGE ON *.*`. Nada sobre la base completa.
2. Tablas de Trámites: `17`.
3. Tablas de Matrícula: el mismo número del paso 1.
4. Llaves foráneas que cruzan entre módulos: ninguna fila.

## Requisitos del servidor

- MySQL 8 o 9 (la base de Matrícula es 9.7.2), con el `sql_mode` ANSI o el vacío. Los scripts usan solo comillas simples y acento grave, así que funcionan con `ANSI_QUOTES`.
- Funciona con `sql_require_primary_key=ON`: las 17 tablas tienen llave primaria.
- Charset y collation: los de la base (`utf8mb4` / `utf8mb4_0900_ai_ci`).
- Conexiones: el backend de Trámites abre hasta 10 (`DB_POOL_SIZE`). Hay que confirmar que caben en `max_connections` junto con las de Matrícula.

## Reglas de convivencia (TP-208, secciones 7.2 y 9)

- El prefijo `tp_` queda reservado para Trámites: Matrícula no crea tablas con ese prefijo y sus scripts de `db/scripts/` no tocan tablas `tp_*`.
- Si Matrícula adopta Flyway, no debe usar `tp_flyway_schema_history` como historial.
- Respaldo antes de cualquier cambio de esquema en la base compartida. Si un cambio afecta al otro módulo, se avisa antes por escrito.
- Cuando Trámites agregue una tabla nueva, les enviaremos la línea `GRANT` correspondiente.

## Si hay que deshacerlo

`04_vuelta_atras.sql` borra solo las tablas `tp_*` y el usuario `tramites_app`. Viene comentado a propósito. Una vez
que Trámites tenga datos reales en la base, no se usa sin hablar antes con el PO de Trámites.

## Nota para el equipo de Trámites

Como las tablas ya van a existir cuando el backend se conecte, el backend arranca con
**`FLYWAY_BASELINE_VERSION=1`**. Flyway crea `tp_flyway_schema_history`, registra V1 como aplicada sin ejecutarla y
V2 no hace nada, porque en esta base no están las 17 tablas sin prefijo. Con `0` intentaría crear las tablas otra
vez y no arrancaría. Ver `docs/MIGRACIONES.md`.
