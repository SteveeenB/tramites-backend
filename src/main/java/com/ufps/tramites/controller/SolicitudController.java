package com.ufps.tramites.controller;

import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.service.SolicitudService;
import com.ufps.tramites.service.UsuarioService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitudes")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class SolicitudController {

    @Autowired
    private SolicitudService solicitudService;

    @Autowired
    private UsuarioService usuarioService;

    /**
     * POST /api/solicitudes/terminacion-materias?cedula=...
     * Crea una solicitud de terminación de materias para el estudiante.
     */
    @PostMapping("/terminacion-materias")
    public ResponseEntity<?> crearSolicitudTerminacion(
            @RequestParam String cedula) {

        Usuario estudiante = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (estudiante == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Estudiante no encontrado"));
        }

        try {
            Map<String, Object> resultado = solicitudService.crearSolicitudTerminacion(estudiante);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error(e.getMessage()));
        }
    }

    /**
     * GET /api/solicitudes?cedula=...
     * Retorna todas las solicitudes del estudiante.
     */
    @GetMapping
    public ResponseEntity<?> obtenerSolicitudes(@RequestParam String cedula) {
        Usuario estudiante = usuarioService.obtenerUsuarioPorCedula(cedula);
        if (estudiante == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Estudiante no encontrado"));
        }
        return ResponseEntity.ok(solicitudService.obtenerSolicitudesPorCedula(cedula));
    }

    /**
     * PUT /api/solicitudes/{id}/estado?estado=APROBADA&observaciones=...
     * Permite al director o admin cambiar el estado de una solicitud.
     * Dispara el evento de dominio que notifica al estudiante por correo y SSE.
     */
    @PutMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(
            @PathVariable Long id,
            @RequestParam String estado,
            @RequestParam(required = false) String observaciones) {

        if (!"EN_REVISION".equals(estado) && !"APROBADA".equals(estado) && !"RECHAZADA".equals(estado)) {
            return ResponseEntity.badRequest().body(error("Estado inválido: " + estado
                    + ". Valores permitidos: EN_REVISION, APROBADA, RECHAZADA"));
        }
        if ("RECHAZADA".equals(estado) && (observaciones == null || observaciones.isBlank())) {
            return ResponseEntity.badRequest().body(error("Se requiere motivo de rechazo en 'observaciones'"));
        }

        try {
            Map<String, Object> resultado = solicitudService.actualizarEstado(id, estado, observaciones);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    private Map<String, Object> error(String mensaje) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", mensaje);
        return map;
    }
}
