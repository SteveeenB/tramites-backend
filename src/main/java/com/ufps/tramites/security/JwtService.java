package com.ufps.tramites.security;

import com.ufps.tramites.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public static final String CLAIM_ROL    = "rol";
    public static final String CLAIM_NOMBRE = "nombreCompleto";
    public static final String CLAIM_CEDULA = "cedula";
    public static final String CLAIM_CODIGO = "codigo";
    public static final String CLAIM_EMAIL  = "email";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    public String generateToken(Usuario usuario) {
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim(CLAIM_CEDULA,  usuario.getCedula())
                .claim(CLAIM_ROL,     usuario.getRolNombre())
                .claim(CLAIM_NOMBRE,  usuario.getNombreCompleto())
                .claim(CLAIM_CODIGO,  usuario.getCodigo())
                .claim(CLAIM_EMAIL,   usuario.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getKey())
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractCedula(String token) {
        return (String) extractAllClaims(token).get(CLAIM_CEDULA);
    }

    public String extractRol(String token) {
        return (String) extractAllClaims(token).get(CLAIM_ROL);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
