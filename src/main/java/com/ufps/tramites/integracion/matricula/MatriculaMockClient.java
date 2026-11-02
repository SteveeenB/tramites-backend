package com.ufps.tramites.integracion.matricula;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Implementación simulada del Módulo de Matrícula (TP-146) para avanzar mientras
 * el servicio real no esté disponible. Activa con {@code matricula.modo=mock}
 * (valor por defecto). Los datos son ficticios y usan las cédulas de los
 * estudiantes demo de {@code data.sql}.
 *
 * Cédulas especiales para probar los flujos alternos de RF16:
 * <ul>
 *   <li>{@value #CEDULA_SERVICIO_CAIDO} → NO_DISPONIBLE</li>
 *   <li>{@value #CEDULA_RESPUESTA_INVALIDA} → RESPUESTA_INVALIDA</li>
 *   <li>{@value #CEDULA_SIN_CREDITOS_APROBADOS} → ENCONTRADO con créditos aprobados null (brecha B1)</li>
 *   <li>cualquier otra cédula válida → NO_ENCONTRADO</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "matricula.modo", havingValue = "mock", matchIfMissing = true)
public class MatriculaMockClient implements MatriculaClient {

    private static final Logger log = LoggerFactory.getLogger(MatriculaMockClient.class);

    static final String CEDULA_SERVICIO_CAIDO = "1090000503";
    static final String CEDULA_RESPUESTA_INVALIDA = "1090000422";
    static final String CEDULA_SIN_CREDITOS_APROBADOS = "1090000001";

    private static final int CREDITOS_PROGRAMA = 56;

    private static final Map<String, SituacionAcademica> ESTUDIANTES = Map.of(
            "1098765432", estudiante("1098765432", "20261001", "Juan", "Perez", 40),
            "1098765435", estudiante("1098765435", "20261005", "Laura", "Gomez", 56),
            "1098765436", estudiante("1098765436", "20261006", "Pedro", "Martinez", 56),
            "1098765437", estudiante("1098765437", "20261007", "Carlos", "Rueda", 56),
            "2000000010", estudiante("2000000010", "EST010", "Andrea", "Prueba Grado", 56),
            CEDULA_SIN_CREDITOS_APROBADOS,
            estudiante(CEDULA_SIN_CREDITOS_APROBADOS, "20261099", "Laura Marcela", "Pérez Gómez", null));

    @PostConstruct
    void avisar() {
        log.warn("[MATRICULA] usando datos SIMULADOS (matricula.modo=mock). No usar en producción.");
    }

    @Override
    public ConsultaMatricula consultarSituacion(String cedula) {
        log.info("[MATRICULA-MOCK] consulta cedula={}", cedula);
        if (!MatriculaClient.cedulaValida(cedula)) {
            return ConsultaMatricula.cedulaInvalida();
        }
        if (CEDULA_SERVICIO_CAIDO.equals(cedula)) {
            return ConsultaMatricula.noDisponible();
        }
        if (CEDULA_RESPUESTA_INVALIDA.equals(cedula)) {
            return ConsultaMatricula.respuestaInvalida();
        }
        SituacionAcademica s = ESTUDIANTES.get(cedula);
        return s != null ? ConsultaMatricula.encontrado(s) : ConsultaMatricula.noEncontrado();
    }

    @Override
    public boolean estaDisponible() {
        return true;
    }

    private static SituacionAcademica estudiante(String cedula, String codigo, String nombres,
                                                 String apellidos, Integer creditosAprobados) {
        return new SituacionAcademica(
                cedula, codigo, nombres, apellidos,
                nombres.toLowerCase().split(" ")[0] + "@example.com",
                "ACTIVO",
                1L, "MGE", "Maestría en Gerencia de Empresas",
                34L, "Cohorte 2025-1", "2025-1",
                LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 3),
                "COMPLETADO", new BigDecimal("4500000"), LocalDate.of(2025, 2, 1),
                CREDITOS_PROGRAMA, creditosAprobados);
    }
}
