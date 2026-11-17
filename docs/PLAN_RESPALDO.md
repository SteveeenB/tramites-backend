# Plan de respaldo y recuperación

| | |
|---|---|
| **Tarea** | TP-172 · AUD-06 (auditoría IEEE 828:2022, hallazgo 6: "sin plan de respaldo documentado") |
| **Vigencia** | Desde el 16/11/2026 |
| **Responsable** | Product Owner. Cualquier desarrollador puede ejecutar los respaldos |

## 1. Qué se respalda

| Elemento | Dónde vive | Respaldo | Frecuencia | Dónde se guarda | Retención |
|---|---|---|---|---|---|
| Código, historial y tags | GitHub (EC-01, EC-02) | El propio Git: cada clon es una copia completa. Antes de cada línea base, `git bundle create tramites-<repo>-<tag>.bundle --all` | En cada tag | Carpeta privada del PO en Google Drive | Todas las líneas base |
| Tablas `tp_*` (datos de Trámites) | BD compartida en el Aiven de Matrícula (EC-21) | `mysqldump` de las 17 tablas `tp_*` y `tp_flyway_schema_history` (sección 2) | **Semanal**, y **siempre** antes de una migración Flyway o de un cambio de variables de BD | Carpeta privada del PO en Google Drive, en `.zip` **cifrado** (contiene datos personales) | 4 semanales + el de cada línea base |
| Servidor MySQL completo | Aiven de Matrícula | Respaldos automáticos de Aiven, según el plan del servicio | Lo define Matrícula | Aiven | Lo define Matrícula (preguntado en la entrega de TP-209) |
| Documentos subidos | Supabase, bucket `tramites-documentos` (EC-23) | Descarga del bucket (CLI o panel de Supabase) | Mensual | Carpeta privada del PO | 2 copias |
| Nombres de las variables de producción | Render → *Environment* (EC-13) | Lista de **nombres** en `.env.example`. Los **valores** van en el gestor de contraseñas del PO | En cada cambio | Gestor de contraseñas | Vigente |
| Backlog y trazabilidad | Jira TP (EC-28) | Exportar a CSV (*Filters* → *Export*) | Al cierre de cada sprint | Carpeta privada del PO | Todas |

Nunca se guarda un respaldo con datos personales en un repositorio, en un chat ni en una carpeta compartida sin cifrar.

## 2. Respaldo de las tablas `tp_*`

Con el usuario `tramites_app` (tiene `SELECT` sobre todas las tablas `tp_*`). Las credenciales van en un archivo
local `aiven.cnf` fuera de cualquier repositorio (`docs/CORTE_RAILWAY_A_AIVEN.md` §2.2):

```bash
F=$(date +%Y%m%d-%H%M)
TABLAS=$(echo tp_admins tp_dependencias tp_documento_solicitud tp_estados_estudiantes tp_estudiante tp_fechas_grado \
  tp_historial_estado_solicitud tp_notificaciones tp_pagos tp_paz_y_salvo tp_programa_academico tp_roles tp_solicitud \
  tp_solicitud_certificado tp_tipo_certificado tp_tipos_solicitud tp_usuario tp_flyway_schema_history)
mysqldump --defaults-extra-file=aiven.cnf --single-transaction --set-gtid-purged=OFF --no-tablespaces \
  --skip-triggers --hex-blob <base> $TABLAS > tramites_tp_$F.sql
mysql --defaults-extra-file=aiven.cnf <base> < docs/sql/corte/conteos_tp.sql > conteos_$F.txt   # para comprobar la restauración
```

- `--single-transaction` toma una foto consistente sin bloquear a los usuarios ni a Matrícula.
- Solo se respaldan tablas de Trámites. `tramites_app` no puede leer las de Matrícula, y no debe.
- Se registra en el acta del CCB la fecha, el archivo y los conteos.

## 3. Restauración

1. **Ensayo primero.** Restaurar en el MySQL local (una base con el esquema de Matrícula y el `sql_mode` ANSI) y
   comparar los conteos con `conteos_<F>.txt`.
2. **En producción** (lo aprueba el CCB): suspender el backend en Render, restaurar con `tramites_app` y volver a
   comparar los conteos. Para pisar los datos actuales, vaciar antes las tablas `tp_*` (`TRUNCATE`, con
   `FOREIGN_KEY_CHECKS=0` en la sesión). Después, reanudar y hacer las pruebas de humo de
   `docs/CORTE_RAILWAY_A_AIVEN.md` §4.
3. **Simulacro:** una restauración de ensayo por sprint, con el respaldo semanal más reciente. Se registra en el acta.

## 4. Incidente que motiva este plan (16/11/2026)

La BD de producción estaba en Railway, en un plan de prueba. Al vencer la prueba el servicio quedó **fuera de
servicio** y no hubo forma de recuperar sus datos sin pagar el plan. **No existía ningún respaldo**: la búsqueda en
los equipos del PO no encontró ninguno. El backend de producción quedó caído. Los datos eran de prueba, así que se
arrancó en limpio en la BD compartida con Matrícula (`docs/CORTE_RAILWAY_A_AIVEN.md` §0). Las cuentas se
reconstruyeron con las semillas de `data.sql` y los 6 estudiantes que se habían copiado a la copia de Matrícula.

**Lecciones:**
- Un servicio en plan de prueba o gratuito puede apagarse sin aviso. Su respaldo no puede depender de él.
- El respaldo semanal y el previo a cada cambio son obligatorios desde hoy (sección 1).
- La fecha de vencimiento de cada plan gratuito (Render, Aiven, Supabase) debe estar anotada en el CMR, con un
  responsable que la revise.
