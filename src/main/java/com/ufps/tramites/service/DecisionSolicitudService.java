package com.ufps.tramites.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.repository.SolicitudRepository;

@Service
public class DecisionSolicitudService {

    private final SolicitudRepository solicitudRepository;

    public DecisionSolicitudService(SolicitudRepository solicitudRepository) {
        this.solicitudRepository = solicitudRepository;
    }

    public Solicitud registrarDecision(Long id, String decision,
                                       String observacionesDirector,
                                       String cedulaDirector) {

        // 1. Busca la solicitud — lanza excepción si no existe
        Optional<Solicitud> optional = solicitudRepository.findById(id);
        if (optional.isEmpty()) {
            throw new RuntimeException("Solicitud no encontrada con id: " + id);
        }

        Solicitud solicitud = optional.get();

        // 2. Valida que la solicitud esté en el estado correcto
        //    El director solo puede decidir sobre solicitudes EN_REVISION
        if (!"EN_REVISION".equals(solicitud.getEstado())) {
            throw new RuntimeException(
                "La solicitud no esta en estado EN_REVISION. " +
                "Estado actual: " + solicitud.getEstado()
            );
        }

        // 3. Valida que la decision sea válida
        if (!"APROBADA".equals(decision) && !"RECHAZADA".equals(decision)) {
            throw new RuntimeException(
                "Decision invalida: " + decision +
                ". Valores permitidos: APROBADA, RECHAZADA"
            );
        }

        // 4. Aplica la decisión
        solicitud.setDecision(decision);
        solicitud.setEstado(decision);              // estado sigue a la decisión
        solicitud.setObservacionesDirector(observacionesDirector);
        solicitud.setCedulaDirector(cedulaDirector);
        solicitud.setFechaDecision(LocalDateTime.now());  // trazabilidad TP-41

        // 5. Persiste y retorna
        return solicitudRepository.save(solicitud);
    }
}