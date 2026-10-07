package com.ufps.tramites.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

// FIX TP-193 (Johan Bueno, 07/10/2026): utilidad para calcular la fecha
// límite de la liquidación en días hábiles (lunes a viernes). No se
// contempla calendario de festivos — no existe uno institucional en el
// alcance del módulo; si en el futuro se agrega, este helper es el único
// punto a tocar. Antes la fecha límite se calculaba con plusDays(5), lo
// que podía caer en fin de semana y adelantar el vencimiento efectivo
// (CP-009).
public final class FechaUtil {

    private FechaUtil() {}

    public static LocalDate sumarDiasHabiles(LocalDate inicio, int dias) {
        if (inicio == null) return null;
        LocalDate fecha = inicio;
        int sumados = 0;
        while (sumados < dias) {
            fecha = fecha.plusDays(1);
            if (fecha.getDayOfWeek() != DayOfWeek.SATURDAY
                    && fecha.getDayOfWeek() != DayOfWeek.SUNDAY) {
                sumados++;
            }
        }
        return fecha;
    }

    public static boolean estaVencida(LocalDate fechaLimite) {
        return fechaLimite != null && fechaLimite.isBefore(LocalDate.now());
    }
}
