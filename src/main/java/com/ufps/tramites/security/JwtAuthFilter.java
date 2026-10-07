package com.ufps.tramites.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            // FIX TP-186 (Santiago Cepeda, 07/10/2026): aceptar el token por
            // query param "token" en los endpoints SSE porque EventSource no
            // puede enviar el header Authorization. Se restringe a las rutas
            // SSE para no habilitar un vector de token-leak genérico (los
            // query params pueden quedar en logs de proxy).
            String path = request.getRequestURI();
            if (path != null
                    && (path.endsWith("/api/notificaciones/subscribe")
                        || path.endsWith("/api/notificaciones/stream"))) {
                token = request.getParameter("token");
            }
        }

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String principalType = jwtService.extractPrincipalType(token);
        String rol           = jwtService.extractRol(token);

        // El nombre del principal viaja distinto según el tipo:
        //   USUARIO → cédula (los controllers académicos resuelven Usuario por cédula)
        //   ADMIN   → código (los controllers refactorizados a admin resolverán por código)
        // En Bloque 0 la tabla `admins` está vacía, así que en la práctica siempre es cédula.
        String principalName = JwtService.PRINCIPAL_ADMIN.equals(principalType)
                ? jwtService.extractCodigo(token)
                : jwtService.extractCedula(token);

        if (principalName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var auth = new UsernamePasswordAuthenticationToken(
                    principalName,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + rol))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
