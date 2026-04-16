package com.ufps.tramites.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Usuario {

    @Id
    private String cedula;
    private String codigo;
    private String nombre;
    private String contrasena;
    private String rol; // ESTUDIANTE, DIRECTOR, ADMIN

    private Integer creditosAprobados;
    private String correo;

    @ManyToOne
    @JoinColumn(name = "programa_id")
    private ProgramaAcademico programaAcademico;

    public Usuario() {}

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Integer getCreditosAprobados() { return creditosAprobados; }
    public void setCreditosAprobados(Integer creditosAprobados) { this.creditosAprobados = creditosAprobados; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public ProgramaAcademico getProgramaAcademico() { return programaAcademico; }
    public void setProgramaAcademico(ProgramaAcademico programaAcademico) { this.programaAcademico = programaAcademico; }
}
