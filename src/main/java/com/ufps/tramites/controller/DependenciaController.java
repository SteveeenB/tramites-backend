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
import org.springframework.web.bind.annotation.*;

import com.ufps.tramites.model.Dependencia;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.UsuarioRepository;
import com.ufps.tramites.service.DependenciaService;

@Tag(name = "Admin – Dependencias",
     description = "Gestión del catálogo de dependencias y consulta de usuarios con rol DEPENDENCIA.")
@RestController
@RequestMapping("/api/dependencias")
public class DependenciaController {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private DependenciaService dependenciaService;

    /** Devuelve usuarios con rol DEPENDENCIA (o POSGRADOS) para poblar dropdowns de tipos de certificado. */
    @Operation(summary = "Listar usuarios de dependencias",
               description = "Devuelve cédula, nombre, correo y dependencia asignada de usuarios con rol DEPENDENCIA.")
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
            if (u.getDependencia() != null) {
                m.put("dependenciaId", u.getDependencia().getId());
                m.put("dependenciaNombre", u.getDependencia().getNombre());
            }
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(data);
    }

    /** Catálogo de entidades Dependencia configurables por el admin. */
    @GetMapping("/catalogo")
    public ResponseEntity<List<Dependencia>> listarCatalogo() {
        return ResponseEntity.ok(dependenciaService.obtenerTodas());
    }

    @PostMapping("/catalogo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Dependencia> crear(@RequestBody Dependencia dependencia) {
        return ResponseEntity.ok(dependenciaService.crear(dependencia));
    }

    @DeleteMapping("/catalogo/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> desactivar(@PathVariable Long id) {
        dependenciaService.desactivar(id);
        return ResponseEntity.ok(Map.of("message", "Dependencia desactivada"));
    }
}
