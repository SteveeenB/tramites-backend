package com.ufps.tramites.repository;

import com.ufps.tramites.model.DocumentoCargado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentoCargadoRepository extends JpaRepository<DocumentoCargado, Long> {
    List<DocumentoCargado> findBySolicitudId(Long solicitudId);
    Optional<DocumentoCargado> findBySolicitudIdAndTipoDocumentoId(Long solicitudId, Long tipoDocumentoId);
    long countBySolicitudId(Long solicitudId);
}
