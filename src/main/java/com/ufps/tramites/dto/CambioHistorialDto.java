// CambioHistorialDto.java
package com.ufps.tramites.dto;

import java.time.LocalDateTime;

public class CambioHistorialDto {
    private Long id;
    private String estadoAnterior;
    private String estadoNuevo;
    private String actor;
    private String rolActor;
    private String observaciones;
    private LocalDateTime fechaCambio;

    public CambioHistorialDto() {}

    public CambioHistorialDto(Long id, String estadoAnterior, String estadoNuevo,
                               String actor, String rolActor,
                               String observaciones, LocalDateTime fechaCambio) {
        this.id = id;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.actor = actor;
        this.rolActor = rolActor;
        this.observaciones = observaciones;
        this.fechaCambio = fechaCambio;
    }

    public Long getId() { return id; }
    public String getEstadoAnterior() { return estadoAnterior; }
    public String getEstadoNuevo() { return estadoNuevo; }
    public String getActor() { return actor; }
    public String getRolActor() { return rolActor; }
    public String getObservaciones() { return observaciones; }
    public LocalDateTime getFechaCambio() { return fechaCambio; }
}