package com.ufps.tramites.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ufps.tramites.model.TipoCertificado;

@Repository
public interface TipoCertificadoRepository extends JpaRepository<TipoCertificado, Long> {

    List<TipoCertificado> findByActivoTrue();
    Optional<TipoCertificado> findByCodigo(String codigo);
}