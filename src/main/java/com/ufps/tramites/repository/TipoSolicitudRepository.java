package com.ufps.tramites.repository;

import com.ufps.tramites.model.TipoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoSolicitudRepository extends JpaRepository<TipoSolicitud, Long> {
    Optional<TipoSolicitud> findByCodigo(String codigo);
}
