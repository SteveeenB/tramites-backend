# Plan de funcionalidades faltantes — Vista de Administrador

> Constancia técnica del estado actual de la administración del sistema y de lo que falta por implementar. Documento de planeación para sprints futuros.

---

## 1. Estado actual — qué puede hacer el admin hoy

La vista de administrador (`ConfiguracionAdmin.jsx`) está montada como única página de "configuración" del sistema. Hoy expone exactamente **dos** secciones funcionales:

| Sección | Endpoint backend | Quién la puede usar |
|---|---|---|
| Convocatoria de Terminación de Materias (fechas inicio/fin) | `PUT /api/convocatorias?cedula=` | Rol `ADMIN` (chequeado en código del controller) |
| Tipos de Certificado (CRUD completo) | `/api/admin/tipos-certificado/**` | Sin chequeo de rol explícito |

**Ruta de acceso:** `/tramites/admin/configuracion`, protegida en el frontend con `rolesPermitidos={['ADMIN', 'POSGRADOS']}` (debate de unificación de roles documentado en `plan_certificados.md §3.11`).

**Todo lo demás del sistema está hardcoded, en el seed (`data.sql`), o configurable solo manipulando BD directamente.** Esto incluye usuarios, programas académicos, dependencias, paz y salvos, costos de trámites, plantillas de correo, documentos requeridos, etc.

---

## 2. Diagnóstico — brechas detectadas

Tras revisar todos los controllers, services y entidades existentes, estas son las áreas donde el administrador **no tiene control** desde la UI:

### 2.1 Gestión de usuarios
- No hay CRUD de estudiantes, directores, dependencias ni administradores.
- Las cédulas, contraseñas, programas asignados y roles se cargan **solo** desde `data.sql` (`ON CONFLICT DO NOTHING`).
- El endpoint de "login" real (`POST /api/usuarios/login`) **está comentado**. Solo funciona el `loginDemo` que recibe la cédula directamente, sin validar contraseña.
- No hay endpoint para resetear contraseña, dar de baja un usuario, cambiarle rol o reasignar programa.

### 2.2 Gestión de programas académicos
- `programa_academico` tiene 16 filas en el seed (5 doctorados + maestrías + especializaciones).
- No hay endpoint, controller ni UI para crear, editar o desactivar un programa.
- Si abren un nuevo programa de posgrado, el admin no tiene cómo registrarlo — toca SQL directo.

### 2.3 Gestión de roles y permisos
- Los roles (`ESTUDIANTE`, `DIRECTOR`, `ADMIN`, `DEPENDENCIA`, `POSGRADOS`) son **strings sueltos** en `usuario.rol`. No hay tabla `rol` ni matriz de permisos.
- El sistema no permite crear roles nuevos (por ejemplo, "REVISOR_TESIS" o "ADMINISTRADOR_PROGRAMA").
- La distinción `ADMIN` vs `POSGRADOS` está sin resolver — históricamente eran el mismo concepto pero el código tiene ambos.
- No existe el concepto de "administrador por programa" (rol que tendría acceso solo a las solicitudes y configuraciones de un programa específico).

### 2.4 Configuración de tipos de trámite y sus precios
- `solicitud.tipo` admite valores hardcoded: `TERMINACION_MATERIAS`, `GRADO`, etc. No hay tabla `tipo_solicitud` ni un equivalente al `tipo_certificado`.
- El **costo** de cada trámite está hardcoded en `data.sql` por fila individual (150.000 para terminación). No hay configurabilidad.
- Si Posgrados decide cambiar el precio de la Solicitud de Grado a $200.000, no hay UI ni endpoint — toca código.
- No hay manera de desactivar temporalmente un tipo de trámite (por ejemplo, "no se aceptan solicitudes de grado durante diciembre").

### 2.5 Configuración de paz y salvos
- `paz_y_salvo.tipo_dependencia` admite strings (`BIBLIOTECA`, `FINANCIERA`, `ADMISIONES`, `DIRECTOR_PROGRAMA`...). No hay catálogo configurable.
- La asociación "qué dependencias debe consultar el sistema al iniciar la solicitud de grado" está implícita en código (`PazYSalvoService`).
- No hay UI para agregar una nueva dependencia al flujo (ej. si Posgrados decide que ahora también se requiere paz y salvo de Bienestar Universitario).

### 2.6 Documentos requeridos por trámite
- La tabla `tipo_documento_requerido` existe en BD con campos `nombre`, `descripcion`, `obligatorio`, `orden`.
- No tiene seed, no tiene controller, no tiene UI.
- Los documentos que se le piden al estudiante al solicitar el grado están **hardcoded** en el frontend (`SolicitudGradoPage.jsx`).
- El admin no puede agregar/quitar documentos requeridos sin desplegar.

### 2.7 Plantillas de correo y notificaciones
- Los correos que envía el sistema (`CorreoCertificadoService`, `CorreoConstanciaService`, `NotificacionEmailService`) tienen el cuerpo **hardcoded** en Java.
- Si Posgrados quiere cambiar el saludo, agregar el logo, modificar la firma — toca tocar código y redeployar.
- No hay tabla `plantilla_correo` ni endpoint para editar plantillas.
- No hay forma de probar el envío (botón "enviar correo de prueba") desde la UI.

### 2.8 Convocatorias
- Existe **una sola convocatoria global** para Terminación de Materias.
- No se pueden tener convocatorias distintas por programa, por nivel (doctorado vs maestría) o por trámite (grado vs terminación).
- No hay histórico de convocatorias anteriores.

### 2.9 Reportes y métricas
- No hay dashboard de admin.
- No se puede consultar: cuántas solicitudes de grado se procesaron este mes, cuántos certificados se emitieron por dependencia, cuál es el tiempo promedio de aprobación por director, etc.
- No hay export a CSV/Excel para ningún listado.

### 2.10 Auditoría
- No hay tabla `auditoria` ni log de quién cambió qué.
- Si alguien edita el precio de un certificado o aprueba una solicitud, no queda rastro de quién lo hizo más allá de los `cedula_director` / `cedula_posgrados` en la propia entidad.
- No hay forma de saber "quién marcó como ENTREGADO el certificado 73 y a qué hora".

### 2.11 Configuración del sistema
- Constantes como "días de vigencia del recibo de pago" (3 en certificados) están hardcoded en cada servicio.
- No hay endpoint para "modo mantenimiento" (suspender solicitudes temporalmente).
- No hay configuración de límites (máximo de certificados por estudiante por mes, etc.).

### 2.12 Seguridad — autenticación inexistente a nivel de endpoint
- **Ningún endpoint usa `@PreAuthorize` ni filtros de seguridad.**
- La identidad del solicitante se pasa como `@RequestParam String cedula` — cualquier cliente puede impersonar a cualquier usuario simplemente cambiando el query param.
- `UsuarioController.login` (con contraseña) está comentado. Solo `loginDemo` funciona y no valida nada.
- `HttpSession` existe pero solo `UsuarioController` la usa; los demás controllers no consultan `session.getAttribute("usuarioCedula")` para autorizar.

---

## 3. Plan de implementación por bloques

A continuación se detalla **qué construir**, agrupado por bloque funcional. Cada bloque incluye los endpoints sugeridos, la UI necesaria en `ConfiguracionAdmin.jsx` y la matriz de autorización propuesta.

---

### Bloque A — Gestión de usuarios

**Objetivo:** que el admin pueda crear, editar, desactivar y reasignar usuarios sin tocar BD.

#### Modelo (cambios sugeridos)
- Agregar `usuario.activo` (boolean, default true).
- Agregar `usuario.fecha_creacion` y `usuario.fecha_ultimo_acceso`.
- Considerar tabla `rol` separada (ver bloque C) o mantener `usuario.rol` como string con catálogo cerrado.

#### Endpoints nuevos (`/api/admin/usuarios`)

| Método | Ruta | Body / Params | Descripción |
|---|---|---|---|
| `GET` | `/api/admin/usuarios?rol=&programa=&busqueda=` | filtros opcionales | Lista paginada con filtros |
| `GET` | `/api/admin/usuarios/{cedula}` | — | Detalle de un usuario |
| `POST` | `/api/admin/usuarios` | `{cedula, codigo, nombre, correo, rol, programaId, contrasena}` | Crear usuario |
| `PUT` | `/api/admin/usuarios/{cedula}` | mismos campos editables | Editar datos |
| `PATCH` | `/api/admin/usuarios/{cedula}/activo?valor=` | — | Activar / desactivar (soft delete) |
| `PATCH` | `/api/admin/usuarios/{cedula}/rol` | `{rol, programaId}` | Cambiar rol y/o programa |
| `POST` | `/api/admin/usuarios/{cedula}/reset-password` | — | Genera contraseña temporal, envía por correo |
| `POST` | `/api/admin/usuarios/importar-csv` | multipart file | Carga masiva (ej. matrícula nueva) |

#### UI en `ConfiguracionAdmin.jsx`
Nueva sección "Usuarios":
- Tabla con filtros (rol, programa, búsqueda por nombre/cédula).
- Modal de creación/edición con campos según rol elegido (un estudiante necesita programa, una dependencia no).
- Botón "Importar CSV" con preview antes de confirmar.

#### Roles autorizados
- **ADMIN / POSGRADOS:** acceso total.
- **COORDINADOR_PROGRAMA** (rol nuevo, ver §C): solo puede ver/editar usuarios de su programa.

---

### Bloque B — Gestión de programas académicos

**Objetivo:** que el admin registre, edite o desactive programas sin SQL.

#### Modelo
- Agregar `programa_academico.activo` (boolean, default true).
- Agregar `programa_academico.codigo_snies` (string opcional, registro oficial).
- Agregar `programa_academico.duracion_semestres` (integer).
- Agregar `programa_academico.modalidad` (PRESENCIAL | VIRTUAL | MIXTA).

#### Endpoints nuevos (`/api/admin/programas`)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/admin/programas` | Lista todos (activos e inactivos) |
| `POST` | `/api/admin/programas` | Crear programa |
| `PUT` | `/api/admin/programas/{id}` | Editar |
| `PATCH` | `/api/admin/programas/{id}/activo?valor=` | Activar/desactivar |
| `GET` | `/api/admin/programas/{id}/estadisticas` | Estudiantes activos, graduados último año, etc. |

> **Endpoint público existente:** `GET /api/programas` debería exponer **solo los activos** (lo usa el dropdown de solicitud de grado).

#### UI
Sección "Programas académicos":
- Tabla con columnas: Nombre, Tipo, Créditos, Modalidad, Activo, # estudiantes, Acciones.
- Modal de edición.

#### Roles
- **ADMIN / POSGRADOS:** total.

---

### Bloque C — Gestión de roles y permisos

**Objetivo:** dejar de tener roles como strings dispersos y permitir creación de roles nuevos.

#### Decisión arquitectónica (a tomar antes de codear)

**Opción 1 (simple):** mantener `usuario.rol` como string pero con catálogo cerrado en una tabla `rol`. Sin matriz de permisos granular. Si en código se necesita un check, sigue siendo `if "DIRECTOR".equals(rol)`.

**Opción 2 (granular):** tabla `rol`, tabla `permiso`, tabla `rol_permiso` (many-to-many), y los endpoints usan `@PreAuthorize("hasPermission(...)")`. Más limpio pero requiere reescribir todos los checks actuales.

**Recomendación:** Opción 1 para este sprint, Opción 2 como deuda explícita. Lo crítico ahora es **unificar `ADMIN` y `POSGRADOS`** y dejar lista la posibilidad de agregar roles nuevos (como `COORDINADOR_PROGRAMA`).

#### Modelo (Opción 1)
```sql
CREATE TABLE rol (
  codigo varchar PRIMARY KEY,        -- ESTUDIANTE, DIRECTOR, ADMIN, ...
  nombre varchar NOT NULL,           -- 'Estudiante de posgrado'
  descripcion text,
  es_sistema boolean DEFAULT false   -- los predefinidos no se pueden borrar
);
ALTER TABLE usuario ADD CONSTRAINT fk_usuario_rol
  FOREIGN KEY (rol) REFERENCES rol(codigo);
```

#### Endpoints nuevos (`/api/admin/roles`)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/admin/roles` | Lista de roles disponibles |
| `POST` | `/api/admin/roles` | Crear rol nuevo (no-sistema) |
| `PUT` | `/api/admin/roles/{codigo}` | Editar nombre/descripción (no el código) |
| `DELETE` | `/api/admin/roles/{codigo}` | Solo si no es de sistema y no tiene usuarios asignados |

#### UI
Sección "Roles del sistema":
- Tabla de roles con conteo de usuarios por rol.
- Crear/editar rol custom (ej. "REVISOR_TESIS", "BIBLIOTECARIO_JEFE").
- Los roles de sistema (ESTUDIANTE, DIRECTOR, ADMIN, DEPENDENCIA, POSGRADOS) aparecen con candado.

#### Roles autorizados
- **ADMIN únicamente.** POSGRADOS no debería crear roles, eso es decisión técnica.

---

### Bloque D — Configuración de tipos de trámite y precios

**Objetivo:** mismo modelo que `tipo_certificado` aplicado a `tipo_solicitud`.

#### Modelo nuevo
```sql
CREATE TABLE tipo_solicitud (
  codigo varchar PRIMARY KEY,           -- TERMINACION_MATERIAS, GRADO, ...
  nombre varchar NOT NULL,
  descripcion text,
  precio double precision NOT NULL DEFAULT 0,
  dias_plazo_pago integer DEFAULT 3,
  requiere_director boolean DEFAULT true,
  requiere_posgrados boolean DEFAULT true,
  requiere_paz_y_salvos boolean DEFAULT false,
  activo boolean DEFAULT true
);
```

Migrar de `solicitud.tipo` (string libre) a FK opcional contra `tipo_solicitud.codigo`.

#### Endpoints (`/api/admin/tipos-solicitud`)
Igual estructura que `tipos-certificado`:
- `GET /` — lista todos
- `POST /` — crear
- `PUT /{id}` — editar (incluyendo precio)
- `PATCH /{id}/activo?valor=` — toggle

#### UI
Sección "Tipos de Trámite":
- Tabla con código, nombre, precio, plazo de pago, qué workflow requiere (director sí/no, posgrados sí/no, paz y salvos sí/no), activo.
- Modal de edición.

#### Roles
- **ADMIN / POSGRADOS** pueden editar precios y desactivar tipos.
- Crear tipos nuevos: solo **ADMIN** (puede romper flujos hardcoded).

> **Nota de coordinación con Posgrados:** se discutió en sprint review la posibilidad de que cada dependencia configure sus propios precios. Si se aprueba, agregar `tipo_solicitud.dependencia_cedula` y restringir edición de precio a esa dependencia.

---

### Bloque E — Configuración de paz y salvos

**Objetivo:** catálogo de tipos de dependencia configurable.

#### Modelo nuevo
```sql
CREATE TABLE tipo_paz_y_salvo (
  codigo varchar PRIMARY KEY,        -- BIBLIOTECA, FINANCIERA, ...
  nombre varchar NOT NULL,
  cedula_responsable varchar,        -- FK a usuario(cedula) con rol DEPENDENCIA o DIRECTOR
  obligatorio boolean DEFAULT true,  -- si es opcional para iniciar la solicitud de grado
  orden integer,                     -- orden de presentación
  activo boolean DEFAULT true
);
```

#### Endpoints (`/api/admin/tipos-paz-y-salvo`)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/` | Lista todos |
| `POST` | `/` | Crear (ej. agregar "Bienestar Universitario") |
| `PUT` | `/{id}` | Editar responsable, orden, etc. |
| `PATCH` | `/{id}/activo?valor=` | Toggle |

#### UI
Sección "Paz y Salvos requeridos":
- Tabla ordenable (drag & drop opcional) con tipo, dependencia responsable, obligatorio sí/no.
- Modal de creación/edición.

#### Roles
- **ADMIN / POSGRADOS.**

---

### Bloque F — Documentos requeridos por trámite

**Objetivo:** activar la tabla `tipo_documento_requerido` que ya existe en BD.

#### Modelo (extensión de la tabla existente)
```sql
ALTER TABLE tipo_documento_requerido
  ADD COLUMN tipo_solicitud_codigo varchar,  -- FK a tipo_solicitud, qué trámite lo pide
  ADD COLUMN formatos_aceptados varchar,      -- 'pdf,jpg,png'
  ADD COLUMN tamano_max_mb integer DEFAULT 5;
```

#### Endpoints (`/api/admin/documentos-requeridos`)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `?tipoSolicitud=GRADO` | Lista los documentos requeridos para un trámite |
| `POST` | `/` | Agregar documento requerido |
| `PUT` | `/{id}` | Editar |
| `DELETE` | `/{id}` | Quitar |
| `PATCH` | `/{id}/orden?valor=N` | Reordenar |

> **Endpoint público:** `GET /api/tipos-solicitud/{codigo}/documentos-requeridos` para que el frontend del estudiante consulte qué subir.

#### UI
Dentro de "Tipos de Trámite", al editar un tipo aparece una sub-tabla "Documentos requeridos" con CRUD inline.

#### Roles
- **ADMIN / POSGRADOS.**

---

### Bloque G — Plantillas de correo

**Objetivo:** que los correos se puedan editar desde la UI sin tocar Java.

#### Modelo nuevo
```sql
CREATE TABLE plantilla_correo (
  codigo varchar PRIMARY KEY,        -- CERTIFICADO_GENERADO, GRADO_APROBADO, ...
  asunto varchar NOT NULL,
  cuerpo_html text NOT NULL,         -- soporta placeholders: {{nombre}}, {{numero_solicitud}}, ...
  cuerpo_plano text,                 -- fallback
  variables_disponibles varchar,     -- doc: "nombre, programa, fecha_grado"
  activo boolean DEFAULT true,
  fecha_modificacion timestamp
);
```

Los servicios de correo (`CorreoConstanciaService`, etc.) leen de aquí en vez de tener el texto hardcoded.

#### Endpoints (`/api/admin/plantillas-correo`)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/` | Lista todas |
| `GET` | `/{codigo}` | Obtiene una |
| `PUT` | `/{codigo}` | Edita asunto y cuerpo |
| `POST` | `/{codigo}/preview` | Renderiza con datos de ejemplo (sin enviar) |
| `POST` | `/{codigo}/enviar-prueba?correo=` | Envío de prueba al correo dado |

#### UI
Sección "Plantillas de correo":
- Lista de plantillas.
- Editor con preview lado a lado, lista de variables disponibles en una sidebar.
- Botón "Enviar prueba".

#### Roles
- **ADMIN únicamente.** El contenido institucional es sensible.

---

### Bloque H — Convocatorias múltiples

**Objetivo:** soportar convocatorias por programa o por trámite, con histórico.

#### Modelo (extensión)
```sql
ALTER TABLE convocatoria
  ADD COLUMN tipo_solicitud_codigo varchar,    -- FK a tipo_solicitud (NULL = aplica a todos)
  ADD COLUMN programa_id bigint,                -- FK a programa_academico (NULL = aplica a todos)
  ADD COLUMN nombre varchar,                    -- '2026-1 Maestrías'
  ADD COLUMN estado varchar DEFAULT 'PROGRAMADA'; -- PROGRAMADA | ACTIVA | CERRADA
```

#### Endpoints (`/api/admin/convocatorias`)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/?estado=&tipo=&programa=` | Lista con filtros |
| `POST` | `/` | Crear nueva |
| `PUT` | `/{id}` | Editar |
| `PATCH` | `/{id}/estado?valor=` | Cambiar estado |
| `GET` | `/historico` | Convocatorias pasadas |

#### UI
Sección "Convocatorias":
- Tabla con todas, filtrable por estado.
- Crear/editar con selector de trámite y programa (ambos opcionales = global).
- Vista de timeline para visualizar solapamientos.

#### Roles
- **ADMIN / POSGRADOS.**

---

### Bloque I — Reportes y métricas

**Objetivo:** dashboard de admin con indicadores clave.

#### Endpoints (`/api/admin/reportes`)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/dashboard` | Resumen: solicitudes activas, certificados emitidos último mes, etc. |
| `GET` | `/solicitudes?desde=&hasta=&tipo=&estado=` | Listado filtrable |
| `GET` | `/solicitudes/export?formato=csv` | Exporta a CSV |
| `GET` | `/certificados?desde=&hasta=&dependencia=` | Listado filtrable |
| `GET` | `/certificados/export?formato=csv` | Export |
| `GET` | `/tiempos-aprobacion?desde=&hasta=` | Tiempo promedio de cada director |
| `GET` | `/ingresos?desde=&hasta=` | Suma de pagos por tipo de trámite |

#### UI
Sección "Reportes":
- Cards con métricas del último mes (n° solicitudes, n° certificados, ingresos estimados).
- Filtros de rango de fechas.
- Tabla detalle con botón "Exportar CSV".
- Gráficos opcionales (chart.js).

#### Roles
- **ADMIN / POSGRADOS.**
- **COORDINADOR_PROGRAMA** (futuro): solo ve métricas de su programa.

---

### Bloque J — Auditoría

**Objetivo:** dejar rastro de quién hizo qué.

#### Modelo nuevo
```sql
CREATE TABLE auditoria (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  cedula_actor varchar NOT NULL,
  rol_actor varchar NOT NULL,
  accion varchar NOT NULL,              -- 'EDITAR_TIPO_CERTIFICADO', 'APROBAR_SOLICITUD', ...
  entidad_tipo varchar,                 -- 'TipoCertificado', 'Solicitud', ...
  entidad_id varchar,
  detalle jsonb,                        -- antes/después
  ip varchar,
  user_agent varchar,
  fecha timestamp NOT NULL DEFAULT now()
);
```

Implementar como `@Aspect` o filtro Spring que intercepta los endpoints admin y persiste automáticamente.

#### Endpoints (`/api/admin/auditoria`)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/?cedula=&accion=&desde=&hasta=` | Búsqueda en el log |
| `GET` | `/{id}` | Detalle de una entrada |
| `GET` | `/export?formato=csv` | Exporta para compliance |

#### UI
Sección "Auditoría":
- Tabla con filtros: actor, acción, rango de fechas.
- Vista de detalle con diff antes/después en JSON formateado.

#### Roles
- **ADMIN únicamente.**

---

### Bloque K — Configuración global del sistema

**Objetivo:** parámetros que hoy están como constantes en código.

#### Modelo nuevo
```sql
CREATE TABLE configuracion_sistema (
  clave varchar PRIMARY KEY,            -- 'certificados.dias_vigencia_pago', 'mantenimiento.activo', ...
  valor varchar NOT NULL,
  tipo varchar NOT NULL,                -- 'INT', 'STRING', 'BOOLEAN', 'JSON'
  descripcion text,
  editable boolean DEFAULT true
);
```

Carga al iniciar la app, hot-reload via endpoint de admin.

#### Endpoints (`/api/admin/configuracion`)

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/` | Lista todas las configs |
| `PUT` | `/{clave}` | Actualiza una |
| `POST` | `/reload` | Recarga la cache en memoria |

#### UI
Sección "Configuración global":
- Tabla agrupada por prefijo (`certificados.*`, `mantenimiento.*`, etc.).
- Edición inline con validación según tipo.

#### Configs iniciales sugeridas
- `certificados.dias_vigencia_pago` (default 3)
- `certificados.minutos_generacion_pdf` (default 5, para el mensaje del PRD)
- `mantenimiento.activo` (default false)
- `mantenimiento.mensaje` (string mostrado si está activo)
- `email.from` (override de spring.mail.username)
- `email.firma_institucional` (HTML)

#### Roles
- **ADMIN únicamente.**

---

## 4. Matriz de autorización propuesta

> **Estado actual:** ningún endpoint usa `@PreAuthorize`. La identidad del cliente se pasa como `@RequestParam String cedula` — patrón inseguro. Esta matriz **asume** que en una fase posterior se migra a autenticación basada en sesión o JWT.

Convención: ✅ = puede; ❌ = no puede; 🔒 = puede pero solo sobre su propio recurso (su programa, su dependencia, su cédula).

### 4.1 Endpoints existentes (clasificación de seguridad pendiente)

| Endpoint | ESTUDIANTE | DIRECTOR | DEPENDENCIA | POSGRADOS | ADMIN |
|---|---|---|---|---|---|
| `GET /api/tramites?cedula=` | 🔒 | 🔒 | 🔒 | 🔒 | ✅ |
| `GET /api/usuarios/me` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `POST /api/usuarios/login` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `POST /api/solicitudes/terminacion-materias` | ✅ | ❌ | ❌ | ❌ | ✅ |
| `POST /api/solicitudes/grado` | ✅ | ❌ | ❌ | ❌ | ✅ |
| `GET /api/solicitudes?cedula=` | 🔒 | ❌ | ❌ | ❌ | ✅ |
| `GET /api/solicitudes/bandeja?cedula=` | ❌ | 🔒 | ❌ | ❌ | ✅ |
| `POST /api/solicitudes/{id}/aprobar` | ❌ | 🔒 | ❌ | ❌ | ✅ |
| `POST /api/solicitudes/{id}/rechazar` | ❌ | 🔒 | ❌ | ❌ | ✅ |
| `GET /api/solicitudes/posgrados/bandeja` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `POST /api/solicitudes/{id}/validar-grado` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `POST /api/solicitudes/{id}/aprobar-posgrados` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `POST /api/solicitudes/{id}/rechazar-posgrados` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `GET /api/solicitudes/{id}/acta` | 🔒 | 🔒 | ❌ | ✅ | ✅ |
| `POST /api/solicitudes/{id}/pagar-grado` | 🔒 | ❌ | ❌ | ✅ | ✅ |
| `POST /api/solicitudes/{id}/fecha-grado` | 🔒 | ❌ | ❌ | ✅ | ✅ |
| `POST /api/certificados/solicitar` | ✅ | ❌ | ❌ | ❌ | ✅ |
| `GET /api/certificados?cedula=` | 🔒 | ❌ | 🔒 | ❌ | ✅ |
| `POST /api/certificados/{id}/pagar` | 🔒 | ❌ | ❌ | ❌ | ✅ |
| `GET /api/certificados/{id}/pdf` | 🔒 | ❌ | 🔒 | ❌ | ✅ |
| `GET /api/certificados/dependencia/{cedula}` | ❌ | ❌ | 🔒 | ✅ | ✅ |
| `POST /api/certificados/{id}/marcar-listo` | ❌ | ❌ | 🔒 | ❌ | ✅ |
| `POST /api/certificados/{id}/marcar-entregado` | ❌ | ❌ | 🔒 | ❌ | ✅ |
| `GET /api/certificados/tipos` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `GET /api/convocatorias/activa` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `PUT /api/convocatorias` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `GET /api/paz-y-salvo/mis-solicitudes?cedula=` | ❌ | 🔒 | 🔒 | ❌ | ✅ |
| `GET /api/paz-y-salvo/pendientes?cedula=` | ❌ | 🔒 | 🔒 | ❌ | ✅ |
| `POST /api/paz-y-salvo/{id}/responder` | ❌ | 🔒 | 🔒 | ❌ | ✅ |
| `GET /api/paz-y-salvo/estado-estudiantes?cedula=` | ❌ | 🔒 | ❌ | ❌ | ✅ |
| `GET /api/dependencias` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `GET /api/notificaciones/subscribe?cedula=` | 🔒 | 🔒 | 🔒 | 🔒 | ✅ |

### 4.2 Endpoints admin nuevos propuestos

| Endpoint | DIRECTOR | DEPENDENCIA | POSGRADOS | ADMIN |
|---|---|---|---|---|
| `/api/admin/usuarios/**` | ❌ | ❌ | ✅ (sin crear ADMIN) | ✅ |
| `/api/admin/programas/**` | ❌ | ❌ | ✅ | ✅ |
| `/api/admin/roles/**` | ❌ | ❌ | ❌ | ✅ |
| `/api/admin/tipos-solicitud/**` | ❌ | ❌ | ✅ (editar precios) | ✅ (crear/borrar) |
| `/api/admin/tipos-certificado/**` (existente) | ❌ | ❌ | ✅ | ✅ |
| `/api/admin/tipos-paz-y-salvo/**` | ❌ | ❌ | ✅ | ✅ |
| `/api/admin/documentos-requeridos/**` | ❌ | ❌ | ✅ | ✅ |
| `/api/admin/plantillas-correo/**` | ❌ | ❌ | ❌ | ✅ |
| `/api/admin/convocatorias/**` | ❌ | ❌ | ✅ | ✅ |
| `/api/admin/reportes/**` | ❌ | ❌ | ✅ | ✅ |
| `/api/admin/auditoria/**` | ❌ | ❌ | ❌ | ✅ |
| `/api/admin/configuracion/**` | ❌ | ❌ | ❌ | ✅ |

### 4.3 Cómo implementar el control de autorización (cuando se decida)

Tres caminos posibles, de menor a mayor esfuerzo:

1. **Anotación `@PreAuthorize` por endpoint** (Spring Security):
   ```java
   @PreAuthorize("hasAnyRole('ADMIN','POSGRADOS')")
   @PostMapping("/admin/usuarios")
   public ResponseEntity<?> crear(...) { ... }
   ```
   Requiere configurar `SecurityFilterChain` y reemplazar el patrón `@RequestParam String cedula` por `Authentication principal`.

2. **Filtro Spring + lectura de sesión**:
   - Activar el login real (descomentar `UsuarioController.login`).
   - Crear `AuthFilter` que valida `HttpSession` y pone el `Usuario` actual en un `ThreadLocal`.
   - Cada controller chequea `AuthContext.getUsuario().getRol()` antes de actuar.

3. **JWT + Spring Security** (más limpio para producción):
   - Login devuelve token JWT.
   - Frontend lo guarda y lo manda en header `Authorization: Bearer ...`.
   - Filtro JWT extrae cédula+rol del token y los pone en el `SecurityContext`.

**Recomendación:** opción 2 para este sprint (cambio mínimo respecto al estado actual), opción 3 cuando se vaya a desplegar a producción real.

---

## 5. Orden sugerido de implementación

Priorizado por **valor inmediato** vs **complejidad**:

### Sprint 1 (mínimo viable de admin)
1. **Bloque A — Usuarios:** CRUD básico sin importar CSV. Es lo que más duele hoy.
2. **Bloque B — Programas académicos:** simple, alto impacto, base para todo lo demás.
3. **Bloque D — Tipos de trámite:** habilitar configuración de precios para Terminación y Grado.

### Sprint 2 (consolidación funcional)
4. **Bloque E — Tipos de paz y salvo:** desligar del código.
5. **Bloque F — Documentos requeridos:** activar la tabla muerta.
6. **Bloque C — Roles (Opción 1):** unificar ADMIN/POSGRADOS y abrir el catálogo.

### Sprint 3 (configurabilidad avanzada)
7. **Bloque G — Plantillas de correo:** quitar el copy del código Java.
8. **Bloque H — Convocatorias múltiples:** soportar histórico y por programa.
9. **Bloque K — Configuración global:** los parámetros sueltos.

### Sprint 4 (visibilidad y seguridad)
10. **Bloque I — Reportes y dashboard.**
11. **Bloque J — Auditoría.**
12. **Migración a Spring Security real + login funcional** (precondición de §4.3 opción 2 o 3).

---

## 6. Riesgos y precauciones

- **Compatibilidad hacia atrás:** muchos de estos cambios tocan tablas que ya tienen datos (`usuario`, `programa_academico`, `solicitud`). Las migraciones deben usar `ON CONFLICT DO NOTHING` o `NOT VALID` en las FKs (ver `plan_certificados.md` §4.1 para el patrón).
- **El antipatrón `@RequestParam String cedula`:** mientras se mantenga, **cualquier endpoint admin nuevo es vulnerable**. Una persona maliciosa puede llamar `PUT /api/admin/tipos-solicitud/1` pasando cualquier cedula. **No es aceptable para producción.** Antes de exponer admin a internet, hay que cerrar esa puerta.
- **Credenciales SMTP en `application.properties`:** ya están en el repo público. Cuando se implemente el Bloque G, considerar rotar la contraseña de app de Gmail y moverla a variable de entorno (ver `plan_certificados.md` deuda transversal).
- **Hibernate `ddl-auto=update`:** suma columnas pero no las dropea ni añade FKs. Cada bloque debe documentar su migración manual.

---

## 7. Decisiones que deben tomarse con el equipo antes de codear

1. **Unificación de roles ADMIN y POSGRADOS** — ¿uno solo o se mantienen separados con permisos distintos?
2. **Rol "COORDINADOR_PROGRAMA"** — ¿se necesita un rol acotado a un programa específico, o se puede modelar con el rol DIRECTOR existente?
3. **Pricing por dependencia** — ¿cada dependencia configura sus propios precios o solo Posgrados centraliza?
4. **Autenticación real** — ¿se mantiene `loginDemo` para el sprint o se prioriza implementar el login con contraseña ya?
5. **Mecanismo de roles/permisos** — Opción 1 (string + tabla catálogo) o Opción 2 (matriz granular). Esta decisión condiciona el Bloque C.
6. **Multi-tenancy de convocatorias** — ¿una global o múltiples por programa/trámite?

---

## 8. Resumen ejecutivo

| Categoría | Tablas nuevas | Endpoints nuevos | Secciones nuevas en UI admin |
|---|---|---|---|
| Usuarios y roles | `rol` | ~12 | 3 (Usuarios, Roles, Programas) |
| Configuración de trámites | `tipo_solicitud`, `tipo_paz_y_salvo` | ~14 | 3 (Tipos trámite, Paz y salvos, Documentos) |
| Comunicación | `plantilla_correo` | ~5 | 1 |
| Convocatorias | (extensión) | ~5 | 1 |
| Reportería | — | ~7 | 1 + dashboard |
| Seguridad y trazabilidad | `auditoria`, `configuracion_sistema` | ~6 | 2 |
| **Total estimado** | **6 tablas nuevas + 4 extendidas** | **~49 endpoints** | **11 secciones** |

Esto representa al menos **3 sprints de trabajo serio**. La página `ConfiguracionAdmin.jsx` debería migrar de una sola página plana a una estructura con sub-navegación (tabs laterales o breadcrumb tipo `Configuración / Usuarios / Editar Juan Pérez`).
