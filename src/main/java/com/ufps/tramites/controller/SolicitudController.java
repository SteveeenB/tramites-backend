package com.ufps.tramites.controller;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.service.DocumentoService;
import com.ufps.tramites.service.SolicitudService;
import com.ufps.tramites.service.UsuarioService;
import com.ufps.tramites.service.ValidacionGradoService;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController

@RequestMapping("/api/solicitudes")
public class SolicitudController {

    @Autowired
    private SolicitudService solicitudService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private DocumentoService documentoService;

    @Autowired
    private ValidacionGradoService validacionGradoService;

    /**
     * POST /api/solicitudes/terminacion-materias?cedula=...
     * Crea una solicitud de terminación de materias para el estudiante.
     */
    @PostMapping("/terminacion-materias")
    public ResponseEntity<?> crearSolicitudTerminacion(
            @RequestParam String cedula) {

        Usuario estudiante = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (estudiante == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Estudiante no encontrado"));
        }

        try {
            Map<String, Object> resultado = solicitudService.crearSolicitudTerminacion(estudiante);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        }
    }

    /**
     * POST /api/solicitudes/grado (multipart/form-data)
     * Crea una solicitud de grado con detalle académico y documentos adjuntos.
     */
    @PostMapping(value = "/grado", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crearSolicitudGrado(
            @RequestParam String cedula,
            @RequestParam String tituloProyecto,
            @RequestParam String resumen,
            @RequestParam String tipoProyecto,
            @RequestParam MultipartFile foto,
            @RequestParam MultipartFile actaSustentacion,
            @RequestParam(required = false) MultipartFile certificadoIngles) {

        Usuario estudiante = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (estudiante == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Estudiante no encontrado"));
        }
        try {
            Map<String, Object> resultado = solicitudService.crearSolicitudGrado(
                    estudiante, tituloProyecto, resumen, tipoProyecto,
                    foto, actaSustentacion, certificadoIngles);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Error al procesar los archivos: " + e.getMessage()));
        }
    }

    /**
     * GET /api/solicitudes?cedula=...
     * Retorna todas las solicitudes del estudiante.
     */
    @GetMapping
    public ResponseEntity<?> obtenerSolicitudes(@RequestParam String cedula) {
        Usuario estudiante = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (estudiante == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Estudiante no encontrado"));
        }
        return ResponseEntity.ok(solicitudService.obtenerSolicitudesPorCedula(cedula));
    }

    /**
     * GET /api/solicitudes/bandeja?cedula=...
     * Retorna la bandeja del director: solicitudes de su programa agrupadas por estado.
     */
    @GetMapping("/bandeja")
    public ResponseEntity<?> obtenerBandeja(@RequestParam String cedula) {
        Usuario director = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (director == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Director no encontrado"));
        }
        if (!"DIRECTOR".equals(director.getRol())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido a directores de programa"));
        }
        return ResponseEntity.ok(solicitudService.obtenerBandejaDirector(director));
    }

    /**
     * GET /api/solicitudes/bandeja-grado?cedula=...
     * Retorna la bandeja del director: solicitudes de GRADO de su programa agrupadas por estado.
     */
    @GetMapping("/bandeja-grado")
    public ResponseEntity<?> obtenerBandejaGrado(@RequestParam String cedula) {
        Usuario director = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (director == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Director no encontrado"));
        }
        if (!"DIRECTOR".equals(director.getRol())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido a directores de programa"));
        }
        return ResponseEntity.ok(solicitudService.obtenerBandejaGrado(director));
    }

    /**
     * POST /api/solicitudes/{id}/documentos
     * Sube un archivo de soporte para la solicitud.
     */
    @PostMapping(value = "/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirDocumento(
            @PathVariable Long id,
            @RequestParam MultipartFile archivo) {
        try {
            Map<String, Object> resultado = documentoService.guardarDocumento(id, archivo);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Error al guardar el archivo: " + e.getMessage()));
        }
    }

    /**
     * GET /api/solicitudes/{id}/documentos
     * Lista los documentos subidos para una solicitud.
     */
    @GetMapping("/{id}/documentos")
    public ResponseEntity<?> listarDocumentos(@PathVariable Long id) {
        return ResponseEntity.ok(documentoService.listarDocumentos(id));
    }

    /**
     * GET /api/solicitudes/{id}/documentos/{docId}/file
     * Descarga un archivo desde Supabase Storage y lo transmite al cliente.
     * Funciona con buckets privados usando el service-role-key.
     */
    @GetMapping("/{id}/documentos/{docId}/file")
    public ResponseEntity<byte[]> descargarArchivo(
            @PathVariable Long id,
            @PathVariable Long docId) {
        try {
            var resultado = documentoService.obtenerArchivo(id, docId);
            if (resultado == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            String nombreOriginal = (String) resultado.get("nombreOriginal");
            String contentType    = (String) resultado.get("contentType");
            byte[] bytes          = (byte[]) resultado.get("bytes");

            return ResponseEntity.ok()
                    .header("Content-Disposition", "inline; filename=\"" + nombreOriginal + "\"")
                    .contentType(org.springframework.http.MediaType.parseMediaType(
                            contentType != null ? contentType : "application/octet-stream"))
                    .body(bytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /** POST /api/solicitudes/{id}/aprobar?cedula=... */
    @PostMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobarSolicitud(@PathVariable Long id, @RequestParam String cedula) {
        Usuario director = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (director == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Director no encontrado"));
        if (!"DIRECTOR".equals(director.getRol())) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido a directores"));
        try {
            return ResponseEntity.ok(solicitudService.aprobarSolicitudConDirector(id, cedula));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        }
    }

    /** POST /api/solicitudes/{id}/rechazar?cedula=...&motivo=... */
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazarSolicitud(@PathVariable Long id, @RequestParam String cedula,
            @RequestParam(required = false) String motivo) {
        Usuario director = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (director == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Director no encontrado"));
        if (!"DIRECTOR".equals(director.getRol())) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido a directores"));
        try {
            return ResponseEntity.ok(solicitudService.rechazarSolicitud(id, motivo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        }
    }

    /**
     * GET /api/solicitudes/{id}/acta
     * Genera (o descarga) el acta de grado en PDF.
     * Solo disponible cuando la solicitud es de tipo GRADO y está APROBADA.
     * En la primera llamada actualiza al estudiante como GRADUADO y vincula el PDF al expediente.
     */
    @GetMapping("/{id}/acta")
    public ResponseEntity<byte[]> descargarActa(@PathVariable Long id) {
        try {
            byte[] contenido = solicitudService.generarActa(id);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"acta-grado-" + id + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(contenido);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/solicitudes/{id}/certificado
     * Descarga el certificado de terminacion de materias.
     * Solo disponible cuando la solicitud esta APROBADA y es de tipo TERMINACION_MATERIAS.
     */
    @GetMapping("/{id}/certificado")
    public ResponseEntity<byte[]> descargarCertificado(@PathVariable Long id) {
        try {
            byte[] contenido = solicitudService.generarCertificado(id);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"certificado-terminacion-" + id + ".txt\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(contenido);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).build();
        }
    }

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", mensaje);
        return map;
    }

   @GetMapping("/posgrados/pendientes")
public ResponseEntity<?> obtenerPendientesValidacion(@RequestParam String cedula) {
    Usuario admin = usuarioService.obtenerUsuarioPorCedula(cedula);
    if (admin == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error("Usuario no encontrado"));
    }
    if (!"ADMIN".equals(admin.getRol())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error("Acceso restringido al administrador"));
    }
    return ResponseEntity.ok(validacionGradoService.obtenerSolicitudesPendientesValidacion());
}

@PostMapping("/{id}/validar-grado")
public ResponseEntity<?> validarSolicitudGrado(
        @PathVariable Long id,
        @RequestParam String cedula,
        @RequestParam String decision,
        @RequestParam(required = false) String observaciones) {

    Usuario admin = usuarioService.obtenerUsuarioPorCedula(cedula);
    if (admin == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error("Usuario no encontrado"));
    }
    if (!"ADMIN".equals(admin.getRol())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error("Acceso restringido al administrador"));
    }
    try {
        return ResponseEntity.ok(
            validacionGradoService.registrarValidacion(id, decision, observaciones, admin)
        );
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
    } catch (IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
    }
}
/**
 * POST /api/solicitudes/{id}/pagar-grado
 * Registra el pago demo de derechos de grado.
 */
@PostMapping("/{id}/pagar-grado")
public ResponseEntity<?> pagarGrado(@PathVariable Long id) {
    try {
        return ResponseEntity.ok(solicitudService.registrarPagoGrado(id));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
    } catch (IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
    }
}

/**
 * POST /api/solicitudes/{id}/fecha-grado?fecha=2026-08-15
 * Registra la fecha de grado elegida por el estudiante.
 */
@PostMapping("/{id}/fecha-grado")
public ResponseEntity<?> registrarFechaGrado(
        @PathVariable Long id,
        @RequestParam String fecha) {
    try {
        java.time.LocalDate fechaGrado = java.time.LocalDate.parse(fecha);
        return ResponseEntity.ok(solicitudService.registrarFechaGrado(id, fechaGrado));
    } catch (java.time.format.DateTimeParseException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("Formato de fecha inválido. Use YYYY-MM-DD"));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
    } catch (IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
    }
}

}
