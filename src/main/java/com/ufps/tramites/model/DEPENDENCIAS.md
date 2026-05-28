     Opción B — Tabla Dependencia + FK en Usuario (RECOMENDADA)

     Nueva entidad Dependencia (id, nombre, descripcion, activa). Usuario con rol DEPENDENCIA o POSGRADOS tiene FK → Dependencia. PazYSalvo migra de tipoDependencia: String a FK → Dependencia.

     - Pros: Configurable por el admin, limpio, refleja el negocio, extensible, fácil de mantener
     - Cons: 4 archivos nuevos, actualizar data.sql y PazYSalvo
     
     Plan de Implementación (Opción B)

     Roles finales en BD

     ┌─────────────┬─────────────────────────────────────┬─────────────────────────────────────────────┐
     │     Rol     │          FK dependencia_id          │                 Descripción                 │
     ├─────────────┼─────────────────────────────────────┼─────────────────────────────────────────────┤
     │ ESTUDIANTE  │ null                                │ Inicia solicitudes                          │
     ├─────────────┼─────────────────────────────────────┼─────────────────────────────────────────────┤
     │ DIRECTOR    │ null                                │ Valida Paso 2, referenciado por programa    │
     ├─────────────┼─────────────────────────────────────┼─────────────────────────────────────────────┤
     │ ADMIN       │ null                                │ Administración                              │
     ├─────────────┼─────────────────────────────────────┼─────────────────────────────────────────────┤
     │ DEPENDENCIA │ requerida                           │ Paz y salvos (Biblioteca, Financiera, etc.) │
     ├─────────────┼─────────────────────────────────────┼─────────────────────────────────────────────┤
     │ POSGRADOS   │ requerida (dependencia "Posgrados") │ Paz y salvos + gestión convocatorias        │
     └─────────────┴─────────────────────────────────────┴─────────────────────────────────────────────┘

     ---
     Paso 1 — Crear Dependencia.java

     Archivo: backend/src/main/java/com/ufps/tramites/model/Dependencia.java

     @Entity
     @Table(name = "dependencias")
     public class Dependencia {
         @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
         private Long id;
         private String nombre;       // "Biblioteca", "Financiera", "Admisiones", "Posgrados"
         private String descripcion;
         private Boolean activa = true;
         // getters/setters
     }

     ---
     Paso 2 — Modificar Usuario.java

     Archivo: backend/src/main/java/com/ufps/tramites/model/Usuario.java

     Añadir campo:
     @ManyToOne
     @JoinColumn(name = "dependencia_id")
     private Dependencia dependencia; // null para ESTUDIANTE, DIRECTOR, ADMIN
     Añadir getDependencia() / setDependencia() y getDependenciaNombre() (helper que retorna dependencia != null ? dependencia.getNombre() : null).

     ---
     Paso 3 — Migrar PazYSalvo.java

     Archivo: backend/src/main/java/com/ufps/tramites/model/PazYSalvo.java

     Reemplazar:
     private String tipoDependencia;  // ELIMINAR
     Por:
     @ManyToOne
     @JoinColumn(name = "dependencia_id")
     private Dependencia dependencia;  // FK real
     Mantener todos los demás campos (solicitudId, cedulaEstudiante, cedulaResponsable, estado, observaciones, fechas).

     ---
     Paso 4 — Crear DependenciaRepository.java

     Archivo: backend/src/main/java/com/ufps/tramites/repository/DependenciaRepository.java

     public interface DependenciaRepository extends JpaRepository<Dependencia, Long> {
         List<Dependencia> findByActivaTrue();
         Optional<Dependencia> findByNombre(String nombre);
     }

     ---
     Paso 5 — Crear DependenciaService.java

     Archivo: backend/src/main/java/com/ufps/tramites/service/DependenciaService.java

     Métodos:
     - obtenerTodas() → List<Dependencia> (solo activas)
     - obtenerPorId(Long id) → Dependencia
     - obtenerPorNombre(String nombre) → Dependencia
     - crear(Dependencia d) → Dependencia
     - desactivar(Long id) → void

     ---
     Paso 6 — Crear DependenciaController.java

     Archivo: backend/src/main/java/com/ufps/tramites/controller/DependenciaController.java

     Endpoints:
     - GET /api/dependencias → lista todas las activas (roles: ADMIN, DEPENDENCIA, POSGRADOS)
     - POST /api/dependencias → crea nueva (solo ADMIN)
     - DELETE /api/dependencias/{id} → desactiva (solo ADMIN)

     ---
     Paso 7 — Actualizar UsuarioController.java y LoginResponseDTO/UsuarioResponseDTO

     GET /api/usuarios/me debe incluir dependenciaNombre en la respuesta para que el frontend sepa a qué dependencia pertenece el usuario logueado.

     ---
     Paso 8 — Actualizar data.sql

     Archivo: backend/src/main/resources/data.sql

     -- Dependencias
     INSERT INTO dependencias (nombre, descripcion, activa) VALUES
       ('Biblioteca', 'Biblioteca central UFPS', true),
       ('Financiera', 'División Financiera UFPS', true),
       ('Admisiones', 'Registro y Control Académico', true),
       ('Posgrados', 'Oficina de Posgrados UFPS', true);

     -- Roles (asegurar que POSGRADOS existe)
     INSERT INTO roles (nombre) VALUES ('POSGRADOS') ON CONFLICT DO NOTHING;

     -- Actualizar usuarios DEPENDENCIA existentes con su dependencia_id
     UPDATE usuarios SET dependencia_id = (SELECT id FROM dependencias WHERE nombre = 'Biblioteca')
       WHERE cedula = '<cedula_biblioteca>';
     -- (ídem Financiera, Admisiones)

     -- Usuario POSGRADOS de ejemplo
     INSERT INTO usuarios (..., dependencia_id) VALUES
       (..., (SELECT id FROM dependencias WHERE nombre = 'Posgrados'));

     ---
     Paso 9 — Verificar servicios que usan tipoDependencia

     Buscar en SolicitudService.java y cualquier otro servicio que referenciaba PazYSalvo.getTipoDependencia() como String y actualizar a getPazYSalvo().getDependencia().getNombre().

     ---
     Archivos críticos a modificar

     ┌───────────────────────────────────────┬─────────────────────────────────────────┐
     │                Archivo                │             Tipo de cambio              │
     ├───────────────────────────────────────┼─────────────────────────────────────────┤
     │ model/Dependencia.java                │ CREAR                                   │
     ├───────────────────────────────────────┼─────────────────────────────────────────┤
     │ model/Usuario.java                    │ Añadir FK dependencia                   │
     ├───────────────────────────────────────┼─────────────────────────────────────────┤
     │ model/PazYSalvo.java                  │ Migrar String → FK                      │
     ├───────────────────────────────────────┼─────────────────────────────────────────┤
     │ repository/DependenciaRepository.java │ CREAR                                   │
     ├───────────────────────────────────────┼─────────────────────────────────────────┤
     │ service/DependenciaService.java       │ CREAR                                   │
     ├───────────────────────────────────────┼─────────────────────────────────────────┤
     │ controller/DependenciaController.java │ CREAR                                   │
     ├───────────────────────────────────────┼─────────────────────────────────────────┤
     │ controller/UsuarioController.java     │ Añadir dependenciaNombre en response    │
     ├───────────────────────────────────────┼─────────────────────────────────────────┤
     │ resources/data.sql                    │ Seed dependencias + actualizar usuarios │
     ├───────────────────────────────────────┼─────────────────────────────────────────┤
     │ Servicios que usen tipoDependencia    │ Actualizar referencias                  │
     └───────────────────────────────────────┴─────────────────────────────────────────┘

     ---
     Verificación

     1. Levantar backend (mvn spring-boot:run) → H2 debe crear tabla dependencias sin errores
     2. GET /api/dependencias → retorna lista con Biblioteca, Financiera, Admisiones, Posgrados
     3. Login con usuario DEPENDENCIA → response incluye dependenciaNombre: "Biblioteca"
     4. Login con usuario POSGRADOS → response incluye dependenciaNombre: "Posgrados" y rol POSGRADOS
     5. POST /api/dependencias con ADMIN → crea nueva dependencia dinámica
     6. Verificar que PazYSalvo existentes en BD se crean con dependencia_id (no tipoDependencia null)