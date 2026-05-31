package com.ufps.tramites.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "estudiante")
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identidad personal (espejo de edu_virtual_ufps.estudiantes)
    private String nombre;
    private String nombre2;
    private String apellido;
    private String apellido2;
    private String cedula;
    private String codigo;
    private String email;
    private String telefono;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    // Académico / sistema
    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Column(name = "moodle_id")
    private String moodleId;

    @Column(name = "es_posgrado")
    private Boolean esPosgrado;

    private Boolean migrado;

    // Campos propios del módulo de trámites (movidos desde Usuario)
    @Column(name = "creditos_aprobados")
    private Integer creditosAprobados;

    @Column(name = "estado_grado")
    private String estadoGrado;

    // FK a ProgramaAcademico (entidad existente)
    @ManyToOne
    @JoinColumn(name = "programa_id")
    private ProgramaAcademico programa;

    // FK stubs — entidades pendientes de crear (Cohorte, EstadoEstudiante, Pensum)
    @Column(name = "cohorte_id")
    private Long cohorteId;

    @Column(name = "estado_estudiante_id")
    private Long estadoEstudianteId;

    @Column(name = "pensum_id")
    private Long pensumId;

    // FK a Usuario — igual que edu_virtual_ufps: Estudiante apunta a Usuario.
    // referencedColumnName = "id" es obligatorio porque la PK real de
    // `usuario` en BD es `cedula` (varchar), no `id`. Sin esto Hibernate
    // genera `references usuario` sin columna y PostgreSQL asume la PK,
    // fallando con type mismatch (bigint vs varchar). El UNIQUE en
    // usuario.id ya se añadió en migracion_admins_bloque2_3.sql §3a.
    @OneToOne
    @JoinColumn(name = "usuario_id", referencedColumnName = "id", unique = true)
    private Usuario usuario;

    public Estudiante() {}

    // ── Getters y setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getNombre2() { return nombre2; }
    public void setNombre2(String nombre2) { this.nombre2 = nombre2; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getApellido2() { return apellido2; }
    public void setApellido2(String apellido2) { this.apellido2 = apellido2; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDate fechaIngreso) { this.fechaIngreso = fechaIngreso; }

    public String getMoodleId() { return moodleId; }
    public void setMoodleId(String moodleId) { this.moodleId = moodleId; }

    public Boolean getEsPosgrado() { return esPosgrado; }
    public void setEsPosgrado(Boolean esPosgrado) { this.esPosgrado = esPosgrado; }

    public Boolean getMigrado() { return migrado; }
    public void setMigrado(Boolean migrado) { this.migrado = migrado; }

    public Integer getCreditosAprobados() { return creditosAprobados; }
    public void setCreditosAprobados(Integer creditosAprobados) { this.creditosAprobados = creditosAprobados; }

    public String getEstadoGrado() { return estadoGrado; }
    public void setEstadoGrado(String estadoGrado) { this.estadoGrado = estadoGrado; }

    public ProgramaAcademico getPrograma() { return programa; }
    public void setPrograma(ProgramaAcademico programa) { this.programa = programa; }

    public Long getCohorteId() { return cohorteId; }
    public void setCohorteId(Long cohorteId) { this.cohorteId = cohorteId; }

    public Long getEstadoEstudianteId() { return estadoEstudianteId; }
    public void setEstadoEstudianteId(Long estadoEstudianteId) { this.estadoEstudianteId = estadoEstudianteId; }

    public Long getPensumId() { return pensumId; }
    public void setPensumId(Long pensumId) { this.pensumId = pensumId; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
