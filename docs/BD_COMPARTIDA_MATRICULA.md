# Acuerdo de base de datos compartida: Trámites de Posgrado ↔ Matrícula

| | |
|---|---|
| **Tarea** | TP-208 · SC02-T8 (épica SC-02) |
| **Estado** | Estrategia acordada entre ambos equipos (chat de WhatsApp, 15/11/2026, 10:00 a. m. – 12:00 m.) · **pendiente de aprobación de los PO** |
| **Acta** | Acta de reunión TP-208 (formato de actas del equipo), firmada por el PO de cada módulo |
| **Fecha** | 15/11/2026 |
| **Responsable** | Diego Bermúdez (Trámites) |
| **Implementa** | TP-209 (Kevin Arias, con apoyo de Diego) |

> **Nota del 16/11/2026:** la base de producción de Matrícula, donde quedaron las tablas `tp_*`, es un addon MySQL en **Northflank** (no Aiven). La BD de Trámites de la época de este análisis estaba en **Railway**. El análisis de esquemas de este documento sigue vigente. Datos verificados el 16/11/2026 con `tramites_app`: **MySQL 9.7.2**, base `d4013b5ab4aa` (no `defaultdb`), conexión con TLS y `sql_mode` global **vacío** (no ANSI). El código de Trámites funciona con los dos modos.

## 1. Contexto

Este documento fija qué tablas son de quién, qué nombres chocan y cómo conviven los dos
módulos en **la misma base MySQL**, según las condiciones del ing. Matías (Posgrados):

| # | Condición del ing. Matías | Cómo la cumple la estrategia acordada |
|---|---|---|
| 1 | Cada equipo entrega su código **dockerizado** y el equipo de Posgrados lo despliega | Trámites se configura solo por variables de entorno (`DB_URL`, `FLYWAY_BASELINE_VERSION`, …) y crea sus tablas al arrancar con Flyway. Se entrega con el script de permisos de la sección 8 |
| 2 | Todo queda en **un solo servidor** | Ambos módulos usan el mismo servidor MySQL |
| 3 | La BD de Trámites **convive** con la de Matrícula | Ambos módulos quedan en la misma base. Las tablas de Trámites llevan el prefijo `tp_`, sin choques de nombre y sin cambios en el esquema de Matrícula |

**Fuentes usadas (15/11/2026):**

- Trámites: `src/main/resources/db/migration/V1__esquema_base.sql` (rama `develop`, desde la fusión de Flyway en el PR #65), 17 tablas, y las entidades JPA de `com.ufps.tramites.model`.
- Matrícula: estructura de su BD vigente en Aiven (`defaultdb`), extraída con `mysqldump --no-data` (solo estructura, sin datos), con acceso de solo lectura dado por su equipo. 53 tablas, sin vistas, triggers ni rutinas.

## 2. Datos de los servidores

| | Matrícula (vigente) | Trámites (vigente) |
|---|---|---|
| Motor | MySQL **8.4.11** | MySQL 8 (Railway) |
| Charset / collation | `utf8mb4` / `utf8mb4_0900_ai_ci` | Hereda el de la base |
| Motor de tablas | InnoDB (53/53) | InnoDB (17/17) |
| Nombre de la base | `defaultdb` (nombre por defecto de Aiven) | — |
| Cambios de esquema | Scripts manuales fechados en `db/scripts/`, `ddl-auto=none` | Flyway (`V<n>__*.sql`), `ddl-auto=validate` |
| `sql_mode` | `REAL_AS_FLOAT, PIPES_AS_CONCAT, ANSI_QUOTES, IGNORE_SPACE, ONLY_FULL_GROUP_BY, ANSI, STRICT_ALL_TABLES, NO_ZERO_IN_DATE, NO_ZERO_DATE, ERROR_FOR_DIVISION_BY_ZERO, NO_ENGINE_SUBSTITUTION` (modo **ANSI**) | Por defecto de MySQL 8 |

El nombre definitivo de la base en el servidor institucional lo define el ing. Matías.
Trámites lo recibe solo por la variable `DB_URL`.

### 2.1 Qué implica el modo ANSI del servidor de Matrícula

| Opción | Efecto | Impacto en Trámites |
|---|---|---|
| `ANSI_QUOTES` | Las comillas dobles `"..."` son nombres de tabla o columna, no textos | Ninguno: nuestras migraciones, `data.sql` y consultas JPQL usan comillas simples (verificado) |
| `PIPES_AS_CONCAT` | `\|\|` concatena textos en vez de ser un "o" lógico | Ninguno: no usamos `\|\|` en SQL |
| `REAL_AS_FLOAT` | `REAL` equivale a `FLOAT` | Ninguno: V1 declara `float(53)` explícitamente |
| `STRICT_ALL_TABLES` | Un dato que no cabe en la columna produce error, no se recorta | Confirma el riesgo de 4.4 (`codigo` de 50 frente a 100) |

**Regla:** el servidor compartido debe conservar el `sql_mode` actual de Matrícula, porque
sus scripts pueden depender de él, y Trámites debe funcionar con ese modo. Las pruebas
locales de TP-209 se hacen con este mismo `sql_mode`, no con el de MySQL por defecto.

## 3. Tablas de cada módulo

**Trámites (17):** `admins`, `dependencias`, `documento_solicitud`, `estados_estudiantes`,
`estudiante`, `fechas_grado`, `historial_estado_solicitud`, `notificaciones`, `pagos`,
`paz_y_salvo`, `programa_academico`, `roles`, `solicitud`, `solicitud_certificado`,
`tipo_certificado`, `tipos_solicitud`, `usuario`.

**Matrícula (53):** `administrativo`, `admitido`, `aspirante`, `calificacioncriterio`,
`cambiodocumento`, `capacidadexepcional`, `cargo`, `clave`, `cohorte`, `criteriocohorte`,
`criterioevaluacion`, `departamento`, `discapacidad`, `documento`, `documentopersona`,
`documentosrequisitoconsejo`, `documentosrequisitoconsejocohorte`,
`documentosrequisitoprograma`, `documentosrequisitoprogramacohorte`, `entrevista`, `estado`,
`estadocivil`, `estadodocumento`, `estudiantes`, `facultad`, `genero`, `grupoetnico`,
`modalidad`, `municipio`, `otrosvalores`, `pago`, `pagoconcepto`, `pagoreciboinscripcion`,
`pagorecibomatricula`, `pais`, `persona`, `plazo`, `poblacionindigena`, `programas`,
`prueba`, `roles`, `sede`, `semestre`, `tipodocumentopersona`, `tipoentrevista`,
`tipoplazo`, `tipoprueba`, `tiporegistro`, `tipovinculacion`, `ubicacion`,
`ultimocodigoprograma`, `usuarios`, `valoresglobales`.

## 4. Choques

### 4.1 Choque exacto de nombre de tabla

Solo hay uno: **`roles`**. Además de tener el mismo nombre, los ids significan cosas
distintas, así que compartir la tabla sería un **riesgo de seguridad** (un usuario podría
quedar con permisos de otro rol):

| id | Trámites | Matrícula |
|---|---|---|
| 1 | ESTUDIANTE | ASPIRANTE |
| 2 | DIRECTOR | SUPER_ADMINISTRADOR |
| 3 | ADMIN | DIRECTOR_DE_PROGRAMA |
| 4 | POSGRADOS | POSGRADOS |
| 5 | DEPENDENCIA | — (más `Servicio Tramites`, creado por script) |

Tipos: en Trámites `roles.id` es `BIGINT` sin autoincremento; en Matrícula es
`INT AUTO_INCREMENT` con `nombre VARCHAR(50) NOT NULL`.

> La guía del sprint suponía que también chocaban `admins` y `estados_estudiantes`. La BD
> vigente de Matrícula **no tiene esas tablas**, así que el único choque exacto es `roles`.

### 4.2 Nombres de restricciones

En MySQL, el nombre de una llave foránea debe ser único en toda la base. Se compararon los
83 nombres de restricción de Matrícula con los 27 de Trámites: **ninguno coincide**.

### 4.3 Casi-choques (mismo concepto, otro nombre)

No chocan técnicamente, pero se prestan a confusión. Con la estrategia elegida **no se
comparten**: cada módulo usa las suyas y los datos de Matrícula llegan a Trámites por la
API REST de SC-03.

| Concepto | Trámites | Matrícula | Relación entre ambos |
|---|---|---|---|
| Estudiante | `estudiante` (id BIGINT) | `estudiantes` (id INT) | Trámites guarda una copia que sincroniza por API usando la `cedula` |
| Programa | `programa_academico` | `programas` | Se relacionan por `codigo` (SC-03) |
| Usuario | `usuario` | `usuarios` + `persona` + `clave` | Independientes: cada módulo tiene su propio login |
| Rol | `roles` | `roles` | Independientes (ver 4.1) |
| Pago | `pagos` (Wompi) | `pago`, `pagoconcepto`, `pagorecibo*` | Conceptos distintos, sin relación |

### 4.4 Diferencia de longitud a corregir

`programas.codigo` en Matrícula es `VARCHAR(100)`, y nuestra copia
`programa_academico.codigo` es `VARCHAR(50)`. Si llegara un código de más de 50 caracteres,
el guardado fallaría (`Data too long`). Trámites amplía su columna a 100 (ver 7.1).

## 5. Estrategia acordada

Se evaluaron tres opciones:

| Opción | Descripción | Resultado |
|---|---|---|
| **Misma base, prefijo `tp_` en las tablas de Trámites** | Las 17 tablas de Trámites se renombran (`tp_solicitud`, `tp_usuario`, …) con una migración Flyway y se ajusta `@Table` en las entidades. Los datos de estudiantes y programas se siguen consultando a Matrícula por la API REST de SC-03 | **Elegida** |
| Unificación de tablas maestras | Trámites usaría `estudiantes`, `programas`, `usuarios` y `roles` de Matrícula. Implica reescribir entidades, repositorios, login y llaves foráneas, y acordar cada tabla con Matrícula | Descartada para esta entrega; queda como evolución futura |
| Bases separadas en el mismo servidor | Una base por módulo | No cumple "la misma base" pedida por el ing. Matías |

**Por qué el prefijo:**

- Elimina el choque de `roles` sin tocar nada de Matrícula.
- Se ve a simple vista de quién es cada tabla (`tp_*` es de Trámites; todo lo demás, de Matrícula).
- No cambia la integración ya implementada (API REST de SC-03).
- Cabe en el tiempo de la entrega: una migración, 17 anotaciones `@Table` y pruebas.

## 6. Matriz de propiedad

| Tablas | Dueño | Quién escribe | Quién lee | Acción |
|---|---|---|---|---|
| Las 53 de Matrícula (sección 3) | Matrícula | Matrícula | Matrícula | Ninguna. Trámites **no** las lee ni las escribe |
| `tp_admins`, `tp_dependencias`, `tp_documento_solicitud`, `tp_estados_estudiantes`, `tp_estudiante`, `tp_fechas_grado`, `tp_historial_estado_solicitud`, `tp_notificaciones`, `tp_pagos`, `tp_paz_y_salvo`, `tp_programa_academico`, `tp_roles`, `tp_solicitud`, `tp_solicitud_certificado`, `tp_tipo_certificado`, `tp_tipos_solicitud`, `tp_usuario` | Trámites | Trámites | Trámites | Renombrar (hoy sin prefijo) |
| `tp_flyway_schema_history` | Trámites (Flyway) | Flyway de Trámites | Flyway de Trámites | Nueva |

Se verificó que ninguna tabla de Matrícula empieza por `tp_` y que ningún nombre
`tp_*` choca.

## 7. Cambios por módulo

### 7.1 Trámites (TP-209)

1. **Prefijo en las tablas.** `@Table(name = "tp_...")` en las 17 entidades. `Solicitud` y `DocumentoSolicitud` hoy no tienen `@Table` (toman el nombre de la clase), así que hay que agregárselo.
2. **Historial de Flyway propio:** `spring.flyway.table=tp_flyway_schema_history`. Así no choca si Matrícula adopta Flyway más adelante.
3. **Migraciones.** Hay que cubrir tres escenarios:

   | Escenario | Qué debe pasar |
   |---|---|
   | BD vacía | Se crean las 17 tablas `tp_*` |
   | BD compartida (ya tiene las tablas de Matrícula, ninguna nuestra) | Se crean las 17 tablas `tp_*` **sin tocar las de Matrícula** |
   | BD actual de Trámites en Railway (tablas sin prefijo, con datos) | Se renombran conservando los datos |

   Propuesta:
   - **V1** crea directamente las tablas con prefijo `tp_`. Se puede editar porque ningún entorno ha ejecutado V1 todavía; **confirmarlo antes de cambiarlo** (que ningún despliegue haya corrido Flyway desde `develop`). Si alguno ya lo ejecutó, se deja V1 y todo va en migraciones nuevas.
   - **V2** renombra de forma idempotente: `RENAME TABLE solicitud TO tp_solicitud` solo si existe `solicitud` y no existe `tp_solicitud`, con el mismo patrón de `.docs/sql/migracion_sc03_snapshot_matricula.sql`. En una BD vacía o compartida no hace nada.
   - **Baseline.** Flyway considera "no vacía" una base que ya tiene las tablas de Matrícula: con `baseline-on-migrate` y `baseline-version=1` **se saltaría V1** y no crearía nuestras tablas. Por eso la versión de baseline se vuelve configurable (`spring.flyway.baseline-version=${FLYWAY_BASELINE_VERSION:1}`):
     - BD actual de Trámites (Railway): `1`, para saltar V1 y aplicar solo el renombrado.
     - BD compartida institucional: `0`, para que V1 cree las tablas. Va en el perfil institucional del `docker-compose` y en `DEPLOY.md` (TP-211).
     - Si alguien olvida el `0` en la BD compartida, la aplicación **falla al arrancar** (`validate`), sin dañar nada.
4. **Ampliar** `tp_programa_academico.codigo` a `VARCHAR(100)` (ver 4.4).
5. **Datos semilla para el perfil H2:** `src/main/resources/data.sql` usa los nombres viejos (`roles`, `usuario`, `admins`, `programa_academico`, …). Hay que cambiarlo a `tp_*`, o fallarán las pruebas del CI, que corren con H2.
6. **Catálogos en una BD nueva.** Hoy los catálogos (`roles`, `estados_estudiantes`, `tipos_solicitud`, `tipo_certificado`, `dependencias`) solo se cargan en el perfil H2. En la BD institucional quedarían vacíos. Se propone una migración de semillas idempotente (`INSERT IGNORE`), y el PO confirma qué datos son obligatorios.
7. **Sin cambios** en las consultas: los repositorios usan JPQL (nombres de entidad) y no hay consultas nativas ni `@JoinTable`.
8. Los scripts de `.docs/sql/` quedan como **históricos**: se aplicaron con los nombres viejos y no se modifican.
9. Actualizar `docs/MIGRACIONES.md`.

### 7.2 Matrícula

**Ningún cambio en su esquema.** Solo se le pide:

1. Reservar el prefijo `tp_` para Trámites: no crear tablas con ese prefijo y que sus scripts de `db/scripts/` no toquen tablas `tp_*`.
2. Si adopta Flyway, no usar `tp_flyway_schema_history` como su tabla de historial.
3. Crear en el entorno final el usuario técnico con rol `Servicio Tramites` (script `2026-11-SC03-rol-servicio-tramites.sql`), necesario para la API de SC-03.

## 8. Usuarios y permisos en la BD compartida

| Usuario | Permisos | Lo crea |
|---|---|---|
| Matrícula | Los que defina su equipo | Su equipo / ing. Matías |
| Trámites | Solo sobre sus tablas `tp_*`: `SELECT, INSERT, UPDATE, DELETE`, y para Flyway `CREATE, ALTER, INDEX, REFERENCES`. **Ninguno** sobre las tablas de Matrícula | ing. Matías, con el script de TP-209 |

- MySQL no permite comodines en nombres de tabla (`tp_%`), así que los permisos se otorgan **tabla por tabla**. Incluir `CREATE` en un permiso por tabla deja otorgarlo antes de que la tabla exista.
- **No** se da `ALTER` ni `DROP` a nivel de toda la base: permitiría modificar tablas de Matrícula.
- Cada tabla nueva en una migración futura necesita su línea en el script de permisos.
- El script va en `docs/sql/permisos_usuario_tramites.sql` (TP-209) y se entrega al ing. Matías.

## 9. Convenciones comunes

- MySQL 8.4, InnoDB, `utf8mb4`, collation `utf8mb4_0900_ai_ci`, `sql_mode` ANSI (sección 2.1).
- En SQL escrito a mano, textos siempre con comillas simples y nombres sin comillas o con acento grave (`` ` ``), nunca con comillas dobles.
- Trámites: nombres en `snake_case` con prefijo `tp_`. Matrícula conserva los suyos.
- Ningún módulo crea llaves foráneas hacia tablas del otro. La integración es solo por API REST.
- Cambios futuros de esquema: Trámites con Flyway (migraciones nuevas, nunca editar una ya aplicada); Matrícula con sus scripts fechados. Si un cambio afecta al otro módulo, se avisa antes por escrito.
- Respaldo (`mysqldump`) antes de cualquier migración en el servidor compartido.

## 10. Pendientes

| Pendiente | Responsable |
|---|---|
| Confirmar que el servidor institucional usará el mismo `sql_mode` ANSI que el actual de Matrícula (sección 2.1) | ing. Matías / Matrícula |
| Nombre de la base en el servidor institucional | ing. Matías |
| Quién crea los usuarios de BD en el servidor institucional | ing. Matías |
| Datos de catálogo obligatorios para una BD nueva (7.1 punto 6) | PO |
| Confirmar que ningún entorno ha ejecutado V1 antes de editarlo | Kevin / PO |

## 11. Aprobaciones

La aprobación queda registrada con la firma del acta de reunión de TP-208.

| Nombre | Rol | Firma del acta | Fecha |
|---|---|---|---|
| Johan Steven Bueno Rojas | Product Owner, Módulo de Trámites | Pendiente | |
| Adolfo Alejandro Arenas Ramos | Product Owner, Módulo de Matrícula | Pendiente | |

Elaborado por: Diego Alexander Bermúdez Flórez (Trámites, TP-208).
