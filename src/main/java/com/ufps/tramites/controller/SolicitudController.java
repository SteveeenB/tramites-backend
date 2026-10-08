package com.ufps.tramites.controller;
import com.ufps.tramites.security.PrincipalResolver;
import com.ufps.tramites.security.ResolvedPrincipal;
import com.ufps.tramites.service.DocumentoService;
import com.ufps.tramites.service.SolicitudService;
import com.ufps.tramites.service.ValidacionGradoService;
import java.io.IOException;
import java.util.LinkedHashMap;
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
    @Autowired private PrincipalResolver principalResolver;
    @Autowired private DocumentoService documentoService;
    @Autowired private ValidacionGradoService validacionGradoService;

    /** POST /api/solicitudes/terminacion-materias */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @PostMapping("/terminacion-materias")
    public ResponseEntity<?> crearSolicitudTerminacion(Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null || !p.isUsuario()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(solicitudService.crearSolicitudTerminacion(p.usuario()));
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

        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null || !p.isUsuario()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    solicitudService.crearSolicitudGrado(p.usuario(), tituloProyecto, resumen,
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
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null || !p.isUsuario()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        return ResponseEntity.ok(solicitudService.obtenerSolicitudesPorCedula(p.cedula()));
    }

    /** GET /api/solicitudes/bandeja — bandeja del director */
    // FIX TP-201 (Johan Bueno, 07/10/2026): acepta filtros estado, desde,
    // hasta y cedulaEstudiante. Antes el controller ignoraba cualquier
    // parámetro (CP-048).
    @PreAuthorize("hasRole('DIRECTOR')")
    @GetMapping("/bandeja")
    public ResponseEntity<?> obtenerBandeja(Authentication auth,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate desde,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate hasta,
            @RequestParam(required = false) String cedulaEstudiante) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null || !p.isUsuario()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        return ResponseEntity.ok(solicitudService.obtenerBandejaDirector(
                p.usuario(), estado, desde, hasta, cedulaEstudiante));
    }

    /** GET /api/solicitudes/{id}/historial — línea de tiempo de cambios de estado. */
    // FIX TP-201 (Johan Bueno, 07/10/2026): expone el historial para el
    // detalle de la solicitud en la UI del Director/Posgrados (CP-046).
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/historial")
    public ResponseEntity<?> obtenerHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.obtenerHistorial(id));
    }

    /** GET /api/solicitudes/bandeja-grado */
    @PreAuthorize("hasRole('DIRECTOR')")
    @GetMapping("/bandeja-grado")
    public ResponseEntity<?> obtenerBandejaGrado(Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null || !p.isUsuario()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        return ResponseEntity.ok(solicitudService.obtenerBandejaGrado(p.usuario()));
    }

    /** POST /api/solicitudes/{id}/documentos */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @PostMapping(value = "/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirDocumento(@PathVariable Long id, @RequestParam MultipartFile archivo, Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null || !p.isUsuario()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        if (!solicitudService.perteneceAEstudiante(id, p.cedula()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("No tiene acceso a esta solicitud"));
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
    public ResponseEntity<?> listarDocumentos(@PathVariable Long id, Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null || !p.isUsuario()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        if ("ESTUDIANTE".equals(p.rol()) && !solicitudService.perteneceAEstudiante(id, p.cedula()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("No tiene acceso a esta solicitud"));
        return ResponseEntity.ok(documentoService.listarDocumentos(id));
    }

    /** GET /api/solicitudes/{id}/documentos/{docId}/file */
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DIRECTOR')")
    @GetMapping("/{id}/documentos/{docId}/file")
    public ResponseEntity<byte[]> descargarArchivo(@PathVariable Long id, @PathVariable Long docId, Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null || !p.isUsuario()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if ("ESTUDIANTE".equals(p.rol()) && !solicitudService.perteneceAEstudiante(id, p.cedula()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            var resultado = documentoService.obtenerArchivo(id, docId);
            if (resultado == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            // FIX (Diego Bermudez, dd/mm/aaaa): guard con instanceof — el chequeo anterior solo validaba null,
            // no el tipo, y podía lanzar ClassCastException si "contentType" no era String
            Object ct = resultado.get("contentType");
            String contentType = (ct instanceof String) ? (String) ct : "application/octet-stream";
            return ResponseEntity.ok()
                    .header("Content-Disposition", "inline; filename=\"" + resultado.get("nombreOriginal") + "\"")
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .body((byte[]) resultado.get("bytes"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /** POST /api/solicitudes/{id}/aprobar */
    @PreAuthorize("hasRole('DIRECTOR')")
    @PostMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobarSolicitud(@PathVariable Long id, Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null || !p.isUsuario()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        try {
            return ResponseEntity.ok(solicitudService.aprobarSolicitudConDirector(id, p.cedula()));
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
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null || !p.isUsuario()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        try {
            // FIX TP-161 (Diego Bermúdez, 07/10/2026): pasar la cédula del
            // director autenticado al service para que quede trazabilidad.
            return ResponseEntity.ok(solicitudService.rechazarSolicitud(id, motivo, p.cedula()));
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
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
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
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        try {
            return ResponseEntity.ok(validacionGradoService.registrarValidacion(id, decision, observaciones, p));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        }
    }

    /** POST /api/solicitudes/{id}/pagar-grado */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @PostMapping("/{id}/pagar-grado")
    public ResponseEntity<?> pagarGrado(@PathVariable Long id, Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null || !p.isUsuario()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        if (!solicitudService.perteneceAEstudiante(id, p.cedula()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("No tiene acceso a esta solicitud"));
        try {
            return ResponseEntity.ok(solicitudService.registrarPagoGrado(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        }
    }

    /** POST /api/solicitudes/{id}/modalidad-grado?modalidad=CEREMONIA|SECRETARIA */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @PostMapping("/{id}/modalidad-grado")
    public ResponseEntity<?> registrarModalidadGrado(@PathVariable Long id, @RequestParam String modalidad, Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null || !p.isUsuario()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        if (!solicitudService.perteneceAEstudiante(id, p.cedula()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("No tiene acceso a esta solicitud"));
        try {
            return ResponseEntity.ok(solicitudService.registrarModalidadGrado(id, modalidad));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        }
    }

    /** POST /api/solicitudes/{id}/pagar-modalidad */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @PostMapping("/{id}/pagar-modalidad")
    public ResponseEntity<?> pagarModalidad(@PathVariable Long id, Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null || !p.isUsuario()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        if (!solicitudService.perteneceAEstudiante(id, p.cedula()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("No tiene acceso a esta solicitud"));
        try {
            return ResponseEntity.ok(solicitudService.registrarPagoModalidad(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        }
    }

    /**
     * POST /api/solicitudes/{id}/fecha-grado
     *
     * El estudiante elige su fecha de grado en el Paso 3 del proceso (la pantalla
     * llama a este endpoint con su propio token). Antes solo se permitía
     * DIRECTOR/POSGRADOS y el estudiante recibía 403, quedando atascado. Un
     * estudiante solo puede fijar la fecha de SU solicitud.
     */
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DIRECTOR', 'POSGRADOS')")
    @PostMapping("/{id}/fecha-grado")
    public ResponseEntity<?> registrarFechaGrado(@PathVariable Long id, @RequestParam String fecha, Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        if ("ESTUDIANTE".equals(p.rol()) && !solicitudService.perteneceAEstudiante(id, p.cedula()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("No tiene acceso a esta solicitud"));
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
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        return ResponseEntity.ok(solicitudService.getBandejaPosgrados());
    }

    /** GET /api/solicitudes/{id}/certificado-pdf */
    @PreAuthorize("hasAnyRole('POSGRADOS', 'ESTUDIANTE')")
    @GetMapping("/{id}/certificado-pdf")
    public ResponseEntity<byte[]> descargarCertificadoPdf(@PathVariable Long id, Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if ("ESTUDIANTE".equals(p.rol()) && !solicitudService.perteneceAEstudiante(id, p.cedula())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
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
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido"));
        try {
            return ResponseEntity.ok(solicitudService.rechazarSolicitud(id, motivo));
        } catch (IllegalArgumentException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage())); }
        catch (IllegalStateException e)      { return ResponseEntity.status(422).body(error(e.getMessage())); }
    }

    /** POST /api/solicitudes/{id}/aprobar-posgrados */
    @PreAuthorize("hasRole('POSGRADOS')")
    @PostMapping("/{id}/aprobar-posgrados")
    public ResponseEntity<?> aprobarPosgrados(@PathVariable Long id, Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido"));
        try {
            byte[] pdf = solicitudService.aprobarPosgrados(id, p.admin());
            // No es download endpoint — devolvemos JSON con confirmación
            return ResponseEntity.ok(Map.of(
                "ok", true,
                "id", id,
                "pdfBytes", pdf != null ? pdf.length : 0
            ));
        } catch (IllegalArgumentException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage())); }
        catch (IllegalStateException e)      { return ResponseEntity.status(422).body(error(e.getMessage())); }
    }

    /** GET /api/solicitudes/verificar — público, sin JWT (QR de certificados) */
    @GetMapping("/verificar")
    public ResponseEntity<Map<String, Object>> verificarCertificado(@RequestParam String codigo) {
        return ResponseEntity.ok(solicitudService.verificarCertificado(codigo));
    }

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", mensaje);
        return map;
    }
}
