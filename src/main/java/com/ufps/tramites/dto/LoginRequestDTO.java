package com.ufps.tramites.dto;

public class LoginRequestDTO {
    private String codigo;
    private String contrasena;

    public LoginRequestDTO() {
    }

    public LoginRequestDTO(String codigo, String contrasena) {
        this.codigo = codigo;
        this.contrasena = contrasena;
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
}
