package com.ufps.tramites.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ufps.tramites.model.HistorialEstadoTramite;

@Repository
public interface HistorialEstadoTramiteRepository extends JpaRepository<HistorialEstadoTramite, Long> {

    List<HistorialEstadoTramite> findBySolicitudIdOrderByFechaCambioAsc(Long solicitudId);

    List<HistorialEstadoTramite> findBySolicitudIdAndTipoTramiteStartingWithOrderByFechaCambioAsc(
        Long solicitudId, String prefijo);

    List<HistorialEstadoTramite> findBySolicitudIdAndTipoTramiteNotStartingWithOrderByFechaCambioAsc(
        Long solicitudId, String prefijo);
}