# Acta de Sprint Review final · ciclo 2026-2 (BORRADOR)

| | |
|---|---|
| **Fecha** | (por definir, después de la regresión conjunta) |
| **Tarea** | TP-156 · épica TP-184 |
| **Asistentes** | Equipo GROWEB: Johan Steven Bueno Rojas (PO), Diego Alexander Bermúdez Flórez, Bryan Alexander Niño López, Kevin David Arias Villamizar, Santiago Danilo Cepeda Galeano. Docentes: (completar) |

> Borrador preparado el 16/11/2026 con el estado de Jira y de los repositorios. Las secciones 3 y 4 se completan en la reunión.

## 1. Incremento del ciclo

### SC-03 · Interoperabilidad con el Módulo de Matrícula (TP-138)

| Entregado | Jira |
|---|---|
| Contrato REST v1 con Matrícula, autenticación servicio a servicio con cuenta técnica `SERVICIO_TRAMITES` | TP-142, TP-143, TP-144 |
| Estimación del nuevo EIF (IFPUG) | TP-145 |
| Adaptador `MatriculaClient` (HTTP y simulado), contingencia con copia de 24 h y manejo de caídas (RNF-09) | TP-146 |
| Datos reales del estudiante en lugar de `DEMO_USERS` | TP-147 |
| Pruebas de integración en modo simulado y real | TP-148 |
| Estilo visual alineado con el de Matrícula | TP-207 |
| HU-19: consumir matrícula, admisión y créditos | TP-140 |

### SC-02 · BD compartida con Matrícula (TP-139)

| Entregado | Jira |
|---|---|
| Flyway sobre el esquema (V1) y `ddl-auto=validate` | TP-150 |
| Credenciales externalizadas y pool de conexiones | TP-152 |
| Acuerdo de BD compartida con Matrícula: prefijo `tp_`, integración solo por API | TP-208 |
| Migración V2 (prefijo `tp_`), usuario con permisos solo sobre `tp_*` | TP-209 |
| Backend y frontend dockerizados, docker-compose y `DEPLOY.md` | TP-154, TP-210, TP-211 |
| Paquete de permisos y DDL para Matrícula, V3 (catálogos), plan de corte | PR #73, #74, #75 |

### Auditoría IEEE 828:2022 (TP-182)

| Hallazgo | Estado |
|---|---|
| AUD-01 Credenciales expuestas · AUD-02 `main` desactualizada · AUD-03 Versionado · AUD-05 Ramas | Cerrados (TP-170, TP-167, TP-169, TP-168) |
| AUD-04 CCB y revisión obligatoria de PR | Cerrado (TP-171): CCB constituido, `main` protegida |
| AUD-06 CMR, identidades, métricas, respaldo | Entregado en PR #76 y frontend #51 (TP-172) |

### Documentación

ADD v3.1 (TP-206); correcciones del Primer Informe (TP-179, TP-180, TP-181); presentaciones (TP-173 a TP-178).

## 2. Lo que no se completó y por qué

| Pendiente | Motivo | Plan |
|---|---|---|
| Producción sobre la BD compartida (TP-141, TP-153) | Matrícula ya creó las tablas en su base de Northflank (16/11/2026); falta la cuenta técnica (TP-205) para el corte. Railway quedó fuera de servicio el 16/11/2026 | Corte según `docs/CORTE_RAILWAY_A_AIVEN.md` §0 |
| Línea base `v2.0.0` (TP-155) | Se crea después del corte y de la regresión | Tag en los dos repos |
| Sincronización con la producción de Matrícula (TP-203, TP-205) | Falta la cuenta técnica en su producción | Coordinación con su equipo |
| Monitor anti-sueño (TP-204) | La producción de Matrícula ya no está en Render | Revisar si sigue aplicando |
| Despliegue en el servidor UFPS (alcance original de SC-02) | Descartado por el PO el 16/11/2026 | No aplica (acta CCB N.º 2, D1) |

## 3. Demostración y resultado de la regresión

(Completar con `docs/REGRESION_CONJUNTA.md`.)

## 4. Retroalimentación de los asistentes

(Completar en la reunión.)

## 5. Aprobación

| Nombre | Rol | Firma | Fecha |
|---|---|---|---|
| Johan Steven Bueno Rojas | Product Owner | | |
