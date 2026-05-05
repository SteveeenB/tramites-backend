package com.ufps.tramites.controller;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.service.PazYSalvoService;
import com.ufps.tramites.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/paz-y-salvo")
public class PazYSalvoController {

    @Autowired private PazYSalvoService pazYSalvoService;
    @Autowired private UsuarioService usuarioService;

    /** GET /api/paz-y-salvo/mis-solicitudes?cedula=... — para DEPENDENCIA o DIRECTOR */
    @GetMapping("/mis-solicitudes")
    public ResponseEntity<?> misSolicitudes(@RequestParam String cedula) {
        Usuario u = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (u == null) return notFound("Usuario no encontrado");
        if (!List.of("DEPENDENCIA", "DIRECTOR").contains(u.getRol()))
            return forbidden("Solo dependencias y directores pueden usar este endpoint");
        return ResponseEntity.ok(pazYSalvoService.obtenerPazYSalvosPorResponsable(cedula));
    }

    /** GET /api/paz-y-salvo/pendientes?cedula=... — solo los PENDIENTES del responsable */
    @GetMapping("/pendientes")
    public ResponseEntity<?> pendientes(@RequestParam String cedula) {
        Usuario u = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (u == null) return notFound("Usuario no encontrado");
        if (!List.of("DEPENDENCIA", "DIRECTOR").contains(u.getRol()))
            return forbidden("Solo dependencias y directores pueden usar este endpoint");
        return ResponseEntity.ok(pazYSalvoService.obtenerPazYSalvosPendientes(cedula));
    }

    /** POST /api/paz-y-salvo/{id}/responder?cedula=...&decision=APROBADO|RECHAZADO&observaciones=... */
    @PostMapping("/{id}/responder")
    public ResponseEntity<?> responder(
            @PathVariable Long id,
            @RequestParam String cedula,
            @RequestParam String decision,
            @RequestParam(required = false) String observaciones) {
        Usuario u = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (u == null) return notFound("Usuario no encontrado");
        if (!List.of("DEPENDENCIA", "DIRECTOR").contains(u.getRol()))
            return forbidden("No tiene permiso");
        try {
            return ResponseEntity.ok(pazYSalvoService.responderPazYSalvo(id, cedula, decision, observaciones));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.valueOf(422)).body(error(e.getMessage()));
        }
    }

    /** GET /api/paz-y-salvo/solicitud/{solicitudId} — estado general de todos los paz y salvos */
    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<?> estadoSolicitud(@PathVariable Long solicitudId) {
        return ResponseEntity.ok(pazYSalvoService.obtenerEstadoPazYSalvos(solicitudId));
    }

    /** GET /api/paz-y-salvo/estado-estudiantes?cedula=... — vista de estado para el director */
    @GetMapping("/estado-estudiantes")
    public ResponseEntity<?> estadoEstudiantes(@RequestParam String cedula) {
        Usuario u = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (u == null) return notFound("Usuario no encontrado");
        if (!"DIRECTOR".equals(u.getRol())) return forbidden("Solo directores");
        return ResponseEntity.ok(pazYSalvoService.obtenerEstadoEstudiantes(u));
    }

    private ResponseEntity<?> notFound(String msg) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(msg));
    }
    private ResponseEntity<?> forbidden(String msg) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(msg));
    }
    private Map<String, String> error(String msg) {
        return Map.of("error", msg);
    }
}
