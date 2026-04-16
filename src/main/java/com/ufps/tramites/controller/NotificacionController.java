package com.ufps.tramites.controller;

import com.ufps.tramites.service.NotificacionSseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notificaciones")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class NotificacionController {

    @Autowired
    private NotificacionSseService sseService;

    /**
     * GET /api/notificaciones/subscribe?cedula=...
     * El frontend se suscribe al stream SSE para recibir actualizaciones en tiempo real
     * cuando el estado de una solicitud cambia (aprobada/rechazada).
     *
     * Eventos emitidos:
     *   - "conectado": confirmación de suscripción exitosa
     *   - "estado-actualizado": JSON con solicitudId, tipo, estadoAnterior, estadoNuevo,
     *                           observaciones y certificadoDisponible
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam String cedula) {
        return sseService.suscribir(cedula);
    }
}
