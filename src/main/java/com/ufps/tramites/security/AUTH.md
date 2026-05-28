  ---
  Resumen de lo implementado

  Backend — Modelo de datos (Fase 0)

  - Rol.java — nueva entidad que mapea la tabla roles (id, nombre)
  - Usuario.java — rediseñado: PK es ahora id (Long), rol es @ManyToOne → Rol, se añadieron email, googleId, fotoUrl, moodleId, telefono. Helpers de compatibilidad: getNombre(), getCorreo(), getRolNombre()
  - UsuarioRepository — usa findByCedula(), findByEmail(), findByGoogleId(), findByRolNombre() en lugar del viejo findById(String)
  - data.sql — inserta primero los roles (1-5), luego los usuarios con el nuevo esquema
  - Todos los controllers y servicios actualizados: getRol() → getRolNombre(), findByRol → findByRolNombre

  Backend — Autenticación JWT (Fase 1)

  - JwtService.java — genera/valida tokens HS256 con claims: sub=id, cedula, rol, nombreCompleto, email
  - JwtAuthFilter.java — lee el header Authorization: Bearer, valida el token, puebla el SecurityContext
  - SecurityConfig.java — Spring Security stateless, CORS desde CorsConfig, rutas públicas: /api/auth/**, /swagger-ui/**, SSE
  - AuthController.java — tres endpoints: POST /api/auth/login (código+contraseña), POST /api/auth/google (id_token de Google), POST /api/auth/login-demo (solo dev)
  - CorsConfig.java — convertido a CorsConfigurationSource bean para Spring Security

  Frontend (Fase 2)

  - AuthContext.js — reemplazado completamente: restaura sesión desde localStorage, login(), logout(), cambiarRol() llama a /api/auth/login-demo
  - apiClient.js — añade Authorization: Bearer, elimina credentials: 'include', maneja 401 redirigiendo a /login
  - Login.jsx — página nueva con login por código/contraseña + botón Google OAuth
  - solicitudesApi.js, tramitesApi.js, pazYSalvoApi.js — eliminados todos los ?cedula=
  - ProtectedRoute.js — redirige a /login si no hay sesión
  - Todos los hooks y páginas actualizados para no pasar cedula a las funciones de API

  Para activar el modo demo en local: cambia spring.profiles.active=local a spring.profiles.active=local,dev en application.properties.

  spring.profiles.active=local,dev

  Spring Boot aplica las propiedades en capas, de menor a mayor prioridad:

  application.properties          ← base
  application-local.properties    ← sobrescribe con credenciales locales
  application-dev.properties      ← sobrescribe con configuración de desarrollo

  Con local,dev activos al mismo tiempo:
  - local aporta las credenciales de Supabase (URL, usuario, password) — ese archivo no se sube al repo por .gitignore
  - dev aporta demo.auth.enabled=true, que sobrescribe el false del application.properties base y habilita el endpoint /api/auth/login-demo

   Plan: @PreAuthorize + BCrypt password hashing

 Context

 El backend actualmente no protege sus endpoints por rol — solo verifica que el usuario esté autenticado. Cualquiera con un JWT válido (incluso de ESTUDIANTE) puede llamar endpoints de DIRECTOR o ADMIN
 directamente desde Postman. Adicionalmente, las contraseñas en data.sql están en texto plano y AuthController acepta texto plano como fallback. El objetivo es agregar protección real por rol en el backend y
 adoptar BCrypt como estándar de almacenamiento de contraseñas.

 ---
 Orden de ejecución (importa)

 1. SecurityConfig.java       → habilitar method security (aditivo, no rompe nada)
 2. Todos los controllers     → agregar @PreAuthorize
 3. UsuarioService.java       → encodear contraseña al guardar
 4. data.sql                  → reemplazar '123456' con hash BCrypt
 5. [DB] UPDATE manual        → actualizar usuarios ya existentes en Supabase
 6. AuthController.java       → eliminar fallback de texto plano

 No invertir 4 y 6: si se elimina el fallback antes de actualizar la DB, los usuarios actuales quedan bloqueados.

 ---
 Paso 1 — SecurityConfig.java

 config/SecurityConfig.java — agregar anotación en la clase:

 @Configuration
 @EnableWebSecurity
 @EnableMethodSecurity(prePostEnabled = true)   // ← agregar
 public class SecurityConfig {

 Import: org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

 ---
 Paso 2 — @PreAuthorize en controllers

 Import a agregar en cada controller: org.springframework.security.access.prepost.PreAuthorize

 Regla sintáctica: hasRole('DIRECTOR') → busca autoridad ROLE_DIRECTOR (el prefijo lo agrega Spring automáticamente). Nunca escribir hasRole('ROLE_DIRECTOR').

 SolicitudController (19 anotaciones)

 ┌───────────────────────────────────┬───────────────────────────────────────────────────────┐
 │             Endpoint              │                       Anotación                       │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ POST /terminacion-materias        │ @PreAuthorize("hasRole('ESTUDIANTE')")                │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ POST /grado                       │ @PreAuthorize("hasRole('ESTUDIANTE')")                │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ GET /                             │ @PreAuthorize("hasRole('ESTUDIANTE')")                │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ GET /bandeja                      │ @PreAuthorize("hasRole('DIRECTOR')")                  │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ GET /bandeja-grado                │ @PreAuthorize("hasRole('DIRECTOR')")                  │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ POST /{id}/documentos             │ @PreAuthorize("hasRole('ESTUDIANTE')")                │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ GET /{id}/documentos              │ @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DIRECTOR')") │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ GET /{id}/documentos/{docId}/file │ @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DIRECTOR')") │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ POST /{id}/aprobar                │ @PreAuthorize("hasRole('DIRECTOR')")                  │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ POST /{id}/rechazar               │ @PreAuthorize("hasRole('DIRECTOR')")                  │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ GET /{id}/acta                    │ @PreAuthorize("hasAnyRole('DIRECTOR', 'POSGRADOS')")  │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ GET /{id}/certificado             │ @PreAuthorize("hasAnyRole('DIRECTOR', 'POSGRADOS')")  │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ GET /posgrados/pendientes         │ @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")     │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ POST /{id}/validar-grado          │ @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")     │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ POST /{id}/pagar-grado            │ @PreAuthorize("hasRole('ESTUDIANTE')")                │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ POST /{id}/fecha-grado            │ @PreAuthorize("hasAnyRole('DIRECTOR', 'POSGRADOS')")  │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ GET /posgrados/bandeja            │ @PreAuthorize("hasRole('POSGRADOS')")                 │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ GET /{id}/certificado-pdf         │ @PreAuthorize("hasRole('POSGRADOS')")                 │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ POST /{id}/rechazar-posgrados     │ @PreAuthorize("hasRole('POSGRADOS')")                 │
 ├───────────────────────────────────┼───────────────────────────────────────────────────────┤
 │ POST /{id}/aprobar-posgrados      │ @PreAuthorize("hasRole('POSGRADOS')")                 │
 └───────────────────────────────────┴───────────────────────────────────────────────────────┘

 AdminController (5 anotaciones)

 ┌────────────────────────────────────────────┬───────────────────────────────────────────────────┐
 │                  Endpoint                  │                     Anotación                     │
 ├────────────────────────────────────────────┼───────────────────────────────────────────────────┤
 │ GET /dependencias                          │ @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')") │
 ├────────────────────────────────────────────┼───────────────────────────────────────────────────┤
 │ GET /admin/tipos-certificado               │ @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')") │
 ├────────────────────────────────────────────┼───────────────────────────────────────────────────┤
 │ POST /admin/tipos-certificado              │ @PreAuthorize("hasRole('ADMIN')")                 │
 ├────────────────────────────────────────────┼───────────────────────────────────────────────────┤
 │ PUT /admin/tipos-certificado/{id}          │ @PreAuthorize("hasRole('ADMIN')")                 │
 ├────────────────────────────────────────────┼───────────────────────────────────────────────────┤
 │ PATCH /admin/tipos-certificado/{id}/activo │ @PreAuthorize("hasRole('ADMIN')")                 │
 └────────────────────────────────────────────┴───────────────────────────────────────────────────┘

 PazYSalvoController (5 anotaciones)

 ┌──────────────────────────────┬──────────────────────────────────────────────────────────────────────┐
 │           Endpoint           │                              Anotación                               │
 ├──────────────────────────────┼──────────────────────────────────────────────────────────────────────┤
 │ GET /mis-solicitudes         │ @PreAuthorize("hasAnyRole('DEPENDENCIA', 'DIRECTOR')")               │
 ├──────────────────────────────┼──────────────────────────────────────────────────────────────────────┤
 │ GET /pendientes              │ @PreAuthorize("hasAnyRole('DEPENDENCIA', 'DIRECTOR')")               │
 ├──────────────────────────────┼──────────────────────────────────────────────────────────────────────┤
 │ POST /{id}/responder         │ @PreAuthorize("hasAnyRole('DEPENDENCIA', 'DIRECTOR')")               │
 ├──────────────────────────────┼──────────────────────────────────────────────────────────────────────┤
 │ GET /solicitud/{solicitudId} │ @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DIRECTOR', 'DEPENDENCIA')") │
 ├──────────────────────────────┼──────────────────────────────────────────────────────────────────────┤
 │ GET /estado-estudiantes      │ @PreAuthorize("hasRole('DIRECTOR')")                                 │
 └──────────────────────────────┴──────────────────────────────────────────────────────────────────────┘

 CertificadoController (6 anotaciones)

 ┌─────────────────────────────┬──────────────────────────────────────────────────────────┐
 │          Endpoint           │                        Anotación                         │
 ├─────────────────────────────┼──────────────────────────────────────────────────────────┤
 │ GET /                       │ @PreAuthorize("hasRole('ESTUDIANTE')")                   │
 ├─────────────────────────────┼──────────────────────────────────────────────────────────┤
 │ POST /solicitar             │ @PreAuthorize("hasRole('ESTUDIANTE')")                   │
 ├─────────────────────────────┼──────────────────────────────────────────────────────────┤
 │ POST /{id}/pagar            │ @PreAuthorize("hasRole('ESTUDIANTE')")                   │
 ├─────────────────────────────┼──────────────────────────────────────────────────────────┤
 │ GET /{id}/pdf               │ @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DEPENDENCIA')") │
 ├─────────────────────────────┼──────────────────────────────────────────────────────────┤
 │ GET /dependencia/{cedula}   │ @PreAuthorize("hasRole('DEPENDENCIA')")                  │
 ├─────────────────────────────┼──────────────────────────────────────────────────────────┤
 │ POST /{id}/marcar-listo     │ @PreAuthorize("hasRole('DEPENDENCIA')")                  │
 ├─────────────────────────────┼──────────────────────────────────────────────────────────┤
 │ POST /{id}/marcar-entregado │ @PreAuthorize("hasRole('DEPENDENCIA')")                  │
 └─────────────────────────────┴──────────────────────────────────────────────────────────┘

 ConvocatoriaController (1 anotación)

 ┌──────────┬───────────────────────────────────────────────────┐
 │ Endpoint │                     Anotación                     │
 ├──────────┼───────────────────────────────────────────────────┤
 │ PUT /    │ @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')") │
 └──────────┴───────────────────────────────────────────────────┘

 Sin cambios

 - UsuarioController, TramiteController, NotificacionController → solo requieren autenticación, el filtro chain ya lo cubre.

 ---
 Paso 3 — UsuarioService.java

 Agregar PasswordEncoder y guard de encodeo en guardarUsuario():

 @Autowired
 private PasswordEncoder passwordEncoder;

 public Usuario guardarUsuario(Usuario usuario) {
     if (usuario.getPassword() != null && !usuario.getPassword().startsWith("$2a$")) {
         usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
     }
     return usuarioRepository.save(usuario);
 }

 El guard !startsWith("$2a$") evita doble-encodeo si el método es llamado sobre un usuario que ya tiene hash.

 ---
 Paso 4 — data.sql

 Reemplazar todas las ocurrencias de '123456' en columna contrasena con:

 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LbZibmznP02'

 Son 14 ocurrencias en 6 bloques INSERT. Usar replace_all=true en el editor.

 ---
 Paso 5 — UPDATE en Supabase (usuarios ya existentes)

 Como los INSERTs usan ON CONFLICT DO NOTHING, el data.sql actualizado solo aplica a deployments frescos. Para la DB actual ejecutar en el editor SQL de Supabase:

 UPDATE usuario
 SET contrasena = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LbZibmznP02'
 WHERE contrasena = '123456';

 ---
 Paso 6 — AuthController.java

 Eliminar el fallback de texto plano (líneas 59-61). Reemplazar:

 // Soporta contraseñas en texto plano (data.sql demo) y hasheadas con BCrypt
 boolean passwordValida = usuario.getPassword() != null && (
         passwordEncoder.matches(request.getContrasena(), usuario.getPassword())
         || request.getContrasena().equals(usuario.getPassword())
 );

 Por:

 boolean passwordValida = usuario.getPassword() != null &&
         passwordEncoder.matches(request.getContrasena(), usuario.getPassword());

 ---
 Verificación

 1. POST /api/auth/login con código 20261002 y contraseña 123456 → debe devolver 200 + JWT
 2. Mismo endpoint con contraseña incorrecta → 401
 3. GET /api/solicitudes/bandeja con token de DIRECTOR → 200
 4. GET /api/solicitudes/bandeja con token de ESTUDIANTE → 403
 // Soporta contraseñas en texto plano (data.sql demo) y hasheadas con BCrypt
 boolean passwordValida = usuario.getPassword() != null && (
         passwordEncoder.matches(request.getContrasena(), usuario.getPassword())
         || request.getContrasena().equals(usuario.getPassword())
 );

 Por:

 boolean passwordValida = usuario.getPassword() != null &&
         passwordEncoder.matches(request.getContrasena(), usuario.getPassword());

 ---
 ---
 Paso 4 — data.sql

 Reemplazar todas las ocurrencias de '123456' en columna contrasena con:

 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LbZibmznP02'

 Son 14 ocurrencias en 6 bloques INSERT. Usar replace_all=true en el editor.

 ---
 Paso 5 — UPDATE en Supabase (usuarios ya existentes)

 Como los INSERTs usan ON CONFLICT DO NOTHING, el data.sql actualizado solo aplica a deployments frescos. Para la DB actual ejecutar en el
 editor SQL de Supabase:

 UPDATE usuario
 SET contrasena = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LbZibmznP02'
 WHERE contrasena = '123456';

 ---
 Paso 6 — AuthController.java

 Eliminar el fallback de texto plano (líneas 59-61). Reemplazar:

 // Soporta contraseñas en texto plano (data.sql demo) y hasheadas con BCrypt
 Son 14 ocurrencias en 6 bloques INSERT. Usar replace_all=true en el editor.

 ---
 Paso 5 — UPDATE en Supabase (usuarios ya existentes)

 Como los INSERTs usan ON CONFLICT DO NOTHING, el data.sql actualizado solo aplica a deployments frescos. Para la DB actual ejecutar en el editor SQL de Supabase:

 UPDATE usuario
 SET contrasena = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LbZibmznP02'
 WHERE contrasena = '123456';

 ---
 Paso 6 — AuthController.java

 Eliminar el fallback de texto plano (líneas 59-61). Reemplazar:

 // Soporta contraseñas en texto plano (data.sql demo) y hasheadas con BCrypt
 boolean passwordValida = usuario.getPassword() != null && (
         passwordEncoder.matches(request.getContrasena(), usuario.getPassword())
         || request.getContrasena().equals(usuario.getPassword())
 );

 Por:

 boolean passwordValida = usuario.getPassword() != null &&
         passwordEncoder.matches(request.getContrasena(), usuario.getPassword());

 ---
 Verificación

 1. POST /api/auth/login con código 20261002 y contraseña 123456 → debe devolver 200 + JWT
 2. Mismo endpoint con contraseña incorrecta → 401
 3. GET /api/solicitudes/bandeja con token de DIRECTOR → 200
 4. GET /api/solicitudes/bandeja con token de ESTUDIANTE → 403
 5. GET /api/solicitudes con token de DIRECTOR → 403
 6. POST /api/auth/login sin Authorization header → 200 (endpoint público no bloqueado)

 --- 
 Nota sobre compatibilidad de JWT claims

 Los @PreAuthorize usan autoridades de Spring Security (ROLE_X), no los claim names del JWT. Cuando se integre con la plataforma real, solo cambia JwtAuthFilter.java líneas 42-43 (cómo se extraen cedula y rol del token). Las 36+ anotaciones @PreAuthorize quedan
 intactas. Costo de migración: 1 archivo, 2 líneas.

  36 anotaciones @PreAuthorize agregadas en 5 controllers — el backend ahora rechaza con 403 cualquier llamada a un endpoint con rol incorrecto, aunque el token sea válido.

  BCrypt activado — UsuarioService ahora encripta la contraseña antes de guardar cualquier usuario nuevo. data.sql tiene los hashes precomputados para los datos de prueba.

  Fallback texto plano eliminado — AuthController ya solo acepta contraseñas BCrypt.

  Paso pendiente (manual): Ejecutar esto en el editor SQL de Supabase para actualizar los usuarios que ya existen en la DB:

  UPDATE usuario
  SET contrasena = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LbZibmznP02'
  WHERE contrasena = '123456';

  Sin ese UPDATE, los usuarios existentes no podrán iniciar sesión hasta que lo ejecutes.
