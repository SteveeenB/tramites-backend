package com.ufps.tramites.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "paz_y_salvo")
public class PazYSalvo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long solicitudId;        // ID de la solicitud de GRADO
    private String cedulaEstudiante;
    private String cedulaResponsable; // cédula del usuario DEPENDENCIA o DIRECTOR que debe responder
    private String tipoDependencia;   // BIBLIOTECA, FINANCIERA, ADMISIONES, DIRECTOR_PROGRAMA, etc.
    private String estado;            // PENDIENTE | APROBADO | RECHAZADO
    private String observaciones;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaRespuesta;

    public PazYSalvo() {}

    public Long getId() { return id; }
    public Long getSolicitudId() { return solicitudId; }
    public void setSolicitudId(Long solicitudId) { this.solicitudId = solicitudId; }
    public String getCedulaEstudiante() { return cedulaEstudiante; }
    public void setCedulaEstudiante(String cedulaEstudiante) { this.cedulaEstudiante = cedulaEstudiante; }
    public String getCedulaResponsable() { return cedulaResponsable; }
    public void setCedulaResponsable(String cedulaResponsable) { this.cedulaResponsable = cedulaResponsable; }
    public String getTipoDependencia() { return tipoDependencia; }
    public void setTipoDependencia(String tipoDependencia) { this.tipoDependencia = tipoDependencia; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }
    public LocalDateTime getFechaRespuesta() { return fechaRespuesta; }
    public void setFechaRespuesta(LocalDateTime fechaRespuesta) { this.fechaRespuesta = fechaRespuesta; }
}
