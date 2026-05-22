package com.ufps.tramites.repository;

import com.ufps.tramites.model.TipoCertificado;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoCertificadoRepository extends JpaRepository<TipoCertificado, Long> {
    Optional<TipoCertificado> findByCodigo(String codigo);
    List<TipoCertificado> findByActivoTrue();
}
