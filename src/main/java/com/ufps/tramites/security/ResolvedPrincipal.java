package com.ufps.tramites.security;

import com.ufps.tramites.model.Admin;
import com.ufps.tramites.model.Usuario;

/**
 * Vista unificada del actor autenticado, sirve tanto para Usuario (académicos)
 * como para Admin (operadores). Producido por PrincipalResolver desde la
 * Authentication del SecurityContext.
 *
 * Solo uno de los campos {usuario, admin} será non-null. type lo indica.
 */
public record ResolvedPrincipal(
        String type,            // "USUARIO" | "ADMIN"
        Long id,                // PK en su tabla de origen
        String cedula,          // null para ADMIN
        String codigo,
        String rol,             // ESTUDIANTE | DIRECTOR | POSGRADOS | DEPENDENCIA | ADMIN
        String nombreCompleto,
        String email,
        Long dependenciaId,     // null si no aplica
        String dependenciaNombre, // null si no aplica
        Usuario usuario,        // non-null sii type == "USUARIO"
        Admin admin             // non-null sii type == "ADMIN"
) {

    public boolean isAdmin()   { return JwtService.PRINCIPAL_ADMIN.equals(type); }
    public boolean isUsuario() { return JwtService.PRINCIPAL_USUARIO.equals(type); }

    public static ResolvedPrincipal ofUsuario(Usuario u) {
        // Usuarios académicos (ESTUDIANTE, DIRECTOR) no tienen dependencia —
        // esa relación vive en Admin. Pasamos null en dependenciaId/Nombre.
        return new ResolvedPrincipal(
                JwtService.PRINCIPAL_USUARIO,
                u.getId(),
                u.getCedula(),
                u.getCodigo(),
                u.getRolNombre(),
                u.getNombreCompleto(),
                u.getEmail(),
                null,
                null,
                u,
                null
        );
    }

    public static ResolvedPrincipal ofAdmin(Admin a) {
        return new ResolvedPrincipal(
                JwtService.PRINCIPAL_ADMIN,
                a.getId(),
                null,
                a.getCodigo(),
                a.getRolNombre(),
                a.getNombreCompleto(),
                a.getEmail(),
                a.getDependenciaId(),
                a.getDependenciaNombre(),
                null,
                a
        );
    }
}
