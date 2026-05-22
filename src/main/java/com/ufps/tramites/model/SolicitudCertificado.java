package com.ufps.tramites.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitud_certificado")
public class SolicitudCertificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cedula;
    private String tipoCertificado;   // TipoCertificado.codigo
    private String modalidadEnvio;    // DIGITAL | FISICA
    private String estado;            // PENDIENTE_PAGO | PAGADO | GENERADO | LISTO_RETIRO | ENTREGADO | VENCIDA
    private Double costo;
    private String cedulaDependencia; // quién gestiona la entrega física
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaVencimientoPago;
    private LocalDateTime fechaEntrega;

    public SolicitudCertificado() {}

    public Long getId() { return id; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getTipoCertificado() { return tipoCertificado; }
    public void setTipoCertificado(String tipoCertificado) { this.tipoCertificado = tipoCertificado; }

    public String getModalidadEnvio() { return modalidadEnvio; }
    public void setModalidadEnvio(String modalidadEnvio) { this.modalidadEnvio = modalidadEnvio; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Double getCosto() { return costo; }
    public void setCosto(Double costo) { this.costo = costo; }

    public String getCedulaDependencia() { return cedulaDependencia; }
    public void setCedulaDependencia(String cedulaDependencia) { this.cedulaDependencia = cedulaDependencia; }

    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public LocalDateTime getFechaVencimientoPago() { return fechaVencimientoPago; }
    public void setFechaVencimientoPago(LocalDateTime fechaVencimientoPago) { this.fechaVencimientoPago = fechaVencimientoPago; }

    public LocalDateTime getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDateTime fechaEntrega) { this.fechaEntrega = fechaEntrega; }
}
