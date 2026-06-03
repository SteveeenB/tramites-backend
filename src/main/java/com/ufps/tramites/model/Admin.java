package com.ufps.tramites.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "admins")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String codigo;

    @Column(name = "primer_nombre")
    private String primerNombre;

    @Column(name = "segundo_nombre")
    private String segundoNombre;

    @Column(name = "primer_apellido")
    private String primerApellido;

    @Column(name = "segundo_apellido")
    private String segundoApellido;

    @Column(name = "nombre_completo")
    private String nombreCompleto;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "es_super_admin")
    private Boolean esSuperAdmin = false;

    private Boolean active = true;

    private String tipo;  // 'SUPER' | 'POSGRADOS' | 'DEPENDENCIA'

    @ManyToOne
    @JoinColumn(name = "dependencia_id")
    private Dependencia dependencia;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    public Admin() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getPrimerNombre() { return primerNombre; }
    public void setPrimerNombre(String primerNombre) { this.primerNombre = primerNombre; }

    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(String segundoNombre) { this.segundoNombre = segundoNombre; }

    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(String primerApellido) { this.primerApellido = primerApellido; }

    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(String segundoApellido) { this.segundoApellido = segundoApellido; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Boolean getEsSuperAdmin() { return esSuperAdmin; }
    public void setEsSuperAdmin(Boolean esSuperAdmin) { this.esSuperAdmin = esSuperAdmin; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Dependencia getDependencia() { return dependencia; }
    public void setDependencia(Dependencia dependencia) { this.dependencia = dependencia; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    // Mapea el `tipo` de admin al `rol` que esperan los @PreAuthorize.
    // SUPER → ADMIN (configurador), POSGRADOS y DEPENDENCIA conservan su nombre.
    public String getRolNombre() {
        if (tipo == null) return null;
        return "SUPER".equals(tipo) ? "ADMIN" : tipo;
    }

    public Long getDependenciaId() {
        return dependencia != null ? dependencia.getId() : null;
    }

    public String getDependenciaNombre() {
        return dependencia != null ? dependencia.getNombre() : null;
    }
}
