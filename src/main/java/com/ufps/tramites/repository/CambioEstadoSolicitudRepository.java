package com.ufps.tramites.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ufps.tramites.model.CambioEstadoSolicitud;

// FIX TP-201 (Johan Bueno, 07/10/2026): acceso al historial de cambios
// de estado por solicitud, ordenado cronológicamente para la timeline
// del detalle de la solicitud.
@Repository
public interface CambioEstadoSolicitudRepository
        extends JpaRepository<CambioEstadoSolicitud, Long> {

    List<CambioEstadoSolicitud> findBySolicitudIdOrderByFechaAsc(Long solicitudId);
}
