package com.ufps.tramites.controller;

import com.ufps.tramites.dto.UsuarioResponseDTO;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.service.UsuarioService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/me")
    public ResponseEntity<?> obtenerUsuarioActual(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
        }
        String cedula = auth.getName();
        Usuario usuario = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
        }
        return ResponseEntity.ok(new UsuarioResponseDTO(
                usuario.getCedula(),
                usuario.getNombreCompleto(),
                usuario.getCodigo(),
                usuario.getRolNombre(),
                usuario.getDependenciaNombre()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Stateless JWT: el cliente descarta el token
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada"));
    }
}
