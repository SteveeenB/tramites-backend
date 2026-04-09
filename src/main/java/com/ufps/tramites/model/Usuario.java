/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ufps.tramites.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 *
 * @author StevenB
 *
 *
 */
@Entity
public class Usuario {

    @Id
    private String cedula;
    private String codigo;
    private String nombre;
    private String contrasena;
    private String rol; // ESTUDIANTE, DIRECTOR, ADMIN, etc.

    public Usuario() {
    }

    public Usuario(String cedula, String codigo, String nombre, String contrasena, String rol) {
        this.cedula = cedula;
        this.codigo = codigo;
        this.nombre = nombre;
        this.contrasena = contrasena;
        this.rol = rol;
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
}
