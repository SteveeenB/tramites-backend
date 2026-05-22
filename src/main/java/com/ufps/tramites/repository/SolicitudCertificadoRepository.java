package com.ufps.tramites.repository;

import com.ufps.tramites.model.SolicitudCertificado;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudCertificadoRepository extends JpaRepository<SolicitudCertificado, Long> {
    List<SolicitudCertificado> findByCedulaOrderByFechaSolicitudDesc(String cedula);
    List<SolicitudCertificado> findByCedulaDependenciaOrderByFechaSolicitudDesc(String cedulaDependencia);
    boolean existsByCedulaAndTipoCertificadoAndEstado(String cedula, String tipoCertificado, String estado);
}
