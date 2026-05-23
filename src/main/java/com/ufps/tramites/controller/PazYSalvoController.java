package com.ufps.tramites.controller;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.service.PazYSalvoService;
import com.ufps.tramites.service.UsuarioService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/paz-y-salvo")
public class PazYSalvoController {

    @Autowired private PazYSalvoService pazYSalvoService;
    @Autowired private UsuarioService usuarioService;

    /** GET /api/paz-y-salvo/mis-solicitudes */
    @PreAuthorize("hasAnyRole('DEPENDENCIA', 'DIRECTOR')")
    @GetMapping("/mis-solicitudes")
    public ResponseEntity<?> misSolicitudes(Authentication auth) {
        Usuario u = resolverUsuario(auth);
        if (u == null) return unauthorized();
        if (!List.of("DEPENDENCIA", "DIRECTOR").contains(u.getRolNombre()))
            return forbidden("Solo dependencias y directores pueden usar este endpoint");
        return ResponseEntity.ok(pazYSalvoService.obtenerPazYSalvosPorResponsable(u.getCedula()));
    }

    /** GET /api/paz-y-salvo/pendientes */
    @PreAuthorize("hasAnyRole('DEPENDENCIA', 'DIRECTOR')")
    @GetMapping("/pendientes")
    public ResponseEntity<?> pendientes(Authentication auth) {
        Usuario u = resolverUsuario(auth);
        if (u == null) return unauthorized();
        if (!List.of("DEPENDENCIA", "DIRECTOR").contains(u.getRolNombre()))
            return forbidden("Solo dependencias y directores pueden usar este endpoint");
        return ResponseEntity.ok(pazYSalvoService.obtenerPazYSalvosPendientes(u.getCedula()));
    }

    /** POST /api/paz-y-salvo/{id}/responder */
    @PreAuthorize("hasAnyRole('DEPENDENCIA', 'DIRECTOR')")
    @PostMapping("/{id}/responder")
    public ResponseEntity<?> responder(
            @PathVariable Long id,
            Authentication auth,
            @RequestParam String decision,
            @RequestParam(required = false) String observaciones) {
        Usuario u = resolverUsuario(auth);
        if (u == null) return unauthorized();
        if (!List.of("DEPENDENCIA", "DIRECTOR").contains(u.getRolNombre()))
            return forbidden("No tiene permiso");
        try {
            return ResponseEntity.ok(pazYSalvoService.responderPazYSalvo(id, u.getCedula(), decision, observaciones));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(error(e.getMessage()));
        }
    }

    /** GET /api/paz-y-salvo/solicitud/{solicitudId} */
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DIRECTOR', 'DEPENDENCIA')")
    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<?> estadoSolicitud(@PathVariable Long solicitudId) {
        return ResponseEntity.ok(pazYSalvoService.obtenerEstadoPazYSalvos(solicitudId));
    }

    /** GET /api/paz-y-salvo/estado-estudiantes */
    @PreAuthorize("hasRole('DIRECTOR')")
    @GetMapping("/estado-estudiantes")
    public ResponseEntity<?> estadoEstudiantes(Authentication auth) {
        Usuario u = resolverUsuario(auth);
        if (u == null) return unauthorized();
        if (!"DIRECTOR".equals(u.getRolNombre())) return forbidden("Solo directores");
        return ResponseEntity.ok(pazYSalvoService.obtenerEstadoEstudiantes(u));
    }

    private Usuario resolverUsuario(Authentication auth) {
        if (auth == null) return null;
        return usuarioService.obtenerUsuarioPorCedula(auth.getName());
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
    }
    private ResponseEntity<?> forbidden(String msg) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(msg));
    }
    private Map<String, String> error(String msg) {
        return Map.of("error", msg);
    }
}
