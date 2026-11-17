# Regresión conjunta SC-02 + SC-03

| | |
|---|---|
| **Tarea** | TP-156 · cierre del ciclo (épica TP-184) |
| **Cuándo** | Después del corte a la BD compartida (`docs/CORTE_RAILWAY_A_AIVEN.md` §0), sobre producción (Render + base de Matrícula en Northflank) |
| **Quién** | Equipo de Trámites. Para R-30 a R-33, con alguien del equipo de Matrícula |
| **Cuentas** | Las cuentas iniciales del paso 7 del corte. Las contraseñas se entregan por privado |

Cada caso se marca **OK**, **Falla** (con la clave del error en Jira) o **N/A**. Una falla en un caso marcado ★ impide
el tag `v2.0.0`.

## 1. Evidencia automática previa (16/11/2026)

| Prueba | Resultado |
|---|---|
| `mvn test` (perfil h2) | 125 de 125 |
| `npm test` (frontend) | 32 de 32, 5 suites |
| CI en las últimas 50 ejecuciones | backend 50/50, frontend 40/40 |
| Ensayo DDL + permisos + baseline 1 + V2/V3 en MySQL 8.0.44 con el esquema de Matrícula y `sql_mode` ANSI | `Started TramitesApplication`; las 52 tablas de Matrícula intactas; `SELECT`/`DROP` sobre ellas denegados |
| Ensayo de las 15 cuentas iniciales | 15 de 15 inician sesión; una contraseña equivocada da 401 |
| Ensayo completo en **MySQL 9.7.2** (la versión de la base de Matrícula), `sql_mode` vacío, 16/11/2026 | Permisos y DDL sin errores; Flyway: baseline 1 → V2 → V3 con solo una advertencia («MySQL 9.7 es más nuevo que lo verificado, 8.1»); `Started TramitesApplication`; las 15 cuentas inician sesión; lectura de `usuarios` denegada |

## 2. Casos

### Base de datos e infraestructura (SC-02)

| ID | Caso | Resultado esperado | ★ | Resultado |
|---|---|---|---|---|
| R-01 | Arranque del backend en Render | Log: `Successfully baselined schema with version: 1`, `now at version v3`, `Started TramitesApplication`; `/api/health` = `UP` | ★ | **OK** · `/api/health` = `UP` (17/11/2026, 32 chequeos seguidos durante el deploy de #89); el arranque con Flyway queda probado en R-02 |
| R-02 | Historial de Flyway | `tp_flyway_schema_history`: baseline 1, V2 y V3, todas con `success = 1` | ★ | **OK** · versiones 1, 2 y 3 con `success = 1` |
| R-03 | Catálogos | 5 roles, 3 estados, 16 programas con código, 4 dependencias, 4 tipos de certificado, 2 tipos de solicitud | ★ | **OK** · 5 roles, 3 estados, 16 programas con código, 4 dependencias, 4 tipos de certificado, 2 tipos de solicitud |
| R-04 | Aislamiento | Con `tramites_app`: `SELECT COUNT(*) FROM usuarios` → `SELECT command denied` | ★ | **OK** · `ERROR 1142: SELECT command denied to user 'tramites_app'` sobre `usuarios` |
| R-05 | Respaldo | `mysqldump` de las tablas `tp_*` (`docs/PLAN_RESPALDO.md` §2) y restauración de ensayo con los mismos conteos | | **OK** · `mysqldump` de las 18 tablas `tp_*` (39 KB) restaurado en MySQL 8.0.44 local: 93 filas, mismos conteos tabla por tabla |

### Funcionalidad del módulo

| ID | Caso | Resultado esperado | ★ | Resultado |
|---|---|---|---|---|
| R-10 | Login de cada rol: estudiante, director, Posgrados, dependencia y administrador | Cada uno entra y ve el menú de su rol | ★ | **OK** (por API) · las 17 cuentas iniciales entran con su rol: ADMIN, POSGRADOS, DEPENDENCIA, DIRECTOR y ESTUDIANTE. El menú se revisa en la demo |
| R-11 | Un estudiante intenta abrir una vista de director | Acceso denegado | ★ | **OK** · estudiante → `GET /api/solicitudes/bandeja` y `GET /api/fechas-grado`: 403 |
| R-12 | Terminación de materias, estudiante con créditos completos (`22030002`) | Se crea la solicitud y llega al director | ★ | **OK** con `22070000`; no se probó con `22030002`, que tiene una solicitud demo vieja (hallazgo 3) · solicitud 5: pago → `EN_REVISION` → `APROBADA_DIRECTOR` |
| R-13 | Terminación de materias, estudiante sin créditos completos (`22030001`, 40 de 56) | Bloqueada con el motivo | | **OK** · 422 «No cumples los requisitos académicos. Te faltan 16 créditos (llevas 40 de 56).» |
| R-14 | El director aprueba y rechaza solicitudes, con observación | Cambio de estado e historial con fecha y actor | ★ | **OK parcial** · aprobación con historial (fecha y actor `2000002207`, DIR2207) en las solicitudes 5 y 13. El rechazo no se probó en producción; lo cubren `SolicitudServiceGradoYRechazoTest` y `DecisionSolicitudServiceTest` |
| R-15 | Subida de documentos (Supabase): formato y tamaño válidos e inválidos | Válidos: se guardan y se abren con URL firmada. Inválidos: mensaje claro | | **OK parcial** · válidos: `foto_prueba.png` y `acta_prueba.pdf` guardados en la solicitud 13. Inválidos: no probados |
| R-16 | Pago sandbox de Wompi para una terminación | Pago aprobado y solicitud actualizada con el evento de Wompi | ★ | **OK** · pago `APROBADO` (transacción Wompi `12083901-1794889042-48972`) y la solicitud 5 pasa a `EN_REVISION` por `SISTEMA_WOMPI`. También el pago de grado (solicitud 13) |
| R-17 | Paz y salvo por dependencia (Biblioteca, Financiera, Admisiones) | Cada dependencia registra su aval | | **OK** · Biblioteca, Financiera, Admisiones, Posgrados y Director: `APROBADO` en la solicitud 13 |
| R-18 | Solicitud de grado y fecha de grado publicada por Posgrados | El flujo completo avanza hasta el estado final | | **OK** · solicitud 13 (`22070000`): `APROBADA`, fecha de grado 18/11/2026, pago de grado aprobado y cierre registrado |
| R-19 | Certificado con QR | El PDF se genera y el QR abre la verificación en `FRONTEND_URL` | | **Pendiente** · el acta de la solicitud 5 se generó (`acta_generada = 1`); falta abrir el QR y ver la verificación |
| R-20 | Notificaciones | Se crean en la app; el correo llega si `MAIL_*` está configurado | | **OK** (app) · 2 notificaciones «Solicitud aprobada» para `22070000`. El correo no se verificó |

### Integración con Matrícula (SC-03)

| ID | Caso | Resultado esperado | ★ | Resultado |
|---|---|---|---|---|
| R-30 | Login técnico (`MATRICULA_MODO=http`) | Log: `[MATRICULA] POST /auth/login status=200` | ★ | **OK** · el 17/11/2026 a las 10:37 UTC se actualizaron las copias de 8 estudiantes; sin login técnico no habría respuesta |
| R-31 | Consulta por cédula de un estudiante que existe en Matrícula | `GET …/integracion/v1/estudiantes/{cedula} status=200` y copia actualizada | ★ | **OK** · `matricula_sincronizado_en` y `cohorte_id` actualizados; `22070000` y `22030001` crean certificados (ids 1 y 2) validados contra Matrícula |
| R-32 | Estudiante que no existe en Matrícula | No puede iniciar trámites nuevos; los que ya inició siguen | | **OK** · `steven` (cédula sin cohorte en Matrícula) no puede pedir un certificado: 422. Observación: el mensaje es el genérico «No fue posible validar…» |
| R-33 | Matrícula no responde (`MATRICULA_URL` inválida, temporalmente) | Con copia de menos de 24 h sigue normal; sin copia vigente, mensaje de validación no disponible | | **N/A** · exige cambiar `MATRICULA_URL` en producción. Lo cubren `SincronizacionMatriculaServiceTest` y `MatriculaHttpClientTest` |

## 3. Resultado

| Total | OK | Falla | N/A | ¿Se puede crear el tag `v2.0.0`? |
|---|---|---|---|---|
| 20 | 16 + 2 parciales | 0 | 1 (R-33) | Sí: ningún caso ★ falla. R-14 (★) queda parcial: el rechazo lo cubren las pruebas automáticas. Lo decide el PO |

R-19 queda pendiente (sin ★).

Ejecutaron: el equipo de desarrollo, sobre producción. Fecha: del 16/11/2026 (flujo completo con `22070000` durante el corte) al 17/11/2026 (resto de casos, después de #89). Errores registrados en Jira: ninguno nuevo; los hallazgos del 16/11 se corrigieron en #85 y #86.

### Hallazgos

1. **Cohortes (resuelto el 17/11/2026).** Los estudiantes de prueba de 2203 y 2102 no tenían cohorte en Matrícula y no podían iniciar trámites. Matrícula asignó las cohortes 9 y 4; `22030001` ya crea solicitudes.
2. **Memoria en Render (resuelto el 17/11/2026).** El backend se quedó sin memoria (512 MB). #88 y #89 ajustan `JAVA_OPTS`.
3. **Filas demo que quedaron del incidente de `data.sql`.** `tp_solicitud` tiene 4 solicitudes de terminación de abril de 2026 (ids 7 a 10, sin `estudiante_id`) con cédulas de estudiantes de prueba (`22030002`, `20261006`, `22030003`, `22030004`). Siguen ahí después de la limpieza del 16/11. Las cuentas demo sí se borraron y no volvieron con el último reinicio. Borrarlas requiere aprobación del PO.

