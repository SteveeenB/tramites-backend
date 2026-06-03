package com.ufps.tramites.controller;

import com.ufps.tramites.security.PrincipalResolver;
import com.ufps.tramites.security.ResolvedPrincipal;
import com.ufps.tramites.service.TramiteService;
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
    @Autowired private PrincipalResolver principalResolver;

    @GetMapping
    public ResponseEntity<?> obtenerModuloTramites(Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        if (p.isAdmin()) {
            // Admins no requieren el shape de Usuario; devolvemos un payload mínimo.
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("modulo", "TRAMITES");
            resp.put("rol", p.rol());
            resp.put("nombreCompleto", p.nombreCompleto());
            return ResponseEntity.ok(resp);
        }
        return ResponseEntity.ok(tramiteService.construirModuloPorRol(p.usuario()));
    }

    @GetMapping("/proceso-grado")
    public ResponseEntity<?> obtenerProcesoDeGrado(Authentication auth) {
        ResolvedPrincipal p = principalResolver.resolve(auth);
        if (p == null || !p.isUsuario()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("No autenticado"));
        return ResponseEntity.ok(tramiteService.construirProcesoDeGrado(p.usuario()));
    }

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", mensaje);
        return error;
    }
}
