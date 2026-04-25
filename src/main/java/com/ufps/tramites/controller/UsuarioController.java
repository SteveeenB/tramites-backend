package com.ufps.tramites.controller;

import com.ufps.tramites.dto.LoginRequestDTO;
import com.ufps.tramites.dto.UsuarioResponseDTO;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest, HttpSession session) {
        Usuario usuario = usuarioService.autenticar(loginRequest.getCodigo(), loginRequest.getContrasena());
        if (usuario == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Credenciales inválidas"));
        }
        session.setAttribute("usuarioCedula", usuario.getCedula());
        return ResponseEntity.ok(toDTO(usuario));
    }

    /** Establece sesión por cédula sin validar contraseña — solo para modo demo/desarrollo. */
    @PostMapping("/login-demo")
    public ResponseEntity<?> loginDemo(@RequestParam String cedula, HttpSession session) {
        Usuario usuario = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
        }
        session.setAttribute("usuarioCedula", usuario.getCedula());
        return ResponseEntity.ok(toDTO(usuario));
    }

    @GetMapping("/me")
    public ResponseEntity<?> obtenerUsuarioActual(HttpSession session) {
        String cedula = (String) session.getAttribute("usuarioCedula");
        if (cedula == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
        }
        Usuario usuario = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
        }
        return ResponseEntity.ok(toDTO(usuario));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada"));
    }

    private UsuarioResponseDTO toDTO(Usuario u) {
        return new UsuarioResponseDTO(u.getCedula(), u.getNombre(), u.getCodigo(), u.getRol());
    }
}
