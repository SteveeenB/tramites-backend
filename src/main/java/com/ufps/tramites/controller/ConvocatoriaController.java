package com.ufps.tramites.controller;

import com.ufps.tramites.model.Convocatoria;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.service.ConvocatoriaService;
import com.ufps.tramites.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/convocatorias")
public class ConvocatoriaController {

    @Autowired
    private ConvocatoriaService convocatoriaService;

    @Autowired
    private UsuarioService usuarioService;

    /** GET /api/convocatorias/activa — cualquier usuario autenticado */
    @GetMapping("/activa")
    public ResponseEntity<?> getActiva() {
        Convocatoria c = convocatoriaService.getActiva();
        return ResponseEntity.ok(mapear(c));
    }

    /** PUT /api/convocatorias — solo ADMIN autenticado */
    @PutMapping
    public ResponseEntity<?> actualizar(
            @RequestBody Map<String, String> body,
            HttpSession session) {

        String cedula = (String) session.getAttribute("usuarioCedula");
        if (cedula == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No autenticado"));
        }

        Usuario usuario = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (usuario == null || !"ADMIN".equals(usuario.getRol())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Acción no permitida"));
        }

        String inicioStr = body.get("fechaInicio");
        String finStr    = body.get("fechaFin");

        if (inicioStr == null || finStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "fechaInicio y fechaFin son requeridas"));
        }

        try {
            LocalDate inicio = LocalDate.parse(inicioStr);
            LocalDate fin    = LocalDate.parse(finStr);
            return ResponseEntity.ok(mapear(convocatoriaService.actualizar(inicio, fin)));
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
