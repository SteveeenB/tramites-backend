package com.ufps.tramites.security;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
    } else if (request.getRequestURI().contains("/mis/stream")) {
        // SSE no soporta headers — lee token del query param
        token = request.getParameter("token");
    }

    if (token == null || !jwtService.isTokenValid(token)) {
        filterChain.doFilter(request, response);
        return;
    }

    String cedula = jwtService.extractCedula(token);
    String rol    = jwtService.extractRol(token);

    if (cedula != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        var auth = new UsernamePasswordAuthenticationToken(
                cedula,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    filterChain.doFilter(request, response);
}
}
