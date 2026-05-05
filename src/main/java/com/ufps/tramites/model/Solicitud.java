package com.ufps.tramites.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cedula;          // cédula del estudiante
    private String tipo;            // TERMINACION_MATERIAS | GRADO | CERTIFICADO
    private String estado;          // PENDIENTE_PAGO | EN_REVISION | APROBADA | RECHAZADA
    private LocalDate fechaSolicitud;
    private Double costo;
    private String observaciones;   // observaciones del estudiante (no tocar)

    // ── Campos nuevos TP-41 ───────────────────────────────────────────────

    private String decision;              // APROBADA | RECHAZADA  (null mientras no decide)
    private String observacionesDirector; // motivo de aprobación o rechazo del director
    private LocalDateTime fechaDecision;  // cuándo decidió — base para alerta de plazo (TP-44)
    private String cedulaDirector;        // quién decidió — sigue tu convención de cédula como FK ligera

    // ── Constructor ───────────────────────────────────────────────────────

    public Solicitud() {}

    // ── Getters y setters existentes (sin cambios) ────────────────────────

    public Long getId() { return id; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDate getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDate fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public Double getCosto() { return costo; }
    public void setCosto(Double costo) { this.costo = costo; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    // ── Getters y setters nuevos TP-41 ────────────────────────────────────

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getObservacionesDirector() { return observacionesDirector; }
    public void setObservacionesDirector(String observacionesDirector) {
        this.observacionesDirector = observacionesDirector;
    }

    public LocalDateTime getFechaDecision() { return fechaDecision; }
    public void setFechaDecision(LocalDateTime fechaDecision) { this.fechaDecision = fechaDecision; }

    public String getCedulaDirector() { return cedulaDirector; }
    public void setCedulaDirector(String cedulaDirector) { this.cedulaDirector = cedulaDirector; }

    // campo
    private LocalDateTime fechaEnRevision;

    // getter
    public LocalDateTime getFechaEnRevision() { return fechaEnRevision; }

    // setter
    public void setFechaEnRevision(LocalDateTime fechaEnRevision) {
    this.fechaEnRevision = fechaEnRevision;
}

// ── Solicitud de Grado ────────────────────────────────────────────────
private String tituloProyecto;
private String tipoProyecto;   // INVESTIGACION | MONOGRAFIA | SISTEMATIZACION | TRABAJO_DIRIGIDO | PASANTIA

@jakarta.persistence.Column(columnDefinition = "TEXT")
private String resumenProyecto;

public String getTituloProyecto() { return tituloProyecto; }
public void setTituloProyecto(String tituloProyecto) { this.tituloProyecto = tituloProyecto; }

public String getTipoProyecto() { return tipoProyecto; }
public void setTipoProyecto(String tipoProyecto) { this.tipoProyecto = tipoProyecto; }

public String getResumenProyecto() { return resumenProyecto; }
public void setResumenProyecto(String resumenProyecto) { this.resumenProyecto = resumenProyecto; }

// ── HU-09 ─────────────────────────────────────────────────────────────
private String validacionPosgrados;     // APROBADA | RECHAZADA
private String observacionesPosgrados;  // motivo de la decisión
private LocalDateTime fechaValidacion;  // cuándo validó
private String cedulaPosgrados;         // quién validó

// Getters y setters
public String getValidacionPosgrados() { return validacionPosgrados; }
public void setValidacionPosgrados(String v) { this.validacionPosgrados = v; }

public String getObservacionesPosgrados() { return observacionesPosgrados; }
public void setObservacionesPosgrados(String o) { this.observacionesPosgrados = o; }

public LocalDateTime getFechaValidacion() { return fechaValidacion; }
public void setFechaValidacion(LocalDateTime f) { this.fechaValidacion = f; }

public String getCedulaPosgrados() { return cedulaPosgrados; }
public void setCedulaPosgrados(String c) { this.cedulaPosgrados = c; }

// ── Pago y fecha de grado ──────────────────────────────────────────────
private Boolean pagoGradoRealizado = false;
private java.time.LocalDate fechaGrado;

public Boolean getPagoGradoRealizado() { return pagoGradoRealizado; }
public void setPagoGradoRealizado(Boolean pagoGradoRealizado) {
    this.pagoGradoRealizado = pagoGradoRealizado;
}

public java.time.LocalDate getFechaGrado() { return fechaGrado; }
public void setFechaGrado(java.time.LocalDate fechaGrado) { this.fechaGrado = fechaGrado; }

}