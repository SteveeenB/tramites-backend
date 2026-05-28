package com.ufps.tramites.controller;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.service.TramiteService;
import com.ufps.tramites.service.UsuarioService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tramites")
public class TramiteController {

    @Autowired private TramiteService tramiteService;
    @Autowired private UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<?> obtenerModuloTramites(Authentication auth) {
        Usuario usuario = resolverUsuario(auth);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        return ResponseEntity.ok(tramiteService.construirModuloPorRol(usuario));
    }

    @GetMapping("/proceso-grado")
    public ResponseEntity<?> obtenerProcesoDeGrado(Authentication auth) {
        Usuario usuario = resolverUsuario(auth);
        if (usuario == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        return ResponseEntity.ok(tramiteService.construirProcesoDeGrado(usuario));
    }

    private Usuario resolverUsuario(Authentication auth) {
        if (auth == null) return null;
        return usuarioService.obtenerUsuarioPorCedula(auth.getName());
    }

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", mensaje);
        return error;
    }
}
