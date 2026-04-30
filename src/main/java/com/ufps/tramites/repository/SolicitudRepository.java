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

    /** Solicitudes de varios estudiantes filtrando por varios tipos a la vez. */
    List<Solicitud> findByCedulaInAndTipoIn(List<String> cedulas, List<String> tipos);

    List<Solicitud> findByEstado(String estado);
}