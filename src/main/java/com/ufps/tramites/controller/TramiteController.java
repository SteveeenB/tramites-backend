package com.ufps.tramites.controller;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.service.TramiteService;
import com.ufps.tramites.service.UsuarioService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tramites")
public class TramiteController {

    @Autowired
    private TramiteService tramiteService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<?> obtenerModuloTramites(
            @RequestParam(required = false) String cedula,
            @RequestParam(required = false) String codigo) {
        Usuario usuario;

        if (cedula != null && !cedula.isBlank()) {
            usuario = usuarioService.obtenerUsuarioPorCedula(cedula);
        } else if (codigo != null && !codigo.isBlank()) {
            usuario = usuarioService.obtenerUsuarioPorCodigo(codigo);
        } else {
            usuario = usuarioService.obtenerPrimerUsuario();
        }

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(error("Usuario no encontrado"));
        }

        return ResponseEntity.ok(tramiteService.construirModuloPorRol(usuario));
    }

    @GetMapping("/proceso-grado")
    public ResponseEntity<?> obtenerProcesoDeGrado(
            @RequestParam(required = false) String cedula,
            @RequestParam(required = false) String codigo) {
        Usuario usuario;

        if (cedula != null && !cedula.isBlank()) {
            usuario = usuarioService.obtenerUsuarioPorCedula(cedula);
        } else if (codigo != null && !codigo.isBlank()) {
            usuario = usuarioService.obtenerUsuarioPorCodigo(codigo);
        } else {
            usuario = usuarioService.obtenerPrimerUsuario();
        }

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(error("Usuario no encontrado"));
        }

        return ResponseEntity.ok(tramiteService.construirProcesoDeGrado(usuario));
    }

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", mensaje);
        return error;
    }
}
