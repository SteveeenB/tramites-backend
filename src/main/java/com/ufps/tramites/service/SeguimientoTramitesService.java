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

    @Autowired private SolicitudRepository solicitudRepository;
    @Autowired private SolicitudCertificadoRepository certificadoRepository;
    @Autowired private HistorialEstadoTramiteRepository historialRepository;

    public List<TramiteResumenDto> obtenerMisTramites(String cedula) {
        List<TramiteResumenDto> resultado = new ArrayList<>();

        // Terminaciones y grados
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

    /**
     * Historial de solicitudes (TERMINACION_MATERIAS, GRADO).
     * Filtra por tipoTramite que NO empiece por CERTIFICADO_ para
     * evitar colisiones de id entre las dos tablas.
     */
    public List<CambioHistorialDto> obtenerHistorial(Long solicitudId) {
        List<HistorialEstadoTramite> registros = historialRepository
            .findBySolicitudIdAndTipoTramiteNotStartingWithOrderByFechaCambioAsc(
                solicitudId, "CERTIFICADO_");
        return mapearHistorial(registros);
    }

    /**
     * Historial de certificados.
     * Filtra por tipoTramite que empiece por CERTIFICADO_ para
     * evitar colisiones de id entre las dos tablas.
     */
    public List<CambioHistorialDto> obtenerHistorialCertificado(Long certificadoId) {
        List<HistorialEstadoTramite> registros = historialRepository
            .findBySolicitudIdAndTipoTramiteStartingWithOrderByFechaCambioAsc(
                certificadoId, "CERTIFICADO_");
        return mapearHistorial(registros);
    }

    private List<CambioHistorialDto> mapearHistorial(List<HistorialEstadoTramite> registros) {
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