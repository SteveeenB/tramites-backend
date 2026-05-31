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

import com.ufps.tramites.model.Admin;
import com.ufps.tramites.model.Dependencia;
import com.ufps.tramites.repository.AdminRepository;
import com.ufps.tramites.service.AdminService;
import com.ufps.tramites.service.DependenciaService;

@Tag(name = "Admin – Dependencias",
     description = "Gestión del catálogo de dependencias y consulta de admins con tipo DEPENDENCIA.")
@RestController
@RequestMapping("/api/dependencias")
public class DependenciaController {

    @Autowired private AdminRepository adminRepository;
    @Autowired private AdminService adminService;
    @Autowired private DependenciaService dependenciaService;

    /** Devuelve admins tipo DEPENDENCIA para poblar dropdowns. */
    @Operation(summary = "Listar admins de dependencias",
               description = "Devuelve código, nombre, correo y dependencia asignada de admins con tipo DEPENDENCIA.")
    @ApiResponse(responseCode = "200", description = "Lista de admins DEPENDENCIA")
    @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar() {
        List<Admin> admins = adminRepository.findByTipo("DEPENDENCIA");
        List<Map<String, Object>> data = admins.stream().map(this::toAdminMap).collect(Collectors.toList());
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

    /** Listar responsables (admins tipo DEPENDENCIA) para gestión en admin. */
    @GetMapping("/usuarios-dependencia")
    @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")
    public ResponseEntity<List<Map<String, Object>>> listarUsuariosDependencia() {
        List<Admin> admins = adminRepository.findByTipo("DEPENDENCIA");
        List<Map<String, Object>> data = admins.stream().map(this::toAdminMap).collect(Collectors.toList());
        return ResponseEntity.ok(data);
    }

    /** Crear un nuevo responsable admin tipo DEPENDENCIA. */
    @PostMapping("/usuarios-dependencia")
    @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")
    public ResponseEntity<?> crearUsuarioDependencia(@RequestBody Map<String, Object> body) {
        String nombreCompleto = (String) body.get("nombreCompleto");
        String codigo         = (String) body.get("codigo");
        String correo         = (String) body.get("correo");
        String contrasena     = (String) body.get("contrasena");
        Long dependenciaId    = body.get("dependenciaId") != null
                ? Long.valueOf(body.get("dependenciaId").toString()) : null;
        Admin creado = adminService.crearAdminDependencia(
                nombreCompleto, codigo, correo, contrasena, dependenciaId);
        return ResponseEntity.ok(toAdminMap(creado));
    }

    /** Eliminar un responsable por código (path param). */
    @DeleteMapping("/usuarios-dependencia/{codigo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")
    public ResponseEntity<?> eliminarUsuarioDependencia(@PathVariable String codigo) {
        adminService.eliminarPorCodigo(codigo);
        return ResponseEntity.ok(Map.of("message", "Responsable eliminado"));
    }

    private Map<String, Object> toAdminMap(Admin a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                a.getId());
        m.put("codigo",            a.getCodigo());
        m.put("nombreCompleto",    a.getNombreCompleto());
        m.put("correo",            a.getEmail());
        m.put("dependenciaId",     a.getDependenciaId());
        m.put("dependenciaNombre", a.getDependenciaNombre());
        return m;
    }
}
