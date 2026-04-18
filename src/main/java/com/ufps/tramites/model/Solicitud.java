package com.ufps.tramites.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cedula;       // cédula del estudiante
    private String tipo;         // TERMINACION_MATERIAS | GRADO | CERTIFICADO
    private String estado;       // PENDIENTE_PAGO | EN_REVISION | APROBADA | RECHAZADA
    private LocalDate fechaSolicitud;
    private Double costo;
    private String observaciones;

    public Solicitud() {}

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

    // ── Campos nuevos para TP-41 ──────────────────────────────────────────

    @Column(name = "decision")
    @Enumerated(EnumType.STRING)
    private DecisionDirector decision;           // APROBADA | RECHAZADA | null si aún no decidió

    @Column(name = "observaciones_director")
    private String observacionesDirector;        // separado de observaciones del estudiante

    @Column(name = "fecha_decision")
    private LocalDateTime fechaDecision;         // cuándo decidió — base para la alerta de plazo

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director_id")
    private Usuario responsableDecision;         // quién decidió — trazabilidad HU-04

    // getters y setters

}
