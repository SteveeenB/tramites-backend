package com.ufps.tramites.event;

import org.springframework.context.ApplicationEvent;

import com.ufps.tramites.model.SolicitudCertificado;
import com.ufps.tramites.model.Usuario;

public class CertificadoEstadoCambiadoEvent extends ApplicationEvent {

    private final SolicitudCertificado solicitud;
    private final Usuario actor;
    private final String estadoAnterior;

    public CertificadoEstadoCambiadoEvent(Object source, SolicitudCertificado solicitud,
                                           Usuario actor, String estadoAnterior) {
        super(source);
        this.solicitud = solicitud;
        this.actor = actor;
        this.estadoAnterior = estadoAnterior;
    }

    public SolicitudCertificado getSolicitud() { return solicitud; }
    public Usuario getActor() { return actor; }
    public String getEstadoAnterior() { return estadoAnterior; }
}