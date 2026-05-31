package com.ufps.tramites.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    // FK formal al estudiante (Bloque 5a, plan_roles_v3 §4.1).
    // Doble-write transicional: el campo `cedula` se sigue poblando por
    // compatibilidad hasta que todo el código lea del FK.
    @ManyToOne
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    private String cedula;
    private String tipo;
    private String estado;
    private LocalDate fechaSolicitud;
    private Double costo;
    private String observaciones;

    // ── TP-41 ─────────────────────────────────────────────────────────────
    private String decision;
    private String observacionesDirector;
    private LocalDateTime fechaDecision;
    private String cedulaDirector;

    private LocalDateTime fechaEnRevision;

    // ── Solicitud de Grado ────────────────────────────────────────────────
    private String tituloProyecto;
    private String tipoProyecto;

    @Column(columnDefinition = "TEXT")
    private String resumenProyecto;

    // ── HU-09 ─────────────────────────────────────────────────────────────
    private String validacionPosgrados;
    private String observacionesPosgrados;
    private LocalDateTime fechaValidacion;

    // FK formal al admin POSGRADOS que validó/aprobó (refactor Bloque 2)
    @ManyToOne
    @JoinColumn(name = "posgrados_admin_id")
    private Admin posgradosAdmin;

    // ── Acta de terminación ────────────────────────────────────────────────
    private Boolean actaGenerada;

    // ── Radicado ──────────────────────────────────────────────────────────
    @Column(unique = true)
    private String radicado;

    // ── Proceso de Grado (pago y fecha) ───────────────────────────────────
    private String estadoPagoGrado;   // null | APROBADO
    private LocalDate fechaGrado;

    // ── Modalidad de grado ─────────────────────────────────────────────────
    private String modalidadGrado;         // CEREMONIA | SECRETARIA
    private Boolean pagoModalidadRealizado = false;

    // ── Constructor ───────────────────────────────────────────────────────
    public Solicitud() {}

    // ── Getters y setters ─────────────────────────────────────────────────

    public Long getId() { return id; }

    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante estudiante) { this.estudiante = estudiante; }

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

    public LocalDateTime getFechaEnRevision() { return fechaEnRevision; }
    public void setFechaEnRevision(LocalDateTime fechaEnRevision) { this.fechaEnRevision = fechaEnRevision; }

    public String getTituloProyecto() { return tituloProyecto; }
    public void setTituloProyecto(String tituloProyecto) { this.tituloProyecto = tituloProyecto; }

    public String getTipoProyecto() { return tipoProyecto; }
    public void setTipoProyecto(String tipoProyecto) { this.tipoProyecto = tipoProyecto; }

    public String getResumenProyecto() { return resumenProyecto; }
    public void setResumenProyecto(String resumenProyecto) { this.resumenProyecto = resumenProyecto; }

    public String getValidacionPosgrados() { return validacionPosgrados; }
    public void setValidacionPosgrados(String v) { this.validacionPosgrados = v; }

    public String getObservacionesPosgrados() { return observacionesPosgrados; }
    public void setObservacionesPosgrados(String o) { this.observacionesPosgrados = o; }

    public LocalDateTime getFechaValidacion() { return fechaValidacion; }
    public void setFechaValidacion(LocalDateTime f) { this.fechaValidacion = f; }

    public Admin getPosgradosAdmin() { return posgradosAdmin; }
    public void setPosgradosAdmin(Admin admin) { this.posgradosAdmin = admin; }

    public Long getPosgradosAdminId() { return posgradosAdmin != null ? posgradosAdmin.getId() : null; }

    public boolean isActaGenerada() { return Boolean.TRUE.equals(actaGenerada); }
    public void setActaGenerada(boolean actaGenerada) { this.actaGenerada = actaGenerada; }

    public String getRadicado() { return radicado; }
    public void setRadicado(String radicado) { this.radicado = radicado; }

    public String getEstadoPagoGrado() { return estadoPagoGrado; }
    public void setEstadoPagoGrado(String estadoPagoGrado) { this.estadoPagoGrado = estadoPagoGrado; }

    public LocalDate getFechaGrado() { return fechaGrado; }
    public void setFechaGrado(LocalDate fechaGrado) { this.fechaGrado = fechaGrado; }

    public String getModalidadGrado() { return modalidadGrado; }
    public void setModalidadGrado(String modalidadGrado) { this.modalidadGrado = modalidadGrado; }

    public Boolean getPagoModalidadRealizado() { return pagoModalidadRealizado; }
    public void setPagoModalidadRealizado(Boolean v) { this.pagoModalidadRealizado = v; }
}
