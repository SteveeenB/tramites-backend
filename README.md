[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/SteveeenB/tramites-backend)
# tramites-backend

Documentación técnica del backend del sistema **GROWEB — TposgradosUFPS**.  
API REST desarrollada con Java + Spring Boot para la gestión de trámites académicos de posgrado de la Universidad Francisco de Paula Santander (UFPS).

**Equipo:** Raúl David Báez Suárez · Johan Steven Bueno Rojas · Kevin David Arias Villamizar · Santiago Danilo Cepeda Galeano · Diego Alexander Bermúdez Flores · Bryan Alexander Niño López  
**Asignatura:** Análisis y Diseño de Sistemas — Junio 2026

---

## Tabla de Contenidos

1. [Visión General](#1-visión-general)
2. [Arquitectura y Estructura de Paquetes](#2-arquitectura-y-estructura-de-paquetes)
3. [Seguridad — JWT y OAuth](#3-seguridad--jwt-y-oauth)
4. [Modelo de Datos](#4-modelo-de-datos)
5. [Endpoints API REST](#5-endpoints-api-rest)
6. [Reglas de Negocio](#6-reglas-de-negocio)
7. [Instalación y Ejecución Local](#7-instalación-y-ejecución-local)
8. [Pruebas](#8-pruebas)

---

## 1. Visión General

El backend gestiona los trámites académicos de posgrado de la UFPS: **terminación de materias**, **solicitud de grado**, **expedición de certificados académicos**, **gestión de paz y salvos**, **pagos vía Wompi** y **notificaciones automáticas**. Expone sus servicios a través de HTTP/JSON con autenticación JWT y utiliza Supabase (PostgreSQL + Storage) como capa de persistencia.

### Stack Tecnológico

| Componente | Tecnología | Detalle |
|---|---|---|
| Framework | Java 17 + Spring Boot 4.x | Desplegado en Render (prod) |
| Seguridad | Spring Security + JJWT | JWT Stateless, BCrypt, Google OAuth |
| Base de Datos | PostgreSQL (Supabase) | ORM: Hibernate / JPA — `ddl-auto: update` |
| Almacenamiento | Supabase Storage | Bucket: `tramites-documentos` (privado) |
| Generación PDF | iText 7 (v7.2.5) | Actas de grado y certificados en PDF |
| Códigos QR | ZXing (v3.5.2) | QR embebido en certificados para validación |
| Pasarela de Pagos | Wompi | Checkout, webhook y consulta de estado |
| Documentación API | Swagger / OpenAPI (springdoc 2.8.8) | Ruta: `/swagger-ui.html` |
| Notificaciones RT | Server-Sent Events (SSE) | `/api/notificaciones/subscribe` |
| Email | Gmail SMTP / Spring Mail | Notificaciones automáticas por correo |
| Contenedor | Docker | `Dockerfile` incluido en la raíz |

### Variables de Entorno Requeridas

| Variable | Descripción |
|---|---|
| `DB_URL` | URL JDBC de PostgreSQL en Supabase |
| `DB_USERNAME` | Usuario de la base de datos |
| `DB_PASSWORD` | Contraseña de la base de datos |
| `JWT_SECRET` | Clave secreta para firmar tokens JWT (mín. 32 chars) |
| `JWT_EXPIRATION_MS` | Duración del token en ms (ej. `86400000` = 24h) |
| `SUPABASE_URL` | URL base del proyecto Supabase |
| `SUPABASE_SERVICE_ROLE_KEY` | Clave para acceso a Storage privado |
| `SUPABASE_BUCKET` | Nombre del bucket (`tramites-documentos`) |
| `CORS_ORIGIN` | Origen permitido en CORS (`http://localhost:3000`) |
| `PORT` | Puerto del servidor HTTP (`8080`) |
| `GOOGLE_CLIENT_ID` | Client ID de Google OAuth (opcional) |
| `DEMO_AUTH_ENABLED` | `true` en dev para activar `/auth/login-demo` |
| `WOMPI_PUBLIC_KEY` | Llave pública de Wompi |
| `WOMPI_PRIVATE_KEY` | Llave privada de Wompi |
| `WOMPI_EVENTS_SECRET` | Secreto para verificar webhooks de Wompi |

---

## 2. Arquitectura y Estructura de Paquetes

```
com.ufps.tramites
├── TramitesApplication.java
├── config/
│   ├── CorsConfig.java
│   ├── SecurityConfig.java             JWT Stateless + rutas públicas
│   └── SwaggerConfig.java
├── controller/
│   ├── AdminTipoCertificadoController.java   /api/admin/tipos-certificado
│   ├── AuthController.java                   /api/auth/**
│   ├── CertificadoController.java            /api/certificados/**
│   ├── ConvocatoriaController.java           /api/convocatorias/**
│   ├── DependenciaController.java            /api/dependencias
│   ├── NotificacionController.java           /api/notificaciones/**
│   ├── PazYSalvoController.java              /api/paz-y-salvo/**
│   ├── SolicitudController.java              /api/solicitudes/**
│   ├── TramiteController.java                /api/tramites/**
│   ├── UsuarioController.java                /api/usuarios/**
│   └── WompiController.java                  /api/pagos/**
├── dto/
│   ├── LoginRequestDTO.java
│   ├── LoginResponseDTO.java
│   ├── NotificacionDTO.java
│   ├── SolicitudGradoDetalleDTO.java
│   ├── SolicitudGradoListDTO.java
│   └── UsuarioResponseDTO.java
├── event/
│   └── SolicitudEstadoCambiadoEvent.java
├── model/
│   ├── Admin.java                      Operadores administrativos (POSGRADOS, DEPENDENCIA, SUPER)
│   ├── Convocatoria.java
│   ├── DecisionDirector.java
│   ├── Dependencia.java
│   ├── DocumentoSolicitud.java
│   ├── EstadoEstudiante.java
│   ├── Estudiante.java                 Perfil académico del usuario ESTUDIANTE
│   ├── Notificacion.java               Notificaciones in-app persistidas
│   ├── Pago.java                       Registro de pagos Wompi
│   ├── PazYSalvo.java
│   ├── ProgramaAcademico.java
│   ├── Rol.java
│   ├── Solicitud.java
│   ├── SolicitudCertificado.java
│   ├── TipoCertificado.java
│   └── Usuario.java                    Usuarios académicos (ESTUDIANTE, DIRECTOR)
├── repository/
│   ├── AdminRepository.java
│   ├── ConvocatoriaRepository.java
│   ├── DependenciaRepository.java
│   ├── DocumentoSolicitudRepository.java
│   ├── EstadoEstudianteRepository.java
│   ├── EstudianteRepository.java
│   ├── NotificacionRepository.java
│   ├── PagoRepository.java
│   ├── PazYSalvoRepository.java
│   ├── RolRepository.java
│   ├── SolicitudCertificadoRepository.java
│   ├── SolicitudRepository.java
│   ├── TipoCertificadoRepository.java
│   └── UsuarioRepository.java
├── security/
│   ├── JwtAuthFilter.java              Intercepta cada request y valida el Bearer token
│   ├── JwtService.java                 Genera y valida JWTs para Usuario y Admin
│   ├── PrincipalResolver.java          Resuelve el principal desde el SecurityContext
│   └── ResolvedPrincipal.java          DTO del principal autenticado
└── service/
    ├── ActaPdfGeneratorService.java
    ├── AdminService.java
    ├── AlertaDirectorService.java
    ├── CertificadoConstanciaPdfService.java
    ├── CertificadoPdfService.java
    ├── CertificadoService.java
    ├── ConvocatoriaService.java
    ├── CorreoCertificadoService.java
    ├── CorreoConstanciaService.java
    ├── DecisionSolicitudService.java
    ├── DependenciaService.java
    ├── DocumentoService.java
    ├── NotificacionEmailService.java
    ├── NotificacionService.java
    ├── NotificacionSseService.java
    ├── PazYSalvoService.java
    ├── SolicitudService.java
    ├── SupabaseStorageService.java
    ├── TramiteService.java
    ├── UsuarioService.java
    ├── ValidacionGradoService.java
    └── WompiService.java
```

---

## 3. Seguridad — JWT y OAuth

### Arquitectura de Seguridad (Sprint 4 — TP-137)

El sistema migró de sesiones HTTP a autenticación **JWT Stateless** con Spring Security. Cada petición debe incluir el header `Authorization: Bearer <token>`.

### Rutas Públicas (no requieren JWT)

| Ruta | Descripción |
|---|---|
| `POST /api/auth/**` | Login manual, Google OAuth, login-demo |
| `GET /api/solicitudes/verificar` | Verificación pública de certificados por código QR |
| `POST /api/pagos/webhook` | Webhook de Wompi (llamado por Wompi directamente) |
| `GET /api/notificaciones/subscribe` | Stream SSE (autenticación por parámetro) |
| `GET /swagger-ui/**` | Documentación Swagger |

### Roles del Sistema

| Rol | Tabla | Descripción |
|---|---|---|
| `ESTUDIANTE` | `usuario` | Realiza solicitudes y trámites académicos |
| `DIRECTOR` | `usuario` | Revisa y aprueba solicitudes de su programa |
| `POSGRADOS` | `admins` | Perfil operativo: atiende solicitudes y consulta métricas |
| `ADMIN` | `admins` | Perfil configurador: gestiona catálogos y configuración global |
| `DEPENDENCIA` | `admins` | Gestiona paz y salvos y entrega de certificados físicos |

### `JwtService` — Claims del Token

| Claim | Descripción |
|---|---|
| `sub` | ID del usuario/admin |
| `principalType` | `USUARIO` (académico) o `ADMIN` (operativo) |
| `rol` | `ESTUDIANTE` \| `DIRECTOR` \| `POSGRADOS` \| `ADMIN` \| `DEPENDENCIA` |
| `cedula` | Solo usuarios académicos |
| `codigo` | Código institucional |
| `nombreCompleto` | Nombre del usuario |
| `email` | Correo electrónico |
| `estudianteId` | ID del perfil estudiante (solo rol ESTUDIANTE) |
| `programaNombre` | Nombre del programa académico |
| `dependenciaId` | ID de la dependencia (solo admins de tipo DEPENDENCIA) |
| `esSuperAdmin` | `true` si el admin es SUPER/ADMIN |

### Endpoints de Autenticación — `/api/auth`

| Método | Ruta | Body / Params | Descripción | Respuesta |
|---|---|---|---|---|
| `POST` | `/login` | `{codigo, contrasena}` | Login con código institucional y contraseña. Busca primero en `admins`, luego en `usuario`. | `200 LoginResponseDTO` \| `401` |
| `POST` | `/google` | `{idToken}` | Login con token de Google Identity Services. Crea el usuario si no existe (rol ESTUDIANTE). | `200 LoginResponseDTO` \| `401` \| `503` |
| `POST` | `/login-demo` | `?cedula=` (query) | Solo disponible con `demo.auth.enabled=true`. Emite JWT sin validar contraseña. | `200 LoginResponseDTO` \| `404` |

**`LoginResponseDTO` — campos clave:**

| Campo | Descripción |
|---|---|
| `token` | JWT Bearer para incluir en el header `Authorization` |
| `rol` | Rol del usuario autenticado |
| `cedula` | Cédula (usuarios académicos) |
| `codigo` | Código institucional |
| `nombreCompleto` | Nombre completo |
| `programaNombre` | Programa académico (solo estudiante/director) |
| `dependenciaNombre` | Nombre de la dependencia (solo admins DEPENDENCIA) |
| `esSuperAdmin` | `true` para rol ADMIN |

---

## 4. Modelo de Datos

### Usuario _(tabla: usuario)_ — Usuarios académicos

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` (PK) | Identificador autoincremental |
| `cedula` | `String` | Cédula de ciudadanía |
| `codigo` | `String` | Código institucional UFPS |
| `nombreCompleto` | `String` | Nombre completo |
| `email` | `String` | Correo electrónico |
| `password` | `String` | Contraseña cifrada con BCrypt |
| `googleId` | `String` | ID de Google OAuth (opcional) |
| `fotoUrl` | `String` | URL de foto de perfil (Google) |
| `rol` | `Rol` (FK) | Rol asignado: `ESTUDIANTE` \| `DIRECTOR` |
| `programaAcademico` | `ProgramaAcademico` (FK) | Programa al que pertenece |
| `estadoGrado` | `String` | `null` \| `GRADUADO` |

### Admin _(tabla: admins)_ — Operadores administrativos

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` (PK) | Identificador autoincremental |
| `codigo` | `String` | Código institucional (login) |
| `nombreCompleto` | `String` | Nombre completo |
| `email` | `String` | Correo electrónico |
| `password` | `String` | Contraseña cifrada con BCrypt |
| `rolNombre` | `String` | `POSGRADOS` \| `ADMIN` \| `DEPENDENCIA` |
| `dependenciaId` | `Long` | FK a dependencia (solo DEPENDENCIA) |
| `dependenciaNombre` | `String` | Nombre descriptivo de la dependencia |
| `esSuperAdmin` | `Boolean` | `true` para rol ADMIN |

### Solicitud _(tabla: solicitud)_

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` (PK) | Identificador autoincremental |
| `cedula` | `String` | Cédula del estudiante solicitante |
| `tipo` | `String` | `TERMINACION_MATERIAS` \| `GRADO` |
| `estado` | `String` | `PENDIENTE_PAGO` \| `EN_REVISION` \| `APROBADA` \| `RECHAZADA` \| `PENDIENTE_VALIDACION` \| `APROBADA_POSGRADOS` \| `RECHAZADA_POSGRADOS` |
| `fechaSolicitud` | `LocalDate` | Fecha de creación |
| `costo` | `Double` | Monto liquidado en COP |
| `observaciones` | `String` | Notas del estudiante |
| `decision` | `String` | `APROBADA` \| `RECHAZADA` (Director) |
| `observacionesDirector` | `String` | Motivo del Director |
| `fechaDecision` | `LocalDateTime` | Timestamp de la decisión |
| `cedulaDirector` | `String` | Cédula del Director que decidió |
| `fechaEnRevision` | `LocalDateTime` | Inicio del plazo de revisión |
| `tituloProyecto` | `String` | Solo tipo `GRADO` |
| `tipoProyecto` | `String` | `INVESTIGACION` \| `MONOGRAFIA` \| `SISTEMATIZACION` \| `TRABAJO_DIRIGIDO` \| `PASANTIA` |
| `resumenProyecto` | `String` | Resumen del proyecto de grado |
| `validacionPosgrados` | `String` | `APROBADA` \| `RECHAZADA` |
| `observacionesPosgrados` | `String` | Motivo de la validación de posgrados |
| `fechaValidacion` | `LocalDateTime` | Timestamp de la validación |
| `cedulaPosgrados` | `String` | Cédula/código del Admin que validó |

### Pago _(tabla: pagos)_ — Sprint 4

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` (PK) | Identificador autoincremental |
| `referencia` | `String` (UNIQUE) | Referencia única enviada a Wompi |
| `solicitudId` | `Long` | FK a `solicitud.id` |
| `cedulaEstudiante` | `String` | Cédula del pagador |
| `tipoPago` | `String` | `TERMINACION` \| `GRADO` \| `MODALIDAD_CEREMONIA` |
| `montoCentavos` | `Long` | Monto en centavos (COP × 100) |
| `estado` | `String` | `PENDIENTE` \| `APROBADO` \| `RECHAZADO` \| `ANULADO` \| `ERROR` |
| `wompiTransactionId` | `String` | ID de transacción en Wompi |
| `redirectUrl` | `String` | URL de retorno tras el pago |
| `fechaCreacion` | `LocalDateTime` | Timestamp de creación |
| `fechaActualizacion` | `LocalDateTime` | Timestamp de última actualización |

### Notificacion _(tabla: notificaciones)_ — Sprint 4

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` (PK) | Identificador autoincremental |
| `destinatarioCedula` | `String` | Cédula (ESTUDIANTE/DIRECTOR) o código (ADMIN/DEPENDENCIA) |
| `tipo` | `String` | `ESTADO_CAMBIADO` \| `PLAZO_VENCIDO` \| `GENERAL` |
| `titulo` | `String` | Título de la notificación |
| `mensaje` | `String (TEXT)` | Cuerpo del mensaje |
| `enlace` | `String` | Ruta del frontend para navegar al hacer clic |
| `leida` | `Boolean` | `false` por defecto |
| `fechaCreacion` | `LocalDateTime` | Timestamp de creación |

### SolicitudCertificado _(tabla: solicitud_certificado)_

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` (PK) | Identificador autoincremental |
| `cedula` | `String` | Cédula del solicitante |
| `tipoCertificado` | `TipoCertificado` (FK) | Tipo de certificado solicitado |
| `estado` | `String` | `PENDIENTE_PAGO` \| `PAGADO` \| `GENERADO` \| `LISTO_RETIRO` \| `ENTREGADO` |
| `modalidad` | `String` | `DIGITAL` \| `FISICO` |
| `destinatario` | `String` | Para quién va dirigido (opcional) |
| `costo` | `Double` | Monto liquidado en COP |
| `fechaSolicitud` | `LocalDateTime` | Timestamp de creación |
| `fechaPago` | `LocalDateTime` | Timestamp del pago |
| `fechaGeneracion` | `LocalDateTime` | Timestamp de generación del PDF |
| `radicado` | `String` | Número de radicado único institucional |
| `urlPdf` | `String` | URL del PDF en Supabase Storage |
| `cedulaDependencia` | `String` | Dependencia responsable de entrega |

### PazYSalvo _(tabla: paz_y_salvo)_

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` (PK) | Identificador autoincremental |
| `solicitudId` | `Long` | FK a `solicitud.id` |
| `cedulaResponsable` | `String` | Cédula/código del responsable |
| `nombreResponsable` | `String` | Nombre descriptivo de la dependencia |
| `estado` | `String` | `PENDIENTE` \| `APROBADO` \| `RECHAZADO` |
| `observaciones` | `String` | Observaciones al responder |
| `fechaRespuesta` | `LocalDateTime` | Timestamp de la respuesta |

---

## 5. Endpoints API REST

URL base local: `http://localhost:8080/api`
URL en producción (frontend): `https://tramites-frontend-r08z.onrender.com`
Swagger UI: `http://localhost:8080/swagger-ui.html`  
**Autenticación:** `Authorization: Bearer <token>` en todas las rutas protegidas.

### 5.1 Autenticación — `/api/auth`

| Método | Ruta | Descripción | Auth |
|---|---|---|---|
| `POST` | `/login` | Login con código + contraseña | Pública |
| `POST` | `/google` | Login con token de Google OAuth | Pública |
| `POST` | `/login-demo` | Login demo sin contraseña (solo dev) | Pública |

### 5.2 Trámites — `/api/tramites`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/` | Módulo de trámites disponibles según rol |
| `GET` | `/proceso-grado` | Estado, créditos y etapas del proceso de grado |

### 5.3 Solicitudes — `/api/solicitudes`

| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| `POST` | `/terminacion-materias` | Crea solicitud de terminación | ESTUDIANTE |
| `POST` | `/grado` | Crea solicitud de grado con documentos (multipart) | ESTUDIANTE |
| `GET` | `/` | Lista solicitudes propias | ESTUDIANTE |
| `GET` | `/bandeja` | Bandeja de terminación del programa | DIRECTOR |
| `GET` | `/bandeja-grado` | Bandeja de grado del programa | DIRECTOR |
| `POST` | `/{id}/aprobar` | Aprueba la solicitud | DIRECTOR |
| `POST` | `/{id}/rechazar` | Rechaza la solicitud | DIRECTOR |
| `GET` | `/{id}/acta` | Genera/descarga acta de grado en PDF | ESTUDIANTE |
| `GET` | `/{id}/certificado` | Descarga certificado de terminación | ESTUDIANTE |
| `POST` | `/{id}/documentos` | Sube documento a Supabase Storage | ESTUDIANTE |
| `GET` | `/{id}/documentos` | Lista documentos de la solicitud | Autenticado |
| `GET` | `/{id}/documentos/{docId}/file` | Descarga archivo desde Storage | Autenticado |
| `GET` | `/posgrados/pendientes` | Solicitudes de grado pendientes de validación | ADMIN/POSGRADOS |
| `POST` | `/{id}/validar-grado` | Validación de la Unidad de Posgrados | ADMIN/POSGRADOS |
| `GET` | `/verificar` | Verificación pública de certificado por código | Pública |

### 5.4 Pagos — `/api/pagos` (Sprint 4 — TP-37 a TP-40, TP-76)

| Método | Ruta | Body | Descripción | Auth |
|---|---|---|---|---|
| `POST` | `/crear` | `{solicitudId, tipoPago, cedula}` | Crea el pago en Wompi y retorna la URL de checkout | JWT |
| `GET` | `/estado/{referencia}` | — | Consulta el estado de un pago por referencia | JWT |
| `POST` | `/webhook` | Evento Wompi | Recibe eventos de Wompi y actualiza el estado del pago | Pública |

**Flujo de pago:**
1. El frontend llama `POST /pagos/crear` → recibe la URL de checkout de Wompi
2. El usuario completa el pago en Wompi → Wompi redirige al frontend con `?reference=...&status=...`
3. El frontend llama `GET /pagos/estado/{referencia}` para confirmar
4. Wompi llama `POST /pagos/webhook` para notificar el estado final

### 5.5 Certificados — `/api/certificados`

| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| `GET` | `/tipos` | Lista tipos de certificado activos | Público |
| `POST` | `/solicitar` | Crea solicitud de certificado | ESTUDIANTE |
| `GET` | `/` | Lista certificados del estudiante | ESTUDIANTE |
| `POST` | `/{id}/pagar` | Simula pago y genera PDF con QR | ESTUDIANTE |
| `GET` | `/{id}/pdf` | Descarga PDF del certificado | ESTUDIANTE / DEPENDENCIA |
| `GET` | `/dependencia/{cedulaDependencia}` | Bandeja de certificados de la dependencia | DEPENDENCIA |
| `POST` | `/{id}/marcar-listo` | Marca como listo para retiro | DEPENDENCIA |
| `POST` | `/{id}/marcar-entregado` | Confirma entrega física | DEPENDENCIA |

#### HU-12 — `GET /api/certificados/tipos` (TP-88)

Retorna el catálogo de tipos activos. No requiere autenticación.

**Respuesta `200 OK`:**
```json
[
  {
    "id": 9,
    "codigo": "CONSTANCIA_BUENA_CONDUCTA",
    "label": "CONSTANCIA BUENA CONDUCTA POSGRADO",
    "precioDigital": 12000,
    "costoLogisticaFisica": 2000,
    "dependenciaCedula": "3000000001",
    "direccionOficina": "Biblioteca Cote Lamus",
    "tiempoEntregaDias": 1,
    "activo": true
  }
]
```

**Reglas:** Solo `activo = true`. Para modalidad `FISICA` el costo total es `precioDigital + costoLogisticaFisica`.

#### HU-14 — `GET /api/certificados` (TP-104)

Retorna el historial completo de certificados del estudiante en todos los estados.

**Campos clave de respuesta:**

| Campo | Descripción |
|---|---|
| `estado` | `PENDIENTE_PAGO` \| `PAGADO` \| `GENERADO` \| `LISTO_RETIRO` \| `ENTREGADO` \| `VENCIDA` |
| `urlPdf` | Ruta en Supabase Storage (disponible cuando `estado = GENERADO` o posterior) |
| `hashPdf` | Hash SHA-256 del PDF — permite verificar integridad |
| `radicado` | Formato: `UFPS-CERT-{id}-{últimos4CédulaEstudiante}` |
| `fechaVencimientoPago` | 3 días hábiles desde la solicitud |

### 5.6 Paz y Salvos — `/api/paz-y-salvo`

| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| `GET` | `/mis-solicitudes` | Todos los paz y salvos del responsable | DEPENDENCIA / DIRECTOR |
| `GET` | `/pendientes` | Solo los pendientes del responsable | DEPENDENCIA / DIRECTOR |
| `POST` | `/{id}/responder` | Responder con `APROBADO` o `RECHAZADO` | DEPENDENCIA / DIRECTOR |
| `GET` | `/solicitud/{solicitudId}` | Estado de todos los paz y salvos de una solicitud | Autenticado |
| `GET` | `/estado-estudiantes` | Vista de etapas de estudiantes del programa | DIRECTOR |

### 5.7 Notificaciones — `/api/notificaciones` (Sprint 4 — TP-131 a TP-134)

| Método | Ruta | Descripción | Auth |
|---|---|---|---|
| `GET` | `/subscribe` | Stream SSE. Emite `notificacion-nueva` y `estado-actualizado` | Pública (por query param) |
| `GET` | `/mias` | Lista todas las notificaciones del usuario autenticado | JWT |
| `GET` | `/no-leidas/count` | Conteo de notificaciones no leídas | JWT |
| `PUT` | `/{id}/leer` | Marca una notificación como leída | JWT |

**Eventos SSE:**

| Evento | Payload | Descripción |
|---|---|---|
| `notificacion-nueva` | `{id, tipo, titulo, mensaje, enlace, leida, fechaCreacion}` | Nueva notificación in-app |
| `estado-actualizado` | `{solicitudId, tipo, estadoAnterior, estadoNuevo, observaciones}` | Cambio de estado de una solicitud |
| `conectado` | `{message}` | Confirmación de conexión SSE |

### 5.8 Dependencias — `/api/dependencias`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/` | Lista todas las dependencias del sistema |

### 5.9 Admin — Tipos de Certificado — `/api/admin/tipos-certificado`

| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| `GET` | `/` | Lista todos (activos e inactivos) | ADMIN |
| `POST` | `/` | Crea nuevo tipo | ADMIN |
| `PUT` | `/{id}` | Actualiza tipo existente | ADMIN |
| `DELETE` | `/{id}` | Desactiva (baja lógica) | ADMIN |

### 5.10 Convocatorias — `/api/convocatorias`

| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| `GET` | `/activa` | Retorna la convocatoria activa | Público |
| `PUT` | `/` | Actualiza fechas del período habilitado | ADMIN/POSGRADOS |

---

## 6. Reglas de Negocio

### 6.1 Flujo de Estados

```
TERMINACION_MATERIAS:
  PENDIENTE_PAGO → EN_REVISION → APROBADA
                              → RECHAZADA

GRADO:
  PENDIENTE_PAGO → EN_REVISION → PENDIENTE_VALIDACION → APROBADA_POSGRADOS
                                                       → RECHAZADA_POSGRADOS

CERTIFICADO:
  PENDIENTE_PAGO → PAGADO → GENERADO → LISTO_RETIRO → ENTREGADO

PAGO (Wompi):
  PENDIENTE → APROBADO | RECHAZADO | ANULADO | ERROR
```

### 6.2 Terminación de Materias

| Regla | Validación |
|---|---|
| Créditos suficientes | `creditosAprobados >= programa.totalCreditos` |
| Convocatoria vigente | Fecha actual dentro del rango de la convocatoria activa |
| Sin duplicado activo | No existe solicitud activa del mismo tipo para la cédula |
| Costo fijo | `150.000 COP` — liquidación automática al crear |

### 6.3 Solicitud de Grado

| Regla | Validación |
|---|---|
| Terminación previa aprobada | Existe solicitud tipo `TERMINACION_MATERIAS` con estado `APROBADA` |
| Sin solicitud de grado activa | No existe solicitud tipo `GRADO` activa para la cédula |
| Documentos obligatorios | `foto` + `actaSustentacion` no nulos |
| Costo fijo | `250.000 COP` — liquidación automática |

### 6.4 Pagos Wompi

| Regla | Descripción |
|---|---|
| Referencia única | Formato: `UFPS-{tipo}-{solicitudId}-{timestamp}` |
| Monto en centavos | El monto se envía a Wompi multiplicado por 100 |
| Webhook sin JWT | `/api/pagos/webhook` está en la whitelist pública de Spring Security |
| Avance bloqueado | El trámite no avanza al estado `EN_REVISION` sin pago `APROBADO` |

### 6.5 Paz y Salvos

| Regla | Descripción |
|---|---|
| Generación automática | Al aprobar la solicitud de grado, se crean registros para cada dependencia y el Director |
| Bloqueo de avance | El flujo no avanza a `PENDIENTE_VALIDACION` hasta que todos los paz y salvos estén `APROBADO` |

### 6.6 Certificados

| Regla | Descripción |
|---|---|
| Filtro por estado | Solo tipos habilitados para el estado académico del solicitante |
| Generación automática | Al confirmar el pago: PDF con firma digital, radicado único y QR |
| Radicado | Formato: `UFPS-CERT-{id}-{últimos4Cédula}` |
| Correo automático | Email con PDF adjunto al confirmar el pago (modalidad digital) |

### 6.7 Notificaciones Automáticas (Sprint 4 — TP-132)

| Evento | Destinatario | Canal |
|---|---|---|
| Cambio de estado de solicitud | Estudiante | SSE + in-app + correo |
| Plazo del Director vencido | Director | in-app + correo |
| Paz y salvo recibido | Dependencia / Director | in-app |
| Certificado generado | Estudiante | in-app + correo con PDF |

### 6.8 Roles y Permisos

| Rol | Acciones |
|---|---|
| `ESTUDIANTE` | Crear solicitudes, pagar, subir documentos, descargar actas/certificados, recibir notificaciones SSE |
| `DIRECTOR` | Bandeja de terminación y grado, aprobar/rechazar, responder paz y salvos, ver estado estudiantes |
| `POSGRADOS` | Ver pendientes de validación, validar solicitudes de grado, consultar reportes |
| `ADMIN` | Todo lo de POSGRADOS + gestionar catálogos, dependencias, convocatorias, usuarios y configuración global |
| `DEPENDENCIA` | Responder paz y salvos, gestionar bandeja de certificados físicos |

---

## 7. Instalación y Ejecución Local

### Requisitos Previos

- Java 17 o superior
- Maven 3.8+ (o usar `mvnw`)
- Cuenta y proyecto en Supabase (PostgreSQL + Storage)

### Clonar y Configurar

```bash
git clone https://github.com/SteveeenB/tramites-backend.git
cd tramites-backend
```

Crear `application-local.properties` (no subir a Git):

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
jwt.expiration-ms=86400000
supabase.url=${SUPABASE_URL}
supabase.service-role-key=${SUPABASE_SERVICE_ROLE_KEY}
supabase.bucket=tramites-documentos
cors.origin=http://localhost:3000
demo.auth.enabled=true
wompi.public-key=${WOMPI_PUBLIC_KEY}
wompi.private-key=${WOMPI_PRIVATE_KEY}
wompi.events-secret=${WOMPI_EVENTS_SECRET}
```

### Ejecutar

```bash
./mvnw spring-boot:run
```

- Servidor: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Docker

```bash
docker build -t tramites-backend .
docker run -p 8080:8080 --env-file .env tramites-backend
```

### Actualizar desde el Remoto

```bash
git stash
git pull origin main
git stash pop
```

---

## 8. Pruebas

### Clases de Prueba

| Clase | Capa | Sprint | Cobertura |
|---|---|---|---|
| `TramitesApplicationTests` | Integración | S1 | Carga del contexto Spring Boot |
| `SolicitudRepositoryTest` | Repository | S1 | Consultas JPA sobre `Solicitud` |
| `AlertaDirectorServiceTest` | Service | S2 | Alertas por plazo vencido del Director (TP-44) |
| `DecisionSolicitudServiceTest` | Service | S2 | Flujo completo de aprobación y rechazo (HU-04) |
| `CertificadoEndpointTest` | Service | S3 | Catálogo de tipos, validación de tipo/modalidad, duplicados y precios — HU-12 (TP-87) |
| `CertificadoQRValidationTest` | Service | S3 | Radicado único, QR con ZXing, hash SHA-256, reglas de pago — HU-13 (TP-97) |

### Ejecutar

```bash
./mvnw test
```

Reporte HTML:

```bash
./mvnw surefire-report:report
```

### Pendientes de Cobertura

| Servicio / Flujo | Notas |
|---|---|
| `WompiService` | Requiere mock del cliente HTTP de Wompi |
| `NotificacionService` / `NotificacionSseService` | Mocking de SSE y jobs programados |
| `PazYSalvoService` | Mocking de dependencias externas |
| `ValidacionGradoService` | Validación de posgrados HU-09 |
| `JwtService` | Tests de generación y validación de tokens (TP-137) |
