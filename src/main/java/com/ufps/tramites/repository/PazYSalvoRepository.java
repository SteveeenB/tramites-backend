package com.ufps.tramites.repository;

import com.ufps.tramites.model.PazYSalvo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PazYSalvoRepository extends JpaRepository<PazYSalvo, Long> {
    List<PazYSalvo> findBySolicitudId(Long solicitudId);

    List<PazYSalvo> findByResponsableAdmin_Id(Long responsableAdminId);
    List<PazYSalvo> findByResponsableAdmin_IdAndEstado(Long responsableAdminId, String estado);

    List<PazYSalvo> findByResponsableUsuario_Id(Long responsableUsuarioId);
    List<PazYSalvo> findByResponsableUsuario_IdAndEstado(Long responsableUsuarioId, String estado);

    boolean existsBySolicitudIdAndEstado(Long solicitudId, String estado);
    long countBySolicitudId(Long solicitudId);
    long countBySolicitudIdAndEstado(Long solicitudId, String estado);
}
