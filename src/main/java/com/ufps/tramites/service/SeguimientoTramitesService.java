package com.ufps.tramites.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ufps.tramites.dto.CambioHistorialDto;
import com.ufps.tramites.dto.TramiteResumenDto;
import com.ufps.tramites.model.HistorialEstadoTramite;
import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.SolicitudCertificado;
import com.ufps.tramites.repository.HistorialEstadoTramiteRepository;
import com.ufps.tramites.repository.SolicitudCertificadoRepository;
import com.ufps.tramites.repository.SolicitudRepository;

@Service
public class SeguimientoTramitesService {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private SolicitudCertificadoRepository certificadoRepository;

    @Autowired
    private HistorialEstadoTramiteRepository historialRepository;

    public List<TramiteResumenDto> obtenerMisTramites(String cedula) {
        List<TramiteResumenDto> resultado = new ArrayList<>();

        // Terminaciones y grados — busca por cedula del estudiante
        List<Solicitud> solicitudes = solicitudRepository.findByCedula(cedula);
        for (Solicitud s : solicitudes) {
            resultado.add(new TramiteResumenDto(
                s.getId(),
                s.getTipo(),
                s.getEstado(),
                s.getFechaSolicitud(),
                s.getCosto(),
                s.getObservaciones(),
                "APROBADA".equals(s.getEstado()) && "TERMINACION_MATERIAS".equals(s.getTipo())
            ));
        }

        // Certificados
        List<SolicitudCertificado> certificados = certificadoRepository.findByCedula(cedula);
        for (SolicitudCertificado c : certificados) {
            resultado.add(new TramiteResumenDto(
                c.getId(),
                "CERTIFICADO_" + c.getTipoCertificado(),
                c.getEstado(),
                c.getFechaSolicitud(),
                c.getCosto(),
                c.getObservaciones(),
                "PAGADO".equals(c.getEstado())
            ));
        }

        resultado.sort(Comparator.comparing(
            TramiteResumenDto::getFechaSolicitud,
            Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return resultado;
    }

    public List<CambioHistorialDto> obtenerHistorial(Long solicitudId) {
        List<HistorialEstadoTramite> registros = historialRepository
            .findBySolicitudIdOrderByFechaCambioAsc(solicitudId);

        List<CambioHistorialDto> resultado = new ArrayList<>();
        for (HistorialEstadoTramite r : registros) {
            resultado.add(new CambioHistorialDto(
                r.getId(),
                r.getEstadoAnterior(),
                r.getEstadoNuevo(),
                r.getActor(),
                r.getRolActor(),
                r.getObservaciones(),
                r.getFechaCambio()
            ));
        }
        return resultado;
    }
}