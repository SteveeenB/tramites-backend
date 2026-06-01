package com.ufps.tramites.event;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ufps.tramites.model.HistorialEstadoTramite;
import com.ufps.tramites.repository.HistorialEstadoTramiteRepository;

@Component
public class HistorialEstadoListener {

    @Autowired
    private HistorialEstadoTramiteRepository historialRepository;

    @EventListener
    public void onEstadoCambiado(SolicitudEstadoCambiadoEvent event) {
        HistorialEstadoTramite registro = new HistorialEstadoTramite();
        registro.setSolicitudId(event.getSolicitud().getId());
        registro.setTipoTramite(event.getSolicitud().getTipo());
        registro.setEstadoAnterior(event.getEstadoAnterior());
        registro.setEstadoNuevo(event.getSolicitud().getEstado());
        registro.setActor(event.getEstudiante().getCedula());
        registro.setRolActor(event.getEstudiante().getRolNombre());
        registro.setObservaciones(event.getSolicitud().getObservaciones());
        registro.setFechaCambio(LocalDateTime.now());
        historialRepository.save(registro);
    }
}