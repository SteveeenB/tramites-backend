package com.ufps.tramites.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ufps.tramites.model.Solicitud;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    List<Solicitud> findByCedula(String cedula);

    Optional<Solicitud> findFirstByCedulaAndTipo(String cedula, String tipo);

    List<Solicitud> findByCedulaInAndTipo(List<String> cedulas, String tipo);

    // ── Nuevo para TP-44 ──────────────────────────────────────────────────
    // Spring Data genera el SQL automáticamente desde el nombre del método.
    // Equivale a: SELECT * FROM solicitud WHERE estado = 'EN_REVISION'
    List<Solicitud> findByEstado(String estado);
    // ── HU-09 ─────────────────────────────────────────────────────────────
    List<Solicitud> findByTipoAndEstado(String tipo, String estado);
}
