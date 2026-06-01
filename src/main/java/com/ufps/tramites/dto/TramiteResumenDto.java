// TramiteResumenDto.java
package com.ufps.tramites.dto;

import java.time.LocalDate;

public class TramiteResumenDto {
    private Long id;
    private String tipo;
    private String estado;
    private LocalDate fechaSolicitud;
    private Double costo;
    private String observaciones;
    private boolean certificadoDisponible;

    public TramiteResumenDto() {}

    public TramiteResumenDto(Long id, String tipo, String estado,
                              LocalDate fechaSolicitud, Double costo,
                              String observaciones, boolean certificadoDisponible) {
        this.id = id;
        this.tipo = tipo;
        this.estado = estado;
        this.fechaSolicitud = fechaSolicitud;
        this.costo = costo;
        this.observaciones = observaciones;
        this.certificadoDisponible = certificadoDisponible;
    }

    public Long getId() { return id; }
    public String getTipo() { return tipo; }
    public String getEstado() { return estado; }
    public LocalDate getFechaSolicitud() { return fechaSolicitud; }
    public Double getCosto() { return costo; }
    public String getObservaciones() { return observaciones; }
    public boolean isCertificadoDisponible() { return certificadoDisponible; }
}