package com.ufps.tramites.service;

import com.ufps.tramites.model.Solicitud;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotificacionSseService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionSseService.class);

    // CopyOnWriteArrayList por cedula: permite iterar y eliminar sin ConcurrentModificationException
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter suscribir(String cedula) {
        // 0L = sin timeout: la conexion se mantiene hasta que el cliente la cierre
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(cedula, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> eliminarEmitter(cedula, emitter));
        emitter.onTimeout(()    -> eliminarEmitter(cedula, emitter));
        emitter.onError(e       -> eliminarEmitter(cedula, emitter));

        try {
            emitter.send(SseEmitter.event().name("conectado").data("Suscripcion activa", MediaType.TEXT_PLAIN));
        } catch (IOException e) {
            eliminarEmitter(cedula, emitter);
        }

        log.info("Nuevo suscriptor SSE: cedula={}", cedula);
        return emitter;
    }

    public void notificarCambioEstado(Solicitud solicitud, String estadoAnterior) {
        String cedula = solicitud.getCedula();
        List<SseEmitter> lista = emitters.getOrDefault(cedula, List.of());
        if (lista.isEmpty()) return;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("solicitudId",          solicitud.getId());
        payload.put("tipo",                 solicitud.getTipo());
        payload.put("estadoAnterior",       estadoAnterior);
        payload.put("estadoNuevo",          solicitud.getEstado());
        payload.put("observaciones",        solicitud.getObservaciones());
        payload.put("certificadoDisponible",
                "APROBADA".equals(solicitud.getEstado()) && "TERMINACION_MATERIAS".equals(solicitud.getTipo()));

        for (SseEmitter emitter : lista) {
            try {
                emitter.send(SseEmitter.event()
                        .name("estado-actualizado")
                        .data(payload, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                eliminarEmitter(cedula, emitter);
            }
        }
        log.info("SSE enviado a {} suscriptores de cedula {}", lista.size(), cedula);
    }

    private void eliminarEmitter(String cedula, SseEmitter emitter) {
        List<SseEmitter> lista = emitters.get(cedula);
        if (lista != null) lista.remove(emitter);
    }
}
