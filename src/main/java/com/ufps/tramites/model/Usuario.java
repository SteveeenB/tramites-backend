/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ufps.tramites.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.Set;

/**
 *
 * @author StevenB
 *
 *
 */
@Entity
public class Usuario {

    private static final Set<String> PROGRAMAS_ACADEMICOS_VALIDOS = Set.of(
            "Maestría en Gerencia de Empresas",
            "Maestría en Estudios Sociales y Educación Para la Paz",
            "Maestría en Ingeniería de Recursos Hidráulicos",
            "Maestría en Tecnologías de la Información y la Comunicación aplicadas a la Educación",
            "Maestría en Educación Matemáticas",
            "Maestría en Práctica Pedagógica",
            "Maestría en Ciencias Biológicas",
            "Maestría en Negocios Internacionales",
            "Maestría en Derecho Público: Gobierno, Justicia y Derechos Humanos",
            "Especialización en Práctica Pedagógica",
            "Especialización en Estructuras",
            "Especialización en Logística y Negocios Internacionales",
            "Especialización en Educación, Emprendimiento y Economía Solidaria",
            "Especialización en Educación para la Atención a Población Afectada por el Conflicto Armado y en Problemática Fronteriza"
    );

    @Id
    private String cedula;
    private String codigo;
    private String nombre;
    private String contrasena;
    private String rol; // ESTUDIANTE, DIRECTOR, ADMIN, etc.
    private Integer creditosAprobados;
    private String programaAcademico;

    public Usuario() {
    }

    public Usuario(String cedula, String codigo, String nombre, String contrasena, String rol,
            Integer creditosAprobados, String programaAcademico) {
        this.cedula = cedula;
        this.codigo = codigo;
        this.nombre = nombre;
        this.contrasena = contrasena;
        this.rol = rol;
        this.creditosAprobados = creditosAprobados;
        setProgramaAcademico(programaAcademico);
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public Integer getCreditosAprobados() {
        return creditosAprobados;
    }

    public void setCreditosAprobados(Integer creditosAprobados) {
        this.creditosAprobados = creditosAprobados;
    }

    public String getProgramaAcademico() {
        return programaAcademico;
    }

    public void setProgramaAcademico(String programaAcademico) {
        validarProgramaAcademico(programaAcademico);
        this.programaAcademico = programaAcademico;
    }

    private void validarProgramaAcademico(String programaAcademico) {
        if (programaAcademico == null || !PROGRAMAS_ACADEMICOS_VALIDOS.contains(programaAcademico)) {
            throw new IllegalArgumentException("Programa academico no valido");
        }
    }
}
