package com.ufps.tramites.dto;

import java.time.LocalDateTime;

public record NotificacionDTO(
        Long id,
        String tipo,
        String titulo,
        String mensaje,
        String enlace,
        boolean leida,
        LocalDateTime fechaCreacion
) {}
