package com.ufps.tramites.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ufps.tramites.model.SolicitudCertificado;

@Repository
public interface SolicitudCertificadoRepository extends JpaRepository<SolicitudCertificado, Long> {

    List<SolicitudCertificado> findByCedula(String cedula);

    List<SolicitudCertificado> findByCedulaAndEstado(String cedula, String estado);

    List<SolicitudCertificado> findByCedulaAndTipoCertificado(String cedula, String tipoCertificado);

    List<SolicitudCertificado> findByEstado(String estado);
}