package com.ufps.tramites.repository;

import com.ufps.tramites.model.Notificacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByDestinatarioCedulaOrderByFechaCreacionDesc(String destinatarioCedula);

    long countByDestinatarioCedulaAndLeidaFalse(String destinatarioCedula);

    Optional<Notificacion> findByIdAndDestinatarioCedula(Long id, String destinatarioCedula);
}
