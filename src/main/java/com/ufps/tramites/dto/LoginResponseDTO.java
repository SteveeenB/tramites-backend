package com.ufps.tramites.dto;

public class LoginResponseDTO {
    private String token;
    private Long id;
    private String cedula;
    private String nombreCompleto;
    private String email;
    private String codigo;
    private String rol;
    private String dependenciaNombre;

    public LoginResponseDTO(String token, Long id, String cedula,
                             String nombreCompleto, String email,
                             String codigo, String rol, String dependenciaNombre) {
        this.token = token;
        this.id = id;
        this.cedula = cedula;
        this.nombreCompleto = nombreCompleto;
        this.email = email;
        this.codigo = codigo;
        this.rol = rol;
        this.dependenciaNombre = dependenciaNombre;
    }

    public String getToken() { return token; }
    public Long getId() { return id; }
    public String getCedula() { return cedula; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getEmail() { return email; }
    public String getCodigo() { return codigo; }
    public String getRol() { return rol; }
    public String getDependenciaNombre() { return dependenciaNombre; }
}
