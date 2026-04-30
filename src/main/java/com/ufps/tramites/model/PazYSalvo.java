package com.ufps.tramites.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Representa el paz y salvo que una dependencia (Biblioteca, Tesorería,
 * Bienestar o Director) debe cargar en el sistema para un estudiante que
 * tiene la solicitud de GRADO aprobada.
 *
 * Flujo:
 *  1. Se aprueba la solicitud de GRADO del estudiante.
 *  2. El sistema crea automáticamente un registro PazYSalvo por cada
 *     dependencia con estado PENDIENTE y un token único.
 *  3. Se envían correos con el link de carga a cada dependencia.
 *  4. La dependencia abre el link, sube su documento → estado → CARGADO.
 *  5. El Director sube el suyo desde su vista autenticada (sin token).
 */
@Entity
@Table(name = "paz_y_salvo")
public class PazYSalvo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cédula del estudiante al que pertenece este paz y salvo. */
    private String cedulaEstudiante;

    /** ID de la solicitud de GRADO que disparó el proceso. */
    private Long solicitudId;

    /**
     * Identificador de la dependencia:
     * BIBLIOTECA | TESORERIA | BIENESTAR | DIRECTOR
     */
    private String dependencia;

    /** Nombre legible de la dependencia para mostrar en la UI. */
    private String nombreDependencia;

    /**
     * Token UUID enviado por correo. Permite a la dependencia subir su
     * documento sin necesidad de autenticarse en el sistema.
     * Null para el DIRECTOR (usa su sesión autenticada).
     */
    @Column(unique = true)
    private String token;

    /** PENDIENTE | CARGADO */
    private String estado;

    /** Nombre original del archivo subido (ej. "paz-salvo-biblioteca.pdf"). */
    private String archivoNombre;

    /** Contenido binario del archivo subido. */
    @Lob
    @Column(columnDefinition = "bytea")
    private byte[] archivoContenido;

    /** Tipo MIME del archivo (application/pdf, image/jpeg, etc.). */
    private String archivoTipo;

    /** Fecha en que se creó el registro (cuando se aprobó el GRADO). */
    private LocalDateTime fechaCreacion;

    /** Fecha en que la dependencia subió el documento. */
    private LocalDateTime fechaCarga;

    public PazYSalvo() {}

    // ── Getters y setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getCedulaEstudiante() { return cedulaEstudiante; }
    public void setCedulaEstudiante(String cedulaEstudiante) { this.cedulaEstudiante = cedulaEstudiante; }

    public Long getSolicitudId() { return solicitudId; }
    public void setSolicitudId(Long solicitudId) { this.solicitudId = solicitudId; }

    public String getDependencia() { return dependencia; }
    public void setDependencia(String dependencia) { this.dependencia = dependencia; }

    public String getNombreDependencia() { return nombreDependencia; }
    public void setNombreDependencia(String nombreDependencia) { this.nombreDependencia = nombreDependencia; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getArchivoNombre() { return archivoNombre; }
    public void setArchivoNombre(String archivoNombre) { this.archivoNombre = archivoNombre; }

    public byte[] getArchivoContenido() { return archivoContenido; }
    public void setArchivoContenido(byte[] archivoContenido) { this.archivoContenido = archivoContenido; }

    public String getArchivoTipo() { return archivoTipo; }
    public void setArchivoTipo(String archivoTipo) { this.archivoTipo = archivoTipo; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaCarga() { return fechaCarga; }
    public void setFechaCarga(LocalDateTime fechaCarga) { this.fechaCarga = fechaCarga; }
}
