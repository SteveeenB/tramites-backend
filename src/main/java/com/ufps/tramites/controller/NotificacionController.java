package com.ufps.tramites.controller;

import com.ufps.tramites.service.NotificacionSseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    @Autowired
    private NotificacionSseService sseService;

    /**
     * GET /api/notificaciones/subscribe?cedula=...
     * SSE: EventSource del browser no puede enviar headers, por eso cedula va como query param.
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam String cedula) {
        return sseService.suscribir(cedula);
    }
}
