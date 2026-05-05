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

El backend gestiona los trámites académicos de posgrado de la UFPS: **terminación de materias**, **solicitud de grado** y **expedición de certificados**. Expone sus servicios a través de HTTP/JSON y utiliza Supabase (PostgreSQL + Storage) como capa de persistencia.

### Stack Tecnológico

| Componente | Tecnología | Detalle |
|---|---|---|
| Framework | Java + Spring Boot | Desplegado en Render (prod) |
| Base de Datos | PostgreSQL (Supabase) | ORM: Hibernate / JPA — `ddl-auto: update` |
| Almacenamiento | Supabase Storage | Bucket: `tramites-documentos` (privado) |
| Documentación API | Swagger / OpenAPI | Ruta: `/swagger-ui.html` |
| Notificaciones RT | Server-Sent Events (SSE) | `/api/notificaciones/subscribe` |
| Email (dev) | MailHog — `localhost:1025` | SMTP local sin autenticación |
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
│   ├── ConvocatoriaController.java         GET /api/convocatorias/activa, PUT /api/convocatorias
│   ├── NotificacionController.java         GET /api/notificaciones/subscribe (SSE)
│   ├── SolicitudController.java            /api/solicitudes/** (14 endpoints)
│   ├── TramiteController.java              GET /api/tramites, /api/tramites/proceso-grado
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
│   ├── ProgramaAcademico.java              Programa con totalCreditos requeridos
│   ├── Solicitud.java                      Entidad central del sistema
│   └── Usuario.java                        Estudiante / Director / Admin
├── repository/
│   ├── ConvocatoriaRepository.java
│   ├── DocumentoSolicitudRepository.java
│   ├── SolicitudRepository.java
│   └── UsuarioRepository.java
└── service/
    ├── ActaPdfGeneratorService.java        Genera el acta de grado en PDF
    ├── AlertaDirectorService.java          Alertas cuando el Director supera el plazo
    ├── ConvocatoriaService.java            Validación de fechas del calendario académico
    ├── DecisionSolicitudService.java       Flujo aprobar/rechazar
    ├── DocumentoService.java              Subida y descarga de archivos
    ├── NotificacionEmailService.java       Envío de emails
    ├── NotificacionService.java            Lógica de notificaciones
    ├── NotificacionSseService.java         Stream SSE por cédula
    ├── SolicitudService.java               Orquestador principal de trámites
    ├── SupabaseStorageService.java         Cliente HTTP para Supabase Storage
    ├── TramiteService.java                 Módulo de trámites por rol
    ├── UsuarioService.java                 CRUD de usuarios
    └── ValidacionGradoService.java         Validación de posgrados sobre solicitud de grado
```

---

## 3. Modelo de Datos

### Solicitud _(tabla: solicitud)_

Entidad central del sistema. Representa cualquier trámite iniciado por un estudiante o graduado.

| Campo | Tipo Java | Descripción |
|---|---|---|
| `id` | `Long` (PK, auto) | Identificador autoincremental |
| `cedula` | `String` | Cédula del estudiante solicitante |
| `tipo` | `String` | `TERMINACION_MATERIAS` \| `GRADO` \| `CERTIFICADO` |
| `estado` | `String` | `PENDIENTE_PAGO` \| `EN_REVISION` \| `APROBADA` \| `RECHAZADA` \| `PENDIENTE_VALIDACION` \| `APROBADA_POSGRADOS` \| `RECHAZADA_POSGRADOS` |
| `fechaSolicitud` | `LocalDate` | Fecha de creación |
| `costo` | `Double` | Monto liquidado en COP |
| `observaciones` | `String` | Notas del estudiante |
| `decision` | `String` | `APROBADA` \| `RECHAZADA` (Director) — `null` hasta que decide |
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

### Usuario _(tabla: usuario)_

| Campo | Tipo Java | Descripción |
|---|---|---|
| `cedula` | `String` (PK) | Cédula — identificador principal |
| `codigo` | `String` | Código institucional UFPS |
| `nombre` | `String` | Nombre completo |
| `contrasena` | `String` | No expuesta en respuestas de API |
| `rol` | `String` | `ESTUDIANTE` \| `DIRECTOR` \| `ADMIN` |
| `creditosAprobados` | `Integer` | Créditos aprobados del estudiante |
| `correo` | `String` | Correo electrónico institucional |
| `estadoGrado` | `String` | `null` \| `GRADUADO` |
| `programaAcademico` | `ProgramaAcademico` (FK) | Programa al que pertenece (`@ManyToOne`) |

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
| `GET` | `/{id}/documentos/{docId}/file` | `id`, `docId` (path) | Descarga archivo desde Supabase Storage (bucket privado). | `200 bytes` \| `404` |
| `GET` | `/posgrados/pendientes` | `cedula` (query, **ADMIN**) | Lista solicitudes de grado en estado `PENDIENTE_VALIDACION`. | `200` \| `403` |
| `POST` | `/{id}/validar-grado` | `id` (path), `cedula`, `decision`, `[observaciones]` (**ADMIN**) | Unidad Posgrados aprueba o rechaza solicitud de grado. | `200` \| `422` |

### 4.4 Convocatorias — `/api/convocatorias`

| Método | Ruta | Parámetros | Descripción | Respuesta |
|---|---|---|---|---|
| `GET` | `/activa` | — | Retorna la convocatoria activa (`fechaInicio`, `fechaFin`). | `200 JSON` |
| `PUT` | `/` | `cedula` (query, **ADMIN**), body `{fechaInicio, fechaFin}` | Actualiza las fechas del período habilitado. | `200` \| `400` \| `403` |

### 4.5 Notificaciones — `/api/notificaciones`

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
| Sin duplicado activo | No existe `Solicitud` con misma cédula + tipo `TERMINACION_MATERIAS` | `422: "Ya existe una solicitud de terminación de materias con estado: X"` |
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

### 5.4 Flujo de Estados

```
TERMINACION_MATERIAS:
  PENDIENTE_PAGO → EN_REVISION → APROBADA
                              → RECHAZADA

GRADO:
  PENDIENTE_PAGO → EN_REVISION → PENDIENTE_VALIDACION → APROBADA_POSGRADOS
                                                       → RECHAZADA_POSGRADOS
```

### 5.5 Roles y Permisos

| Rol | Acciones permitidas |
|---|---|
| `ESTUDIANTE` | Crear solicitudes, consultar propias, subir documentos, descargar acta/certificado, suscribirse a SSE |
| `DIRECTOR` | Ver bandeja de terminación y grado de su programa, aprobar/rechazar solicitudes |
| `ADMIN` | Ver pendientes de validación, validar solicitudes de grado, actualizar convocatoria |

---

## 6. Instalación y Ejecución Local

### Requisitos Previos

- Java 17 o superior
- Maven 3.8+ (o usar el wrapper `mvnw` incluido)
- Cuenta y proyecto en Supabase (PostgreSQL + Storage)
- MailHog instalado localmente para pruebas de email (opcional)

### Clonar el Repositorio

```bash
git clone https://github.com/SteveeenB/tramites-backend.git
cd tramites-backend
```

### Actualizar desde el Remoto

```bash
# Traer todo del remoto descartando cambios locales
git fetch origin
git reset --hard origin/main

# Si quieres conservar cambios locales primero
git stash
git pull origin main
git stash pop
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

### Configurar Correo para Producción

Editar `application.properties`:

```properties
spring.mail.host=smtp.tuservidor.com
spring.mail.port=587
spring.mail.username=tu@correo.com
spring.mail.password=tucontraseña
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

---

## 7. Pruebas

### Clases de Prueba Existentes

| Clase | Capa | Cobertura |
|---|---|---|
| `TramitesApplicationTests` | Integración | Carga del contexto Spring Boot completo |
| `SolicitudRepositoryTest` | Repository | Consultas JPA sobre la entidad `Solicitud` |
| `AlertaDirectorServiceTest` | Service | Alertas por plazo vencido del Director (TP-44) |
| `DecisionSolicitudServiceTest` | Service | Flujo completo de aprobación y rechazo (HU-04) |

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
| `SolicitudService.crearSolicitudGrado` | Sin prueba unitaria | Requiere mock de `SupabaseStorageService` |
| `ValidacionGradoService` | Sin prueba unitaria | Validación de posgrados HU-09 |
| `POST /pagos/confirmar` | Endpoint pendiente (TP-37) | Bloqueante para HU-03 |
| Paz y salvos (HU-08) | En curso | Mocking de dependencias externas no implementado |
