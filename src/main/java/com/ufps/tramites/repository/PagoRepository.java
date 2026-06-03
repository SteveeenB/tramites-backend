package com.ufps.tramites.repository;

import com.ufps.tramites.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByReferencia(String referencia);
    List<Pago> findBySolicitudId(Long solicitudId);
    Optional<Pago> findBySolicitudIdAndTipoPago(Long solicitudId, String tipoPago);
    List<Pago> findByCedulaEstudiante(String cedula);
}