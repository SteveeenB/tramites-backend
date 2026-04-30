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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tramites")
public class TramiteController {

    @Autowired private TramiteService tramiteService;
    @Autowired private UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<?> obtenerModulo(
            @RequestParam(required = false) String cedula,
            @RequestParam(required = false) String codigo) {
        Usuario u = resolver(cedula, codigo);
        if (u == null) return notFound("Usuario no encontrado");
        return ResponseEntity.ok(tramiteService.construirModuloPorRol(u));
    }

    @GetMapping("/proceso-grado")
    public ResponseEntity<?> obtenerProcesoGrado(
            @RequestParam(required = false) String cedula,
            @RequestParam(required = false) String codigo) {
        Usuario u = resolver(cedula, codigo);
        if (u == null) return notFound("Usuario no encontrado");
        return ResponseEntity.ok(tramiteService.construirProcesoDeGrado(u));
    }

    /**
     * GET /api/tramites/director/estudiantes?cedula=...
     * Lista de todos los estudiantes del programa del director con su estado
     * en el proceso de grado.
     * Solo accesible por el rol DIRECTOR.
     */
    @GetMapping("/director/estudiantes")
    public ResponseEntity<?> obtenerEstudiantesDirector(@RequestParam String cedula) {
        Usuario director = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (director == null) return notFound("Director no encontrado");
        if (!"DIRECTOR".equals(director.getRol())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error("Acceso restringido a directores de programa"));
        }
        return ResponseEntity.ok(tramiteService.construirListaEstudiantesDirector(director));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Usuario resolver(String cedula, String codigo) {
        if (cedula != null && !cedula.isBlank())
            return usuarioService.obtenerUsuarioPorCedula(cedula);
        if (codigo != null && !codigo.isBlank())
            return usuarioService.obtenerUsuarioPorCodigo(codigo);
        return usuarioService.obtenerPrimerUsuario();
    }

    private ResponseEntity<?> notFound(String msg) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(msg));
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", msg);
        return m;
    }
}
