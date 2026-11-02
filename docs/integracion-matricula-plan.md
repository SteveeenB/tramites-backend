# Integración Trámites ↔ Matrícula · Análisis y plan (TP-144)

> Épica TP-138 (SC-03) · Tarea TP-144 · Documento de trabajo del equipo de desarrollo · 2 nov 2026
> Insumos: informe TP-143 (contrato v0.1, RF16, RNF06, brechas B1–B6), código de los cuatro repos (solo lectura) y Jira.
> **Estado:** propuesta. Lo que depende del equipo de Matrícula o del ing. Matías queda como opciones, no como decisiones.

## 0. Hallazgos que cambian el punto de partida

1. **Nuestro repo.**
   - Es `SteveeenB/tramites-backend`. El análisis se hizo sobre `origin/main`, commit `8b02669`; en ese momento `develop` estaba en el mismo commit.
2. **TP-146 figura como «Finalizada», pero no está en `main`.**
   - El adaptador vive en la rama `origin/HU19-ClienteMatricula`, commit `1d88d22`, sin mergear: `integracion/matricula/{MatriculaClient, MatriculaHttpClient, MatriculaMockClient, ConsultaMatricula, SituacionAcademica, MatriculaRespuesta}` más pruebas.
   - Ya implementa el contrato v0.1 (TP-143) y los umbrales de RNF06.
   - Este plan **parte de ese código** en lugar de rediseñarlo.
3. **El repo de Matrícula que se nos indicó está desactualizado.**
   - `a-cubica-r/LEG-0001-PROJECT_POSGRADOS_UFPS-BND`, rama `remote-main`, tiene su último commit el 12 jun 2026.
   - El repo vigente es `a-cubica-r/ACA-0001-POSGRADOS_PROJECT-BND`, con las ramas `dev` y `render` del 1 nov 2026. TP-143 ya lo había usado.
   - Este documento cita **`ACA-0001`, rama `origin/dev`**, salvo que se indique otra cosa. Su rama `SC-03` es una migración a MapStruct y no tiene relación con nuestra épica.
4. **Matrícula también se despliega en Render free.**
   - `render.yaml:7` (`plan: free`) y `:9` (`healthCheckPath: /posgrados-project/actuator/health`), en la rama `render`.
   - Por tanto, una llamada puede sufrir dos *cold starts* encadenados (el nuestro y el suyo).
5. **`SolicitudEstadoCambiadoEvent` existe, pero nadie lo publica.**
   - Clase: `event/SolicitudEstadoCambiadoEvent.java:7`. Listener: `service/NotificacionEmailService.java:29-30`.
   - No hay ningún `publishEvent` en el código. El contexto lo da como mecanismo vigente, y no lo es.

---

## 1. Inventario

### 1.1 Nuestro repo (Paso 0.1)

| Comprobación | Resultado |
|---|---|
| Rama | `main`, alineada con `origin/main` y con `origin/develop` |
| `git status` | limpio |
| `git log --oneline -15` (últimos) | `8b02669` ci develop · `46ed4bd` docs readme · `0006f54` ci h2 · `a53177d` docs a docs/ · `d298e29` show-sql · `db87ed4`/`83c7320`/`02a3dc7` correos de seed · `77f4ed6` PR #45 · `9baa5dd` · `477cb0f` PR #44 · `5aa6d90` · `eec72fa` PR #43 · `7e0c08e` · `59ccf5e` PR #42 |
| Stack | Java 17 (`pom.xml:30`), Spring Boot 4.0.5 (`pom.xml:8`), MySQL (`application.properties:5-8`), `ddl-auto=update` marcado como temporal (`:28-32`), H2 en el perfil `h2`, JWT propio con JJWT 0.12.6 |

**Contradicciones con el contexto:**
- **Autenticación.** Además del JWT propio hay login con Google (`AuthController.java:93-151`) y un login demo (`:160-188`).
- **Documentación desactualizada.** Varios documentos todavía dicen PostgreSQL/Supabase: `docs/rules/instrucciones_proyecto_v2.md:69,76` y el comentario de `model/Estudiante.java:81-85`. Supabase hoy solo se usa como Storage.
- **Migraciones.** No hay Flyway; las migraciones son SQL manuales en `.docs/sql/`.

### 1.2 Datos de Matrícula que hoy tiene Trámites (Paso 0.2)

Rutas relativas a `src/main/java/com/ufps/tramites/`.

| Concepto | Dónde | Campos relevantes | De dónde salen hoy |
|---|---|---|---|
| Usuario (identidad) | `model/Usuario.java` | `cedula` (único, `:21-22`), `codigo` (único, `:24-25`), nombres (`:27-34`), `email` y `correo` (`:36-39`), `rol` (`:55-57`), `programaAcademico` (`:59-61`) | Seed (`data.sql:63-87,111-115,141-145`). Alta automática por Google (`AuthController.java:127-137`), que **no guarda cédula** |
| Estudiante (perfil 1:1) | `model/Estudiante.java` | nombres, cédula, código, email y teléfono (`:23-30`, sin `unique`), `fechaIngreso` (`:36-37`), `esPosgrado` (`:42-43`), **`creditosAprobados` (`:53-54`)**, `estadoEstudiante` (`:60-62`), `programa` (`:69-70`), `cohorteId` y `pensumId` como *stubs* sin entidad (`:74-78`), `usuario` (`:86-88`) | Solo seed (`data.sql:188-205`) o SQL manual. El código de producción solo escribe `estadoEstudiante` (`SolicitudService.java:510-516`, `:927-937`) |
| ProgramaAcademico | `model/ProgramaAcademico.java` | `nombre` (único, `:19`), `tipo` (`:20`), `totalCreditos` (`:21`). **No tiene `codigo`** | Seed con 16 programas (`data.sql:42-61`); por ejemplo, Gerencia de Empresas tiene 56 créditos (`:47`). No hay endpoint ni servicio |
| EstadoEstudiante | catálogo | ACTIVO, PAGO_GRADO_PENDIENTE, GRADUADO | Seed (`data.sql:28-31`) |
| Cohorte / Pensum / Materia / Nota | — | — | No existen. Solo están los *stubs* Long de `Estudiante.java:72-78` |
| Convocatoria | — | — | Se eliminó en TP-193 (`service/TramiteService.java:16-19,65`). El javadoc de `SolicitudService.java:146-147` y `README.md:557` siguen mencionándola |
| DEMO_USERS (frontend) | `tramites-frontend/src/config/menuConfig.js:47` | mapa de clave demo a cédula | Lo usa `context/AuthContext.js:85` (TP-147) |

**Cómo se identifica al estudiante.**
- La clave de negocio es la **cédula**. Se duplica en `solicitud.cedula` (`Solicitud.java:28`), `paz_y_salvo.cedula_estudiante` (`PazYSalvo.java:23`), `pago.cedula_estudiante` (`Pago.java:20-21`) y otras tablas.
- El principal del JWT es la cédula (`security/JwtAuthFilter.java:64-66`). El claim `sub` es `usuario.id` (`JwtService.java:48`), y los claims incluyen `cedula`, `codigo` y `estudianteId`.

> **Riesgo previo (bug):** un usuario creado por Google queda sin cédula (`AuthController.java:131-137`), así que su principal es `null`. Como toda la integración usa la cédula como llave, ese estudiante nunca podrá consultarse en Matrícula.

### 1.3 Cómo calculamos hoy los créditos aprobados

- **No se calcula nada.** El dato es un único campo persistido, `Estudiante.creditosAprobados` (`Estudiante.java:53-54`).
  - Su comentario (`:48-52`) lo describe como *«snapshot del cálculo oficial (DIVISIT)… sumando `materias.creditos` desde `notas_divisit`… se sincroniza cuando el módulo se integre (Fase 3)»*.
  - Ningún código de producción lo escribe: `setCreditosAprobados` solo aparece en pruebas.
- **Requeridos:** `ProgramaAcademico.totalCreditos`, leído siempre desde `Usuario.getProgramaAcademico()`. El campo `Estudiante.programa` no participa en ninguna regla.
- **El 100 %:** no hay ninguna constante «100». Es la comparación `aprobados >= requeridos`. Si `aprobados` es `null`, se toma 0.

Reglas que dependen de los créditos:

| # | Regla | Dónde | Comportamiento y defectos |
|---|---|---|---|
| R1 | **Terminación de Materias** (la única regla que bloquea) | `SolicitudService.java:150-162`; el 422 lo arma `SolicitudController.java:43-44` (`POST /api/solicitudes/terminacion-materias`) | Sin programa usa `Integer.MAX_VALUE` y bloquea. El mensaje es «tiene X/Y créditos aprobados», no «Faltan N créditos» como pide RF16. Pruebas en `SolicitudServiceTerminacionTest.java:117-146` |
| R2 | Solicitud de Grado | `SolicitudService.java:215-223`; 422 en `SolicitudController.java:66-67` | No vuelve a mirar los créditos: exige una TERMINACION_MATERIAS en estado APROBADA. El 100 % se cumple **de forma indirecta** |
| R3 | Etapa 1 de la vista de proceso de grado | `TramiteService.java:41-48` (`GET /api/tramites/proceso-grado`) | Fallback `MAX_VALUE`. `estadoAcademico` está fijo en `"Regular"` (`:64`) |
| R4 | Nodo `creditos{aprobados,requeridos}` | `TramiteService.java:87-96` | Aquí el fallback de requeridos es **0**, inconsistente con R3 |
| R5 | Etapa del estudiante en la vista del director | `PazYSalvoService.java:244-247,297` (`GET /api/paz-y-salvo/estado-estudiantes`) | Con fallback 0, un estudiante sin programa aparece como «Requisitos completos» |
| R6 | Bandeja de grado | `SolicitudService.java:428-430` | Solo muestra los datos |
| — | Certificados, liquidación y pago | `CertificadoService.java:90-96`; `WompiService.java:274-279` | No dependen de créditos |

En R1, R3 y R5, si `totalCreditos` es `null` (con programa presente), el auto-unboxing lanza un NPE. La misma lógica está **copiada en cuatro sitios**.

### 1.4 Backend de Matrícula (Paso 0.3): hallazgos previos confirmados o corregidos

Rutas relativas a `ACA-0001` en `origin/dev`. Abreviaturas: `B=backend/api-base/src/main/java/ufps/edu/co`, `C=backend/api-core/...`, `R=backend/api-rest/...`.

| Hallazgo previo | Veredicto | Evidencia |
|---|---|---|
| Java 21, Spring Boot 4.1, Maven multimódulo, MySQL, JWT propio, `context-path=${APP_PREFIX}` | **Confirmado, con matices.** Son 11 módulos (además `api-auth`, `api-wompi`, `s3-storage`, `mail-module`, `check-builder`, `api-test`). `APP_PREFIX` no tiene valor por defecto; el convencional es `/posgrados-project` (`render.yaml:14-15`) | `platform/application/src/main/resources/application.properties:8` |
| Flujo de pago: `PagoProcessor` publica el evento y `EstudianteRegistroListener` crea el estudiante | **Confirmado, con otros nombres.** En `dev` los *Processor* pasaron a llamarse `*CoreService`. Publica `C/services/core/crud/PagoCoreService.java:865`, escucha `C/services/core/listeners/EstudianteRegistroListener.java:21` y crea `EstudiantesCoreService.java:173`, que deja `pensumId=null` | idem |
| Campos de `EstudiantesEntity` | **Hay que corregir los nombres.** Son `nombre`/`nombre2`, `apellido`/`apellido2`, `fechalngreso` (con «l»), `moodleld`, `esposgrado`, `migrado` (String "N"), además de `fechanacimiento`. `cedula` y `codigo` son `varchar(200)` sin `unique` (`:47-51`); `cohorteId`, `estadoEstudianteId` y `pensumId` son enteros sueltos (`:80-87`) | `B/persistence/entities/EstudiantesEntity.java` |
| No existe entidad `Pensum` | **Confirmado.** `pensum_id` solo aparece como entero (Entity, DTO, Input, Output). No hay ningún `.sql` que cree esa tabla | — |
| `EstudiantesCase` sin búsqueda por cédula | **Confirmado, y además** expone `GET /estudiantes` (listar todos) y `DELETE /{id}`. El repositorio tiene `existsByCedula` (`:24`), pero no `findByCedula` ni `findByCodigo` | `R/controllers/cases/estudiante/EstudiantesCase.java:27-117`; `B/persistence/repositories/EstudiantesRepository.java:16-32` |
| Créditos solo en `Programa` | **Confirmado** (`ProgramaEntity.creditos`, `:48-49`). Las «calificaciones» (`calificacioncriterio`) son puntajes de admisión, y `semestre` es el periodo de la cohorte; ninguno es académico por estudiante | — |
| Cuatro roles y un catch-all para SUPER_ADMINISTRADOR, sin autenticación máquina a máquina | **Confirmado.** Los roles ahora son el enum `B/enums/Rol.java:14-18` más la tabla `roles`. Las rutas `/estudiantes/**` no están en ninguna lista, así que caen en el catch-all `SUPER_ADMIN_PATHS = "/**"` (`SecurityConfig.java:78-79,129`). No hay httpBasic, de modo que una petición sin autenticar recibe 403. No hay API key | `platform/security/.../config/SecurityConfig.java` |
| Entidad `EstadoEstudiante` | **No existe como entidad.** Hay un enum con un único valor, `ACTIVO` (`B/enums/EstadoEstudiante.java:10`), y la tabla genérica `estado` (con `entidad` = 'estudiante') | — |

**URL final.**
- Patrón: `{host}{APP_PREFIX}{prefijo}{ruta}`.
- Los prefijos se asignan con `addPathPrefix` por paquete (`R/config/path/ApiRestConfiguration.java:33-37`):
  - `controllers.cases` → `/api/application/case`
  - `controllers.cases.command` → `/api/application/command`
  - `controllers.rest` → `/api/dev/endpoint`
  - `/auth/**` no lleva prefijo.
- Ejemplo: `https://{host}/posgrados-project/api/application/case/estudiantes/{id}`.
- **Supuesto:** la URL vieja de Railway que aparece en el historial de `LEG-0001` (`rep0010-projectposgradosufps-production.up.railway.app`) ya no es la de producción, porque `render.yaml` apunta a Render.

**Swagger.** Usan `springdoc-openapi-starter-webmvc-ui` 3.0.2. Se activa con `ENABLE_API_DOCS` y `ENABLE_SWAGGER_UI` (`application.properties:68-71`). En Render: `/posgrados-project/swagger-ui.html` y `/posgrados-project/api-docs`. Es público.

**Esquema.**
- `ddl-auto=none` (`application.properties:49`). No usan Flyway ni Liquibase.
- **Ahora sí versionan scripts** en `db/scripts/AAAA-MM-<id>-<tema>.sql`, por ejemplo `db/scripts/2026-10-D00-ultimocodigoprograma.sql`.
  - Cada script lleva una cabecera con PROPÓSITO, PRERREQUISITO (respaldo), VERIFICAR ANTES y CÓMO REVERTIR.
  - Son idempotentes y se ejecutan **antes** de desplegar el código.
- Esquema: `DB_SCHEMA=defaultdb` (`render.yaml:54-55`). **Supuesto:** el nombre `defaultdb` sugiere una MySQL gestionada distinta del Railway nuestro, así que **hoy no compartimos servidor de BD**.

**Otros datos.**
- **Actuator:** está incluido y en la rama `render` ya hicieron público `/actuator/health` (diff de `SecurityConfig.java` respecto a `dev`).
- **Errores:**
  - `GlobalExceptionHandler` responde `{code, message, param}` y elige el estado HTTP por el sufijo del `code`: `_NOT_FOUND` → 404, `_FORBIDDEN` → 403, `_CONFLICT` → 409, cualquier otro → 400.
  - **No hay ningún caso que devuelva 422.**
  - `EstudiantesCase` responde 404 y 500 sin cuerpo (brecha B6).
- **Clientes salientes:** solo `java.net.http.HttpClient` hacia Wompi. No tienen webhooks salientes ni integración con Moodle.

### 1.5 Frontend de Matrícula (Paso 0.4)

- No hay rol ni vista de estudiante. Tras legalizarse, el usuario sigue siendo «aspirante».
- El único elemento posterior a la matrícula es el banner «¡Proceso completado!» (`src/vistas/aspirante/AspiranteInicio.tsx:277-286`).
- Ninguna vista lista estudiantes ni consume `/estudiantes`. La base URL es `VITE_API_URL` (`src/services/authService.ts:15`).
- **Sitio natural para enlazar a Trámites:** un botón en ese banner, con una variable nueva `VITE_TRAMITES_URL`.
  - Alternativa: una entrada condicional en `src/vistas/aspirante/components/Sidebar.tsx:11-17`.
  - No hay SSO: el estudiante tendría que iniciar sesión otra vez en Trámites.

---

## 2. Comparativa de integración

| Criterio | **A. Trámites consume a Matrícula** | **B. Matrícula consume a Trámites** | **C. Híbrido por eventos (webhooks)** |
|---|---|---|---|
| Dueño del dato y duplicación | Matrícula es dueña de la identidad, el programa, la cohorte y la legalización. Trámites guarda solo un *snapshot* en `estudiante` para la trazabilidad de sus actas | Trámites es dueño del estado del grado. Matrícula duplicaría «GRADUADO» en `estudiantes.estado_estudiante_id` | Igual que A, pero la copia en Trámites se vuelve permanente y se alimenta por eventos: más duplicación |
| Acoplamiento y disponibilidad | Síncrono: si Matrícula duerme (Render free, ~50 s), el estudiante espera o queda bloqueado. Se mitiga con *warm-up* y *snapshot* | No afecta al flujo de Trámites. Sí afecta a Matrícula si nos consulta en línea | Desacoplado en lectura, pero cada webhook choca con un *cold start* del receptor y **ninguno de los dos tiene cola ni reintentos**: los eventos se pierden |
| Consistencia | Fuerte en el momento de la consulta | Eventual (solo cuando ellos consultan) | Eventual, con huecos si se pierde un webhook. Habría que añadir una reconciliación periódica |
| Autenticación servicio a servicio | Cuenta técnica con rol nuevo `SERVICIO_TRAMITES` en **su** `SecurityConfig` | Nosotros necesitaríamos un filtro de API key o una cuenta técnica en **nuestra** `SecurityConfig` (`config/SecurityConfig.java:28-63`) | Las dos cosas, más firma HMAC del webhook. Su webhook de Wompi hoy no valida firma (`PagoWompiWebhookCase`), así que no hay un patrón que reutilizar |
| Cambios en SU repo | Endpoint C1 por cédula, rol técnico, rutas en `SecurityConfig`, cuerpos de error | Cliente HTTP nuevo (no tienen ninguno salvo el de Wompi) y una vista o un proceso que lo use | Publicador HTTP de eventos, reintentos y cola. Es lo que más esfuerzo les pide |
| Cambios en el NUESTRO | Mergear y ajustar `HU19-ClienteMatricula` y conectar las reglas | Endpoint `/api/integracion/v1/...` más filtro de servicio | Receptor de webhook, idempotencia y el publicador del grado (el evento actual nunca se publica) |
| Riesgos | R7 (dependencia externa), *cold start*, cambios en su contrato | Bajo valor: hoy no tienen vista de estudiante donde mostrarlo | Alto esfuerzo y fragilidad sin infraestructura de mensajería; difícil de probar en dos Render free |

**Lectura.**
- A cubre RF16 y HU-19 tal como están redactados.
- B tiene sentido solo como fase 2, si Matrícula decide marcar a los graduados.
- C no compensa sin cola ni reintentos.

---

## 3. Fuente de los créditos aprobados

Hoy ningún sistema tiene el numerador (créditos aprobados). Matrícula tiene el denominador (`programas.creditos`), y la fuente institucional real, según `Estudiante.java:48-52`, es el registro académico (DIVISIT / `notas_divisit`).

| Criterio | **Opción 1 · BD compartida** | **Opción 2 · Cambio en su modelo + endpoint suyo** | **Opción 3 · Copia en nuestra BD** |
|---|---|---|---|
| Dueño del esquema | Ellos, pero nosotros pasamos a depender de sus columnas | Ellos | Nosotros |
| Quién carga los datos | Si es de escritura, nosotros sobre su BD (inaceptable). Si es de solo lectura, alguien de su lado | Su director o Posgrados desde su UI, que no existe y habría que construirla, o un CSV de su lado | El director o Posgrados desde nuestra UI, una importación CSV, o seed (solo dev) |
| Dueño del dato y duplicación | Un solo dato, sin duplicar | Un solo dato en Matrícula, que tampoco lo produce: también es una carga manual | Copia en Trámites, con auditoría de quién la cargó y de dónde salió |
| Acoplamiento | **Máximo**: si renombran una columna (ya tienen nombres como `fechalngreso` y la tabla la genera Telosys), nuestro backend falla en ejecución sin aviso | Contrato REST versionado | Ninguno para este dato |
| Disponibilidad | Depende de una conexión a otra BD, que hoy está en otro proveedor (`defaultdb`) | Igual que A (*cold start*) | Local, siempre disponible |
| Consistencia | Fuerte | Fuerte respecto a Matrícula | Depende de la disciplina de carga. Se mitiga con fecha de corte visible |
| Autenticación | Credenciales de BD compartidas entre equipos. Va contra RNF06 («el consumo es de backend a backend por el contrato») | La misma cuenta técnica de A | La de nuestro propio sistema (roles DIRECTOR y POSGRADOS) |
| Cambios en SU repo | Usuario MySQL y vista SQL | Tabla, script, Entity, Repository, Mapper, Input y Output, CoreService, Case, ruta y UI de carga | Ninguno |
| Cambios en el NUESTRO | Segundo `DataSource`, entidades de solo lectura | Consumir el campo en C1 o C3 | Columnas de auditoría, endpoint de carga, CSV y UI |
| Riesgos | Rotura silenciosa, fuga de credenciales, violación de límites entre módulos | Ellos no lo priorizan, y su módulo acaba siendo dueño de un dato que no es de su dominio (termina en la legalización) | Copia desactualizada, error humano en la carga |
| Defendible en el ADD | **Débil.** Solo es aceptable con condiciones (abajo) | **Media-alta** si el ing. Matías asigna a Matrícula la propiedad del avance académico | **Alta como transición.** El ADD lo registra como decisión temporal con fecha de revisión, hasta que exista la fuente institucional (SC-02 / TP-151) |

**Condiciones para que la Opción 1 sea aceptable.** Las cinco son obligatorias:
1. Mismo servidor, pero **esquemas separados**.
2. Un usuario MySQL **de solo lectura**, con `GRANT SELECT` limitado a **vistas** (por ejemplo `v_tramites_estudiante`), nunca a tablas.
3. La vista funciona como contrato versionado (`v1`) y la mantienen ellos con sus `db/scripts`.
4. Credenciales en variables de entorno, rotadas, y nunca compartidas con la cuenta de la aplicación.
5. Solo tendría sentido si el ing. Matías confirma que la migración a MySQL buscaba compartir servidor, lo que hoy **no** ocurre.

Aun cumpliéndolas, no resuelve el numerador: la vista tendría que leer una tabla de créditos que nadie llena.

### 3.1 Borrador del PR para la Opción 2 (propuesta, **no** se escribe en su repo)

Sigue sus convenciones en `dev`:
- Entity, Repository y Mapper (MapStruct) en `api-base`.
- Input y Output en `api-core/records`.
- La lógica en `api-core/services/core` (ya no se llama *Processor*).
- El Case en `api-rest/controllers/cases`.
- La ruta en `SecurityConfig`.
- El script en `db/scripts`.

**Comparación de modelos.**

| Modelo | A favor | En contra |
|---|---|---|
| (a) Una tabla `avanceacademico` 1:1 con `estudiantes` | Mínimo, no toca la entidad que genera Telosys, admite auditoría | Guarda el total, no el detalle por materia |
| (b) `pensum` / `materia` / `pensum_materia` / `nota` | Modelo académico completo, el total se calcula | Construye un sistema de registro académico dentro de Matrícula, duplica DIVISIT, requiere UI de notas. Desproporcionado para SC-03 |

**Recomendación del borrador: (a).** Deja `pensum_id` como está, para que el modelo (b) lo use si algún día se construye.

**DDL**, en `db/scripts/2026-11-SC03-avanceacademico.sql`, con su formato de cabecera y de forma idempotente:
```sql
-- PROPÓSITO      SC-03 (Trámites). Créditos aprobados por estudiante, para la regla del 100 % de Trámites.
-- PRERREQUISITO  Respaldo completo (mysqldump). Ejecutar ANTES de desplegar el código (ddl-auto = none).
-- VERIFICAR ANTES SELECT COUNT(*), COUNT(DISTINCT cedula) FROM estudiantes;  -- deben coincidir (cédula única)
-- CÓMO REVERTIR  Revertir el despliegue y luego: DROP TABLE avanceacademico;
CREATE TABLE IF NOT EXISTS avanceacademico (
  id                  INT          NOT NULL AUTO_INCREMENT,
  id_estudiante       INT          NOT NULL,
  creditosaprobados   INT          NOT NULL,
  fuente              VARCHAR(20)  NOT NULL,          -- MANUAL | CSV | INSTITUCIONAL
  soporte             VARCHAR(500) NULL,              -- acta o archivo de origen
  actualizadopor      INT          NULL,              -- usuarios.id
  fechaactualizacion  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_avanceacademico_estudiante (id_estudiante),
  CONSTRAINT fk_avanceacademico_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiantes (id),
  CONSTRAINT ck_avanceacademico_creditos CHECK (creditosaprobados >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- Opcional, si VERIFICAR ANTES no muestra duplicados:
-- ALTER TABLE estudiantes ADD UNIQUE KEY uk_estudiantes_cedula (cedula);
-- Rol técnico: INSERT INTO roles (nombre) SELECT 'Servicio Tramites'
--   WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre = 'Servicio Tramites');
```
El usuario técnico se crea desde su UI de superadmin (`/superadmin/usuarios`), no en el script, para no versionar ningún hash de contraseña.

**Piezas del código.**
1. `B/persistence/entities/AvanceacademicoEntity.java` y `B/persistence/repositories/AvanceacademicoRepository.java` (`findByIdEstudiante`).
2. `EstudiantesRepository`: añadir `Optional<EstudiantesEntity> findByCedula(String cedula)`.
3. `B/enums/Rol.java`: añadir `SERVICIO_TRAMITES("Servicio Tramites")`. Su `RoleNameNormalizer` ya convierte el nombre al código.
4. `C/records/output/integracion/SituacionAcademicaOutput.java` y `AvanceAcademicoOutput.java`, como records `@Builder` que implementan `OutputResponse`.
5. `C/services/core/integracion/IntegracionTramitesCoreService.java`. Lanza `DomainException("ESTUDIANTE_NOT_FOUND")` (404 por su sufijo) y `DomainException("CEDULA_INVALIDA")` (400).
6. `R/controllers/cases/integracion/IntegracionTramitesCase.java`, con `@RequestMapping("/integracion/v1/estudiantes")`. Queda bajo `/api/application/case/...` y solo admite GET.
7. `SecurityConfig`: un `SERVICIO_TRAMITES_PATHS = { "/api/application/case/integracion/v1/**" }` con `.hasRole(Rol.SERVICIO_TRAMITES.getCodigo())`, **antes** del catch-all de la línea 129. Así el rol técnico no hereda nada más.
8. Mergear a `dev` el cambio de la rama `render` que hace público `/actuator/health`, que es C2.

**Endpoints** (los formatos están en la sección 5):
- `GET /posgrados-project/api/application/case/integracion/v1/estudiantes/{cedula}`, que es C1.
- `GET .../integracion/v1/estudiantes/{cedula}/avance-academico`, que es C3 (solo con la Opción 2).

---

## 4. Recomendación

**Integración A, con B como fase 2 opcional. Créditos con la Opción 3 como transición, detrás de un puerto que permita cambiar a la Opción 2 o a la fuente institucional sin tocar las reglas.**

1. Matrícula es la fuente de verdad de la identidad, el programa, la cohorte, la legalización y los **créditos requeridos** (`programas.creditos`). Se consume con C1, de solo lectura.
2. Ninguno de los dos sistemas produce los créditos aprobados. Ponerlos en Matrícula (Opción 2) no los vuelve oficiales: seguirían siendo una carga manual, ahora en un módulo cuyo dominio termina en la legalización.
3. La Opción 3 no depende del calendario del otro equipo, se implementa en nuestro repo y se defiende en el ADD si es auditable (fuente, soporte, quién y cuándo) y temporal.
4. El puerto `AvanceAcademicoProvider` hace que pasar a la Opción 2, o a la BD institucional de SC-02 / TP-151, sea cambiar un adaptador.
5. La Opción 1 se descarta salvo que el ing. Matías confirme servidor compartido, y aun así solo con vistas y un usuario de solo lectura.
6. Autenticación: cuenta técnica con rol `SERVICIO_TRAMITES` vía su `/auth/login`. Reutiliza su seguridad sin escribir filtros nuevos y aplica mínimo privilegio. **La decisión es suya.**
7. Hace falta que el ing. Matías confirme quién es institucionalmente dueño del historial académico. Si responde «Matrícula», se activa la Opción 2 con el borrador de la sección 3.1.

---

## 5. Contrato propuesto (v1, evolución del v0.1 de TP-143)

**Convenciones.**
- HTTPS, JSON en UTF-8, fechas ISO 8601 (`AAAA-MM-DD`).
- La cédula viaja como texto, solo dígitos (`\d{5,15}`).
- Las rutas llevan `/v1`. Un cambio incompatible exige `/v2` y aviso previo.
- Errores con cuerpo `{code, message, param}` (su formato).

### 5.1 Lo que expone Matrícula

| ID | Método y ruta completa | Auth | Propósito |
|---|---|---|---|
| C1 | `GET {host}/posgrados-project/api/application/case/integracion/v1/estudiantes/{cedula}` | `Bearer` con rol `SERVICIO_TRAMITES` | Situación del estudiante (identidad, programa, cohorte, estado, legalización, créditos requeridos) |
| C1b | `GET .../integracion/v1/estudiantes?codigo={codigo}` | idem | Opcional: búsqueda por código |
| C2 | `GET {host}/posgrados-project/actuator/health` | pública (rama `render`) | Disponibilidad y *warm-up* |
| C3 | `GET .../integracion/v1/estudiantes/{cedula}/avance-academico` | idem | Solo con la Opción 2: créditos aprobados y requeridos |
| Auth | `POST {host}/posgrados-project/auth/login` `{username, password, requestedRole:"SERVICIO_TRAMITES"}` → `{accessToken, refreshToken}` (120 min / 7 días); renovación con `POST /auth/refresh` | — | Obtener el token de servicio |

**C1 · 200.** Los nombres de campo son los del contrato v0.1. Son limpios a propósito: no se exponen `fechalngreso` ni `moodleld`.
```json
{
  "cedula": "1090000001", "codigo": "11550007",
  "nombres": "Laura Marcela", "apellidos": "Pérez Gómez",
  "email": "laura.perez@example.com", "estado": "ACTIVO",
  "programa": { "id": 12, "codigo": "1155", "nombre": "Maestría de ejemplo", "creditosRequeridos": 56 },
  "admision": { "cohorteId": 34, "cohorte": "Cohorte 2025-1", "semestre": "2025-1",
                "fechaAdmision": "2025-01-20", "fechaIngreso": "2025-02-03" },
  "matricula": { "estado": "COMPLETADO", "valor": 4500000, "fechaVencimiento": "2025-02-01" },
  "creditos": { "requeridos": 56, "aprobados": null }
}
```
`creditos.aprobados` es `null` mientras rija la Opción 3. Si se adopta la Opción 2, se llena desde `avanceacademico`.

**C3 · 200** (solo con la Opción 2):
```json
{ "cedula": "1090000001", "creditosRequeridos": 56, "creditosAprobados": 40,
  "porcentaje": 71.4, "fuente": "MANUAL", "fechaActualizacion": "2026-10-30" }
```

**Errores (C1 y C3).**

| HTTP | `code` | Cuándo | Qué hace Trámites |
|---|---|---|---|
| 400 | `CEDULA_INVALIDA` | La cédula no es numérica o viene vacía | Registra el error, no reintenta |
| 401 / 403 | sin cuerpo | Token ausente o vencido. **Ojo:** sin httpBasic, su Spring responde **403 también para «no autenticado»** | Hace *login* otra vez una sola vez y reintenta; si vuelve a fallar, lo trata como indisponible y lo registra como error de configuración |
| 404 | `ESTUDIANTE_NOT_FOUND` | No hay estudiante con esa cédula, o no está legalizado | «No apareces como admitido y legalizado» |
| 422 | `PROGRAMA_SIN_CREDITOS_UNPROCESSABLE` | El estudiante existe pero su programa no tiene `creditos` | Bloquea el trámite con un mensaje claro. **Requiere** que ellos mapeen el sufijo `_UNPROCESSABLE` a 422 en su `GlobalExceptionHandler`, que hoy lo convierte en 400 |
| 500 | `ERROR_INTERNO` | Fallo en Matrícula | Reintenta según RNF06 |
| 502 / 503 | — | *Cold start* o proxy de Render | Reintenta y aplica la contingencia (sección 6.3) |

### 5.2 Lo que expone Trámites (fase 2, opcional, si Matrícula quiere reflejar el grado)

| ID | Método y ruta | Auth | Respuesta |
|---|---|---|---|
| T1 | `GET {tramites}/api/integracion/v1/estudiantes/{cedula}/grado` | `X-Api-Key` validada por un filtro nuevo en nuestra `SecurityConfig`, con rol técnico `SERVICIO_MATRICULA` | `200 {"cedula":"…","estadoGrado":"GRADUADO","fechaGrado":"2026-12-05","numeroActa":"…"}` · `404 ESTUDIANTE_NOT_FOUND` · `401` |

Es *pull* a propósito, para no depender de webhooks entre dos Render free.

---

## 6. Diseño interno en nuestro backend

### 6.1 Puertos y adaptadores (reutilizan la rama `HU19-ClienteMatricula`)

- **El contexto pide crear `MatriculaGateway`, pero el puerto ya existe** como `integracion/matricula/MatriculaClient`. Se propone **renombrarlo** a `MatriculaGateway` solo si el equipo lo prefiere; no aporta valor funcional.
- Adaptadores existentes:
  - `MatriculaHttpClient`: `RestClient` con `SimpleClientHttpRequestFactory`, sin dependencias nuevas.
  - `MatriculaMockClient`: datos fijos y cédulas especiales para caída, respuesta inválida y créditos `null`.
  - Se eligen con `matricula.modo=http|mock` mediante `@ConditionalOnProperty`. El valor por defecto es `mock`.
- **Puerto nuevo** `AvanceAcademicoProvider`, con `AvanceAcademico obtener(Estudiante)`:
  - `LocalAvanceAcademicoProvider` (Opción 3): lee la columna `estudiante.creditos_aprobados` y sus campos de auditoría.
  - `MatriculaAvanceAcademicoProvider` (Opción 2): usa C3. Se activa con `tramites.creditos.fuente=matricula`.
- **Servicio nuevo** `AvanceAcademicoService`. Es la **única** fuente de `aprobados`, `requeridos`, `faltantes` y `cumple100`, y sustituye las cuatro copias de R1, R3, R4 y R5.

### 6.2 Ajustes al adaptador existente antes del merge

1. **Ruta:** `RUTA_SITUACION` pasa de `/api/application/case/integracion/estudiantes/{cedula}` a `/api/application/case/integracion/v1/estudiantes/{cedula}`.
2. **Credencial** (hecho en N2):
   - Hoy `matricula.token` es un token fijo y `renovarCredencial()` no hace nada.
   - Sus tokens duran 120 minutos, así que un token fijo **caduca**. Hay que implementar el *login* técnico (`/auth/login` con `requestedRole`) y guardar el token en memoria hasta unos 110 minutos.
   - Ante un 401 o 403, `renovarCredencial()` vuelve a hacer *login*.
   - Si ellos eligen API key, se queda la cabecera fija y se cambia solo `autenticar()`.
3. **Propiedades.** El contexto pedía `matricula.base-url` y `matricula.api-key`, pero la rama ya usa `matricula.url` / `MATRICULA_URL` y `matricula.token` / `MATRICULA_TOKEN`. Se propone **conservar los nombres existentes**, que ya tienen pruebas, y añadir `matricula.usuario` / `MATRICULA_USUARIO` y `matricula.password` / `MATRICULA_PASSWORD`. Todas van con valor por defecto vacío (`${X:}`), como `supabase.*`. No se versiona ninguna credencial; en Render se configuran como variables de entorno.

### 6.3 Timeouts, reintentos y contingencia

Lo que ya implementa la rama:
- 2 s de conexión y 5 s de respuesta.
- Hasta 2 reintentos, solo ante 5xx o *timeout*, con un presupuesto total de 15 s.
- Nunca lanza excepciones hacia el llamador: devuelve `ConsultaMatricula.Resultado`.

**El problema del *cold start*.** Con unos 50 s de arranque, la primera llamada **siempre** agota el presupuesto. Propuestas:
1. ***Warm-up*:** al hacer *login* un estudiante en Trámites, se lanza un `@Async` (`@EnableAsync` ya existe en `TramitesApplication.java:9-10`) contra C2, sin bloquear.
2. ***Snapshot*:** cada C1 exitosa actualiza el perfil `Estudiante` (código, nombres, email, `cohorteId`, programa, `fechaIngreso`) y guarda una marca nueva, `matricula_sincronizado_en`.
   - Si Matrícula no está disponible y el *snapshot* tiene menos de N horas (propuesta: 24 h), se usa. Son datos reales en caché, no simulados, así que es compatible con RF16.
   - **El Product Owner debe validar N.**
3. Sin un *snapshot* vigente, se bloquea **solo el inicio** de trámites nuevos con el mensaje de `ConsultaMatricula.noDisponible()`. Los trámites ya iniciados siguen su curso (RF16).
4. Opcional, **decisión de ellos**: un *ping* externo a C2 cada 10 minutos para mantener despierto su servicio. Consume horas del plan free.

**Hecho en N5 y N6** (`SincronizacionMatriculaService`):
- El *warm-up* y la consulta se hacen al hacer *login* un estudiante (C2 y, si responde, C1). Además, C1 se consulta antes de crear una terminación, un grado o un certificado.
- El *snapshot* va en `estudiante.estado_matricula` y `estudiante.matricula_sincronizado_en`. El programa se mapea por `programa_academico.codigo`; si el programa aún no tiene código, lo adopta del programa con el mismo nombre. Script: `.docs/sql/migracion_sc03_snapshot_matricula.sql`.
- Interruptor: `matricula.sincronizar` / `MATRICULA_SINCRONIZAR`.
  - Con `false` (valor por defecto) no se consulta ni se bloquea nada, para no guardar datos del mock en una BD real.
  - En el perfil `h2` está en `true`.
  - En producción se activa junto con `matricula.modo=http`.
- Vigencia de la copia: `matricula.snapshot-vigencia-horas` (24).

### 6.4 Mapeo de su respuesta a nuestro modelo

| C1 | Nuestro modelo | Nota |
|---|---|---|
| `cedula` | `Usuario.cedula`, `Estudiante.cedula` | Llave. Se valida que coincida con la del JWT |
| `codigo`, `nombres`, `apellidos`, `email` | `Estudiante.*` (*snapshot*) | `Usuario` no se sobreescribe, para no romper el *login* por código |
| `programa.codigo` | `ProgramaAcademico` | **Hace falta añadir `codigo`** a `ProgramaAcademico` (script en `.docs/sql/`) para mapear por código y no por nombre. Si no existe el programa: 422 local y registro en el log |
| `programa.creditosRequeridos` | denominador de `AvanceAcademicoService` | Sustituye a `totalCreditos` del seed. Si difieren, gana Matrícula y se registra `WARN` |
| `admision.cohorteId`, `fechaIngreso` | `Estudiante.cohorteId`, `fechaIngreso` | — |
| `estado` (ACTIVO) | **nuevo** `Estudiante.estadoMatricula` (texto) | **No** se pisa `estadoEstudiante`, porque PAGO_GRADO_PENDIENTE y GRADUADO los decide Trámites |
| `creditos.aprobados` | ignorado mientras rija la Opción 3 | Con la Opción 2, lo lee `MatriculaAvanceAcademicoProvider` |

### 6.5 Qué hacer con nuestro cálculo actual de créditos

1. **Centralizar:** R1, R3, R4, R5 y R6 llaman a `AvanceAcademicoService`. Se eliminan los fallbacks inconsistentes (`MAX_VALUE` frente a 0) y el NPE.
   - Si falta el programa o los créditos requeridos, el resultado es «no evaluable»: bloquea y se muestra como tal, nunca como «Requisitos completos».
2. **Mensaje de RF16:** «Faltan N créditos (X de Y)».
3. **Opción 3:** `estudiante.creditos_aprobados` se mantiene como almacén y se le añaden `creditos_fuente`, `creditos_soporte`, `creditos_actualizado_por` y `creditos_actualizado_en`, con un script en `.docs/sql/`.
   - **Carga individual:** el director, para estudiantes de su programa, con `PUT /api/director/estudiantes/{cedula}/creditos-aprobados`.
   - **Carga masiva:** Posgrados, con `POST /api/posgrados/creditos-aprobados/importar` (CSV `cedula,creditos_aprobados,soporte`) y un informe de filas rechazadas.
   - El seed queda solo para el perfil `h2`.
4. **R2:** se recomienda revalidar el 100 % al crear la solicitud de grado, por defensa en profundidad. Hoy solo depende de que la terminación esté aprobada.

---

## 7. Plan (subtareas propuestas bajo TP-138)

### Nuestro equipo

| # | Subtarea | Criterio de terminado |
|---|---|---|
| N1 | Cerrar TP-144: compartir este documento y el contrato v1 con el equipo de Matrícula y el ing. Matías, y registrar sus respuestas | Respuestas a la sección 8 registradas en TP-144. Contrato v1 marcado como «acordado» o con cambios |
| N2 | **Reabrir o corregir TP-146**: integrar `HU19-ClienteMatricula` en `main` con la ruta `/v1` y el *login* técnico | PR mergeado. Pruebas `MatriculaHttpClientTest` y `MatriculaMockClientTest` en verde en la CI (perfil `h2`). `matricula.modo=mock` por defecto |
| N3 | `AvanceAcademicoService` sustituyendo las cuatro copias del cálculo (sin `AvanceAcademicoProvider`: la fuente es solo nuestra BD, ver 7.1) | Un solo punto de cálculo, mensaje «Faltan N créditos», pruebas de borde (null, programa sin créditos, 0, igual, mayor) |
| N4 (descartada) | Opción 3: auditoría de créditos, endpoint del director, CSV de Posgrados, UI | Script SQL en `.docs/sql/`. Carga individual y masiva con trazabilidad. Pruebas de servicio |
| N5 | Sincronizar el *snapshot* del estudiante desde C1, incluido `ProgramaAcademico.codigo` y `estadoMatricula` | Al *login* o al iniciar un trámite se actualiza el *snapshot* y se registra la hora |
| N6 | Contingencia: *warm-up* contra C2 y política de *snapshot* con TTL | Con el mock en modo «caído», el estudiante recibe respuesta en ≤ 15 s y no se habilita ningún trámite sin un *snapshot* vigente |
| N7 (aplazada) | Corregir el *login* de Google sin cédula (bloquea la llave de la integración) | Un usuario de Google sin cédula no queda con principal `null`: se le pide completar la cédula o no se le crea la cuenta |
| N8 | TP-147: eliminar `DEMO_USERS` (`menuConfig.js:47`, `AuthContext.js:85`) en producción | Ninguna lectura de `DEMO_USERS` fuera del perfil dev |
| N9 | TP-148: pruebas de integración con mock (`MockRestServiceServer`, que ya se usa) y contra su servicio real | Caminos feliz, 404, 401/403, *timeout*, *cold start* y respuesta inválida, con evidencia en el log |
| N10 | Documentación: ADD, CU-05, SRS (unificar RNF06 / RNF-09) y riesgo R7 (resolver el choque con el riesgo de Wompi); TP-145 (EIF) | Secciones de la sección 9 actualizadas y enlazadas desde TP-144 |
| N11 (fase 2) | T1, el endpoint del estado del grado, con filtro de API key | Solo si Matrícula lo pide |
| N12 (fase 2) | Inicio de sesión único con **token de paso**: Matrícula emite un token de 2 min, solo para Trámites, con la cédula y firmado con un secreto exclusivo; nuestro `POST /api/auth/matricula` lo verifica, busca al estudiante por cédula y entrega nuestro JWT | El botón «Ir a Trámites» deja al estudiante dentro sin volver a iniciar sesión. Ni su JWT principal ni su `app.jwt.secret` salen de Matrícula |

### Equipo de Matrícula

| # | Subtarea | Criterio de terminado |
|---|---|---|
| M1 | Endpoint C1 por cédula (`findByCedula`, Case `integracion/v1`) con cuerpos de error `{code,message,param}` | Aparece en su Swagger, con respuesta según la sección 5.1 y errores 400/404 con cuerpo |
| M2 | Rol `SERVICIO_TRAMITES` (o la alternativa que elijan), usuario técnico y `SERVICIO_TRAMITES_PATHS` en `SecurityConfig` antes del catch-all | Con el token técnico, C1 responde 200; cualquier otra ruta, 403 |
| M3 | Llevar a `dev` el `/actuator/health` público de la rama `render` | C2 responde sin token en producción |
| M4 | Compartir la URL de producción, la rama desplegada y la lista de valores de `estado` (estudiante y pago) | Datos registrados en TP-144 |
| M5 | (Si eligen 422) mapear el sufijo `_UNPROCESSABLE` → 422 en su `GlobalExceptionHandler` | Prueba unitaria suya |
| M6 (si Opción 2) | `avanceacademico` (script, entidad, C3, UI de carga) según la sección 3.1 | C3 según contrato, con script idempotente en `db/scripts/` |
| M7 (opcional) | Botón «Ir a Trámites» en `AspiranteInicio.tsx:277-286` con `VITE_TRAMITES_URL` | Visible solo cuando el aspirante está legalizado |

**Orden:** N1 → (M1, M2, M3 en paralelo con N2, N3, N8) → N5, N6 → N9 contra el servicio real → N10. N11, N12 y N7 quedan fuera de esta iteración; N4 se descarta (7.1, punto 4).

### 7.1 Decisiones del PO (2 nov 2026)

1. **Contingencia (6.3):** si Matrícula no responde, se usa el *snapshot* si tiene menos de 24 h; si no, se bloquea el inicio de trámites nuevos y los ya iniciados siguen su curso.
2. **Login con Google:** hoy no funciona; N7 se aplaza.
3. **Condición de acceso (pregunta 12, brecha B5):** basta con que el estudiante exista en Matrícula (antes de registrarse ni siquiera es aspirante). No se valida matrícula por semestre.
4. **Créditos aprobados (preguntas 10 y 11):** no tienen dueño institucional; vienen precargados en nuestra BD (Opción 3). Los requeridos los define el superadmin de Matrícula al crear el programa. Se asume que **siempre** serán precargados: ni Matrícula ni Trámites manejan materias, notas ni pensum (en Matrícula, `pensum_id` siempre queda en null y «semestre» es el periodo de la cohorte). Por eso N3 no lleva el puerto `AvanceAcademicoProvider` y N4 se descarta: los créditos se actualizan directamente en la BD. La pregunta 1 de la sección 8 queda cerrada.
5. **Autenticación (pregunta 3):** Matrícula autentica por rol + JWT, así que se usa la cuenta técnica `SERVICIO_TRAMITES` con su `/auth/login`. La API key se descarta.
6. **Inicio de sesión único:** fuera de esta iteración; si se hace, con token de paso (N12). No se comparte su `app.jwt.secret`, que además firma los enlaces de confirmación de correo y de cambio de contraseña (`EmailConfirmationTokenService.java:25-26`, `PasswordResetTokenService.java:25-26`).
7. **Producción de Matrícula:** la rama `render` (confirmado). Los PR van a `dev`.

---

## 8. Preguntas abiertas

**Para el equipo de Matrícula**
1. ~~¿Existe en su BD la tabla `pensum` u otras sin mapear (materias, notas)? En el código no hay ningún rastro y su `.gitignore` excluye los `.sql`.~~ Cerrada: créditos precargados (7.1).
2. ¿Aceptan un PR nuestro en su repo, o prefieren implementar M1, M2 y M3 ellos? ¿Sobre qué rama (`dev`)?
3. ¿Aceptan el rol técnico `SERVICIO_TRAMITES` con cuenta de servicio, o prefieren API key con un filtro propio? ¿Algún otro mecanismo?
4. ¿Agregan la búsqueda por cédula (C1)? ¿Garantizan que la cédula es única (`existsByCedula` lo valida en la aplicación, pero no hay índice `UNIQUE`)?
5. ¿Cuál es su URL de producción, qué rama está desplegada, y aceptan mantener el servicio despierto (Render free)?
6. ¿Valores completos de la tabla `estado` para estudiante y pago de matrícula? ¿Tienen previsto algún estado distinto de ACTIVO?
7. ¿Validan los umbrales de RNF06 (2 s / 5 s / 2 reintentos / 15 s)?
8. ¿Les interesa la fase 2 (T1: estado del grado)?

**Para el ing. Matías**
9. ¿La migración de Trámites a MySQL buscaba **compartir el servidor de BD** con Matrícula? ¿Con el servidor institucional de SC-02?
10. ¿Quién es institucionalmente dueño del historial académico (notas y créditos aprobados): Registro / DIVISIT, Matrícula o Trámites?
11. Mientras no exista esa fuente, ¿quién está autorizado a cargar los créditos aprobados, el director de programa o la oficina de Posgrados, y con qué soporte?
12. ¿Basta con la legalización inicial, o hay que validar matrícula vigente por semestre (brecha B5)?

---

## 9. Cambios al ADD y al CU-05

**ADD.** El documento EP4 no está en el repo; los nombres de sección son **Supuesto:** según la estructura habitual de ADD.

| Sección | Contenido a añadir |
|---|---|
| Contexto del sistema | Matrícula como sistema externo proveedor (solo lectura), el registro académico como fuente futura de créditos, y el flujo de datos con la cédula como llave |
| Decisiones de arquitectura (ADR) | **ADR-SC03-1:** integración REST síncrona, de solo lectura, Trámites → Matrícula (opción A), con la alternativa C descartada y su motivo. **ADR-SC03-2:** fuente de créditos aprobados (Opción 3 temporal, con condiciones de salida hacia la 2 o la institucional; Opción 1 descartada y sus condiciones). **ADR-SC03-3:** autenticación servicio a servicio (la opción que se acuerde) |
| Vista de componentes | Puerto `MatriculaClient` (o `MatriculaGateway`), adaptadores `http` y `mock`, `AvanceAcademicoProvider` y `AvanceAcademicoService`, y el límite de aislamiento de RNF06 |
| Vista de despliegue | Dos servicios en Render free con BD separadas, variables `MATRICULA_*`, C2 como *health check* |
| Atributos de calidad | Escenarios RNF06: disponibilidad (*cold start* de unos 50 s y respuesta en ≤ 15 s), seguridad (credencial solo en el backend), trazabilidad (log por llamada), modificabilidad (contrato versionado `/v1`) |
| Riesgos | R7 (dependencia externa) con sus mitigaciones (mock, *snapshot*, contrato versionado). Riesgo nuevo: copia desactualizada de los créditos. Resolver el choque de numeración de R7 |
| Interfaces externas | Referencia a la sección 5 (contrato v1) |

**CU-05 · Consultar la situación académica en el Módulo de Matrícula**

| Campo | Contenido |
|---|---|
| Actores | Principal: sistema de Trámites. Proveedor: Módulo de Matrícula. Beneficiarios: el estudiante y el director de programa |
| Trazabilidad | HU-19 (TP-140), RF16, RNF06, R7, SC-03 |
| Precondiciones | Contrato v1 acordado. Credencial de servicio vigente. El estudiante ha iniciado sesión y tiene cédula |
| Flujo normal | 1. El estudiante entra o inicia un trámite. 2. Trámites consulta C1 con su cédula. 3. Valida el contrato y actualiza el *snapshot*. 4. Obtiene los créditos aprobados con `AvanceAcademicoProvider`. 5. Evalúa: ACTIVO, legalizado y aprobados ≥ requeridos. 6. Habilita los trámites o explica la causa («Faltan N créditos») |
| Flujos alternos | 404: no figura como legalizado. 401/403: *login* técnico otra vez una sola vez, y después indisponible más alerta de configuración. *Timeout*, 5xx o *cold start*: reintentos; con *snapshot* vigente se usa el *snapshot*, sin él se bloquea el inicio. Contrato inválido: se descarta y se registra. Créditos aprobados no registrados: bloquea con «Créditos aprobados no registrados; contacta a tu director». 422: programa sin créditos |
| Postcondiciones | Trámite habilitado o bloqueado con datos oficiales. Llamada registrada con fecha, ruta, cédula, código HTTP y duración |
| Reglas de negocio | Grado y terminación solo con el 100 % de créditos. Trámites no escribe en Matrícula. Los trámites ya iniciados no dependen de la disponibilidad de Matrícula |
| Requisitos especiales | Umbrales de RNF06. La credencial nunca llega al navegador |

---

### Supuestos usados en el documento
- **Supuesto:** la cédula es la llave común (como en TP-143).
- **Supuesto:** `APP_PREFIX=/posgrados-project` en producción (`render.yaml:14-15`).
- **Supuesto:** su producción despliega la rama `render`, derivada de `dev`.
- **Supuesto:** `defaultdb` indica una MySQL gestionada distinta de la nuestra (Railway).
- **Supuesto:** el *cold start* es de unos 50 s en ambos servicios.
- **Supuesto:** el ADD (EP4) tiene secciones estándar; hay que ajustar los nombres al documento real.
