package com.ufps.tramites.controller;

import com.ufps.tramites.model.TipoSolicitud;
import com.ufps.tramites.repository.TipoSolicitudRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/tipos-solicitud")
public class AdminTipoSolicitudController {

    @Autowired
    private TipoSolicitudRepository repository;

    @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar() {
        List<Map<String, Object>> data = repository.findAll().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(data);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'POSGRADOS')")
    @PatchMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repository.findById(id)
                .<ResponseEntity<?>>map(tipo -> {
                    if (body.containsKey("costo")) {
                        Object val = body.get("costo");
                        if (val instanceof Number) {
                            tipo.setCosto(((Number) val).doubleValue());
                        }
                    }
                    if (body.containsKey("nombre")) {
                        Object val = body.get("nombre");
                        if (val instanceof String s && !s.isBlank()) {
                            tipo.setNombre(s);
                        }
                    }
                    repository.save(tipo);
                    return ResponseEntity.ok(toMap(tipo));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Tipo de solicitud no encontrado")));
    }

    private Map<String, Object> toMap(TipoSolicitud t) {
        return Map.of(
                "id",     t.getId(),
                "codigo", t.getCodigo() != null ? t.getCodigo() : "",
                "nombre", t.getNombre() != null ? t.getNombre() : "",
                "costo",  t.getCosto()  != null ? t.getCosto()  : 0.0,
                "activo", t.getActivo() != null && t.getActivo()
        );
    }
}
