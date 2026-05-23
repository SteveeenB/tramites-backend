# Documentación de Autenticación y Seguridad

## Fase 0 — Modelo de datos

- **`Rol.java`** — entidad que mapea la tabla `roles` (id, nombre). Roles: ESTUDIANTE, DIRECTOR, ADMIN, POSGRADOS, DEPENDENCIA.
- **`Usuario.java`** — rediseñado: PK es `id` (Long), `rol` es `@ManyToOne → Rol`. Campos agregados: `email`, `googleId`, `fotoUrl`, `moodleId`, `telefono`. Helpers de compatibilidad: `getNombre()`, `getCorreo()`, `getRolNombre()`.
- **`UsuarioRepository`** — métodos: `findByCedula()`, `findByEmail()`, `findByGoogleId()`, `findByRolNombre()`.
- **`data.sql`** — inserta primero los roles (IDs 1–5), luego los usuarios con el nuevo esquema. Contraseñas almacenadas como hash BCrypt de `123456`.
- Todos los controllers y servicios actualizados: `getRol()` → `getRolNombre()`.

---

## Fase 1 — Autenticación JWT

### Cómo funciona el flujo

```
[Login.jsx]
  POST /api/auth/login  { codigo, contrasena }
        ↓
[AuthController]
  1. Busca usuario por código en DB
  2. Valida contraseña con BCrypt (passwordEncoder.matches)
  3. Genera JWT con JwtService
  4. Devuelve { token, id, cedula, nombreCompleto, email, codigo, rol }
        ↓
[Frontend — AuthContext.js]
  1. Guarda token en localStorage
  2. Decodifica payload del JWT (sin verificar firma, solo leer claims)
  3. Guarda usuario en estado React
```

### Claims del JWT

| Claim          | Valor             | Tipo     |
|----------------|-------------------|----------|
| `sub`          | id del usuario    | estándar |
| `cedula`       | cédula            | custom   |
| `rol`          | nombre del rol    | custom   |
| `nombreCompleto` | nombre completo | custom   |
| `codigo`       | código académico  | custom   |
| `email`        | correo            | custom   |
| `iat` / `exp`  | emisión/expiración (24h) | estándar |

### Archivos clave

- **`JwtService.java`** — genera y valida tokens HS256. Secreto configurado en `jwt.secret` (properties).
- **`JwtAuthFilter.java`** — intercepta cada request, valida el token, extrae `cedula` y `rol`, puebla el `SecurityContext` con autoridad `ROLE_<ROL>`.
- **`SecurityConfig.java`** — Spring Security stateless, CORS desde `CorsConfig`, rutas públicas: `/api/auth/**`, `/swagger-ui/**`, SSE.
- **`AuthController.java`** — tres endpoints:
  - `POST /api/auth/login` — código + contraseña (BCrypt)
  - `POST /api/auth/google` — id_token de Google OAuth
  - `POST /api/auth/login-demo` — solo activo con perfil `dev` (`demo.auth.enabled=true`)

### Perfiles de Spring Boot

```
application.properties          ← base (demo.auth.enabled=false)
application-local.properties    ← credenciales Supabase (no se sube al repo)
application-dev.properties      ← demo.auth.enabled=true
```

`spring.profiles.active=local,dev` activa ambos perfiles simultáneamente.

---

## Fase 2 — Frontend

- **`AuthContext.js`** — restaura sesión desde `localStorage`, expone `login()`, `logout()`, `cambiarRol()` (llama a `/api/auth/login-demo`).
- **`apiClient.js`** — agrega `Authorization: Bearer <token>` a cada request, maneja 401 redirigiendo a `/login`.
- **`Login.jsx`** — formulario código/contraseña + botón Google OAuth (solo visible si `REACT_APP_GOOGLE_CLIENT_ID` está configurado).
- **`ProtectedRoute.js`** — redirige a `/login` si no hay sesión; redirige a `/no-autorizado` si el rol no tiene acceso a la ruta.
- Todos los hooks y APIs eliminaron el parámetro `?cedula=` — la cédula la provee el JWT.

### Variable de entorno para el selector demo

En `frontend/.env`:
```
REACT_APP_DEMO_MODE=false   # oculta el selector de usuarios demo en el sidebar
```
Cambiar a `true` (o eliminar la línea) para mostrarlo.

---

## Fase 3 — Seguridad por rol y contraseñas BCrypt

### Problema que resuelve

Antes de esta fase, cualquier usuario con un JWT válido (incluso ESTUDIANTE) podía llamar endpoints de DIRECTOR o ADMIN directamente desde Postman. Además, las contraseñas estaban en texto plano en la base de datos.

### BCrypt — contraseñas

**`UsuarioService.guardarUsuario()`** — ahora encripta la contraseña antes de persistir:
```java
if (usuario.getPassword() != null && !usuario.getPassword().startsWith("$2a$")) {
    usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
}
```
El guard `!startsWith("$2a$")` evita doble-encodeo si el usuario ya tiene hash.

**`AuthController.login()`** — eliminado el fallback de texto plano. Solo acepta BCrypt:
```java
boolean passwordValida = usuario.getPassword() != null &&
        passwordEncoder.matches(request.getContrasena(), usuario.getPassword());
```

Hash BCrypt de `123456` usado en `data.sql`:
```
$2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq
```

> **Nota para deploys existentes:** como los INSERTs usan `ON CONFLICT DO NOTHING`, ejecutar en Supabase:
> ```sql
> UPDATE usuario SET contrasena = '$2a$10$TCpV633Sg7xBIMP/VpL80uQw9YHjSPvk5iFmk6aFs.yxQwVq5eSBq'
> WHERE contrasena = '123456';
> ```

### @PreAuthorize — protección por rol en el backend

Habilitado con `@EnableMethodSecurity(prePostEnabled = true)` en `SecurityConfig`.

**Regla:** `hasRole('DIRECTOR')` busca autoridad `ROLE_DIRECTOR` (Spring agrega el prefijo automáticamente).

#### SolicitudController — `/api/solicitudes`

| Endpoint | Rol requerido |
|----------|---------------|
| `POST /terminacion-materias` | ESTUDIANTE |
| `POST /grado` | ESTUDIANTE |
| `GET /` | ESTUDIANTE |
| `POST /{id}/pagar-grado` | ESTUDIANTE |
| `POST /{id}/documentos` | ESTUDIANTE |
| `GET /{id}/documentos` | ESTUDIANTE, DIRECTOR |
| `GET /{id}/documentos/{docId}/file` | ESTUDIANTE, DIRECTOR |
| `GET /bandeja` | DIRECTOR |
| `GET /bandeja-grado` | DIRECTOR |
| `POST /{id}/aprobar` | DIRECTOR |
| `POST /{id}/rechazar` | DIRECTOR |
| `GET /{id}/acta` | DIRECTOR, POSGRADOS |
| `GET /{id}/certificado` | DIRECTOR, POSGRADOS |
| `POST /{id}/fecha-grado` | DIRECTOR, POSGRADOS |
| `GET /posgrados/pendientes` | ADMIN, POSGRADOS |
| `POST /{id}/validar-grado` | ADMIN, POSGRADOS |
| `GET /posgrados/bandeja` | POSGRADOS |
| `GET /{id}/certificado-pdf` | POSGRADOS |
| `POST /{id}/rechazar-posgrados` | POSGRADOS |
| `POST /{id}/aprobar-posgrados` | POSGRADOS |

#### AdminController — `/api`

| Endpoint | Rol requerido |
|----------|---------------|
| `GET /dependencias` | ADMIN, POSGRADOS |
| `GET /admin/tipos-certificado` | ADMIN, POSGRADOS |
| `POST /admin/tipos-certificado` | ADMIN |
| `PUT /admin/tipos-certificado/{id}` | ADMIN |
| `PATCH /admin/tipos-certificado/{id}/activo` | ADMIN |

#### PazYSalvoController — `/api/paz-y-salvo`

| Endpoint | Rol requerido |
|----------|---------------|
| `GET /mis-solicitudes` | DEPENDENCIA, DIRECTOR |
| `GET /pendientes` | DEPENDENCIA, DIRECTOR |
| `POST /{id}/responder` | DEPENDENCIA, DIRECTOR |
| `GET /solicitud/{solicitudId}` | ESTUDIANTE, DIRECTOR, DEPENDENCIA |
| `GET /estado-estudiantes` | DIRECTOR |

#### CertificadoController — `/api/certificados`

| Endpoint | Rol requerido |
|----------|---------------|
| `GET /` | ESTUDIANTE |
| `POST /solicitar` | ESTUDIANTE |
| `POST /{id}/pagar` | ESTUDIANTE |
| `GET /{id}/pdf` | ESTUDIANTE, DEPENDENCIA |
| `GET /dependencia/{cedula}` | DEPENDENCIA |
| `POST /{id}/marcar-listo` | DEPENDENCIA |
| `POST /{id}/marcar-entregado` | DEPENDENCIA |

#### ConvocatoriaController — `/api/convocatorias`

| Endpoint | Rol requerido |
|----------|---------------|
| `PUT /` | ADMIN, POSGRADOS |

#### Sin restricción de rol (solo autenticación)
- `UsuarioController`, `TramiteController`, `NotificacionController`, `GET /api/certificados/tipos`, `GET /api/convocatorias/activa`

---

## Compatibilidad con plataforma externa (JWT federation)

Cuando el módulo se integre a la plataforma principal, la plataforma emitirá los tokens y este módulo solo los validará. El único cambio necesario será en **`JwtAuthFilter.java` líneas 42–43** — cómo se extraen `cedula` y `rol` del token entrante. Las 36+ anotaciones `@PreAuthorize` en los controllers quedan intactas porque operan sobre las autoridades de Spring Security, no sobre los claim names del JWT.

**Costo de migración: 1 archivo, 2 líneas.**
