package com.ufps.tramites.repository;

import com.ufps.tramites.model.DocumentoSolicitud;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentoSolicitudRepository extends JpaRepository<DocumentoSolicitud, Long> {
    List<DocumentoSolicitud> findBySolicitudId(Long solicitudId);
    java.util.Optional<DocumentoSolicitud> findBySolicitudIdAndTipo(Long solicitudId, String tipo);
}
