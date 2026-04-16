package com.ufps.tramites.service;

import com.ufps.tramites.event.SolicitudEstadoCambiadoEvent;
import com.ufps.tramites.model.Solicitud;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotificacionSseService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionSseService.class);

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter suscribir(String cedula) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(cedula, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> eliminarEmitter(cedula, emitter));
        emitter.onTimeout(() -> eliminarEmitter(cedula, emitter));
        emitter.onError(e -> eliminarEmitter(cedula, emitter));

        try {
            emitter.send(SseEmitter.event().name("conectado").data("Suscripción activa", MediaType.TEXT_PLAIN));
        } catch (IOException e) {
            eliminarEmitter(cedula, emitter);
        }

        log.info("Nuevo suscriptor SSE: cédula={}", cedula);
        return emitter;
    }

    @EventListener
    public void onEstadoCambiado(SolicitudEstadoCambiadoEvent evento) {
        Solicitud solicitud = evento.getSolicitud();
        String cedula = solicitud.getCedula();

        List<SseEmitter> lista = emitters.getOrDefault(cedula, List.of());
        if (lista.isEmpty()) return;

        Map<String, Object> payload = construirPayload(solicitud, evento.getEstadoAnterior());

        for (SseEmitter emitter : lista) {
            try {
                emitter.send(SseEmitter.event()
                        .name("estado-actualizado")
                        .data(payload, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                eliminarEmitter(cedula, emitter);
            }
        }
        log.info("SSE enviado a {} suscriptores de cédula {}", lista.size(), cedula);
    }

    private Map<String, Object> construirPayload(Solicitud solicitud, String estadoAnterior) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("solicitudId", solicitud.getId());
        map.put("tipo", solicitud.getTipo());
        map.put("estadoAnterior", estadoAnterior);
        map.put("estadoNuevo", solicitud.getEstado());
        map.put("observaciones", solicitud.getObservaciones());
        map.put("certificadoDisponible",
                "APROBADA".equals(solicitud.getEstado()) && "TERMINACION_MATERIAS".equals(solicitud.getTipo()));
        return map;
    }

    private void eliminarEmitter(String cedula, SseEmitter emitter) {
        List<SseEmitter> lista = emitters.get(cedula);
        if (lista != null) {
            lista.remove(emitter);
        }
    }
}
