package com.ufps.tramites.dto;

public class UsuarioResponseDTO {
    private String cedula;
    private String nombre;
    private String codigo;
    private String rol;

    public UsuarioResponseDTO() {
    }

    public UsuarioResponseDTO(String cedula, String nombre, String codigo, String rol) {
        this.cedula = cedula;
        this.nombre = nombre;
        this.codigo = codigo;
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

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
