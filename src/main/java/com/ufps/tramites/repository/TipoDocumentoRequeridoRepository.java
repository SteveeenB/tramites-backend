package com.ufps.tramites.repository;

import com.ufps.tramites.model.TipoDocumentoRequerido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoDocumentoRequeridoRepository extends JpaRepository<TipoDocumentoRequerido, Long> {
    List<TipoDocumentoRequerido> findAllByOrderByOrdenAsc();
    long countByObligatorio(boolean obligatorio);
}
