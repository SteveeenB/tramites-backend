package com.ufps.tramites.controller;

import com.ufps.tramites.security.PrincipalResolver;
import com.ufps.tramites.security.ResolvedPrincipal;
import com.ufps.tramites.service.NotificacionService;
import com.ufps.tramites.service.NotificacionSseService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    @Autowired private NotificacionSseService sseService;
    @Autowired private NotificacionService notificacionService;
    @Autowired private PrincipalResolver principalResolver;

    /**
     * SSE: el browser no puede enviar Authorization headers en EventSource,
     * por eso el frontend envía el JWT como query param ?token=... y el
     * JwtAuthFilter lo valida (ver FIX TP-186). El identificador del
     * suscriptor se toma del token, nunca del request.
     *
     * Ambas rutas /subscribe y /stream apuntan al mismo handler.
     */
    // FIX TP-186 (Santiago Cepeda, 07/10/2026): se exige autenticación y la
    // cédula/código del suscriptor se resuelve del token. Antes se tomaba de
    // un @RequestParam, permitiendo que cualquiera leyera las notificaciones
    // de otro usuario conociendo su cédula.
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = {"/subscribe", "/stream"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(Authentication auth) {
        String id = resolverIdentificador(auth);
        if (id == null) {
            // Fallback: emitir un emisor que se cierre inmediatamente. El
            // JwtAuthFilter normalmente ya habrá devuelto 401 antes de llegar
            // aquí, pero esto evita NPE si el principal no resuelve.
            SseEmitter emitter = new SseEmitter(0L);
            emitter.complete();
            return emitter;
        }
        return sseService.suscribir(id);
    }

    /** GET /api/notificaciones/mias — lista paginada de notificaciones del usuario autenticado. */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/mias")
    public ResponseEntity<?> getMias(Authentication auth) {
        String id = resolverIdentificador(auth);
        if (id == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(notificacionService.listarPorDestinatario(id));
    }

    /** GET /api/notificaciones/no-leidas/count — contador del badge. */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/no-leidas/count")
    public ResponseEntity<?> countNoLeidas(Authentication auth) {
        String id = resolverIdentificador(auth);
        if (id == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("count", notificacionService.contarNoLeidas(id)));
    }

    /** PUT /api/notificaciones/{id}/leer — marca una notificación como leída. */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}/leer")
    public ResponseEntity<?> marcarLeida(@PathVariable Long id, Authentication auth) {
        String ced = resolverIdentificador(auth);
        if (ced == null) return ResponseEntity.status(401).build();
        try {
            notificacionService.marcarLeida(id, ced);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String resolverIdentificador(Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null) return null;
        // cedula para ESTUDIANTE/DIRECTOR; codigo para POSGRADOS/DEPENDENCIA/ADMIN
        return p.cedula() != null ? p.cedula() : p.codigo();
    }
}
