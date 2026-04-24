package com.ufps.tramites.service;

import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.SolicitudRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ValidacionGradoService {

    @Autowired
    private SolicitudRepository solicitudRepository;

    // Retorna todas las solicitudes de grado pendientes de validación
    public List<Solicitud> obtenerSolicitudesPendientesValidacion() {
        return solicitudRepository.findByTipoAndEstado("GRADO", "PENDIENTE_VALIDACION");
    }

    // Registra la validación de posgrados sobre una solicitud de grado
    public Map<String, Object> registrarValidacion(Long id, String decision,
                                                    String observaciones,
                                                    Usuario responsable) {
        // 1. Busca la solicitud
        Optional<Solicitud> optional = solicitudRepository.findById(id);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("Solicitud no encontrada con id: " + id);
        }

        Solicitud solicitud = optional.get();

        // 2. Valida que sea una solicitud de grado
        if (!"GRADO".equals(solicitud.getTipo())) {
            throw new IllegalStateException("Esta solicitud no es de tipo GRADO");
        }

        // 3. Valida que esté en el estado correcto
        if (!"PENDIENTE_VALIDACION".equals(solicitud.getEstado())) {
            throw new IllegalStateException(
                "La solicitud no está pendiente de validación. " +
                "Estado actual: " + solicitud.getEstado()
            );
        }

        // 4. Valida que la decisión sea válida
        if (!"APROBADA".equals(decision) && !"RECHAZADA".equals(decision)) {
            throw new IllegalStateException(
                "Decisión inválida: " + decision +
                ". Valores permitidos: APROBADA, RECHAZADA"
            );
        }

        // 5. Registra la validación
        solicitud.setValidacionPosgrados(decision);
        solicitud.setObservacionesPosgrados(observaciones);
        solicitud.setFechaValidacion(LocalDateTime.now());
        solicitud.setCedulaPosgrados(responsable.getCedula());

        // 6. Actualiza el estado de la solicitud
        if ("APROBADA".equals(decision)) {
            solicitud.setEstado("APROBADA_POSGRADOS");
        } else {
            solicitud.setEstado("RECHAZADA_POSGRADOS");
        }

        solicitudRepository.save(solicitud);

        // 7. Construye la respuesta
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("id", solicitud.getId());
        respuesta.put("tipo", solicitud.getTipo());
        respuesta.put("estado", solicitud.getEstado());
        respuesta.put("validacionPosgrados", solicitud.getValidacionPosgrados());
        respuesta.put("observacionesPosgrados", solicitud.getObservacionesPosgrados());
        respuesta.put("fechaValidacion", solicitud.getFechaValidacion());
        respuesta.put("validadoPor", responsable.getNombre());
        return respuesta;
    }
}