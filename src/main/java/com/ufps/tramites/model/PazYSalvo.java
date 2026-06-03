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

    // FK formal al estudiante (Bloque 5a, plan_roles_v3 §4.1).
    // Doble-write transicional: cedula_estudiante se sigue poblando por
    // compatibilidad hasta que todo el código lea del FK.
    @ManyToOne
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    private String cedulaEstudiante;

    // FK polimórficas al responsable (uno de los dos, no ambos):
    //   - DIRECTOR de programa  → responsableUsuario (sigue siendo Usuario)
    //   - DEPENDENCIA/POSGRADOS → responsableAdmin   (migrados a admins en Bloque 3)
    @ManyToOne
    @JoinColumn(name = "responsable_admin_id")
    private Admin responsableAdmin;

    @ManyToOne
    @JoinColumn(name = "responsable_usuario_id")
    private Usuario responsableUsuario;

    private String tipoDependencia;   // BIBLIOTECA, FINANCIERA, ADMISIONES, DIRECTOR_PROGRAMA, etc.

    @ManyToOne
    @JoinColumn(name = "dependencia_id")
    private Dependencia dependencia;  // FK a entidad Dependencia (null para DIRECTOR_PROGRAMA)

    private String estado;            // PENDIENTE | APROBADO | RECHAZADO
    private String observaciones;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaRespuesta;

    public PazYSalvo() {}

    public Long getId() { return id; }
    public Long getSolicitudId() { return solicitudId; }
    public void setSolicitudId(Long solicitudId) { this.solicitudId = solicitudId; }
    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante estudiante) { this.estudiante = estudiante; }

    public String getCedulaEstudiante() { return cedulaEstudiante; }
    public void setCedulaEstudiante(String cedulaEstudiante) { this.cedulaEstudiante = cedulaEstudiante; }

    public Admin getResponsableAdmin() { return responsableAdmin; }
    public void setResponsableAdmin(Admin responsableAdmin) { this.responsableAdmin = responsableAdmin; }

    public Usuario getResponsableUsuario() { return responsableUsuario; }
    public void setResponsableUsuario(Usuario responsableUsuario) { this.responsableUsuario = responsableUsuario; }

    public Long getResponsableAdminId() {
        return responsableAdmin != null ? responsableAdmin.getId() : null;
    }
    public Long getResponsableUsuarioId() {
        return responsableUsuario != null ? responsableUsuario.getId() : null;
    }

    public String getTipoDependencia() { return tipoDependencia; }
    public void setTipoDependencia(String tipoDependencia) { this.tipoDependencia = tipoDependencia; }
    public Dependencia getDependencia() { return dependencia; }
    public void setDependencia(Dependencia dependencia) { this.dependencia = dependencia; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }
    public LocalDateTime getFechaRespuesta() { return fechaRespuesta; }
    public void setFechaRespuesta(LocalDateTime fechaRespuesta) { this.fechaRespuesta = fechaRespuesta; }
}
