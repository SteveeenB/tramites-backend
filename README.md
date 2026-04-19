# Trámites UFPS — Backend

API REST para el sistema de trámites de posgrado de la Universidad Francisco de Paula Santander.

## Tecnologías

- Java 17 · Spring Boot 4.0.5
- Spring Data JPA · H2 (en memoria)
- Spring Mail · SSE (Server-Sent Events)

## Levantar el proyecto

```bash
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8080`.  
Consola H2: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:tramitesdb`, usuario `sa`, sin contraseña).

## Usuarios de prueba

| Nombre | Código | Contraseña | Rol | Correo |
|---|---|---|---|---|
| Juan Perez | 20261001 | 123456 | ESTUDIANTE | juan.perez@ufps.edu.co |
| Laura Gomez | 20261005 | 123456 | ESTUDIANTE | laura.gomez@ufps.edu.co |
| Maria Director | 20261002 | 123456 | DIRECTOR | maria.director@ufps.edu.co |
| Admin User | 20261003 | 123456 | ADMIN | admin@ufps.edu.co |

> Laura Gomez ya tiene una solicitud de terminación de materias **APROBADA** en la base de datos inicial.

---

## Endpoints

### Autenticación

| Método | Ruta | Body | Descripción |
|---|---|---|---|
| POST | `/api/usuarios/login` | `{ "codigo": "20261001", "contrasena": "123456" }` | Inicia sesión |
| GET | `/api/usuarios/me` | — | Usuario de la sesión activa |
| POST | `/api/usuarios/logout` | — | Cierra sesión |

### Solicitudes

| Método | Ruta | Params | Descripción |
|---|---|---|---|
| POST | `/api/solicitudes/terminacion-materias` | `cedula` | Crea solicitud de terminación |
| GET | `/api/solicitudes` | `cedula` | Lista solicitudes del estudiante |
| **PUT** | **`/api/solicitudes/{id}/estado`** | `estado`, `observaciones` | Cambia estado (director/admin) |

**Estados válidos para PUT:** `EN_REVISION` · `APROBADA` · `RECHAZADA`  
**Regla:** si `estado=RECHAZADA`, el parámetro `observaciones` es **obligatorio** (motivo del rechazo).

### Trámites

| Método | Ruta | Params | Descripción |
|---|---|---|---|
| GET | `/api/tramites` | `cedula` o `codigo` | Módulo con sidebar y acciones por rol |
| GET | `/api/tramites/proceso-grado` | `cedula` o `codigo` | Estado del proceso de grado (incluye `certificadoDisponible`) |

### Notificaciones en tiempo real

| Método | Ruta | Params | Descripción |
|---|---|---|---|
| GET | `/api/notificaciones/subscribe` | `cedula` | Stream SSE para actualizaciones en tiempo real |

El frontend conecta con `EventSource` y escucha dos eventos:

- `conectado` — confirma la suscripción.
- `estado-actualizado` — JSON enviado cuando el director cambia el estado:

```json
{
  "solicitudId": 1,
  "tipo": "TERMINACION_MATERIAS",
  "estadoAnterior": "EN_REVISION",
  "estadoNuevo": "APROBADA",
  "observaciones": "...",
  "certificadoDisponible": true
}
```

---

## Flujo de notificaciones

```
Director llama PUT /api/solicitudes/{id}/estado
        │
        ▼
SolicitudService.actualizarEstado()
  └── publica SolicitudEstadoCambiadoEvent
              │
              ├── NotificacionEmailService   →  envía correo al estudiante
              └── NotificacionSseService    →  empuja evento SSE al panel del estudiante
```

---

## Configuración de correo

El archivo `application.properties` apunta por defecto a **MailHog** en `localhost:1025` (SMTP local para desarrollo).

### Opción A — MailHog (recomendado para desarrollo)

```bash
# Instalar con Docker
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

Los correos enviados se visualizan en `http://localhost:8025`.

### Opción B — SMTP real (Gmail, Mailtrap, etc.)

Edita `application.properties`:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=tucuenta@gmail.com
spring.mail.password=tu-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### Modo sin SMTP

Si no hay servidor de correo disponible, la aplicación **no falla**. El contenido del correo se imprime en la consola del servidor con el prefijo `[SIMULACIÓN DE CORREO]`.

---

## Probar el flujo completo

1. Iniciar la aplicación (`./mvnw spring-boot:run`).
2. El estudiante crea una solicitud:
   ```
   POST /api/solicitudes/terminacion-materias?cedula=1098765435
   ```
3. El director aprueba o rechaza:
   ```
   PUT /api/solicitudes/1/estado?estado=APROBADA
   PUT /api/solicitudes/1/estado?estado=RECHAZADA&observaciones=Documentación%20incompleta
   ```
4. El estudiante recibe el evento SSE en su panel (si estaba suscrito) y un correo con el resultado.
5. Si fue aprobado, `certificadoDisponible: true` aparece tanto en el SSE como en `GET /api/solicitudes`.
