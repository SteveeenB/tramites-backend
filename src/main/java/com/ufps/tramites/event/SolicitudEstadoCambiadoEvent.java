package com.ufps.tramites.event;

import com.ufps.tramites.model.Solicitud;
import com.ufps.tramites.model.Usuario;
import org.springframework.context.ApplicationEvent;

public class SolicitudEstadoCambiadoEvent extends ApplicationEvent {

    private final Solicitud solicitud;
    private final Usuario estudiante;
    private final String estadoAnterior;

    public SolicitudEstadoCambiadoEvent(Object source, Solicitud solicitud, Usuario estudiante, String estadoAnterior) {
        super(source);
        this.solicitud = solicitud;
        this.estudiante = estudiante;
        this.estadoAnterior = estadoAnterior;
    }

    public Solicitud getSolicitud() { return solicitud; }
    public Usuario getEstudiante() { return estudiante; }
    public String getEstadoAnterior() { return estadoAnterior; }
}
