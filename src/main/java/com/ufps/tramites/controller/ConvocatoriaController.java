package com.ufps.tramites.controller;

import com.ufps.tramites.model.Convocatoria;
import com.ufps.tramites.service.ConvocatoriaService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/convocatorias")
public class ConvocatoriaController {

    @Autowired private ConvocatoriaService convocatoriaService;

    /** GET /api/convocatorias/activa — cualquier usuario autenticado */
    @GetMapping("/activa")
    public ResponseEntity<?> getActiva() {
        Convocatoria c = convocatoriaService.getActiva();
        return ResponseEntity.ok(mapear(c));
    }

    /** PUT /api/convocatorias — solo ADMIN o POSGRADOS */
    @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")
    @PutMapping
    public ResponseEntity<?> actualizar(Authentication auth, @RequestBody Map<String, String> body) {
        if (auth == null) return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));

        String inicioStr = body.get("fechaInicio");
        String finStr    = body.get("fechaFin");
        if (inicioStr == null || finStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "fechaInicio y fechaFin son requeridas"));
        }
        try {
            Convocatoria actualizada = convocatoriaService.actualizar(LocalDate.parse(inicioStr), LocalDate.parse(finStr));
            return ResponseEntity.ok(mapear(actualizada));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Formato de fecha inválido (use YYYY-MM-DD)"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> mapear(Convocatoria c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("fechaInicio", c.getFechaInicio().toString());
        map.put("fechaFin", c.getFechaFin().toString());
        return map;
    }
}
