package com.ufps.tramites.controller;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.service.SolicitudService;
import com.ufps.tramites.service.UsuarioService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @Autowired
    private SolicitudService solicitudService;

    @Autowired
    private UsuarioService usuarioService;

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
     * POST /api/solicitudes/grado?cedula=...
     * Crea una solicitud de grado académico para el estudiante.
     */
    @PostMapping("/grado")
    public ResponseEntity<?> crearSolicitudGrado(@RequestParam String cedula) {
        Usuario estudiante = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (estudiante == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Estudiante no encontrado"));
        }
        try {
            Map<String, Object> resultado = solicitudService.crearSolicitudGrado(estudiante);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
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

    /** POST /api/solicitudes/{id}/aprobar?cedula=... */
    @PostMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobarSolicitud(@PathVariable Long id, @RequestParam String cedula) {
        Usuario director = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (director == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Director no encontrado"));
        if (!"DIRECTOR".equals(director.getRol())) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido a directores"));
        try {
            return ResponseEntity.ok(solicitudService.aprobarSolicitud(id));
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

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", mensaje);
        return map;
    }
}
