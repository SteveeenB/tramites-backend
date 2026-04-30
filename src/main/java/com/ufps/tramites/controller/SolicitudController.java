package com.ufps.tramites.controller;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.service.SolicitudService;
import com.ufps.tramites.service.UsuarioService;
import java.util.LinkedHashMap;
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

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    @Autowired private SolicitudService solicitudService;
    @Autowired private UsuarioService   usuarioService;

    // ── Terminación de Materias ──────────────────────────────────────────────

    @PostMapping("/terminacion-materias")
    public ResponseEntity<?> crearSolicitudTerminacion(@RequestParam String cedula) {
        Usuario estudiante = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (estudiante == null) return notFound("Estudiante no encontrado");
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(solicitudService.crearSolicitudTerminacion(estudiante));
        } catch (IllegalStateException e) {
            return unprocessable(e.getMessage());
        }
    }

    // ── Grado ────────────────────────────────────────────────────────────────

    @PostMapping("/grado")
    public ResponseEntity<?> crearSolicitudGrado(@RequestParam String cedula) {
        Usuario estudiante = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (estudiante == null) return notFound("Estudiante no encontrado");
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(solicitudService.crearSolicitudGrado(estudiante));
        } catch (IllegalStateException e) {
            return unprocessable(e.getMessage());
        }
    }

    // ── Consultas generales ──────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<?> obtenerSolicitudes(@RequestParam String cedula) {
        Usuario estudiante = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (estudiante == null) return notFound("Estudiante no encontrado");
        return ResponseEntity.ok(solicitudService.obtenerSolicitudesPorCedula(cedula));
    }

    // ── Bandeja del Director (Terminación de Materias) ───────────────────────

    @GetMapping("/bandeja")
    public ResponseEntity<?> obtenerBandeja(@RequestParam String cedula) {
        Usuario director = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (director == null) return notFound("Director no encontrado");
        if (!"DIRECTOR".equals(director.getRol()))
            return forbidden("Acceso restringido a directores de programa");
        return ResponseEntity.ok(solicitudService.obtenerBandejaDirector(director));
    }

    @PostMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobarSolicitud(@PathVariable Long id, @RequestParam String cedula) {
        Usuario director = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (director == null) return notFound("Director no encontrado");
        if (!"DIRECTOR".equals(director.getRol()))
            return forbidden("Acceso restringido a directores");
        try {
            return ResponseEntity.ok(solicitudService.aprobarSolicitud(id));
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (IllegalStateException e) {
            return unprocessable(e.getMessage());
        }
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazarSolicitud(
            @PathVariable Long id,
            @RequestParam String cedula,
            @RequestParam(required = false) String motivo) {
        Usuario director = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (director == null) return notFound("Director no encontrado");
        if (!"DIRECTOR".equals(director.getRol()))
            return forbidden("Acceso restringido a directores");
        try {
            return ResponseEntity.ok(solicitudService.rechazarSolicitud(id, motivo));
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (IllegalStateException e) {
            return unprocessable(e.getMessage());
        }
    }

    // ── Certificado (Terminación de Materias aprobada) ───────────────────────

    @GetMapping("/{id}/certificado")
    public ResponseEntity<byte[]> descargarCertificado(@PathVariable Long id) {
        try {
            byte[] contenido = solicitudService.generarCertificado(id);
            return ResponseEntity.ok()
                    .header("Content-Disposition",
                            "attachment; filename=\"certificado-terminacion-" + id + ".txt\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(contenido);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).build();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<?> notFound(String msg) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(msg));
    }

    private ResponseEntity<?> forbidden(String msg) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(msg));
    }

    private ResponseEntity<?> unprocessable(String msg) {
        return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(msg));
    }

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", mensaje);
        return map;
    }
}