package com.ufps.tramites.integracion.matricula;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Situación académica de un estudiante según el Módulo de Matrícula (SC-03, RF16).
 *
 * Es el modelo propio de Trámites: el resto del código depende de estos nombres
 * y no de los campos del API de Matrícula (RNF06 · Aislamiento). La traducción
 * se hace en {@link MatriculaHttpClient}.
 *
 * {@code creditosAprobados} puede ser null: Matrícula no registra ese dato
 * (brecha B1 del informe TP-143) hasta que TP-144 defina su fuente.
 */
public record SituacionAcademica(
        String cedula,
        String codigo,
        String nombres,
        String apellidos,
        String email,
        String estado,
        Long programaId,
        String programaCodigo,
        String programaNombre,
        Long cohorteId,
        String cohorte,
        String semestre,
        LocalDate fechaAdmision,
        LocalDate fechaIngreso,
        String estadoMatricula,
        BigDecimal valorMatricula,
        LocalDate fechaVencimientoMatricula,
        int creditosRequeridos,
        Integer creditosAprobados) {
}
