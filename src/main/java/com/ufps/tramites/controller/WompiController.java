package com.ufps.tramites.controller;

import com.ufps.tramites.service.WompiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
public class WompiController {

    @Autowired
    private WompiService wompiService;

    /**
     * POST /api/pagos/crear
     * Crea el pago y devuelve la URL de checkout de Wompi.
     * Requiere JWT (el estudiante debe estar autenticado).
     *
     * Body: { solicitudId, tipoPago }
     * tipoPago: TERMINACION | GRADO | MODALIDAD_CEREMONIA
     */
    @PostMapping("/crear")
    public ResponseEntity<?> crearPago(@RequestBody Map<String, Object> body) {
        try {
            Long solicitudId = Long.valueOf(body.get("solicitudId").toString());
            String tipoPago  = (String) body.get("tipoPago");
            String cedula    = (String) body.get("cedula");

            if (solicitudId == null || tipoPago == null || tipoPago.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "solicitudId y tipoPago son requeridos"));

            return ResponseEntity.ok(wompiService.crearPago(solicitudId, cedula, tipoPago));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creando el pago: " + e.getMessage()));
        }
    }

    /**
     * GET /api/pagos/estado/{referencia}
     * Consulta el estado de un pago por su referencia.
     * Se llama desde ResultadoPago.jsx después de que Wompi redirige.
     */
    @GetMapping("/estado/{referencia}")
    public ResponseEntity<?> consultarEstado(@PathVariable String referencia) {
        try {
            return ResponseEntity.ok(wompiService.consultarEstado(referencia));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/pagos/webhook
     * Recibe eventos de Wompi (NO requiere JWT — Wompi llama esto directamente).
     * Debe estar en la whitelist de SecurityConfig.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody Map<String, Object> evento) {
        wompiService.procesarWebhook(evento);
        return ResponseEntity.ok().build();
    }
}