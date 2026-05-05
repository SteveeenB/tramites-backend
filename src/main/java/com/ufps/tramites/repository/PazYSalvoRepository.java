package com.ufps.tramites.repository;

import com.ufps.tramites.model.PazYSalvo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PazYSalvoRepository extends JpaRepository<PazYSalvo, Long> {
    List<PazYSalvo> findBySolicitudId(Long solicitudId);
    List<PazYSalvo> findByCedulaResponsable(String cedulaResponsable);
    List<PazYSalvo> findByCedulaResponsableAndEstado(String cedulaResponsable, String estado);
    List<PazYSalvo> findBySolicitudIdAndCedulaResponsable(Long solicitudId, String cedulaResponsable);
    boolean existsBySolicitudIdAndEstado(Long solicitudId, String estado);
    long countBySolicitudId(Long solicitudId);
    long countBySolicitudIdAndEstado(Long solicitudId, String estado);
}
