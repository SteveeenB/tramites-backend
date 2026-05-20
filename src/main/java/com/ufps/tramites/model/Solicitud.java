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

    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String resumenProyecto;

    // ── HU-09 ─────────────────────────────────────────────────────────────
    private String validacionPosgrados;
    private String observacionesPosgrados;
    private LocalDateTime fechaValidacion;
    private String cedulaPosgrados;

    // ── Acta de terminación ────────────────────────────────────────────────
    private Boolean actaGenerada;

    // ── Radicado ──────────────────────────────────────────────────────────
    @jakarta.persistence.Column(unique = true)
    private String radicado;

    // ── Proceso de Grado (pago y fecha) ───────────────────────────────────
    private String estadoPagoGrado;   // null | APROBADO
    private LocalDate fechaGrado;

    // ── Constructor ───────────────────────────────────────────────────────
    public Solicitud() {}

    // ── Getters y setters ─────────────────────────────────────────────────

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

    public String getCedulaPosgrados() { return cedulaPosgrados; }
    public void setCedulaPosgrados(String c) { this.cedulaPosgrados = c; }

    public boolean isActaGenerada() { return Boolean.TRUE.equals(actaGenerada); }
    public void setActaGenerada(boolean actaGenerada) { this.actaGenerada = actaGenerada; }

    public String getRadicado() { return radicado; }
    public void setRadicado(String radicado) { this.radicado = radicado; }

    public String getEstadoPagoGrado() { return estadoPagoGrado; }
    public void setEstadoPagoGrado(String estadoPagoGrado) { this.estadoPagoGrado = estadoPagoGrado; }

    public LocalDate getFechaGrado() { return fechaGrado; }
    public void setFechaGrado(LocalDate fechaGrado) { this.fechaGrado = fechaGrado; }

    // ── Modalidad de grado ─────────────────────────────────────────────────
    private String modalidadGrado;         // CEREMONIA | SECRETARIA
    private Boolean pagoModalidadRealizado = false;

    // ── Integridad del PDF generado ─────────────────────────────────────────
    private String urlPdf;        // ruta en Supabase Storage del PDF de terminación
    private String hashPdf;       // SHA-256 hex del PDF en su primera generación
    private String firmaDigital;  // Base64 RSA-2048/SHA256withRSA sobre los bytes del PDF
    
    public String getModalidadGrado() { return modalidadGrado; }
    public void setModalidadGrado(String modalidadGrado) { this.modalidadGrado = modalidadGrado; }

    public Boolean getPagoModalidadRealizado() { return pagoModalidadRealizado; }
    public void setPagoModalidadRealizado(Boolean v) { this.pagoModalidadRealizado = v; }

    public String getUrlPdf() { return urlPdf; }
    public void setUrlPdf(String urlPdf) { this.urlPdf = urlPdf; }

    public String getHashPdf() { return hashPdf; }
    public void setHashPdf(String hashPdf) { this.hashPdf = hashPdf; }

    public String getFirmaDigital() { return firmaDigital; }
    public void setFirmaDigital(String firmaDigital) { this.firmaDigital = firmaDigital; }
}