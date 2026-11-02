package com.ufps.tramites.service;

import org.junit.jupiter.api.Test;

import com.ufps.tramites.dto.AvanceAcademico;

import static org.assertj.core.api.Assertions.assertThat;

/** Bordes de la regla del 100 % de créditos (SC-03, RF16). */
class AvanceAcademicoTest {

    @Test
    void aprobadosIgualARequeridos_cumple() {
        var avance = new AvanceAcademico(48, 48);
        assertThat(avance.cumple()).isTrue();
        assertThat(avance.faltantes()).isZero();
        assertThat(avance.mensaje()).isNull();
    }

    @Test
    void aprobadosMayorQueRequeridos_cumple() {
        assertThat(new AvanceAcademico(60, 48).cumple()).isTrue();
        assertThat(new AvanceAcademico(60, 48).faltantes()).isZero();
    }

    @Test
    void aprobadosMenorQueRequeridos_indicaCuantosFaltan() {
        var avance = new AvanceAcademico(30, 48);
        assertThat(avance.cumple()).isFalse();
        assertThat(avance.faltantes()).isEqualTo(18);
        assertThat(avance.mensaje()).isEqualTo("Te faltan 18 créditos (llevas 30 de 48).");
    }

    @Test
    void ceroAprobados_noCumple() {
        assertThat(new AvanceAcademico(0, 48).cumple()).isFalse();
        assertThat(new AvanceAcademico(0, 48).faltantes()).isEqualTo(48);
    }

    @Test
    void aprobadosNull_noEsEvaluableNiCumple() {
        var avance = new AvanceAcademico(null, 48);
        assertThat(avance.evaluable()).isFalse();
        assertThat(avance.cumple()).isFalse();
        assertThat(avance.mensaje()).contains("no están registrados");
    }

    @Test
    void requeridosNullOCero_noEsEvaluableNiCumple() {
        // Antes, con fallback 0, cualquier estudiante quedaba en "Requisitos completos".
        for (Integer requeridos : new Integer[] { null, 0 }) {
            var avance = new AvanceAcademico(10, requeridos);
            assertThat(avance.evaluable()).isFalse();
            assertThat(avance.cumple()).isFalse();
            assertThat(avance.mensaje()).contains("oficina de Posgrados");
        }
    }
}
