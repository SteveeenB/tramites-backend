package com.ufps.tramites.controller;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TP-154 (SC02-T6): health check del contenedor sin agregar Actuator.
 * Lo usan el HEALTHCHECK del Dockerfile y el equipo que despliega para saber
 * que la aplicación arrancó (si el arranque falla, por ejemplo sin JWT_SECRET o
 * sin BD, este endpoint nunca responde). Es público en SecurityConfig.
 */
@Tag(name = "Salud", description = "Estado del servicio para el health check del contenedor.")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Operation(summary = "Estado del servicio",
               description = "Devuelve 200 con {\"status\":\"UP\"} cuando la aplicación está arriba. No requiere token.")
    @ApiResponse(responseCode = "200", description = "Servicio arriba")
    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
