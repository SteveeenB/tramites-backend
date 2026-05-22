package com.ufps.tramites.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cedula;
    private String codigo;

    @Column(name = "primer_nombre")
    private String primerNombre;

    @Column(name = "primer_apellido")
    private String primerApellido;

    @Column(name = "nombre_completo")
    private String nombreCompleto;

    private String email;
    @Column(name = "contrasena")
    private String password;

    @Column(name = "google_id")
    private String googleId;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(name = "moodle_id")
    private String moodleId;

    private String telefono;

    @ManyToOne
    @JoinColumn(name = "rol_id")
    private Rol rol;

    // Campos propios del módulo de tramites (no existen en el sistema externo aún)
    @Column(name = "creditos_aprobados")
    private Integer creditosAprobados;

    @Column(name = "estado_grado")
    private String estadoGrado;

    @ManyToOne
    @JoinColumn(name = "programa_id")
    private ProgramaAcademico programaAcademico;

    public Usuario() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getPrimerNombre() { return primerNombre; }
    public void setPrimerNombre(String primerNombre) { this.primerNombre = primerNombre; }

    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(String primerApellido) { this.primerApellido = primerApellido; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getMoodleId() { return moodleId; }
    public void setMoodleId(String moodleId) { this.moodleId = moodleId; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    public Integer getCreditosAprobados() { return creditosAprobados; }
    public void setCreditosAprobados(Integer creditosAprobados) { this.creditosAprobados = creditosAprobados; }

    public String getEstadoGrado() { return estadoGrado; }
    public void setEstadoGrado(String estadoGrado) { this.estadoGrado = estadoGrado; }

    public ProgramaAcademico getProgramaAcademico() { return programaAcademico; }
    public void setProgramaAcademico(ProgramaAcademico programaAcademico) { this.programaAcademico = programaAcademico; }

    // Helpers para compatibilidad interna con código existente
    public String getNombre() { return nombreCompleto; }
    public String getCorreo() { return email; }
    public String getRolNombre() { return rol != null ? rol.getNombre() : null; }
}
