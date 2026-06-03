package com.ufps.tramites.security;

import com.ufps.tramites.model.Admin;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.AdminRepository;
import com.ufps.tramites.repository.UsuarioRepository;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Resuelve el actor autenticado a una vista unificada Usuario/Admin.
 *
 * El plan v2 propone que los roles POSGRADOS/DEPENDENCIA/ADMIN vivan en
 * `admins`, pero hay casos transicionales (ej. Coordinador Posgrados con
 * código 20261003) que aún viven en `usuario`. Por eso la búsqueda intenta
 * ambas fuentes con orden según el rol — admin primero si es rol
 * administrativo, fallback a usuario si no se encontró ahí.
 */
@Service
public class PrincipalResolver {

    private static final Set<String> ROLES_ADMIN = Set.of("POSGRADOS", "DEPENDENCIA", "ADMIN");

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AdminRepository adminRepository;

    public ResolvedPrincipal resolve(Authentication auth) {
        if (auth == null || auth.getName() == null) return null;
        String name = auth.getName();
        String rol = extractRol(auth);

        if (ROLES_ADMIN.contains(rol)) {
            // 1. Intentar en admins (caso normal post-migración)
            Admin admin = adminRepository.findByCodigo(name).orElse(null);
            if (admin != null) return ResolvedPrincipal.ofAdmin(admin);

            // 2. Fallback transicional: el actor con rol administrativo
            //    aún puede vivir en `usuario` (ej. Coordinador Posgrados
            //    código 20261003). El JwtAuthFilter pone `cedula` como
            //    principalName para tokens con principalType=USUARIO.
            Usuario u = usuarioRepository.findByCedula(name).orElse(null);
            return u != null ? ResolvedPrincipal.ofUsuario(u) : null;
        }

        // Roles académicos (ESTUDIANTE, DIRECTOR) viven siempre en `usuario`.
        Usuario u = usuarioRepository.findByCedula(name).orElse(null);
        return u != null ? ResolvedPrincipal.ofUsuario(u) : null;
    }

    private String extractRol(Authentication auth) {
        for (GrantedAuthority a : auth.getAuthorities()) {
            String x = a.getAuthority();
            if (x != null && x.startsWith("ROLE_")) return x.substring(5);
        }
        return null;
    }
}
