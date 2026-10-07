package com.ufps.tramites.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

// FIX TP-191 (Bryan Niño, 07/10/2026): manejadores globales para los dos
// escenarios que la auditoría dejaba sin mensaje legible:
//   - MaxUploadSizeExceededException: 413 con el límite real configurado.
//   - MissingServletRequestPartException: 400 diciendo qué parte faltó
//     (p.ej. "foto" o "actaSustentacion") en POST /api/solicitudes/grado.
@ControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.servlet.multipart.max-file-size:15MB}")
    private String maxFileSize;

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> manejarArchivoDemasiadoGrande(
            MaxUploadSizeExceededException ex) {
        String mensaje = "El archivo supera el tamaño máximo permitido ("
                + maxFileSize + ").";
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", mensaje));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, String>> manejarParteFaltante(
            MissingServletRequestPartException ex) {
        String mensaje = "Falta el documento obligatorio \""
                + ex.getRequestPartName() + "\".";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", mensaje));
    }
}
