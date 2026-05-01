package com.ufps.tramites.controller;

import com.ufps.tramites.dto.LoginRequestDTO;
import com.ufps.tramites.dto.UsuarioResponseDTO;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author StevenB
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;
/* 
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest, HttpSession session) {
        Usuario usuario = usuarioService.autenticar(loginRequest.getCodigo(), loginRequest.getContrasena());
        
        if (usuario != null) {
            // Guardar la cedula del usuario en la sesion
            session.setAttribute("usuarioCedula", usuario.getCedula());
            
            // Retornar informacion del usuario sin la contraseña
            UsuarioResponseDTO response = new UsuarioResponseDTO(
                usuario.getCedula(),
                usuario.getNombre(),
                usuario.getCodigo(),
                usuario.getRol()
            );
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).body("Credenciales inválidas");
    }
*/
    @PostMapping("/login-demo")
    public ResponseEntity<?> loginDemo(@RequestParam String cedula, HttpSession session) {
        Usuario usuario = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado: " + cedula));
        }
        session.setAttribute("usuarioCedula", cedula);
        return ResponseEntity.ok(new UsuarioResponseDTO(
            usuario.getCedula(),
            usuario.getNombre(),
            usuario.getCodigo(),
            usuario.getRol()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> obtenerUsuarioActual(HttpSession session) {
        String usuarioCedula = (String) session.getAttribute("usuarioCedula");
        
        if (usuarioCedula == null) {
            return ResponseEntity.status(401).body("No autenticado");
        }
        
        Usuario usuario = usuarioService.obtenerUsuarioPorCedula(usuarioCedula);
        
        if (usuario != null) {
            UsuarioResponseDTO response = new UsuarioResponseDTO(
                usuario.getCedula(),
                usuario.getNombre(),
                usuario.getCodigo(),
                usuario.getRol()
            );
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404).body("Usuario no encontrado");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Sesión cerrada");
    }
}
