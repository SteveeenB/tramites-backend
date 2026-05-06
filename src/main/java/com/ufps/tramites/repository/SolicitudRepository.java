package com.ufps.tramites.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ufps.tramites.model.Solicitud;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    List<Solicitud> findByCedula(String cedula);

    Optional<Solicitud> findFirstByCedulaAndTipo(String cedula, String tipo);

    Optional<Solicitud> findFirstByCedulaAndTipoOrderByIdDesc(String cedula, String tipo);

    List<Solicitud> findByCedulaInAndTipo(List<String> cedulas, String tipo);

    List<Solicitud> findByEstado(String estado);

    List<Solicitud> findByTipoAndEstado(String tipo, String estado);

    // ── Nuevo para bandeja de grado (TP-45) ────────────────────────────────
    @Query("SELECT s FROM Solicitud s WHERE s.tipo = 'GRADO' AND s.estado = :estado ORDER BY s.fechaSolicitud DESC")
    List<Solicitud> findGradoByEstado(@Param("estado") String estado);

    @Query("SELECT s FROM Solicitud s WHERE s.tipo = 'GRADO' ORDER BY s.fechaSolicitud DESC")
    List<Solicitud> findAllGrado();
}

