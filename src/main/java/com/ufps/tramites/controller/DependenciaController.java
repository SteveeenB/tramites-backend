package com.ufps.tramites.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.UsuarioRepository;

/**
 * Lista las dependencias existentes (usuarios con rol DEPENDENCIA) para
 * poblar dropdowns del panel administrador.
 */
@Tag(name = "Admin – Dependencias",
     description = "Consulta de usuarios con rol DEPENDENCIA. Usado por el panel admin para poblar selectores al crear tipos de certificado.")
@RestController
@RequestMapping("/api/dependencias")
public class DependenciaController {

    @Autowired private UsuarioRepository usuarioRepository;

    @Operation(summary = "Listar dependencias disponibles",
               description = "Devuelve cédula, nombre y correo de todos los usuarios con rol DEPENDENCIA.")
    @ApiResponse(responseCode = "200", description = "Lista de dependencias")
    @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar() {
        List<Usuario> dependencias = usuarioRepository.findByRol_Nombre("DEPENDENCIA");
        List<Map<String, Object>> data = dependencias.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("cedula", u.getCedula());
            m.put("nombre", u.getNombre());
            m.put("correo", u.getCorreo());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(data);
    }
}
