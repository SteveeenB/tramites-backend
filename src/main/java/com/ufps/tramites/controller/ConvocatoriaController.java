package com.ufps.tramites.controller;

import com.ufps.tramites.model.Convocatoria;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.service.ConvocatoriaService;
import com.ufps.tramites.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin – Convocatoria",
     description = "Gestión de la ventana de convocatoria académica (registro único). Lectura pública; escritura solo ADMIN o POSGRADOS.")
@RestController
@RequestMapping("/api/convocatorias")
public class ConvocatoriaController {

    @Autowired
    private ConvocatoriaService convocatoriaService;

    @Autowired
    private UsuarioService usuarioService;

    @Operation(summary = "Obtener la convocatoria activa",
               description = "Devuelve las fechas de inicio y fin de la ventana vigente. Disponible para todos los roles.")
    @ApiResponse(responseCode = "200", description = "Convocatoria vigente con fechaInicio y fechaFin (formato YYYY-MM-DD)")
    @GetMapping("/activa")
    public ResponseEntity<?> getActiva() {
        Convocatoria c = convocatoriaService.getActiva();
        return ResponseEntity.ok(mapear(c));
    }

    @Operation(summary = "Actualizar las fechas de convocatoria",
               description = "Reemplaza las fechas de la convocatoria activa. Solo permitido para rol ADMIN o POSGRADOS. Body: `{ fechaInicio: 'YYYY-MM-DD', fechaFin: 'YYYY-MM-DD' }`.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Convocatoria actualizada"),
        @ApiResponse(responseCode = "400", description = "Fechas ausentes o con formato inválido"),
        @ApiResponse(responseCode = "403", description = "El usuario no tiene rol ADMIN o POSGRADOS")
    })
    @PutMapping
    public ResponseEntity<?> actualizar(
            @Parameter(description = "Cédula del usuario ADMIN o POSGRADOS que realiza el cambio") @RequestParam String cedula,
            @RequestBody Map<String, String> body) {

        Usuario usuario = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (usuario == null || !"ADMIN".equals(usuario.getRol())) {
            return ResponseEntity.status(403).body(Map.of("error", "Acción no permitida"));
        }

        String inicioStr = body.get("fechaInicio");
        String finStr    = body.get("fechaFin");

        if (inicioStr == null || finStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "fechaInicio y fechaFin son requeridas"));
        }

        try {
            LocalDate inicio = LocalDate.parse(inicioStr);
            LocalDate fin    = LocalDate.parse(finStr);
            Convocatoria actualizada = convocatoriaService.actualizar(inicio, fin);
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
