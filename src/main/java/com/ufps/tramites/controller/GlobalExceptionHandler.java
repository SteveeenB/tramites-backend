package com.ufps.tramites.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Traduce errores de carga de archivos y de parámetros faltantes a respuestas
 * JSON con el formato {"error": "..."} que usa el resto de la API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Nombres legibles de los campos de los formularios multipart. */
    private static final Map<String, String> NOMBRE_CAMPO = Map.of(
        "foto",              "Fotografía actualizada",
        "actaSustentacion",  "Acta de sustentación",
        "certificadoIngles", "Certificado de inglés",
        "archivo",           "Archivo",
        "tituloProyecto",    "Título del proyecto",
        "resumen",           "Resumen del proyecto",
        "tipoProyecto",      "Tipo de proyecto"
    );

    @Value("${spring.servlet.multipart.max-file-size:15MB}")
    private DataSize maxFileSize;

    @Value("${spring.servlet.multipart.max-request-size:60MB}")
    private DataSize maxRequestSize;

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tamanoExcedido(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(error(
            "El archivo supera el tamaño máximo permitido de " + maxFileSize.toMegabytes()
                + " MB por archivo (" + maxRequestSize.toMegabytes() + " MB en total por envío)."
        ));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, String>> parteFaltante(MissingServletRequestPartException e) {
        return ResponseEntity.badRequest().body(error(
            "Falta el documento obligatorio: " + nombre(e.getRequestPartName()) + "."
        ));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> parametroFaltante(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest().body(error(
            "Falta el campo obligatorio: " + nombre(e.getParameterName()) + "."
        ));
    }

    private String nombre(String campo) {
        return NOMBRE_CAMPO.getOrDefault(campo, campo);
    }

    private Map<String, String> error(String mensaje) {
        return Map.of("error", mensaje);
    }
}
