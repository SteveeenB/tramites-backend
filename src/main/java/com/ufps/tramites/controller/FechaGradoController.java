package com.ufps.tramites.controller;

import com.ufps.tramites.model.FechaGrado;
import com.ufps.tramites.repository.FechaGradoRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fechas de grado configurables por la oficina de Posgrados.
 *
 *  GET    /api/fechas-grado/disponibles  estudiante: activas y futuras
 *  GET    /api/fechas-grado              Posgrados/Admin: todas (incluye inactivas y vencidas)
 *  POST   /api/fechas-grado              crear
 *  PATCH  /api/fechas-grado/{id}         editar / activar / desactivar
 *  DELETE /api/fechas-grado/{id}         eliminar
 */
@RestController
@RequestMapping("/api/fechas-grado")
public class FechaGradoController {

    private static final Set<String> MODALIDADES = Set.of("CEREMONIA", "SECRETARIA");

    @Autowired private FechaGradoRepository repository;

    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'DIRECTOR', 'POSGRADOS', 'ADMIN')")
    @GetMapping("/disponibles")
    public ResponseEntity<List<Map<String, Object>>> disponibles() {
        return ResponseEntity.ok(repository
                .findByActivaTrueAndFechaGreaterThanEqualOrderByFechaAscHoraAsc(LocalDate.now())
                .stream().map(this::toMap).toList());
    }

    @PreAuthorize("hasAnyRole('POSGRADOS', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar() {
        return ResponseEntity.ok(repository.findAll().stream()
                .sorted((a, b) -> b.getFecha().compareTo(a.getFecha()))
                .map(this::toMap).toList());
    }

    @PreAuthorize("hasAnyRole('POSGRADOS', 'ADMIN')")
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        FechaGrado f = new FechaGrado();
        String error = aplicar(f, body, true);
        if (error != null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", error));
        if (repository.existsByFechaAndModalidadAndHora(f.getFecha(), f.getModalidad(), f.getHora()))
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Ya existe esa fecha con la misma modalidad y hora"));
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(repository.save(f)));
    }

    @PreAuthorize("hasAnyRole('POSGRADOS', 'ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repository.findById(id).<ResponseEntity<?>>map(f -> {
            String error = aplicar(f, body, false);
            if (error != null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", error));
            if (repository.existsByFechaAndModalidadAndHoraAndIdNot(f.getFecha(), f.getModalidad(), f.getHora(), f.getId()))
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Ya existe esa fecha con la misma modalidad y hora"));
            return ResponseEntity.ok(toMap(repository.save(f)));
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Fecha de grado no encontrada")));
    }

    @PreAuthorize("hasAnyRole('POSGRADOS', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (!repository.existsById(id))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Fecha de grado no encontrada"));
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Copia al entidad los campos presentes en el body y valida. Devuelve el
     * mensaje de error, o null si todo está bien. Una fecha pasada solo se
     * rechaza si se está creando o si se está cambiando la fecha (así se puede
     * seguir desactivando o corrigiendo el lugar de una fecha ya vencida).
     */
    private String aplicar(FechaGrado f, Map<String, Object> body, boolean creando) {
        try {
            if (creando || body.containsKey("fecha")) {
                Object v = body.get("fecha");
                if (v == null || v.toString().isBlank()) return "La fecha es obligatoria";
                LocalDate nueva = LocalDate.parse(v.toString().trim());
                if ((creando || !nueva.equals(f.getFecha())) && nueva.isBefore(LocalDate.now()))
                    return "La fecha no puede estar en el pasado";
                f.setFecha(nueva);
            }
        } catch (DateTimeParseException e) {
            return "Formato de fecha inválido. Use YYYY-MM-DD";
        }
        if (creando || body.containsKey("modalidad")) {
            String m = texto(body.get("modalidad"));
            if (!MODALIDADES.contains(m)) return "Modalidad inválida. Use CEREMONIA o SECRETARIA";
            f.setModalidad(m);
        }
        if (creando || body.containsKey("hora")) {
            String h = texto(body.get("hora"));
            if (h.isEmpty()) return "La hora es obligatoria";
            f.setHora(h);
        }
        if (creando || body.containsKey("lugar")) {
            String l = texto(body.get("lugar"));
            if (l.isEmpty()) return "El lugar es obligatorio";
            f.setLugar(l);
        }
        if (body.containsKey("activa")) {
            f.setActiva(Boolean.parseBoolean(String.valueOf(body.get("activa"))));
        } else if (creando) {
            f.setActiva(true);
        }
        return null;
    }

    private static String texto(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private Map<String, Object> toMap(FechaGrado f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", f.getId());
        m.put("fecha", f.getFecha().toString());
        m.put("modalidad", f.getModalidad());
        m.put("hora", f.getHora());
        m.put("lugar", f.getLugar());
        m.put("activa", Boolean.TRUE.equals(f.getActiva()));
        return m;
    }
}
