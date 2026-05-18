package com.ufps.tramites.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.UsuarioRepository;

/**
 * Lista las dependencias existentes (usuarios con rol DEPENDENCIA) para
 * poblar dropdowns del panel administrador.
 */
@RestController
@RequestMapping("/api/dependencias")
public class DependenciaController {

    @Autowired private UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar() {
        List<Usuario> dependencias = usuarioRepository.findByRol("DEPENDENCIA");
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
