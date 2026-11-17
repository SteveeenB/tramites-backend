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
| R-01 | Arranque del backend en Render | Log: `Successfully baselined schema with version: 1`, `now at version v3`, `Started TramitesApplication`; `/api/health` = `UP` | ★ | |
| R-02 | Historial de Flyway | `tp_flyway_schema_history`: baseline 1, V2 y V3, todas con `success = 1` | ★ | |
| R-03 | Catálogos | 5 roles, 3 estados, 16 programas con código, 4 dependencias, 4 tipos de certificado, 2 tipos de solicitud | ★ | |
| R-04 | Aislamiento | Con `tramites_app`: `SELECT COUNT(*) FROM usuarios` → `SELECT command denied` | ★ | |
| R-05 | Respaldo | `mysqldump` de las tablas `tp_*` (`docs/PLAN_RESPALDO.md` §2) y restauración de ensayo con los mismos conteos | | |

### Funcionalidad del módulo

| ID | Caso | Resultado esperado | ★ | Resultado |
|---|---|---|---|---|
| R-10 | Login de cada rol: estudiante, director, Posgrados, dependencia y administrador | Cada uno entra y ve el menú de su rol | ★ | |
| R-11 | Un estudiante intenta abrir una vista de director | Acceso denegado | ★ | |
| R-12 | Terminación de materias, estudiante con créditos completos (`22030002`) | Se crea la solicitud y llega al director | ★ | |
| R-13 | Terminación de materias, estudiante sin créditos completos (`22030001`, 40 de 56) | Bloqueada con el motivo | | |
| R-14 | El director aprueba y rechaza solicitudes, con observación | Cambio de estado e historial con fecha y actor | ★ | |
| R-15 | Subida de documentos (Supabase): formato y tamaño válidos e inválidos | Válidos: se guardan y se abren con URL firmada. Inválidos: mensaje claro | | |
| R-16 | Pago sandbox de Wompi para una terminación | Pago aprobado y solicitud actualizada con el evento de Wompi | ★ | |
| R-17 | Paz y salvo por dependencia (Biblioteca, Financiera, Admisiones) | Cada dependencia registra su aval | | |
| R-18 | Solicitud de grado y fecha de grado publicada por Posgrados | El flujo completo avanza hasta el estado final | | |
| R-19 | Certificado con QR | El PDF se genera y el QR abre la verificación en `FRONTEND_URL` | | |
| R-20 | Notificaciones | Se crean en la app; el correo llega si `MAIL_*` está configurado | | |

### Integración con Matrícula (SC-03)

| ID | Caso | Resultado esperado | ★ | Resultado |
|---|---|---|---|---|
| R-30 | Login técnico (`MATRICULA_MODO=http`) | Log: `[MATRICULA] POST /auth/login status=200` | ★ | |
| R-31 | Consulta por cédula de un estudiante que existe en Matrícula | `GET …/integracion/v1/estudiantes/{cedula} status=200` y copia actualizada | ★ | |
| R-32 | Estudiante que no existe en Matrícula | No puede iniciar trámites nuevos; los que ya inició siguen | | |
| R-33 | Matrícula no responde (`MATRICULA_URL` inválida, temporalmente) | Con copia de menos de 24 h sigue normal; sin copia vigente, mensaje de validación no disponible | | |

## 3. Resultado

| Total | OK | Falla | N/A | ¿Se puede crear el tag `v2.0.0`? |
|---|---|---|---|---|
| 20 | | | | |

Ejecutaron: | Fecha: | Errores registrados en Jira:
