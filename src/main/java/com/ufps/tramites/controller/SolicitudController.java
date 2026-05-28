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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @Autowired private SolicitudService solicitudService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private DocumentoService documentoService;
    @Autowired private ValidacionGradoService validacionGradoService;

    /** POST /api/solicitudes/terminacion-materias */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @PostMapping("/terminacion-materias")
    public ResponseEntity<?> crearSolicitudTerminacion(Authentication auth) {
        Usuario estudiante = resolverUsuario(auth);
        if (estudiante == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(solicitudService.crearSolicitudTerminacion(estudiante));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        }
    }

    /** POST /api/solicitudes/grado (multipart) */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @PostMapping(value = "/grado", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crearSolicitudGrado(
            Authentication auth,
            @RequestParam String tituloProyecto,
            @RequestParam String resumen,
            @RequestParam String tipoProyecto,
            @RequestParam MultipartFile foto,
            @RequestParam MultipartFile actaSustentacion,
            @RequestParam(required = false) MultipartFile certificadoIngles) {

        Usuario estudiante = resolverUsuario(auth);
        if (estudiante == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    solicitudService.crearSolicitudGrado(estudiante, tituloProyecto, resumen,
                            tipoProyecto, foto, actaSustentacion, certificadoIngles));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Error al procesar los archivos: " + e.getMessage()));
        }
    }

    /** GET /api/solicitudes */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @GetMapping
    public ResponseEntity<?> obtenerSolicitudes(Authentication auth) {
        Usuario estudiante = resolverUsuario(auth);
        if (estudiante == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        return ResponseEntity.ok(solicitudService.obtenerSolicitudesPorCedula(estudiante.getCedula()));
    }

    /** GET /api/solicitudes/bandeja — bandeja del director */
    @PreAuthorize("hasRole('DIRECTOR')")
    @GetMapping("/bandeja")
    public ResponseEntity<?> obtenerBandeja(Authentication auth) {
        Usuario director = resolverUsuario(auth);
        if (director == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        if (!"DIRECTOR".equals(director.getRolNombre()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido a directores de programa"));
        return ResponseEntity.ok(solicitudService.obtenerBandejaDirector(director));
    }

    /** GET /api/solicitudes/bandeja-grado */
    @PreAuthorize("hasRole('DIRECTOR')")
    @GetMapping("/bandeja-grado")
    public ResponseEntity<?> obtenerBandejaGrado(Authentication auth) {
        Usuario director = resolverUsuario(auth);
        if (director == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        if (!"DIRECTOR".equals(director.getRolNombre()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido a directores de programa"));
        return ResponseEntity.ok(solicitudService.obtenerBandejaGrado(director));
    }

    /** POST /api/solicitudes/{id}/documentos */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @PostMapping(value = "/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirDocumento(@PathVariable Long id, @RequestParam MultipartFile archivo) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(documentoService.guardarDocumento(id, archivo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Error al guardar el archivo: " + e.getMessage()));
        }
    }

    /** GET /api/solicitudes/{id}/documentos */
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DIRECTOR')")
    @GetMapping("/{id}/documentos")
    public ResponseEntity<?> listarDocumentos(@PathVariable Long id) {
        return ResponseEntity.ok(documentoService.listarDocumentos(id));
    }

    /** GET /api/solicitudes/{id}/documentos/{docId}/file */
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DIRECTOR')")
    @GetMapping("/{id}/documentos/{docId}/file")
    public ResponseEntity<byte[]> descargarArchivo(@PathVariable Long id, @PathVariable Long docId) {
        try {
            var resultado = documentoService.obtenerArchivo(id, docId);
            if (resultado == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            return ResponseEntity.ok()
                    .header("Content-Disposition", "inline; filename=\"" + resultado.get("nombreOriginal") + "\"")
                    .contentType(org.springframework.http.MediaType.parseMediaType(
                            resultado.get("contentType") != null ? (String) resultado.get("contentType") : "application/octet-stream"))
                    .body((byte[]) resultado.get("bytes"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /** POST /api/solicitudes/{id}/aprobar */
    @PreAuthorize("hasRole('DIRECTOR')")
    @PostMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobarSolicitud(@PathVariable Long id, Authentication auth) {
        Usuario director = resolverUsuario(auth);
        if (director == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        if (!"DIRECTOR".equals(director.getRolNombre()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido a directores"));
        try {
            return ResponseEntity.ok(solicitudService.aprobarSolicitudConDirector(id, director.getCedula()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        }
    }

    /** POST /api/solicitudes/{id}/rechazar */
    @PreAuthorize("hasRole('DIRECTOR')")
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazarSolicitud(@PathVariable Long id,
            Authentication auth,
            @RequestParam(required = false) String motivo) {
        Usuario director = resolverUsuario(auth);
        if (director == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        if (!"DIRECTOR".equals(director.getRolNombre()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido a directores"));
        try {
            return ResponseEntity.ok(solicitudService.rechazarSolicitud(id, motivo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        }
    }

    /** GET /api/solicitudes/{id}/acta */
    @PreAuthorize("hasAnyRole('DIRECTOR', 'POSGRADOS')")
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
            return ResponseEntity.status(422).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** GET /api/solicitudes/{id}/certificado */
    @PreAuthorize("hasAnyRole('DIRECTOR', 'POSGRADOS')")
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
            return ResponseEntity.status(422).build();
        }
    }

    /** GET /api/solicitudes/posgrados/pendientes */
    @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")
    @GetMapping("/posgrados/pendientes")
    public ResponseEntity<?> obtenerPendientesValidacion(Authentication auth) {
        Usuario admin = resolverUsuario(auth);
        if (admin == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        return ResponseEntity.ok(validacionGradoService.obtenerSolicitudesPendientesValidacion());
    }

    /** POST /api/solicitudes/{id}/validar-grado */
    @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")
    @PostMapping("/{id}/validar-grado")
    public ResponseEntity<?> validarSolicitudGrado(
            @PathVariable Long id,
            Authentication auth,
            @RequestParam String decision,
            @RequestParam(required = false) String observaciones) {
        Usuario admin = resolverUsuario(auth);
        if (admin == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        try {
            return ResponseEntity.ok(validacionGradoService.registrarValidacion(id, decision, observaciones, admin));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        }
    }

    /** POST /api/solicitudes/{id}/pagar-grado */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @PostMapping("/{id}/pagar-grado")
    public ResponseEntity<?> pagarGrado(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(solicitudService.registrarPagoGrado(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        }
    }

    /** POST /api/solicitudes/{id}/fecha-grado */
    @PreAuthorize("hasAnyRole('DIRECTOR', 'POSGRADOS')")
    @PostMapping("/{id}/fecha-grado")
    public ResponseEntity<?> registrarFechaGrado(@PathVariable Long id, @RequestParam String fecha) {
        try {
            return ResponseEntity.ok(solicitudService.registrarFechaGrado(id, java.time.LocalDate.parse(fecha)));
        } catch (java.time.format.DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("Formato de fecha inválido. Use YYYY-MM-DD"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        }
    }

    /** GET /api/solicitudes/posgrados/bandeja */
    @PreAuthorize("hasRole('POSGRADOS')")
    @GetMapping("/posgrados/bandeja")
    public ResponseEntity<?> getBandejaPosgrados(Authentication auth) {
        Usuario u = resolverUsuario(auth);
        if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        if (!"POSGRADOS".equals(u.getRolNombre()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido"));
        return ResponseEntity.ok(solicitudService.getBandejaPosgrados());
    }

    /** GET /api/solicitudes/{id}/certificado-pdf */
    @PreAuthorize("hasRole('POSGRADOS')")
    @GetMapping("/{id}/certificado-pdf")
    public ResponseEntity<byte[]> descargarCertificadoPdf(@PathVariable Long id, Authentication auth) {
        Usuario u = resolverUsuario(auth);
        if (u == null || !"POSGRADOS".equals(u.getRolNombre()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            byte[] pdf = solicitudService.generarCertificadoPdf(id);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"certificado-" + id + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); }
        catch (IllegalStateException e)      { return ResponseEntity.status(422).build(); }
        catch (Exception e)                  { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); }
    }

    /** POST /api/solicitudes/{id}/rechazar-posgrados */
    @PreAuthorize("hasRole('POSGRADOS')")
    @PostMapping("/{id}/rechazar-posgrados")
    public ResponseEntity<?> rechazarPosgrados(@PathVariable Long id,
            Authentication auth, @RequestParam(required = false) String motivo) {
        Usuario u = resolverUsuario(auth);
        if (u == null || !"POSGRADOS".equals(u.getRolNombre()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido"));
        try {
            return ResponseEntity.ok(solicitudService.rechazarSolicitud(id, motivo));
        } catch (IllegalArgumentException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage())); }
        catch (IllegalStateException e)      { return ResponseEntity.status(422).body(error(e.getMessage())); }
    }

    /** POST /api/solicitudes/{id}/aprobar-posgrados */
    @PreAuthorize("hasRole('POSGRADOS')")
    @PostMapping("/{id}/aprobar-posgrados")
    public ResponseEntity<?> aprobarPosgrados(@PathVariable Long id, Authentication auth) {
        Usuario u = resolverUsuario(auth);
        if (u == null || !"POSGRADOS".equals(u.getRolNombre()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido"));
        try {
            return ResponseEntity.ok(solicitudService.aprobarPosgrados(id));
        } catch (IllegalArgumentException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage())); }
        catch (IllegalStateException e)      { return ResponseEntity.status(422).body(error(e.getMessage())); }
    }

    private Usuario resolverUsuario(Authentication auth) {
        if (auth == null) return null;
        return usuarioService.obtenerUsuarioPorCedula(auth.getName());
    }

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", mensaje);
        return map;
    }
}
