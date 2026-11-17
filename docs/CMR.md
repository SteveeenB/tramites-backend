# Registro Maestro de Configuración (CMR)

| | |
|---|---|
| **Tarea** | TP-172 · AUD-06 (auditoría IEEE 828:2022, hallazgo 6) |
| **Aplica a** | Módulo de Trámites de Posgrado: `tramites-backend` y `tramites-frontend` |
| **Responsable** | Product Owner (CCB, `docs/CONTROL_DE_CAMBIOS.md`) |
| **Corte** | 16/11/2026 |
| **Actualización** | En cada PR que agregue, quite o cambie de versión un elemento de este registro, y en cada línea base (tag) |

El CMR lista los **elementos de configuración** (EC) del módulo: qué son, dónde viven, en qué versión están, quién
responde por ellos y cómo se cambian. Complementa a `docs/METRICAS_CONFIGURACION.md` y `docs/PLAN_RESPALDO.md`.

## 1. Líneas base

| Línea base | Backend | Frontend | Estado |
|---|---|---|---|
| `pre-roles-v2-refactor` (tag) | 31/05/2026 | 31/05/2026 | Histórica |
| `main` actual | `89ff490` (02/11/2026) | `97269cf` (02/11/2026) | En producción hasta la caída de Railway (16/11/2026) |
| `develop` actual | `6a306f5` | `9e5756a` | Integración: BD compartida, Flyway V1–V3 |
| **`v2.0.0`** | — | — | Próxima (TP-155): `develop` → `main` con producción en la BD compartida |

## 2. Elementos de configuración

### 2.1 Código y construcción

| ID | Elemento | Ubicación | Versión / estado | Responsable | Cómo se cambia |
|---|---|---|---|---|---|
| EC-01 | Repositorio backend | GitHub `SteveeenB/tramites-backend` | `main` protegida; `develop` | PO | PR (`CONTROL_DE_CAMBIOS.md`) |
| EC-02 | Repositorio frontend | GitHub `SteveeenB/tramites-frontend` | `main` protegida; `develop` | PO | PR |
| EC-03 | Backend (Spring Boot) | `pom.xml` | Spring Boot 4.0.5, Java 17, artefacto `0.0.1-SNAPSHOT` | Equipo | PR |
| EC-04 | Frontend (React) | `package.json` | React 19.2, Create React App 5.0.1, Tailwind 3.4, React Router 7; artefacto `0.1.0` | Equipo | PR |
| EC-05 | Dependencias | `pom.xml`, `package-lock.json`, `docs/DEPENDENCIAS.md` | Versiones fijadas | Equipo | PR |
| EC-06 | Imágenes Docker | `Dockerfile` (los dos repos), `docker-compose.yml` | Backend `eclipse-temurin:17.0.15_6-jre-jammy`; frontend `node:22-alpine` + `nginx-unprivileged:1.27-alpine` | Equipo | PR |

### 2.2 Base de datos

| ID | Elemento | Ubicación | Versión / estado | Responsable | Cómo se cambia |
|---|---|---|---|---|---|
| EC-07 | Esquema (Flyway) | `src/main/resources/db/migration/` | V1 esquema base · V2 prefijo `tp_` · V3 catálogos | Equipo | Migración nueva por PR + CCB. Nunca se edita una aplicada (`docs/MIGRACIONES.md`) |
| EC-08 | Permisos del usuario de BD | `docs/sql/permisos_usuario_tramites.sql`, `docker/mysql-init/02_usuario_tramites.sh`, `docs/entrega-matricula/01_*.sql` | 18 tablas `tp_*` | Equipo | PR; cada tabla nueva agrega su `GRANT` |
| EC-09 | Acuerdo de BD compartida | `docs/BD_COMPARTIDA_MATRICULA.md` (TP-208) | Prefijo `tp_`, integración solo por API | PO + PO de Matrícula | Acta firmada por los dos PO |
| EC-10 | Datos semilla demo | `src/main/resources/data.sql` | Solo perfil `h2` | Equipo | PR |

### 2.3 Configuración

| ID | Elemento | Ubicación | Notas |
|---|---|---|---|
| EC-11 | Propiedades de la aplicación | `application.properties`, `application-h2.properties`, `application-dev.properties` | Perfil por defecto `local`. **`dev` nunca en producción**: activa un login sin contraseña |
| EC-12 | Plantilla de variables | `.env.example` | Sin valores reales; `.env` está en `.gitignore` |
| EC-13 | Variables de producción | Render → *Environment* (backend y frontend) | Las administra el PO. **No están en el repo.** Al cambiarlas se registra en el acta del CCB qué se cambió, sin valores |

### 2.4 Integración continua y calidad

| ID | Elemento | Ubicación | Check requerido |
|---|---|---|---|
| EC-14 | CI backend | `.github/workflows/ci.yml` | `Tests (JDK 17, perfil h2)` (requerido en `main`) |
| EC-15 | CI frontend | `.github/workflows/ci.yml` (frontend) | `Build (Node 22)` (requerido en `main`) |
| EC-16 | Análisis estático y secretos | SonarCloud, GitGuardian (apps de GitHub, los dos repos) | Informativos; un fallo se corrige o se justifica en el PR |
| EC-17 | Protección de ramas | GitHub → *Settings* → *Branches* | `main`: PR, 1 aprobación, CI, sin bypass. `develop`: sin protección (sección 5) |

### 2.5 Entornos y servicios externos

| ID | Elemento | Dónde | Estado al 16/11/2026 | Responsable |
|---|---|---|---|---|
| EC-18 | Backend en producción | Render `tramites-backend` (`https://tramites-backend.onrender.com`) | **Caído**: su BD (Railway) quedó fuera de servicio | PO |
| EC-19 | Frontend en producción | Render `tramites-frontend` (`https://tramites-frontend-r08z.onrender.com`) | Arriba | PO |
| EC-20 | BD de producción (anterior) | Railway (MySQL) | **Fuera de servicio** (venció la prueba gratuita). Datos de prueba, sin respaldo (`docs/PLAN_RESPALDO.md` §4) | PO |
| EC-21 | BD de producción (nueva) | Aiven del equipo de Matrícula, tablas `tp_*` | Pendiente de que Matrícula ejecute `docs/entrega-matricula/` | Matrícula (servidor) / Trámites (tablas `tp_*`) |
| EC-22 | API del Módulo de Matrícula | Producción: `https://p01--posgrados-project--kqqwl9rwqwbv.code.run/posgrados-project`. Copia temporal SC-03: Render `posgrados-api-d4yv` + nuestra Aiven | Producción UP; falta la cuenta técnica (TP-205) | Equipo de Matrícula |
| EC-23 | Almacenamiento de documentos | Supabase Storage, bucket `tramites-documentos` | Operativo | PO |
| EC-24 | Pagos | Wompi (sandbox) | Operativo | PO |

### 2.6 Documentación y gestión

| ID | Elemento | Ubicación |
|---|---|---|
| EC-25 | Documento de arquitectura (ADD v3) | Google Doc, carpeta de la v2 (TP-206) |
| EC-26 | Documentación técnica | `docs/` de cada repo (este registro, `CONTROL_DE_CAMBIOS.md`, `MIGRACIONES.md`, `DEPLOY.md`, `CORTE_RAILWAY_A_AIVEN.md`, integración con Matrícula, …) |
| EC-27 | Actas del CCB | `docs/actas/ACTA_CCB_<AAAA-MM-DD>.md` |
| EC-28 | Backlog y trazabilidad | Jira `tposgrados.atlassian.net`, proyecto TP. Cada rama, PR y commit lleva la clave `TP-xxx` |

## 3. Trazabilidad

Requerimiento o hallazgo (Jira `TP-xxx`) → rama `tipo/TP-xxx-…` → commits `tipo(TP-xxx): …` → PR con descripción y
pruebas → merge en `develop` → PR `develop` → `main` (CCB) → tag de línea base. Desde el 03/11/2026 todo cambio en
`main` y `develop` entra por PR (`docs/METRICAS_CONFIGURACION.md`, M1).

## 4. Identidades del equipo

Antes de TP-172, una misma persona aparecía con varios nombres y correos: 14 identidades en el backend y 13 en el
frontend, para 6 personas. El archivo `.mailmap` (en los dos repos) las normaliza **sin reescribir el historial**.
`git shortlog -sne --all` ya muestra una identidad por persona.

| Persona | Rol | GitHub | Identidades que se unifican |
|---|---|---|---|
| Johan Steven Bueno Rojas | Product Owner, CCB | `SteveeenB` | correo UFPS, *noreply* de GitHub |
| Kevin David Arias Villamizar | Desarrollador | `kevindavidav` | `unknown <kevarias.2195@…>`, *noreply* |
| Santiago Danilo Cepeda Galeano | Desarrollador (empaquetado) | `sdcepeda7` | `sdcepeda7 <gmail>`, `sdcepeda <UFPS>`, *noreply* |
| Diego Alexander Bermúdez Flórez | Desarrollador | `DieBer1` | `DieBer1 <UFPS>`, `Dieber115 <gmail>`, *noreply* |
| Bryan Alexander Niño López | Desarrollador | `AlexCo10` | `AlexNiño <UFPS>`, *noreply* de `AlexCo10` |
| Raúl David Báez Suárez | Desarrollador | `DavidBaez-14` | `DavidBaez-14` y `Raul David Baez Suarez`, mismo correo |

Evidencia de los casos no obvios:
- **`AlexCo10` = Bryan Niño.** Los PR #26 (HU-16) y #32 (PRB-06) de `AlexCo10` contienen commits de autor
  `AlexNiño <bryanalexandernl@ufps.edu.co>` con los mismos títulos ("HU-16 Backend", "fix(PRB-06): …").
- **`unknown <kevarias.2195@gmail.com>` = Kevin Arias.** Es el mismo correo con el Git sin configurar.
- **`Claude <noreply@anthropic.com>`** (8 commits del frontend, 02/11/2026 de 7:40 a 9:47 p. m., hora de Bogotá) **no se
  unifica**. Son commits hechos con un asistente de IA (Claude Code) y los subió a GitHub `SteveeenB`, según el registro
  de eventos *PushEvent* del repositorio. La responsabilidad es del PO. Se deja visible a propósito para no atribuir a
  una persona un texto que generó una herramienta.

**Regla desde ahora:** cada integrante configura `git config user.name` y `user.email` con su nombre y correo
canónicos de la tabla. Una identidad nueva se agrega al `.mailmap` en el mismo PR.

## 5. Observaciones abiertas

| # | Observación | Acción propuesta | Decide |
|---|---|---|---|
| 1 | En `develop` los PR se fusionan **sin aprobación formal**: 0 de 17 desde el 03/11/2026 (M3). Copilot no alcanzó a revisar #73–#75 por falta de cuota | Activar en `develop` la regla "1 aprobación" o exigir el botón *Approve* antes de fusionar | CCB |
| 2 | La BD de producción no tenía respaldo y se perdió el acceso a ella al vencer el plan | `docs/PLAN_RESPALDO.md` | CCB |
| 3 | No hay tag de versión desde mayo | Tag `v2.0.0` (TP-155) | CCB |
