package com.ufps.tramites.repository;

import com.ufps.tramites.model.HistorialEstadoTramite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialEstadoTramiteRepository extends JpaRepository<HistorialEstadoTramite, Long> {
    List<HistorialEstadoTramite> findBySolicitudIdOrderByFechaCambioAsc(Long solicitudId);
}