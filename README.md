[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/SteveeenB/tramites-backend)
# tramites-backend

Documentación técnica del backend del sistema **GROWEB — TposgradosUFPS**.  
API REST desarrollada con Java + Spring Boot para la gestión de trámites académicos de posgrado de la Universidad Francisco de Paula Santander (UFPS).

**Equipo:** Raúl David Báez Suárez · Johan Steven Bueno Rojas · Kevin David Arias Villamizar · Santiago Danilo Cepeda Galeano · Diego Alexander Bermúdez Flores · Bryan Alexander Niño López  
**Asignatura:** Análisis y Diseño de Sistemas — Mayo 2026

---

## Tabla de Contenidos

1. [Visión General](#1-visión-general)
2. [Arquitectura y Estructura de Paquetes](#2-arquitectura-y-estructura-de-paquetes)
3. [Modelo de Datos](#3-modelo-de-datos)
4. [Endpoints API REST](#4-endpoints-api-rest)
5. [Reglas de Negocio](#5-reglas-de-negocio)
6. [Instalación y Ejecución Local](#6-instalación-y-ejecución-local)
7. [Pruebas](#7-pruebas)

---

## 1. Visión General

El backend gestiona los trámites académicos de posgrado de la UFPS: **terminación de materias**, **solicitud de grado**, **expedición de certificados académicos** y **gestión de paz y salvos**. Expone sus servicios a través de HTTP/JSON y utiliza Supabase (PostgreSQL + Storage) como capa de persistencia.

### Stack Tecnológico

| Componente | Tecnología | Detalle |
|---|---|---|
| Framework | Java 17 + Spring Boot 4.0.5 | Desplegado en Render (prod) |
| Base de Datos | PostgreSQL (Supabase) | ORM: Hibernate / JPA — `ddl-auto: update` |
| Almacenamiento | Supabase Storage | Bucket: `tramites-documentos` (privado) |
| Generación PDF | iText 7 (v7.2.5) | Actas de grado y certificados en PDF |
| Códigos QR | ZXing (v3.5.2) | QR embebido en certificados para validación |
| Documentación API | Swagger / OpenAPI (springdoc 2.8.8) | Ruta: `/swagger-ui.html` |
| Notificaciones RT | Server-Sent Events (SSE) | `/api/notificaciones/subscribe` |
| Email | Gmail SMTP / Spring Mail | Notificaciones automáticas por correo |
| Contenedor | Docker | `Dockerfile` incluido en la raíz |

### Variables de Entorno Requeridas

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `DB_URL` | (obligatorio) | URL JDBC de PostgreSQL en Supabase |
| `DB_USERNAME` | (obligatorio) | Usuario de la base de datos |
| `DB_PASSWORD` | (obligatorio) | Contraseña de la base de datos |
| `SUPABASE_URL` | (obligatorio) | URL base del proyecto Supabase |
| `SUPABASE_SERVICE_ROLE_KEY` | (obligatorio) | Clave para acceso a Storage privado |
| `SUPABASE_BUCKET` | `tramites-documentos` | Nombre del bucket de archivos |
| `CORS_ORIGIN` | `http://localhost:3000` | Origen permitido en CORS |
| `PORT` | `8080` | Puerto del servidor HTTP |

---

## 2. Arquitectura y Estructura de Paquetes

### Capas de la Aplicación

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Controller | `controller/` | Recibe peticiones HTTP, valida rol/existencia del usuario, delega al Service |
| Service | `service/` | Lógica de negocio, reglas institucionales, orquestación entre repositorios |
| Repository | `repository/` | Acceso a datos vía Spring Data JPA |
| Model (Entity) | `model/` | Clases JPA que mapean las tablas de PostgreSQL |
| DTO | `dto/` | Objetos de transferencia para request/response |
| Config | `config/` | `CorsConfig`, `SwaggerConfig` |
| Event | `event/` | `SolicitudEstadoCambiadoEvent` (SSE) |

### Estructura Completa de Paquetes

```
com.ufps.tramites
├── TramitesApplication.java                Clase principal Spring Boot
├── config/
│   ├── CorsConfig.java                     Configuración CORS (env: CORS_ORIGIN)
│   └── SwaggerConfig.java                  Configuración Swagger/OpenAPI
├── controller/
│   ├── AdminTipoCertificadoController.java /api/admin/tipos-certificado (ADMIN)
│   ├── CertificadoController.java          /api/certificados/** (estudiante + dependencia)
│   ├── ConvocatoriaController.java         /api/convocatorias/activa, PUT /api/convocatorias
│   ├── DependenciaController.java          /api/dependencias (listado de dependencias)
│   ├── NotificacionController.java         /api/notificaciones/subscribe (SSE)
│   ├── PazYSalvoController.java            /api/paz-y-salvo/** (dependencias + director)
│   ├── SolicitudController.java            /api/solicitudes/** (16+ endpoints)
│   ├── TramiteController.java              /api/tramites, /api/tramites/proceso-grado
│   └── UsuarioController.java              POST /login-demo, GET /me, POST /logout
├── dto/
│   ├── LoginRequestDTO.java
│   ├── SolicitudGradoDetalleDTO.java
│   ├── SolicitudGradoListDTO.java
│   └── UsuarioResponseDTO.java
├── event/
│   └── SolicitudEstadoCambiadoEvent.java
├── model/
│   ├── Convocatoria.java                   Período habilitado de trámites
│   ├── DecisionDirector.java
│   ├── DocumentoSolicitud.java             Archivos subidos a Supabase Storage
│   ├── PazYSalvo.java                      Paz y salvo por dependencia/director
│   ├── ProgramaAcademico.java              Programa con totalCreditos requeridos
│   ├── Solicitud.java                      Entidad central (terminación + grado)
│   ├── SolicitudCertificado.java           Solicitud específica de certificado
│   ├── TipoCertificado.java                Catálogo de tipos de certificado configurables
│   └── Usuario.java                        Estudiante / Director / Admin / Dependencia
├── repository/
│   ├── ConvocatoriaRepository.java
│   ├── DocumentoSolicitudRepository.java
│   ├── PazYSalvoRepository.java
│   ├── SolicitudCertificadoRepository.java
│   ├── SolicitudRepository.java
│   ├── TipoCertificadoRepository.java
│   └── UsuarioRepository.java
└── service/
    ├── ActaPdfGeneratorService.java         Genera el acta de grado en PDF (iText 7)
    ├── AlertaDirectorService.java           Alertas cuando el Director supera el plazo
    ├── CertificadoConstanciaPdfService.java PDF de constancias académicas
    ├── CertificadoPdfService.java           PDF de certificados con firma digital + QR
    ├── CertificadoService.java              Lógica completa del módulo de certificados
    ├── ConvocatoriaService.java             Validación de fechas del calendario académico
    ├── CorreoCertificadoService.java        Envío de email al emitir certificado
    ├── CorreoConstanciaService.java         Envío de email por constancias
    ├── DecisionSolicitudService.java        Flujo aprobar/rechazar del Director
    ├── DocumentoService.java                Subida y descarga de archivos
    ├── NotificacionEmailService.java        Envío de emails transaccionales
    ├── NotificacionService.java             Lógica de notificaciones
    ├── NotificacionSseService.java          Stream SSE por cédula
    ├── PazYSalvoService.java                Gestión del flujo de paz y salvos
    ├── SolicitudService.java                Orquestador principal de trámites
    ├── SupabaseStorageService.java          Cliente HTTP para Supabase Storage
    ├── TramiteService.java                  Módulo de trámites por rol
    ├── UsuarioService.java                  CRUD de usuarios
    └── ValidacionGradoService.java          Validación de la Unidad de Posgrados
```

---

## 3. Modelo de Datos

### Solicitud _(tabla: solicitud)_

Entidad central del sistema. Representa trámites de terminación de materias y solicitudes de grado.

| Campo | Tipo Java | Descripción |
|---|---|---|
| `id` | `Long` (PK, auto) | Identificador autoincremental |
| `cedula` | `String` | Cédula del estudiante solicitante |
| `tipo` | `String` | `TERMINACION_MATERIAS` \| `GRADO` |
| `estado` | `String` | `PENDIENTE_PAGO` \| `EN_REVISION` \| `APROBADA` \| `RECHAZADA` \| `PENDIENTE_VALIDACION` \| `APROBADA_POSGRADOS` \| `RECHAZADA_POSGRADOS` |
| `fechaSolicitud` | `LocalDate` | Fecha de creación |
| `costo` | `Double` | Monto liquidado en COP |
| `observaciones` | `String` | Notas del estudiante |
| `decision` | `String` | `APROBADA` \| `RECHAZADA` (Director) |
| `observacionesDirector` | `String` | Motivo del Director |
| `fechaDecision` | `LocalDateTime` | Timestamp de la decisión del Director |
| `cedulaDirector` | `String` | Cédula del Director que decidió |
| `fechaEnRevision` | `LocalDateTime` | Inicio del plazo de revisión |
| `tituloProyecto` | `String` | Solo solicitudes de tipo `GRADO` |
| `tipoProyecto` | `String` | `INVESTIGACION` \| `MONOGRAFIA` \| `SISTEMATIZACION` \| `TRABAJO_DIRIGIDO` \| `PASANTIA` |
| `resumenProyecto` | `String (TEXT)` | Resumen del proyecto de grado |
| `validacionPosgrados` | `String` | `APROBADA` \| `RECHAZADA` (Unidad Posgrados) |
| `observacionesPosgrados` | `String` | Motivo de la validación de posgrados |
| `fechaValidacion` | `LocalDateTime` | Timestamp de la validación de posgrados |
| `cedulaPosgrados` | `String` | Cédula del Admin que validó |

### SolicitudCertificado _(tabla: solicitud_certificado)_

Entidad independiente para el módulo de certificados académicos.

| Campo | Tipo Java | Descripción |
|---|---|---|
| `id` | `Long` (PK, auto) | Identificador autoincremental |
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

### TipoCertificado _(tabla: tipo_certificado)_

Catálogo configurable por el Administrador.

| Campo | Tipo Java | Descripción |
|---|---|---|
| `id` | `Long` (PK, auto) | Identificador autoincremental |
| `nombre` | `String` | Nombre del certificado |
| `descripcion` | `String` | Descripción breve |
| `costo` | `Double` | Tarifa en COP |
| `activo` | `Boolean` | Si está disponible para solicitar |
| `estadosHabilitados` | `String` | Estados académicos que pueden solicitarlo |
| `cedulaDependencia` | `String` | Dependencia encargada de gestionar la entrega |

### PazYSalvo _(tabla: paz_y_salvo)_

Registro de paz y salvos por dependencia para el proceso de grado.

| Campo | Tipo Java | Descripción |
|---|---|---|
| `id` | `Long` (PK, auto) | Identificador autoincremental |
| `solicitudId` | `Long` | FK a `solicitud.id` |
| `cedulaResponsable` | `String` | Cédula de la dependencia o director responsable |
| `nombreResponsable` | `String` | Nombre descriptivo de la dependencia |
| `estado` | `String` | `PENDIENTE` \| `APROBADO` \| `RECHAZADO` |
| `observaciones` | `String` | Observaciones al responder |
| `fechaRespuesta` | `LocalDateTime` | Timestamp de la respuesta |

### Usuario _(tabla: usuario)_

| Campo | Tipo Java | Descripción |
|---|---|---|
| `cedula` | `String` (PK) | Cédula — identificador principal |
| `codigo` | `String` | Código institucional UFPS |
| `nombre` | `String` | Nombre completo |
| `contrasena` | `String` | No expuesta en respuestas de API |
| `rol` | `String` | `ESTUDIANTE` \| `DIRECTOR` \| `ADMIN` \| `DEPENDENCIA` |
| `creditosAprobados` | `Integer` | Créditos aprobados del estudiante |
| `correo` | `String` | Correo electrónico institucional |
| `estadoGrado` | `String` | `null` \| `GRADUADO` |
| `programaAcademico` | `ProgramaAcademico` (FK) | Programa al que pertenece |

### DocumentoSolicitud _(tabla: documento_solicitud)_

| Campo | Tipo Java | Descripción |
|---|---|---|
| `id` | `Long` (PK, auto) | Identificador autoincremental |
| `solicitudId` | `Long` | FK ligera a `solicitud.id` |
| `tipo` | `String` | `SOPORTE` \| `ACTA` |
| `nombreOriginal` | `String` | Nombre del archivo del usuario |
| `nombreAlmacenado` | `String` | Nombre en Supabase Storage |
| `contentType` | `String` | MIME type del archivo |
| `tamano` | `Long` | Tamaño en bytes |
| `fechaSubida` | `LocalDateTime` | Timestamp de carga |

### Convocatoria _(tabla: convocatoria)_

| Campo | Tipo Java | Descripción |
|---|---|---|
| `id` | `Long` (PK, auto) | Identificador autoincremental |
| `fechaInicio` | `LocalDate` | Inicio del período habilitado |
| `fechaFin` | `LocalDate` | Fin del período habilitado |

### ProgramaAcademico _(tabla: programa_academico)_

| Campo | Tipo Java | Descripción |
|---|---|---|
| `id` | `Long` (PK, auto) | Identificador autoincremental |
| `nombre` | `String` (UNIQUE) | Nombre del programa |
| `tipo` | `String` | `DOCTORADO` \| `MAESTRIA` \| `ESPECIALIZACION` |
| `totalCreditos` | `Integer` | Créditos totales requeridos para graduarse |

---

## 4. Endpoints API REST

URL base: `http://localhost:8080/api`  
Swagger UI: `http://localhost:8080/swagger-ui.html`  
Autenticación: sesión HTTP. El login crea la sesión; cada petición debe incluir `credentials: 'include'`.

### 4.1 Usuarios — `/api/usuarios`

| Método | Ruta | Parámetros | Descripción | Respuesta |
|---|---|---|---|---|
| `POST` | `/login-demo` | `cedula` (query) | Login sin contraseña (modo demo). Guarda cédula en sesión HTTP. | `200 UsuarioResponseDTO` \| `404` |
| `GET` | `/me` | — (sesión) | Retorna el usuario autenticado en la sesión actual. | `200 UsuarioResponseDTO` \| `401` |
| `POST` | `/logout` | — (sesión) | Invalida la sesión. | `200 OK` |

### 4.2 Trámites — `/api/tramites`

| Método | Ruta | Parámetros | Descripción | Respuesta |
|---|---|---|---|---|
| `GET` | `/` | `cedula` o `codigo` (query, opcional) | Módulo de trámites disponibles según rol del usuario. | `200 JSON` |
| `GET` | `/proceso-grado` | `cedula` o `codigo` (query, opcional) | Estado, créditos y etapas del proceso de grado del estudiante. | `200 JSON` |

### 4.3 Solicitudes — `/api/solicitudes`

| Método | Ruta | Parámetros | Descripción | Resp. |
|---|---|---|---|---|
| `POST` | `/terminacion-materias` | `cedula` (query) | Crea solicitud de terminación. Valida: créditos, convocatoria, sin duplicado. | `201` \| `422` |
| `POST` | `/grado` | `cedula`, `tituloProyecto`, `resumen`, `tipoProyecto`, `foto`, `actaSustentacion`, `[certificadoIngles]` (multipart) | Crea solicitud de grado con documentos. Requiere terminación `APROBADA` previa. | `201` \| `422` |
| `GET` | `/` | `cedula` (query) | Lista todas las solicitudes del estudiante. | `200 List` |
| `GET` | `/bandeja` | `cedula` (query, **DIRECTOR**) | Solicitudes de terminación del programa del Director agrupadas por estado. | `200` \| `403` |
| `GET` | `/bandeja-grado` | `cedula` (query, **DIRECTOR**) | Solicitudes de grado del programa del Director. | `200` \| `403` |
| `POST` | `/{id}/aprobar` | `id` (path), `cedula` (query, **DIRECTOR**) | Director aprueba la solicitud. Dispara notificación SSE al estudiante. | `200` \| `403` \| `422` |
| `POST` | `/{id}/rechazar` | `id` (path), `cedula`, `[motivo]` (**DIRECTOR**) | Director rechaza con motivo opcional. | `200` \| `403` \| `422` |
| `GET` | `/{id}/acta` | `id` (path) | Genera/descarga acta de grado en PDF. En primer acceso marca al estudiante como `GRADUADO`. | `200 PDF` |
| `GET` | `/{id}/certificado` | `id` (path) | Descarga certificado de terminación (requiere estado `APROBADA`). | `200 TXT` |
| `POST` | `/{id}/documentos` | `id` (path), `archivo` (multipart) | Sube documento a Supabase Storage y registra metadatos. | `201` \| `400` |
| `GET` | `/{id}/documentos` | `id` (path) | Lista documentos de la solicitud. | `200 List` |
| `GET` | `/{id}/documentos/{docId}/file` | `id`, `docId` (path) | Descarga archivo desde Supabase Storage. | `200 bytes` \| `404` |
| `GET` | `/posgrados/pendientes` | `cedula` (query, **ADMIN**) | Lista solicitudes de grado en estado `PENDIENTE_VALIDACION`. | `200` \| `403` |
| `POST` | `/{id}/validar-grado` | `id` (path), `cedula`, `decision`, `[observaciones]` (**ADMIN**) | Unidad Posgrados aprueba o rechaza solicitud de grado. | `200` \| `422` |

### 4.4 Certificados — `/api/certificados`

| Método | Ruta | Parámetros | Descripción | Resp. |
|---|---|---|---|---|
| `GET` | `/tipos` | — | Lista todos los tipos de certificado activos. | `200 List` |
| `POST` | `/solicitar` | `cedula`, `tipo`, `modalidad`, `[destinatario]` (query) | Crea solicitud de certificado y genera liquidación. Valida estado académico. | `201` \| `422` |
| `GET` | `/` | `cedula` (query) | Lista todos los certificados del estudiante. | `200 List` |
| `POST` | `/{id}/pagar` | `id` (path), `cedula` (query) | Simula pago y avanza el certificado a estado `PAGADO`. Genera el PDF con firma digital y QR. | `200` \| `422` |
| `GET` | `/{id}/pdf` | `id` (path), `cedula` (query) | Descarga el PDF del certificado. Disponible para el dueño o la dependencia encargada. | `200 PDF` \| `422` |
| `GET` | `/dependencia/{cedulaDependencia}` | `cedulaDependencia` (path), `[estado]` (query) | Bandeja de certificados asignados a la dependencia, filtrable por estado. | `200 List` \| `403` |
| `POST` | `/{id}/marcar-listo` | `id` (path), `cedulaDependencia` (query) | Dependencia marca el certificado físico como listo para retiro. | `200` \| `422` |
| `POST` | `/{id}/marcar-entregado` | `id` (path), `cedulaDependencia` (query) | Dependencia confirma la entrega física al estudiante. | `200` \| `422` |

### 4.5 Paz y Salvos — `/api/paz-y-salvo`

| Método | Ruta | Parámetros | Descripción | Resp. |
|---|---|---|---|---|
| `GET` | `/mis-solicitudes` | `cedula` (query, **DEPENDENCIA** o **DIRECTOR**) | Lista todos los paz y salvos asignados al responsable. | `200 List` \| `403` |
| `GET` | `/pendientes` | `cedula` (query, **DEPENDENCIA** o **DIRECTOR**) | Lista únicamente los paz y salvos pendientes del responsable. | `200 List` \| `403` |
| `POST` | `/{id}/responder` | `id` (path), `cedula`, `decision` (`APROBADO`\|`RECHAZADO`), `[observaciones]` | Dependencia o Director responde su paz y salvo. Si todos quedan respondidos, avanza la solicitud. | `200` \| `422` |
| `GET` | `/solicitud/{solicitudId}` | `solicitudId` (path) | Estado general de todos los paz y salvos de una solicitud de grado. | `200` |
| `GET` | `/estado-estudiantes` | `cedula` (query, **DIRECTOR**) | Vista del estado de los estudiantes del programa en el proceso de paz y salvos. | `200` \| `403` |

### 4.6 Dependencias — `/api/dependencias`

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| `GET` | `/` | Lista todas las dependencias registradas en el sistema. | `200 List` |

### 4.7 Admin — Tipos de Certificado — `/api/admin/tipos-certificado`

| Método | Ruta | Parámetros | Descripción | Resp. |
|---|---|---|---|---|
| `GET` | `/` | `cedula` (query, **ADMIN**) | Lista todos los tipos de certificado (activos e inactivos). | `200 List` |
| `POST` | `/` | `cedula` (query, **ADMIN**), body JSON | Crea un nuevo tipo de certificado. | `201` \| `403` |
| `PUT` | `/{id}` | `id` (path), `cedula` (query, **ADMIN**), body JSON | Actualiza un tipo de certificado existente. | `200` \| `403` |
| `DELETE` | `/{id}` | `id` (path), `cedula` (query, **ADMIN**) | Desactiva (baja lógica) un tipo de certificado. | `200` \| `403` |

### 4.8 Convocatorias — `/api/convocatorias`

| Método | Ruta | Parámetros | Descripción | Respuesta |
|---|---|---|---|---|
| `GET` | `/activa` | — | Retorna la convocatoria activa (`fechaInicio`, `fechaFin`). | `200 JSON` |
| `PUT` | `/` | `cedula` (query, **ADMIN**), body `{fechaInicio, fechaFin}` | Actualiza las fechas del período habilitado. | `200` \| `400` \| `403` |

### 4.9 Notificaciones — `/api/notificaciones`

| Método | Ruta | Parámetros | Descripción | Respuesta |
|---|---|---|---|---|
| `GET` | `/subscribe` | `cedula` (query) | Suscripción SSE. Emite `conectado` y `estado-actualizado` con `{solicitudId, tipo, estadoAnterior, estadoNuevo, observaciones, certificadoDisponible}`. | `text/event-stream` |

---

## 5. Reglas de Negocio

### 5.1 Terminación de Materias (`SolicitudService`)

| Regla | Validación | Error |
|---|---|---|
| Créditos suficientes | `creditosAprobados >= programa.totalCreditos` | `422: "No cumple los requisitos académicos: tiene X/Y créditos aprobados."` |
| Convocatoria vigente | `ConvocatoriaService.estaVigente() == true` | `422: "La solicitud está fuera del período habilitado (fecha al fecha)."` |
| Sin duplicado activo | No existe `Solicitud` con misma cédula + tipo `TERMINACION_MATERIAS` activa | `422: "Ya existe una solicitud de terminación de materias con estado: X"` |
| Costo fijo | `COSTO_TERMINACION = 150.000 COP` | Liquidación automática al crear |

### 5.2 Solicitud de Grado (`SolicitudService`)

| Regla | Validación | Error |
|---|---|---|
| Terminación previa aprobada | Existe `Solicitud` tipo `TERMINACION_MATERIAS` con estado `APROBADA` | `422` |
| Sin solicitud de grado activa | No existe `Solicitud` tipo `GRADO` activa | `422` |
| Documentos obligatorios | `foto` + `actaSustentacion` no nulos | `422 / 400` |
| Costo fijo | `COSTO_GRADO = 250.000 COP` | Liquidación automática |

### 5.3 Validación de Grado (`ValidacionGradoService`)

| Regla | Validación | Error |
|---|---|---|
| Tipo correcto | `solicitud.tipo == GRADO` | `422: "Esta solicitud no es de tipo GRADO"` |
| Estado correcto | `solicitud.estado == PENDIENTE_VALIDACION` | `422: "La solicitud no está pendiente de validación"` |
| Decisión válida | `decision == APROBADA \|\| RECHAZADA` | `422: "Decisión inválida: X"` |

### 5.4 Paz y Salvos (`PazYSalvoService`)

| Regla | Descripción |
|---|---|
| Generación automática | Al aprobar una solicitud de grado el sistema crea automáticamente registros `PazYSalvo` para cada dependencia responsable y el Director. |
| Bloqueo de avance | El flujo no avanza a `PENDIENTE_VALIDACION` hasta que todos los paz y salvos estén en estado `APROBADO`. |
| Responsable único | Cada dependencia solo puede ver y responder sus propios paz y salvos. |

### 5.5 Certificados (`CertificadoService`)

| Regla | Descripción |
|---|---|
| Filtro por estado | Solo se muestran tipos de certificado habilitados para el estado académico del solicitante. |
| Generación con autenticidad | Al confirmar el pago, el sistema genera automáticamente el PDF con firma digital, número de radicado único y código QR de verificación. |
| Correo automático | Al generar el PDF se envía automáticamente un email al estudiante con el certificado adjunto. |

### 5.6 Flujo de Estados

```
TERMINACION_MATERIAS:
  PENDIENTE_PAGO → EN_REVISION → APROBADA
                              → RECHAZADA

GRADO:
  PENDIENTE_PAGO → EN_REVISION → PENDIENTE_VALIDACION → APROBADA_POSGRADOS
                                                       → RECHAZADA_POSGRADOS

CERTIFICADO:
  PENDIENTE_PAGO → PAGADO → GENERADO → LISTO_RETIRO → ENTREGADO
```

### 5.7 Roles y Permisos

| Rol | Acciones permitidas |
|---|---|
| `ESTUDIANTE` | Crear solicitudes, consultar propias, subir documentos, descargar acta/certificado, solicitar y descargar certificados académicos, suscribirse a SSE |
| `DIRECTOR` | Ver bandeja de terminación y grado de su programa, aprobar/rechazar solicitudes, responder paz y salvos, ver estado de estudiantes |
| `ADMIN` | Ver pendientes de validación, validar solicitudes de grado, actualizar convocatoria, gestionar tipos de certificado |
| `DEPENDENCIA` | Ver y responder sus paz y salvos, gestionar bandeja de certificados asignados |

---

## 6. Instalación y Ejecución Local

### Requisitos Previos

- Java 17 o superior
- Maven 3.8+ (o usar el wrapper `mvnw` incluido)
- Cuenta y proyecto en Supabase (PostgreSQL + Storage)

### Clonar el Repositorio

```bash
git clone https://github.com/SteveeenB/tramites-backend.git
cd tramites-backend
```

### Actualizar desde el Remoto

```bash
# Traer todo del remoto conservando cambios locales
git stash
git pull origin main
git stash pop

# O descartar cambios locales completamente
git fetch origin
git reset --hard origin/main
```

### Configurar Variables de Entorno

```bash
export DB_URL=jdbc:postgresql://<host>:<port>/<database>
export DB_USERNAME=<usuario>
export DB_PASSWORD=<contraseña>
export SUPABASE_URL=https://<proyecto>.supabase.co
export SUPABASE_SERVICE_ROLE_KEY=<service-role-key>
export CORS_ORIGIN=http://localhost:3000
```

### Ejecutar el Servidor

```bash
./mvnw spring-boot:run
```

- Servidor: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API docs JSON: `http://localhost:8080/api-docs`

### Ejecutar con Docker

```bash
docker build -t tramites-backend .
docker run -p 8080:8080 \
  -e DB_URL=... \
  -e DB_USERNAME=... \
  -e DB_PASSWORD=... \
  -e SUPABASE_URL=... \
  -e SUPABASE_SERVICE_ROLE_KEY=... \
  tramites-backend
```

### Configurar Correo

El sistema usa Gmail SMTP para envío de notificaciones. Editar `application.properties` o establecer las variables correspondientes:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=tu@correo.com
spring.mail.password=tu-contrasena-de-aplicacion
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

---

## 7. Pruebas

### Clases de Prueba Existentes

| Clase | Capa | Sprint | Cobertura |
|---|---|---|---|
| `TramitesApplicationTests` | Integración | S1 | Carga del contexto Spring Boot completo |
| `SolicitudRepositoryTest` | Repository | S1 | Consultas JPA sobre la entidad `Solicitud` |
| `AlertaDirectorServiceTest` | Service | S2 | Alertas por plazo vencido del Director (TP-44) |
| `DecisionSolicitudServiceTest` | Service | S2 | Flujo completo de aprobación y rechazo (HU-04) |
| `CertificadoEndpointTest` | Service | S3 | Catálogo de tipos activos, validación de tipo/modalidad, duplicados vigentes y cálculo de precios — HU-12 (TP-87) |
| `CertificadoQRValidationTest` | Service | S3 | Radicado único, generación y determinismo del QR con ZXing, hash SHA-256 del PDF, reglas de negocio de pago y descarga — HU-13 (TP-97) |

### Ejecutar las Pruebas

```bash
./mvnw test
```

Para generar reporte HTML en `target/site/surefire-report.html`:

```bash
./mvnw surefire-report:report
```

### Pendientes de Cobertura

| Servicio / Flujo | Estado | Notas |
|---|---|---|
| `PazYSalvoService` | Sin prueba unitaria | Mocking de dependencias externas no implementado |
| `ValidacionGradoService` | Sin prueba unitaria | Validación de posgrados HU-09 |
| `POST /pagos/confirmar` | Endpoint pendiente (TP-37) | Integración Wompi — bloqueante para HU-03 |.
