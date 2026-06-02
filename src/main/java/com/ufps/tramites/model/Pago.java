package com.ufps.tramites.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String referencia;           // referencia única enviada a Wompi

    @Column(name = "solicitud_id", nullable = false)
    private Long solicitudId;

    @Column(name = "cedula_estudiante")
    private String cedulaEstudiante;

    @Column(name = "tipo_pago")
    private String tipoPago;             // TERMINACION | GRADO | MODALIDAD_CEREMONIA

    @Column(name = "monto_centavos", nullable = false)
    private Long montoCentavos;          // monto en centavos (COP × 100)

    @Column(nullable = false)
    private String estado = "PENDIENTE"; // PENDIENTE | APROBADO | RECHAZADO | ANULADO | ERROR

    @Column(name = "wompi_transaction_id")
    private String wompiTransactionId;

    @Column(name = "redirect_url")
    private String redirectUrl;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public Pago() {}

    // Getters y setters

    public Long getId() { return id; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public Long getSolicitudId() { return solicitudId; }
    public void setSolicitudId(Long solicitudId) { this.solicitudId = solicitudId; }

    public String getCedulaEstudiante() { return cedulaEstudiante; }
    public void setCedulaEstudiante(String cedulaEstudiante) { this.cedulaEstudiante = cedulaEstudiante; }

    public String getTipoPago() { return tipoPago; }
    public void setTipoPago(String tipoPago) { this.tipoPago = tipoPago; }

    public Long getMontoCentavos() { return montoCentavos; }
    public void setMontoCentavos(Long montoCentavos) { this.montoCentavos = montoCentavos; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getWompiTransactionId() { return wompiTransactionId; }
    public void setWompiTransactionId(String wompiTransactionId) { this.wompiTransactionId = wompiTransactionId; }

    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}