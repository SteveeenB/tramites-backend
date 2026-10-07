package com.ufps.tramites.util;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// FIX TP-193 (Johan Bueno, 07/10/2026): fija el contrato de sumarDiasHabiles.
class FechaUtilTest {

    @Test
    void sumarDiasHabiles_desdeViernes_saltaElFinDeSemana() {
        // 2026-10-09 es viernes. +1 día hábil = lunes 2026-10-12.
        LocalDate viernes = LocalDate.of(2026, 10, 9);
        assertThat(FechaUtil.sumarDiasHabiles(viernes, 1))
                .isEqualTo(LocalDate.of(2026, 10, 12));
    }

    @Test
    void sumarDiasHabiles_cincoDiasDesdeMiercoles_cae_miercolesSiguiente() {
        // Miércoles 2026-10-07 + 5 días hábiles = miércoles 2026-10-14.
        LocalDate miercoles = LocalDate.of(2026, 10, 7);
        assertThat(FechaUtil.sumarDiasHabiles(miercoles, 5))
                .isEqualTo(LocalDate.of(2026, 10, 14));
    }

    @Test
    void estaVencida_fechaAnteriorAHoy_esTrue() {
        assertThat(FechaUtil.estaVencida(LocalDate.now().minusDays(1))).isTrue();
    }

    @Test
    void estaVencida_fechaFutura_esFalse() {
        assertThat(FechaUtil.estaVencida(LocalDate.now().plusDays(1))).isFalse();
    }

    @Test
    void estaVencida_null_esFalse() {
        assertThat(FechaUtil.estaVencida(null)).isFalse();
    }
}
