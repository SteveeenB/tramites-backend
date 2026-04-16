package com.ufps.tramites.repository;

import com.ufps.tramites.model.Solicitud;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    List<Solicitud> findByCedula(String cedula);

    Optional<Solicitud> findByCedulaAndTipo(String cedula, String tipo);

    List<Solicitud> findByCedulaInAndTipo(List<String> cedulas, String tipo);
}
