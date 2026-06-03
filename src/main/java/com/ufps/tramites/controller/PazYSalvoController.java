package com.ufps.tramites.controller;

import com.ufps.tramites.security.PrincipalResolver;
import com.ufps.tramites.security.ResolvedPrincipal;
import com.ufps.tramites.service.PazYSalvoService;
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
    @Autowired private PrincipalResolver principalResolver;

    /** GET /api/paz-y-salvo/mis-solicitudes */
    @PreAuthorize("hasAnyRole('DEPENDENCIA', 'DIRECTOR')")
    @GetMapping("/mis-solicitudes")
    public ResponseEntity<?> misSolicitudes(Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null) return unauthorized();
        return ResponseEntity.ok(pazYSalvoService.obtenerPazYSalvosPorResponsable(p));
    }

    /** GET /api/paz-y-salvo/pendientes */
    @PreAuthorize("hasAnyRole('DEPENDENCIA', 'DIRECTOR')")
    @GetMapping("/pendientes")
    public ResponseEntity<?> pendientes(Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null) return unauthorized();
        return ResponseEntity.ok(pazYSalvoService.obtenerPazYSalvosPendientes(p));
    }

    /** POST /api/paz-y-salvo/{id}/responder */
    @PreAuthorize("hasAnyRole('DEPENDENCIA', 'DIRECTOR')")
    @PostMapping("/{id}/responder")
    public ResponseEntity<?> responder(
            @PathVariable Long id,
            Authentication auth,
            @RequestParam String decision,
            @RequestParam(required = false) String observaciones) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null) return unauthorized();
        try {
            return ResponseEntity.ok(pazYSalvoService.responderPazYSalvo(id, p, decision, observaciones));
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
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null || !p.isUsuario()) return forbidden("Solo directores");
        return ResponseEntity.ok(pazYSalvoService.obtenerEstadoEstudiantes(p.usuario()));
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
