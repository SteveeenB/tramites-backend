package com.ufps.tramites.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ufps.tramites.dto.CambioHistorialDto;
import com.ufps.tramites.dto.TramiteResumenDto;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.service.NotificacionSseService;
import com.ufps.tramites.service.SeguimientoTramitesService;
import com.ufps.tramites.service.UsuarioService;

@RestController
@RequestMapping("/api/tramites")
public class SeguimientoController {

    @Autowired
    private SeguimientoTramitesService seguimientoService;

    @Autowired
    private NotificacionSseService notificacionSseService;

    @Autowired
    private UsuarioService usuarioService;

    /**
     * GET /api/tramites/mis
     * Retorna todos los trámites del usuario autenticado unificados.
     */
    @GetMapping("/mis")
    public ResponseEntity<?> obtenerMisTramites(Authentication auth) {
        Usuario usuario = resolverUsuario(auth);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(error("No autenticado"));
        }
        try {
            List<TramiteResumenDto> tramites = seguimientoService
                    .obtenerMisTramites(usuario.getCedula());
            return ResponseEntity.ok(tramites);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Error al obtener trámites: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tramites/{id}/historial
     * Retorna el historial cronológico de una solicitud.
     */
    @GetMapping("/{id}/historial")
    public ResponseEntity<?> obtenerHistorial(@PathVariable Long id, Authentication auth) {
        Usuario usuario = resolverUsuario(auth);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(error("No autenticado"));
        }
        try {
            List<CambioHistorialDto> historial = seguimientoService.obtenerHistorial(id);
            return ResponseEntity.ok(historial);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Error al obtener historial: " + e.getMessage()));
        }
    }

    /**
     * GET /api/tramites/mis/stream
     * SSE stream con keep-alive cada 30s para Render.
     */
    @GetMapping(value = "/mis/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication auth) {
        Usuario usuario = resolverUsuario(auth);
        String cedula = usuario != null ? usuario.getCedula() : "anonimo";

        SseEmitter emitter = notificacionSseService.suscribir(cedula);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("keep-alive")
                        .data("ping", MediaType.TEXT_PLAIN));
            } catch (Exception e) {
                scheduler.shutdown();
            }
        }, 30, 30, TimeUnit.SECONDS);

        emitter.onCompletion(scheduler::shutdown);
        emitter.onTimeout(scheduler::shutdown);
        emitter.onError(e -> scheduler.shutdown());

        return emitter;
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