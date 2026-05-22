package com.ufps.tramites.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.ufps.tramites.dto.LoginRequestDTO;
import com.ufps.tramites.dto.LoginResponseDTO;
import com.ufps.tramites.model.Rol;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.RolRepository;
import com.ufps.tramites.repository.UsuarioRepository;
import com.ufps.tramites.security.JwtService;
import java.util.Collections;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Value("${google.client-id:}")
    private String googleClientId;

    @Value("${demo.auth.enabled:false}")
    private boolean demoAuthEnabled;

    /**
     * POST /api/auth/login
     * Login con código + contraseña (tipeo manual).
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        if (request.getCodigo() == null || request.getContrasena() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "codigo y contrasena son requeridos"));
        }

        Usuario usuario = usuarioRepository.findByCodigo(request.getCodigo()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        // Soporta contraseñas en texto plano (data.sql demo) y hasheadas con BCrypt
        boolean passwordValida = usuario.getPassword() != null && (
                passwordEncoder.matches(request.getContrasena(), usuario.getPassword())
                || request.getContrasena().equals(usuario.getPassword())
        );

        if (!passwordValida) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));
        }

        return ResponseEntity.ok(buildResponse(usuario));
    }

    /**
     * POST /api/auth/google
     * Login con id_token de Google OAuth.
     * Busca al usuario por googleId o email; si no existe, lo crea con rol ESTUDIANTE.
     */
    @PostMapping("/google")
    public ResponseEntity<?> loginGoogle(@RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "idToken es requerido"));
        }
        if (googleClientId == null || googleClientId.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Google OAuth no está configurado en este servidor"));
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token de Google inválido o expirado"));
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String googleId = payload.getSubject();
            String email    = payload.getEmail();
            String nombre   = (String) payload.get("name");
            String foto     = (String) payload.get("picture");

            // Buscar usuario existente por googleId o email
            Usuario usuario = usuarioRepository.findByGoogleId(googleId)
                    .or(() -> usuarioRepository.findByEmail(email))
                    .orElse(null);

            if (usuario == null) {
                // Crear usuario nuevo con rol ESTUDIANTE por defecto
                Rol rolEstudiante = rolRepository.findByNombre("ESTUDIANTE")
                        .orElseThrow(() -> new IllegalStateException("Rol ESTUDIANTE no encontrado"));
                usuario = new Usuario();
                usuario.setGoogleId(googleId);
                usuario.setEmail(email);
                usuario.setNombreCompleto(nombre);
                usuario.setFotoUrl(foto);
                usuario.setRol(rolEstudiante);
                usuario = usuarioRepository.save(usuario);
            } else if (usuario.getGoogleId() == null) {
                // Vincular googleId si ya existía por email
                usuario.setGoogleId(googleId);
                if (foto != null) usuario.setFotoUrl(foto);
                usuario = usuarioRepository.save(usuario);
            }

            return ResponseEntity.ok(buildResponse(usuario));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al verificar token de Google: " + e.getMessage()));
        }
    }

    /**
     * POST /api/auth/login-demo?cedula=...
     * Solo disponible cuando demo.auth.enabled=true (perfil dev).
     * Emite un JWT real para el usuario con la cédula indicada sin validar contraseña.
     */
    @PostMapping("/login-demo")
    public ResponseEntity<?> loginDemo(@RequestParam String cedula) {
        if (!demoAuthEnabled) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Endpoint no disponible en este entorno"));
        }

        Usuario usuario = usuarioRepository.findByCedula(cedula).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado: " + cedula));
        }

        return ResponseEntity.ok(buildResponse(usuario));
    }

    private LoginResponseDTO buildResponse(Usuario usuario) {
        String token = jwtService.generateToken(usuario);
        return new LoginResponseDTO(
                token,
                usuario.getId(),
                usuario.getCedula(),
                usuario.getNombreCompleto(),
                usuario.getEmail(),
                usuario.getCodigo(),
                usuario.getRolNombre()
        );
    }
}
