package com.ufps.tramites.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

// FIX TP-201 (Johan Bueno, 07/10/2026): historial de cambios de estado de
// una Solicitud. Cada vez que una transición es relevante (aprobar,
// rechazar, pagar, validar) se persiste una fila con el estado anterior,
// el nuevo, la fecha y el actor que la ejecutó. Alimenta el endpoint
// GET /api/solicitudes/{id}/historial y los reportes de trazabilidad
// (CP-046).
@Entity
@Table(
    name = "historial_estado_solicitud",
    indexes = {
        @Index(name = "idx_hes_solicitud_id", columnList = "solicitud_id"),
        @Index(name = "idx_hes_fecha",        columnList = "fecha")
    }
)
public class CambioEstadoSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitud_id", nullable = false)
    private Long solicitudId;

    @Column(name = "estado_anterior")
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false)
    private String estadoNuevo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    /** Cédula del Usuario o código del Admin que ejecutó la transición. */
    @Column(name = "actor_identificador")
    private String actor;

    /** USUARIO | ADMIN | SISTEMA — ayuda a entender quién actuó sin tener
     *  que consultar las dos tablas por el identificador. */
    @Column(name = "actor_tipo")
    private String actorTipo;

    public CambioEstadoSolicitud() {}

    public CambioEstadoSolicitud(Long solicitudId, String estadoAnterior, String estadoNuevo,
                                 LocalDateTime fecha, String actor, String actorTipo) {
        this.solicitudId    = solicitudId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo    = estadoNuevo;
        this.fecha          = fecha;
        this.actor          = actor;
        this.actorTipo      = actorTipo;
    }

    public Long getId() { return id; }
    public Long getSolicitudId() { return solicitudId; }
    public String getEstadoAnterior() { return estadoAnterior; }
    public String getEstadoNuevo() { return estadoNuevo; }
    public LocalDateTime getFecha() { return fecha; }
    public String getActor() { return actor; }
    public String getActorTipo() { return actorTipo; }

    public void setId(Long id) { this.id = id; }
    public void setSolicitudId(Long solicitudId) { this.solicitudId = solicitudId; }
    public void setEstadoAnterior(String estadoAnterior) { this.estadoAnterior = estadoAnterior; }
    public void setEstadoNuevo(String estadoNuevo) { this.estadoNuevo = estadoNuevo; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public void setActor(String actor) { this.actor = actor; }
    public void setActorTipo(String actorTipo) { this.actorTipo = actorTipo; }
}
