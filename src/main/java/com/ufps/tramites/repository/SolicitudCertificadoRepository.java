package com.ufps.tramites.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ufps.tramites.model.SolicitudCertificado;

@Repository
public interface SolicitudCertificadoRepository extends JpaRepository<SolicitudCertificado, Long> {

    List<SolicitudCertificado> findByCedula(String cedula);

    List<SolicitudCertificado> findByCedulaAndEstado(String cedula, String estado);

    List<SolicitudCertificado> findByCedulaAndTipoCertificado(String cedula, String tipoCertificado);

    List<SolicitudCertificado> findByEstado(String estado);

    /**
     * Solicitudes físicas que la dependencia debe gestionar (imprimir / entregar).
     * Se hace JOIN lógico por código contra tipo_certificado.
     */
    @Query("""
           SELECT s FROM SolicitudCertificado s, TipoCertificado t
           WHERE s.tipoCertificado = t.codigo
             AND t.dependenciaCedula = :cedulaDependencia
             AND s.modalidadEnvio = 'FISICA'
             AND (:estado IS NULL OR s.estado = :estado)
           ORDER BY s.fechaSolicitud DESC, s.id DESC
           """)
    List<SolicitudCertificado> findByDependencia(@Param("cedulaDependencia") String cedulaDependencia,
                                                 @Param("estado") String estado);
}
