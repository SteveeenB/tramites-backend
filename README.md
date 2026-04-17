# Backend - Módulo de Trámites Académicos de Posgrados

## Descripción general

Este backend hace parte del proyecto de clase para la gestión de trámites académicos de posgrados.  
Está desarrollado con Spring Boot y expone una API REST para manejar:

- usuarios del sistema
- trámites académicos por rol
- solicitudes de terminación de materias
- bandeja de revisión para directores
- consulta del estado académico para el proceso de grado

El sistema trabaja con una base de datos H2 en memoria y utiliza sesiones HTTP para mantener el usuario autenticado.

---

## Tecnologías utilizadas

- Java 17
- Spring Boot 4.0.5
- Spring Web MVC
- Spring Data JPA
- Base de datos H2
- PostgreSQL como dependencia preparada para despliegue futuro
- Lombok
- Maven

---

## Estructura del backend

### Paquetes principales

- `controller`: expone los endpoints REST
- `service`: contiene la lógica de negocio
- `repository`: acceso a datos con Spring Data JPA
- `model`: entidades JPA
- `dto`: objetos de transferencia de datos

### Archivos principales

- [backend/pom.xml](c:/AYD/tramites-posgrados/backend/pom.xml)
- [backend/src/main/resources/application.properties](c:/AYD/tramites-posgrados/backend/src/main/resources/application.properties)
- [backend/src/main/resources/data.sql](c:/AYD/tramites-posgrados/backend/src/main/resources/data.sql)

---

## Arquitectura general

El backend sigue una arquitectura por capas:

1. **Controladores**
   - reciben peticiones HTTP
   - validan datos básicos
   - llaman a los servicios

2. **Servicios**
   - aplican las reglas de negocio
   - construyen las respuestas para el frontend
   - validan requisitos académicos y estados de solicitud

3. **Repositorios**
   - consultan y guardan información en la base de datos

4. **Modelos**
   - representan las tablas principales del sistema

5. **DTOs**
   - evitan exponer directamente las entidades en respuestas sensibles

---

## Configuración del proyecto

La aplicación está configurada con:

- base de datos H2 en memoria
- creación y eliminación automática de tablas al iniciar y cerrar
- carga automática de datos iniciales desde `data.sql`
- consola H2 habilitada en `/h2-console`
- cookies de sesión configuradas para integración con el frontend React

### Datos de conexión H2

- URL: `jdbc:h2:mem:tramitesdb`
- usuario: `sa`
- contraseña: vacía

---

## Entidades del sistema

### Usuario

Representa a las personas que interactúan con el sistema.

Campos principales:

- cédula
- código
- nombre
- contraseña
- rol
- créditos aprobados
- programa académico

Roles soportados:

- ESTUDIANTE
- DIRECTOR
- ADMIN

### ProgramaAcadémico

Representa los programas de posgrado.

Campos principales:

- id
- nombre
- tipo
- total de créditos

Tipos:

- DOCTORADO
- MAESTRIA
- ESPECIALIZACION

### Solicitud

Representa un trámite académico registrado en el sistema.

Campos principales:

- id
- cédula del estudiante
- tipo de solicitud
- estado
- fecha de solicitud
- costo
- observaciones

Estados manejados:

- PENDIENTE_PAGO
- EN_REVISION
- APROBADA
- RECHAZADA

Tipos manejados:

- TERMINACION_MATERIAS
- otros tipos previstos para crecimiento del sistema

---

## Repositorios

### UsuarioRepository

Permite consultar usuarios por:

- cédula
- código
- programa académico y rol

### SolicitudRepository

Permite consultar solicitudes por:

- cédula
- cédula y tipo
- lista de cédulas y tipo

---

## Servicios

### UsuarioService

Responsable de:

- autenticar usuarios por código y contraseña
- guardar usuarios
- consultar usuario por cédula
- consultar usuario por código
- obtener el primer usuario disponible

### SolicitudService

Responsable de la lógica de negocio de solicitudes.

Funciones principales:

- crear solicitudes de terminación de materias
- validar créditos aprobados
- validar convocatoria académica
- evitar solicitudes duplicadas
- aprobar solicitudes
- rechazar solicitudes
- obtener solicitudes de un estudiante
- construir la bandeja del director

### TramiteService

Responsable de construir la información del módulo de trámites según el rol del usuario.

Funciones principales:

- construir el módulo principal de trámites
- construir la vista del proceso de grado
- armar el sidebar según el rol
- definir acciones habilitadas por rol
- calcular habilitación de etapas académicas

---

## Controladores REST

### UsuarioController

Ruta base: `/api/usuarios`

#### GET `/me`
Retorna el usuario autenticado a partir de la sesión.

Respuesta esperada:

- 401 si no hay sesión activa
- 404 si el usuario ya no existe
- 200 con la información del usuario si todo está correcto

#### POST `/logout`
Cierra la sesión actual.

---

### TramiteController

Ruta base: `/api/tramites`

#### GET `/`
Devuelve la configuración del módulo de trámites según el usuario consultado.

Parámetros opcionales:

- `cedula`
- `codigo`

Si no se envía ningún parámetro, toma el primer usuario disponible.

#### GET `/proceso-grado`
Devuelve la información del proceso de grado.

Parámetros opcionales:

- `cedula`
- `codigo`

Incluye:

- créditos aprobados
- créditos requeridos
- estado académico
- convocatoria
- habilitación de etapas

---

### SolicitudController

Ruta base: `/api/solicitudes`

#### POST `/terminacion-materias`
Crea una solicitud de terminación de materias.

Parámetro requerido:

- `cedula`

Validaciones:

- que el estudiante exista
- que tenga créditos suficientes
- que esté dentro del período de convocatoria
- que no tenga una solicitud previa del mismo tipo

#### GET `/`
Consulta todas las solicitudes de un estudiante.

Parámetro requerido:

- `cedula`

#### GET `/bandeja`
Devuelve la bandeja de solicitudes para un director.

Parámetro requerido:

- `cedula`

Solo permite acceso a usuarios con rol DIRECTOR.

#### POST `/{id}/aprobar`
Aprueba una solicitud.

Parámetros:

- `id`
- `cedula`

Solo directores pueden ejecutar esta operación.

#### POST `/{id}/rechazar`
Rechaza una solicitud.

Parámetros:

- `id`
- `cedula`
- `motivo` opcional

Solo directores pueden ejecutar esta operación.

---

## Reglas de negocio implementadas

### Para solicitudes de terminación de materias

El sistema solo permite crear la solicitud si:

- el estudiante tiene todos los créditos requeridos
- la fecha actual está dentro de la convocatoria
- no existe otra solicitud activa del mismo tipo

### Para la bandeja del director

El director solo ve solicitudes de estudiantes de su mismo programa académico.

### Para aprobar o rechazar

Solo se pueden aprobar o rechazar solicitudes en estado:

- PENDIENTE_PAGO
- EN_REVISION

---

## Flujo del proceso de grado

El sistema define dos etapas:

### Etapa 1
Se habilita cuando:

- el estudiante tiene los créditos suficientes
- la convocatoria está vigente

### Etapa 2
Se habilita cuando:

- existe una solicitud de terminación de materias
- dicha solicitud está en estado APROBADA

---

## Datos de prueba

El archivo [backend/src/main/resources/data.sql](c:/AYD/tramites-posgrados/backend/src/main/resources/data.sql) carga datos iniciales de:

- programas académicos
- estudiantes
- director
- administrador
- solicitudes de ejemplo

### Usuarios de prueba

- Juan Perez: estudiante con créditos insuficientes
- Laura Gomez: estudiante habilitada
- Pedro Martinez: estudiante con solicitud pendiente
- Carlos Rueda: estudiante con solicitud rechazada
- Maria Director: directora del programa
- Admin User: usuario administrador

Contraseña de prueba para todos:

- `123456`

---

## Criterios de autenticación

El sistema usa sesión HTTP para mantener al usuario autenticado.

- la sesión guarda la cédula del usuario
- el frontend debe enviar credenciales para conservar la sesión
- el endpoint `/api/usuarios/me` permite recuperar el usuario actual

---

## Ejecución del backend

### Requisitos

- Java 17
- Maven
- Node.js solo si también se ejecuta el frontend

### Comando de ejecución

Desde la carpeta del backend:

```powershell
mvn spring-boot:run