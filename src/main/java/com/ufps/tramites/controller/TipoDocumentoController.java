package com.ufps.tramites.controller;

import com.ufps.tramites.model.TipoDocumentoRequerido;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.service.TipoDocumentoService;
import com.ufps.tramites.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/documentos-requeridos")
public class TipoDocumentoController {

    private final TipoDocumentoService tipoDocumentoService;
    private final UsuarioService usuarioService;

    public TipoDocumentoController(TipoDocumentoService tipoDocumentoService,
                                   UsuarioService usuarioService) {
        this.tipoDocumentoService = tipoDocumentoService;
        this.usuarioService = usuarioService;
    }

    /** GET /api/documentos-requeridos — accesible por cualquier rol autenticado */
    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(tipoDocumentoService.listarTodos());
    }

    /** POST /api/documentos-requeridos?cedula=... — solo ADMIN */
    @PostMapping
    public ResponseEntity<?> crear(@RequestParam String cedula,
                                   @RequestBody TipoDocumentoRequerido tipo) {
        if (!esAdmin(cedula)) return forbidden();
        return ResponseEntity.status(HttpStatus.CREATED).body(tipoDocumentoService.crear(tipo));
    }

    /** PUT /api/documentos-requeridos/{id}?cedula=... — solo ADMIN */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id,
                                        @RequestParam String cedula,
                                        @RequestBody TipoDocumentoRequerido tipo) {
        if (!esAdmin(cedula)) return forbidden();
        try {
            return ResponseEntity.ok(tipoDocumentoService.actualizar(id, tipo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    /** DELETE /api/documentos-requeridos/{id}?cedula=... — solo ADMIN */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id, @RequestParam String cedula) {
        if (!esAdmin(cedula)) return forbidden();
        try {
            tipoDocumentoService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    private boolean esAdmin(String cedula) {
        Usuario u = usuarioService.obtenerUsuarioPorCedula(cedula);
        return u != null && "ADMIN".equals(u.getRol());
    }

    private ResponseEntity<Map<String, Object>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Acceso restringido a administradores"));
    }

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", mensaje);
        return map;
    }
}
