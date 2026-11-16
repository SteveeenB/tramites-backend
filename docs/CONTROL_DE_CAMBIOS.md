# Control de cambios y Comité de Control de Cambios (CCB)

| | |
|---|---|
| **Tarea** | TP-171 · AUD-04 (auditoría IEEE 828:2022, hallazgo 4) |
| **Aplica a** | `tramites-backend` y `tramites-frontend` |
| **Vigencia** | Integración solo por Pull Request desde el **03/11/2026**. Formalizada y con protección de rama activa desde el **15/11/2026** (acta `docs/actas/ACTA_CCB_2026-11-15.md`) |
| **Responsable** | Product Owner |

## 1. Ramas

| Rama | Qué es | Cómo entra un cambio |
|---|---|---|
| `main` | Línea base: lo que se entrega y despliega | Solo por PR desde `develop` (o un `hotfix/…`), con aprobación del CCB |
| `develop` | Integración del sprint | Solo por PR desde una rama de trabajo |
| `feat/TP-xxx-…`, `fix/TP-xxx-…`, `docs/TP-xxx-…` | Trabajo de una tarea de Jira | Commits del desarrollador; se borra al fusionar |

## 2. Política de Pull Requests

1. **Ningún cambio llega a `develop` ni a `main` sin PR.** No se hace `push` ni `merge` local directo a esas ramas.
2. **Revisión obligatoria:** cada PR necesita **1 aprobación** de alguien distinto del autor antes de fusionarse.
3. **CI en verde:** el check `Tests (JDK 17, perfil h2)` (backend) o `Build (Node 22)` (frontend) debe pasar. SonarCloud y GitGuardian se revisan, y un fallo se corrige o se justifica en el PR.
4. **Trazabilidad:** la rama, el título del PR y los commits llevan la clave de Jira (`tipo(TP-xxx): descripción`). La descripción del PR dice qué cambia y cómo se probó.
5. **Fusión:** con *merge commit* desde GitHub. No se reescribe el historial de `main` ni de `develop` (sin `force push`).

**Control técnico en `main` (ambos repositorios, desde el 15/11/2026):** PR obligatorio, 1 aprobación, el check de CI como requerido y **sin excepción para administradores**. No se permiten `force push` ni el borrado de la rama. Evidencia en `docs/evidencias/TP-171/`.

**En `develop` no hay protección de rama configurada en GitHub.** Ahí las reglas 1 a 5 son un acuerdo del equipo, no un bloqueo técnico, para no frenar la integración diaria del sprint. Se controla así: el CCB revisa en la reunión semanal el historial de `develop` con los comandos de la sección 4, y cualquier cambio que no entró por PR queda en el acta. Además, todo lo que está en `develop` vuelve a pasar por un PR con aprobación y CI obligatorios antes de llegar a `main`.

## 3. Comité de Control de Cambios (CCB)

**Miembros**

| Nombre | Rol en el CCB | GitHub |
|---|---|---|
| Johan Steven Bueno Rojas | Product Owner, preside el CCB | `SteveeenB` |
| Kevin Arias | Desarrollador | `kevindavidav` |

- Una decisión es válida con la aprobación de **los dos** miembros. Si uno es autor del cambio, el otro revisa y aprueba el PR; la aprobación de ese PR en GitHub cuenta como su voto.
- Si un miembro no puede asistir, la decisión queda pendiente hasta la siguiente reunión, salvo un cambio de emergencia (ver más abajo).

**Reuniones**

- **Ordinaria:** una vez por semana, para revisar las solicitudes de cambio abiertas y lo que va a pasar de `develop` a `main`.
- **Extraordinaria:** cuando haya un cambio urgente (falla en producción, seguridad, entrega con fecha). La convoca cualquier miembro por el canal del equipo.
- Cada reunión deja un acta en `docs/actas/ACTA_CCB_<AAAA-MM-DD>.md`, con las decisiones y las claves de Jira y PR.

**Qué aprueba el CCB**

| Requiere CCB | No requiere CCB (basta la revisión del PR) |
|---|---|
| Pasar cambios de `develop` a `main` (entrega o *release*) y crear tags de versión | Correcciones y mejoras internas en `develop` dentro del alcance del sprint |
| Cambios de esquema de BD (migraciones Flyway) y de la base compartida con Matrícula | Pruebas, documentación y refactorizaciones sin cambio de comportamiento |
| Cambios en el contrato de la API con Matrícula u otros módulos | |
| Cambios de infraestructura, despliegue, CI, seguridad o de esta política (incluida la protección de ramas) | |
| Cambios de alcance: historias nuevas o modificadas fuera del sprint planeado | |

**Cambio de emergencia:** se puede fusionar con la aprobación de un solo miembro y el CI en verde, siempre por PR. Se ratifica en la siguiente reunión y queda en el acta.

## 4. Historial anterior a la política

Antes del 03/11/2026 hubo cambios directos en las líneas principales (commits y fusiones locales sin PR), que señaló la auditoría del 09/09/2026. Eran anteriores a esta política.

- **Backend:** 55 commits directos (del 09/04/2026 al 30/10/2026) y 6 fusiones locales (la última, el 07/10/2026).
- **Frontend:** 37 commits directos (del 09/04/2026 al 02/11/2026) y 6 fusiones locales (la última, el 02/11/2026 a las 21:47, hora de Bogotá).
- **Desde el 03/11/2026:** ninguno en ninguno de los dos repositorios. Todas las integraciones posteriores son PR fusionados desde GitHub (backend #65 a #70, frontend #49). Las fechas de este documento están en hora de Bogotá.

Se verifica con:

```bash
git log origin/main --first-parent --no-merges --since=2026-11-03     # commits directos
git log origin/develop --first-parent --merges --since=2026-11-03     # solo "Merge pull request #…"
```
