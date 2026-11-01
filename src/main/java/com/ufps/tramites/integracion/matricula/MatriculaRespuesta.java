package com.ufps.tramites.integracion.matricula;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Formato de la respuesta 200 de la operación C1 tal como la define el contrato
 * v0.1 (TP-143). Solo lo usa el adaptador; las fechas llegan como texto y se
 * validan al traducir para poder marcar la respuesta como inválida.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record MatriculaRespuesta(
        String cedula,
        String codigo,
        String nombres,
        String apellidos,
        String email,
        String estado,
        Programa programa,
        Admision admision,
        Matricula matricula,
        Creditos creditos) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Programa(Long id, String codigo, String nombre, Integer creditosRequeridos) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Admision(Long cohorteId, String cohorte, String semestre, String fechaAdmision, String fechaIngreso) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Matricula(String estado, BigDecimal valor, String fechaVencimiento) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Creditos(Integer requeridos, Integer aprobados) {}
}
