package com.ufps.tramites.controller;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.service.TramiteService;
import com.ufps.tramites.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tramites")
public class TramiteController {

    @Autowired
    private TramiteService tramiteService;

    @Autowired
    private UsuarioService usuarioService;

    /** GET /api/tramites — módulo principal según el rol del usuario en sesión */
    @GetMapping
    public ResponseEntity<?> obtenerModuloTramites(HttpSession session) {
        Usuario usuario = usuarioDeSession(session);
        if (usuario == null) return noAutenticado();
        return ResponseEntity.ok(tramiteService.construirModuloPorRol(usuario));
    }

    /** GET /api/tramites/proceso-grado — estado del proceso de grado del estudiante en sesión */
    @GetMapping("/proceso-grado")
    public ResponseEntity<?> obtenerProcesoDeGrado(HttpSession session) {
        Usuario usuario = usuarioDeSession(session);
        if (usuario == null) return noAutenticado();
        return ResponseEntity.ok(tramiteService.construirProcesoDeGrado(usuario));
    }

    private Usuario usuarioDeSession(HttpSession session) {
        String cedula = (String) session.getAttribute("usuarioCedula");
        if (cedula == null) return null;
        return usuarioService.obtenerUsuarioPorCedula(cedula);
    }

    private ResponseEntity<?> noAutenticado() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
    }

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", mensaje);
        return map;
    }
}
