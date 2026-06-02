package com.ufps.tramites.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ufps.tramites.model.HistorialEstadoTramite;

@Repository
public interface HistorialEstadoTramiteRepository extends JpaRepository<HistorialEstadoTramite, Long> {

    List<HistorialEstadoTramite> findBySolicitudIdOrderByFechaCambioAsc(Long solicitudId);

    List<HistorialEstadoTramite> findBySolicitudIdAndTipoTramiteStartingWithOrderByFechaCambioAsc(
        Long solicitudId, String prefijo);

    // Spring Data no soporta NotStartingWith — usar @Query
    @Query("SELECT h FROM HistorialEstadoTramite h WHERE h.solicitudId = :id AND h.tipoTramite NOT LIKE :prefijo% ORDER BY h.fechaCambio ASC")
    List<HistorialEstadoTramite> findSolicitudHistorial(
        @Param("id") Long solicitudId,
        @Param("prefijo") String prefijo);
}