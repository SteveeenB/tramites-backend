# Implementación: Bandeja y Detalle de Solicitudes de Grado (TP-45)

## Resumen
Se implementaron los endpoints para que el director del programa pueda listar y visualizar el detalle completo de las solicitudes de grado, incluyendo archivos subidos a Supabase Storage.

## Cambios Realizados

### 1. Backend

#### Nuevos DTOs
- **`SolicitudGradoDetalleDTO.java`** (`backend/src/main/java/com/ufps/tramites/dto/`)
  - Contiene toda la información detallada de una solicitud de grado
  - Incluye clase interna `ArchivoDTO` para la información de archivos
  - Campos: id, tipo, estado, decision, fechas, info estudiante, info proyecto, lista de archivos

- **`SolicitudGradoListDTO.java`** (`backend/src/main/java/com/ufps/tramites/dto/`)
  - Estructura de respuesta para listado agrupado por estado
  - Campos: pendientes, aprobadas, rechazadas (cada uno es lista de Map)

#### Repository
- **`SolicitudRepository.java`**
  - `List<Solicitud> findGradoByEstado(@Param("estado") String estado)`
    - Query JPQL filtrando por tipo='GRADO' y estado específico
    - Ordenado por fechaSolicitud DESC
  - `List<Solicitud> findAllGrado()`
    - Query JPQL para obtener todas las solicitudes de grado
    - Ordenado por fechaSolicitud DESC

#### Service
- **`DocumentoService.java`**
  - Modificado `listarDocumentos()` para incluir: tipo, url, contentType
  - La URL se genera usando `SupabaseStorageService.obtenerUrl()`

- **`SupabaseStorageService.java`**
  - Nuevo método `obtenerUrl(String path)`
    - Retorna URL pública directa al archivo en el bucket
    - Formato: `{supabaseUrl}/storage/v1/object/public/{bucket}/{path}`

#### Controller
- **`SolicitudController.java`**
  - **GET** `/api/solicitudes/bandeja-grado?estado={estado}`
    - Parámetro opcional: estado (PENDIENTE, APROBADA, RECHAZADA)
    - Si no se especifica, devuelve todo
    - Mapea EN_REVISION/PENDIENTE_PAGO → PENDIENTE
    - Retorna para cada solicitud: id, tipo, fechaSolicitud, estado, decision, observacionesDirector, fechaEnRevision, fechaDecision, informacionGrado (titulo, tipo, resumen), estudiante (cedula, nombre, codigo, email), archivos (lista con url)
  
  - **GET** `/api/solicitudes/grado/{id}`
    - Detalle completo de una solicitud de grado específica
    - Valida que sea tipo GRADO
    - Retorna `SolicitudGradoDetalleDTO` con toda la información estructurada
    - Cada archivo incluye: id, tipoArchivo, nombreOriginal, url, tamano, fechaSubida, contentType

  - **Helper privado**: `mapearEstadoGrado(Solicitud s)`
    - Convierte estados internos (EN_REVISION, PENDIENTE_PAGO) → PENDIENTE para el frontend

### 2. Frontend

#### API Client
- **`solicitudesApi.js`**
  - Nuevo método: `getBandejaGrado(estado)`
    - GET /api/solicitudes/bandeja-grado?estado=...
  
  - Nuevo método: `obtenerDetalleGrado(id)`
    - GET /api/solicitudes/grado/{id}

## Ejemplos de Respuesta

### GET /api/solicitudes/bandeja-grado?estado=PENDIENTE
```json
[
  {
    "id": 152,
    "tipo": "GRADO",
    "fechaSolicitud": "2026-04-15",
    "estado": "PENDIENTE",
    "decision": null,
    "observacionesDirector": null,
    "fechaEnRevision": "2026-04-15T10:30:00",
    "fechaDecision": null,
    "informacionGrado": {
      "tituloProyecto": "Sistema de Gestión Inteligente",
      "tipoProyecto": "INVESTIGACION",
      "resumenProyecto": "Proyecto de investigación..."
    },
    "estudiante": {
      "cedula": "1234567890",
      "nombre": "Juan Pérez",
      "codigo": "202012345",
      "email": "juan@ufps.edu.co"
    },
    "archivos": [
      {
        "id": 45,
        "tipo": "FOTO_ESTUDIANTE",
        "nombreOriginal": "foto_juan.jpg",
        "url": "https://.../storage/v1/object/public/.../152/foto.jpg",
        "tamano": 245678,
        "fechaSubida": "2026-04-15T10:35:00.123456",
        "contentType": "image/jpeg"
      }
    ]
  }
]
```

### GET /api/solicitudes/grado/152 (SolicitudGradoDetalleDTO)
```json
{
  "id": 152,
  "tipo": "GRADO",
  "cedula": "1234567890",
  "nombreEstudiante": "Juan Pérez",
  "codigoEstudiante": "202012345",
  "fechaSolicitud": "2026-04-15",
  "estado": "PENDIENTE",
  "decision": null,
  "observacionesDirector": null,
  "fechaDecision": null,
  "fechaEnRevision": "2026-04-15T10:30:00.000+00:00",
  "tituloProyecto": "Sistema de Gestión Inteligente",
  "tipoProyecto": "INVESTIGACION",
  "resumenProyecto": "Proyecto de investigación sobre...",
  "archivos": [
    {
      "id": 45,
      "tipoArchivo": "FOTO_ESTUDIANTE",
      "nombreOriginal": "foto_juan.jpg",
      "url": "https://.../storage/v1/object/public/.../152/foto.jpg",
      "tamano": 245678,
      "fechaSubida": "2026-04-15T10:35:00.123456",
      "contentType": "image/jpeg"
    },
    {
      "id": 46,
      "tipoArchivo": "ACTA_SUSTENTACION",
      "nombreOriginal": "acta_sustentacion.pdf",
      "url": "https://.../storage/v1/object/public/.../152/acta.pdf",
      "tamano": 1234567,
      "fechaSubida": "2026-04-15T10:36:00.000+00:00",
      "contentType": "application/pdf"
    }
  ]
}
```

## Diseño y Convenciones

1. **No modificación de entidades**: Se respetó la restricción de no modificar `Solicitud.java`
2. **Reutilización**: Se usan las tablas existentes (`DocumentoSolicitud`, campos de grado en `Solicitud`)
3. **Nomenclatura coherente**: Se mantiene el estilo camelCase y las convenciones del código existente
4. **Manejo de estados**: Transformación de estados internos (EN_REVISION) a semántica de UI (PENDIENTE)
5. **URLs públicas**: Se usa el endpoint `/storage/v1/object/public/` de Supabase para URLs accesibles sin token
6. **Paginación**: No implementada por ahora (se puede agregar después si se requiere)

## Testing

Para probar los endpoints:

```bash
# Listar todas las solicitudes de grado
curl http://localhost:8080/api/solicitudes/bandeja-grado

# Listar solo pendientes
curl http://localhost:8080/api/solicitudes/bandeja-grado?estado=PENDIENTE

# Detalle específico
curl http://localhost:8080/api/solicitudes/grado/152
```

## Integración Frontend

El frontend puede usar:
- `solicitudesApi.getBandejaGrado(estado)` para la bandeja
- `solicitudesApi.obtenerDetalleGrado(id)` para el modal/detail view
- Los archivos tienen URLs directas: `<img src={archivo.url} />` o `<a href={archivo.url} download>`
