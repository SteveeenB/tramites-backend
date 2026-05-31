package com.ufps.tramites.dto;

/**
 * Respuesta del login. Sirve tanto para usuarios académicos (Usuario)
 * como para operadores administrativos (Admin); los campos que no aplican
 * a un tipo se devuelven en null.
 *
 * principalType distingue la fuente:
 *   - "USUARIO" → académicos (ESTUDIANTE, DIRECTOR). Lleva cedula, estudianteId, programaNombre.
 *   - "ADMIN"   → operadores (POSGRADOS, DEPENDENCIA, ADMIN). Lleva dependenciaId, esSuperAdmin.
 */
public class LoginResponseDTO {
    private String token;
    private Long id;
    private String cedula;
    private String nombreCompleto;
    private String email;
    private String codigo;
    private String rol;
    private String dependenciaNombre;
    private Long estudianteId;
    private String programaNombre;
    private String principalType;
    private Long dependenciaId;
    private Boolean esSuperAdmin;

    public LoginResponseDTO(String token, Long id, String cedula,
                             String nombreCompleto, String email,
                             String codigo, String rol, String dependenciaNombre,
                             Long estudianteId, String programaNombre,
                             String principalType, Long dependenciaId,
                             Boolean esSuperAdmin) {
        this.token = token;
        this.id = id;
        this.cedula = cedula;
        this.nombreCompleto = nombreCompleto;
        this.email = email;
        this.codigo = codigo;
        this.rol = rol;
        this.dependenciaNombre = dependenciaNombre;
        this.estudianteId = estudianteId;
        this.programaNombre = programaNombre;
        this.principalType = principalType;
        this.dependenciaId = dependenciaId;
        this.esSuperAdmin = esSuperAdmin;
    }

    public String getToken() { return token; }
    public Long getId() { return id; }
    public String getCedula() { return cedula; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getEmail() { return email; }
    public String getCodigo() { return codigo; }
    public String getRol() { return rol; }
    public String getDependenciaNombre() { return dependenciaNombre; }
    public Long getEstudianteId() { return estudianteId; }
    public String getProgramaNombre() { return programaNombre; }
    public String getPrincipalType() { return principalType; }
    public Long getDependenciaId() { return dependenciaId; }
    public Boolean getEsSuperAdmin() { return esSuperAdmin; }
}
