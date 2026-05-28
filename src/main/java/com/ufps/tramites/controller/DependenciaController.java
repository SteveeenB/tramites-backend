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
import com.ufps.tramites.service.UsuarioService;

@Tag(name = "Admin – Dependencias",
     description = "Gestión del catálogo de dependencias y consulta de usuarios con rol DEPENDENCIA.")
@RestController
@RequestMapping("/api/dependencias")
public class DependenciaController {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private DependenciaService dependenciaService;
    @Autowired private UsuarioService usuarioService;

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

    /** Catálogo de entidades Dependencia configurables por el admin (activas e inactivas). */
    @GetMapping("/catalogo")
    @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")
    public ResponseEntity<List<Dependencia>> listarCatalogo() {
        return ResponseEntity.ok(dependenciaService.obtenerTodasParaAdmin());
    }

    @PostMapping("/catalogo")
    @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")
    public ResponseEntity<Dependencia> crear(@RequestBody Dependencia dependencia) {
        return ResponseEntity.ok(dependenciaService.crear(dependencia));
    }

    @PutMapping("/catalogo/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")
    public ResponseEntity<Dependencia> actualizar(@PathVariable Long id,
                                                   @RequestBody Dependencia dependencia) {
        return ResponseEntity.ok(dependenciaService.actualizar(id, dependencia));
    }

    @PatchMapping("/catalogo/{id}/activo")
    @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")
    public ResponseEntity<?> toggleActivo(@PathVariable Long id,
                                           @RequestParam boolean valor) {
        Dependencia dep = dependenciaService.setActiva(id, valor);
        return ResponseEntity.ok(Map.of("id", dep.getId(), "activa", dep.getActiva()));
    }

    @DeleteMapping("/catalogo/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")
    public ResponseEntity<?> desactivar(@PathVariable Long id) {
        dependenciaService.desactivar(id);
        return ResponseEntity.ok(Map.of("message", "Dependencia desactivada"));
    }

    /** Listar responsables (usuarios con rol DEPENDENCIA) para gestión en admin. */
    @GetMapping("/usuarios-dependencia")
    @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")
    public ResponseEntity<List<Map<String, Object>>> listarUsuariosDependencia() {
        List<Usuario> usuarios = usuarioRepository.findByRol_Nombre("DEPENDENCIA");
        List<Map<String, Object>> data = usuarios.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("cedula",            u.getCedula());
            m.put("codigo",            u.getCodigo());
            m.put("nombreCompleto",    u.getNombreCompleto());
            m.put("correo",            u.getCorreo());
            m.put("dependenciaId",     u.getDependencia() != null ? u.getDependencia().getId()     : null);
            m.put("dependenciaNombre", u.getDependencia() != null ? u.getDependencia().getNombre() : null);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(data);
    }

    /** Crear un nuevo responsable con rol DEPENDENCIA. */
    @PostMapping("/usuarios-dependencia")
    @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")
    public ResponseEntity<?> crearUsuarioDependencia(@RequestBody Map<String, Object> body) {
        String nombreCompleto = (String) body.get("nombreCompleto");
        String cedula         = (String) body.get("cedula");
        String codigo         = (String) body.get("codigo");
        String correo         = (String) body.get("correo");
        String contrasena     = (String) body.get("contrasena");
        Long dependenciaId    = body.get("dependenciaId") != null
                ? Long.valueOf(body.get("dependenciaId").toString()) : null;
        Usuario creado = usuarioService.crearUsuarioDependencia(
                nombreCompleto, cedula, codigo, correo, contrasena, dependenciaId);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("cedula",         creado.getCedula());
        res.put("nombreCompleto", creado.getNombreCompleto());
        return ResponseEntity.ok(res);
    }

    /** Eliminar un responsable por cédula. */
    @DeleteMapping("/usuarios-dependencia/{cedula}")
    @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")
    public ResponseEntity<?> eliminarUsuarioDependencia(@PathVariable String cedula) {
        usuarioService.eliminarUsuarioDependencia(cedula);
        return ResponseEntity.ok(Map.of("message", "Responsable eliminado"));
    }
}
