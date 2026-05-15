package com.ufps.tramites.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "solicitud_certificado")
public class SolicitudCertificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cedula;           // cédula del estudiante
    private String tipoCertificado;  // NOTAS | ESTUDIO | GRADUACION | RANKING
    private String modalidadEnvio;   // FISICO | DIGITAL
    private String estado;           // PENDIENTE_PAGO | PAGADO | GENERADO
    private LocalDate fechaSolicitud;
    private Double costo;
    private String observaciones;
    private String destinatario;     // opcional — entidad a quien va dirigido

    // ── Constructor ───────────────────────────────────────────────────────
    public SolicitudCertificado() {}

    // ── Getters y setters ─────────────────────────────────────────────────
    public Long getId() { return id; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getTipoCertificado() { return tipoCertificado; }
    public void setTipoCertificado(String tipoCertificado) {
        this.tipoCertificado = tipoCertificado;
    }

    public String getModalidadEnvio() { return modalidadEnvio; }
    public void setModalidadEnvio(String modalidadEnvio) {
        this.modalidadEnvio = modalidadEnvio;
    }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDate getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDate fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public Double getCosto() { return costo; }
    public void setCosto(Double costo) { this.costo = costo; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }
}